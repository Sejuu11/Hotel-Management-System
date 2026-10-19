const test = require('node:test');
const assert = require('node:assert/strict');
const { calculateNights, calculateBill } = require('../src/services/billingService');

test('BillingService: calculateNights', () => {
  // 3-day stay
  const nights3 = calculateNights('2026-11-01', '2026-11-04');
  assert.equal(nights3, 3, 'Should compute exactly 3 nights');

  // 1-day stay
  const nights1 = calculateNights('2026-11-01', '2026-11-02');
  assert.equal(nights1, 1, 'Should compute 1 night');

  // Same day checkout (day-use minimum 1 night)
  const sameDay = calculateNights('2026-11-01', '2026-11-01');
  assert.equal(sameDay, 1, 'Same day checkout should count as minimum 1 night');
});

test('BillingService: calculateBill with 12% GST and advance payment', () => {
  const bill = calculateBill({
    checkInDate: '2026-11-01',
    checkOutDate: '2026-11-03', // 2 nights
    pricePerNight: 2000,
    additionalCharges: 500,
    taxRate: 0.12,
    alreadyPaid: 1000,
  });

  // 2 nights * 2000 = 4000
  assert.equal(bill.numberOfNights, 2);
  assert.equal(bill.roomCharges, 4000);
  assert.equal(bill.additionalCharges, 500);

  // Subtotal = 4500
  assert.equal(bill.subtotal, 4500);

  // 12% GST on 4500 = 540
  assert.equal(bill.taxAmount, 540);

  // Total = 4500 + 540 = 5040
  assert.equal(bill.totalAmount, 5040);

  // Balance due = 5040 - 1000 = 4040
  assert.equal(bill.alreadyPaid, 1000);
  assert.equal(bill.balanceDue, 4040);
});

test('BillingService: calculateBill with zero extra charges and full settlement', () => {
  const bill = calculateBill({
    checkInDate: '2026-11-01',
    checkOutDate: '2026-11-02', // 1 night
    pricePerNight: 1500,
    additionalCharges: 0,
    taxRate: 0.12,
    alreadyPaid: 1680, // Full amount (1500 + 180 tax)
  });

  assert.equal(bill.numberOfNights, 1);
  assert.equal(bill.subtotal, 1500);
  assert.equal(bill.taxAmount, 180);
  assert.equal(bill.totalAmount, 1680);
  assert.equal(bill.balanceDue, 0, 'Balance due should be 0 after full payment');
});
