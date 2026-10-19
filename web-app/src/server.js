const express = require('express');
const path = require('path');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const helmet = require('helmet');
const morgan = require('morgan');
require('dotenv').config();

const { pool, initDatabase } = require('./db');
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

// Health check endpoint
app.get('/api/health', async (req, res) => {
  let dbStatus = 'DISCONNECTED';
  try {
    const dbRes = await pool.query('SELECT NOW()');
    if (dbRes.rows.length > 0) {
      dbStatus = 'CONNECTED';
    }
  } catch (err) {
    dbStatus = `ERROR: ${err.message}`;
  }

  res.json({
    status: 'UP',
    database: dbStatus,
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'development',
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

// Start server after database initialization
async function startServer() {
  try {
    await initDatabase();
  } catch (err) {
    console.error('⚠️ Database initialization warning (server will still start):', err.message);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`====================================================`);
    console.log(`🏨 Grand Palace Hotel Management System is LIVE!`);
    console.log(`🌐 Local URL:  http://localhost:${PORT}`);
    console.log(`🚀 Network:    http://0.0.0.0:${PORT}`);
    console.log(`📊 Mode:       ${process.env.NODE_ENV || 'development'}`);
    console.log(`====================================================`);
  });
}

// If executed directly, run the server
if (require.main === module) {
  startServer();
}

module.exports = { app, startServer };
