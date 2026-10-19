/**
 * Check-Out & Itemized Billing Controller
 */

let currentCheckoutBill = null;

async function renderCheckOutView() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Guest Check-Out & Billing</h2>
        <p>Active hotel stays, itemized invoice calculation with 12% GST, and payment settlements</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-secondary btn-sm" onclick="renderCheckOutView()">
          <i class="fa-solid fa-rotate"></i> Refresh Active Stays
        </button>
      </div>
    </div>

    <div id="checkout-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Loading active in-house guests...</div>
    </div>
  `;

  try {
    const data = await API.get('/checkout/active');
    const activeStays = data.activeStays || [];

    // Update navigation badge
    const badge = document.getElementById('nav-checkout-badge');
    if (badge) badge.textContent = activeStays.length;

    if (activeStays.length === 0) {
      document.getElementById('checkout-container').innerHTML = `
        <div class="card" style="text-align: center; padding: 48px 20px;">
          <div style="font-size: 36px; color: var(--text-dim); margin-bottom: 12px;">
            <i class="fa-solid fa-bed"></i>
          </div>
          <h3>No Guests Currently Checked In</h3>
          <p style="color: var(--text-muted); margin-top: 6px;">All occupied rooms have been settled or no active stays are in-house.</p>
        </div>
      `;
      return;
    }

    document.getElementById('checkout-container').innerHTML = `
      <div class="card">
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Booking #</th>
                <th>Guest Name</th>
                <th>Phone</th>
                <th>Room</th>
                <th>Check-In Date</th>
                <th>Expected Check-Out</th>
                <th>Advance Paid</th>
                <th class="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              ${activeStays.map(s => `
                <tr>
                  <td><strong>#${s.booking_id}</strong></td>
                  <td>${s.customer_name}</td>
                  <td>${s.customer_phone}</td>
                  <td><strong>Room ${s.room_number}</strong> <span class="badge badge-accent">${s.room_type}</span></td>
                  <td>${formatDate(s.check_in_date)}</td>
                  <td>${formatDate(s.expected_check_out_date)}</td>
                  <td>${formatCurrency(s.total_paid)}</td>
                  <td class="text-right">
                    <button class="btn btn-warning btn-sm" onclick="openCheckOutModal(${s.booking_id})">
                      <i class="fa-solid fa-receipt"></i> Process Check-Out
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
    document.getElementById('checkout-container').innerHTML = `
      <div class="alert alert-danger">Failed to load active stays: ${err.message}</div>
    `;
  }
}

async function openCheckOutModal(bookingId) {
  try {
    document.getElementById('checkout-booking-id').value = bookingId;
    document.getElementById('checkout-extra-charges').value = 0;

    await refreshCheckoutCalculation();
    openModal('checkout-modal');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function refreshCheckoutCalculation() {
  const bookingId = document.getElementById('checkout-booking-id').value;
  const extraCharges = document.getElementById('checkout-extra-charges').value || 0;

  try {
    const data = await API.post(`/checkout/calculate/${bookingId}`, {
      additionalCharges: parseFloat(extraCharges) || 0,
    });

    const { booking, bill } = data;
    currentCheckoutBill = { booking, bill };

    document.getElementById('bill-guest-name').textContent = booking.customer_name;
    document.getElementById('bill-room-number').textContent = `Room ${booking.room_number} (${booking.room_type})`;
    document.getElementById('bill-duration').textContent = `${bill.numberOfNights} Night(s)`;

    document.getElementById('invoice-room-rate').textContent = formatCurrency(bill.pricePerNight);
    document.getElementById('invoice-nights').textContent = bill.numberOfNights;
    document.getElementById('invoice-room-total').textContent = formatCurrency(bill.roomCharges);
    document.getElementById('invoice-subtotal').textContent = formatCurrency(bill.subtotal);
    document.getElementById('invoice-tax').textContent = formatCurrency(bill.taxAmount);
    document.getElementById('invoice-gross-total').textContent = formatCurrency(bill.totalAmount);
    document.getElementById('invoice-advance-paid').textContent = `- ${formatCurrency(bill.alreadyPaid)}`;
    document.getElementById('invoice-balance-due').textContent = formatCurrency(bill.balanceDue);

    // Auto-fill settlement payment amount to match remaining balance
    document.getElementById('checkout-pay-amount').value = bill.balanceDue;
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function displayPrintableReceipt(finalBill, booking) {
  const receiptHtml = `
    <div class="receipt-paper" style="padding: 24px; font-family: monospace; border: 1px dashed var(--border-color); border-radius: 8px;">
      <div style="text-align: center; margin-bottom: 20px;">
        <h2 style="margin: 0; font-size: 20px; font-weight: 700;">GRAND PALACE HOTEL</h2>
        <p style="margin: 4px 0; font-size: 12px; color: var(--text-muted);">Luxury Stays & Executive Hospitality</p>
        <p style="margin: 0; font-size: 11px; color: var(--text-dim);">GSTIN: 29AAAAA0000A1Z5 | Ph: +91 98765 43210</p>
      </div>

      <hr style="border: 0; border-top: 1px dashed var(--border-color); margin: 14px 0;">

      <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 12px;">
        <div>
          <div><strong>Bill To:</strong> ${booking.customer_name}</div>
          <div><strong>Room:</strong> Room ${booking.room_number} (${booking.room_type})</div>
        </div>
        <div style="text-align: right;">
          <div><strong>Receipt #:</strong> RCPT-${Date.now().toString().slice(-6)}</div>
          <div><strong>Date:</strong> ${finalBill.actualCheckOutDate}</div>
        </div>
      </div>

      <table style="width: 100%; border-collapse: collapse; font-size: 12px; margin: 16px 0;">
        <thead>
          <tr style="border-bottom: 1px solid var(--border-color); text-align: left;">
            <th style="padding: 6px 0;">Item Description</th>
            <th style="text-align: right; padding: 6px 0;">Rate</th>
            <th style="text-align: right; padding: 6px 0;">Qty</th>
            <th style="text-align: right; padding: 6px 0;">Total</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td style="padding: 6px 0;">Room Accommodation (${finalBill.numberOfNights} Nights)</td>
            <td style="text-align: right; padding: 6px 0;">${formatCurrency(finalBill.pricePerNight)}</td>
            <td style="text-align: right; padding: 6px 0;">${finalBill.numberOfNights}</td>
            <td style="text-align: right; padding: 6px 0;">${formatCurrency(finalBill.roomCharges)}</td>
          </tr>
          ${finalBill.additionalCharges > 0 ? `
            <tr>
              <td style="padding: 6px 0;">Additional Hotel Services</td>
              <td style="text-align: right; padding: 6px 0;">-</td>
              <td style="text-align: right; padding: 6px 0;">1</td>
              <td style="text-align: right; padding: 6px 0;">${formatCurrency(finalBill.additionalCharges)}</td>
            </tr>
          ` : ''}
          <tr style="border-top: 1px solid var(--border-color);">
            <td colspan="3" style="text-align: right; padding: 6px 0;">Subtotal:</td>
            <td style="text-align: right; padding: 6px 0;">${formatCurrency(finalBill.subtotal)}</td>
          </tr>
          <tr>
            <td colspan="3" style="text-align: right; padding: 6px 0;">GST (12%):</td>
            <td style="text-align: right; padding: 6px 0;">${formatCurrency(finalBill.taxAmount)}</td>
          </tr>
          <tr style="font-weight: 700; border-top: 1px dashed var(--border-color);">
            <td colspan="3" style="text-align: right; padding: 8px 0;">Gross Total:</td>
            <td style="text-align: right; padding: 8px 0;">${formatCurrency(finalBill.totalAmount)}</td>
          </tr>
          ${finalBill.alreadyPaid > 0 ? `
            <tr>
              <td colspan="3" style="text-align: right; padding: 4px 0; color: var(--success);">Advance Paid:</td>
              <td style="text-align: right; padding: 4px 0; color: var(--success);">- ${formatCurrency(finalBill.alreadyPaid)}</td>
            </tr>
          ` : ''}
          <tr style="font-weight: 700; border-top: 1px solid var(--border-color);">
            <td colspan="3" style="text-align: right; padding: 8px 0;">Settled at Checkout (${finalBill.paymentMethod}):</td>
            <td style="text-align: right; padding: 8px 0; color: var(--primary);">${formatCurrency(finalBill.settlementPayment)}</td>
          </tr>
        </tbody>
      </table>

      <div style="text-align: center; margin-top: 24px; font-size: 11px; color: var(--text-muted);">
        <p>*** PAYMENT RECEIVED WITH THANKS — VISIT AGAIN ***</p>
      </div>
    </div>
  `;

  document.getElementById('printable-receipt-content').innerHTML = receiptHtml;
  openModal('receipt-modal');
}

// Event listener for checkout dynamic updates
document.addEventListener('DOMContentLoaded', () => {
  const extraInput = document.getElementById('checkout-extra-charges');
  if (extraInput) {
    extraInput.addEventListener('input', refreshCheckoutCalculation);
  }

  const checkoutForm = document.getElementById('checkout-form');
  if (checkoutForm) {
    checkoutForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const bookingId = document.getElementById('checkout-booking-id').value;
      const payload = {
        additionalCharges: parseFloat(document.getElementById('checkout-extra-charges').value) || 0,
        paymentAmount: parseFloat(document.getElementById('checkout-pay-amount').value) || 0,
        paymentMethod: document.getElementById('checkout-pay-method').value,
        transactionReference: document.getElementById('checkout-pay-ref').value,
      };

      try {
        const res = await API.post(`/checkout/confirm/${bookingId}`, payload);
        showToast(res.message, 'success');
        closeModal('checkout-modal');

        // Show official receipt
        if (currentCheckoutBill) {
          displayPrintableReceipt(res.finalBill, currentCheckoutBill.booking);
        }

        await renderCheckOutView();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }
});
