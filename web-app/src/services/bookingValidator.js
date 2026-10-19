/**
 * Booking Validation Service - Validates reservation inputs and detects date collisions.
 */

function validateBookingDates(checkIn, checkOut) {
  if (!checkIn || !checkOut) {
    throw new Error('Both Check-in date and Check-out date are required.');
  }

  const inDate = new Date(checkIn);
  const outDate = new Date(checkOut);

  if (isNaN(inDate.getTime())) {
    throw new Error('Check-in date is not a valid date format.');
  }
  if (isNaN(outDate.getTime())) {
    throw new Error('Check-out date is not a valid date format.');
  }

  // Strip time components
  const inUtc = Date.UTC(inDate.getFullYear(), inDate.getMonth(), inDate.getDate());
  const outUtc = Date.UTC(outDate.getFullYear(), outDate.getMonth(), outDate.getDate());

  if (outUtc <= inUtc) {
    throw new Error('Check-out date must be strictly after the check-in date.');
  }

  return true;
}

/**
 * Pure helper to verify if two date ranges collide:
 * Collision occurs if: (startA < endB) AND (endA > startB)
 */
function hasDateOverlap(startA, endA, startB, endB) {
  const sA = new Date(startA).getTime();
  const eA = new Date(endA).getTime();
  const sB = new Date(startB).getTime();
  const eB = new Date(endB).getTime();

  return sA < eB && eA > sB;
}

/**
 * Queries database to check if room has conflicting active bookings
 */
async function isRoomBookedForDates(dbQuery, roomId, checkInDate, expectedCheckOutDate, excludeBookingId = null) {
  let sql = `
    SELECT booking_id, check_in_date, expected_check_out_date, booking_status
    FROM bookings
    WHERE room_id = $1
      AND booking_status IN ('RESERVED', 'CHECKED_IN')
      AND (check_in_date < $3 AND expected_check_out_date > $2)
  `;
  const params = [roomId, checkInDate, expectedCheckOutDate];

  if (excludeBookingId) {
    sql += ` AND booking_id != $4`;
    params.push(excludeBookingId);
  }

  const result = await dbQuery(sql, params);
  return result.rows.length > 0;
}

module.exports = {
  validateBookingDates,
  hasDateOverlap,
  isRoomBookedForDates,
};
