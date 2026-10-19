const test = require('node:test');
const assert = require('node:assert/strict');
const { validateBookingDates, hasDateOverlap } = require('../src/services/bookingValidator');

test('BookingValidator: validateBookingDates', () => {
  // Valid range
  assert.doesNotThrow(() => {
    validateBookingDates('2026-12-01', '2026-12-05');
  });

  // Check-out on same day should throw
  assert.throws(() => {
    validateBookingDates('2026-12-01', '2026-12-01');
  }, /Check-out date must be strictly after the check-in date/);

  // Check-out before check-in should throw
  assert.throws(() => {
    validateBookingDates('2026-12-05', '2026-12-01');
  }, /Check-out date must be strictly after the check-in date/);

  // Missing dates
  assert.throws(() => {
    validateBookingDates(null, '2026-12-05');
  }, /Both Check-in date and Check-out date are required/);
});

test('BookingValidator: hasDateOverlap', () => {
  // Existing booking: Nov 10 to Nov 15
  const b1Start = '2026-11-10';
  const b1End = '2026-11-15';

  // Overlap 1: Exact same dates
  assert.equal(hasDateOverlap(b1Start, b1End, '2026-11-10', '2026-11-15'), true);

  // Overlap 2: Inside range (Nov 11 to Nov 14)
  assert.equal(hasDateOverlap(b1Start, b1End, '2026-11-11', '2026-11-14'), true);

  // Overlap 3: Enveloping range (Nov 08 to Nov 18)
  assert.equal(hasDateOverlap(b1Start, b1End, '2026-11-08', '2026-11-18'), true);

  // Overlap 4: Starts before, ends during (Nov 08 to Nov 12)
  assert.equal(hasDateOverlap(b1Start, b1End, '2026-11-08', '2026-11-12'), true);

  // Overlap 5: Starts during, ends after (Nov 13 to Nov 18)
  assert.equal(hasDateOverlap(b1Start, b1End, '2026-11-13', '2026-11-18'), true);

  // No overlap: Strictly before (Nov 01 to Nov 09)
  assert.equal(hasDateOverlap(b1Start, b1End, '2026-11-01', '2026-11-09'), false);

  // No overlap: Strictly after (Nov 16 to Nov 20)
  assert.equal(hasDateOverlap(b1Start, b1End, '2026-11-16', '2026-11-20'), false);
});
