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
  process.env.PORT = '5060';
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
        uid: 'checkin_driver_uid_405',
        email: 'checkin.driver@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'OWNER_TOKEN') {
      return {
        uid: 'checkin_owner_uid_405',
        email: 'checkin.owner@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'STRANGER_TOKEN') {
      return {
        uid: 'stranger_uid_405',
        email: 'stranger@parkshare.com',
        email_verified: true
      };
    }
    throw new Error('Invalid Firebase Token');
  };

  await connectDB();

  const app = require('../src/server');
  const server = http.createServer(app);

  await new Promise((resolve) => server.listen(5060, resolve));

  let passed = 0;
  let failed = 0;

  const assertEqual = (desc, actual, expected) => {
    if (actual === expected) {
      console.log(`✓ PASS: ${desc}`);
      passed++;
    } else {
      console.error(`✗ FAIL: ${desc} -> Expected "${expected}", got "${actual}"`);
      failed++;
    }
  };

  const assertTrue = (desc, condition) => {
    if (condition) {
      console.log(`✓ PASS: ${desc}`);
      passed++;
    } else {
      console.error(`✗ FAIL: ${desc} -> Condition was false`);
      failed++;
    }
  };

  console.log('--- [Sub-Phase 4.5 Focused Tests: Check-In & Check-Out Lifecycle Suite] ---\n');

  let testListingId;
  const createdBookingIds = [];

  try {
    // -------------------------------------------------------------
    // Setup: Create a temporary active listing owned by OWNER_TOKEN
    // -------------------------------------------------------------
    const testListing = await ParkingListing.create({
      ownerFirebaseUid: 'checkin_owner_uid_405',
      title: 'Sector 62 IT Park Covered Spot',
      address: 'Core-4, Sector 62, Noida',
      description: 'Covered spot for test suite 4.5',
      parkingType: 'Car',
      vehicleCompatibility: 'CAR',
      pricePerHour: 40,
      availableFrom: '00:00',
      availableUntil: '23:59',
      isActive: true,
      latitude: 28.6280,
      longitude: 77.3649
    });
    testListingId = testListing._id.toString();
    console.log(`[Atlas Setup] Created temporary test listing: ${testListingId}\n`);

    // Helper to create a confirmed booking with a valid QR token
    const createConfirmedBooking = async (startOffsetMinutes, durationHours) => {
      const now = Date.now();
      const startTime = new Date(now + startOffsetMinutes * 60 * 1000);
      const endTime = new Date(startTime.getTime() + durationHours * 60 * 60 * 1000);

      const qrToken = Booking.generateQrToken();
      const booking = await Booking.create({
        listingId: testListingId,
        listingTitle: 'Sector 62 IT Park Covered Spot',
        listingAddress: 'Core-4, Sector 62, Noida',
        driverFirebaseUid: 'checkin_driver_uid_405',
        ownerFirebaseUid: 'checkin_owner_uid_405',
        startTime,
        endTime,
        totalHours: durationHours,
        hourlyRateSnapshot: 40,
        totalAmount: durationHours * 40,
        status: 'CONFIRMED',
        qrToken,
        driverConfirmedAt: new Date()
      });
      createdBookingIds.push(booking._id.toString());
      return { booking, qrToken, startTime, endTime };
    };

    // =============================================================
    // Test 1: Valid check-in by Driver (and verified by Owner)
    // =============================================================
    console.log('[Scenario 1: Valid Check-In]');
    const b1 = await createConfirmedBooking(0, 2); // starts right now
    const checkInRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b1.booking._id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b1.qrToken });

    assertEqual('Valid check-in returns 200 OK', checkInRes.status, 200);
    assertEqual('Booking status is CHECKED_IN', checkInRes.body.data.status, 'CHECKED_IN');
    assertTrue('checkedInAt timestamp is populated', !!checkInRes.body.data.checkedInAt);

    // =============================================================
    // Test 2: Invalid QR token rejection
    // =============================================================
    console.log('\n[Scenario 2: Invalid QR Token Rejection]');
    const b2 = await createConfirmedBooking(0, 2);
    const badTokenRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b2.booking._id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: 'wrong_fake_token_12345678901234567890123456789012' });

    assertEqual('Invalid QR token returns 400 Bad Request', badTokenRes.status, 400);
    assertEqual('Error message indicates invalid QR token', badTokenRes.body.message, 'Invalid QR verification token');

    // =============================================================
    // Test 3: Unauthorized user (Stranger attempting check-in/out)
    // =============================================================
    console.log('\n[Scenario 3: Unauthorized User Rejection]');
    const strangerRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b2.booking._id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer STRANGER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b2.qrToken });

    assertEqual('Stranger attempting check-in returns 403 Forbidden', strangerRes.status, 403);
    assertEqual('Error indicates unauthorized user', strangerRes.body.message, 'Unauthorized to verify check-in for this booking');

    // =============================================================
    // Test 4: Cancelled or Expired booking cannot check in
    // =============================================================
    console.log('\n[Scenario 4: Cancelled / Expired Booking Rejection]');
    const b4 = await createConfirmedBooking(0, 2);
    await Booking.updateOne({ _id: b4.booking._id }, { $set: { status: 'CANCELLED', cancellationReason: 'Test cancellation' } });

    const cancelledCheckInRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b4.booking._id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b4.qrToken });

    assertEqual('Cancelled booking check-in returns 400 Bad Request', cancelledCheckInRes.status, 400);
    assertEqual('Error states booking status is CANCELLED', cancelledCheckInRes.body.message, 'Cannot check in. Booking status is CANCELLED');

    // =============================================================
    // Test 5: Duplicate check-in prevention
    // =============================================================
    console.log('\n[Scenario 5: Duplicate Check-In Prevention]');
    const duplicateCheckInRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b1.booking._id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b1.qrToken });

    assertEqual('Duplicate check-in returns 400 Bad Request', duplicateCheckInRes.status, 400);
    assertEqual('Error message indicates already checked in', duplicateCheckInRes.body.message, 'Booking has already been checked in');

    // =============================================================
    // Test 6: Check-in outside permitted time window
    // =============================================================
    console.log('\n[Scenario 6: Check-In Outside Permitted Time Window]');
    // 6a: Too early (slot starts 30 minutes from now, arrival permitted only 15m before)
    const b6Early = await createConfirmedBooking(30, 2);
    const earlyRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b6Early.booking._id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b6Early.qrToken });

    assertEqual('Too early check-in returns 400 Bad Request', earlyRes.status, 400);
    assertEqual('Error states arrival allowed 15m before', earlyRes.body.message, 'Check-in is permitted starting 15 minutes before booking start time');

    // 6b: Too late (slot ended 10 minutes ago)
    const b6Late = await createConfirmedBooking(-130, 2); // started 130 min ago, ended 10 min ago
    const lateRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b6Late.booking._id}/check-in`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b6Late.qrToken });

    assertEqual('Too late check-in returns 400 Bad Request', lateRes.status, 400);
    assertEqual('Error states booking slot has elapsed', lateRes.body.message, 'Booking slot has already elapsed. Check-in is no longer allowed.');

    // =============================================================
    // Test 7: Valid Check-Out (by Owner or Driver)
    // =============================================================
    console.log('\n[Scenario 7: Valid Check-Out]');
    const checkOutRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b1.booking._id}/check-out`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer OWNER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b1.qrToken });

    assertEqual('Valid check-out returns 200 OK', checkOutRes.status, 200);
    assertEqual('Status transitions to COMPLETED', checkOutRes.body.data.status, 'COMPLETED');
    assertTrue('checkedOutAt is populated', !!checkOutRes.body.data.checkedOutAt);
    assertTrue('completedAt is populated', !!checkOutRes.body.data.completedAt);

    // =============================================================
    // Test 8: Duplicate Check-Out Prevention
    // =============================================================
    console.log('\n[Scenario 8: Duplicate Check-Out Prevention]');
    const duplicateCheckOutRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b1.booking._id}/check-out`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b1.qrToken });

    assertEqual('Duplicate check-out returns 400 Bad Request', duplicateCheckOutRes.status, 400);
    assertEqual('Error indicates status is already COMPLETED', duplicateCheckOutRes.body.message, 'Cannot check out. Booking status is COMPLETED');

    // =============================================================
    // Test 9: Invalid Status Transition (Check-Out before Check-In)
    // =============================================================
    console.log('\n[Scenario 9: Invalid Status Transition (Check-Out on CONFIRMED)]');
    const b9 = await createConfirmedBooking(0, 2);
    const prematureCheckOutRes = await request({
      hostname: '127.0.0.1',
      port: 5060,
      path: `/api/bookings/${b9.booking._id}/check-out`,
      method: 'POST',
      headers: {
        Authorization: 'Bearer DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { qrToken: b9.qrToken });

    assertEqual('Check-out on CONFIRMED returns 400 Bad Request', prematureCheckOutRes.status, 400);
    assertEqual('Error message indicates status must be CHECKED_IN', prematureCheckOutRes.body.message, 'Cannot check out. Booking status is CONFIRMED');

    // =============================================================
    // Test 10: Concurrent Check-In and Check-Out Attempts
    // =============================================================
    console.log('\n[Scenario 10: Concurrent Check-In Attempts]');
    const b10 = await createConfirmedBooking(0, 2);
    const [c1, c2] = await Promise.all([
      request({
        hostname: '127.0.0.1',
        port: 5060,
        path: `/api/bookings/${b10.booking._id}/check-in`,
        method: 'POST',
        headers: { Authorization: 'Bearer DRIVER_TOKEN', 'Content-Type': 'application/json' }
      }, { qrToken: b10.qrToken }),
      request({
        hostname: '127.0.0.1',
        port: 5060,
        path: `/api/bookings/${b10.booking._id}/check-in`,
        method: 'POST',
        headers: { Authorization: 'Bearer OWNER_TOKEN', 'Content-Type': 'application/json' }
      }, { qrToken: b10.qrToken })
    ]);

    const statuses = [c1.status, c2.status].sort();
    assertTrue('Exactly one concurrent check-in succeeded with 200 and other was rejected', statuses[0] === 200 && (statuses[1] === 400 || statuses[1] === 409));

    // Verify DB state
    const dbB10 = await Booking.findById(b10.booking._id);
    assertEqual('Final database status is CHECKED_IN', dbB10.status, 'CHECKED_IN');

  } finally {
    // -------------------------------------------------------------
    // Cleanup temporary test records from MongoDB Atlas
    // -------------------------------------------------------------
    console.log('\n[Atlas Cleanup] Cleaning up test records...');
    if (createdBookingIds.length > 0) {
      const delRes = await Booking.deleteMany({ _id: { $in: createdBookingIds } });
      console.log(`✓ Removed ${delRes.deletedCount} temporary test booking records.`);
    }
    if (testListingId) {
      await ParkingListing.deleteOne({ _id: testListingId });
      console.log(`✓ Removed temporary test listing ${testListingId}.`);
    }

    // Verify that demo listings remain intact
    const remainingDemo = await ParkingListing.countDocuments();
    assertEqual('All 20 demo listings remain undisturbed', remainingDemo, 20);
  }

  console.log('\n========================================');
  console.log(`Check-In / Check-Out Test Results: ${passed} passed, ${failed} failed`);
  console.log('========================================\n');

  admin.auth().verifyIdToken = originalVerify;
  server.close(() => {
    process.exit(failed > 0 ? 1 : 0);
  });
};

runTests().catch((err) => {
  console.error('[Test Execution Error]', err);
  process.exit(1);
});
