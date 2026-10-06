-- ====================================================================
-- HOTEL MANAGEMENT SYSTEM - DATABASE SCHEMA & SEED DATA
-- Database: hotel_management
-- Compatible with MySQL 8.0+
-- ====================================================================

DROP DATABASE IF EXISTS hotel_management;
CREATE DATABASE hotel_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_management;

-- --------------------------------------------------------------------
-- 1. Table: users
-- Authentication and role-based access control (ADMIN / RECEPTIONIST)
-- --------------------------------------------------------------------
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'RECEPTIONIST') NOT NULL DEFAULT 'RECEPTIONIST',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------------
-- 2. Table: customers
-- Hotel guests with contact and identity proof details
-- --------------------------------------------------------------------
CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    date_of_birth DATE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address VARCHAR(255),
    id_type VARCHAR(50) NOT NULL,
    id_number VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_phone (phone),
    INDEX idx_customer_name (full_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------------
-- 3. Table: employees
-- Hotel staff management, payroll, and department designations
-- --------------------------------------------------------------------
CREATE TABLE employees (
    employee_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address VARCHAR(255),
    designation VARCHAR(50) NOT NULL,
    salary DECIMAL(10,2) NOT NULL,
    joining_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    INDEX idx_employee_designation (designation),
    INDEX idx_employee_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------------
-- 4. Table: rooms
-- Room inventory, categorization, pricing, and live availability
-- --------------------------------------------------------------------
CREATE TABLE rooms (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type ENUM('Single', 'Double', 'Deluxe', 'Suite') NOT NULL,
    floor INT NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    status ENUM('AVAILABLE', 'RESERVED', 'OCCUPIED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    description VARCHAR(255),
    INDEX idx_room_number (room_number),
    INDEX idx_room_status (status),
    INDEX idx_room_type (room_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------------
-- 5. Table: bookings
-- Guest room reservations, check-in/out scheduling, and status lifecycle
-- --------------------------------------------------------------------
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    room_id INT NOT NULL,
    booking_date DATE NOT NULL,
    check_in_date DATE NOT NULL,
    expected_check_out_date DATE NOT NULL,
    actual_check_out_date DATE,
    number_of_guests INT NOT NULL DEFAULT 1,
    booking_status ENUM('RESERVED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') NOT NULL DEFAULT 'RESERVED',
    special_request VARCHAR(255),
    created_by INT,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_booking_status (booking_status),
    INDEX idx_booking_dates (check_in_date, expected_check_out_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------------
-- 6. Table: payments
-- Financial transactions, payment methods, receipts, and settlements
-- --------------------------------------------------------------------
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    customer_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method ENUM('CASH', 'CARD', 'UPI', 'BANK_TRANSFER') NOT NULL,
    payment_status ENUM('PAID', 'PARTIAL', 'REFUNDED', 'PENDING') NOT NULL DEFAULT 'PAID',
    transaction_reference VARCHAR(100),
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_payment_booking (booking_id),
    INDEX idx_payment_date (payment_date),
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====================================================================
-- SEED DATA INSERTIONS
-- ====================================================================

-- 1. Default system users:
-- admin: password "admin123" (salt:hash)
-- receptionist: password "recep123" (salt:hash)
INSERT INTO users (username, password, full_name, role) VALUES
('admin', 'a1b2c3d4e5f67890:6fcf2c472dbf4cf3fb36657e173743e5a2345489da639f38263f5f4a2234ad1a', 'System Administrator', 'ADMIN'),
('receptionist', 'b2c3d4e5f6a17890:2920d0341c92dbe01b57b295fe0e56654a93037f0742a7827e5d5112d9fa8166', 'Front Desk Officer', 'RECEPTIONIST');

-- 2. Sample rooms across 3 floors
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
('304', 'Single', 3, 1600.00, 'AVAILABLE', 'Quiet single room on top floor, mountain facing');

-- 3. Sample customers
INSERT INTO customers (full_name, gender, date_of_birth, phone, email, address, id_type, id_number) VALUES
('Aarav Sharma', 'Male', '1992-05-14', '9876543210', 'aarav.sharma@example.com', 'Flat 402, Green Glen Layout, Bangalore', 'Aadhaar', '4589-1234-5678'),
('Priya Patel', 'Female', '1995-11-20', '9823456781', 'priya.patel@example.com', '12 SV Road, Bandra West, Mumbai', 'Passport', 'Z1234567'),
('Rahul Verma', 'Male', '1988-03-08', '9811223344', 'rahul.verma@example.com', '78 Model Town, Phase 2, New Delhi', 'Driving License', 'DL-0420110012345'),
('Ananya Iyer', 'Female', '1996-09-18', '9733445566', 'ananya.iyer@example.com', '25 Anna Nagar Main Rd, Chennai', 'Voter ID', 'ABC1234567'),
('Vikram Singh', 'Male', '1985-07-25', '9944556677', 'vikram.singh@example.com', '34 Civil Lines, Jaipur', 'Aadhaar', '7890-3456-1234'),
('Sneha Kulkarni', 'Female', '1994-12-02', '9866778899', 'sneha.kulkarni@example.com', '88 FC Road, Shivaji Nagar, Pune', 'Passport', 'M9876543');

-- 4. Sample employees
INSERT INTO employees (full_name, gender, phone, email, address, designation, salary, joining_date, status) VALUES
('Rajesh Khanna', 'Male', '9899001122', 'rajesh.manager@hotel.com', 'Sector 15, Gurgaon', 'Manager', 75000.00, '2021-01-15', 'ACTIVE'),
('Sunita Rao', 'Female', '9899001133', 'sunita.frontdesk@hotel.com', 'Indiranagar, Bangalore', 'Receptionist', 32000.00, '2022-04-10', 'ACTIVE'),
('Ramesh Yadav', 'Male', '9899001144', 'ramesh.hk@hotel.com', 'Kalyan Nagar, Bangalore', 'Housekeeping', 20000.00, '2022-08-01', 'ACTIVE'),
('Balwant Singh', 'Male', '9899001155', 'balwant.sec@hotel.com', 'HSR Layout, Bangalore', 'Security', 22000.00, '2021-11-20', 'ACTIVE'),
('Kavita Joshi', 'Female', '9899001166', 'kavita.accounts@hotel.com', 'Whitefield, Bangalore', 'Accountant', 45000.00, '2021-06-01', 'ACTIVE'),
('Dinesh Kumar', 'Male', '9899001177', 'dinesh.transport@hotel.com', 'BTM Layout, Bangalore', 'Driver', 24000.00, '2023-02-15', 'ACTIVE');

-- 5. Sample bookings
-- Booking 1: RESERVED for Room 102
INSERT INTO bookings (customer_id, room_id, booking_date, check_in_date, expected_check_out_date, actual_check_out_date, number_of_guests, booking_status, special_request, created_by)
VALUES (1, 2, CURDATE(), CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY), NULL, 1, 'RESERVED', 'Late check-in requested (after 8 PM)', 1);

-- Booking 2: CHECKED_IN for Room 103
INSERT INTO bookings (customer_id, room_id, booking_date, check_in_date, expected_check_out_date, actual_check_out_date, number_of_guests, booking_status, special_request, created_by)
VALUES (2, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY), NULL, 2, 'CHECKED_IN', 'Extra pillows and non-smoking room', 2);

-- Booking 3: CHECKED_OUT for Room 202 (past completed booking)
INSERT INTO bookings (customer_id, room_id, booking_date, check_in_date, expected_check_out_date, actual_check_out_date, number_of_guests, booking_status, special_request, created_by)
VALUES (3, 6, DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, 'CHECKED_OUT', 'Airport pick-up requested', 1);

-- Booking 4: CHECKED_IN for Room 203
INSERT INTO bookings (customer_id, room_id, booking_date, check_in_date, expected_check_out_date, actual_check_out_date, number_of_guests, booking_status, special_request, created_by)
VALUES (4, 7, CURDATE(), CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 DAY), NULL, 2, 'CHECKED_IN', 'High floor quiet room', 2);

-- 6. Sample payments
-- Payment for Booking 1 (Advance payment)
INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
VALUES (1, 1, 1500.00, NOW(), 'UPI', 'PARTIAL', 'UPI/20261005/982341');

-- Payment for Booking 2 (Advance payment)
INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
VALUES (2, 2, 2400.00, DATE_SUB(NOW(), INTERVAL 1 DAY), 'CARD', 'PARTIAL', 'TXN-CARD-492041');

-- Payment for Booking 3 (Full settlement at checkout: 3 nights * 3800 = 11400)
INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
VALUES (3, 3, 11400.00, DATE_SUB(NOW(), INTERVAL 2 DAY), 'BANK_TRANSFER', 'PAID', 'NEFT-8849201948');

-- Payment for Booking 4 (Advance payment)
INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
VALUES (4, 4, 3800.00, NOW(), 'CASH', 'PARTIAL', 'CASH-REC-00104');
