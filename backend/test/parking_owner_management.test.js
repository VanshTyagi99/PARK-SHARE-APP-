const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const http = require('http');
const mongoose = require('mongoose');

// Helper to make local HTTP requests
const request = (options, postData = null) => {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve({ status: res.statusCode, headers: res.headers, body: json });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, body: data });
        }
      });
    });

    req.on('error', reject);

    if (postData) {
      req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
    }
    req.end();
  });
};

const runTests = async () => {
  process.env.PORT = '5057';
  process.env.NODE_ENV = 'test';

  const { admin } = require('../src/config/firebase');
  const db = require('../src/config/db');

  // Stub Firebase verifyIdToken for testing
  const originalVerify = admin.auth().verifyIdToken;
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'OWNER_TOKEN') {
      return {
        uid: 'test_owner_uid_priority2',
        email: 'owner_p2@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'OTHER_OWNER_TOKEN') {
      return {
        uid: 'other_owner_uid_priority2',
        email: 'other_p2@parkshare.com',
        email_verified: true
      };
    }
    const err = new Error('Firebase ID token has invalid signature');
    err.code = 'auth/invalid-id-token';
    throw err;
  };

  const { connectDB } = require('../src/config/db');
  await connectDB();

  const app = require('../src/server');
  const server = app.listen(5057, '127.0.0.1');

  // Wait for server and DB connection
  await new Promise((resolve) => setTimeout(resolve, 500));

  console.log('--- Running Priority 2: Parking Owner Management Test Suite ---');

  let passed = 0;
  let failed = 0;

  const assertEqual = (testName, actual, expected, detail = '') => {
    if (actual === expected) {
      console.log(`✓ PASS: ${testName} ${detail ? '(' + detail + ')' : ''}`);
      passed++;
    } else {
      console.error(`✗ FAIL: ${testName} - Expected ${expected}, got ${actual} ${detail}`);
      failed++;
    }
  };

  const ParkingListing = require('../src/models/ParkingListing');
  const Booking = require('../src/models/Booking');
  const User = require('../src/models/User');

  const OWNER_UID = 'test_owner_uid_priority2';
  const OTHER_UID = 'other_owner_uid_priority2';

  let testListingId = null;
  let activeBookingId = null;

  try {
    // Clean up any previous test artifacts for this test UID
    await ParkingListing.deleteMany({ ownerFirebaseUid: { $in: [OWNER_UID, OTHER_UID] } });
    await Booking.deleteMany({ driverFirebaseUid: { $in: [OWNER_UID, OTHER_UID] } });
    await User.deleteMany({ firebaseUid: { $in: [OWNER_UID, OTHER_UID] } });

    // Seed test users as verified owners
    await User.create({ firebaseUid: OWNER_UID, email: 'owner@test.com', verificationStatus: 'APPROVED' });
    await User.create({ firebaseUid: OTHER_UID, email: 'other@test.com', verificationStatus: 'APPROVED' });

    console.log('\n[Group 1: DELETE /api/parking/:id Authentication & Validation]');

    // Test 1: DELETE without token -> 401
    const res1 = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/507f1f77bcf86cd799439011',
      method: 'DELETE'
    });
    assertEqual('DELETE without token returns 401', res1.status, 401);

    // Test 2: DELETE with invalid ObjectId format -> 400
    const res2 = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/invalid-mongo-id-xyz',
      method: 'DELETE',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('DELETE with invalid ObjectId returns 400', res2.status, 400);

    // Test 3: DELETE non-existent listing -> 404
    const res3 = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/507f1f77bcf86cd799439011',
      method: 'DELETE',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('DELETE non-existent listing returns 404', res3.status, 404);

    console.log('\n[Group 2: Creation, maxLimit in my-listings & Ownership Guard]');

    // Test 4: Create listing as OWNER_UID
    const createRes = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer OWNER_TOKEN'
      }
    }, {
      title: 'Priority 2 Test Space',
      address: '123 Tech Park, Sector 62, Noida',
      description: 'Test space with photo support',
      parkingType: 'Car',
      pricePerHour: 40,
      availableFrom: '08:00',
      availableUntil: '20:00',
      photos: ['data:image/jpeg;base64,dGVzdHBob3Rv']
    });
    assertEqual('POST /api/parking creates listing (201)', createRes.status, 201);
    testListingId = createRes.body.data._id;

    // Test 5: Verify GET /api/parking/my-listings returns maxLimit: 10
    const myListingsRes = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/my-listings',
      method: 'GET',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('GET /my-listings returns 200', myListingsRes.status, 200);
    assertEqual('GET /my-listings response has maxLimit: 10', myListingsRes.body.maxLimit, 10);
    assertEqual('GET /my-listings has count: 1', myListingsRes.body.count, 1);
    assertEqual('Listing contains photo array', myListingsRes.body.data[0].photos.length, 1);

    // Test 6: Attempt DELETE as OTHER_OWNER_TOKEN -> 403 Forbidden
    const unauthDeleteRes = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: `/api/parking/${testListingId}`,
      method: 'DELETE',
      headers: { Authorization: 'Bearer OTHER_OWNER_TOKEN' }
    });
    assertEqual('DELETE as non-owner rejects with 403 Forbidden', unauthDeleteRes.status, 403);

    console.log('\n[Group 3: Reservation-Safety Guard on Deletion]');

    // Create an active blocking booking on testListingId
    const activeBooking = await Booking.create({
      listingId: testListingId,
      listingTitle: 'Priority 2 Test Space',
      listingAddress: '123 Tech Park, Sector 62, Noida',
      driverFirebaseUid: OTHER_UID,
      ownerFirebaseUid: OWNER_UID,
      startTime: new Date(Date.now() + 3600000).toISOString(),
      endTime: new Date(Date.now() + 7200000).toISOString(),
      totalHours: 1,
      hourlyRateSnapshot: 40,
      totalAmount: 40,
      status: 'CONFIRMED'
    });
    activeBookingId = activeBooking._id;

    // Test 7: DELETE listing with active CONFIRMED booking -> 400 Bad Request
    const blockedDeleteRes = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: `/api/parking/${testListingId}`,
      method: 'DELETE',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('DELETE listing with active booking returns 400', blockedDeleteRes.status, 400);
    assertEqual(
      'Rejection message mentions active reservations',
      blockedDeleteRes.body.message.includes('active or upcoming reservations'),
      true
    );

    // Test 8: Set booking status to CANCELLED (non-blocking) and retry DELETE -> 200 OK
    activeBooking.status = 'CANCELLED';
    await activeBooking.save();

    const allowedDeleteRes = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: `/api/parking/${testListingId}`,
      method: 'DELETE',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('DELETE listing after booking cancelled returns 200', allowedDeleteRes.status, 200);

    // Verify listing is removed from database
    const checkDeleted = await ParkingListing.findById(testListingId);
    assertEqual('Listing document is removed from MongoDB', checkDeleted, null);

    // Verify user listingCount was decremented atomically to 0
    const ownerUser = await User.findOne({ firebaseUid: OWNER_UID });
    assertEqual('Owner listingCount decremented to 0', ownerUser.listingCount, 0);

    console.log('\n[Group 4: Maximum 10 Listings Limit Enforcement]');

    // Seed 10 listings for OWNER_UID
    const createdIds = [];
    for (let i = 1; i <= 10; i++) {
      const listing = await ParkingListing.create({
        ownerFirebaseUid: OWNER_UID,
        title: `Capacity Space ${i}`,
        address: `Sector ${i}, Noida`,
        parkingType: 'Car',
        vehicleCompatibility: 'CAR',
        pricePerHour: 30,
        availableFrom: '09:00',
        availableUntil: '18:00',
        isActive: true
      });
      createdIds.push(listing._id);
    }
    await User.updateOne({ firebaseUid: OWNER_UID }, { listingCount: 10 });

    // Test 9: Attempt creating 11th listing -> 400 Limit Reached
    const eleventhRes = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer OWNER_TOKEN'
      }
    }, {
      title: 'Eleventh Overflow Space',
      address: 'Should Fail Address',
      parkingType: 'Car',
      pricePerHour: 50,
      availableFrom: '08:00',
      availableUntil: '20:00'
    });
    assertEqual('POST 11th listing returns 400 Limit Reached', eleventhRes.status, 400);
    assertEqual(
      'Message warns about 10-listing limit',
      eleventhRes.body.message.includes('at most 10 parking listings'),
      true
    );

    // Clean up the 10 test listings
    await ParkingListing.deleteMany({ _id: { $in: createdIds } });
    await Booking.deleteOne({ _id: activeBookingId });
    await User.deleteOne({ firebaseUid: OWNER_UID });

    console.log('\n[Group 5: Demo Data Integrity Check]');
    // Test 10: Verify the 20 seeded demo listings are intact
    const totalDemoListings = await ParkingListing.countDocuments();
    assertEqual('Atlas demo listings preserved (expected >= 20)', totalDemoListings >= 20, true, `actual: ${totalDemoListings}`);

  } catch (err) {
    console.error('Unhandled test exception:', err);
    failed++;
  } finally {
    // Restore original verifyIdToken
    admin.auth().verifyIdToken = originalVerify;
    server.close();
  }

  console.log('\n========================================');
  console.log(`Priority 2 Test Results: ${passed} passed, ${failed} failed`);
  console.log('========================================\n');

  if (failed > 0) {
    process.exit(1);
  } else {
    process.exit(0);
  }
};

runTests();
