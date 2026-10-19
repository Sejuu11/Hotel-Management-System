/**
 * Billing Service - Computes stay duration, tax, additional charges, and balance settlements.
 */

function calculateNights(checkInDate, checkOutDate) {
  const start = new Date(checkInDate);
  const end = new Date(checkOutDate);

  // Strip hours for pure date comparison
  const utcStart = Date.UTC(start.getFullYear(), start.getMonth(), start.getDate());
  const utcEnd = Date.UTC(end.getFullYear(), end.getMonth(), end.getDate());

  const msPerDay = 1000 * 60 * 60 * 24;
  const diffDays = Math.round((utcEnd - utcStart) / msPerDay);

  // If check-out is same day or earlier, default to 1 night minimum
  return diffDays > 0 ? diffDays : 1;
}

function calculateBill({
  checkInDate,
  checkOutDate = new Date(),
  pricePerNight,
  additionalCharges = 0,
  taxRate = 0.12, // 12% GST
  alreadyPaid = 0,
}) {
  const nights = calculateNights(checkInDate, checkOutDate);
  const roomPrice = Number(pricePerNight) || 0;
  const extraCharges = Number(additionalCharges) || 0;
  const paid = Number(alreadyPaid) || 0;
  const rate = Number(taxRate) || 0.12;

  const roomCharges = Number((nights * roomPrice).toFixed(2));
  const subtotal = Number((roomCharges + extraCharges).toFixed(2));
  const taxAmount = Number((subtotal * rate).toFixed(2));
  const totalAmount = Number((subtotal + taxAmount).toFixed(2));
  const balanceDue = Number(Math.max(0, totalAmount - paid).toFixed(2));

  return {
    numberOfNights: nights,
    pricePerNight: roomPrice,
    roomCharges,
    additionalCharges: extraCharges,
    subtotal,
    taxRate: rate,
    taxAmount,
    totalAmount,
    alreadyPaid: paid,
    balanceDue,
  };
}

module.exports = {
  calculateNights,
  calculateBill,
};
