const mongoose = require('mongoose');

const parkingListingSchema = new mongoose.Schema(
  {
    ownerFirebaseUid: {
      type: String,
      required: [true, 'Owner ID is required'],
      index: true,
      trim: true
    },
    title: {
      type: String,
      required: [true, 'Parking title is required'],
      trim: true,
      minlength: [3, 'Title must be at least 3 characters'],
      maxlength: [100, 'Title cannot exceed 100 characters']
    },
    address: {
      type: String,
      required: [true, 'Parking address is required'],
      trim: true,
      minlength: [3, 'Address must be at least 3 characters'],
      maxlength: [300, 'Address cannot exceed 300 characters']
    },
    description: {
      type: String,
      default: '',
      trim: true,
      maxlength: [1000, 'Description cannot exceed 1000 characters']
    },
    parkingType: {
      type: String,
      enum: {
        values: ['Car', 'Bike', 'Car + Bike'],
        message: '{VALUE} is not a valid parking type'
      },
      default: 'Car'
    },
    vehicleCompatibility: {
      type: String,
      enum: {
        values: ['CAR', 'BIKE', 'CAR_AND_BIKE'],
        message: '{VALUE} is not a valid vehicle compatibility option'
      },
      default: 'CAR'
    },
    pricePerHour: {
      type: Number,
      required: [true, 'Price per hour is required'],
      min: [1, 'Price per hour must be greater than zero']
    },
    availableFrom: {
      type: String,
      required: [true, 'Available from time is required'],
      trim: true
    },
    availableUntil: {
      type: String,
      required: [true, 'Available until time is required'],
      trim: true
    },
    photos: {
      type: [String],
      default: [],
      validate: [
        function (val) {
          return val.length <= 5;
        },
        'A listing can have at most 5 photos'
      ]
    },
    isActive: {
      type: Boolean,
      default: true,
      index: true
    },
    latitude: {
      type: Number,
      min: -90,
      max: 90,
      default: null
    },
    longitude: {
      type: Number,
      min: -180,
      max: 180,
      default: null
    },
    location: {
      type: {
        type: String,
        enum: ['Point'],
        default: 'Point'
      },
      coordinates: {
        type: [Number], // [longitude, latitude]
        default: undefined
      }
    },
    isDemoData: {
      type: Boolean,
      default: false,
      index: true
    },
    bookingLockVersion: {
      type: Number,
      default: 0
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

// Pre-save hook: keep location GeoJSON synchronized with latitude and longitude
parkingListingSchema.pre('save', function (next) {
  if (
    this.latitude != null &&
    this.longitude != null &&
    !isNaN(this.latitude) &&
    !isNaN(this.longitude) &&
    (this.latitude !== 0 || this.longitude !== 0)
  ) {
    this.location = {
      type: 'Point',
      coordinates: [Number(this.longitude), Number(this.latitude)]
    };
  }
  next();
});

// Compound index for owner queries sorted by creation date
parkingListingSchema.index({ ownerFirebaseUid: 1, createdAt: -1 });

// Geospatial 2dsphere index for radius/proximity queries
parkingListingSchema.index({ location: '2dsphere' }, { sparse: true });

// Compound index for active listings filtering and price sorting
parkingListingSchema.index({ isActive: 1, parkingType: 1, pricePerHour: 1 });

const ParkingListing = mongoose.model('ParkingListing', parkingListingSchema);

module.exports = ParkingListing;
