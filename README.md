# Grand Palace Hotel Management System 🏨

[![Render Deployment](https://img.shields.io/badge/Render-Web%20Service-46E3B7?style=for-the-badge&logo=render&logoColor=white)](DEPLOYMENT.md)
[![Neon](https://img.shields.io/badge/Database-Neon%20PostgreSQL-00E599?style=for-the-badge&logo=postgresql&logoColor=white)](https://neon.tech/)
[![Node.js](https://img.shields.io/badge/Node.js-Express%20API-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Swing](https://img.shields.io/badge/GUI-Java%20Swing-5382a1?style=for-the-badge&logo=java&logoColor=white)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Build-Maven%203.9-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

A complete, production-grade **Hotel Management System** providing dual deployment targets:
1. **🌐 Cloud Web Service (Node.js + Express + Neon PostgreSQL)**: Zero-installation browser application deployed to Render for instant sharing with interviewers and recruiters.
2. **🖥️ Native Desktop Application (Core Java + Java Swing + JDBC + MySQL)**: Desktop GUI application built with FlatLaf and multi-layer enterprise architecture.

> 📖 **Deploying to Render?** Read the complete [Render & Neon Deployment Guide](DEPLOYMENT.md).

---

## 🌟 Key Features

- **🔐 Secure Authentication & RBAC**:
  - Cryptographic password hashing using salted **SHA-256**.
  - Distinct access tiers for **ADMIN** and **RECEPTIONIST**.
  - Unauthorized modules (e.g. Employee Payroll & Staff Management) are dynamically hidden/disabled for Receptionists.

- **📊 Dynamic Analytics Dashboard**:
  - Live metric cards (Total Rooms, Available, Occupied, Reserved, Total Guests, Active Stays, Today's Check-ins, Today's Revenue).
  - Categorical breakdowns for Room Status and Room Inventory.
  - Interactive quick-action navigation shortcuts.
  - All statistics are computed on-the-fly using MySQL aggregate queries.

- **🛏️ Comprehensive Room Management**:
  - Complete inventory management across floors (Single, Double, Deluxe, Suite).
  - Strict room number uniqueness enforcement.
  - Color-coded live status badges (`AVAILABLE`, `RESERVED`, `OCCUPIED`, `MAINTENANCE`).
  - Active reservation deletion guard preventing accidental removal of occupied rooms.

- **👥 Guest & Customer Management**:
  - Complete CRUD operations with search filtering across Name, Phone, Email, and ID Proof.
  - Multi-document identity proof support (Aadhaar, Passport, Driving License, Voter ID).
  - Validation guards for phone digits and email formatting.

- **👔 Employee & Payroll Management (Admin Only)**:
  - Department management (Manager, Receptionist, Housekeeping, Security, Driver, Accountant).
  - Salary, status (`ACTIVE`, `INACTIVE`, `ON_LEAVE`), and joining date tracking.

- **📅 Reservation & Booking Workflow**:
  - Room picker automatically filtered to currently `AVAILABLE` units.
  - Automated stay duration (nights) calculation and live price estimation.
  - Automatic status transition: Room moves to `RESERVED` upon booking creation.
  - Collision detection preventing double-booking of overlapping dates.

- **🛎️ Dedicated Front Desk Check-In**:
  - Dedicated queue of `RESERVED` guests awaiting arrival.
  - One-click key issue & room check-in: Booking transitions to `CHECKED_IN`, Room transitions to `OCCUPIED`.

- **💳 Atomic Transactional Check-Out & Settlement**:
  - Live itemized invoice calculation ($Nights \times Rate + Extra Charges + 12\% GST - Advance Paid$).
  - Multi-method settlement support (`CASH`, `CARD`, `UPI`, `BANK_TRANSFER`).
  - Strict validation preventing payment from exceeding outstanding balance.
  - **JDBC Transaction Safe**: Booking status (`CHECKED_OUT`), payment record creation, and room status release (`AVAILABLE`) commit together or roll back atomically.
  - Printable checkout receipt modal.

- **📈 Advanced SQL Reporting & Analytics**:
  - **Reservations Ledger**: Multi-table `JOIN` linking Bookings, Customers, Rooms, and Payments with date filtering.
  - **Revenue Analysis**: Aggregations using `GROUP BY`, `COUNT()`, `SUM()`, `AVG()`, `MIN()`, and `MAX()`.
  - **Room Utilization**: Grouped by room category with operational occupancy metrics.
  - **Department Payroll**: Staff headcount and salary distribution grouped by designation.
  - Native Swing table printing support (`JTable.print()`).

---

## 🏗️ Clean Layered Architecture

```text
hotel-management-system/
│
├── pom.xml                               # Maven project configuration
├── run.bat                               # Windows one-click desktop launcher
├── README.md                             # Comprehensive project documentation
├── .gitignore                            # Git exclusion rules (credentials, build targets)
├── db.properties.example                 # Safe credential template
│
├── database/
│   └── hotel_management.sql              # Database schema, indexes, FKs, and seed data
│
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── db.properties             # Local database credentials (ignored by Git)
    │   │   └── db.properties.example     # Template credentials
    │   │
    │   └── java/
    │       └── com/
    │           └── hotelmanagement/
    │               ├── Main.java         # Application entry point (FlatLaf setup & DB check)
    │               │
    │               ├── config/
    │               │   └── DatabaseConfig.java      # Configuration reader (env vars + properties)
    │               ├── database/
    │               │   └── DatabaseConnection.java  # JDBC connection factory & transaction manager
    │               ├── exception/
    │               │   ├── ApplicationException.java
    │               │   ├── DatabaseException.java
    │               │   └── ValidationException.java
    │               ├── model/
    │               │   ├── User.java                # Auth user entity
    │               │   ├── Customer.java            # Guest entity
    │               │   ├── Employee.java            # Staff payroll entity
    │               │   ├── Room.java                # Room inventory entity
    │               │   ├── Booking.java             # Reservation entity
    │               │   ├── Payment.java             # Financial ledger entity
    │               │   ├── BillDetails.java         # Itemized billing model
    │               │   └── DashboardStats.java      # Operational analytics model
    │               ├── dao/
    │               │   ├── UserDAO.java             # User queries
    │               │   ├── CustomerDAO.java         # Customer CRUD & search
    │               │   ├── EmployeeDAO.java         # Staff CRUD & department queries
    │               │   ├── RoomDAO.java             # Room inventory & status transitions
    │               │   ├── BookingDAO.java          # Relational JOIN queries & date checks
    │               │   ├── PaymentDAO.java          # Transactional payment recording
    │               │   └── ReportDAO.java           # Complex SQL: JOIN, GROUP BY, aggregations
    │               ├── service/
    │               │   ├── AuthService.java         # Auth, session, and role checking
    │               │   ├── CustomerService.java     # Customer validation & business rules
    │               │   ├── EmployeeService.java     # Staff validation & payroll rules
    │               │   ├── RoomService.java         # Room constraints & duplicate checks
    │               │   ├── BookingService.java      # Reservation rules & collision checks
    │               │   ├── CheckInOutService.java   # Atomic check-in and checkout transactions
    │               │   ├── PaymentService.java      # Balance validation & transaction logging
    │               │   └── ReportService.java       # Reporting & business intelligence
    │               ├── ui/
    │               │   ├── LoginFrame.java          # Authentication window
    │               │   ├── DashboardFrame.java      # Main window with sidebar navigation
    │               │   ├── DashboardPanel.java      # Metric cards & quick actions
    │               │   ├── CustomerPanel.java       # Guest management view
    │               │   ├── EmployeePanel.java       # Staff management view (Admin only)
    │               │   ├── RoomPanel.java           # Room inventory view
    │               │   ├── BookingPanel.java        # Reservation workflow view
    │               │   ├── CheckInPanel.java        # Front desk check-in view
    │               │   ├── CheckOutPanel.java       # Front desk check-out & billing view
    │               │   ├── PaymentPanel.java        # Financial transaction view
    │               │   └── ReportsPanel.java        # Analytical reports view
    │               └── util/
    │                   ├── DateUtil.java            # Date calculations and formatters
    │                   ├── PasswordUtil.java        # Salted SHA-256 cryptographic utility
    │                   ├── UIUtil.java              # Modern Flat styling & component helpers
    │                   └── ValidationUtil.java      # Strict regex and business validators
    │
    └── test/
        └── java/
            └── com/
                └── hotelmanagement/
                    ├── service/
                    │   ├── BusinessLogicTest.java            # Billing formulas & night calculation tests
                    │   └── SystemWorkflowIntegrationTest.java# Full lifecycle tests against live MySQL
                    └── util/
                        ├── PasswordUtilTest.java             # Salted SHA-256 verification tests
                        └── ValidationUtilTest.java           # Input & parameter validation tests
```

---

## 🗄️ Database Design

### Entity-Relationship Diagram (ERD)

```text
       +-----------------------+
       |         USERS         |
       +-----------------------+
       | PK  id                |
       |     username (UNIQUE) |
       |     password (HASHED) |
       |     full_name         |
       |     role              |
       |     created_at        |
       +-----------+-----------+
                   |
                   | creates (FK)
                   v
+------------------+----+       +---------------------+
|       CUSTOMERS       |       |        ROOMS        |
+-----------------------+       +---------------------+
| PK  customer_id       |       | PK  room_id         |
|     full_name         |       |     room_number(UQ) |
|     gender            |       |     room_type       |
|     date_of_birth     |       |     floor           |
|     phone             |       |     price_per_night |
|     email             |       |     status          |
|     address           |       |     description     |
|     id_type, id_number|       +----------+----------+
+-----------+-----------+                  |
            |                              |
            | reserves (FK)                | booked in (FK)
            |                              |
            v                              v
       +----+------------------------------+----+
       |                BOOKINGS                |
       +----------------------------------------+
       | PK  booking_id                         |
       | FK  customer_id                        |
       | FK  room_id                            |
       |     booking_date                       |
       |     check_in_date                      |
       |     expected_check_out_date            |
       |     actual_check_out_date              |
       |     number_of_guests                   |
       |     booking_status                     |
       |     special_request                    |
       | FK  created_by                         |
       +-------------------+--------------------+
                           |
                           | settled by (FK)
                           v
       +-------------------+--------------------+
       |                PAYMENTS                |
       +----------------------------------------+
       | PK  payment_id                         |
       | FK  booking_id                         |
       | FK  customer_id                        |
       |     amount                             |
       |     payment_date                       |
       |     payment_method                     |
       |     payment_status                     |
       |     transaction_reference              |
       +----------------------------------------+
```

---

## 🛠️ Technology Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java (JDK 17+) | Core Java, OOP, Generics, Lambdas |
| **GUI Framework** | Java Swing + FlatLaf 3.5 | Modern Flat UI Look & Feel |
| **Persistence** | JDBC (`mysql-connector-j`) | Direct PreparedStatement SQL Execution |
| **Database** | MySQL 8.0+ | Relational schema with foreign keys & indexes |
| **Build Tool** | Apache Maven 3.9+ | Dependency resolution & packaging |
| **Testing** | JUnit 5 Jupiter | Unit tests & end-to-end integration tests |

---

## 📋 Prerequisites

Ensure the following tools are installed on your workstation:
1. **Java Development Kit (JDK) 17** or higher (`java -version`).
2. **Apache Maven 3.9+** (`mvn -version`).
3. **MySQL Server 8.0+** running locally on port 3306.

---

## 🚀 Installation & Setup

### Step 1: Clone Repository
```bash
git clone https://github.com/Sejuu11/Hotel-Management-System.git
cd Hotel-Management-System
```

### Step 2: Set Up the Database
Run the bundled SQL setup script into your local MySQL server:
```bash
# Using MySQL Command Line Client:
mysql -u root -p < database/hotel_management.sql
```
This script will:
- Create the database `hotel_management`.
- Create tables with strict constraints, indexes, and foreign keys.
- Populate initial realistic seed data: 2 users, 12 rooms, 6 customers, 6 employees, 4 bookings, and 4 payments.

### Step 3: Configure Database Credentials
Create your local `src/main/resources/db.properties` from the example template:
```properties
DB_URL=jdbc:mysql://localhost:3306/hotel_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASSWORD=your_mysql_password
```
*(Note: `db.properties` is automatically ignored by Git to keep credentials secure).*

You may also configure via environment variables:
```bash
set DB_URL=jdbc:mysql://localhost:3306/hotel_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
set DB_USER=root
set DB_PASSWORD=your_mysql_password
```

### Step 4: Build and Test
Execute the test suite and package the standalone executable:
```bash
mvn clean test
mvn clean package
```

---

## 🔑 Default Demo Credentials

The SQL setup script seeds the following demo accounts:

| Role | Username | Password | Access Privileges |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin` | `admin123` | Full access to all modules including Staff Payroll |
| **Front Desk Receptionist** | `receptionist` | `recep123` | Front Desk, Rooms, Guests, Bookings, Billing, Reports |

*(Passwords are stored as salted SHA-256 hashes inside MySQL).*

---

## 💻 Running the Application

### Option A: Using Windows Launcher Script
Double-click `run.bat` or execute in PowerShell / CMD:
```cmd
run.bat
```

### Option B: Using the Packaged Executable JAR
```bash
java -jar target/hotel-management-system-1.0.0-jar-with-dependencies.jar
```

### Option C: Using Maven Exec Plugin
```bash
mvn exec:java
```

---

## 🔄 Room & Booking Lifecycle Workflow

```text
                   [ Create Booking ]
 [ AVAILABLE ] --------------------------> [ RESERVED ]
       ^                                         |
       |                                         | [ Check-In ]
       |                                         v
       |                                   [ OCCUPIED ]
       |                                         |
       |            [ Check-Out ]                |
       +-----------------------------------------+
               (Settle Balance & Release)
```

1. **Reservation**: Front desk selects an available room and guest $\rightarrow$ Room status becomes `RESERVED`.
2. **Check-In**: On arrival, guest identity is verified $\rightarrow$ Booking becomes `CHECKED_IN`, Room becomes `OCCUPIED`.
3. **Check-Out**: System compiles nights, extra charges, taxes, and subtracts prior deposits $\rightarrow$ Balance is settled via cash/card/UPI $\rightarrow$ In a single JDBC transaction, payment is recorded, booking marked `CHECKED_OUT`, and room released back to `AVAILABLE`.

---

## 🧪 Automated Test Results

The application includes a comprehensive test suite covering utilities, business logic, and live database workflows:

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.hotelmanagement.service.BusinessLogicTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.hotelmanagement.service.SystemWorkflowIntegrationTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.hotelmanagement.util.PasswordUtilTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.hotelmanagement.util.ValidationUtilTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 🔮 Future Enhancements

- 📄 Automatic PDF Invoice generation using iText or OpenPDF.
- ✉️ Automated guest booking confirmation emails and SMS gateway integration.
- 📊 Advanced interactive charts (JFreeChart) for occupancy rate trends.
- 🛎️ Housekeeping room cleaning checklist workflow integration.
- 🌐 Optional REST API bridge for online guest mobile check-in.

---

## 📄 License
This project is licensed under the MIT License.
