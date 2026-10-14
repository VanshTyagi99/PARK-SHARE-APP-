const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const ParkingListing = require('../models/ParkingListing');
const Booking = require('../models/Booking');
const User = require('../models/User');
const { requireAuth } = require('../middleware/auth');
const { requireApprovedVerification } = require('../middleware/verification');
const { isDbConnected } = require('../config/db');

// Helper to normalize vehicle compatibility and parking type
const normalizeCompatibility = (parkingType, compatibility) => {
  if (compatibility && ['CAR', 'BIKE', 'CAR_AND_BIKE'].includes(compatibility.toUpperCase())) {
    const comp = compatibility.toUpperCase();
    const type = comp === 'CAR' ? 'Car' : comp === 'BIKE' ? 'Bike' : 'Car + Bike';
    return { parkingType: type, vehicleCompatibility: comp };
  }

  const type = parkingType || 'Car';
  let comp = 'CAR';
  if (type === 'Bike') comp = 'BIKE';
  else if (type === 'Car + Bike') comp = 'CAR_AND_BIKE';

  return { parkingType: type, vehicleCompatibility: comp };
};

// Helper to escape regex special characters (prevents ReDoS)
const escapeRegex = (str) => {
  return typeof str === 'string' ? str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') : '';
};

// Helper to calculate Haversine distance in kilometers
const calculateDistanceKm = (lat1, lon1, lat2, lon2) => {
  if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
  const numLat1 = Number(lat1);
  const numLon1 = Number(lon1);
  const numLat2 = Number(lat2);
  const numLon2 = Number(lon2);
  if (isNaN(numLat1) || isNaN(numLon1) || isNaN(numLat2) || isNaN(numLon2)) return null;
  if ((numLat1 === 0 && numLon1 === 0) || (numLat2 === 0 && numLon2 === 0)) return null;

  const R = 6371; // Earth's radius in km
  const dLat = (numLat2 - numLat1) * (Math.PI / 180);
  const dLon = (numLon2 - numLon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(numLat1 * (Math.PI / 180)) *
      Math.cos(numLat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
};

// Middleware to check database connectivity
const checkDb = (req, res, next) => {
  if (!isDbConnected()) {
    return res.status(503).json({
      success: false,
      message: 'Database service is currently unavailable. Check MONGODB_URI configuration.'
    });
  }
  next();
};

// @route   POST /api/parking
// @desc    Create a new parking listing for the authenticated owner
// @access  Protected
router.post('/', requireAuth, checkDb, requireApprovedVerification, async (req, res) => {
  const {
    title,
    address,
    description,
    parkingType,
    vehicleCompatibility,
    pricePerHour,
    availableFrom,
    availableUntil,
    photos,
    latitude,
    longitude
  } = req.body;

  // Validation
  if (!title || typeof title !== 'string' || title.trim().length < 3) {
    return res.status(400).json({
      success: false,
      message: 'Title is required and must be at least 3 characters'
    });
  }

  if (!address || typeof address !== 'string' || address.trim().length < 3) {
    return res.status(400).json({
      success: false,
      message: 'Address is required and must be at least 3 characters'
    });
  }

  const numericPrice = Number(pricePerHour);
  if (isNaN(numericPrice) || numericPrice <= 0) {
    return res.status(400).json({
      success: false,
      message: 'Price per hour must be a valid number greater than zero'
    });
  }

  if (!availableFrom || typeof availableFrom !== 'string' || availableFrom.trim().length === 0) {
    return res.status(400).json({
      success: false,
      message: 'Available from time is required'
    });
  }

  if (!availableUntil || typeof availableUntil !== 'string' || availableUntil.trim().length === 0) {
    return res.status(400).json({
      success: false,
      message: 'Available until time is required'
    });
  }

  const { parkingType: normType, vehicleCompatibility: normComp } = normalizeCompatibility(
    parkingType,
    vehicleCompatibility
  );

  // Enforce Max 10 Listings Limit with Concurrency Safety
  // Step 1: Ensure user record exists with listingCount
  let user = await User.findOne({ firebaseUid: req.user.uid });
  if (!user) {
    const existingCount = await ParkingListing.countDocuments({ ownerFirebaseUid: req.user.uid });
    user = await User.create({
      firebaseUid: req.user.uid,
      email: req.user.email || '',
      listingCount: existingCount
    });
  }

  // Step 2: Atomic conditional increment (only succeeds if listingCount < 10)
  const updatedUser = await User.findOneAndUpdate(
    {
      firebaseUid: req.user.uid,
      $or: [
        { listingCount: { $lt: 10 } },
        { listingCount: { $exists: false } }
      ]
    },
    { $inc: { listingCount: 1 } },
    { new: true }
  );

  // If atomic update failed, verify count directly
  if (!updatedUser) {
    const activeCount = await ParkingListing.countDocuments({ ownerFirebaseUid: req.user.uid });
    // Resync user's listing count
    await User.updateOne({ firebaseUid: req.user.uid }, { listingCount: activeCount });

    if (activeCount >= 10) {
      return res.status(400).json({
        success: false,
        message: 'Listing limit reached. A verified owner can have at most 10 parking listings.'
      });
    }
  }

  try {
    const listing = new ParkingListing({
      ownerFirebaseUid: req.user.uid,
      title: title.trim(),
      address: address.trim(),
      description: description ? description.trim() : '',
      parkingType: normType,
      vehicleCompatibility: normComp,
      pricePerHour: numericPrice,
      availableFrom: availableFrom.trim(),
      availableUntil: availableUntil.trim(),
      photos: Array.isArray(photos) ? photos.slice(0, 5) : [],
      latitude: latitude != null && !isNaN(Number(latitude)) ? Number(latitude) : null,
      longitude: longitude != null && !isNaN(Number(longitude)) ? Number(longitude) : null,
      isActive: true
    });

    await listing.save();

    res.status(201).json({
      success: true,
      message: 'Parking listing created successfully',
      data: listing
    });
  } catch (error) {
    // Rollback atomic counter increment if save fails
    await User.updateOne({ firebaseUid: req.user.uid }, { $inc: { listingCount: -1 } });
    console.error(`[Parking API] Create error: ${error.message}`);
    res.status(400).json({
      success: false,
      message: error.message || 'Failed to create parking listing'
    });
  }
});

// @route   GET /api/parking/my-listings
// @desc    Retrieve all parking listings owned by the authenticated user
// @access  Protected
router.get('/my-listings', requireAuth, checkDb, async (req, res) => {
  try {
    const listings = await ParkingListing.find({
      ownerFirebaseUid: req.user.uid
    }).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: listings.length,
      maxLimit: 10,
      data: listings
    });
  } catch (error) {
    console.error(`[Parking API] My listings error: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error retrieving your parking listings'
    });
  }
});

// @route   GET /api/parking/search
// @desc    Discover and search active parking spaces with vehicle, price, text & proximity filters
// @access  Protected
router.get('/search', requireAuth, checkDb, requireApprovedVerification, async (req, res) => {
  const {
    query,
    vehicleType,
    minPrice,
    maxPrice,
    latitude,
    longitude,
    radiusKm,
    limit,
    page
  } = req.query;

  // Validate price parameters
  if (minPrice !== undefined && (isNaN(Number(minPrice)) || Number(minPrice) < 0)) {
    return res.status(400).json({
      success: false,
      message: 'minPrice must be a valid positive number'
    });
  }

  if (maxPrice !== undefined && (isNaN(Number(maxPrice)) || Number(maxPrice) < 0)) {
    return res.status(400).json({
      success: false,
      message: 'maxPrice must be a valid positive number'
    });
  }

  if (minPrice !== undefined && maxPrice !== undefined && Number(minPrice) > Number(maxPrice)) {
    return res.status(400).json({
      success: false,
      message: 'minPrice cannot be greater than maxPrice'
    });
  }

  // Validate coordinates if provided
  let userLat = null;
  let userLng = null;
  if (latitude !== undefined) {
    userLat = Number(latitude);
    if (isNaN(userLat) || userLat < -90 || userLat > 90) {
      return res.status(400).json({
        success: false,
        message: 'Invalid latitude parameter (must be between -90 and 90)'
      });
    }
  }
  if (longitude !== undefined) {
    userLng = Number(longitude);
    if (isNaN(userLng) || userLng < -180 || userLng > 180) {
      return res.status(400).json({
        success: false,
        message: 'Invalid longitude parameter (must be between -180 and 180)'
      });
    }
  }

  // Validate and constrain radius
  const maxRadiusKm = Math.min(Math.max(Number(radiusKm) || 25, 1), 100);

  // Pagination / Limit
  const maxLimit = Math.min(Math.max(Number(limit) || 50, 1), 100);
  const skipCount = ((Math.max(Number(page) || 1, 1)) - 1) * maxLimit;

  // Build filter conditions — ALWAYS enforce isActive: true
  const conditions = [{ isActive: true }];

  // Text search on title, address, description (safely escaped)
  if (query && typeof query === 'string' && query.trim().length > 0) {
    const safeRegex = escapeRegex(query.trim());
    conditions.push({
      $or: [
        { title: { $regex: safeRegex, $options: 'i' } },
        { address: { $regex: safeRegex, $options: 'i' } },
        { description: { $regex: safeRegex, $options: 'i' } }
      ]
    });
  }

  // Vehicle type filter
  if (vehicleType && typeof vehicleType === 'string') {
    const vt = vehicleType.trim();
    if (vt && vt !== 'All' && vt !== 'ALL') {
      if (vt === 'Car' || vt === 'CAR') {
        conditions.push({
          $or: [
            { parkingType: { $in: ['Car', 'Car + Bike'] } },
            { vehicleCompatibility: { $in: ['CAR', 'CAR_AND_BIKE'] } }
          ]
        });
      } else if (vt === 'Bike' || vt === 'BIKE') {
        conditions.push({
          $or: [
            { parkingType: { $in: ['Bike', 'Car + Bike'] } },
            { vehicleCompatibility: { $in: ['BIKE', 'CAR_AND_BIKE'] } }
          ]
        });
      } else if (vt === 'Car + Bike' || vt === 'CAR_AND_BIKE') {
        conditions.push({
          $or: [
            { parkingType: 'Car + Bike' },
            { vehicleCompatibility: 'CAR_AND_BIKE' }
          ]
        });
      }
    }
  }

  // Price range filters
  if (minPrice !== undefined) {
    conditions.push({ pricePerHour: { $gte: Number(minPrice) } });
  }
  if (maxPrice !== undefined) {
    conditions.push({ pricePerHour: { $lte: Number(maxPrice) } });
  }

  const finalFilter = conditions.length === 1 ? conditions[0] : { $and: conditions };

  try {
    const rawListings = await ParkingListing.find(finalFilter)
      .sort({ createdAt: -1 })
      .limit(maxLimit * 2)
      .lean();

    // Enrich with calculated distance if coordinates provided
    let results = rawListings.map((item) => {
      let distanceKm = null;
      if (userLat != null && userLng != null) {
        distanceKm = calculateDistanceKm(userLat, userLng, item.latitude, item.longitude);
      }
      return {
        ...item,
        distanceKm
      };
    });

    // If user provided coordinates, filter by radius if distance is known and sort by proximity
    if (userLat != null && userLng != null) {
      results = results
        .filter((item) => item.distanceKm === null || item.distanceKm <= maxRadiusKm)
        .sort((a, b) => {
          if (a.distanceKm != null && b.distanceKm != null) {
            return a.distanceKm - b.distanceKm;
          }
          if (a.distanceKm != null) return -1;
          if (b.distanceKm != null) return 1;
          return 0;
        });
    }

    const paginated = results.slice(skipCount, skipCount + maxLimit);

    res.status(200).json({
      success: true,
      count: paginated.length,
      total: results.length,
      data: paginated
    });
  } catch (error) {
    console.error(`[Parking API] Search error: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error searching parking listings'
    });
  }
});

// @route   GET /api/parking/:id
// @desc    Get details of a specific parking listing
// @access  Protected
router.get('/:id', requireAuth, checkDb, requireApprovedVerification, async (req, res) => {
  const { id } = req.params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid parking listing ID format'
    });
  }

  try {
    const listing = await ParkingListing.findById(id);

    if (!listing) {
      return res.status(404).json({
        success: false,
        message: 'Parking listing not found'
      });
    }

    res.status(200).json({
      success: true,
      data: listing
    });
  } catch (error) {
    console.error(`[Parking API] Get by ID error: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error retrieving parking listing'
    });
  }
});

// @route   PUT /api/parking/:id
// @route   PUT /api/parking/:id
// @desc    Update a parking listing owned by the current user
// @access  Protected
router.put('/:id', requireAuth, checkDb, requireApprovedVerification, async (req, res) => {
  const { id } = req.params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid parking listing ID format'
    });
  }

  try {
    const listing = await ParkingListing.findById(id);

    if (!listing) {
      return res.status(404).json({
        success: false,
        message: 'Parking listing not found'
      });
    }

    // Ownership verification
    if (listing.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized: You can only edit your own parking listings'
      });
    }

    const {
      title,
      address,
      description,
      parkingType,
      vehicleCompatibility,
      pricePerHour,
      availableFrom,
      availableUntil,
      photos
    } = req.body;

    // Explicit whitelist update
    if (title !== undefined) {
      if (typeof title !== 'string' || title.trim().length < 3) {
        return res.status(400).json({
          success: false,
          message: 'Title must be at least 3 characters'
        });
      }
      listing.title = title.trim();
    }

    if (address !== undefined) {
      if (typeof address !== 'string' || address.trim().length < 3) {
        return res.status(400).json({
          success: false,
          message: 'Address must be at least 3 characters'
        });
      }
      listing.address = address.trim();
    }

    if (description !== undefined) {
      listing.description = typeof description === 'string' ? description.trim() : '';
    }

    if (parkingType !== undefined || vehicleCompatibility !== undefined) {
      const { parkingType: normType, vehicleCompatibility: normComp } = normalizeCompatibility(
        parkingType || listing.parkingType,
        vehicleCompatibility || listing.vehicleCompatibility
      );
      listing.parkingType = normType;
      listing.vehicleCompatibility = normComp;
    }

    if (pricePerHour !== undefined) {
      const numericPrice = Number(pricePerHour);
      if (isNaN(numericPrice) || numericPrice <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Price per hour must be a valid number greater than zero'
        });
      }
      listing.pricePerHour = numericPrice;
    }

    if (availableFrom !== undefined) {
      if (typeof availableFrom !== 'string' || availableFrom.trim().length === 0) {
        return res.status(400).json({
          success: false,
          message: 'Available from time cannot be empty'
        });
      }
      listing.availableFrom = availableFrom.trim();
    }

    if (availableUntil !== undefined) {
      if (typeof availableUntil !== 'string' || availableUntil.trim().length === 0) {
        return res.status(400).json({
          success: false,
          message: 'Available until time cannot be empty'
        });
      }
      listing.availableUntil = availableUntil.trim();
    }

    if (photos !== undefined && Array.isArray(photos)) {
      listing.photos = photos.slice(0, 5);
    }

    if (latitude !== undefined) {
      const latNum = Number(latitude);
      if (latitude === null || isNaN(latNum) || latNum < -90 || latNum > 90) {
        return res.status(400).json({
          success: false,
          message: 'Latitude must be a valid number between -90 and 90'
        });
      }
      listing.latitude = latNum;
    }

    if (longitude !== undefined) {
      const lngNum = Number(longitude);
      if (longitude === null || isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
        return res.status(400).json({
          success: false,
          message: 'Longitude must be a valid number between -180 and 180'
        });
      }
      listing.longitude = lngNum;
    }

    await listing.save();

    res.status(200).json({
      success: true,
      message: 'Parking listing updated successfully',
      data: listing
    });
  } catch (error) {
    console.error(`[Parking API] Update error: ${error.message}`);
    res.status(400).json({
      success: false,
      message: error.message || 'Failed to update parking listing'
    });
  }
});

// @route   PATCH /api/parking/:id/status
// @desc    Activate or deactivate a parking listing owned by the current user
// @access  Protected
router.patch('/:id/status', requireAuth, checkDb, requireApprovedVerification, async (req, res) => {
  const { id } = req.params;
  const { isActive } = req.body;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid parking listing ID format'
    });
  }

  if (typeof isActive !== 'boolean') {
    return res.status(400).json({
      success: false,
      message: 'isActive must be a boolean (true or false)'
    });
  }

  try {
    const listing = await ParkingListing.findById(id);

    if (!listing) {
      return res.status(404).json({
        success: false,
        message: 'Parking listing not found'
      });
    }

    // Ownership verification
    if (listing.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized: You can only change status for your own parking listings'
      });
    }

    listing.isActive = isActive;
    await listing.save();

    res.status(200).json({
      success: true,
      message: `Listing ${isActive ? 'activated' : 'deactivated'} successfully`,
      data: listing
    });
  } catch (error) {
    console.error(`[Parking API] Status update error: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error updating listing status'
    });
  }
});

// @route   DELETE /api/parking/:id
// @desc    Permanently delete a parking listing owned by the current user
// @access  Protected
router.delete('/:id', requireAuth, checkDb, requireApprovedVerification, async (req, res) => {
  const { id } = req.params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid parking listing ID format'
    });
  }

  try {
    const listing = await ParkingListing.findById(id);

    if (!listing) {
      return res.status(404).json({
        success: false,
        message: 'Parking listing not found'
      });
    }

    // Ownership verification
    if (listing.ownerFirebaseUid !== req.user.uid) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized: You can only delete your own parking listings'
      });
    }

    // Reservation-Safety Guard: Check for active or blocking bookings
    const activeBooking = await Booking.findOne({
      listingId: id,
      status: { $in: ['PENDING', 'ACCEPTED', 'AWAITING_DRIVER_CONFIRMATION', 'CONFIRMED', 'CHECKED_IN'] }
    });

    if (activeBooking) {
      return res.status(400).json({
        success: false,
        message: 'Cannot delete parking space with active or upcoming reservations. Please wait for sessions to complete or cancel them first.',
        activeBookingId: activeBooking._id,
        bookingStatus: activeBooking.status
      });
    }

    // Delete listing document
    await ParkingListing.deleteOne({ _id: id });

    // Atomically decrement owner's listing count (ensuring it does not drop below 0)
    await User.updateOne(
      { firebaseUid: req.user.uid, listingCount: { $gt: 0 } },
      { $inc: { listingCount: -1 } }
    );

    res.status(200).json({
      success: true,
      message: 'Parking listing deleted successfully',
      deletedListingId: id
    });
  } catch (error) {
    console.error(`[Parking API] Delete error: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error deleting parking listing'
    });
  }
});

module.exports = router;
