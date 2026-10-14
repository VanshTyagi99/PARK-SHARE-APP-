const express = require('express');
const router = express.Router();
const UserVerification = require('../models/UserVerification');
const User = require('../models/User');
const { requireAuth } = require('../middleware/auth');
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

// @route   GET /api/verification/status
// @desc    Get current user verification status (NO document Base64 returned)
// @access  Protected
router.get('/status', requireAuth, checkDb, async (req, res) => {
  try {
    const user = await User.findOne({ firebaseUid: req.user.uid });
    const verification = await UserVerification.findOne({ userId: req.user.uid });

    const status = verification ? verification.status : (user?.verificationStatus || 'NOT_SUBMITTED');

    return res.status(200).json({
      success: true,
      data: {
        status,
        rejectionReason: verification?.rejectionReason || null,
        submittedAt: verification?.submittedAt || null,
        reviewedAt: verification?.reviewedAt || null
      }
    });
  } catch (error) {
    console.error(`[Verification API] Status error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving verification status'
    });
  }
});

// @route   POST /api/verification/submit
// @desc    Submit all three verification documents (Identity, Address, Selfie)
// @access  Protected
router.post('/submit', requireAuth, checkDb, async (req, res) => {
  const {
    identityDocumentType,
    identityDocumentData,
    addressDocumentType,
    addressDocumentData,
    selfieData
  } = req.body;

  // 1. Mandatory Identity Proof check
  if (!identityDocumentType || !['DRIVING_LICENCE', 'VOTER_ID', 'PASSPORT', 'AADHAAR'].includes(identityDocumentType)) {
    return res.status(400).json({
      success: false,
      message: 'Valid identityDocumentType is required (DRIVING_LICENCE, VOTER_ID, PASSPORT, AADHAAR)'
    });
  }
  if (!identityDocumentData || typeof identityDocumentData !== 'string' || identityDocumentData.trim().length === 0) {
    return res.status(400).json({
      success: false,
      message: 'identityDocumentData is required'
    });
  }

  // 2. Mandatory Address Proof check
  if (!addressDocumentType || !['GOVERNMENT_ID', 'UTILITY_BILL', 'RENTAL_AGREEMENT', 'BANK_STATEMENT', 'OTHER'].includes(addressDocumentType)) {
    return res.status(400).json({
      success: false,
      message: 'Valid addressDocumentType is required (GOVERNMENT_ID, UTILITY_BILL, RENTAL_AGREEMENT, BANK_STATEMENT, OTHER)'
    });
  }
  if (!addressDocumentData || typeof addressDocumentData !== 'string' || addressDocumentData.trim().length === 0) {
    return res.status(400).json({
      success: false,
      message: 'addressDocumentData is required'
    });
  }

  // 3. Mandatory Selfie check
  if (!selfieData || typeof selfieData !== 'string' || selfieData.trim().length === 0) {
    return res.status(400).json({
      success: false,
      message: 'selfieData is required for manual visual verification'
    });
  }

  // Size guard: per-file ~2MB limit
  const maxBytes = 2 * 1024 * 1024;
  if (identityDocumentData.length > maxBytes || addressDocumentData.length > maxBytes || selfieData.length > maxBytes) {
    return res.status(400).json({
      success: false,
      message: 'Uploaded documents exceed allowable size (max 2MB per document)'
    });
  }

  try {
    const user = await User.findOne({ firebaseUid: req.user.uid });

    // Upsert verification record setting status to PENDING
    const verification = await UserVerification.findOneAndUpdate(
      { userId: req.user.uid },
      {
        userId: req.user.uid,
        userEmail: req.user.email || user?.email || '',
        userName: user?.fullName || '',
        identityDocumentType,
        identityDocumentData,
        addressDocumentType,
        addressDocumentData,
        selfieData,
        status: 'PENDING',
        rejectionReason: null,
        submittedAt: new Date(),
        reviewedAt: null,
        reviewedBy: null
      },
      { upsert: true, new: true, runValidators: true }
    );

    // Update status in User model for fast middleware lookup
    await User.findOneAndUpdate(
      { firebaseUid: req.user.uid },
      {
        $setOnInsert: {
          firebaseUid: req.user.uid,
          email: req.user.email || ''
        },
        $set: {
          verificationStatus: 'PENDING'
        }
      },
      { upsert: true, new: true }
    );

    return res.status(200).json({
      success: true,
      message: 'Verification submitted successfully and is now under review',
      data: {
        status: 'PENDING',
        submittedAt: verification.submittedAt
      }
    });
  } catch (error) {
    console.error(`[Verification API] Submit error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error processing verification submission'
    });
  }
});

module.exports = router;
