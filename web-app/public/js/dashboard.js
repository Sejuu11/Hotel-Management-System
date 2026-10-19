/**
 * Dashboard View Controller
 */

async function renderDashboard() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Operations Dashboard</h2>
        <p>Live metrics, room availability, and operational turnover</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-primary" onclick="openNewBookingModal()">
          <i class="fa-solid fa-plus"></i> New Reservation
        </button>
        <button class="btn btn-secondary" onclick="openNewCustomerModal()">
          <i class="fa-solid fa-user-plus"></i> Add Guest
        </button>
      </div>
    </div>
    <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Loading live statistics...</div>
  `;

  try {
    const data = await API.get('/dashboard/stats');
    const { stats, recentBookings } = data;

    // Update navigation badge counters
    const ciBadge = document.getElementById('nav-checkin-badge');
    const coBadge = document.getElementById('nav-checkout-badge');
    if (ciBadge) ciBadge.textContent = stats.reservedRooms;
    if (coBadge) coBadge.textContent = stats.occupiedRooms;

    container.innerHTML = `
      <div class="view-header">
        <div class="view-title">
          <h2>Operations Dashboard</h2>
          <p>Live room availability, front desk queue, and financial overview</p>
        </div>
        <div class="view-actions">
          <button class="btn btn-primary" onclick="openNewBookingModal()">
            <i class="fa-solid fa-plus"></i> New Reservation
          </button>
          <button class="btn btn-secondary" onclick="openNewCustomerModal()">
            <i class="fa-solid fa-user-plus"></i> Add Guest
          </button>
        </div>
      </div>

      <!-- 8 Real-time KPI Metric Cards -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Total Inventory</span>
            <div class="stat-value">${stats.totalRooms} Rooms</div>
          </div>
          <div class="stat-icon purple"><i class="fa-solid fa-hotel"></i></div>
        </div>

        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Available Now</span>
            <div class="stat-value text-success">${stats.availableRooms}</div>
          </div>
          <div class="stat-icon success"><i class="fa-solid fa-door-open"></i></div>
        </div>

        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Currently Occupied</span>
            <div class="stat-value text-primary">${stats.occupiedRooms}</div>
          </div>
          <div class="stat-icon primary"><i class="fa-solid fa-bed"></i></div>
        </div>

        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Reserved (Incoming)</span>
            <div class="stat-value text-warning">${stats.reservedRooms}</div>
          </div>
          <div class="stat-icon warning"><i class="fa-solid fa-clock"></i></div>
        </div>

        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Today's Check-ins</span>
            <div class="stat-value">${stats.todayCheckIns}</div>
          </div>
          <div class="stat-icon primary"><i class="fa-solid fa-bell-concierge"></i></div>
        </div>

        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Today's Check-outs</span>
            <div class="stat-value">${stats.todayCheckOuts}</div>
          </div>
          <div class="stat-icon warning"><i class="fa-solid fa-receipt"></i></div>
        </div>

        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Today's Revenue</span>
            <div class="stat-value text-success">${formatCurrency(stats.todayRevenue)}</div>
          </div>
          <div class="stat-icon success"><i class="fa-solid fa-indian-rupee-sign"></i></div>
        </div>

        <div class="stat-card">
          <div class="stat-info">
            <span class="stat-label">Total Revenue</span>
            <div class="stat-value">${formatCurrency(stats.totalRevenue)}</div>
          </div>
          <div class="stat-icon purple"><i class="fa-solid fa-chart-line"></i></div>
        </div>
      </div>

      <!-- Breakdown by Room Type & Status -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px; margin-bottom: 24px;">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title"><i class="fa-solid fa-layer-group"></i> Inventory by Category</h3>
          </div>
          <table class="table">
            <thead>
              <tr>
                <th>Category</th>
                <th class="text-right">Total Rooms</th>
              </tr>
            </thead>
            <tbody>
              ${Object.entries(stats.roomsByType).map(([type, count]) => `
                <tr>
                  <td><strong>${type}</strong></td>
                  <td class="text-right">${count}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>

        <div class="card">
          <div class="card-header">
            <h3 class="card-title"><i class="fa-solid fa-chart-simple"></i> Status Distribution</h3>
          </div>
          <table class="table">
            <thead>
              <tr>
                <th>Status</th>
                <th class="text-right">Count</th>
                <th class="text-right">Share</th>
              </tr>
            </thead>
            <tbody>
              ${Object.entries(stats.roomsByStatus).map(([st, count]) => {
                const pct = stats.totalRooms > 0 ? Math.round((count / stats.totalRooms) * 100) : 0;
                return `
                  <tr>
                    <td><span class="badge badge-${st.toLowerCase()}">${st}</span></td>
                    <td class="text-right">${count}</td>
                    <td class="text-right">${pct}%</td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>
      </div>

      <!-- Recent Reservations Table -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title"><i class="fa-solid fa-clock-rotate-left"></i> Recent Booking Activity</h3>
          <button class="btn btn-secondary btn-sm" onclick="navigateTo('bookings')">View All Bookings</button>
        </div>
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Guest</th>
                <th>Room</th>
                <th>Category</th>
                <th>Check-In</th>
                <th>Check-Out</th>
                <th>Paid</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${recentBookings.length === 0 ? `
                <tr><td colspan="8" class="text-center">No bookings recorded yet.</td></tr>
              ` : recentBookings.map(b => `
                <tr>
                  <td>#${b.booking_id}</td>
                  <td><strong>${b.customer_name}</strong></td>
                  <td>${b.room_number}</td>
                  <td>${b.room_type}</td>
                  <td>${formatDate(b.check_in_date)}</td>
                  <td>${formatDate(b.expected_check_out_date)}</td>
                  <td>${formatCurrency(b.total_paid)}</td>
                  <td><span class="badge badge-${b.booking_status.toLowerCase()}">${b.booking_status}</span></td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<div class="alert alert-danger">Failed to load dashboard: ${err.message}</div>`;
  }
}
