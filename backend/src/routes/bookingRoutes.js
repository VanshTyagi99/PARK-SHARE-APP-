const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const Booking = require('../models/Booking');
const ParkingListing = require('../models/ParkingListing');
const { requireAuth } = require('../middleware/auth');
const { requireApprovedVerification } = require('../middleware/verification');
const { isDbConnected } = require('../config/db');

// Middleware: Verify database connectivity
const checkDb = (req, res, next) => {
  if (!isDbConnected()) {
    return res.status(503).json({
      success: false,
      message: 'Database service is currently unavailable. Check MONGODB_URI configuration.'
    });
  }
  next();
};

/**
 * Sweeper function: atomically marks expired pending requests and unconfirmed bookings.
 * 1. AWAITING_DRIVER_CONFIRMATION past confirmationDeadline -> EXPIRED.
 * 2. PENDING bookings within 15 minutes of slot start time -> EXPIRED.
 * Safe across concurrent instances via atomic MongoDB operations.
 */
const expireStaleBookings = async () => {
  try {
    const now = new Date();

    // 1. Expire unconfirmed driver bookings whose deadline has passed
    await Booking.updateMany(
      {
        status: 'AWAITING_DRIVER_CONFIRMATION',
        confirmationDeadline: { $lte: now }
      },
      {
        $set: {
          status: 'EXPIRED',
          expiredAt: now,
          expiryReason: 'Driver confirmation deadline elapsed'
        },
        $push: {
          statusHistory: {
            status: 'EXPIRED',
            timestamp: now,
            note: 'Driver confirmation deadline elapsed without confirmation'
          }
        }
      }
    );

    // 2. Expire unattended pending bookings whose slot starts within 15 minutes or earlier
    const pendingCutoff = new Date(now.getTime() + 15 * 60 * 1000);
    await Booking.updateMany(
      {
        status: 'PENDING',
        startTime: { $lte: pendingCutoff }
      },
      {
        $set: {
          status: 'EXPIRED',
          expiredAt: now,
          expiryReason: 'Owner response window elapsed before slot start'
        },
        $push: {
          statusHistory: {
            status: 'EXPIRED',
            timestamp: now,
            note: 'Pending booking request expired before start time'
          }
        }
      }
    );
  } catch (err) {
    console.error('[Booking Expiry Sweeper Error]', err.message);
  }
};

/**
 * POST /api/bookings
 * Driver submits a new booking request.
 * Concurrency-safe: utilizes parent document write lock & session-level conflict verification.
 */
router.post('/', requireAuth, checkDb, requireApprovedVerification, async (req, res) => {
  try {
    const { listingId, startTime, endTime, vehicleNumber, vehicleType, driverNotes } = req.body;

    // Validate listing ID
    if (!listingId || !mongoose.Types.ObjectId.isValid(listingId)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid parking listing ID format'
      });
    }

    // Validate dates presence and format
    if (!startTime || !endTime) {
      return res.status(400).json({
        success: false,
        message: 'Both startTime and endTime are required'
      });
    }

    const start = new Date(startTime).getTime();
    const end = new Date(endTime).getTime();

    if (isNaN(start) || isNaN(end)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid start or end date format'
      });
    }

    if (start >= end) {
      return res.status(400).json({
        success: false,
        message: 'Start time must be before end time'
      });
    }

    // Reject bookings in the past (allow 2-minute clock skew / submission grace period)
    const gracePastThreshold = Date.now() - 2 * 60 * 1000;
    if (start < gracePastThreshold) {
      return res.status(400).json({
        success: false,
        message: 'Booking start time cannot be in the past'
      });
    }

    // Calculate duration in hours (ceiling)
    const durationMs = end - start;
    if (durationMs < 60 * 60 * 1000) {
      return res.status(400).json({
        success: false,
        message: 'Minimum booking duration is 1 hour'
      });
    }

    const durationHours = Math.ceil(durationMs / (60 * 60 * 1000));
    if (durationHours > 24) {
      return res.status(400).json({
        success: false,
        message: 'Maximum booking duration is 24 hours'
      });
    }

    // Trigger lazy expiry sweep prior to conflict inspection
    await expireStaleBookings();

    // Concurrency Strategy: Multi-document ACID session with parent write lock
    const MAX_RETRIES = 3;
    let attempt = 0;

    while (attempt < MAX_RETRIES) {
      attempt++;
      let session;
      let isSessionActive = false;

      try {
        session = await mongoose.startSession();
        session.startTransaction();
        isSessionActive = true;

        // 1. Lock and increment parent ParkingListing bookingLockVersion
        const listing = await ParkingListing.findOneAndUpdate(
          { _id: listingId, isActive: true },
          { $inc: { bookingLockVersion: 1 } },
          { session, new: true }
        );

        if (!listing) {
          await session.abortTransaction();
          await session.endSession();
          isSessionActive = false;

          const exists = await ParkingListing.findById(listingId);
          if (!exists) {
            return res.status(404).json({
              success: false,
              message: 'Parking listing not found'
            });
          }
          return res.status(400).json({
            success: false,
            message: 'Parking listing is currently inactive'
          });
        }

        // 2. Reject owner booking own space
        if (listing.ownerFirebaseUid === req.user.uid) {
          await session.abortTransaction();
          await session.endSession();
          isSessionActive = false;
          return res.status(400).json({
            success: false,
            message: 'You cannot book your own parking space'
          });
        }

        // 3. Conflict verification on half-open interval [startTime, endTime)
        // Two intervals overlap if and only if: existing.start < requested.end && existing.end > requested.start
        const conflict = await Booking.findOne({
          listingId,
          status: { $in: Booking.ACTIVE_BLOCKING_STATUSES },
          startTime: { $lt: new Date(endTime) },
          endTime: { $gt: new Date(startTime) }
        }).session(session);

        if (conflict) {
          await session.abortTransaction();
          await session.endSession();
          isSessionActive = false;
          return res.status(409).json({
            success: false,
            message: 'This parking slot has already been reserved for the selected time window.',
            conflict: {
              startTime: conflict.startTime,
              endTime: conflict.endTime,
              status: conflict.status
            }
          });
        }

        // 4. Compute authoritative price
        const totalAmount = durationHours * listing.pricePerHour;

        // 5. Create Booking within ACID session
        const [newBooking] = await Booking.create(
          [
            {
              listingId,
              listingTitle: listing.title,
              listingAddress: listing.address,
              driverFirebaseUid: req.user.uid,
              ownerFirebaseUid: listing.ownerFirebaseUid,
              startTime: new Date(startTime),
              endTime: new Date(endTime),
              totalHours: durationHours,
              hourlyRateSnapshot: listing.pricePerHour,
              totalAmount,
              status: 'PENDING',
              vehicleNumber: vehicleNumber || '',
              vehicleType: vehicleType || 'Car',
              driverNotes: driverNotes || '',
              statusHistory: [
                {
                  status: 'PENDING',
                  timestamp: new Date(),
                  note: 'Booking request created by driver'
                }
              ]
            }
          ],
          { session }
        );

        await session.commitTransaction();
        await session.endSession();
        isSessionActive = false;

        return res.status(201).json({
          success: true,
          message: 'Booking request submitted successfully. Awaiting owner approval.',
          data: newBooking
        });
      } catch (txnError) {
        if (isSessionActive && session) {
          try {
            await session.abortTransaction();
            await session.endSession();
          } catch (_) {}
        }

        // Check for replica set unsupported fallback (e.g. standalone test environment)
        if (
          txnError.message &&
          (txnError.message.includes('replica set') ||
            txnError.message.includes('standalone') ||
            txnError.message.includes('Transaction numbers are only allowed'))
        ) {
          // Fallback to atomic lock without transaction session
          const fallbackListing = await ParkingListing.findOneAndUpdate(
            { _id: listingId, isActive: true },
            { $inc: { bookingLockVersion: 1 } },
            { new: true }
          );

          if (!fallbackListing) {
            return res.status(400).json({ success: false, message: 'Parking listing is currently inactive' });
          }
          if (fallbackListing.ownerFirebaseUid === req.user.uid) {
            return res.status(400).json({ success: false, message: 'You cannot book your own parking space' });
          }

          const fallbackConflict = await Booking.findOne({
            listingId,
            status: { $in: Booking.ACTIVE_BLOCKING_STATUSES },
            startTime: { $lt: new Date(endTime) },
            endTime: { $gt: new Date(startTime) }
          });

          if (fallbackConflict) {
            return res.status(409).json({
              success: false,
              message: 'This parking slot has already been reserved for the selected time window.',
              conflict: {
                startTime: fallbackConflict.startTime,
                endTime: fallbackConflict.endTime,
                status: fallbackConflict.status
              }
            });
          }

          const totalAmount = durationHours * fallbackListing.pricePerHour;
          const fallbackBooking = await Booking.create({
            listingId,
            listingTitle: fallbackListing.title,
            listingAddress: fallbackListing.address,
            driverFirebaseUid: req.user.uid,
            ownerFirebaseUid: fallbackListing.ownerFirebaseUid,
            startTime: new Date(startTime),
            endTime: new Date(endTime),
            totalHours: durationHours,
            hourlyRateSnapshot: fallbackListing.pricePerHour,
            totalAmount,
            status: 'PENDING',
            vehicleNumber: vehicleNumber || '',
            vehicleType: vehicleType || 'Car',
            driverNotes: driverNotes || ''
          });

          return res.status(201).json({
            success: true,
            message: 'Booking request submitted successfully. Awaiting owner approval.',
            data: fallbackBooking
          });
        }

        // Check for transient transaction errors (WriteConflict)
        const isTransient =
          (txnError.hasErrorLabel && txnError.hasErrorLabel('TransientTransactionError')) ||
          txnError.code === 112 ||
          (txnError.message && txnError.message.includes('WriteConflict'));

        if (isTransient && attempt < MAX_RETRIES) {
          await new Promise((r) => setTimeout(r, 40 * attempt));
          continue;
        }

        if (isTransient) {
          return res.status(409).json({
            success: false,
            message: 'Concurrent booking conflict occurred. Please try again.'
          });
        }

        throw txnError;
      }
    }
  } catch (error) {
    console.error('[Create Booking Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to create booking reservation'
    });
  }
});

/**
 * GET /api/bookings/my-bookings
 * Returns bookings for authenticated driver.
 */
router.get('/my-bookings', requireAuth, checkDb, async (req, res) => {
  try {
    await expireStaleBookings();

    const query = { driverFirebaseUid: req.user.uid };
    const { status } = req.query;

    if (status) {
      const upper = status.toUpperCase();
      if (upper === 'UPCOMING') {
        query.status = { $in: ['PENDING', 'ACCEPTED', 'AWAITING_DRIVER_CONFIRMATION', 'CONFIRMED'] };
        query.startTime = { $gte: new Date() };
      } else if (upper === 'ACTIVE') {
        query.status = 'CHECKED_IN';
      } else if (upper === 'COMPLETED') {
        query.status = { $in: ['COMPLETED', 'CHECKED_OUT', 'CANCELLED', 'EXPIRED', 'REJECTED'] };
      } else if (upper !== 'ALL') {
        query.status = upper;
      }
    }

    const bookings = await Booking.find(query).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: bookings.length,
      data: bookings
    });
  } catch (error) {
    console.error('[Get My Bookings Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to retrieve bookings'
    });
  }
});

/**
 * GET /api/bookings/owner-requests
 * Returns incoming booking requests for the authenticated parking owner.
 * Security: qrToken is omitted from owner list response.
 */
router.get('/owner-requests', requireAuth, checkDb, async (req, res) => {
  try {
    await expireStaleBookings();

    const query = { ownerFirebaseUid: req.user.uid };
    const { status } = req.query;

    if (status) {
      const upper = status.toUpperCase();
      if (upper === 'PENDING') {
        query.status = 'PENDING';
      } else if (upper === 'ACTIVE') {
        query.status = { $in: ['ACCEPTED', 'AWAITING_DRIVER_CONFIRMATION', 'CONFIRMED', 'CHECKED_IN'] };
      } else if (upper === 'COMPLETED') {
        query.status = { $in: ['COMPLETED', 'CHECKED_OUT', 'CANCELLED', 'EXPIRED', 'REJECTED'] };
      } else if (upper !== 'ALL') {
        query.status = upper;
      }
    }

    const requests = await Booking.find(query).select('-qrToken').sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: requests.length,
      data: requests
    });
  } catch (error) {
    console.error('[Get Owner Requests Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to retrieve owner booking requests'
    });
  }
});

/**
 * GET /api/bookings/:id
 * Retrieves details for a specific booking.
 * Enforces participant authorization (caller must be driver or owner).
 */
router.get('/:id', requireAuth, checkDb, async (req, res) => {
  try {
    if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid booking ID format'
      });
    }

    await expireStaleBookings();

    const booking = await Booking.findById(req.params.id);
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    // Authorization: Must be driver or owner
    if (booking.driverFirebaseUid !== req.user.uid && booking.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Access denied. You are neither the driver nor the owner of this booking.'
      });
    }

    // Mask qrToken if accessed by owner (only driver holds verification pass)
    const bookingData = booking.toObject();
    if (req.user.uid !== booking.driverFirebaseUid) {
      delete bookingData.qrToken;
    }

    res.status(200).json({
      success: true,
      data: bookingData
    });
  } catch (error) {
    console.error('[Get Booking Details Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to retrieve booking details'
    });
  }
});

/**
 * PATCH /api/bookings/:id/accept
 * Parking owner accepts a pending booking request.
 * Transitions PENDING -> AWAITING_DRIVER_CONFIRMATION with calculated deadline.
 */
router.patch('/:id/accept', requireAuth, checkDb, async (req, res) => {
  try {
    if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid booking ID format'
      });
    }

    await expireStaleBookings();

    const booking = await Booking.findById(req.params.id);
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    if (booking.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Only the listing owner can accept this booking request'
      });
    }

    if (booking.status !== 'PENDING') {
      return res.status(400).json({
        success: false,
        message: `Booking is not in PENDING status (current status: ${booking.status})`
      });
    }

    const now = new Date();
    const { deadline, canAccept, reason } = Booking.calculateConfirmationDeadline(now, booking.startTime);

    if (!canAccept) {
      await Booking.updateOne(
        { _id: booking._id },
        {
          $set: {
            status: 'EXPIRED',
            expiredAt: now,
            expiryReason: reason
          },
          $push: {
            statusHistory: {
              status: 'EXPIRED',
              timestamp: now,
              note: reason
            }
          }
        }
      );

      return res.status(400).json({
        success: false,
        message: `Cannot accept booking: ${reason}. Booking has been expired.`
      });
    }

    // Atomic conditional status update
    const updated = await Booking.findOneAndUpdate(
      {
        _id: booking._id,
        status: 'PENDING',
        ownerFirebaseUid: req.user.uid
      },
      {
        $set: {
          status: 'AWAITING_DRIVER_CONFIRMATION',
          ownerRespondedAt: now,
          confirmationDeadline: deadline
        },
        $push: {
          statusHistory: {
            status: 'AWAITING_DRIVER_CONFIRMATION',
            timestamp: now,
            note: 'Owner accepted booking request; awaiting driver confirmation'
          }
        }
      },
      { new: true }
    );

    if (!updated) {
      return res.status(409).json({
        success: false,
        message: 'Booking status was updated concurrently. Please refresh.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Booking request accepted. Awaiting driver confirmation.',
      data: updated
    });
  } catch (error) {
    console.error('[Accept Booking Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to accept booking request'
    });
  }
});

/**
 * PATCH /api/bookings/:id/confirm
 * Driver confirms accepted booking before deadline.
 * Generates cryptographic QR verification token.
 */
router.patch('/:id/confirm', requireAuth, checkDb, async (req, res) => {
  try {
    if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid booking ID format'
      });
    }

    await expireStaleBookings();

    const booking = await Booking.findById(req.params.id);
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    if (booking.driverFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Only the driver who requested this booking can confirm it'
      });
    }

    if (booking.status !== 'AWAITING_DRIVER_CONFIRMATION') {
      return res.status(400).json({
        success: false,
        message: `Booking is not awaiting driver confirmation (current status: ${booking.status})`
      });
    }

    const now = new Date();
    if (booking.confirmationDeadline && now > booking.confirmationDeadline) {
      await Booking.updateOne(
        { _id: booking._id },
        {
          $set: {
            status: 'EXPIRED',
            expiredAt: now,
            expiryReason: 'Driver confirmation deadline elapsed'
          },
          $push: {
            statusHistory: {
              status: 'EXPIRED',
              timestamp: now,
              note: 'Confirmation deadline elapsed without confirmation'
            }
          }
        }
      );

      return res.status(400).json({
        success: false,
        message: 'Confirmation deadline has passed. This booking has expired.'
      });
    }

    const qrToken = Booking.generateQrToken();

    // Atomic conditional status update
    const confirmed = await Booking.findOneAndUpdate(
      {
        _id: booking._id,
        driverFirebaseUid: req.user.uid,
        status: 'AWAITING_DRIVER_CONFIRMATION',
        confirmationDeadline: { $gte: now }
      },
      {
        $set: {
          status: 'CONFIRMED',
          driverConfirmedAt: now,
          qrToken
        },
        $push: {
          statusHistory: {
            status: 'CONFIRMED',
            timestamp: now,
            note: 'Driver confirmed booking and QR verification pass generated'
          }
        }
      },
      { new: true }
    );

    if (!confirmed) {
      return res.status(409).json({
        success: false,
        message: 'Booking could not be confirmed. It may have expired or already been confirmed.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Booking confirmed successfully.',
      data: confirmed
    });
  } catch (error) {
    console.error('[Confirm Booking Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to confirm booking'
    });
  }
});

/**
 * PATCH /api/bookings/:id/reject
 * Owner rejects a pending booking request.
 */
router.patch('/:id/reject', requireAuth, checkDb, async (req, res) => {
  try {
    if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid booking ID format'
      });
    }

    const booking = await Booking.findById(req.params.id);
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    if (booking.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Only the listing owner can reject this booking'
      });
    }

    if (booking.status !== 'PENDING') {
      return res.status(400).json({
        success: false,
        message: `Only PENDING bookings can be rejected (current status: ${booking.status})`
      });
    }

    const rejectionReason = req.body.rejectionReason || 'Owner declined booking request';

    const rejected = await Booking.findOneAndUpdate(
      {
        _id: booking._id,
        ownerFirebaseUid: req.user.uid,
        status: 'PENDING'
      },
      {
        $set: {
          status: 'REJECTED',
          ownerRespondedAt: new Date(),
          rejectionReason
        },
        $push: {
          statusHistory: {
            status: 'REJECTED',
            timestamp: new Date(),
            note: rejectionReason
          }
        }
      },
      { new: true }
    );

    if (!rejected) {
      return res.status(409).json({
        success: false,
        message: 'Booking status changed concurrently.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Booking request rejected.',
      data: rejected
    });
  } catch (error) {
    console.error('[Reject Booking Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to reject booking request'
    });
  }
});

/**
 * PATCH /api/bookings/:id/cancel
 * Driver or owner cancels an active/pending reservation.
 */
router.patch('/:id/cancel', requireAuth, checkDb, async (req, res) => {
  try {
    if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid booking ID format'
      });
    }

    const booking = await Booking.findById(req.params.id);
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    const isDriver = booking.driverFirebaseUid === req.user.uid;
    const isOwner = booking.ownerFirebaseUid === req.user.uid;

    if (!isDriver && !isOwner) {
      return res.status(403).json({
        success: false,
        message: 'You are not authorized to cancel this booking'
      });
    }

    const permittedStatuses = ['PENDING', 'ACCEPTED', 'AWAITING_DRIVER_CONFIRMATION', 'CONFIRMED'];
    if (!permittedStatuses.includes(booking.status)) {
      return res.status(400).json({
        success: false,
        message: `Booking cannot be cancelled from its current status (${booking.status})`
      });
    }

    const actor = isDriver ? 'driver' : 'owner';
    const reason = req.body.cancellationReason || `Cancelled by ${actor}`;

    const cancelled = await Booking.findOneAndUpdate(
      {
        _id: booking._id,
        status: { $in: permittedStatuses }
      },
      {
        $set: {
          status: 'CANCELLED',
          cancelledAt: new Date(),
          cancelledBy: actor,
          cancellationReason: reason
        },
        $push: {
          statusHistory: {
            status: 'CANCELLED',
            timestamp: new Date(),
            note: `Cancelled by ${actor}: ${reason}`
          }
        }
      },
      { new: true }
    );

    if (!cancelled) {
      return res.status(409).json({
        success: false,
        message: 'Booking status changed concurrently.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Booking cancelled successfully.',
      data: cancelled
    });
  } catch (error) {
    console.error('[Cancel Booking Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to cancel booking'
    });
  }
});

/**
 * POST /api/bookings/:id/check-in
 * Check-in verification using QR token.
 */
router.post('/:id/check-in', requireAuth, checkDb, async (req, res) => {
  try {
    if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid booking ID format'
      });
    }

    const { qrToken } = req.body;
    if (!qrToken || typeof qrToken !== 'string') {
      return res.status(400).json({
        success: false,
        message: 'QR verification token is required'
      });
    }

    const booking = await Booking.findById(req.params.id);
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    // Authorization: Must be driver or owner
    if (booking.driverFirebaseUid !== req.user.uid && booking.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized to verify check-in for this booking'
      });
    }

    if (booking.status === 'CHECKED_IN') {
      return res.status(400).json({
        success: false,
        message: 'Booking has already been checked in'
      });
    }

    if (booking.status !== 'CONFIRMED') {
      return res.status(400).json({
        success: false,
        message: `Cannot check in. Booking status is ${booking.status}`
      });
    }

    if (!booking.qrToken || booking.qrToken !== qrToken.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Invalid QR verification token'
      });
    }

    const now = new Date();
    // Allow arrival starting 15 minutes before slot start time
    const earlyArrivalWindow = new Date(booking.startTime.getTime() - 15 * 60 * 1000);

    if (now < earlyArrivalWindow) {
      return res.status(400).json({
        success: false,
        message: 'Check-in is permitted starting 15 minutes before booking start time'
      });
    }

    if (now > booking.endTime) {
      return res.status(400).json({
        success: false,
        message: 'Booking slot has already elapsed. Check-in is no longer allowed.'
      });
    }

    const checkedIn = await Booking.findOneAndUpdate(
      {
        _id: booking._id,
        status: 'CONFIRMED',
        qrToken: booking.qrToken
      },
      {
        $set: {
          status: 'CHECKED_IN',
          checkedInAt: now
        },
        $push: {
          statusHistory: {
            status: 'CHECKED_IN',
            timestamp: now,
            note: 'Driver checked in successfully via QR verification'
          }
        }
      },
      { new: true }
    );

    if (!checkedIn) {
      return res.status(409).json({
        success: false,
        message: 'Check-in failed due to concurrent update.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Check-in verified successfully. Space is now active.',
      data: checkedIn
    });
  } catch (error) {
    console.error('[Check-In Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to process check-in'
    });
  }
});

/**
 * POST /api/bookings/:id/check-out
 * Check-out verification using QR token.
 * Slot is immediately released and booking transitions to COMPLETED.
 */
router.post('/:id/check-out', requireAuth, checkDb, async (req, res) => {
  try {
    if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid booking ID format'
      });
    }

    const { qrToken } = req.body;
    if (!qrToken || typeof qrToken !== 'string') {
      return res.status(400).json({
        success: false,
        message: 'QR verification token is required'
      });
    }

    const booking = await Booking.findById(req.params.id);
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    if (booking.driverFirebaseUid !== req.user.uid && booking.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized to verify check-out for this booking'
      });
    }

    if (booking.status !== 'CHECKED_IN') {
      return res.status(400).json({
        success: false,
        message: `Cannot check out. Booking status is ${booking.status}`
      });
    }

    if (!booking.qrToken || booking.qrToken !== qrToken.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Invalid QR verification token'
      });
    }

    const now = new Date();

    const completed = await Booking.findOneAndUpdate(
      {
        _id: booking._id,
        status: 'CHECKED_IN',
        qrToken: booking.qrToken
      },
      {
        $set: {
          status: 'COMPLETED',
          checkedOutAt: now,
          completedAt: now
        },
        $push: {
          statusHistory: {
            status: 'COMPLETED',
            timestamp: now,
            note: 'Check-out completed and parking space released'
          }
        }
      },
      { new: true }
    );

    if (!completed) {
      return res.status(409).json({
        success: false,
        message: 'Check-out failed due to concurrent update.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Check-out verified. Parking session completed.',
      data: completed
    });
  } catch (error) {
    console.error('[Check-Out Error]', error);
    res.status(500).json({
      success: false,
      message: 'Failed to process check-out'
    });
  }
});

// Export router directly with attached sweeper function
router.expireStaleBookings = expireStaleBookings;
module.exports = router;

