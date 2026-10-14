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
  process.env.PORT = '5070';
  process.env.NODE_ENV = 'test';

  const { admin } = require('../src/config/firebase');
  const { connectDB } = require('../src/config/db');
  const User = require('../src/models/User');
  const UserVerification = require('../src/models/UserVerification');
  const ParkingListing = require('../src/models/ParkingListing');
  const Booking = require('../src/models/Booking');
  const Conversation = require('../src/models/Conversation');
  const Message = require('../src/models/Message');

  // Stub Firebase verifyIdToken for testing valid tokens
  const originalVerify = admin.auth().verifyIdToken;
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'DRIVER_TOKEN') {
      return {
        uid: 'chat_test_driver_uid',
        email: 'chat.driver@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'OWNER_TOKEN') {
      return {
        uid: 'chat_test_owner_uid',
        email: 'chat.owner@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'STRANGER_TOKEN') {
      return {
        uid: 'chat_test_stranger_uid',
        email: 'chat.stranger@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'ADMIN_TOKEN') {
      return {
        uid: 'admin_test_uid',
        email: 'vanshtyagi7032@gmail.com',
        email_verified: true
      };
    }
    throw new Error('Invalid Firebase Token');
  };

  await connectDB();

  const app = require('../src/server');
  const server = http.createServer(app);
  await new Promise((resolve) => server.listen(5070, resolve));

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

  console.log('--- [Sprint: Driver-Owner Chat & Admin Dashboard Test Suite] ---\n');

  let testListingId;
  let testBookingId;
  let testConversationId;

  try {
    // 1. Setup approved users for driver, owner, stranger
    await User.findOneAndUpdate(
      { firebaseUid: 'chat_test_driver_uid' },
      { name: 'Chat Driver', email: 'chat.driver@parkshare.com', role: 'user', verificationStatus: 'APPROVED' },
      { upsert: true }
    );
    await User.findOneAndUpdate(
      { firebaseUid: 'chat_test_owner_uid' },
      { name: 'Chat Owner', email: 'chat.owner@parkshare.com', role: 'user', verificationStatus: 'APPROVED' },
      { upsert: true }
    );
    await User.findOneAndUpdate(
      { firebaseUid: 'chat_test_stranger_uid' },
      { name: 'Chat Stranger', email: 'chat.stranger@parkshare.com', role: 'user', verificationStatus: 'APPROVED' },
      { upsert: true }
    );
    await User.findOneAndUpdate(
      { firebaseUid: 'admin_test_uid' },
      { name: 'Admin User', email: 'vanshtyagi7032@gmail.com', role: 'admin', verificationStatus: 'APPROVED' },
      { upsert: true }
    );

    await UserVerification.findOneAndUpdate(
      { userId: 'chat_test_driver_uid' },
      { status: 'APPROVED', reviewedAt: new Date() },
      { upsert: true }
    );
    await UserVerification.findOneAndUpdate(
      { userId: 'chat_test_owner_uid' },
      { status: 'APPROVED', reviewedAt: new Date() },
      { upsert: true }
    );
    await UserVerification.findOneAndUpdate(
      { userId: 'chat_test_stranger_uid' },
      { status: 'APPROVED', reviewedAt: new Date() },
      { upsert: true }
    );
    await UserVerification.findOneAndUpdate(
      { userId: 'admin_test_uid' },
      { status: 'APPROVED', reviewedAt: new Date() },
      { upsert: true }
    );

    // 2. Create test listing & booking
    const testListing = await ParkingListing.create({
      ownerFirebaseUid: 'chat_test_owner_uid',
      title: 'Chat Test Parking Spot',
      address: 'Plot 10, Noida Expressway',
      parkingType: 'Car',
      vehicleCompatibility: 'CAR',
      pricePerHour: 50,
      availableFrom: '00:00',
      availableUntil: '23:59',
      latitude: 28.5355,
      longitude: 77.3910,
      isActive: true,
      isDemoData: false
    });
    testListingId = testListing._id.toString();

    const now = new Date();
    const testBooking = await Booking.create({
      bookingCode: 'CHAT-BOOK-01',
      listingId: testListing._id,
      driverFirebaseUid: 'chat_test_driver_uid',
      ownerFirebaseUid: 'chat_test_owner_uid',
      startTime: new Date(now.getTime() + 3600000),
      endTime: new Date(now.getTime() + 7200000),
      totalAmount: 50,
      totalHours: 1,
      hourlyRateSnapshot: 50,
      durationHours: 1,
      status: 'CONFIRMED'
    });
    testBookingId = testBooking._id.toString();

    // =========================================================================
    // SECTION 1: DRIVER-OWNER CHAT TESTS
    // =========================================================================
    console.log('[Group 1: Conversation Initialization & Access Control]');

    // 1.1 Stranger cannot access booking chat
    const strangerGetConv = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/chat/booking/${testBookingId}`,
      method: 'GET',
      headers: { Authorization: 'Bearer STRANGER_TOKEN' }
    });
    assertEqual('Stranger accessing booking chat returns 403', strangerGetConv.status, 403);

    // 1.2 Driver accesses/creates booking chat
    const driverGetConv = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/chat/booking/${testBookingId}`,
      method: 'GET',
      headers: { Authorization: 'Bearer DRIVER_TOKEN' }
    });
    assertEqual('Driver accessing booking chat returns 200', driverGetConv.status, 200);
    assertTrue('Conversation object is returned', driverGetConv.body && driverGetConv.body.data && driverGetConv.body.data._id);
    testConversationId = driverGetConv.body.data._id;

    // 1.3 Owner accesses same booking chat -> gets same conversation
    const ownerGetConv = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/chat/booking/${testBookingId}`,
      method: 'GET',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('Owner accessing booking chat returns 200', ownerGetConv.status, 200);
    assertEqual('Owner gets same conversation ID', ownerGetConv.body.data._id, testConversationId);

    console.log('\n[Group 2: Message Exchange, Persistence & Roles]');

    // 2.1 Driver sends a message
    const driverSend = await request(
      {
        hostname: '127.0.0.1',
        port: 5070,
        path: `/api/chat/conversations/${testConversationId}/messages`,
        method: 'POST',
        headers: { Authorization: 'Bearer DRIVER_TOKEN', 'Content-Type': 'application/json' }
      },
      { text: 'Hi, I will arrive in 15 minutes. Is the driveway gate open?' }
    );
    assertEqual('Driver sends message successfully (201)', driverSend.status, 201);
    assertEqual('Message senderRole is DRIVER', driverSend.body.data.senderRole, 'DRIVER');
    assertEqual('Message sender is driver UID', driverSend.body.data.senderFirebaseUid, 'chat_test_driver_uid');

    // 2.2 Owner views messages
    const ownerGetMessages = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/chat/conversations/${testConversationId}/messages`,
      method: 'GET',
      headers: { Authorization: 'Bearer OWNER_TOKEN' }
    });
    assertEqual('Owner retrieves messages successfully (200)', ownerGetMessages.status, 200);
    assertEqual('Message count is 1', ownerGetMessages.body.count, 1);
    assertEqual('Message text matches driver input', ownerGetMessages.body.data[0].text, 'Hi, I will arrive in 15 minutes. Is the driveway gate open?');

    // 2.3 Owner replies
    const ownerSend = await request(
      {
        hostname: '127.0.0.1',
        port: 5070,
        path: `/api/chat/conversations/${testConversationId}/messages`,
        method: 'POST',
        headers: { Authorization: 'Bearer OWNER_TOKEN', 'Content-Type': 'application/json' }
      },
      { text: 'Yes, gate is unlocked. Slot #3 is reserved for your car.' }
    );
    assertEqual('Owner sends reply successfully (201)', ownerSend.status, 201);
    assertEqual('Reply senderRole is OWNER', ownerSend.body.data.senderRole, 'OWNER');

    // 2.4 Stranger blocked from reading or sending
    const strangerGetMessages = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/chat/conversations/${testConversationId}/messages`,
      method: 'GET',
      headers: { Authorization: 'Bearer STRANGER_TOKEN' }
    });
    assertEqual('Stranger reading conversation messages returns 403', strangerGetMessages.status, 403);

    const strangerSend = await request(
      {
        hostname: '127.0.0.1',
        port: 5070,
        path: `/api/chat/conversations/${testConversationId}/messages`,
        method: 'POST',
        headers: { Authorization: 'Bearer STRANGER_TOKEN', 'Content-Type': 'application/json' }
      },
      { text: 'I am an intruder' }
    );
    assertEqual('Stranger sending message returns 403', strangerSend.status, 403);

    // 2.5 Validation: Empty text rejected
    const emptySend = await request(
      {
        hostname: '127.0.0.1',
        port: 5070,
        path: `/api/chat/conversations/${testConversationId}/messages`,
        method: 'POST',
        headers: { Authorization: 'Bearer DRIVER_TOKEN', 'Content-Type': 'application/json' }
      },
      { text: '   ' }
    );
    assertEqual('Empty message rejected with 400', emptySend.status, 400);

    // 2.6 List conversations for driver
    const driverListConversations = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/chat/conversations`,
      method: 'GET',
      headers: { Authorization: 'Bearer DRIVER_TOKEN' }
    });
    assertEqual('Driver lists conversations (200)', driverListConversations.status, 200);
    assertTrue('Driver has at least 1 conversation', driverListConversations.body.count >= 1);
    assertEqual('Conversation lastMessageText is updated', driverListConversations.body.data[0].lastMessageText, 'Yes, gate is unlocked. Slot #3 is reserved for your car.');

    // =========================================================================
    // SECTION 2: ADMIN DASHBOARD & MANAGEMENT TESTS
    // =========================================================================
    console.log('\n[Group 3: Admin Dashboard Authorization & Real Metrics]');

    // 3.1 Non-admin blocked from dashboard stats
    const nonAdminStats = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/admin/dashboard/stats`,
      method: 'GET',
      headers: { Authorization: 'Bearer DRIVER_TOKEN' }
    });
    assertEqual('Non-admin accessing dashboard stats returns 403', nonAdminStats.status, 403);

    // 3.2 Admin gets dashboard stats with real metrics
    const adminStats = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/admin/dashboard/stats`,
      method: 'GET',
      headers: { Authorization: 'Bearer ADMIN_TOKEN' }
    });
    assertEqual('Admin accessing dashboard stats returns 200', adminStats.status, 200);
    const stats = adminStats.body.data;
    assertTrue('Stats totalUsers is a valid number >= 1', typeof stats.totalUsers === 'number' && stats.totalUsers >= 1);
    assertTrue('Stats activeListings is a valid number >= 20', typeof stats.activeListings === 'number' && stats.activeListings >= 20);
    assertTrue('Stats totalBookings is a valid number', typeof stats.totalBookings === 'number');
    assertTrue('Stats pendingVerifications is a valid number', typeof stats.pendingVerifications === 'number');
    assertTrue('Stats pendingBookingRequests is a valid number', typeof stats.pendingBookingRequests === 'number');
    assertTrue('Stats completedBookings is a valid number', typeof stats.completedBookings === 'number');

    console.log('\n[Group 4: Admin Management Endpoints: Users, Listings, Bookings]');

    // 4.1 Admin Users list
    const adminUsers = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/admin/users`,
      method: 'GET',
      headers: { Authorization: 'Bearer ADMIN_TOKEN' }
    });
    assertEqual('Admin users list returns 200', adminUsers.status, 200);
    assertTrue('Admin users count is >= 1', adminUsers.body.count >= 1);
    assertTrue('User has name and email fields', !!adminUsers.body.data[0].email);

    // 4.2 Admin Listings list
    const adminListings = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/admin/listings`,
      method: 'GET',
      headers: { Authorization: 'Bearer ADMIN_TOKEN' }
    });
    assertEqual('Admin listings list returns 200', adminListings.status, 200);
    assertTrue('Admin listings count is >= 20', adminListings.body.count >= 20);

    // 4.3 Admin Bookings list
    const adminBookings = await request({
      hostname: '127.0.0.1',
      port: 5070,
      path: `/api/admin/bookings`,
      method: 'GET',
      headers: { Authorization: 'Bearer ADMIN_TOKEN' }
    });
    assertEqual('Admin bookings list returns 200', adminBookings.status, 200);
    assertTrue('Admin bookings count is >= 1', adminBookings.body.count >= 1);
    assertTrue('Booking has status and bookingCode', !!adminBookings.body.data[0].status);

  } finally {
    // Cleanup temporary test data
    console.log('\n[Cleanup] Cleaning test chat & booking data...');
    if (testConversationId) {
      await Message.deleteMany({ conversationId: testConversationId });
      await Conversation.deleteOne({ _id: testConversationId });
      console.log('✓ Removed test messages and conversation.');
    }
    if (testBookingId) {
      await Booking.deleteOne({ _id: testBookingId });
      console.log('✓ Removed test booking.');
    }
    if (testListingId) {
      await ParkingListing.deleteOne({ _id: testListingId });
      console.log('✓ Removed test listing.');
    }

    const remainingDemo = await ParkingListing.countDocuments();
    assertEqual('All 20 demo listings remain undisturbed in Atlas', remainingDemo, 20);
  }

  console.log('\n========================================');
  console.log(`Chat & Admin Test Results: ${passed} passed, ${failed} failed`);
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
