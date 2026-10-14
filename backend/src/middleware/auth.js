const { admin, isFirebaseConfigured } = require('../config/firebase');

const requireAuth = async (req, res, next) => {
  const authHeader = req.headers.authorization;

  if (!authHeader) {
    return res.status(401).json({
      success: false,
      message: 'Authorization header is required (Format: Bearer <firebase-id-token>)'
    });
  }

  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0] !== 'Bearer') {
    return res.status(401).json({
      success: false,
      message: 'Invalid authorization format. Expected format: Bearer <firebase-id-token>'
    });
  }

  const token = parts[1];
  if (!token || token.trim().length === 0) {
    return res.status(401).json({
      success: false,
      message: 'Token is missing or empty'
    });
  }

  if (!isFirebaseConfigured()) {
    return res.status(500).json({
      success: false,
      message: 'Authentication service is not properly configured. Firebase Admin credentials required.'
    });
  }

  try {
    const decodedToken = await admin.auth().verifyIdToken(token);

    if (!decodedToken || !decodedToken.uid) {
      return res.status(401).json({
        success: false,
        message: 'Invalid token payload'
      });
    }

    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email || '',
      emailVerified: decodedToken.email_verified || false
    };

    next();
  } catch (error) {
    let errorMessage = 'Failed to verify authentication token';
    if (error.code === 'auth/id-token-expired') {
      errorMessage = 'Authentication token has expired. Please refresh token and try again.';
    } else if (error.code === 'auth/id-token-revoked') {
      errorMessage = 'Authentication token has been revoked.';
    } else if (error.code === 'auth/invalid-id-token' || error.code === 'auth/argument-error') {
      errorMessage = 'Invalid authentication token.';
    }

    return res.status(401).json({
      success: false,
      message: errorMessage,
      code: error.code || 'UNAUTHORIZED'
    });
  }
};

module.exports = {
  requireAuth
};
