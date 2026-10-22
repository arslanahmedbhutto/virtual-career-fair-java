package Test;

import Model.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the MeetingSession model class.
 *
 * These tests verify the functionality of MeetingSession methods including
 * starting and ending sessions, marking no-shows, creating attendance records,
 * adding audit entries, and retrieving candidate and slot information.
 */
public class MeetingSessionTest {

    private MeetingSession session;
    private Reservation reservation;
    private VirtualRoom virtualRoom;
    private Candidate candidate;
    private Recruiter recruiter;
    private AppointmentSlot slot;
    private Booth booth;
    private VCFSystem system;

    /**
     * Set up test fixtures before each test.
     * Creates a MeetingSession with associated Reservation, VirtualRoom, etc.
     */
    @Before
    public void setUp() {
        // Create test data
        candidate = new Candidate("can1", "John Doe", "john@example.com");
        recruiter = new Recruiter("rec1", "Jane Recruiter", "jane@example.com");
        slot = new AppointmentSlot();
        Organization org = new Organization("org1", "Test Org", "Description", "www.test.com");
        booth = new Booth("booth1", "Booth 1", org);
        reservation = new Reservation("res1", candidate, recruiter, slot, booth, 100);
        virtualRoom = new VirtualRoom();
        system = new VCFSystem();

        // Create the session
        session = new MeetingSession();
        session.setId("session1");
        session.setReservation(reservation);
        session.setVirtualRoom(virtualRoom);
        session.setStatus(SessionStatus.SCHEDULED);
    }

    /**
     * Test that starting a session sets the correct status and join time.
     */
    @Test
    public void testStart() {
        System.out.println("Running testStart...");
        int joinTime = 120;
        session.start(joinTime);

        assertEquals("Session status should be IN_PROGRESS after start", SessionStatus.IN_PROGRESS, session.getStatus());
        assertEquals("Join time should be set correctly", joinTime, session.getJoinTimeMins());
        System.out.println("testStart passed: Status = " + session.getStatus() + ", JoinTime = " + session.getJoinTimeMins());
    }

    /**
     * Test ending a session with no reason sets status to ATTENDED.
     */
    @Test
    public void testEndWithNoReason() {
        System.out.println("Running testEndWithNoReason...");
        int leaveTime = 180;
        session.end(leaveTime, null);

        assertEquals("Session status should be ATTENDED when ended with no reason", SessionStatus.ATTENDED, session.getStatus());
        assertEquals("Leave time should be set correctly", leaveTime, session.getLeaveTimeMins());
        System.out.println("testEndWithNoReason passed: Status = " + session.getStatus() + ", LeaveTime = " + session.getLeaveTimeMins());
    }

    /**
     * Test ending a session with an empty reason sets status to ATTENDED.
     */
    @Test
    public void testEndWithEmptyReason() {
        System.out.println("Running testEndWithEmptyReason...");
        int leaveTime = 180;
        session.end(leaveTime, "");

        assertEquals("Session status should be ATTENDED when ended with empty reason", SessionStatus.ATTENDED, session.getStatus());
        assertEquals("Leave time should be set correctly", leaveTime, session.getLeaveTimeMins());
        System.out.println("testEndWithEmptyReason passed: Status = " + session.getStatus() + ", LeaveTime = " + session.getLeaveTimeMins());
    }

    /**
     * Test ending a session with a reason sets status to ENDED_EARLY.
     */
    @Test
    public void testEndWithReason() {
        System.out.println("Running testEndWithReason...");
        int leaveTime = 150;
        session.end(leaveTime, "Candidate left early");

        assertEquals("Session status should be ENDED_EARLY when ended with reason", SessionStatus.ENDED_EARLY, session.getStatus());
        assertEquals("Leave time should be set correctly", leaveTime, session.getLeaveTimeMins());
        System.out.println("testEndWithReason passed: Status = " + session.getStatus() + ", LeaveTime = " + session.getLeaveTimeMins());
    }

    /**
     * Test marking a session as no-show sets the correct status.
     */
    @Test
    public void testMarkNoShow() {
        System.out.println("Running testMarkNoShow...");
        session.markNoShow();

        assertEquals("Session status should be NO_SHOW", SessionStatus.NO_SHOW, session.getStatus());
        System.out.println("testMarkNoShow passed: Status = " + session.getStatus());
    }

    /**
     * Test creating an attendance record for an attended session.
     */
    @Test
    public void testCreateAttendanceRecordAttended() {
        System.out.println("Running testCreateAttendanceRecordAttended...");
        session.setStatus(SessionStatus.ATTENDED);
        session.setJoinTimeMins(120);
        session.setLeaveTimeMins(180);

        AttendanceRecord record = session.createAttendanceRecord();

        assertNotNull("Attendance record should be created", record);
        assertEquals("Reservation ID should match", reservation.getId(), record.getReservationId());
        assertEquals("Session should match", session, record.getSession());
        assertEquals("Candidate should match", candidate, record.getCandidate());
        assertEquals("Join time should match", 120, record.getJoinTimeMins());
        assertEquals("Leave time should match", 180, record.getLeaveTimeMins());
        assertEquals("Outcome should be ATTENDED", AttendanceOutcome.ATTENDED, record.getOutcome());
        System.out.println("testCreateAttendanceRecordAttended passed: Outcome = " + record.getOutcome());
    }

    /**
     * Test creating an attendance record for a no-show session.
     */
    @Test
    public void testCreateAttendanceRecordNoShow() {
        System.out.println("Running testCreateAttendanceRecordNoShow...");
        session.setStatus(SessionStatus.NO_SHOW);

        AttendanceRecord record = session.createAttendanceRecord();

        assertEquals("Outcome should be NO_SHOW", AttendanceOutcome.NO_SHOW, record.getOutcome());
        System.out.println("testCreateAttendanceRecordNoShow passed: Outcome = " + record.getOutcome());
    }

    /**
     * Test creating an attendance record for an early-ended session.
     */
    @Test
    public void testCreateAttendanceRecordEndedEarly() {
        System.out.println("Running testCreateAttendanceRecordEndedEarly...");
        session.setStatus(SessionStatus.ENDED_EARLY);

        AttendanceRecord record = session.createAttendanceRecord();

        assertEquals("Outcome should be ENDED_EARLY", AttendanceOutcome.ENDED_EARLY, record.getOutcome());
        System.out.println("testCreateAttendanceRecordEndedEarly passed: Outcome = " + record.getOutcome());
    }

    /**
     * Test adding an audit entry to the system.
     */
    @Test
    public void testAddAuditEntry() {
        System.out.println("Running testAddAuditEntry...");
        int initialSize = system.getAuditEntries().size();
        session.addAuditEntry(system);

        assertEquals("Audit entry should be added to system", initialSize + 1, system.getAuditEntries().size());
        AuditEntry lastEntry = system.getAuditEntries().get(system.getAuditEntries().size() - 1);
        assertEquals("Audit action should be MEETING_SESSION", AuditEntry.ACTION_MEETING_SESSION, lastEntry.getAction());
        assertTrue("Audit details should contain reservation ID", lastEntry.getDetails().contains(reservation.getId()));
        System.out.println("testAddAuditEntry passed: Audit entries count = " + system.getAuditEntries().size());
    }

    /**
     * Test getting the candidate from the session.
     */
    @Test
    public void testGetCandidate() {
        System.out.println("Running testGetCandidate...");
        Candidate result = session.getCandidate();

        assertEquals("Should return the candidate from reservation", candidate, result);
        System.out.println("testGetCandidate passed: Candidate = " + result.getName());
    }

    /**
     * Test getting the slot from the session.
     */
    @Test
    public void testGetSlot() {
        System.out.println("Running testGetSlot...");
        AppointmentSlot result = session.getSlot();

        assertEquals("Should return the slot from reservation", slot, result);
        System.out.println("testGetSlot passed: Slot retrieved successfully");
    }

    /**
     * Main method to run tests manually and show output
     */
    public static void main(String[] args) {
        MeetingSessionTest test = new MeetingSessionTest();
        test.setUp();

        System.out.println("=== Running MeetingSession Tests Manually ===\n");

        try {
            test.testStart();
            System.out.println("✓ testStart PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testStart FAILED: " + e.getMessage() + "\n");
        }

        test.setUp(); // Reset for next test
        try {
            test.testEndWithNoReason();
            System.out.println("✓ testEndWithNoReason PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testEndWithNoReason FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testEndWithEmptyReason();
            System.out.println("✓ testEndWithEmptyReason PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testEndWithEmptyReason FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testEndWithReason();
            System.out.println("✓ testEndWithReason PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testEndWithReason FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testMarkNoShow();
            System.out.println("✓ testMarkNoShow PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testMarkNoShow FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testCreateAttendanceRecordAttended();
            System.out.println("✓ testCreateAttendanceRecordAttended PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testCreateAttendanceRecordAttended FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testCreateAttendanceRecordNoShow();
            System.out.println("✓ testCreateAttendanceRecordNoShow PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testCreateAttendanceRecordNoShow FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testCreateAttendanceRecordEndedEarly();
            System.out.println("✓ testCreateAttendanceRecordEndedEarly PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testCreateAttendanceRecordEndedEarly FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testAddAuditEntry();
            System.out.println("✓ testAddAuditEntry PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testAddAuditEntry FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testGetCandidate();
            System.out.println("✓ testGetCandidate PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testGetCandidate FAILED: " + e.getMessage() + "\n");
        }

        test.setUp();
        try {
            test.testGetSlot();
            System.out.println("✓ testGetSlot PASSED\n");
        } catch (Exception e) {
            System.out.println("✗ testGetSlot FAILED: " + e.getMessage() + "\n");
        }

        System.out.println("=== All Tests Completed ===");
    }
}