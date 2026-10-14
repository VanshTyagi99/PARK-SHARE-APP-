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
  process.env.PORT = '5056';
  process.env.NODE_ENV = 'test';

  const { admin } = require('../src/config/firebase');
  const db = require('../src/config/db');

  // Stub Firebase verifyIdToken for testing valid tokens
  const originalVerify = admin.auth().verifyIdToken;
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'VALID_TEST_TOKEN') {
      return {
        uid: 'test_owner_uid_123',
        email: 'owner@parkshare.com',
        email_verified: true
      };
    }
    if (token === 'OTHER_OWNER_TOKEN') {
      return {
        uid: 'other_owner_uid_456',
        email: 'other@parkshare.com',
        email_verified: true
      };
    }
    const err = new Error('Firebase ID token has invalid signature');
    err.code = 'auth/invalid-id-token';
    throw err;
  };

  const app = require('../src/server');
  const server = app.listen(5056, '127.0.0.1');

  await new Promise((resolve) => setTimeout(resolve, 500));

  console.log('--- Running Parking Owner Listing CRUD Test Suite ---');

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

  // Group 1: Authentication & Authorization Tests
  console.log('\n[Group 1: Authentication & Header Enforcement]');

  // Test 1: GET /api/parking/my-listings without token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/my-listings',
      method: 'GET'
    });
    assertEqual('GET /my-listings without token rejects with 401', res.status, 401, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: GET /my-listings without token error:', err.message);
    failed++;
  }

  // Test 2: POST /api/parking without token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { title: 'Test' });
    assertEqual('POST /api/parking without token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL: POST /api/parking without token error:', err.message);
    failed++;
  }

  // Test 3: PUT /api/parking/:id without token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/507f1f77bcf86cd799439011',
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' }
    }, { title: 'Update Test' });
    assertEqual('PUT /api/parking/:id without token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL: PUT /api/parking/:id without token error:', err.message);
    failed++;
  }

  // Test 4: PATCH /api/parking/:id/status without token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/507f1f77bcf86cd799439011/status',
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' }
    }, { isActive: false });
    assertEqual('PATCH /api/parking/:id/status without token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL: PATCH /api/parking/:id/status without token error:', err.message);
    failed++;
  }

  // Test 5: Malformed Authorization header -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/my-listings',
      method: 'GET',
      headers: { Authorization: 'Basic dXNlcjpwYXNz' }
    });
    assertEqual('Malformed Authorization header rejects with 401', res.status, 401, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Malformed header error:', err.message);
    failed++;
  }

  // Test 6: Invalid Firebase token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/my-listings',
      method: 'GET',
      headers: { Authorization: 'Bearer INVALID_EXPIRED_TOKEN' }
    });
    assertEqual('Invalid Firebase token rejects with 401', res.status, 401, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Invalid Firebase token error:', err.message);
    failed++;
  }

  // Group 2: Database Disconnected State (503 Service Unavailable)
  console.log('\n[Group 2: Database Connectivity Guard (503 Service Unavailable)]');

  // Verify that when DB is not connected, endpoints return 503
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/my-listings',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /my-listings returns 503 when DB disconnected', res.status, 503, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: GET /my-listings 503 check error:', err.message);
    failed++;
  }

  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'Driveway Parking',
      address: '123 Main St',
      pricePerHour: 40,
      availableFrom: '09:00',
      availableUntil: '18:00'
    });
    assertEqual('POST /api/parking returns 503 when DB disconnected', res.status, 503, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: POST /api/parking 503 check error:', err.message);
    failed++;
  }

  // Group 3: Input Validation with Connected DB Simulation
  console.log('\n[Group 3: Input Validation and Schema Guardrails]');

  // Simulate readyState = 1 and connection state for input validation tests
  Object.defineProperty(mongoose.connection, 'readyState', { value: 1, configurable: true });

  // Test 7: POST /api/parking with title < 3 chars -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'AB',
      address: '123 Main St',
      pricePerHour: 40,
      availableFrom: '09:00',
      availableUntil: '18:00'
    });
    assertEqual('POST with short title returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Short title validation error:', err.message);
    failed++;
  }

  // Test 8: POST /api/parking with address < 3 chars -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'Valid Title',
      address: 'A',
      pricePerHour: 40,
      availableFrom: '09:00',
      availableUntil: '18:00'
    });
    assertEqual('POST with short address returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Short address validation error:', err.message);
    failed++;
  }

  // Test 9: POST /api/parking with price <= 0 -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'Valid Title',
      address: '123 Main Street',
      pricePerHour: 0,
      availableFrom: '09:00',
      availableUntil: '18:00'
    });
    assertEqual('POST with zero price returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Zero price validation error:', err.message);
    failed++;
  }

  // Test 10: POST /api/parking with negative price -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'Valid Title',
      address: '123 Main Street',
      pricePerHour: -15,
      availableFrom: '09:00',
      availableUntil: '18:00'
    });
    assertEqual('POST with negative price returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Negative price validation error:', err.message);
    failed++;
  }

  // Test 11: POST /api/parking with missing availableFrom -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking',
      method: 'POST',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, {
      title: 'Valid Title',
      address: '123 Main Street',
      pricePerHour: 30,
      availableUntil: '18:00'
    });
    assertEqual('POST with missing availableFrom returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Missing availableFrom validation error:', err.message);
    failed++;
  }

  // Test 12: GET /api/parking/:id with invalid ObjectId format -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/invalid-mongo-id-123',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET with invalid ObjectId format returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: Invalid ID format error:', err.message);
    failed++;
  }

  // Test 13: PUT /api/parking/:id with invalid ObjectId format -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/invalid-mongo-id-123',
      method: 'PUT',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { title: 'Updated Title' });
    assertEqual('PUT with invalid ObjectId format returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: PUT invalid ID format error:', err.message);
    failed++;
  }

  // Test 14: PATCH /api/parking/:id/status with invalid ObjectId format -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/invalid-mongo-id-123/status',
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { isActive: true });
    assertEqual('PATCH with invalid ObjectId format returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: PATCH invalid ID format error:', err.message);
    failed++;
  }

  // Test 15: PATCH /api/parking/:id/status with non-boolean isActive -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5056,
      path: '/api/parking/507f1f77bcf86cd799439011/status',
      method: 'PATCH',
      headers: {
        Authorization: 'Bearer VALID_TEST_TOKEN',
        'Content-Type': 'application/json'
      }
    }, { isActive: 'not_a_boolean' });
    assertEqual('PATCH with non-boolean status returns 400', res.status, 400, res.body.message);
  } catch (err) {
    console.error('✗ FAIL: PATCH non-boolean status error:', err.message);
    failed++;
  }

  console.log(`\n========================================`);
  console.log(`Parking Test Results: ${passed} passed, ${failed} failed`);
  console.log(`========================================\n`);

  server.close(() => {
    process.exit(failed > 0 ? 1 : 0);
  });
};

runTests();
