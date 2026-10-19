/**
 * Analytics & Reports Controller
 */

let activeReportTab = 'revenue'; // 'revenue', 'bookings', 'rooms', 'employees'

async function renderReportsView() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Hospitality Reports & SQL Analytics</h2>
        <p>Comprehensive aggregate queries, multi-table JOINs, and operational metrics</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-secondary btn-sm" onclick="window.print()">
          <i class="fa-solid fa-print"></i> Print Report
        </button>
      </div>
    </div>

    <!-- Report Tabs -->
    <div class="filter-bar" style="margin-bottom: 24px;">
      <button class="btn ${activeReportTab === 'revenue' ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="switchReportTab('revenue')">
        <i class="fa-solid fa-chart-line"></i> Revenue Analytics
      </button>
      <button class="btn ${activeReportTab === 'bookings' ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="switchReportTab('bookings')">
        <i class="fa-solid fa-book"></i> Reservations Ledger
      </button>
      <button class="btn ${activeReportTab === 'rooms' ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="switchReportTab('rooms')">
        <i class="fa-solid fa-hotel"></i> Room Utilization
      </button>
      <button class="btn ${activeReportTab === 'employees' ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="switchReportTab('employees')">
        <i class="fa-solid fa-money-check-dollar"></i> Payroll by Department
      </button>
    </div>

    <div id="report-content-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Generating report analytics...</div>
    </div>
  `;

  await loadActiveReport();
}

function switchReportTab(tab) {
  activeReportTab = tab;
  renderReportsView();
}

async function loadActiveReport() {
  const container = document.getElementById('report-content-container');
  if (!container) return;

  try {
    if (activeReportTab === 'revenue') {
      const data = await API.get('/reports/revenue');
      const rows = data.report || [];

      container.innerHTML = `
        <div class="card">
          <div class="card-header">
            <h3 class="card-title"><i class="fa-solid fa-chart-line"></i> Revenue Grouped by Date & Payment Method</h3>
          </div>
          <div class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Payment Method</th>
                  <th class="text-right">Txn Count</th>
                  <th class="text-right">Total Revenue</th>
                  <th class="text-right">Average Txn</th>
                  <th class="text-right">Min Txn</th>
                  <th class="text-right">Max Txn</th>
                </tr>
              </thead>
              <tbody>
                ${rows.length === 0 ? `
                  <tr><td colspan="7" class="text-center">No revenue recorded yet.</td></tr>
                ` : rows.map(r => `
                  <tr>
                    <td><strong>${formatDate(r.txn_date)}</strong></td>
                    <td><span class="badge badge-accent">${r.payment_method}</span></td>
                    <td class="text-right">${r.payment_count}</td>
                    <td class="text-right font-weight-bold text-success">${formatCurrency(r.total_revenue)}</td>
                    <td class="text-right">${formatCurrency(r.avg_payment)}</td>
                    <td class="text-right">${formatCurrency(r.min_payment)}</td>
                    <td class="text-right">${formatCurrency(r.max_payment)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      `;
    } 
    else if (activeReportTab === 'bookings') {
      const data = await API.get('/reports/bookings');
      const rows = data.report || [];

      container.innerHTML = `
        <div class="card">
          <div class="card-header">
            <h3 class="card-title"><i class="fa-solid fa-book"></i> Multi-Table JOIN Reservations Ledger</h3>
          </div>
          <div class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th>Booking #</th>
                  <th>Guest Name</th>
                  <th>Phone</th>
                  <th>Room</th>
                  <th>Type</th>
                  <th>Check-In</th>
                  <th>Check-Out</th>
                  <th>Status</th>
                  <th class="text-right">Total Collected</th>
                </tr>
              </thead>
              <tbody>
                ${rows.length === 0 ? `
                  <tr><td colspan="9" class="text-center">No bookings on ledger.</td></tr>
                ` : rows.map(r => `
                  <tr>
                    <td>#${r.booking_id}</td>
                    <td><strong>${r.customer_name}</strong></td>
                    <td>${r.customer_phone}</td>
                    <td>Room ${r.room_number}</td>
                    <td>${r.room_type}</td>
                    <td>${formatDate(r.check_in_date)}</td>
                    <td>${formatDate(r.actual_check_out_date || r.expected_check_out_date)}</td>
                    <td><span class="badge badge-${r.booking_status.toLowerCase()}">${r.booking_status}</span></td>
                    <td class="text-right font-weight-bold">${formatCurrency(r.total_paid)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      `;
    }
    else if (activeReportTab === 'rooms') {
      const data = await API.get('/reports/rooms');
      const rows = data.report || [];

      container.innerHTML = `
        <div class="card">
          <div class="card-header">
            <h3 class="card-title"><i class="fa-solid fa-hotel"></i> Room Utilization & Rate Breakdown by Category</h3>
          </div>
          <div class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th>Category</th>
                  <th class="text-right">Total</th>
                  <th class="text-right">Available</th>
                  <th class="text-right">Occupied</th>
                  <th class="text-right">Reserved</th>
                  <th class="text-right">Maintenance</th>
                  <th class="text-right">Avg Rate</th>
                  <th class="text-right">Rate Range</th>
                </tr>
              </thead>
              <tbody>
                ${rows.map(r => `
                  <tr>
                    <td><strong>${r.room_type}</strong></td>
                    <td class="text-right">${r.total_rooms}</td>
                    <td class="text-right text-success">${r.available_count}</td>
                    <td class="text-right text-primary">${r.occupied_count}</td>
                    <td class="text-right text-warning">${r.reserved_count}</td>
                    <td class="text-right text-danger">${r.maintenance_count}</td>
                    <td class="text-right font-weight-bold">${formatCurrency(r.avg_price)}</td>
                    <td class="text-right"><small>${formatCurrency(r.min_price)} - ${formatCurrency(r.max_price)}</small></td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      `;
    }
    else if (activeReportTab === 'employees') {
      const data = await API.get('/reports/employees');
      const rows = data.report || [];

      container.innerHTML = `
        <div class="card">
          <div class="card-header">
            <h3 class="card-title"><i class="fa-solid fa-money-check-dollar"></i> Staff Payroll Grouped by Designation</h3>
          </div>
          <div class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th>Department / Designation</th>
                  <th class="text-right">Headcount</th>
                  <th class="text-right">Active Staff</th>
                  <th class="text-right">Total Payroll</th>
                  <th class="text-right">Avg Salary</th>
                  <th class="text-right">Salary Range</th>
                </tr>
              </thead>
              <tbody>
                ${rows.map(r => `
                  <tr>
                    <td><strong>${r.designation}</strong></td>
                    <td class="text-right">${r.staff_count}</td>
                    <td class="text-right">${r.active_count}</td>
                    <td class="text-right font-weight-bold text-success">${formatCurrency(r.total_payroll)}</td>
                    <td class="text-right">${formatCurrency(r.avg_salary)}</td>
                    <td class="text-right"><small>${formatCurrency(r.min_salary)} - ${formatCurrency(r.max_salary)}</small></td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      `;
    }
  } catch (err) {
    container.innerHTML = `<div class="alert alert-danger">Failed to load report: ${err.message}</div>`;
  }
}
