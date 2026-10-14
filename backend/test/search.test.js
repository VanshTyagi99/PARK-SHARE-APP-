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

const runSearchTests = async () => {
  process.env.PORT = '5057';
  process.env.NODE_ENV = 'test';

  const { admin } = require('../src/config/firebase');

  // Stub Firebase verifyIdToken for testing valid tokens
  admin.auth().verifyIdToken = async (token) => {
    if (token === 'VALID_TEST_TOKEN') {
      return {
        uid: 'test_driver_uid_789',
        email: 'driver@parkshare.com',
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

  await new Promise((resolve) => setTimeout(resolve, 500));

  console.log('--- Running Search & Discovery Test Suite ---');

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

  // Test 1: GET /api/parking/search without token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search',
      method: 'GET'
    });
    assertEqual('GET /search without token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL: GET /search without token error:', err.message);
    failed++;
  }

  // Test 2: GET /api/parking/search with invalid token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search',
      method: 'GET',
      headers: { Authorization: 'Bearer INVALID_TOKEN' }
    });
    assertEqual('GET /search with invalid token rejects with 401', res.status, 401);
  } catch (err) {
    console.error('✗ FAIL: GET /search invalid token error:', err.message);
    failed++;
  }

  // Test 3: GET /api/parking/search with negative minPrice -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search?minPrice=-10',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search with minPrice < 0 returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL: minPrice validation error:', err.message);
    failed++;
  }

  // Test 4: GET /api/parking/search with minPrice > maxPrice -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search?minPrice=50&maxPrice=30',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search with minPrice > maxPrice returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL: minPrice > maxPrice error:', err.message);
    failed++;
  }

  // Test 5: GET /api/parking/search with invalid latitude -> 400
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search?latitude=999',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search with invalid latitude returns 400', res.status, 400);
  } catch (err) {
    console.error('✗ FAIL: invalid latitude error:', err.message);
    failed++;
  }

  // Test 6: GET /api/parking/search with valid token -> 200 and returns array
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search returns 200 OK', res.status, 200);
    if (res.body && Array.isArray(res.body.data)) {
      console.log(`✓ PASS: GET /search returned ${res.body.count} active listings from database`);
      passed++;
    } else {
      console.error('✗ FAIL: GET /search response body is not an array');
      failed++;
    }
  } catch (err) {
    console.error('✗ FAIL: GET /search error:', err.message);
    failed++;
  }

  // Test 7: GET /api/parking/search with query 'Noida'
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search?query=Noida',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search?query=Noida returns 200 OK', res.status, 200);
    const allContainNoida = res.body.data.every(
      (item) =>
        item.title.toLowerCase().includes('noida') ||
        item.address.toLowerCase().includes('noida') ||
        item.description.toLowerCase().includes('noida')
    );
    assertEqual('All returned items match text query "Noida"', allContainNoida, true);
  } catch (err) {
    console.error('✗ FAIL: text search query error:', err.message);
    failed++;
  }

  // Test 8: GET /api/parking/search with vehicleType 'Bike'
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search?vehicleType=Bike',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search?vehicleType=Bike returns 200 OK', res.status, 200);
    const validVehicleType = res.body.data.every(
      (item) => item.parkingType === 'Bike' || item.parkingType === 'Car + Bike'
    );
    assertEqual('All returned items support Bike parking', validVehicleType, true);
  } catch (err) {
    console.error('✗ FAIL: vehicleType search error:', err.message);
    failed++;
  }

  // Test 9: GET /api/parking/search with price filter maxPrice=30
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search?maxPrice=30',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search?maxPrice=30 returns 200 OK', res.status, 200);
    const validPrices = res.body.data.every((item) => item.pricePerHour <= 30);
    assertEqual('All returned items have pricePerHour <= 30', validPrices, true);
  } catch (err) {
    console.error('✗ FAIL: price filter error:', err.message);
    failed++;
  }

  // Test 10: GET /api/parking/search with coordinates (Noida fallback center: 28.6139, 77.3910)
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5057,
      path: '/api/parking/search?latitude=28.6139&longitude=77.3910&radiusKm=20',
      method: 'GET',
      headers: { Authorization: 'Bearer VALID_TEST_TOKEN' }
    });
    assertEqual('GET /search with coordinates returns 200 OK', res.status, 200);
    const hasDistances = res.body.data.some((item) => item.distanceKm !== null);
    assertEqual('Items have calculated distanceKm', hasDistances, true);
  } catch (err) {
    console.error('✗ FAIL: coordinate search error:', err.message);
    failed++;
  }

  console.log(`\n========================================`);
  console.log(`Search Test Results: ${passed} passed, ${failed} failed`);
  console.log(`========================================\n`);

  server.close(() => {
    process.exit(failed > 0 ? 1 : 0);
  });
};

runSearchTests();
