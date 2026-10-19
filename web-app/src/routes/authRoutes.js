const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const { query } = require('../db');
const { generateToken, authenticate } = require('../middleware/auth');

// POST /api/auth/login
router.post('/login', async (req, res, next) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.status(400).json({
        success: false,
        message: 'Username and password are required.',
      });
    }

    const cleanUsername = username.trim();
    const result = await query(
      'SELECT id, username, password, full_name, role FROM users WHERE username = $1',
      [cleanUsername]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({
        success: false,
        message: 'Invalid username or password.',
      });
    }

    const user = result.rows[0];
    let passwordMatches = false;

    // 1. Handle bcrypt hashes ($2a$, $2b$, $2y$)
    if (user.password.startsWith('$2a$') || user.password.startsWith('$2b$') || user.password.startsWith('$2y$')) {
      passwordMatches = await bcrypt.compare(password, user.password);
    } 
    // 2. Handle legacy salt:hash from Java application if present
    else if (user.password.includes(':')) {
      const crypto = require('crypto');
      const [salt, expectedHash] = user.password.split(':');
      const computedHash = crypto.createHash('sha256').update(salt + password).digest('hex');
      try {
        passwordMatches = crypto.timingSafeEqual(Buffer.from(expectedHash), Buffer.from(computedHash));
      } catch (e) {
        passwordMatches = false;
      }
    }
    // 3. Direct plaintext fallback (dev/testing)
    else {
      passwordMatches = user.password === password;
    }

    if (!passwordMatches) {
      return res.status(401).json({
        success: false,
        message: 'Invalid username or password.',
      });
    }

    const token = generateToken(user);

    // Set secure HTTP-only cookie
    res.cookie('token', token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: 24 * 60 * 60 * 1000, // 24 hours
    });

    res.json({
      success: true,
      message: 'Login successful.',
      token,
      user: {
        id: user.id,
        username: user.username,
        fullName: user.full_name,
        role: user.role,
      },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/logout
router.post('/logout', (req, res) => {
  res.clearCookie('token');
  res.json({
    success: true,
    message: 'Logged out successfully.',
  });
});

// GET /api/auth/me
router.get('/me', authenticate, (req, res) => {
  res.json({
    success: true,
    user: req.user,
  });
});

// GET /api/auth/demo-credentials
router.get('/demo-credentials', (req, res) => {
  const adminUser = (process.env.DEMO_USERNAME || 'admin').trim();
  const adminPass = (process.env.DEMO_PASSWORD || 'admin123').trim();

  res.json({
    success: true,
    accounts: {
      admin: {
        role: 'Administrator',
        username: adminUser,
        password: adminPass,
        description: 'Full access to all 9 hotel modules, staff management, and system settings.',
      },
      receptionist: {
        role: 'Receptionist',
        username: 'receptionist',
        password: 'recep123',
        description: 'Front desk operations (Dashboard, Bookings, Check-In, Check-Out, Guests, Rooms, Reports).',
      },
    },
  });
});

module.exports = router;
