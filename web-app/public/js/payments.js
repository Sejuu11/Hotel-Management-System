/**
 * Payments Ledger Controller
 */

let allPayments = [];

async function renderPaymentsView() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Payments & Financial Transactions Ledger</h2>
        <p>Complete record of advance deposits, settlement payments, and receipt references</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-secondary btn-sm" onclick="renderPaymentsView()">
          <i class="fa-solid fa-rotate"></i> Refresh Ledger
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <div class="search-input">
        <input type="text" id="payment-search" class="form-control" placeholder="Search by guest, reference, or room..." oninput="filterPayments()">
      </div>
      <select id="payment-filter-method" class="form-control" style="max-width: 180px;" onchange="filterPayments()">
        <option value="ALL">All Payment Methods</option>
        <option value="UPI">UPI</option>
        <option value="CARD">Credit/Debit Card</option>
        <option value="CASH">Cash</option>
        <option value="BANK_TRANSFER">Bank Transfer</option>
      </select>
    </div>

    <div id="payments-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Loading financial ledger...</div>
    </div>
  `;

  await loadPayments();
}

async function loadPayments() {
  try {
    const data = await API.get('/payments');
    allPayments = data.payments || [];
    filterPayments();
  } catch (err) {
    document.getElementById('payments-container').innerHTML = `
      <div class="alert alert-danger">Failed to load payments: ${err.message}</div>
    `;
  }
}

function filterPayments() {
  const search = document.getElementById('payment-search')?.value.toLowerCase() || '';
  const method = document.getElementById('payment-filter-method')?.value || 'ALL';

  const filtered = allPayments.filter(p => {
    const matchSearch = p.customer_name.toLowerCase().includes(search) ||
                        (p.transaction_reference && p.transaction_reference.toLowerCase().includes(search)) ||
                        p.room_number.toLowerCase().includes(search);
    const matchMethod = method === 'ALL' || p.payment_method === method;
    return matchSearch && matchMethod;
  });

  renderPaymentList(filtered);
}

function renderPaymentList(payments) {
  const container = document.getElementById('payments-container');
  if (!container) return;

  const totalCollected = payments.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);

  if (payments.length === 0) {
    container.innerHTML = `
      <div class="card" style="text-align: center; padding: 40px;">
        <i class="fa-solid fa-wallet" style="font-size: 32px; color: var(--text-dim); margin-bottom: 12px;"></i>
        <p style="color: var(--text-muted);">No payment transactions match the filters.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = `
    <!-- Total Summary Banner -->
    <div class="stat-card" style="margin-bottom: 18px; max-width: 360px;">
      <div class="stat-info">
        <span class="stat-label">Filtered Ledger Collections</span>
        <div class="stat-value text-success">${formatCurrency(totalCollected)}</div>
      </div>
      <div class="stat-icon success"><i class="fa-solid fa-vault"></i></div>
    </div>

    <div class="card">
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Date & Time</th>
              <th>Booking #</th>
              <th>Guest Name</th>
              <th>Room</th>
              <th>Method</th>
              <th>Status</th>
              <th>Reference</th>
              <th class="text-right">Amount</th>
            </tr>
          </thead>
          <tbody>
            ${payments.map(p => `
              <tr>
                <td>#${p.payment_id}</td>
                <td>${formatDate(p.payment_date)}</td>
                <td>#${p.booking_id}</td>
                <td><strong>${p.customer_name}</strong></td>
                <td>Room ${p.room_number}</td>
                <td><span class="badge badge-accent">${p.payment_method}</span></td>
                <td><span class="badge badge-${p.payment_status.toLowerCase()}">${p.payment_status}</span></td>
                <td><small style="font-family: monospace;">${p.transaction_reference || 'N/A'}</small></td>
                <td class="text-right font-weight-bold text-success">${formatCurrency(p.amount)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}
