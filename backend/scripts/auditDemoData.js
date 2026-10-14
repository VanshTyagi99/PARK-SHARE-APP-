const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const ParkingListing = require('../src/models/ParkingListing');
const User = require('../src/models/User');

async function audit() {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.error("MONGODB_URI missing");
    process.exit(1);
  }
  await mongoose.connect(uri);
  console.log('✓ Connected to MongoDB Atlas');

  const totalDemo = await ParkingListing.countDocuments({ isDemoData: true });
  const totalListings = await ParkingListing.countDocuments({});
  const activeListings = await ParkingListing.countDocuments({ isActive: true });
  const inactiveListings = await ParkingListing.countDocuments({ isActive: false });

  const validLatLng = await ParkingListing.countDocuments({
    latitude: { $ne: null, $type: 'number', $gte: -90, $lte: 90 },
    longitude: { $ne: null, $type: 'number', $gte: -180, $lte: 180 }
  });

  const allListings = await ParkingListing.find({}).lean();
  let correctGeoJson = 0;
  let validVehicleCompat = 0;
  let validPrice = 0;
  let validOwnerRef = 0;

  for (const item of allListings) {
    if (item.location && Array.isArray(item.location.coordinates) && item.location.coordinates.length === 2) {
      const [lng, lat] = item.location.coordinates;
      if (lng === item.longitude && lat === item.latitude) {
        correctGeoJson++;
      }
    }
    if (['CAR', 'BIKE', 'CAR_AND_BIKE'].includes(item.vehicleCompatibility)) {
      validVehicleCompat++;
    }
    if (typeof item.pricePerHour === 'number' && item.pricePerHour > 0) {
      validPrice++;
    }
    if (item.ownerFirebaseUid && typeof item.ownerFirebaseUid === 'string' && item.ownerFirebaseUid.length > 0) {
      validOwnerRef++;
    }
  }

  const demoOwner = await User.findOne({ firebaseUid: 'demo_partner_ncr_01' });

  console.log('--- AUDIT METRICS ---');
  console.log(JSON.stringify({
    totalDemo,
    totalListings,
    activeListings,
    inactiveListings,
    validLatLng,
    correctGeoJson,
    validVehicleCompat,
    validPrice,
    validOwnerRef,
    demoOwnerExists: !!demoOwner,
    demoOwnerEmail: demoOwner?.email,
    demoOwnerListingCount: demoOwner?.listingCount
  }, null, 2));

  await mongoose.disconnect();
}

audit().catch((err) => {
  console.error("Audit error:", err);
  process.exit(1);
});
