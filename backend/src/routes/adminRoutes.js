const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const UserVerification = require('../models/UserVerification');
const User = require('../models/User');
const ParkingListing = require('../models/ParkingListing');
const Booking = require('../models/Booking');
const { requireAuth } = require('../middleware/auth');
const { requireAdmin } = require('../middleware/verification');
const { isDbConnected } = require('../config/db');

// Database connectivity check
const checkDb = (req, res, next) => {
  if (!isDbConnected()) {
    return res.status(503).json({
      success: false,
      message: 'Database service is currently unavailable. Check MONGODB_URI configuration.'
    });
  }
  next();
};

// All admin routes require authentication, database connection, and admin privileges
router.use(requireAuth, checkDb, requireAdmin);

// ============================================================================
// ADMIN DASHBOARD & METRICS
// ============================================================================

// @route   GET /api/admin/dashboard/stats
// @desc    Retrieve real MongoDB-backed metrics for the admin dashboard
// @access  Protected (Admin only)
router.get('/dashboard/stats', async (req, res) => {
  try {
    const [
      totalUsers,
      activeListings,
      totalBookings,
      pendingVerifications,
      pendingBookingRequests,
      completedBookings
    ] = await Promise.all([
      User.countDocuments(),
      ParkingListing.countDocuments({ isActive: true }),
      Booking.countDocuments(),
      UserVerification.countDocuments({ status: 'PENDING' }),
      Booking.countDocuments({ status: 'PENDING' }),
      Booking.countDocuments({ status: 'COMPLETED' })
    ]);

    return res.status(200).json({
      success: true,
      data: {
        totalUsers,
        activeListings,
        totalBookings,
        pendingVerifications,
        pendingBookingRequests,
        completedBookings
      }
    });
  } catch (error) {
    console.error(`[Admin Dashboard] Stats error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving dashboard statistics'
    });
  }
});

// ============================================================================
// ADMIN VERIFICATION MANAGEMENT (VERIFICATION CENTER)
// ============================================================================

// @route   GET /api/admin/verifications
// @desc    Queue of verification submissions for admin review (NO Base64 images returned)
// @access  Protected (Admin only)
router.get('/verifications', async (req, res) => {
  try {
    const { status } = req.query;
    const filter = {};
    if (status) {
      filter.status = status.toUpperCase();
    }

    const queue = await UserVerification.find(filter)
      .select('-identityDocumentData -addressDocumentData -selfieData')
      .sort({ submittedAt: -1 })
      .lean();

    return res.status(200).json({
      success: true,
      count: queue.length,
      data: queue
    });
  } catch (error) {
    console.error(`[Admin Verification] Queue error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving admin verification queue'
    });
  }
});

// @route   GET /api/admin/verifications/:id/documents
// @desc    Fetch submitted document previews for authorized manual review
// @access  Protected (Admin only)
router.get('/verifications/:id/documents', async (req, res) => {
  const { id } = req.params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid verification ID format'
    });
  }

  try {
    const record = await UserVerification.findById(id);
    if (!record) {
      return res.status(404).json({
        success: false,
        message: 'Verification record not found'
      });
    }

    return res.status(200).json({
      success: true,
      data: record
    });
  } catch (error) {
    console.error(`[Admin Verification] Documents error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving verification documents'
    });
  }
});

// @route   PATCH /api/admin/verifications/:id/review
// @desc    Approve or reject a user verification
// @access  Protected (Admin only)
router.patch('/verifications/:id/review', async (req, res) => {
  const { id } = req.params;
  const { status, rejectionReason } = req.body;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid verification ID format'
    });
  }

  if (!['APPROVED', 'REJECTED'].includes(status)) {
    return res.status(400).json({
      success: false,
      message: 'Status must be either APPROVED or REJECTED'
    });
  }

  if (status === 'REJECTED' && (!rejectionReason || typeof rejectionReason !== 'string' || rejectionReason.trim().length === 0)) {
    return res.status(400).json({
      success: false,
      message: 'rejectionReason is required when rejecting verification'
    });
  }

  try {
    const record = await UserVerification.findById(id);
    if (!record) {
      return res.status(404).json({
        success: false,
        message: 'Verification record not found'
      });
    }

    record.status = status;
    record.rejectionReason = status === 'REJECTED' ? rejectionReason.trim() : null;
    record.reviewedAt = new Date();
    record.reviewedBy = req.user.email || req.user.uid;
    await record.save();

    // Update User model verificationStatus
    await User.findOneAndUpdate(
      { firebaseUid: record.userId },
      { $set: { verificationStatus: status } }
    );

    return res.status(200).json({
      success: true,
      message: `Verification ${status.toLowerCase()} successfully`,
      data: {
        _id: record._id,
        userId: record.userId,
        status: record.status,
        rejectionReason: record.rejectionReason,
        reviewedAt: record.reviewedAt,
        reviewedBy: record.reviewedBy
      }
    });
  } catch (error) {
    console.error(`[Admin Verification] Review error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error processing verification review'
    });
  }
});

// ============================================================================
// ADMIN MANAGEMENT GAPS: USERS, LISTINGS, BOOKINGS
// ============================================================================

// @route   GET /api/admin/users
// @desc    List registered users for admin oversight
// @access  Protected (Admin only)
router.get('/users', async (req, res) => {
  try {
    const users = await User.find({})
      .select('name email role verificationStatus listingCount createdAt')
      .sort({ createdAt: -1 })
      .limit(100)
      .lean();

    return res.status(200).json({
      success: true,
      count: users.length,
      data: users
    });
  } catch (error) {
    console.error(`[Admin Management] Users error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving users'
    });
  }
});

// @route   GET /api/admin/listings
// @desc    List all parking listings for admin oversight
// @access  Protected (Admin only)
router.get('/listings', async (req, res) => {
  try {
    const listings = await ParkingListing.find({})
      .select('title address pricePerHour parkingType vehicleCompatibility isActive ownerFirebaseUid createdAt')
      .sort({ createdAt: -1 })
      .limit(100)
      .lean();

    return res.status(200).json({
      success: true,
      count: listings.length,
      data: listings
    });
  } catch (error) {
    console.error(`[Admin Management] Listings error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving listings'
    });
  }
});

// @route   GET /api/admin/bookings
// @desc    List bookings for admin oversight
// @access  Protected (Admin only)
router.get('/bookings', async (req, res) => {
  try {
    const bookings = await Booking.find({})
      .populate('listingId', 'title address')
      .select('bookingCode status totalAmount startTime endTime driverFirebaseUid ownerFirebaseUid createdAt')
      .sort({ createdAt: -1 })
      .limit(100)
      .lean();

    return res.status(200).json({
      success: true,
      count: bookings.length,
      data: bookings
    });
  } catch (error) {
    console.error(`[Admin Management] Bookings error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving bookings'
    });
  }
});

module.exports = router;
