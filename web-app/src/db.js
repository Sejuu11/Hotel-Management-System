const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const connectionString = process.env.DATABASE_URL;

let poolConfig = {
  connectionString: connectionString,
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
};

if (connectionString) {
  // Always use SSL for remote PostgreSQL hosts like Neon
  const isLocal = connectionString.includes('localhost') || connectionString.includes('127.0.0.1');
  if (!isLocal) {
    poolConfig.ssl = { rejectUnauthorized: false };
  }
}

const pool = new Pool(poolConfig);

pool.on('error', (err) => {
  console.error('Unexpected error on idle PostgreSQL client:', err);
});

async function query(text, params) {
  const start = Date.now();
  const res = await pool.query(text, params);
  const duration = Date.now() - start;
  if (process.env.NODE_ENV !== 'production') {
    // console.log('Executed query', { text: text.slice(0, 60), duration, rows: res.rowCount });
  }
  return res;
}

async function getClient() {
  const client = await pool.connect();
  return client;
}

/**
 * Initializes database schema and idempotent seed data.
 * Safe to run repeatedly; does NOT drop existing tables.
 */
async function initDatabase() {
  if (!process.env.DATABASE_URL) {
    console.warn('⚠️ DATABASE_URL is not set. Database initialization skipped.');
    return;
  }

  const client = await pool.connect();
  try {
    console.log('🔄 Verifying and initializing PostgreSQL database schema...');

    // 1. Run schema DDL
    const schemaPath = path.join(__dirname, '../database/schema.sql');
    if (fs.existsSync(schemaPath)) {
      const schemaSql = fs.readFileSync(schemaPath, 'utf8');
      await client.query(schemaSql);
      console.log('✅ PostgreSQL schema verified successfully.');
    }

    // 2. Check if users table needs seeding
    const userCountRes = await client.query('SELECT COUNT(*) AS count FROM users');
    const userCount = parseInt(userCountRes.rows[0].count, 10);

    if (userCount === 0) {
      console.log('🌱 Seeding initial demo data for first-time setup...');

      const adminUser = process.env.DEMO_USERNAME || 'admin';
      const adminPass = process.env.DEMO_PASSWORD || 'admin123';
      const adminHash = await bcrypt.hash(adminPass, 10);
      const recepHash = await bcrypt.hash('recep123', 10);

      // Seed Users
      await client.query(`
        INSERT INTO users (username, password, full_name, role) VALUES
        ($1, $2, 'System Administrator', 'ADMIN'),
        ('receptionist', $3, 'Front Desk Officer', 'RECEPTIONIST')
        ON CONFLICT (username) DO NOTHING
      `, [adminUser, adminHash, recepHash]);

      // Seed Rooms
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

      // Seed Customers
      await client.query(`
        INSERT INTO customers (full_name, gender, date_of_birth, phone, email, address, id_type, id_number) VALUES
        ('Aarav Sharma', 'Male', '1992-05-14', '9876543210', 'aarav.sharma@example.com', 'Flat 402, Green Glen Layout, Bangalore', 'Aadhaar', '4589-1234-5678'),
        ('Priya Patel', 'Female', '1995-11-20', '9823456781', 'priya.patel@example.com', '12 SV Road, Bandra West, Mumbai', 'Passport', 'Z1234567'),
        ('Rahul Verma', 'Male', '1988-03-08', '9811223344', 'rahul.verma@example.com', '78 Model Town, Phase 2, New Delhi', 'Driving License', 'DL-0420110012345'),
        ('Ananya Iyer', 'Female', '1996-09-18', '9733445566', 'ananya.iyer@example.com', '25 Anna Nagar Main Rd, Chennai', 'Voter ID', 'ABC1234567'),
        ('Vikram Singh', 'Male', '1985-07-25', '9944556677', 'vikram.singh@example.com', '34 Civil Lines, Jaipur', 'Aadhaar', '7890-3456-1234'),
        ('Sneha Kulkarni', 'Female', '1994-12-02', '9866778899', 'sneha.kulkarni@example.com', '88 FC Road, Shivaji Nagar, Pune', 'Passport', 'M9876543')
      `);

      // Seed Employees
      await client.query(`
        INSERT INTO employees (full_name, gender, phone, email, address, designation, salary, joining_date, status) VALUES
        ('Rajesh Khanna', 'Male', '9899001122', 'rajesh.manager@hotel.com', 'Sector 15, Gurgaon', 'Manager', 75000.00, '2021-01-15', 'ACTIVE'),
        ('Sunita Rao', 'Female', '9899001133', 'sunita.frontdesk@hotel.com', 'Indiranagar, Bangalore', 'Receptionist', 32000.00, '2022-04-10', 'ACTIVE'),
        ('Ramesh Yadav', 'Male', '9899001144', 'ramesh.hk@hotel.com', 'Kalyan Nagar, Bangalore', 'Housekeeping', 20000.00, '2022-08-01', 'ACTIVE'),
        ('Balwant Singh', 'Male', '9899001155', 'balwant.sec@hotel.com', 'HSR Layout, Bangalore', 'Security', 22000.00, '2021-11-20', 'ACTIVE'),
        ('Kavita Joshi', 'Female', '9899001166', 'kavita.accounts@hotel.com', 'Whitefield, Bangalore', 'Accountant', 45000.00, '2021-06-01', 'ACTIVE'),
        ('Dinesh Kumar', 'Male', '9899001177', 'dinesh.transport@hotel.com', 'BTM Layout, Bangalore', 'Driver', 24000.00, '2023-02-15', 'ACTIVE')
      `);

      // Seed Bookings
      await client.query(`
        INSERT INTO bookings (customer_id, room_id, booking_date, check_in_date, expected_check_out_date, actual_check_out_date, number_of_guests, booking_status, special_request, created_by)
        VALUES 
        (1, 2, CURRENT_DATE, CURRENT_DATE, CURRENT_DATE + INTERVAL '2 days', NULL, 1, 'RESERVED', 'Late check-in requested (after 8 PM)', 1),
        (2, 3, CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + INTERVAL '2 days', NULL, 2, 'CHECKED_IN', 'Extra pillows and non-smoking room', 2),
        (3, 6, CURRENT_DATE - INTERVAL '6 days', CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE - INTERVAL '2 days', 2, 'CHECKED_OUT', 'Airport pick-up requested', 1),
        (4, 7, CURRENT_DATE, CURRENT_DATE, CURRENT_DATE + INTERVAL '3 days', NULL, 2, 'CHECKED_IN', 'High floor quiet room', 2)
      `);

      // Seed Payments
      await client.query(`
        INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
        VALUES 
        (1, 1, 1500.00, CURRENT_TIMESTAMP, 'UPI', 'PARTIAL', 'UPI/20261019/982341'),
        (2, 2, 2400.00, CURRENT_TIMESTAMP - INTERVAL '1 day', 'CARD', 'PARTIAL', 'TXN-CARD-492041'),
        (3, 3, 11400.00, CURRENT_TIMESTAMP - INTERVAL '2 days', 'BANK_TRANSFER', 'PAID', 'NEFT-8849201948'),
        (4, 4, 3800.00, CURRENT_TIMESTAMP, 'CASH', 'PARTIAL', 'CASH-REC-00104')
      `);

      console.log('✅ Demo seed data inserted successfully.');
    } else {
      console.log(`ℹ️ Database already contains ${userCount} users. Preserving existing data.`);
    }
  } catch (err) {
    console.error('❌ Error initializing database:', err);
    throw err;
  } finally {
    client.release();
  }
}

module.exports = {
  pool,
  query,
  getClient,
  initDatabase,
};
