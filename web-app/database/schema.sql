-- ====================================================================
-- HOTEL MANAGEMENT SYSTEM - POSTGRESQL SCHEMA FOR NEON / RENDER
-- ====================================================================

-- 1. Table: users
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'RECEPTIONIST' CHECK (role IN ('ADMIN', 'RECEPTIONIST')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);

-- 2. Table: customers
CREATE TABLE IF NOT EXISTS customers (
    customer_id SERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    date_of_birth DATE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address VARCHAR(255),
    id_type VARCHAR(50) NOT NULL,
    id_number VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_customer_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customer_name ON customers(full_name);

-- 3. Table: employees
CREATE TABLE IF NOT EXISTS employees (
    employee_id SERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address VARCHAR(255),
    designation VARCHAR(50) NOT NULL,
    salary DECIMAL(10,2) NOT NULL,
    joining_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);
CREATE INDEX IF NOT EXISTS idx_employee_designation ON employees(designation);
CREATE INDEX IF NOT EXISTS idx_employee_status ON employees(status);

-- 4. Table: rooms
CREATE TABLE IF NOT EXISTS rooms (
    room_id SERIAL PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type VARCHAR(20) NOT NULL CHECK (room_type IN ('Single', 'Double', 'Deluxe', 'Suite')),
    floor INT NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED', 'MAINTENANCE')),
    description VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_room_number ON rooms(room_number);
CREATE INDEX IF NOT EXISTS idx_room_status ON rooms(status);
CREATE INDEX IF NOT EXISTS idx_room_type ON rooms(room_type);

-- 5. Table: bookings
CREATE TABLE IF NOT EXISTS bookings (
    booking_id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    room_id INT NOT NULL REFERENCES rooms(room_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    booking_date DATE NOT NULL DEFAULT CURRENT_DATE,
    check_in_date DATE NOT NULL,
    expected_check_out_date DATE NOT NULL,
    actual_check_out_date DATE,
    number_of_guests INT NOT NULL DEFAULT 1,
    booking_status VARCHAR(20) NOT NULL DEFAULT 'RESERVED' CHECK (booking_status IN ('RESERVED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    special_request VARCHAR(255),
    created_by INT REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_booking_status ON bookings(booking_status);
CREATE INDEX IF NOT EXISTS idx_booking_dates ON bookings(check_in_date, expected_check_out_date);

-- 6. Table: payments
CREATE TABLE IF NOT EXISTS payments (
    payment_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE ON UPDATE CASCADE,
    customer_id INT NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'CARD', 'UPI', 'BANK_TRANSFER')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID' CHECK (payment_status IN ('PAID', 'PARTIAL', 'REFUNDED', 'PENDING')),
    transaction_reference VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_payment_booking ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payment_date ON payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(payment_status);
