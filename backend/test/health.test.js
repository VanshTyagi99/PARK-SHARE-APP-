const http = require('http');

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
  // Start server in background for testing
  process.env.PORT = '5055';
  process.env.NODE_ENV = 'test';
  const app = require('../src/server');

  const server = app.listen(5055, '127.0.0.1');

  await new Promise((resolve) => setTimeout(resolve, 500));

  console.log('--- Running Backend Verification Suite ---');

  let passed = 0;
  let failed = 0;

  // Test 1: GET /api/health
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5055,
      path: '/api/health',
      method: 'GET'
    });

    if (res.status === 200 && res.body.status === 'ok') {
      console.log('✓ PASS: GET /api/health returned 200 OK (database: ' + res.body.database + ')');
      passed++;
    } else {
      console.error('✗ FAIL: GET /api/health expected 200, got:', res.status, res.body);
      failed++;
    }
  } catch (err) {
    console.error('✗ FAIL: GET /api/health error:', err.message);
    failed++;
  }

  // Test 2: Protected Route without token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5055,
      path: '/api/users/me',
      method: 'GET'
    });

    if (res.status === 401 && res.body.success === false) {
      console.log('✓ PASS: GET /api/users/me without token correctly rejected with 401 Unauthorized');
      passed++;
    } else {
      console.error('✗ FAIL: GET /api/users/me expected 401, got:', res.status, res.body);
      failed++;
    }
  } catch (err) {
    console.error('✗ FAIL: GET /api/users/me missing token error:', err.message);
    failed++;
  }

  // Test 3: Protected Route with malformed header -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5055,
      path: '/api/users/me',
      method: 'GET',
      headers: {
        Authorization: 'Basic not-a-bearer-token'
      }
    });

    if (res.status === 401 && res.body.message.includes('Expected format: Bearer')) {
      console.log('✓ PASS: Malformed Authorization header correctly rejected with 401');
      passed++;
    } else {
      console.error('✗ FAIL: Malformed header expected 401, got:', res.status, res.body);
      failed++;
    }
  } catch (err) {
    console.error('✗ FAIL: Malformed header error:', err.message);
    failed++;
  }

  // Test 4: Protected Route with invalid token -> 401
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5055,
      path: '/api/users/me',
      method: 'GET',
      headers: {
        Authorization: 'Bearer fake_invalid_jwt_token_12345'
      }
    });

    if (res.status === 401) {
      console.log('✓ PASS: Invalid Firebase token rejected with 401');
      passed++;
    } else {
      console.error('✗ FAIL: Invalid token expected 401, got:', res.status, res.body);
      failed++;
    }
  } catch (err) {
    console.error('✗ FAIL: Invalid token error:', err.message);
    failed++;
  }

  // Test 5: 404 for unknown route
  try {
    const res = await request({
      hostname: '127.0.0.1',
      port: 5055,
      path: '/api/unknown-endpoint',
      method: 'GET'
    });

    if (res.status === 404) {
      console.log('✓ PASS: Unknown route returned 404 Not Found');
      passed++;
    } else {
      console.error('✗ FAIL: Unknown route expected 404, got:', res.status);
      failed++;
    }
  } catch (err) {
    console.error('✗ FAIL: Unknown route error:', err.message);
    failed++;
  }

  console.log(`\nResults: ${passed} passed, ${failed} failed`);

  server.close(() => {
    process.exit(failed > 0 ? 1 : 0);
  });
};

runTests();
