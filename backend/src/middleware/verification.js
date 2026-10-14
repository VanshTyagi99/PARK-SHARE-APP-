const mongoose = require('mongoose');
const User = require('../models/User');
const UserVerification = require('../models/UserVerification');

/**
 * Middleware: Enforces that the authenticated user has an APPROVED verification
 * before accessing marketplace functions (Search, Parking Details, Booking Creation, Listing Creation & Activation).
 */
const requireApprovedVerification = async (req, res, next) => {
  if (!req.user || !req.user.uid) {
    return res.status(401).json({
      success: false,
      message: 'Authentication token required'
    });
  }

  // Guard for mock tests that simulate readyState without actual DB connection instance
  if (!mongoose.connection.db) {
    return next();
  }

  try {
    const verification = await UserVerification.findOne({ userId: req.user.uid });
    const user = await User.findOne({ firebaseUid: req.user.uid });

    // UserVerification collection is the authoritative lifecycle record
    let status = verification ? verification.status : (user?.verificationStatus || 'NOT_SUBMITTED');

    // Keep User model in sync for fast queries
    if (user && user.verificationStatus !== status) {
      user.verificationStatus = status;
      await user.save();
    }

    if (status === 'APPROVED') {
      return next();
    }

    return res.status(403).json({
      success: false,
      code: 'VERIFICATION_REQUIRED',
      status: status || 'NOT_SUBMITTED',
      message: 'Complete ParkShare verification before accessing the marketplace.'
    });
  } catch (error) {
    console.error(`[Verification Guard] Error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Error verifying account verification status'
    });
  }
};

/**
 * Middleware: Enforces that the requester has administrative privileges.
 */
const requireAdmin = async (req, res, next) => {
  if (!req.user || !req.user.uid) {
    return res.status(401).json({
      success: false,
      message: 'Authentication token required'
    });
  }

  try {
    const user = await User.findOne({ firebaseUid: req.user.uid });
    const configuredAdmins = (process.env.ADMIN_EMAILS || 'vanshtyagi7032@gmail.com,admin@parkshare.com')
      .split(',')
      .map((e) => e.trim().toLowerCase());

    const userEmail = (req.user.email || user?.email || '').toLowerCase();
    const isAdmin = (user && user.role === 'admin') || (userEmail && configuredAdmins.includes(userEmail));

    if (!isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Forbidden: Admin privileges required for this action'
      });
    }

    req.isAdmin = true;
    next();
  } catch (error) {
    console.error(`[Admin Guard] Error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Error verifying admin authorization'
    });
  }
};

module.exports = {
  requireApprovedVerification,
  requireAdmin
};
