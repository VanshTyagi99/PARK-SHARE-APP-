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
  process.env.PORT = '5058';
  process.env.NODE_ENV = 'test';

  const { admin } = require('../src/config/firebase');
  const db = require('../src/config/db');

  // Stub Firebase verifyIdToken for testing valid tokens
  const originalVerify = admin.auth().verifyIdToken;
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'VALID_DRIVER_TOKEN') {
      return {
        uid: 'test_driver_uid_101',
        email: 'driver@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'VALID_OWNER_TOKEN') {
      return {
        uid: 'test_owner_uid_202',
        email: 'owner@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'THIRD_PARTY_TOKEN') {
      return {
        uid: 'test_stranger_uid_303',
        email: 'stranger@parkshare.com',
        email_verified: true
      };
    }
    const err = new Error('Firebase ID token has invalid signature');
    err.code = 'auth/invalid-id-token';
    throw err;
  };

  const app = require('../src/server');
  const server = app.listen(5058, '127.0.0.1');

  await new Promise((resolve) => setTimeout(resolve, 500));

  console.log('--- [Category B: Mocked HTTP/API Tests] Booking REST API Test Suite ---\n');

  let passed = 0;
  let failed = 0;

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

  // ==========================================
  // Group 1: Authentication & Header Enforcement
  // ==========================================
  console.log('[Group 1: Authentication & Header Enforcement]');

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { listingId: '507f1f77bcf86cd799439011' });
    assertEqual('POST /api/bookings without token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/my-bookings',
      method: 'GET'
    });
    assertEqual('GET /api/bookings/my-bookings without token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/owner-requests',
      method: 'GET'
    });
    assertEqual('GET /api/bookings/owner-requests without token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/my-bookings',
      method: 'GET',
      headers: { Authorization: 'Basic dXNlcjpwYXNz' }
    });
    assertEqual('Malformed Authorization header rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/my-bookings',
      method: 'GET',
      headers: { Authorization: 'Bearer INVALID_TOKEN_XYZ' }
    });
    assertEqual('Invalid Firebase token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // ==========================================
  // Group 2: Database Connectivity Guard (503)
  // ==========================================
  console.log('\n[Group 2: Database Connectivity Guard (503 Service Unavailable)]');

  const originalIsDbConnected = db.isDbConnected;
  db.isDbConnected = () => false;

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/my-bookings',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_DRIVER_TOKEN' }
    });
    assertEqual('GET /my-bookings returns 503 when DB disconnected', res.status, 503);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { listingId: '507f1f77bcf86cd799439011' });
    assertEqual('POST /api/bookings returns 503 when DB disconnected', res.status, 503);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Restore DB connected status
  db.isDbConnected = originalIsDbConnected;

  // ==========================================
  // Group 3: Input Validation and Schema Guardrails
  // ==========================================
  console.log('\n[Group 3: Input Validation and Schema Guardrails]');

  // Simulate readyState = 1 for input validation tests
  Object.defineProperty(mongoose.connection, 'readyState', { value: 1, configurable: true });

  // Test 3.1: Invalid listingId format
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: 'invalid-id-xyz',
      startTime: '2026-10-15T10:00:00.000Z',
      endTime: '2026-10-15T12:00:00.000Z'
    });
    assertEqual('Invalid listingId format returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.2: Missing startTime or endTime
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: '507f1f77bcf86cd799439011',
      startTime: '2026-10-15T10:00:00.000Z'
    });
    assertEqual('Missing endTime returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.3: Invalid date strings
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: '507f1f77bcf86cd799439011',
      startTime: 'not_a_valid_date',
      endTime: 'also_not_a_date'
    });
    assertEqual('Malformed date format returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.4: startTime >= endTime
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: '507f1f77bcf86cd799439011',
      startTime: '2026-10-15T14:00:00.000Z',
      endTime: '2026-10-15T12:00:00.000Z'
    });
    assertEqual('startTime after endTime returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.5: Past startTime
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: '507f1f77bcf86cd799439011',
      startTime: '2020-01-01T10:00:00.000Z',
      endTime: '2020-01-01T12:00:00.000Z'
    });
    assertEqual('Start time in past returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.6: Duration under 1 hour
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: '507f1f77bcf86cd799439011',
      startTime: '2026-10-15T10:00:00.000Z',
      endTime: '2026-10-15T10:30:00.000Z'
    });
    assertEqual('Duration < 1 hour returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.7: Duration over 24 hours
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      listingId: '507f1f77bcf86cd799439011',
      startTime: '2026-10-15T10:00:00.000Z',
      endTime: '2026-10-17T10:00:00.000Z'
    });
    assertEqual('Duration > 24 hours returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.8: Invalid booking ID format on GET /:id
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/invalid-id-xyz',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_DRIVER_TOKEN' }
    });
    assertEqual('GET /api/bookings/:id with invalid format returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.9: Invalid booking ID format on PATCH /:id/accept
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/invalid-id-xyz/accept',
      method: 'PATCH',
      headers: { Authorization: 'Bearer VALID_OWNER_TOKEN' }
    });
    assertEqual('PATCH /:id/accept with invalid format returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.10: Invalid booking ID format on PATCH /:id/confirm
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/invalid-id-xyz/confirm',
      method: 'PATCH',
      headers: { Authorization: 'Bearer VALID_DRIVER_TOKEN' }
    });
    assertEqual('PATCH /:id/confirm with invalid format returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.11: Check-in without qrToken returns 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/507f1f77bcf86cd799439011/check-in',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {});
    assertEqual('Check-in without qrToken returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  // Test 3.12: Check-out without qrToken returns 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5058,
      path: '/api/bookings/507f1f77bcf86cd799439011/check-out',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_DRIVER_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {});
    assertEqual('Check-out without qrToken returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL:', err.message);
    failed++;
  }

  console.log('\n========================================');
  console.log(`Booking API Mock Test Results: ${passed} passed, ${failed} failed`);
  console.log('========================================\n');

  // Restore admin
  admin.auth().verifyIdToken = originalVerify;

  server.close(() => {
    process.exit(failed > 0 ? 1 : 0);
  });
};

runTests();
