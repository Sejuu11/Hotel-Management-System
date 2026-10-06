package com.hotelmanagement.service;

import com.hotelmanagement.dao.BookingDAO;
import com.hotelmanagement.dao.CustomerDAO;
import com.hotelmanagement.dao.ReportDAO;
import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemWorkflowIntegrationTest {

    private static AuthService authService;
    private static CustomerService customerService;
    private static EmployeeService employeeService;
    private static RoomService roomService;
    private static BookingService bookingService;
    private static CheckInOutService checkInOutService;
    private static PaymentService paymentService;
    private static ReportService reportService;

    private static int testCustomerId;
    private static int testRoomId;
    private static int testBookingId;

    @BeforeAll
    public static void setUp() {
        authService = AuthService.getInstance();
        customerService = new CustomerService();
        employeeService = new EmployeeService();
        roomService = new RoomService();
        bookingService = new BookingService();
        checkInOutService = new CheckInOutService();
        paymentService = new PaymentService();
        reportService = new ReportService();
    }

    @Test
    @Order(1)
    @DisplayName("1. Authentication: Test Admin, Receptionist, and Invalid Logins")
    public void testAuthentication() {
        // Admin login
        User admin = authService.login("admin", "admin123");
        assertNotNull(admin);
        assertEquals("ADMIN", admin.getRole());
        assertTrue(admin.isAdmin());
        assertTrue(authService.isAdmin());

        // Receptionist login
        User recep = authService.login("receptionist", "recep123");
        assertNotNull(recep);
        assertEquals("RECEPTIONIST", recep.getRole());
        assertFalse(recep.isAdmin());
        assertFalse(authService.isAdmin());

        // Invalid password
        assertThrows(ValidationException.class, () -> authService.login("admin", "wrongpassword"));

        // Invalid username
        assertThrows(ValidationException.class, () -> authService.login("unknown_user", "admin123"));

        // Blank input
        assertThrows(ValidationException.class, () -> authService.login("", "admin123"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Dashboard: Real MySQL Dynamic Metrics & Breakdowns")
    public void testDashboardMetrics() {
        DashboardStats stats = reportService.getDashboardStatistics();
        assertNotNull(stats);
        assertTrue(stats.getTotalRooms() >= 10, "Total rooms should be at least 10 from seed data");
        assertTrue(stats.getTotalCustomers() >= 5, "Total customers should be at least 5");
        assertTrue(stats.getActiveBookings() >= 1, "There should be active seed bookings");
        assertNotNull(stats.getTotalRevenue());
        assertFalse(stats.getRoomsByStatus().isEmpty(), "Room status breakdown should be populated");
        assertFalse(stats.getRoomsByType().isEmpty(), "Room type breakdown should be populated");
    }

    @Test
    @Order(3)
    @DisplayName("3. Reports: SQL JOINs, GROUP BY, and Aggregate Function Queries")
    public void testReportsQueries() {
        // Multi-table JOIN Booking Report
        List<Object[]> bookingRows = reportService.getBookingReport(null, null, "All");
        assertNotNull(bookingRows);
        assertFalse(bookingRows.isEmpty(), "Booking report with JOINs should return records");

        // GROUP BY Revenue Report with COUNT, SUM, AVG, MIN, MAX
        List<Object[]> revenueRows = reportService.getRevenueReport();
        assertNotNull(revenueRows);
        assertFalse(revenueRows.isEmpty(), "Revenue report should return aggregate rows");

        // GROUP BY Room Utilization Report
        List<Object[]> roomRows = reportService.getRoomReport();
        assertNotNull(roomRows);
        assertFalse(roomRows.isEmpty(), "Room report with GROUP BY room_type should return records");

        // GROUP BY Employee Payroll Report
        List<Object[]> empRows = reportService.getEmployeeReport();
        assertNotNull(empRows);
        assertFalse(empRows.isEmpty(), "Employee report with GROUP BY designation should return records");
    }

    @Test
    @Order(4)
    @DisplayName("4. Customer CRUD & Search Operations")
    public void testCustomerLifecycle() {
        Customer c = new Customer();
        c.setFullName("Workflow Test Guest");
        c.setGender("Male");
        c.setDateOfBirth(LocalDate.of(1990, 5, 15));
        c.setPhone("9123456780");
        c.setEmail("workflow.guest@example.com");
        c.setIdType("Passport");
        c.setIdNumber("TEST-PASS-9988");
        c.setAddress("77 Innovation Way");

        assertTrue(customerService.addCustomer(c));
        testCustomerId = c.getCustomerId();
        assertTrue(testCustomerId > 0);

        // Search
        List<Customer> searchResults = customerService.searchCustomers("TEST-PASS-9988");
        assertFalse(searchResults.isEmpty());
        assertEquals("Workflow Test Guest", searchResults.get(0).getFullName());

        // Update
        c.setPhone("9988776655");
        assertTrue(customerService.updateCustomer(c));
        Customer updated = customerService.getCustomerById(testCustomerId);
        assertEquals("9988776655", updated.getPhone());
    }

    @Test
    @Order(5)
    @DisplayName("5. Room Management: Duplicate Prevention & Constraints")
    public void testRoomConstraints() {
        // Create test room
        Room room = new Room();
        room.setRoomNumber("888");
        room.setRoomType("Deluxe");
        room.setFloor(8);
        room.setPricePerNight(new BigDecimal("3500.00"));
        room.setStatus("AVAILABLE");
        room.setDescription("Top floor test executive suite");

        // Delete if already existed from prior run
        Room existing = new RoomDAO().findByRoomNumber("888");
        if (existing != null) {
            new RoomDAO().delete(existing.getRoomId());
        }

        assertTrue(roomService.addRoom(room));
        testRoomId = room.getRoomId();
        assertTrue(testRoomId > 0);

        // Duplicate room number must fail
        Room duplicate = new Room();
        duplicate.setRoomNumber("888");
        duplicate.setRoomType("Single");
        duplicate.setFloor(8);
        duplicate.setPricePerNight(new BigDecimal("2000.00"));
        duplicate.setStatus("AVAILABLE");

        assertThrows(ValidationException.class, () -> roomService.addRoom(duplicate),
            "Duplicate room number should be rejected by validation");
    }

    @Test
    @Order(6)
    @DisplayName("6. Complete Hotel Workflow: AVAILABLE -> RESERVED -> OCCUPIED -> AVAILABLE")
    public void testCompleteBookingAndCheckInOutWorkflow() {
        // Step A: Create Booking for test room 888 (currently AVAILABLE)
        Booking booking = new Booking();
        booking.setCustomerId(testCustomerId);
        booking.setRoomId(testRoomId);
        booking.setCheckInDate(LocalDate.now());
        booking.setExpectedCheckOutDate(LocalDate.now().plusDays(2));
        booking.setNumberOfGuests(2);
        booking.setSpecialRequest("Corner view requested");

        assertTrue(bookingService.createBooking(booking));
        testBookingId = booking.getBookingId();
        assertTrue(testBookingId > 0);

        // Verify status transitions: Booking -> RESERVED, Room -> RESERVED
        Booking savedBooking = bookingService.getBookingById(testBookingId);
        assertEquals("RESERVED", savedBooking.getBookingStatus());

        Room reservedRoom = roomService.getRoomById(testRoomId);
        assertEquals("RESERVED", reservedRoom.getStatus());

        // Step B: Double-booking prevention
        Booking conflicting = new Booking();
        conflicting.setCustomerId(testCustomerId);
        conflicting.setRoomId(testRoomId);
        conflicting.setCheckInDate(LocalDate.now());
        conflicting.setExpectedCheckOutDate(LocalDate.now().plusDays(1));
        conflicting.setNumberOfGuests(1);

        assertThrows(ValidationException.class, () -> bookingService.createBooking(conflicting),
            "Should not allow double-booking on RESERVED room");

        // Step C: Check-In Workflow
        assertTrue(checkInOutService.checkIn(testBookingId));

        Booking checkedInBooking = bookingService.getBookingById(testBookingId);
        assertEquals("CHECKED_IN", checkedInBooking.getBookingStatus());

        Room occupiedRoom = roomService.getRoomById(testRoomId);
        assertEquals("OCCUPIED", occupiedRoom.getStatus());

        // Step D: Payment Calculation & Validation
        BillDetails bill = checkInOutService.calculateBill(testBookingId, new BigDecimal("400.00"), new BigDecimal("0.12"));
        assertNotNull(bill);
        assertTrue(bill.getGrandTotal().compareTo(BigDecimal.ZERO) > 0);
        BigDecimal balance = bill.getBalanceDue();
        assertTrue(balance.compareTo(BigDecimal.ZERO) > 0);

        // Attempt overpayment beyond total
        Payment overPayment = new Payment();
        overPayment.setBookingId(testBookingId);
        overPayment.setAmount(balance.add(new BigDecimal("5000.00")));
        overPayment.setPaymentMethod("CASH");

        assertThrows(ValidationException.class, () -> paymentService.recordPayment(overPayment),
            "Payment amount exceeding valid balance must be rejected");

        // Step E: Atomic Transactional Check-Out
        // Pay exactly the remaining balance
        assertTrue(checkInOutService.checkOut(testBookingId, new BigDecimal("400.00"), new BigDecimal("0.12"),
            balance, "CARD", "TXN-TEST-SETTLE-001"));

        // Step F: Verify final state
        Booking completedBooking = bookingService.getBookingById(testBookingId);
        assertEquals("CHECKED_OUT", completedBooking.getBookingStatus());
        assertNotNull(completedBooking.getActualCheckOutDate());

        // Room must become AVAILABLE again
        Room releasedRoom = roomService.getRoomById(testRoomId);
        assertEquals("AVAILABLE", releasedRoom.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("7. Transaction Rollback: Verify DB state remains unchanged on failure")
    public void testCheckoutTransactionRollback() throws Exception {
        // Create a new reservation and check-in to test rollback
        Room room = new Room();
        room.setRoomNumber("777");
        room.setRoomType("Single");
        room.setFloor(7);
        room.setPricePerNight(new BigDecimal("1000.00"));
        room.setStatus("AVAILABLE");

        Room existing = new RoomDAO().findByRoomNumber("777");
        if (existing != null) new RoomDAO().delete(existing.getRoomId());
        roomService.addRoom(room);
        int rId = room.getRoomId();

        Booking b = new Booking();
        b.setCustomerId(testCustomerId);
        b.setRoomId(rId);
        b.setCheckInDate(LocalDate.now());
        b.setExpectedCheckOutDate(LocalDate.now().plusDays(1));
        b.setNumberOfGuests(1);
        bookingService.createBooking(b);
        int bId = b.getBookingId();

        checkInOutService.checkIn(bId);

        // Verify initial state is CHECKED_IN and OCCUPIED
        assertEquals("CHECKED_IN", bookingService.getBookingById(bId).getBookingStatus());
        assertEquals("OCCUPIED", roomService.getRoomById(rId).getStatus());

        // Now attempt an invalid checkout where payment exceeds balance (validation failure before/during commit)
        assertThrows(ValidationException.class, () -> {
            checkInOutService.checkOut(bId, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("999999.00"), "CASH", "FAIL-TXN");
        });

        // State MUST remain CHECKED_IN and OCCUPIED (no partial commit occurred)
        assertEquals("CHECKED_IN", bookingService.getBookingById(bId).getBookingStatus());
        assertEquals("OCCUPIED", roomService.getRoomById(rId).getStatus());

        // Cleanup rollback test records
        new BookingDAO().delete(bId);
        roomService.deleteRoom(rId);
    }

    @Test
    @Order(8)
    @DisplayName("8. Clean Up Test Records")
    public void testCleanUp() {
        // Cleanup test booking, room, and customer
        if (testBookingId > 0) {
            new BookingDAO().delete(testBookingId);
        }
        if (testRoomId > 0) {
            roomService.deleteRoom(testRoomId);
        }
        if (testCustomerId > 0) {
            customerService.deleteCustomer(testCustomerId);
        }
    }
}
