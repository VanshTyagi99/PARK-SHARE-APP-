const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const Booking = require('../models/Booking');
const { requireAuth } = require('../middleware/auth');
const { requireApprovedVerification } = require('../middleware/verification');
const { isDbConnected } = require('../config/db');

// Check DB availability
const checkDb = (req, res, next) => {
  if (!isDbConnected()) {
    return res.status(503).json({
      success: false,
      message: 'Database service is currently unavailable.'
    });
  }
  next();
};

// All chat operations require authentication, database connection, and approved verification
router.use(requireAuth, checkDb, requireApprovedVerification);

// @route   GET /api/chat/conversations
// @desc    List all conversations for the authenticated user (as driver or owner)
router.get('/conversations', async (req, res) => {
  try {
    const uid = req.user.uid;

    const conversations = await Conversation.find({
      $or: [{ driverFirebaseUid: uid }, { ownerFirebaseUid: uid }]
    })
      .populate({
        path: 'bookingId',
        select: 'bookingCode status startTime endTime totalAmount listingSnapshot'
      })
      .populate({
        path: 'listingId',
        select: 'title address vehicleCompatibility pricePerHour'
      })
      .sort({ lastMessageAt: -1, updatedAt: -1 })
      .lean();

    return res.status(200).json({
      success: true,
      count: conversations.length,
      data: conversations
    });
  } catch (error) {
    console.error(`[Chat] List conversations error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving conversations'
    });
  }
});

// @route   GET /api/chat/booking/:bookingId
// @desc    Get or create conversation for a specific booking
router.get('/booking/:bookingId', async (req, res) => {
  const { bookingId } = req.params;

  if (!mongoose.Types.ObjectId.isValid(bookingId)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid booking ID format'
    });
  }

  try {
    const uid = req.user.uid;

    const booking = await Booking.findById(bookingId).lean();
    if (!booking) {
      return res.status(404).json({
        success: false,
        message: 'Booking not found'
      });
    }

    // Authorize: user must be the driver or the owner
    const isDriver = booking.driverFirebaseUid === uid;
    const isOwner = booking.ownerFirebaseUid === uid;

    if (!isDriver && !isOwner) {
      return res.status(403).json({
        success: false,
        message: 'You are not authorized to access chat for this booking'
      });
    }

    // Find or create conversation
    let conversation = await Conversation.findOne({ bookingId: booking._id });

    if (!conversation) {
      conversation = await Conversation.create({
        bookingId: booking._id,
        driverFirebaseUid: booking.driverFirebaseUid,
        ownerFirebaseUid: booking.ownerFirebaseUid,
        listingId: booking.listingId,
        lastMessageText: '',
        lastMessageSenderUid: '',
        lastMessageAt: new Date()
      });
    }

    const populated = await Conversation.findById(conversation._id)
      .populate({
        path: 'bookingId',
        select: 'bookingCode status startTime endTime totalAmount listingSnapshot'
      })
      .populate({
        path: 'listingId',
        select: 'title address vehicleCompatibility pricePerHour'
      })
      .lean();

    return res.status(200).json({
      success: true,
      data: populated
    });
  } catch (error) {
    console.error(`[Chat] Booking conversation error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving booking conversation'
    });
  }
});

// @route   GET /api/chat/conversations/:conversationId/messages
// @desc    Retrieve message history for an authorized conversation
router.get('/conversations/:conversationId/messages', async (req, res) => {
  const { conversationId } = req.params;

  if (!mongoose.Types.ObjectId.isValid(conversationId)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid conversation ID format'
    });
  }

  try {
    const uid = req.user.uid;

    const conversation = await Conversation.findById(conversationId).lean();
    if (!conversation) {
      return res.status(404).json({
        success: false,
        message: 'Conversation not found'
      });
    }

    // Authorize
    const isDriver = conversation.driverFirebaseUid === uid;
    const isOwner = conversation.ownerFirebaseUid === uid;

    if (!isDriver && !isOwner) {
      return res.status(403).json({
        success: false,
        message: 'You are not authorized to view messages in this conversation'
      });
    }

    const messages = await Message.find({ conversationId: conversation._id })
      .sort({ createdAt: 1 })
      .lean();

    return res.status(200).json({
      success: true,
      count: messages.length,
      data: messages
    });
  } catch (error) {
    console.error(`[Chat] Get messages error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error retrieving messages'
    });
  }
});

// @route   POST /api/chat/conversations/:conversationId/messages
// @desc    Send a message in an authorized conversation
router.post('/conversations/:conversationId/messages', async (req, res) => {
  const { conversationId } = req.params;
  const { text } = req.body;

  if (!mongoose.Types.ObjectId.isValid(conversationId)) {
    return res.status(400).json({
      success: false,
      message: 'Invalid conversation ID format'
    });
  }

  if (!text || typeof text !== 'string' || text.trim().length === 0) {
    return res.status(400).json({
      success: false,
      message: 'Message text cannot be empty'
    });
  }

  if (text.trim().length > 1000) {
    return res.status(400).json({
      success: false,
      message: 'Message text cannot exceed 1000 characters'
    });
  }

  try {
    const uid = req.user.uid;

    const conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      return res.status(404).json({
        success: false,
        message: 'Conversation not found'
      });
    }

    // Authorize
    const isDriver = conversation.driverFirebaseUid === uid;
    const isOwner = conversation.ownerFirebaseUid === uid;

    if (!isDriver && !isOwner) {
      return res.status(403).json({
        success: false,
        message: 'You are not authorized to send messages in this conversation'
      });
    }

    const senderRole = isDriver ? 'DRIVER' : 'OWNER';
    const trimmedText = text.trim();

    const message = await Message.create({
      conversationId: conversation._id,
      bookingId: conversation.bookingId,
      senderFirebaseUid: uid,
      senderRole,
      text: trimmedText
    });

    // Update conversation metadata
    conversation.lastMessageText = trimmedText;
    conversation.lastMessageSenderUid = uid;
    conversation.lastMessageAt = message.createdAt;
    await conversation.save();

    return res.status(201).json({
      success: true,
      data: message
    });
  } catch (error) {
    console.error(`[Chat] Send message error: ${error.message}`);
    return res.status(500).json({
      success: false,
      message: 'Server error sending message'
    });
  }
});

module.exports = router;
