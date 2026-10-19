/**
 * Customer & Guest Management Controller
 */

let allCustomers = [];

async function renderCustomers() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Guest Directory & Profiles</h2>
        <p>Manage customer contacts, identity proofs, and reservation histories</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-primary" onclick="openNewCustomerModal()">
          <i class="fa-solid fa-user-plus"></i> Register New Guest
        </button>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="filter-bar">
      <div class="search-input">
        <input type="text" id="customer-search" class="form-control" placeholder="Search by name, phone, or ID proof..." oninput="searchCustomers()">
      </div>
    </div>

    <div id="customers-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Loading guests...</div>
    </div>
  `;

  await loadCustomers();
}

async function loadCustomers() {
  try {
    const data = await API.get('/customers');
    allCustomers = data.customers || [];
    renderCustomerList(allCustomers);
  } catch (err) {
    document.getElementById('customers-container').innerHTML = `
      <div class="alert alert-danger">Failed to load guests: ${err.message}</div>
    `;
  }
}

function searchCustomers() {
  const query = document.getElementById('customer-search')?.value.toLowerCase() || '';
  const filtered = allCustomers.filter(c => 
    c.full_name.toLowerCase().includes(query) ||
    c.phone.toLowerCase().includes(query) ||
    (c.id_number && c.id_number.toLowerCase().includes(query)) ||
    (c.email && c.email.toLowerCase().includes(query))
  );
  renderCustomerList(filtered);
}

function renderCustomerList(customers) {
  const container = document.getElementById('customers-container');
  if (!container) return;

  if (customers.length === 0) {
    container.innerHTML = `
      <div class="card" style="text-align: center; padding: 40px;">
        <i class="fa-solid fa-users-slash" style="font-size: 32px; color: var(--text-dim); margin-bottom: 12px;"></i>
        <p style="color: var(--text-muted);">No guests found matching your search.</p>
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
              <th>Full Name</th>
              <th>Gender</th>
              <th>Phone</th>
              <th>Email</th>
              <th>ID Proof</th>
              <th>Registered</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${customers.map(c => `
              <tr>
                <td>#${c.customer_id}</td>
                <td><strong>${c.full_name}</strong></td>
                <td>${c.gender}</td>
                <td>${c.phone}</td>
                <td>${c.email || '--'}</td>
                <td><small>${c.id_type}: ${c.id_number}</small></td>
                <td>${formatDate(c.created_at)}</td>
                <td class="text-right">
                  <button class="btn btn-secondary btn-sm" onclick="viewCustomerHistory(${c.customer_id})" title="Booking History">
                    <i class="fa-solid fa-clock-rotate-left"></i>
                  </button>
                  <button class="btn btn-secondary btn-sm" onclick="openEditCustomerModal(${c.customer_id})" title="Edit Profile">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                  <button class="btn btn-outline-danger btn-sm" onclick="deleteCustomer(${c.customer_id})" title="Delete">
                    <i class="fa-solid fa-trash"></i>
                  </button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function openNewCustomerModal() {
  document.getElementById('customer-modal-title').innerHTML = '<i class="fa-solid fa-user-plus"></i> Register Hotel Guest';
  document.getElementById('customer-form-id').value = '';
  document.getElementById('customer-form-name').value = '';
  document.getElementById('customer-form-gender').value = 'Male';
  document.getElementById('customer-form-phone').value = '';
  document.getElementById('customer-form-email').value = '';
  document.getElementById('customer-form-idtype').value = 'Aadhaar';
  document.getElementById('customer-form-idnum').value = '';
  document.getElementById('customer-form-address').value = '';
  openModal('customer-modal');
}

function openEditCustomerModal(customerId) {
  const c = allCustomers.find(item => item.customer_id === customerId);
  if (!c) return;

  document.getElementById('customer-modal-title').innerHTML = `<i class="fa-solid fa-user-pen"></i> Edit Profile — ${c.full_name}`;
  document.getElementById('customer-form-id').value = c.customer_id;
  document.getElementById('customer-form-name').value = c.full_name;
  document.getElementById('customer-form-gender').value = c.gender;
  document.getElementById('customer-form-phone').value = c.phone;
  document.getElementById('customer-form-email').value = c.email || '';
  document.getElementById('customer-form-idtype').value = c.id_type;
  document.getElementById('customer-form-idnum').value = c.id_number;
  document.getElementById('customer-form-address').value = c.address || '';
  openModal('customer-modal');
}

async function viewCustomerHistory(customerId) {
  try {
    const data = await API.get(`/customers/${customerId}`);
    const { customer, bookings } = data;

    let historyHtml = `
      <div style="margin-bottom: 18px;">
        <h4>${customer.full_name}</h4>
        <p style="font-size: 13px; color: var(--text-muted);">${customer.phone} | ${customer.email || 'No email'} | ${customer.address || 'No address'}</p>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th>Booking #</th>
            <th>Room</th>
            <th>Check-In</th>
            <th>Check-Out</th>
            <th>Paid</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          ${bookings.length === 0 ? `
            <tr><td colspan="6" class="text-center">No previous stays recorded for this customer.</td></tr>
          ` : bookings.map(b => `
            <tr>
              <td>#${b.booking_id}</td>
              <td>${b.room_number} (${b.room_type})</td>
              <td>${formatDate(b.check_in_date)}</td>
              <td>${formatDate(b.actual_check_out_date || b.expected_check_out_date)}</td>
              <td>${formatCurrency(b.total_paid)}</td>
              <td><span class="badge badge-${b.booking_status.toLowerCase()}">${b.booking_status}</span></td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;

    document.getElementById('printable-receipt-content').innerHTML = historyHtml;
    openModal('receipt-modal');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteCustomer(customerId) {
  const c = allCustomers.find(item => item.customer_id === customerId);
  if (!confirm(`Delete guest record for ${c?.full_name || customerId}?`)) return;

  try {
    const res = await API.delete(`/customers/${customerId}`);
    showToast(res.message, 'success');
    await loadCustomers();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Form submit event
document.addEventListener('DOMContentLoaded', () => {
  const customerForm = document.getElementById('customer-form');
  if (customerForm) {
    customerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('customer-form-id').value;
      const payload = {
        fullName: document.getElementById('customer-form-name').value,
        gender: document.getElementById('customer-form-gender').value,
        phone: document.getElementById('customer-form-phone').value,
        email: document.getElementById('customer-form-email').value,
        idType: document.getElementById('customer-form-idtype').value,
        idNumber: document.getElementById('customer-form-idnum').value,
        address: document.getElementById('customer-form-address').value,
      };

      try {
        if (id) {
          const res = await API.put(`/customers/${id}`, payload);
          showToast(res.message, 'success');
        } else {
          const res = await API.post('/customers', payload);
          showToast(res.message, 'success');
        }
        closeModal('customer-modal');
        await loadCustomers();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }
});
