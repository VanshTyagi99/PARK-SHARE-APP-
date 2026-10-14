const express = require('express');
const router = express.Router();
const User = require('../models/User');
const { requireAuth } = require('../middleware/auth');
const { isDbConnected } = require('../config/db');

// @route   GET /api/users/me
// @desc    Get current authenticated user profile
// @access  Protected (Firebase ID token required)
router.get('/me', requireAuth, async (req, res) => {
  if (!isDbConnected()) {
    return res.status(503).json({
      success: false,
      message: 'Database service is currently unavailable. Check MONGODB_URI configuration.'
    });
  }

  try {
    const user = await User.findOne({ firebaseUid: req.user.uid });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User profile not found in database. Run sync first.'
      });
    }

    res.status(200).json({
      success: true,
      data: user
    });
  } catch (error) {
    console.error(`[Users API] Error fetching profile: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error retrieving user profile'
    });
  }
});

// @route   POST /api/users/sync
// @desc    Idempotently sync/create user profile upon login or registration
// @access  Protected (Firebase ID token required)
router.post('/sync', requireAuth, async (req, res) => {
  if (!isDbConnected()) {
    return res.status(503).json({
      success: false,
      message: 'Database service is currently unavailable. Check MONGODB_URI configuration.'
    });
  }

  const { fullName, phone } = req.body;

  try {
    const updateData = {};
    if (fullName && typeof fullName === 'string' && fullName.trim().length > 0) {
      updateData.fullName = fullName.trim();
    }
    if (phone && typeof phone === 'string' && phone.trim().length > 0) {
      updateData.phone = phone.trim();
    }

    // Upsert user profile linked to Firebase UID
    const user = await User.findOneAndUpdate(
      { firebaseUid: req.user.uid },
      {
        $setOnInsert: {
          firebaseUid: req.user.uid,
          email: req.user.email || '',
          role: 'user'
        },
        $set: updateData
      },
      {
        new: true,
        upsert: true,
        runValidators: true,
        setDefaultsOnInsert: true
      }
    );

    res.status(200).json({
      success: true,
      message: 'User profile synchronized successfully',
      data: user
    });
  } catch (error) {
    console.error(`[Users API] Error syncing user: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error synchronizing user profile',
      error: error.message
    });
  }
});

module.exports = router;
