const mongoose = require('mongoose');

const conversationSchema = new mongoose.Schema(
  {
    bookingId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Booking',
      required: true,
      unique: true,
      index: true
    },
    driverFirebaseUid: {
      type: String,
      required: true,
      index: true
    },
    ownerFirebaseUid: {
      type: String,
      required: true,
      index: true
    },
    listingId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'ParkingListing'
    },
    lastMessageText: {
      type: String,
      default: ''
    },
    lastMessageSenderUid: {
      type: String,
      default: ''
    },
    lastMessageAt: {
      type: Date,
      default: Date.now
    }
  },
  {
    timestamps: true
  }
);

conversationSchema.index({ driverFirebaseUid: 1, updatedAt: -1 });
conversationSchema.index({ ownerFirebaseUid: 1, updatedAt: -1 });

module.exports = mongoose.model('Conversation', conversationSchema);
