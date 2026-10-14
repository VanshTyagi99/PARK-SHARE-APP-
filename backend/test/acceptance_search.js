const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const http = require('http');
const { admin } = require('../src/config/firebase');
const { connectDB } = require('../src/config/db');

// Stub verifyIdToken for test driver
admin.auth().verifyIdToken = async (token) => {
  if (token === 'TEST_DRIVER_TOKEN') {
    return { uid: 'driver_test_uid', email: 'driver@test.com' };
  }
  const err = new Error('Invalid token');
  err.code = 'auth/invalid-id-token';
  throw err;
};

const request = (endpoint) => {
  return new Promise((resolve, reject) => {
    const req = http.request(
      {
        hostname: '127.0.0.1',
        port: 5060,
        path: endpoint,
        method: 'GET',
        headers: { Authorization: 'Bearer TEST_DRIVER_TOKEN' }
      },
      (res) => {
        let data = '';
        res.on('data', (c) => (data += c));
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, body: JSON.parse(data) });
          } catch {
            resolve({ status: res.statusCode, body: data });
          }
        });
      }
    );
    req.on('error', reject);
    req.end();
  });
};

async function runAcceptance() {
  process.env.PORT = '5060';
  await connectDB();
  const app = require('../src/server');
  const server = app.listen(5060, '127.0.0.1');
  await new Promise((r) => setTimeout(r, 500));

  console.log('=== LIVE SEARCH API ACCEPTANCE AUDIT ===\n');

  // Check 1: Default Search (Active only, distanceKm is null)
  const r1 = await request('/api/parking/search');
  console.log(`[Check 1: Default Search] Status: ${r1.status}`);
  console.log(`  Count: ${r1.body.count}, Total: ${r1.body.total}`);
  const allActive = r1.body.data.every((i) => i.isActive === true);
  const allDistanceNull = r1.body.data.every((i) => i.distanceKm === null);
  console.log(`  All Active: ${allActive}`);
  console.log(`  Missing Coords Produces null distanceKm: ${allDistanceNull}`);

  // Check 2: Text Search
  const r2 = await request('/api/parking/search?query=Botanical');
  console.log(`\n[Check 2: Text Search ("Botanical")] Status: ${r2.status}`);
  console.log(`  Matched: ${r2.body.data.map((i) => i.title).join(', ')}`);

  // Check 3: Vehicle Filters
  const r3 = await request('/api/parking/search?vehicleType=Bike');
  console.log(`\n[Check 3: Vehicle Filter ("Bike")] Status: ${r3.status}`);
  console.log(`  Count: ${r3.body.count}`);
  const validBikes = r3.body.data.every((i) => ['Bike', 'Car + Bike'].includes(i.parkingType));
  console.log(`  All items accept Bike: ${validBikes}`);

  // Check 4: Price Filters
  const r4 = await request('/api/parking/search?maxPrice=25');
  console.log(`\n[Check 4: Price Filter (maxPrice=25)] Status: ${r4.status}`);
  console.log(`  Count: ${r4.body.count}`);
  const validPrices = r4.body.data.every((i) => i.pricePerHour <= 25);
  console.log(`  All items price <= 25: ${validPrices} (Prices: ${r4.body.data.map(i => i.pricePerHour).join(', ')})`);

  // Check 5: Coordinate Search & Proximity Sorting
  const r5 = await request('/api/parking/search?latitude=28.5708&longitude=77.3260&radiusKm=10');
  console.log(`\n[Check 5: Proximity Search (Sector 18 Noida)] Status: ${r5.status}`);
  console.log(`  Count within 10km: ${r5.body.count}`);
  console.log(`  Nearest Item: "${r5.body.data[0]?.title}" - Distance: ${r5.body.data[0]?.distanceKm} km`);
  // Verify ascending sort
  let isSorted = true;
  for (let k = 1; k < r5.body.data.length; k++) {
    if (r5.body.data[k].distanceKm < r5.body.data[k - 1].distanceKm) {
      isSorted = false;
      break;
    }
  }
  console.log(`  Ascending Proximity Sorted: ${isSorted}`);

  // Check 6: Invalid Parameters Rejection
  const r6a = await request('/api/parking/search?minPrice=-1');
  const r6b = await request('/api/parking/search?minPrice=50&maxPrice=20');
  const r6c = await request('/api/parking/search?latitude=95');
  const r6d = await request('/api/parking/search?longitude=-190');
  console.log(`\n[Check 6: Error Rejections]`);
  console.log(`  minPrice < 0: status ${r6a.status} (${r6a.body.message})`);
  console.log(`  minPrice > maxPrice: status ${r6b.status} (${r6b.body.message})`);
  console.log(`  latitude > 90: status ${r6c.status} (${r6c.body.message})`);
  console.log(`  longitude < -180: status ${r6d.status} (${r6d.body.message})`);

  // Check 7: DTO Structure Validation
  const sample = r1.body.data[0];
  const requiredKeys = ['_id', 'ownerFirebaseUid', 'title', 'address', 'parkingType', 'vehicleCompatibility', 'pricePerHour', 'availableFrom', 'availableUntil', 'photos', 'latitude', 'longitude', 'distanceKm', 'isActive'];
  const hasAllKeys = requiredKeys.every((k) => k in sample);
  console.log(`\n[Check 7: DTO Model Compliance]`);
  console.log(`  Response matches SearchParkingResponse: ${r1.body.success === true && Array.isArray(r1.body.data)}`);
  console.log(`  Item matches ParkingListingDto (All 14 fields present): ${hasAllKeys}`);

  console.log('\n=== AUDIT COMPLETED SUCCESSFULLY ===');
  server.close();
  process.exit(0);
}

runAcceptance().catch((err) => {
  console.error('Acceptance error:', err);
  process.exit(1);
});
