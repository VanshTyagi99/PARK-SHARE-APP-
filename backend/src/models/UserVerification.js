const mongoose = require('mongoose');

const userVerificationSchema = new mongoose.Schema(
  {
    userId: {
      type: String,
      required: true,
      unique: true,
      index: true,
      trim: true
    },
    userEmail: {
      type: String,
      default: '',
      lowercase: true,
      trim: true
    },
    userName: {
      type: String,
      default: '',
      trim: true
    },
    identityDocumentType: {
      type: String,
      required: true,
      enum: ['DRIVING_LICENCE', 'VOTER_ID', 'PASSPORT', 'AADHAAR']
    },
    identityDocumentData: {
      type: String,
      required: true
    },
    addressDocumentType: {
      type: String,
      required: true,
      enum: ['GOVERNMENT_ID', 'UTILITY_BILL', 'RENTAL_AGREEMENT', 'BANK_STATEMENT', 'OTHER']
    },
    addressDocumentData: {
      type: String,
      required: true
    },
    selfieData: {
      type: String,
      required: true
    },
    status: {
      type: String,
      enum: ['NOT_SUBMITTED', 'PENDING', 'APPROVED', 'REJECTED'],
      default: 'PENDING',
      index: true
    },
    rejectionReason: {
      type: String,
      default: null,
      trim: true
    },
    submittedAt: {
      type: Date,
      default: Date.now
    },
    reviewedAt: {
      type: Date,
      default: null
    },
    reviewedBy: {
      type: String,
      default: null,
      trim: true
    }
  },
  {
    timestamps: true,
    toJSON: {
      transform: function (doc, ret) {
        delete ret.__v;
        return ret;
      }
    }
  }
);

const UserVerification = mongoose.model('UserVerification', userVerificationSchema);

module.exports = UserVerification;
