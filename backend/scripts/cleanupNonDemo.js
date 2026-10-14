const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const ParkingListing = require('../src/models/ParkingListing');

async function run() {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.error('MONGODB_URI is not set');
    process.exit(1);
  }
  console.log('Connecting to MongoDB Atlas...');
  await mongoose.connect(uri);
  console.log('Connected to MongoDB Atlas');

  const nonDemo = await ParkingListing.find({ isDemoData: { $ne: true } });
  console.log(`Found ${nonDemo.length} non-demo listings:`);
  for (const item of nonDemo) {
    console.log(`- ${item._id} | ${item.title} | owner: ${item.ownerFirebaseUid}`);
  }

  if (nonDemo.length > 0) {
    const res = await ParkingListing.deleteMany({ isDemoData: { $ne: true } });
    console.log(`Deleted ${res.deletedCount} non-demo listings.`);
  }

  const count = await ParkingListing.countDocuments({});
  console.log(`Total listings remaining: ${count}`);

  await mongoose.disconnect();
}

run().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});
