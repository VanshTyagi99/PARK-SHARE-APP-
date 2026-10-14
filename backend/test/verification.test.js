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

const runVerificationTests = async () => {
  process.env.PORT = '5059';
  process.env.NODE_ENV = 'test';
  process.env.ADMIN_EMAILS = 'admin.tester@parkshare.demo,vanshtyagi7032@gmail.com';

  const { admin } = require('../src/config/firebase');

  const UNVERIFIED_UID = 'test_unverified_driver_01';
  const PENDING_UID = 'test_pending_driver_02';
  const REJECTED_UID = 'test_rejected_driver_03';
  const APPROVED_UID = 'test_approved_driver_04';
  const ADMIN_UID = 'test_admin_reviewer_99';

  // Stub Firebase verifyIdToken
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'TOKEN_UNVERIFIED') {
      return { uid: UNVERIFIED_UID, email: 'unverified@parkshare.demo', email_verified: true };
    }
    if (token === 'TOKEN_PENDING') {
      return { uid: PENDING_UID, email: 'pending@parkshare.demo', email_verified: true };
    }
    if (token === 'TOKEN_REJECTED') {
      return { uid: REJECTED_UID, email: 'rejected@parkshare.demo', email_verified: true };
    }
    if (token === 'TOKEN_APPROVED') {
      return { uid: APPROVED_UID, email: 'approved@parkshare.demo', email_verified: true };
    }
    if (token === 'TOKEN_ADMIN') {
      return { uid: ADMIN_UID, email: 'admin.tester@parkshare.demo', email_verified: true };
    }
    const err = new Error('Firebase ID token has invalid signature');
    err.code = 'auth/invalid-id-token';
    throw err;
  };

  const { connectDB } = require('../src/config/db');
  await connectDB();

  const app = require('../src/server');
  const server = app.listen(5059, '127.0.0.1');

  await new Promise((resolve) => setTimeout(resolve, 500));

  console.log('--- Running Mandatory Verification & Admin Center Test Suite ---');

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

  const User = require('../src/models/User');
  const UserVerification = require('../src/models/UserVerification');
  const ParkingListing = require('../src/models/ParkingListing');
  const Booking = require('../src/models/Booking');

  const allTestUids = [UNVERIFIED_UID, PENDING_UID, REJECTED_UID, APPROVED_UID, ADMIN_UID];

  try {
    // Clean up test data
    await User.deleteMany({ firebaseUid: { $in: allTestUids } });
    await UserVerification.deleteMany({ userId: { $in: allTestUids } });
    await ParkingListing.deleteMany({ ownerFirebaseUid: { $in: allTestUids } });
    await Booking.deleteMany({ driverFirebaseUid: { $in: allTestUids } });

    // Seed test states
    // 1. Unverified user: no record or NOT_SUBMITTED
    await User.create({ firebaseUid: UNVERIFIED_UID, email: 'unverified@parkshare.demo', verificationStatus: 'NOT_SUBMITTED' });

    // 2. Pending user
    await User.create({ firebaseUid: PENDING_UID, email: 'pending@parkshare.demo', verificationStatus: 'PENDING' });
    await UserVerification.create({
      userId: PENDING_UID,
      userEmail: 'pending@parkshare.demo',
      identityDocumentType: 'DRIVING_LICENCE',
      identityDocumentData: 'data:image/jpeg;base64,mockIdPending',
      addressDocumentType: 'UTILITY_BILL',
      addressDocumentData: 'data:image/jpeg;base64,mockAddressPending',
      selfieData: 'data:image/jpeg;base64,mockSelfiePending',
      status: 'PENDING'
    });

    // 3. Rejected user
    await User.create({ firebaseUid: REJECTED_UID, email: 'rejected@parkshare.demo', verificationStatus: 'REJECTED' });
    await UserVerification.create({
      userId: REJECTED_UID,
      userEmail: 'rejected@parkshare.demo',
      identityDocumentType: 'PASSPORT',
      identityDocumentData: 'data:image/jpeg;base64,mockIdRejected',
      addressDocumentType: 'GOVERNMENT_ID',
      addressDocumentData: 'data:image/jpeg;base64,mockAddressRejected',
      selfieData: 'data:image/jpeg;base64,mockSelfieRejected',
      status: 'REJECTED',
      rejectionReason: 'Selfie does not match document photo'
    });

    // 4. Approved user
    await User.create({ firebaseUid: APPROVED_UID, email: 'approved@parkshare.demo', verificationStatus: 'APPROVED' });
    await UserVerification.create({
      userId: APPROVED_UID,
      userEmail: 'approved@parkshare.demo',
      identityDocumentType: 'DRIVING_LICENCE',
      identityDocumentData: 'data:image/jpeg;base64,mockIdApproved',
      addressDocumentType: 'GOVERNMENT_ID',
      addressDocumentData: 'data:image/jpeg;base64,mockAddressApproved',
      selfieData: 'data:image/jpeg;base64,mockSelfieApproved',
      status: 'APPROVED'
    });

    // 5. Admin user
    await User.create({ firebaseUid: ADMIN_UID, email: 'admin.tester@parkshare.demo', role: 'admin', verificationStatus: 'APPROVED' });

    console.log('\n[Group 1: Marketplace Gate for Unapproved Users (Find & List Parking)]');

    // Test 1: NOT_SUBMITTED user cannot search for parking -> 403 VERIFICATION_REQUIRED
    const res1 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/parking/search',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_UNVERIFIED' }
    });
    assertEqual('NOT_SUBMITTED user search blocked with 403', res1.status, 403);
    assertEqual('NOT_SUBMITTED code is VERIFICATION_REQUIRED', res1.body.code, 'VERIFICATION_REQUIRED');

    // Test 2: PENDING user cannot search for parking -> 403 VERIFICATION_REQUIRED
    const res2 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/parking/search',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_PENDING' }
    });
    assertEqual('PENDING user search blocked with 403', res2.status, 403);
    assertEqual('PENDING status returned in body', res2.body.status, 'PENDING');

    // Test 3: REJECTED user cannot create listings -> 403 VERIFICATION_REQUIRED
    const res3 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_REJECTED',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'Rejected User Listing Attempt',
      address: 'Test Location',
      pricePerHour: 30,
      availableFrom: '08:00',
      availableUntil: '20:00'
    });
    assertEqual('REJECTED user listing creation blocked with 403', res3.status, 403);
    assertEqual('REJECTED code is VERIFICATION_REQUIRED', res3.body.code, 'VERIFICATION_REQUIRED');

    // Test 4: REJECTED user cannot create bookings -> 403 VERIFICATION_REQUIRED
    const anyListing = await ParkingListing.findOne({ isActive: true });
    const res4 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_REJECTED',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: anyListing ? anyListing._id.toString() : '507f1f77bcf86cd799439011',
      startTime: new Date(Date.now() + 2 * 3600 * 1000).toISOString(),
      endTime: new Date(Date.now() + 4 * 3600 * 1000).toISOString()
    });
    assertEqual('REJECTED user booking creation blocked with 403', res4.status, 403);

    // Test 4B: Unverified users cannot view parking details -> 403 VERIFICATION_REQUIRED
    const testListingId = anyListing ? anyListing._id.toString() : '507f1f77bcf86cd799439011';
    const res4b_unv = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/parking/${testListingId}`,
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_UNVERIFIED' }
    });
    assertEqual('NOT_SUBMITTED user parking details blocked with 403', res4b_unv.status, 403);

    const res4b_pen = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/parking/${testListingId}`,
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_PENDING' }
    });
    assertEqual('PENDING user parking details blocked with 403', res4b_pen.status, 403);

    const res4b_rej = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/parking/${testListingId}`,
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_REJECTED' }
    });
    assertEqual('REJECTED user parking details blocked with 403', res4b_rej.status, 403);

    // Test 4C: Unverified users cannot activate/deactivate listings -> 403 VERIFICATION_REQUIRED
    const res4c = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/parking/${testListingId}/status`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer TOKEN_UNVERIFIED',
        'Content-Type': 'application/json'
      }
    }, { isActive: false });
    assertEqual('Unverified listing activation blocked with 403', res4c.status, 403);

    console.log('\n[Group 2: Approved User Allowed Access (Find & List Parking)]');

    // Test 5: APPROVED user can search for parking -> 200 OK
    const res5 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/parking/search',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_APPROVED' }
    });
    assertEqual('APPROVED user can search parking (200 OK)', res5.status, 200);
    assertEqual('APPROVED user receives parking results', Array.isArray(res5.body.data), true);

    // Test 5B: APPROVED user can view parking details -> 200 OK
    const res5b = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/parking/${testListingId}`,
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_APPROVED' }
    });
    assertEqual('APPROVED user can view parking details (200 OK)', res5b.status, 200);

    // Test 6: APPROVED user can create a parking listing -> 201 Created
    const res6 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_APPROVED',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'Approved Owner Space',
      address: 'Sector 62 Noida',
      pricePerHour: 50,
      availableFrom: '08:00',
      availableUntil: '20:00'
    });
    assertEqual('APPROVED user can create parking listing (201 Created)', res6.status, 201);
    const createdListingId = res6.body.data._id;

    // Test 6B: APPROVED user can toggle listing status -> 200 OK
    const res6b = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/parking/${createdListingId}/status`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer TOKEN_APPROVED',
        'Content-Type': 'application/json'
      }
    }, { isActive: false });
    assertEqual('APPROVED user can update listing status (200 OK)', res6b.status, 200);

    // Test 7: APPROVED user can create booking -> 201 Created
    const res7 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_APPROVED',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: anyListing._id.toString(),
      startTime: new Date(Date.now() + 3 * 3600 * 1000).toISOString(),
      endTime: new Date(Date.now() + 5 * 3600 * 1000).toISOString()
    });
    assertEqual('APPROVED user can create booking (201 Created)', res7.status, 201);

    console.log('\n[Group 3: Verification Submission & Validation Rules]');

    // Test 8: Submission missing identity proof -> 400
    const res8 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/verification/submit',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_UNVERIFIED',
        'Content-Type': 'application/json'
      }
    }, {
      addressDocumentType: 'UTILITY_BILL',
      addressDocumentData: 'data:image/jpeg;base64,abc',
      selfieData: 'data:image/jpeg;base64,def'
    });
    assertEqual('Missing identity proof returns 400', res8.status, 400);

    // Test 9: Submission missing selfie -> 400
    const res9 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/verification/submit',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_UNVERIFIED',
        'Content-Type': 'application/json'
      }
    }, {
      identityDocumentType: 'DRIVING_LICENCE',
      identityDocumentData: 'data:image/jpeg;base64,abc',
      addressDocumentType: 'UTILITY_BILL',
      addressDocumentData: 'data:image/jpeg;base64,def'
    });
    assertEqual('Missing selfie returns 400', res9.status, 400);

    // Test 10: Complete valid submission succeeds and sets status to PENDING -> 200
    const res10 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/verification/submit',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_UNVERIFIED',
        'Content-Type': 'application/json'
      }
    }, {
      identityDocumentType: 'AADHAAR',
      identityDocumentData: 'data:image/jpeg;base64,validAadhaarData',
      addressDocumentType: 'GOVERNMENT_ID',
      addressDocumentData: 'data:image/jpeg;base64,validAddressData',
      selfieData: 'data:image/jpeg;base64,validSelfieData'
    });
    assertEqual('Complete 3-document submission succeeds (200 OK)', res10.status, 200);
    assertEqual('Submitted status is PENDING', res10.body.data.status, 'PENDING');

    // Test 11: GET /api/verification/status returns PENDING without Base64 fields
    const res11 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/verification/status',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_UNVERIFIED' }
    });
    assertEqual('GET /verification/status returns 200 OK', res11.status, 200);
    assertEqual('Status is PENDING', res11.body.data.status, 'PENDING');
    assertEqual('No Base64 documents in status response', res11.body.data.identityDocumentData, undefined);

    console.log('\n[Group 4: Admin Review, Security & Resubmission]');

    // Test 12: Normal user cannot access admin queue -> 403 Forbidden
    const res12 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/admin/verifications',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_UNVERIFIED' }
    });
    assertEqual('Non-admin access to admin queue blocked with 403', res12.status, 403);

    // Test 12B: Non-admin access to admin documents blocked -> 403 Forbidden
    const dummyRecord = await UserVerification.findOne({ userId: PENDING_UID });
    const dummyId = dummyRecord._id.toString();
    const res12b = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/admin/verifications/${dummyId}/documents`,
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_UNVERIFIED' }
    });
    assertEqual('Non-admin document access blocked with 403', res12b.status, 403);

    // Test 12C: Non-admin review attempt blocked -> 403 Forbidden
    const res12c = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/admin/verifications/${dummyId}/review`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer TOKEN_UNVERIFIED',
        'Content-Type': 'application/json'
      }
    }, { status: 'APPROVED' });
    assertEqual('Non-admin review action blocked with 403', res12c.status, 403);

    // Test 12D: Client cannot elevate status or role via /api/users/sync
    const res12d = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/users/sync',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_UNVERIFIED',
        'Content-Type': 'application/json'
      }
    }, {
      fullName: 'Hacker Attempt',
      role: 'admin',
      verificationStatus: 'APPROVED'
    });
    assertEqual('Sync call returns 200', res12d.status, 200);
    const refreshedUser = await User.findOne({ firebaseUid: UNVERIFIED_UID });
    assertEqual('Client cannot self-elevate role to admin', refreshedUser.role, 'user');
    assertEqual('Client cannot self-approve verificationStatus', refreshedUser.verificationStatus !== 'APPROVED', true);

    // Test 13: Admin can access queue -> 200 OK
    const res13 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/admin/verifications',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_ADMIN' }
    });
    assertEqual('Admin access to queue succeeds (200 OK)', res13.status, 200);
    assertEqual('Queue response contains applicants', res13.body.data.length > 0, true);

    // Test 13B: Admin queue filter by status works
    const res13b = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/admin/verifications?status=PENDING',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_ADMIN' }
    });
    assertEqual('Queue filtered by PENDING succeeds (200 OK)', res13b.status, 200);
    const allPending = res13b.body.data.every((item) => item.status === 'PENDING');
    assertEqual('Filtered queue only returns PENDING items', allPending, true);

    // Find the newly submitted record for UNVERIFIED_UID
    const submittedRecord = await UserVerification.findOne({ userId: UNVERIFIED_UID });
    const submittedId = submittedRecord._id.toString();

    // Test 14: Admin fetches documents for preview -> 200 OK
    const res14 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/admin/verifications/${submittedId}/documents`,
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_ADMIN' }
    });
    assertEqual('Admin fetches applicant documents (200 OK)', res14.status, 200);
    assertEqual('Applicant documents include selfieData', typeof res14.body.data.selfieData, 'string');

    // Test 15: Rejection without reason is rejected -> 400
    const res15 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/admin/verifications/${submittedId}/review`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer TOKEN_ADMIN',
        'Content-Type': 'application/json'
      }
    }, {
      status: 'REJECTED'
    });
    assertEqual('Rejection without reason returns 400', res15.status, 400);

    // Test 16: Admin rejects with valid reason -> 200 OK
    const res16 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/admin/verifications/${submittedId}/review`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer TOKEN_ADMIN',
        'Content-Type': 'application/json'
      }
    }, {
      status: 'REJECTED',
      rejectionReason: 'Document image was blurry and unreadable'
    });
    assertEqual('Admin rejection with reason succeeds (200 OK)', res16.status, 200);
    assertEqual('Status updated to REJECTED', res16.body.data.status, 'REJECTED');

    // Test 17: User sees rejection reason on status endpoint -> 200
    const res17 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/verification/status',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_UNVERIFIED' }
    });
    assertEqual('User status returns REJECTED', res17.body.data.status, 'REJECTED');
    assertEqual('User status includes rejectionReason', res17.body.data.rejectionReason, 'Document image was blurry and unreadable');

    // Test 18: Resubmission after rejection resets status to PENDING -> 200
    const res18 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/verification/submit',
      method: 'POST',
      headers: {
        Authorization: 'Bearer TOKEN_UNVERIFIED',
        'Content-Type': 'application/json'
      }
    }, {
      identityDocumentType: 'PASSPORT',
      identityDocumentData: 'data:image/jpeg;base64,newClearPassportData',
      addressDocumentType: 'UTILITY_BILL',
      addressDocumentData: 'data:image/jpeg;base64,newClearBillData',
      selfieData: 'data:image/jpeg;base64,newClearSelfieData'
    });
    assertEqual('Resubmission after rejection returns 200', res18.status, 200);
    assertEqual('Resubmission status returns to PENDING', res18.body.data.status, 'PENDING');

    // Test 19: Admin approves the application -> 200 OK
    const res19 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: `/api/admin/verifications/${submittedId}/review`,
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer TOKEN_ADMIN',
        'Content-Type': 'application/json'
      }
    }, {
      status: 'APPROVED'
    });
    assertEqual('Admin approves application (200 OK)', res19.status, 200);
    assertEqual('Status updated to APPROVED', res19.body.data.status, 'APPROVED');

    // Test 20: Previously unverified user can now search and offer parking
    const res20 = await request({
      hostname: '127.0.0.1',
      port: 5059,
      path: '/api/parking/search',
      method: 'GET',
      headers: { Authorization: 'Bearer TOKEN_UNVERIFIED' }
    });
    assertEqual('Newly approved user can now search parking (200 OK)', res20.status, 200);

    console.log('\n[Group 5: Demo Data Integrity Check]');
    const demoCount = await ParkingListing.countDocuments({ ownerFirebaseUid: 'demo_partner_ncr_01' });
    assertEqual('Atlas demo listings preserved (expected >= 20)', demoCount >= 20, true, `actual: ${demoCount}`);

  } finally {
    // Clean up test records
    await User.deleteMany({ firebaseUid: { $in: allTestUids } });
    await UserVerification.deleteMany({ userId: { $in: allTestUids } });
    await ParkingListing.deleteMany({ ownerFirebaseUid: { $in: allTestUids } });
    await Booking.deleteMany({ driverFirebaseUid: { $in: allTestUids } });

    server.close();
    await mongoose.disconnect();
  }

  console.log('\n========================================');
  console.log(`Verification Test Results: ${passed} passed, ${failed} failed`);
  console.log('========================================\n');

  if (failed > 0) process.exit(1);
};

runVerificationTests();
