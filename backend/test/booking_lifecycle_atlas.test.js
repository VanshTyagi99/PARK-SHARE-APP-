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
  process.env.PORT = '5059';
  process.env.NODE_ENV = 'test';

  const { admin } = require('../src/config/firebase');
  const { connectDB } = require('../src/config/db');
  const ParkingListing = require('../src/models/ParkingListing');
  const Booking = require('../src/models/Booking');

  // Stub Firebase verifyIdToken for testing valid tokens
  const originalVerify = admin.auth().verifyIdToken;
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'DRIVER_TOKEN') {
      return {
        uid: 'atlas_test_driver_402',
        email: 'atlas.driver@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'OWNER_TOKEN') {
      return {
        uid: 'atlas_test_owner_402',
        email: 'atlas.owner@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'STRANGER_TOKEN') {
      return {
        uid: 'atlas_test_stranger_402',
        email: 'stranger@parkshare.com',
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
  const server = app.listen(5059, '127.0.0.1');

  await new Promise((resolve) => setTimeout(resolve, 600));

  console.log('--- [Category C: Real Atlas Integration Tests] Live Booking Lifecycle Suite ---\n');

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
      ownerFirebaseUid: 'atlas_test_owner_402',
      title: 'Atlas Integration Test Space - Noida Sec 62',
      address: 'Plot 402, Sector 62, Noida, Uttar Pradesh',
      description: 'Temporary listing created solely for automated integration testing.',
      parkingType: 'Car',
      vehicleCompatibility: 'CAR',
      pricePerHour: 50,
      availableFrom: '00:00',
      availableUntil: '23:59',
      isActive: true,
      latitude: 28.6280,
      longitude: 77.3649,
      isDemoData: false
    });
    testListingId = testListing._id.toString();
    console.log(`[Atlas Setup] Created temporary test listing: ${testListingId}\n`);

    // Define Future Test Time Windows (in UTC)
    // Slot 1: Tomorrow 10:00 to 12:00 UTC
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
    const dateStr = tomorrow.toISOString().slice(0, 10);
    const slot1Start = `${dateStr}T10:00:00.000Z`;
    const slot1End = `${dateStr}T12:00:00.000Z`;

    // Overlapping Slot: 11:00 to 13:00 UTC
    const overlapStart = `${dateStr}T11:00:00.000Z`;
    const overlapEnd = `${dateStr}T13:00:00.000Z`;

    // Adjacent Slot: 12:00 to 14:00 UTC (Half-open [S, E) boundary: starts exactly when Slot 1 ends)
    const adjacentStart = `${dateStr}T12:00:00.000Z`;
    const adjacentEnd = `${dateStr}T14:00:00.000Z`;

    // -------------------------------------------------------------
    // Test 1: Driver creates booking request (POST /api/bookings) -> 201
    // -------------------------------------------------------------
    let booking1Id = null;
    const createRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: testListingId,
      startTime: slot1Start,
      endTime: slot1End,
      vehicleNumber: 'DL 01 AB 9999',
      vehicleType: 'Car',
      driverNotes: 'Live Atlas integration test'
    });

    assertEqual('POST /api/bookings returns 201 Created', createRes.status, 201);
    assertTrue('Response contains success flag and booking data', createRes.body.success && !!createRes.body.data);
    assertEqual('Initial status is PENDING', createRes.body.data.status, 'PENDING');
    assertEqual('Authoritative hourlyRateSnapshot matches listing price (50)', createRes.body.data.hourlyRateSnapshot, 50);
    assertEqual('Total hours calculated correctly (2 hours)', createRes.body.data.totalHours, 2);
    assertEqual('Total amount calculated correctly (2 * 50 = 100)', createRes.body.data.totalAmount, 100);

    booking1Id = createRes.body.data._id;
    createdBookingIds.push(booking1Id);

    // -------------------------------------------------------------
    // Test 2: Overlapping booking request returns 409 Conflict
    // -------------------------------------------------------------
    const conflictRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: testListingId,
      startTime: overlapStart,
      endTime: overlapEnd,
      vehicleNumber: 'DL 01 AB 8888',
      vehicleType: 'Car'
    });

    assertEqual('Overlapping reservation returns 409 Conflict', conflictRes.status, 409);
    assertEqual('409 contains conflict error message', conflictRes.body.success, false);
    assertTrue('409 contains conflict details object', !!conflictRes.body.conflict);

    // -------------------------------------------------------------
    // Test 3: Adjacent booking [12:00, 14:00) immediately follows [10:00, 12:00) -> 201 Created
    // -------------------------------------------------------------
    const adjacentRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: testListingId,
      startTime: adjacentStart,
      endTime: adjacentEnd,
      vehicleNumber: 'DL 01 AB 7777',
      vehicleType: 'Car'
    });

    assertEqual('Adjacent half-open reservation [12:00, 14:00) succeeds with 201 Created', adjacentRes.status, 201);
    createdBookingIds.push(adjacentRes.body.data._id);

    // -------------------------------------------------------------
    // Test 4: Owner accepts booking (PATCH /api/bookings/:id/accept) -> 200
    // -------------------------------------------------------------
    const acceptRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}/accept`,
      method: 'PATCH',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });

    assertEqual('Owner accepts booking returns 200 OK', acceptRes.status, 200);
    assertEqual('Booking transitions to AWAITING_DRIVER_CONFIRMATION', acceptRes.body.data.status, 'AWAITING_DRIVER_CONFIRMATION');
    assertTrue('confirmationDeadline is populated', !!acceptRes.body.data.confirmationDeadline);
    assertTrue('ownerRespondedAt is populated', !!acceptRes.body.data.ownerRespondedAt);

    // -------------------------------------------------------------
    // Test 5: Stranger cannot confirm booking -> 403 Forbidden
    // -------------------------------------------------------------
    const strangerConfirmRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}/confirm`,
      method: 'PATCH',
      headers: { Authorization: 'Bearer STRANGER_TOKEN' }
    });
    assertEqual('Non-driver attempting to confirm returns 403 Forbidden', strangerConfirmRes.status, 403);

    // -------------------------------------------------------------
    // Test 6: Driver confirms booking (PATCH /api/bookings/:id/confirm) -> 200
    // -------------------------------------------------------------
    const confirmRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}/confirm`,
      method: 'PATCH',
      headers: { Authorization: 'Bearer DRIVER_TOKEN' }
    });

    assertEqual('Driver confirms booking returns 200 OK', confirmRes.status, 200);
    assertEqual('Booking transitions to CONFIRMED', confirmRes.body.data.status, 'CONFIRMED');
    assertTrue('qrToken generated and returned', typeof confirmRes.body.data.qrToken === 'string');
    assertEqual('qrToken is 48 hex characters', confirmRes.body.data.qrToken.length, 48);

    const generatedQrToken = confirmRes.body.data.qrToken;

    // -------------------------------------------------------------
    // Test 7: Check-in with invalid qrToken returns 400 Bad Request
    // -------------------------------------------------------------
    const badQrRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: 'wrong_fake_token_00000000000000000000000000000000' });
    assertEqual('Check-in with invalid qrToken returns 400', badQrRes.status, 400);

    // -------------------------------------------------------------
    // Test 8: Check-in arrival window validation & execution
    // (To test check-in without waiting 24 hours for tomorrow, we temporarily update startTime to now)
    // -------------------------------------------------------------
    const checkInStartTime = new Date(Date.now() - 5 * 60 * 1000); // 5 min ago
    const checkInEndTime = new Date(Date.now() + 60 * 60 * 1000); // 1 hour from now
    await Booking.updateOne(
      { _id: booking1Id },
      { $set: { startTime: checkInStartTime, endTime: checkInEndTime } }
    );

    const checkInRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: generatedQrToken });

    assertEqual('Check-in with valid qrToken within window returns 200 OK', checkInRes.status, 200);
    assertEqual('Status is CHECKED_IN', checkInRes.body.data.status, 'CHECKED_IN');
    assertTrue('checkedInAt timestamp recorded', !!checkInRes.body.data.checkedInAt);

    // -------------------------------------------------------------
    // Test 9: Check-out releases slot and transitions to COMPLETED
    // -------------------------------------------------------------
    const checkOutRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}/check-out`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: generatedQrToken });

    assertEqual('Check-out with valid qrToken returns 200 OK', checkOutRes.status, 200);
    assertEqual('Status is COMPLETED', checkOutRes.body.data.status, 'COMPLETED');
    assertTrue('completedAt timestamp recorded', !!checkOutRes.body.data.completedAt);
    assertTrue('checkedOutAt timestamp recorded', !!checkOutRes.body.data.checkedOutAt);

    // -------------------------------------------------------------
    // Test 10: Owner Rejection (PATCH /api/bookings/:id/reject)
    // -------------------------------------------------------------
    // Create another pending booking to test rejection
    const rejectTargetRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: testListingId,
      startTime: `${dateStr}T16:00:00.000Z`,
      endTime: `${dateStr}T18:00:00.000Z`,
      vehicleNumber: 'DL 01 AB 6666'
    });
    const rejectBookingId = rejectTargetRes.body.data._id;
    createdBookingIds.push(rejectBookingId);

    const rejectRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${rejectBookingId}/reject`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer OWNER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { rejectionReason: 'Driveway undergoing maintenance' });

    assertEqual('Owner rejects pending booking returns 200 OK', rejectRes.status, 200);
    assertEqual('Status is REJECTED', rejectRes.body.data.status, 'REJECTED');
    assertEqual('rejectionReason is recorded', rejectRes.body.data.rejectionReason, 'Driveway undergoing maintenance');

    // -------------------------------------------------------------
    // Test 11: Driver Cancellation (PATCH /api/bookings/:id/cancel)
    // -------------------------------------------------------------
    // Create another pending booking to test cancellation
    const cancelTargetRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: testListingId,
      startTime: `${dateStr}T19:00:00.000Z`,
      endTime: `${dateStr}T21:00:00.000Z`,
      vehicleNumber: 'DL 01 AB 5555'
    });
    const cancelBookingId = cancelTargetRes.body.data._id;
    createdBookingIds.push(cancelBookingId);

    const cancelRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${cancelBookingId}/cancel`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { cancellationReason: 'Plans changed' });

    assertEqual('Driver cancels booking returns 200 OK', cancelRes.status, 200);
    assertEqual('Status is CANCELLED', cancelRes.body.data.status, 'CANCELLED');
    assertEqual('cancelledBy is recorded as driver', cancelRes.body.data.cancelledBy, 'driver');

    // -------------------------------------------------------------
    // Test 12: GET /api/bookings/my-bookings returns driver's bookings
    // -------------------------------------------------------------
    const myListRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings/my-bookings',
      method: 'GET',
      headers: { Authorization: 'Bearer DRIVER_TOKEN' }
    });
    assertEqual('GET /api/bookings/my-bookings returns 200 OK', myListRes.status, 200);
    assertTrue('Returns list of driver bookings', Array.isArray(myListRes.body.data) && myListRes.body.data.length >= 3);

    // -------------------------------------------------------------
    // Test 13: GET /api/bookings/owner-requests returns owner requests (without exposing qrToken)
    // -------------------------------------------------------------
    const ownerListRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings/owner-requests',
      method: 'GET',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('GET /api/bookings/owner-requests returns 200 OK', ownerListRes.status, 200);
    assertTrue('Returns owner requests list', Array.isArray(ownerListRes.body.data));
    const hasExposedQr = ownerListRes.body.data.some(b => !!b.qrToken);
    assertEqual('Owner request list never leaks qrToken', hasExposedQr, false);

    // -------------------------------------------------------------
    // Test 14: GET /api/bookings/:id Participant Authorization
    // -------------------------------------------------------------
    const strangerGetRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}`,
      method: 'GET',
      headers: { Authorization: 'Bearer STRANGER_TOKEN' }
    });
    assertEqual('Stranger attempting to view booking returns 403 Forbidden', strangerGetRes.status, 403);

    const driverGetRes = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/bookings/${booking1Id}`,
      method: 'GET',
      headers: { Authorization: 'Bearer DRIVER_TOKEN' }
    });
    assertEqual('Driver viewing booking details returns 200 OK', driverGetRes.status, 200);

  } catch (testErr) {
    console.error('✗ Test execution error:', testErr);
    failed++;
  } finally {
    // Cleanup: Delete ONLY test bookings and the test listing created in this suite
    console.log('\n[Atlas Cleanup] Cleaning up test records...');
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
  console.log(`Atlas Lifecycle Integration Test Results: ${passed} passed, ${failed} failed`);
  console.log('========================================\n');

  admin.auth().verifyIdToken = originalVerify;

  server.close(() => {
    process.exit(failed > 0 ? 1 : 0);
  });
};

runTests();
