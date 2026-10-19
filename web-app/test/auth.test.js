const test = require('node:test');
const assert = require('node:assert/strict');
const { generateToken, verifyToken } = require('../src/middleware/auth');

test('Auth: generateToken and verifyToken', () => {
  const mockUser = {
    id: 42,
    username: 'testadmin',
    full_name: 'Test Administrator',
    role: 'ADMIN',
  };

  const token = generateToken(mockUser);
  assert.ok(token, 'Token string should be generated');

  const decoded = verifyToken(token);
  assert.ok(decoded, 'Token should be decoded successfully');
  assert.equal(decoded.id, 42);
  assert.equal(decoded.username, 'testadmin');
  assert.equal(decoded.fullName, 'Test Administrator');
  assert.equal(decoded.role, 'ADMIN');
});

test('Auth: verifyToken rejects corrupted or tampered token', () => {
  const invalidToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidpayload.invalidsignature';
  const decoded = verifyToken(invalidToken);
  assert.equal(decoded, null, 'Tampered token should return null');
});
