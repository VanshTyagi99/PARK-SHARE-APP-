const mongoose = require('mongoose');
const crypto = require('crypto');

const ACTIVE_BLOCKING_STATUSES = Object.freeze([
  'PENDING',
  'ACCEPTED',
  'AWAITING_DRIVER_CONFIRMATION',
  'CONFIRMED',
  'CHECKED_IN'
]);

const TERMINAL_OR_NON_BLOCKING_STATUSES = Object.freeze([
  'REJECTED',
  'CANCELLED',
  'EXPIRED',
  'CHECKED_OUT',
  'COMPLETED'
]);

const ALL_STATUSES = Object.freeze([
  ...ACTIVE_BLOCKING_STATUSES,
  ...TERMINAL_OR_NON_BLOCKING_STATUSES
]);

function generateBookingCode() {
  const datePart = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const randomHex = crypto.randomBytes(3).toString('hex').toUpperCase();
  return `PS-${datePart}-${randomHex}`;
}

const statusHistoryEntrySchema = new mongoose.Schema(
  {
    status: {
      type: String,
      required: true,
      enum: ALL_STATUSES
    },
    timestamp: {
      type: Date,
      default: Date.now
    },
    note: {
      type: String,
      default: '',
      trim: true
    }
  },
  { _id: false }
);

const bookingSchema = new mongoose.Schema(
  {
    bookingCode: {
      type: String,
      required: [true, 'Booking code is required'],
      unique: true,
      index: true,
      trim: true,
      default: generateBookingCode
    },
    listingId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'ParkingListing',
      required: [true, 'Listing ID is required'],
      index: true
    },
    listingTitle: {
      type: String,
      trim: true,
      default: ''
    },
    listingAddress: {
      type: String,
      trim: true,
      default: ''
    },
    driverFirebaseUid: {
      type: String,
      required: [true, 'Driver ID is required'],
      index: true,
      trim: true
    },
    ownerFirebaseUid: {
      type: String,
      required: [true, 'Owner ID is required'],
      index: true,
      trim: true
    },
    startTime: {
      type: Date,
      required: [true, 'Start time is required'],
      index: true
    },
    endTime: {
      type: Date,
      required: [true, 'End time is required'],
      index: true,
      validate: {
        validator: function (val) {
          if (!this.startTime || !val) return true;
          const s = new Date(this.startTime).getTime();
          const e = new Date(val).getTime();
          if (isNaN(s) || isNaN(e)) return false;
          return e > s;
        },
        message: 'End time must be after start time'
      }
    },
    totalHours: {
      type: Number,
      required: [true, 'Total hours is required'],
      min: [1, 'Total hours must be at least 1']
    },
    hourlyRateSnapshot: {
      type: Number,
      required: [true, 'Hourly rate snapshot is required'],
      min: [0, 'Hourly rate snapshot cannot be negative']
    },
    totalAmount: {
      type: Number,
      required: [true, 'Total amount is required'],
      min: [0, 'Total amount cannot be negative']
    },
    status: {
      type: String,
      enum: {
        values: ALL_STATUSES,
        message: '{VALUE} is not a valid booking status'
      },
      default: 'PENDING',
      index: true
    },
    vehicleNumber: {
      type: String,
      trim: true,
      default: ''
    },
    vehicleType: {
      type: String,
      trim: true,
      default: 'Car'
    },
    driverNotes: {
      type: String,
      trim: true,
      maxlength: [500, 'Driver notes cannot exceed 500 characters'],
      default: ''
    },
    qrToken: {
      type: String,
      default: null,
      index: true,
      sparse: true
    },
    confirmationDeadline: {
      type: Date,
      default: null
    },
    ownerRespondedAt: {
      type: Date,
      default: null
    },
    driverConfirmedAt: {
      type: Date,
      default: null
    },
    checkedInAt: {
      type: Date,
      default: null
    },
    checkedOutAt: {
      type: Date,
      default: null
    },
    completedAt: {
      type: Date,
      default: null
    },
    rejectionReason: {
      type: String,
      trim: true,
      default: null
    },
    cancelledAt: {
      type: Date,
      default: null
    },
    cancelledBy: {
      type: String,
      enum: ['driver', 'owner', 'system', null],
      default: null
    },
    cancellationReason: {
      type: String,
      trim: true,
      default: null
    },
    expiredAt: {
      type: Date,
      default: null
    },
    expiryReason: {
      type: String,
      trim: true,
      default: null
    },
    statusHistory: {
      type: [statusHistoryEntrySchema],
      default: []
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

// Pre-validate hook: generate bookingCode if missing and validate start/end time ordering
bookingSchema.pre('validate', function (next) {
  if (!this.bookingCode) {
    this.bookingCode = Booking.generateBookingCode();
  }

  if (this.startTime && this.endTime) {
    const start = new Date(this.startTime).getTime();
    const end = new Date(this.endTime).getTime();
    if (isNaN(start) || isNaN(end)) {
      this.invalidate('startTime', 'Invalid start or end date');
    } else if (start >= end) {
      this.invalidate('endTime', 'End time must be after start time');
    }
  }

  next();
});

// Pre-save hook: ensure initial status is tracked in statusHistory if empty
bookingSchema.pre('save', function (next) {
  if (this.isNew && (!this.statusHistory || this.statusHistory.length === 0)) {
    this.statusHistory = [
      {
        status: this.status || 'PENDING',
        timestamp: new Date(),
        note: 'Initial booking request created'
      }
    ];
  }
  next();
});

// Helper instance method to record a status change with history entry
bookingSchema.methods.recordStatusChange = function (newStatus, note = '') {
  if (!ALL_STATUSES.includes(newStatus)) {
    throw new Error(`Invalid booking status: ${newStatus}`);
  }
  this.status = newStatus;
  this.statusHistory.push({
    status: newStatus,
    timestamp: new Date(),
    note
  });
};

// Static helper: determine if two half-open intervals [s1, e1) and [s2, e2) overlap
bookingSchema.statics.hasTimeOverlap = function (start1, end1, start2, end2) {
  const s1 = new Date(start1).getTime();
  const e1 = new Date(end1).getTime();
  const s2 = new Date(start2).getTime();
  const e2 = new Date(end2).getTime();

  if (isNaN(s1) || isNaN(e1) || isNaN(s2) || isNaN(e2)) {
    throw new Error('Invalid dates provided for overlap calculation');
  }

  // Two half-open intervals [S1, E1) and [S2, E2) overlap if and only if:
  // s1 < e2 AND e1 > s2
  return s1 < e2 && e1 > s2;
};

// Static helper: calculate confirmation deadline
// Deadline = 30 minutes after owner acceptance or 15 minutes before booking start time, whichever is earlier.
// Rejects acceptance if resulting deadline is <= current acceptance time.
bookingSchema.statics.calculateConfirmationDeadline = function (ownerAcceptTime, bookingStartTime) {
  const acceptTime = new Date(ownerAcceptTime).getTime();
  const startTime = new Date(bookingStartTime).getTime();

  if (isNaN(acceptTime) || isNaN(startTime)) {
    throw new Error('Invalid dates provided for confirmation deadline calculation');
  }

  const thirtyMinutesAfterAccept = acceptTime + 30 * 60 * 1000;
  const fifteenMinutesBeforeStart = startTime - 15 * 60 * 1000;

  const deadlineMs = Math.min(thirtyMinutesAfterAccept, fifteenMinutesBeforeStart);
  const canAccept = deadlineMs > acceptTime;

  return {
    deadline: new Date(deadlineMs),
    canAccept,
    reason: canAccept ? null : 'Booking start time is too close to allow a valid confirmation window'
  };
};

// Static helper: generate cryptographically secure QR token
bookingSchema.statics.generateQrToken = function () {
  return crypto.randomBytes(24).toString('hex');
};

// Static helper: generate human-readable unique booking code
bookingSchema.statics.generateBookingCode = function () {
  const datePart = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const randomHex = crypto.randomBytes(3).toString('hex').toUpperCase();
  return `PS-${datePart}-${randomHex}`;
};

// Static constants exported on model
bookingSchema.statics.ACTIVE_BLOCKING_STATUSES = ACTIVE_BLOCKING_STATUSES;
bookingSchema.statics.TERMINAL_OR_NON_BLOCKING_STATUSES = TERMINAL_OR_NON_BLOCKING_STATUSES;
bookingSchema.statics.ALL_STATUSES = ALL_STATUSES;

// Compound index for conflict detection (overlap query)
bookingSchema.index({ listingId: 1, status: 1, startTime: 1, endTime: 1 });

// Compound index for driver query sorted by newest first
bookingSchema.index({ driverFirebaseUid: 1, createdAt: -1 });

// Compound index for owner query sorted by newest first
bookingSchema.index({ ownerFirebaseUid: 1, createdAt: -1 });

// Compound index for background expiry sweeps
bookingSchema.index({ status: 1, confirmationDeadline: 1 });
bookingSchema.index({ status: 1, startTime: 1 });

const Booking = mongoose.model('Booking', bookingSchema);

module.exports = Booking;
