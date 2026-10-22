package Test;

import Model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * End-to-end tests for the fair lifecycle: setup, publishing offers, booking, automatic phase
 * changes driven by the simulated clock, and live sessions with attendance recording.
 */
public class VCFSystemTest {

    private static final int DAY = SystemTime.MINS_PER_DAY;

    private VCFSystem system;
    private Administrator admin;
    private Recruiter recruiter;
    private Booth booth;
    private Offer offer;

    @Before
    public void setUp() {
        system = new VCFSystem();
        admin = new Administrator();
        admin.setName("Admin");
        admin.setSystem(system);
        // Bookings: Day 1 09:00-17:00. Fair: Day 2 09:00-12:00.
        admin.setupFairTimings(9 * 60, 17 * 60, DAY + 9 * 60, DAY + 12 * 60);
        admin.advanceSystemState(); // DORMANT -> PREPARING

        Organization org = new Organization(null, "Northwind Analytics", "Data consultancy", "https://example.com");
        admin.addOrganisation(org);
        booth = new Booth(null, "Data Science Booth", org);
        admin.addBooth(booth, org);
        recruiter = new Recruiter("REC-1", "Rita Recruiter", "rita@example.com");
        admin.assignRecruiter(recruiter, booth);

        offer = system.publishOffer(recruiter, "Graduate Data Analyst", "Entry-level role", 30, 1, List.of("data"));
        system.addAvailability(recruiter, offer, DAY + 9 * 60, DAY + 10 * 60, 0);
    }

    @Test
    public void availabilityIsSplitIntoSlotsOfTheOfferDuration() {
        assertEquals("09:00-10:00 with 30-minute slots should give 2 slots.", 2, offer.getSlots().size());
        assertEquals(DAY + 9 * 60, offer.getSlots().get(0).getStartMins());
        assertEquals(DAY + 9 * 60 + 30, offer.getSlots().get(1).getStartMins());
    }

    @Test
    public void clockMovesTheFairThroughItsPhases() {
        SystemTime clock = system.getSystemTime();
        clock.jumpTo(1, 10 * 60);
        assertEquals(SystemState.BOOKINGS_OPEN, system.getState());
        clock.jumpTo(1, 18 * 60);
        assertEquals(SystemState.BOOKINGS_CLOSED, system.getState());
        clock.jumpTo(2, 9 * 60);
        assertEquals(SystemState.FAIR_LIVE, system.getState());
        clock.jumpTo(2, 13 * 60);
        assertEquals(SystemState.FAIR_CLOSED, system.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void bookingIsRejectedBeforeBookingsOpen() {
        Candidate c = system.registerCandidate("Ava", "ava@example.com");
        system.bookSlot(c, offer.getSlots().get(0));
    }

    @Test
    public void bookingConfirmsReservationAndFillsSlot() {
        system.getSystemTime().jumpTo(1, 10 * 60);
        Candidate c = system.registerCandidate("Ava", "ava@example.com");
        Reservation r = system.bookSlot(c, offer.getSlots().get(0));
        assertEquals(ReservationStatus.CONFIRMED, r.getStatus());
        assertTrue(offer.getSlots().get(0).isFull());
        assertTrue(recruiter.getReservations().contains(r));
        assertEquals("Recruiter should be told about the booking.", 1, recruiter.getNotifications().size());
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateEmailRegistrationIsRejected() {
        system.registerCandidate("Ava", "ava@example.com");
        system.registerCandidate("Ava Again", "AVA@example.com");
    }

    @Test
    public void autoRequestSuggestsAndConfirmsMatchingSlots() {
        system.getSystemTime().jumpTo(1, 10 * 60);
        Candidate c = system.registerCandidate("Ava", "ava@example.com");
        AutoRequest req = new AutoRequest();
        req.setCandidate(c);
        req.addDesiredTag("data");
        req.setMaxAppointments(2);
        assertEquals(2, req.generate(system).size());
        assertTrue(req.confirmAutoBook(system));
        assertEquals(2, c.getConfirmedReservation().size());
        assertEquals("Each auto-booked slot should be counted once.", 1, offer.getSlots().get(0).getCurrentBookings());
    }

    @Test
    public void candidateWhoJoinsIsAdmittedAndAttendanceIsRecorded() {
        SystemTime clock = system.getSystemTime();
        clock.jumpTo(1, 10 * 60);
        Candidate c = system.registerCandidate("Ava", "ava@example.com");
        Reservation r = system.bookSlot(c, offer.getSlots().get(0));

        clock.jumpTo(2, 9 * 60);                          // fair goes live, slot starts
        system.joinSession(c, r);
        assertNotNull("Candidate should be admitted into the live session.", c.getCurrentMeetingSession());
        assertEquals(SessionStatus.IN_PROGRESS, c.getCurrentMeetingSession().getStatus());

        clock.jumpTo(2, 9 * 60 + 30);                     // slot over
        assertNull(c.getCurrentMeetingSession());
        AttendanceRecord rec = system.findAttendanceRecordByReservationId(r.getId());
        assertNotNull(rec);
        assertEquals(AttendanceOutcome.ATTENDED, rec.getOutcome());
    }

    @Test
    public void candidateWhoNeverJoinsIsMarkedNoShow() {
        SystemTime clock = system.getSystemTime();
        clock.jumpTo(1, 10 * 60);
        Candidate c = system.registerCandidate("Ava", "ava@example.com");
        Reservation r = system.bookSlot(c, offer.getSlots().get(1));
        clock.jumpTo(2, 9 * 60);
        clock.jumpTo(2, 10 * 60);
        AttendanceRecord rec = system.findAttendanceRecordByReservationId(r.getId());
        assertNotNull(rec);
        assertEquals(AttendanceOutcome.NO_SHOW, rec.getOutcome());
    }

    @Test
    public void auditLogRecordsAdministratorAndSystemActions() {
        system.getSystemTime().jumpTo(1, 10 * 60);
        boolean sawSetup = false;
        boolean sawTransition = false;
        for (AuditEntry e : system.getAuditEntries()) {
            sawSetup |= AuditEntry.ACTION_CONFIGURE_FAIR_TIMINGS.equals(e.getAction());
            sawTransition |= AuditEntry.ACTION_PHASE_TRANSITION.equals(e.getAction());
        }
        assertTrue(sawSetup);
        assertTrue(sawTransition);
    }
}
