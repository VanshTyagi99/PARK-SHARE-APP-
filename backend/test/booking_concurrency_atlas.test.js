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

const runConcurrencyTests = async () => {
  process.env.PORT = '5060';
  process.env.NODE_ENV = 'test';

  const { admin } = require('../src/config/firebase');
  const { connectDB } = require('../src/config/db');
  const ParkingListing = require('../src/models/ParkingListing');
  const Booking = require('../src/models/Booking');

  // Stub Firebase verifyIdToken for testing valid tokens
  const originalVerify = admin.auth().verifyIdToken;
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'DRIVER_A_TOKEN') {
      return {
        uid: 'atlas_concurrent_driver_A',
        email: 'driverA@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'DRIVER_B_TOKEN') {
      return {
        uid: 'atlas_concurrent_driver_B',
        email: 'driverB@parkshare.com',
        email_verified: true
      };
    }
    const err = new Error('Invalid token');
    err.code = 'auth/invalid-id-token';
    throw err;
  };

  // Connect to live Atlas cluster
  await connectDB();

  const app = require('../src/server');
  const server = app.listen(5060, '127.0.0.1');

  await new Promise((resolve) => setTimeout(resolve, 600));

  console.log('--- [Category D: Real Concurrency Tests] Live Atlas Double-Booking Stress Test ---\n');

  let passed = 0;
  let failed = 0;

  const createdBookingIds = [];
  let testListingId = null;

  const assertEqual = (name, actual, expected, message = '') => {
    if (actual === expected) {
      console.log(`✓ PASS: ${name}`);
      passed++;
    } else {
      console.error(`✗ FAIL: ${name} (Expected: ${expected}, Got: ${actual}) ${message}`);
      failed++;
    }
  };

  const assertTrue = (name, condition, message = '') => {
    if (condition) {
      console.log(`✓ PASS: ${name}`);
      passed++;
    } else {
      console.error(`✗ FAIL: ${name} ${message}`);
      failed++;
    }
  };

  try {
    // Setup: Create an isolated test parking listing in MongoDB Atlas
    const testListing = await ParkingListing.create({
      ownerFirebaseUid: 'atlas_concurrency_owner',
      title: 'Atlas Concurrency Stress Space - Noida Expressway',
      address: 'Expressway Tower C, Sector 135, Noida',
      description: 'Temporary listing created solely for concurrency stress testing.',
      parkingType: 'Car',
      vehicleCompatibility: 'CAR',
      pricePerHour: 60,
      availableFrom: '00:00',
      availableUntil: '23:59',
      isActive: true,
      latitude: 28.5000,
      longitude: 77.4000,
      isDemoData: false
    });
    testListingId = testListing._id.toString();
    console.log(`[Atlas Setup] Created temporary listing for concurrency test: ${testListingId}\n`);

    const dayAfterTomorrow = new Date(Date.now() + 48 * 60 * 60 * 1000);
    const dateStr = dayAfterTomorrow.toISOString().slice(0, 10);

    // =============================================================
    // Stress Test 1: Simultaneous Overlapping Requests (10:00-13:00 vs 11:00-14:00)
    // =============================================================
    console.log('[Test 1: Simultaneous Overlapping Requests]');
    const req1Payload = {
      listingId: testListingId,
      startTime: `${dateStr}T10:00:00.000Z`,
      endTime: `${dateStr}T13:00:00.000Z`,
      vehicleNumber: 'DL 01 AB 1111',
      vehicleType: 'Car'
    };

    const req2Payload = {
      listingId: testListingId,
      startTime: `${dateStr}T11:00:00.000Z`,
      endTime: `${dateStr}T14:00:00.000Z`,
      vehicleNumber: 'DL 01 AB 2222',
      vehicleType: 'Car'
    };

    console.log('Dispatching 2 simultaneous overlapping booking requests to /api/bookings...');
    const [resA, resB] = await Promise.all([
      request({
        hostname: '127.0.0.1',
        port: 5060,
        path: '/api/bookings',
        method: 'POST',
        headers: {
          Authorization: 'Bearer DRIVER_A_TOKEN',
          'Content-Type': 'application/json'
        }
      }, req1Payload),
      request({
        hostname: '127.0.0.1',
        port: 5060,
        path: '/api/bookings',
        method: 'POST',
        headers: {
          Authorization: 'Bearer DRIVER_B_TOKEN',
          'Content-Type': 'application/json'
        }
      }, req2Payload)
    ]);

    const statuses = [resA.status, resB.status];
    console.log(`Received concurrent HTTP responses: [${resA.status}, ${resB.status}]`);

    const has201 = statuses.includes(201);
    const has409 = statuses.includes(409);

    assertTrue('Exactly one concurrent request succeeded with 201 Created', has201);
    assertTrue('Competing concurrent request was rejected with 409 Conflict', has409);
    assertEqual('Total responses count is 2', statuses.length, 2);

    // Track winning booking for cleanup
    if (resA.status === 201 && resA.body.data && resA.body.data._id) {
      createdBookingIds.push(resA.body.data._id);
    }
    if (resB.status === 201 && resB.body.data && resB.body.data._id) {
      createdBookingIds.push(resB.body.data._id);
    }

    // Verify in MongoDB Atlas that exactly 1 booking was written for this window
    const atlasBookingsForWindow = await Booking.find({
      listingId: testListingId,
      status: { $in: Booking.ACTIVE_BLOCKING_STATUSES },
      startTime: { $lt: new Date(`${dateStr}T14:00:00.000Z`) },
      endTime: { $gt: new Date(`${dateStr}T10:00:00.000Z`) }
    });

    assertEqual('MongoDB Atlas contains exactly 1 active blocking reservation for overlapping window', atlasBookingsForWindow.length, 1);

    // Verify parent listing bookingLockVersion was incremented by the committed transaction (aborted transaction writes roll back)
    const updatedListing = await ParkingListing.findById(testListingId);
    assertTrue('ParkingListing bookingLockVersion was atomically incremented during concurrency', updatedListing.bookingLockVersion >= 1);

    // =============================================================
    // Stress Test 2: Simultaneous Adjacent Non-Overlapping Requests [14:00, 16:00) & [16:00, 18:00)
    // =============================================================
    console.log('\n[Test 2: Simultaneous Adjacent Non-Overlapping Requests]');
    const adjPayload1 = {
      listingId: testListingId,
      startTime: `${dateStr}T14:00:00.000Z`,
      endTime: `${dateStr}T16:00:00.000Z`,
      vehicleNumber: 'DL 01 AB 3333'
    };

    const adjPayload2 = {
      listingId: testListingId,
      startTime: `${dateStr}T16:00:00.000Z`,
      endTime: `${dateStr}T18:00:00.000Z`,
      vehicleNumber: 'DL 01 AB 4444'
    };

    console.log('Dispatching 2 simultaneous adjacent booking requests to /api/bookings...');
    const [resC, resD] = await Promise.all([
      request({
        hostname: '127.0.0.1',
        port: 5060,
        path: '/api/bookings',
        method: 'POST',
        headers: {
          Authorization: 'Bearer DRIVER_A_TOKEN',
          'Content-Type': 'application/json'
        }
      }, adjPayload1),
      request({
        hostname: '127.0.0.1',
        port: 5060,
        path: '/api/bookings',
        method: 'POST',
        headers: {
          Authorization: 'Bearer DRIVER_B_TOKEN',
          'Content-Type': 'application/json'
        }
      }, adjPayload2)
    ]);

    console.log(`Received adjacent concurrent HTTP responses: [${resC.status}, ${resD.status}]`);
    assertEqual('First adjacent request succeeded with 201 Created', resC.status, 201);
    assertEqual('Second adjacent request succeeded with 201 Created', resD.status, 201);

    if (resC.status === 201 && resC.body.data) createdBookingIds.push(resC.body.data._id);
    if (resD.status === 201 && resD.body.data) createdBookingIds.push(resD.body.data._id);

    // Verify both adjacent bookings exist in MongoDB Atlas
    const adjacentBookingsInAtlas = await Booking.find({
      _id: { $in: [resC.body.data._id, resD.body.data._id] }
    });
    assertEqual('Both adjacent bookings safely exist in MongoDB Atlas', adjacentBookingsInAtlas.length, 2);

  } catch (err) {
    console.error('✗ Concurrency test error:', err);
    failed++;
  } finally {
    // Cleanup: Delete ONLY test bookings and the test listing created in this suite
    console.log('\n[Atlas Cleanup] Cleaning up concurrency test records...');
    if (createdBookingIds.length > 0) {
      const deleteResult = await Booking.deleteMany({ _id: { $in: createdBookingIds } });
      console.log(`✓ Removed ${deleteResult.deletedCount} temporary test booking records.`);
    }
    if (testListingId) {
      await ParkingListing.deleteOne({ _id: testListingId });
      console.log(`✓ Removed temporary test listing ${testListingId}.`);
    }
  }

  console.log('\n========================================');
  console.log(`Atlas Concurrency Stress Test Results: ${passed} passed, ${failed} failed`);
  console.log('========================================\n');

  admin.auth().verifyIdToken = originalVerify;

  server.close(() => {
    process.exit(failed > 0 ? 1 : 0);
  });
};

runConcurrencyTests();
