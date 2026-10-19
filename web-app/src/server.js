const express = require('express');
const path = require('path');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const helmet = require('helmet');
const morgan = require('morgan');
const dotenv = require('dotenv');
dotenv.config();
dotenv.config({ path: path.join(__dirname, '../.env') });
dotenv.config({ path: path.join(__dirname, '../../.env') });

const { pool, initDatabase, isConfigured, getSafeConfigInfo } = require('./db');
const errorHandler = require('./middleware/errorHandler');

// Route handlers
const authRoutes = require('./routes/authRoutes');
const dashboardRoutes = require('./routes/dashboardRoutes');
const roomRoutes = require('./routes/roomRoutes');
const customerRoutes = require('./routes/customerRoutes');
const bookingRoutes = require('./routes/bookingRoutes');
const checkInOutRoutes = require('./routes/checkInOutRoutes');
const paymentRoutes = require('./routes/paymentRoutes');
const reportRoutes = require('./routes/reportRoutes');
const employeeRoutes = require('./routes/employeeRoutes');

const app = express();
const PORT = process.env.PORT || 3000;

// Security and utility middleware
app.use(helmet({
  contentSecurityPolicy: false, // Allow CDN resources (FontAwesome, fonts) without CSP friction
}));
app.use(cors({
  origin: true,
  credentials: true,
}));
app.use(cookieParser());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

if (process.env.NODE_ENV !== 'production') {
  app.use(morgan('dev'));
}

// Serve frontend static assets
app.use(express.static(path.join(__dirname, '../public')));

// Diagnostic health check endpoint
app.get('/api/health', async (req, res) => {
  let dbStatus = 'DISCONNECTED';
  let dbError = null;

  try {
    const dbRes = await pool.query('SELECT NOW() AS now, current_database() AS db');
    if (dbRes.rows.length > 0) {
      dbStatus = `CONNECTED (db: ${dbRes.rows[0].db})`;
    }
  } catch (err) {
    dbStatus = 'DISCONNECTED';
    dbError = err.message || (err.errors ? err.errors.map(e => e.message).join('; ') : err.code);
  }

  const safeDbInfo = getSafeConfigInfo();
  const hasJwt = Boolean(process.env.JWT_SECRET && process.env.JWT_SECRET.trim());

  res.json({
    status: 'UP',
    database: dbStatus,
    ...(dbError ? { databaseError: dbError } : {}),
    config: {
      hasDatabaseUrl: safeDbInfo.configured,
      hasJwtSecret: hasJwt,
      environment: process.env.NODE_ENV || 'development',
      dbHost: safeDbInfo.host,
      dbName: safeDbInfo.database,
      isPooled: safeDbInfo.isPooled,
    },
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/dashboard', dashboardRoutes);
app.use('/api/rooms', roomRoutes);
app.use('/api/customers', customerRoutes);
app.use('/api/bookings', bookingRoutes);
app.use('/api/checkin', checkInOutRoutes);
app.use('/api/checkout', checkInOutRoutes);
app.use('/api/payments', paymentRoutes);
app.use('/api/reports', reportRoutes);
app.use('/api/employees', employeeRoutes);

// Single Page Application client-side routing fallback
app.get('*', (req, res, next) => {
  if (req.path.startsWith('/api')) {
    return res.status(404).json({ success: false, message: 'API endpoint not found.' });
  }
  res.sendFile(path.join(__dirname, '../public/index.html'));
});

// Global error handler
app.use(errorHandler);

// Start server
async function startServer() {
  if (isConfigured) {
    // Trigger background database initialization without blocking port binding
    initDatabase().catch(err => {
      console.warn('⚠️ [Startup] Initial database connection note:', err.message);
    });
  } else {
    console.warn('⚠️ [Startup] DATABASE_URL is not configured. Database initialization skipped. Localhost fallback disabled.');
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`====================================================`);
    console.log(`🏨 Grand Palace Hotel Management System is LIVE!`);
    console.log(`🌐 Local URL:  http://localhost:${PORT}`);
    console.log(`🚀 Network:    http://0.0.0.0:${PORT}`);
    console.log(`📦 Database:   ${isConfigured ? 'Configured (Neon PostgreSQL)' : 'NOT CONFIGURED (DATABASE_URL required)'}`);
    console.log(`📊 Mode:       ${process.env.NODE_ENV || 'development'}`);
    console.log(`====================================================`);
  });
}

// If executed directly, run the server
if (require.main === module) {
  startServer();
}

module.exports = { app, startServer };
