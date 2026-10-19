/**
 * Bookings & Reservations Controller
 */

let allBookings = [];

async function renderBookings() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Reservation & Booking Operations</h2>
        <p>Schedule stays, check date collisions, and manage guest reservations</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-primary" onclick="openNewBookingModal()">
          <i class="fa-solid fa-calendar-plus"></i> New Reservation
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <div class="search-input">
        <input type="text" id="booking-search" class="form-control" placeholder="Search by guest, phone, or room..." oninput="filterBookings()">
      </div>
      <select id="booking-filter-status" class="form-control" style="max-width: 180px;" onchange="filterBookings()">
        <option value="ALL">All Statuses</option>
        <option value="RESERVED">RESERVED</option>
        <option value="CHECKED_IN">CHECKED_IN</option>
        <option value="CHECKED_OUT">CHECKED_OUT</option>
        <option value="CANCELLED">CANCELLED</option>
      </select>
    </div>

    <div id="bookings-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Loading reservations...</div>
    </div>
  `;

  await loadBookings();
}

async function loadBookings() {
  try {
    const data = await API.get('/bookings');
    allBookings = data.bookings || [];
    filterBookings();
  } catch (err) {
    document.getElementById('bookings-container').innerHTML = `
      <div class="alert alert-danger">Failed to load reservations: ${err.message}</div>
    `;
  }
}

function filterBookings() {
  const search = document.getElementById('booking-search')?.value.toLowerCase() || '';
  const status = document.getElementById('booking-filter-status')?.value || 'ALL';

  const filtered = allBookings.filter(b => {
    const matchSearch = b.customer_name.toLowerCase().includes(search) ||
                        b.customer_phone.toLowerCase().includes(search) ||
                        b.room_number.toLowerCase().includes(search);
    const matchStatus = status === 'ALL' || b.booking_status === status;
    return matchSearch && matchStatus;
  });

  renderBookingList(filtered);
}

function renderBookingList(bookings) {
  const container = document.getElementById('bookings-container');
  if (!container) return;

  if (bookings.length === 0) {
    container.innerHTML = `
      <div class="card" style="text-align: center; padding: 40px;">
        <i class="fa-solid fa-calendar-xmark" style="font-size: 32px; color: var(--text-dim); margin-bottom: 12px;"></i>
        <p style="color: var(--text-muted);">No reservations found matching the selected filters.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = `
    <div class="card">
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Guest Name</th>
              <th>Contact</th>
              <th>Room</th>
              <th>Check-In</th>
              <th>Check-Out</th>
              <th>Guests</th>
              <th>Paid</th>
              <th>Status</th>
              <th class="text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            ${bookings.map(b => `
              <tr>
                <td><strong>#${b.booking_id}</strong></td>
                <td>${b.customer_name}</td>
                <td><small>${b.customer_phone}</small></td>
                <td>Room ${b.room_number} <small class="text-dim">(${b.room_type})</small></td>
                <td>${formatDate(b.check_in_date)}</td>
                <td>${formatDate(b.expected_check_out_date)}</td>
                <td>${b.number_of_guests}</td>
                <td>${formatCurrency(b.total_paid)}</td>
                <td><span class="badge badge-${b.booking_status.toLowerCase()}">${b.booking_status}</span></td>
                <td class="text-right">
                  ${b.booking_status === 'RESERVED' ? `
                    <button class="btn btn-success btn-sm" onclick="quickCheckIn(${b.booking_id})" title="Check-In">
                      <i class="fa-solid fa-bell-concierge"></i> Check-In
                    </button>
                    <button class="btn btn-outline-danger btn-sm" onclick="cancelBooking(${b.booking_id})" title="Cancel">
                      <i class="fa-solid fa-ban"></i>
                    </button>
                  ` : b.booking_status === 'CHECKED_IN' ? `
                    <button class="btn btn-warning btn-sm" onclick="openCheckOutModal(${b.booking_id})" title="Check-Out">
                      <i class="fa-solid fa-receipt"></i> Check-Out
                    </button>
                  ` : `
                    <span class="text-dim" style="font-size: 12px;">Completed</span>
                  `}
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

async function openNewBookingModal() {
  try {
    // 1. Load customers
    const custData = await API.get('/customers');
    const custSelect = document.getElementById('booking-form-customer');
    custSelect.innerHTML = '<option value="">-- Choose registered guest --</option>';
    (custData.customers || []).forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.customer_id;
      opt.textContent = `${c.full_name} (${c.phone})`;
      custSelect.appendChild(opt);
    });

    // 2. Load available rooms
    const roomData = await API.get('/rooms', { status: 'AVAILABLE' });
    const roomSelect = document.getElementById('booking-form-room');
    roomSelect.innerHTML = '<option value="">-- Choose available room --</option>';
    (roomData.rooms || []).forEach(r => {
      const opt = document.createElement('option');
      opt.value = r.room_id;
      opt.dataset.price = r.price_per_night;
      opt.textContent = `Room ${r.room_number} - ${r.room_type} (₹${r.price_per_night}/night)`;
      roomSelect.appendChild(opt);
    });

    // Default dates: today and 2 days later
    const today = new Date();
    const twoDaysLater = new Date();
    twoDaysLater.setDate(today.getDate() + 2);

    document.getElementById('booking-form-checkin').value = today.toISOString().split('T')[0];
    document.getElementById('booking-form-checkout').value = twoDaysLater.toISOString().split('T')[0];
    document.getElementById('booking-form-advance').value = '';
    document.getElementById('booking-form-request').value = '';

    updateBookingCostPreview();
    openModal('booking-modal');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function updateBookingCostPreview() {
  const roomSelect = document.getElementById('booking-form-room');
  const checkIn = document.getElementById('booking-form-checkin')?.value;
  const checkOut = document.getElementById('booking-form-checkout')?.value;
  const previewCard = document.getElementById('booking-cost-preview');

  if (!roomSelect || !roomSelect.value || !checkIn || !checkOut) {
    if (previewCard) previewCard.style.display = 'none';
    return;
  }

  const selectedOpt = roomSelect.options[roomSelect.selectedIndex];
  const price = parseFloat(selectedOpt.dataset.price) || 0;

  const dIn = new Date(checkIn);
  const dOut = new Date(checkOut);
  const diffTime = dOut - dIn;
  const nights = Math.max(1, Math.round(diffTime / (1000 * 60 * 60 * 24)));

  const roomTotal = nights * price;
  const tax = roomTotal * 0.12;
  const total = roomTotal + tax;

  document.getElementById('preview-nights').textContent = `${nights} Night(s)`;
  document.getElementById('preview-rate').textContent = `₹${price} / night`;
  document.getElementById('preview-total').textContent = `₹${total.toFixed(2)}`;
  previewCard.style.display = 'block';
}

async function quickCheckIn(bookingId) {
  if (!confirm(`Confirm check-in for booking #${bookingId}?`)) return;
  try {
    const res = await API.post(`/checkin/confirm/${bookingId}`);
    showToast(res.message, 'success');
    await loadBookings();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function cancelBooking(bookingId) {
  if (!confirm(`Are you sure you want to cancel reservation #${bookingId}? The room will be released.`)) return;
  try {
    const res = await API.put(`/bookings/${bookingId}/cancel`);
    showToast(res.message, 'success');
    await loadBookings();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Event listeners for booking modal calculations
document.addEventListener('DOMContentLoaded', () => {
  ['booking-form-room', 'booking-form-checkin', 'booking-form-checkout'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('change', updateBookingCostPreview);
  });

  const bookingForm = document.getElementById('booking-form');
  if (bookingForm) {
    bookingForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        customerId: document.getElementById('booking-form-customer').value,
        roomId: document.getElementById('booking-form-room').value,
        checkInDate: document.getElementById('booking-form-checkin').value,
        expectedCheckOutDate: document.getElementById('booking-form-checkout').value,
        numberOfGuests: document.getElementById('booking-form-guests').value,
        advancePayment: document.getElementById('booking-form-advance').value,
        paymentMethod: document.getElementById('booking-form-paymethod').value,
        specialRequest: document.getElementById('booking-form-request').value,
      };

      try {
        const res = await API.post('/bookings', payload);
        showToast(res.message, 'success');
        closeModal('booking-modal');
        await loadBookings();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }
});
