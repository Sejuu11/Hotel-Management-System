const { Pool } = require('pg');
const parse = require('pg-connection-string').parse;
const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');

// Load environment variables from process.cwd() as well as web-app/ and project root
dotenv.config();
dotenv.config({ path: path.join(__dirname, '../.env') });
dotenv.config({ path: path.join(__dirname, '../../.env') });

const MISSING_CONFIG_MSG = 'DATABASE_URL environment variable is missing or empty. Please set DATABASE_URL in Render Dashboard > Environment.';

/**
 * Searches process.env for DATABASE_URL with tolerance for common alias names
 * and whitespace in keys or values.
 */
function getRawDatabaseUrl() {
  if (process.env.DATABASE_URL && typeof process.env.DATABASE_URL === 'string' && process.env.DATABASE_URL.trim()) {
    return process.env.DATABASE_URL.trim();
  }

  const validKeys = ['database_url', 'postgres_url', 'postgresql_url', 'neon_database_url'];
  for (const [key, val] of Object.entries(process.env)) {
    if (validKeys.includes(key.trim().toLowerCase())) {
      if (val && typeof val === 'string' && val.trim().length > 0) {
        return val.trim();
      }
    }
  }

  return '';
}

/**
 * Sanitizes connection string:
 * - Strips enclosing quotes ("...", '...', `...`)
 * - Strips leading variable assignment (DATABASE_URL=...)
 * - Strips CLI psql invocation prefix (psql '...')
 */
function sanitizeDatabaseUrl(raw) {
  if (!raw || typeof raw !== 'string') return '';
  let url = raw.trim();

  // Strip enclosing quotes
  url = url.replace(/^["'`]|["'`]$/g, '').trim();

  // Strip leading variable assignment
  if (/^database_url\s*=\s*/i.test(url)) {
    url = url.replace(/^database_url\s*=\s*/i, '').trim();
    url = url.replace(/^["'`]|["'`]$/g, '').trim();
  }

  // Strip psql prefix if copied from CLI tab in Neon Connect
  if (/^psql\s+/i.test(url)) {
    url = url.replace(/^psql\s+/i, '').trim();
    url = url.replace(/^["'`]|["'`]$/g, '').trim();
  }

  return url;
}

/**
 * Parses connection string into explicit Pool config.
 * Prohibits localhost fallback in production.
 */
function parseDatabaseConfig(cleanUrl) {
  if (!cleanUrl) return null;

  let host;
  let port = 5432;
  let user;
  let password;
  let database;

  // 1. Try WHATWG URL parser
  try {
    const normalized = cleanUrl.replace(/^postgres:\/\//i, 'postgresql://');
    const u = new URL(normalized);
    host = u.hostname;
    port = parseInt(u.port, 10) || 5432;
    user = u.username ? decodeURIComponent(u.username) : undefined;
    password = u.password ? decodeURIComponent(u.password) : undefined;
    database = (u.pathname || '').replace(/^\//, '') || undefined;
  } catch (err) {
    // 2. Fall back to pg-connection-string
    try {
      const parsed = parse(cleanUrl);
      host = parsed.host;
      port = parseInt(parsed.port, 10) || 5432;
      user = parsed.user;
      password = parsed.password;
      database = parsed.database;
    } catch (e2) {
      // 3. Fall back to regex
      const m = cleanUrl.match(/postgres(?:ql)?:\/\/([^:]+):([^@]+)@([^:/]+)(?::(\d+))?\/([^?]+)/);
      if (m) {
        user = decodeURIComponent(m[1]);
        password = decodeURIComponent(m[2]);
        host = m[3];
        port = parseInt(m[4], 10) || 5432;
        database = m[5];
      }
    }
  }

  if (!host) {
    return null;
  }

  // Clean individual fields of leftover quotes
  if (host) host = host.replace(/^["']|["']$/g, '').trim();
  if (database) database = database.replace(/^["']|["']$/g, '').trim();
  if (user) user = user.replace(/^["']|["']$/g, '').trim();
  if (password) password = password.replace(/^["']|["']$/g, '').trim();

  // In production, prohibit connecting to localhost
  const isProduction = process.env.NODE_ENV === 'production';
  const isLocalHost = host === 'localhost' || host === '127.0.0.1' || host === '::1';
  if (isProduction && isLocalHost) {
    throw new Error('Localhost PostgreSQL connection is prohibited in production. Please set Render process.env.DATABASE_URL.');
  }

  // SSL config: for remote hosts (Neon, etc.), enforce rejectUnauthorized: false
  const ssl = isLocalHost ? false : { rejectUnauthorized: false };

  return {
    host,
    port,
    user,
    password,
    database,
    ssl,
    max: 10,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 15000,
  };
}

/**
 * Dummy pool used when DATABASE_URL is missing.
 * Prevents node-postgres from silently falling back to localhost:5432.
 */
class UnconfiguredPool {
  query() {
    const err = new Error(MISSING_CONFIG_MSG);
    err.code = 'CONFIG_MISSING';
    return Promise.reject(err);
  }
  connect() {
    const err = new Error(MISSING_CONFIG_MSG);
    err.code = 'CONFIG_MISSING';
    return Promise.reject(err);
  }
  on() {
    return this;
  }
  end() {
    return Promise.resolve();
  }
}

const rawUrl = getRawDatabaseUrl();
const cleanUrl = sanitizeDatabaseUrl(rawUrl);
const dbConfig = parseDatabaseConfig(cleanUrl);
const isConfigured = Boolean(dbConfig);

const pool = isConfigured ? new Pool(dbConfig) : new UnconfiguredPool();

if (isConfigured && typeof pool.on === 'function') {
  pool.on('error', (err) => {
    const errMsg = err.message || (err.errors ? err.errors.map(e => e.message).join('; ') : err.code);
    console.error('⚠️ [DB Pool Error]:', errMsg);
  });
}

function getSafeConfigInfo() {
  if (!dbConfig) {
    return {
      configured: false,
      host: null,
      database: null,
      isPooled: false,
      ssl: false,
    };
  }
  return {
    configured: true,
    host: dbConfig.host || null,
    database: dbConfig.database || null,
    isPooled: Boolean(dbConfig.host && dbConfig.host.includes('-pooler')),
    ssl: Boolean(dbConfig.ssl),
  };
}

let isInitialized = false;
let initPromise = null;

async function query(text, params) {
  if (!isConfigured) {
    const err = new Error(MISSING_CONFIG_MSG);
    err.code = 'CONFIG_MISSING';
    throw err;
  }

  if (!isInitialized) {
    try {
      await initDatabase();
    } catch (e) {
      // Continue to let query execute or report error
    }
  }

  const start = Date.now();
  const res = await pool.query(text, params);
  const duration = Date.now() - start;
  return res;
}

async function getClient() {
  if (!isConfigured) {
    const err = new Error(MISSING_CONFIG_MSG);
    err.code = 'CONFIG_MISSING';
    throw err;
  }

  if (!isInitialized) {
    try {
      await initDatabase();
    } catch (e) {}
  }
  return pool.connect();
}

/**
 * Initializes database schema and idempotent seed data.
 * Features retry logic for serverless database cold-starts.
 */
async function initDatabase() {
  if (isInitialized) return true;
  if (initPromise) return initPromise;

  initPromise = (async () => {
    if (!isConfigured) {
      console.warn('⚠️ [DB] DATABASE_URL is not configured. Database initialization skipped. Localhost fallback disabled.');
      return false;
    }

    let client = null;
    let attempts = 3;

    // Retry loop for Neon Serverless compute endpoint cold-start
    while (attempts > 0) {
      try {
        client = await pool.connect();
        break;
      } catch (err) {
        attempts--;
        const errDetail = err.message || (err.errors ? err.errors.map(e => e.message).join('; ') : err.code);
        console.warn(`⏳ [DB] Connection attempt failed (${errDetail}). Retries left: ${attempts}...`);
        if (attempts === 0) {
          throw new Error(`Database connection failed after 3 attempts: ${errDetail}`);
        }
        await new Promise(res => setTimeout(res, 2000));
      }
    }

    try {
      console.log('🔄 [DB] Verifying/creating PostgreSQL tables and schema...');

      // 1. Run schema DDL
      const schemaPath = path.join(__dirname, '../database/schema.sql');
      if (fs.existsSync(schemaPath)) {
        const schemaSql = fs.readFileSync(schemaPath, 'utf8');
        await client.query(schemaSql);
        console.log('✅ [DB] PostgreSQL schema verified successfully.');
      }

      // 2. Ensure demo users exist
      const adminUser = (process.env.DEMO_USERNAME || 'admin').trim();
      const adminPass = (process.env.DEMO_PASSWORD || 'admin123').trim();
      const adminHash = await bcrypt.hash(adminPass, 10);
      const recepHash = await bcrypt.hash('recep123', 10);

      // Check if admin user exists
      const adminCheck = await client.query('SELECT id FROM users WHERE username = $1', [adminUser]);
      if (adminCheck.rows.length === 0) {
        console.log(`🌱 [DB] Creating demo admin user (${adminUser})...`);
        await client.query(
          `INSERT INTO users (username, password, full_name, role) VALUES ($1, $2, 'System Administrator', 'ADMIN') ON CONFLICT (username) DO NOTHING`,
          [adminUser, adminHash]
        );
      }

      // Check if receptionist user exists
      const recepCheck = await client.query("SELECT id FROM users WHERE username = 'receptionist'");
      if (recepCheck.rows.length === 0) {
        console.log('🌱 [DB] Creating demo receptionist user (receptionist)...');
        await client.query(
          `INSERT INTO users (username, password, full_name, role) VALUES ('receptionist', $1, 'Front Desk Officer', 'RECEPTIONIST') ON CONFLICT (username) DO NOTHING`,
          [recepHash]
        );
      }

      // 3. Seed rooms if empty
      const roomCheck = await client.query('SELECT COUNT(*) AS count FROM rooms');
      if (parseInt(roomCheck.rows[0].count, 10) === 0) {
        console.log('🌱 [DB] Seeding default hotel rooms...');
        await client.query(`
          INSERT INTO rooms (room_number, room_type, floor, price_per_night, status, description) VALUES
          ('101', 'Single', 1, 1500.00, 'AVAILABLE', 'Standard single room with garden view, AC, WiFi'),
          ('102', 'Single', 1, 1500.00, 'RESERVED', 'Standard single room with king bed and workstation'),
          ('103', 'Double', 1, 2400.00, 'OCCUPIED', 'Double room with attached balcony and mini fridge'),
          ('104', 'Double', 1, 2400.00, 'AVAILABLE', 'Spacious double bed room, TV, coffee maker'),
          ('201', 'Double', 2, 2600.00, 'AVAILABLE', 'Executive double room on 2nd floor, premium amenities'),
          ('202', 'Deluxe', 2, 3800.00, 'AVAILABLE', 'Deluxe room with panoramic pool view and king bed'),
          ('203', 'Deluxe', 2, 3800.00, 'OCCUPIED', 'Deluxe room with luxury bath and complimentary breakfast'),
          ('204', 'Deluxe', 2, 4000.00, 'AVAILABLE', 'Corner deluxe room with large windows and lounge area'),
          ('301', 'Suite',  3, 6500.00, 'AVAILABLE', 'Presidential suite with separate living room and jacuzzi'),
          ('302', 'Suite',  3, 6500.00, 'AVAILABLE', 'Royal suite with terrace access and 24/7 butler service'),
          ('303', 'Suite',  3, 7000.00, 'MAINTENANCE', 'Penthouse suite undergoing annual AC maintenance'),
          ('304', 'Single', 3, 1600.00, 'AVAILABLE', 'Quiet single room on top floor, mountain facing')
          ON CONFLICT (room_number) DO NOTHING
        `);
      }

      // 4. Seed customers if empty
      const custCheck = await client.query('SELECT COUNT(*) AS count FROM customers');
      if (parseInt(custCheck.rows[0].count, 10) === 0) {
        console.log('🌱 [DB] Seeding sample guests...');
        await client.query(`
          INSERT INTO customers (full_name, gender, date_of_birth, phone, email, address, id_type, id_number) VALUES
          ('Aarav Sharma', 'Male', '1992-05-14', '9876543210', 'aarav.sharma@example.com', 'Flat 402, Green Glen Layout, Bangalore', 'Aadhaar', '4589-1234-5678'),
          ('Priya Patel', 'Female', '1995-11-20', '9823456781', 'priya.patel@example.com', '12 SV Road, Bandra West, Mumbai', 'Passport', 'Z1234567'),
          ('Rahul Verma', 'Male', '1988-03-08', '9811223344', 'rahul.verma@example.com', '78 Model Town, Phase 2, New Delhi', 'Driving License', 'DL-0420110012345')
        `);
      }

      // 5. Seed employees if empty
      const empCheck = await client.query('SELECT COUNT(*) AS count FROM employees');
      if (parseInt(empCheck.rows[0].count, 10) === 0) {
        console.log('🌱 [DB] Seeding staff members...');
        await client.query(`
          INSERT INTO employees (full_name, gender, phone, email, address, designation, salary, joining_date, status) VALUES
          ('Rajesh Khanna', 'Male', '9899001122', 'rajesh.manager@hotel.com', 'Sector 15, Gurgaon', 'Manager', 75000.00, '2021-01-15', 'ACTIVE'),
          ('Sunita Rao', 'Female', '9899001133', 'sunita.frontdesk@hotel.com', 'Indiranagar, Bangalore', 'Receptionist', 32000.00, '2022-04-10', 'ACTIVE')
        `);
      }

      isInitialized = true;
      console.log('✅ [DB] Database initialization and seed verification complete.');
      return true;
    } catch (err) {
      console.error('❌ [DB] Error during schema/seed initialization:', err.message);
      throw err;
    } finally {
      if (client) client.release();
      initPromise = null;
    }
  })();

  return initPromise;
}

module.exports = {
  pool,
  query,
  getClient,
  initDatabase,
  isConfigured,
  getSafeConfigInfo,
};
