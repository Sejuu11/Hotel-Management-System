/**
 * Check-In Queue Controller
 */

async function renderCheckInQueue() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Front Desk Check-In Queue</h2>
        <p>Arriving guests with active reservations ready for room check-in</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-secondary btn-sm" onclick="renderCheckInQueue()">
          <i class="fa-solid fa-rotate"></i> Refresh Queue
        </button>
      </div>
    </div>

    <div id="checkin-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Fetching check-in queue...</div>
    </div>
  `;

  try {
    const data = await API.get('/checkin/queue');
    const queue = data.queue || [];

    // Update navigation badge
    const badge = document.getElementById('nav-checkin-badge');
    if (badge) badge.textContent = queue.length;

    if (queue.length === 0) {
      document.getElementById('checkin-container').innerHTML = `
        <div class="card" style="text-align: center; padding: 48px 20px;">
          <div style="font-size: 36px; color: var(--text-dim); margin-bottom: 12px;">
            <i class="fa-solid fa-circle-check"></i>
          </div>
          <h3>Check-In Queue is Clear</h3>
          <p style="color: var(--text-muted); margin-top: 6px;">All reserved guests have been checked in or no reservations are pending.</p>
        </div>
      `;
      return;
    }

    document.getElementById('checkin-container').innerHTML = `
      <div class="card">
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Booking #</th>
                <th>Guest Name</th>
                <th>Phone</th>
                <th>Assigned Room</th>
                <th>Check-In Date</th>
                <th>Expected Check-Out</th>
                <th>Advance Paid</th>
                <th>Special Request</th>
                <th class="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              ${queue.map(q => `
                <tr>
                  <td><strong>#${q.booking_id}</strong></td>
                  <td>${q.customer_name}</td>
                  <td>${q.customer_phone}</td>
                  <td><strong>Room ${q.room_number}</strong> <span class="badge badge-accent">${q.room_type}</span></td>
                  <td>${formatDate(q.check_in_date)}</td>
                  <td>${formatDate(q.expected_check_out_date)}</td>
                  <td>${formatCurrency(q.total_paid)}</td>
                  <td><small style="color: var(--text-muted);">${q.special_request || 'None'}</small></td>
                  <td class="text-right">
                    <button class="btn btn-success btn-sm" onclick="executeCheckIn(${q.booking_id}, '${q.room_number}', '${q.customer_name}')">
                      <i class="fa-solid fa-key"></i> Confirm Check-In
                    </button>
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `;
  } catch (err) {
    document.getElementById('checkin-container').innerHTML = `
      <div class="alert alert-danger">Failed to load check-in queue: ${err.message}</div>
    `;
  }
}

async function executeCheckIn(bookingId, roomNumber, guestName) {
  if (!confirm(`Check in guest ${guestName} to Room ${roomNumber}?`)) return;

  try {
    const res = await API.post(`/checkin/confirm/${bookingId}`);
    showToast(res.message, 'success');
    await renderCheckInQueue();
  } catch (err) {
    showToast(err.message, 'error');
  }
}
