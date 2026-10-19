# Render & Neon PostgreSQL Cloud Deployment Guide

This guide details how to deploy the **Grand Palace Hotel Management System** as a free web service on **Render** backed by a **Neon Serverless PostgreSQL** database.

---

## 1. Why the Previous Render Deployment Failed (`ENOENT: package.json not found`)

Render detects Node.js services by inspecting the repository root for a `package.json`. Because the original repository contained only Java Maven source code, Render's default root build command failed:
```text
ENOENT: package.json not found
```

### The Solution: Dual-Root Architecture
We introduced:
1. **Root `package.json`**: Provides root-level `npm install` and `npm start` commands that execute the web server seamlessly.
2. **`render.yaml`**: A Render Blueprint specifying exact commands and environment variable mappings.
3. **`web-app/` Directory**: Houses the complete full-stack web application (Express, PostgreSQL client, automated test suite, and responsive Single-Page Application).
4. **Preservation**: The original Core Java Swing application (`src/main/java`, `pom.xml`, test suite) remains 100% intact and unaffected.

---

## 2. Neon PostgreSQL Configuration

You already have an existing Neon project in **AWS US East 2 (Ohio)**.

### Step 1: Copy your Connection String
1. Log in to the [Neon Console](https://console.neon.tech/).
2. Select your project: **Hotel Management System**.
3. Under **Dashboard** $\rightarrow$ **Connection Details**, ensure:
   - Branch: `production` (or `main`)
   - Role: your database user
   - Database: `hotel_management` (or default `neondb`)
4. Copy the **Postgres Connection URI**:
   ```text
   postgresql://username:password@ep-xyz-123456.us-east-2.aws.neon.tech/hotel_management?sslmode=require
   ```
   *(Ensure `sslmode=require` is present at the end)*.

### Step 2: Automatic Database Initialization
You **do not need to manually run SQL scripts**. When the web service boots on Render, `src/db.js` automatically:
- Executes `CREATE TABLE IF NOT EXISTS` for all tables (`users`, `customers`, `employees`, `rooms`, `bookings`, `payments`).
- Safely detects if the database is empty.
- Seeds demo users (`admin` / `admin123`, `receptionist` / `recep123`), 12 rooms, sample customers, bookings, and payments.
- Does **not** drop or overwrite existing tables.

---

## 3. Render Web Service Deployment Settings

In your [Render Dashboard](https://dashboard.render.com/):

### Configuration Settings
| Setting | Value |
| :--- | :--- |
| **Service Name** | `Hotel-Management-System` |
| **Repository** | `https://github.com/Sejuu11/Hotel-Management-System` |
| **Branch** | `main` |
| **Root Directory** | *(Leave blank or set to `.`)* |
| **Environment / Runtime** | `Node` |
| **Build Command** | `npm install` |
| **Start Command** | `npm start` |
| **Instance Type / Plan** | **Free** ($0 / month) |

---

## 4. Environment Variables on Render

In your Render Web Service $\rightarrow$ **Environment** tab, add the following variables:

| Key | Value / Instructions |
| :--- | :--- |
| `NODE_ENV` | `production` |
| `DATABASE_URL` | *Paste your full Neon Connection String from Step 2* |
| `JWT_SECRET` | *Any strong random 32+ character string (e.g. `g7H!k9#mP2$xL5^vR8*qT1@wY4&zB6)`) |
| `DEMO_USERNAME` | `admin` |
| `DEMO_PASSWORD` | `admin123` |

> [!IMPORTANT]
> Never commit actual passwords or your `DATABASE_URL` to GitHub. Always configure them securely inside Render's **Environment** tab.

---

## 5. Pushing Changes to GitHub

To trigger Render's automatic deployment:

```bash
# 1. Stage all web files and root configurations
git add .gitignore package.json package-lock.json render.yaml web-app/ DEPLOYMENT.md

# 2. Commit the changes
git commit -m "feat: Add full-stack web application with Neon PostgreSQL support and Render deployment configuration"

# 3. Push to main branch
git push origin main
```

Render will detect the push, run `npm install`, start `node web-app/src/server.js`, and provide your public URL (e.g., `https://hotel-management-system.onrender.com`).

---

## 6. Accessing & Demonstrating the Application

When opening the public Render URL:
1. **1-Click Interviewer Login**:
   - Click **Admin (All 9 Modules)** to test full management and payroll access (`admin` / `admin123`).
   - Click **Receptionist (Front Desk)** to test restricted front desk access (`receptionist` / `recep123`).
2. **Operations Dashboard**: View live room counters, check-in queues, and financial totals loaded directly from Neon PostgreSQL.
3. **Room Inventory**: Add rooms, edit prices, filter by category (`Single`, `Double`, `Deluxe`, `Suite`).
4. **Guest Directory**: Register guests and inspect previous stay history.
5. **Reservations & Overlap Guard**: Test date collision detection by attempting to book the same room for overlapping dates.
6. **Check-In**: Move reserved guests to active in-house stays.
7. **Check-Out & 12% GST Invoice**: Settle stay duration, add extra charges, deduct advance deposits, and generate an official printable invoice receipt.
8. **Reports**: View SQL aggregate queries, revenue by payment method, and departmental payroll summaries.
