const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { app } = require('../src/server');

let server;
const TEST_PORT = 3099;
const BASE_URL = `http://127.0.0.1:${TEST_PORT}`;

test.before(async () => {
  await new Promise((resolve) => {
    server = app.listen(TEST_PORT, '127.0.0.1', resolve);
  });
});

test.after(async () => {
  if (server) {
    await new Promise((resolve) => server.close(resolve));
  }
});

test('API Integration: GET /api/health returns status UP', async () => {
  const res = await fetch(`${BASE_URL}/api/health`);
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.status, 'UP');
  assert.ok(data.timestamp);
  assert.ok(data.config);
  assert.equal(typeof data.config.hasDatabaseUrl, 'boolean');
  assert.equal(typeof data.config.hasJwtSecret, 'boolean');
  // Confirm no connection refusal to localhost is reported
  if (data.databaseError) {
    assert.doesNotMatch(data.databaseError, /ECONNREFUSED.*(127\.0\.0\.1|::1)/);
    assert.match(data.databaseError, /DATABASE_URL/);
  }
});

test('API Integration: GET /api/auth/demo-credentials returns demo roles', async () => {
  const res = await fetch(`${BASE_URL}/api/auth/demo-credentials`);
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.success, true);
  assert.ok(data.accounts);
  assert.equal(data.accounts.admin.role, 'Administrator');
  assert.equal(data.accounts.receptionist.role, 'Receptionist');
});


test('API Integration: POST /api/auth/login with missing inputs returns 400', async () => {
  const res = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({}),
  });
  assert.equal(res.status, 400);
  const data = await res.json();
  assert.equal(data.success, false);
  assert.match(data.message, /Username and password are required/);
});

test('Static Serving: GET / returns HTML SPA shell', async () => {
  const res = await fetch(`${BASE_URL}/`);
  assert.equal(res.status, 200);
  const text = await res.text();
  assert.match(text, /Grand Palace Hotel/);
  assert.match(text, /id="login-screen"/);
  assert.match(text, /id="app-screen"/);
});

test('Static Serving: GET /css/styles.css returns CSS stylesheet', async () => {
  const res = await fetch(`${BASE_URL}/css/styles.css`);
  assert.equal(res.status, 200);
  const text = await res.text();
  assert.match(text, /--bg-primary/);
});

test('Static Serving: GET /js/app.js returns client JavaScript', async () => {
  const res = await fetch(`${BASE_URL}/js/app.js`);
  assert.equal(res.status, 200);
  const text = await res.text();
  assert.match(text, /checkInitialSession/);
});

test('Database Safety: Unconfigured pool rejects queries with clear error and never connects to localhost', async () => {
  const { pool, isConfigured } = require('../src/db');
  if (!isConfigured) {
    await assert.rejects(
      async () => {
        await pool.query('SELECT 1');
      },
      (err) => {
        assert.equal(err.code, 'CONFIG_MISSING');
        assert.match(err.message, /DATABASE_URL environment variable is missing or empty/);
        return true;
      }
    );
  }
});
