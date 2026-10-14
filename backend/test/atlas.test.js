const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const mongoose = require('mongoose');

const runAtlasVerification = async () => {
  console.log('--- Running Live MongoDB Atlas Integration Verification ---');

  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.error('✗ FAIL: MONGODB_URI is not set in backend/.env');
    process.exit(1);
  }

  try {
    const conn = await mongoose.connect(uri, {
      serverSelectionTimeoutMS: 5000
    });

    console.log(`✓ PASS: Connected to MongoDB Atlas cluster at host: ${conn.connection.host}`);
    console.log(`✓ PASS: Target database confirmed as: '${conn.connection.name}'`);
    console.log(`✓ PASS: Mongoose connection readyState is: ${conn.connection.readyState} (1 = connected)`);

    // Ping check
    const adminDb = conn.connection.db.admin();
    const pingRes = await adminDb.ping();
    if (pingRes && pingRes.ok === 1) {
      console.log('✓ PASS: MongoDB database ping returned ok: 1');
    } else {
      console.error('✗ FAIL: Ping response did not return ok: 1', pingRes);
      process.exit(1);
    }

    // Harmless collection listing check
    const collections = await conn.connection.db.listCollections().toArray();
    console.log(`✓ PASS: Connected to database '${conn.connection.name}'. Existing collections: [${collections.map(c => c.name).join(', ')}]`);

    await mongoose.disconnect();
    console.log('✓ PASS: Clean disconnect from MongoDB Atlas.');
    console.log('\n--- Atlas Verification Result: ALL CHECKS PASSED ---\n');
    process.exit(0);
  } catch (err) {
    const sanitizedMsg = err.message.replace(/:\/\/([^:]+):([^@]+)@/g, '://<credentials>@');
    console.error('✗ FAIL: MongoDB Atlas verification failed:', sanitizedMsg);
    process.exit(1);
  }
};

runAtlasVerification();
