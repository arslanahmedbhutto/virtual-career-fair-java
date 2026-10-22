package Test;

import Model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/** Unit tests for reservations, slot capacity and clash detection. */
public class ReservationTest {

    private Candidate candidate;
    private Recruiter recruiter;
    private Booth booth;
    private AppointmentSlot slot;

    @Before
    public void setUp() {
        candidate = new Candidate("can1", "Marvin", "marvin@example.com");
        recruiter = new Recruiter("rec1", "Jim", "jim@example.com");
        Organization org = new Organization("org1", "Test Org", "Description", "https://example.com");
        booth = new Booth("booth1", "Booth 1", org);
        booth.assignRecruiter(recruiter);
        Offer offer = new Offer("off1", "Graduate role", "desc", 30, 1, recruiter, booth, new ArrayList<>());
        booth.addOffer(offer);
        slot = new AppointmentSlot();
        slot.setStartMins(600);
        slot.setEndMins(630);
        slot.setOffer(offer);
    }

    @Test
    public void constructorStoresCandidateDetails() {
        assertEquals("can1", candidate.getUserId());
        assertEquals("Marvin", candidate.getName());
        assertEquals("marvin@example.com", candidate.getEmail());
    }

    @Test
    public void newReservationIsPendingAndConfirmBooksSlot() {
        Reservation res = new Reservation("res1", candidate, recruiter, slot, booth, 10);
        assertEquals(ReservationStatus.PENDING, res.getStatus());
        res.confirm();
        assertEquals(ReservationStatus.CONFIRMED, res.getStatus());
        assertTrue("Single-capacity slot should be full after one booking.", slot.isFull());
    }

    @Test
    public void confirmingTwiceDoesNotDoubleBookTheSlot() {
        slot.setMaxBookings(2);
        Reservation res = new Reservation("res1", candidate, recruiter, slot, booth, 10);
        res.confirm();
        res.confirm();
        assertEquals(1, slot.getCurrentBookings());
    }

    @Test
    public void candidateCancellationFreesSlotAndNotifiesRecruiter() {
        Reservation res = new Reservation("res1", candidate, recruiter, slot, booth, 10);
        res.confirm();
        res.cancel(candidate.getUserId());
        res.notifyOtherParty();
        assertEquals(ReservationStatus.CANCELLED_BY_CANDIDATE, res.getStatus());
        assertFalse(res.isActive());
        assertFalse("Cancelled slot should be bookable again.", slot.isFull());
        assertEquals(1, recruiter.getNotifications().size());
    }

    @Test
    public void recruiterCancellationNotifiesCandidate() {
        Reservation res = new Reservation("res1", candidate, recruiter, slot, booth, 10);
        res.confirm();
        recruiter.trackReservation(res);
        recruiter.cancelReservation(res);
        assertEquals(ReservationStatus.CANCELLED_BY_RECRUITER, res.getStatus());
        assertEquals(1, candidate.getNotifications().size());
    }

    @Test
    public void overlappingSlotsClashButAdjacentOnesDoNot() {
        Reservation res = new Reservation("res1", candidate, recruiter, slot, booth, 10);
        res.confirm();
        candidate.addReservation(res);

        AppointmentSlot overlapping = new AppointmentSlot();
        overlapping.setStartMins(615);
        overlapping.setEndMins(645);
        AppointmentSlot adjacent = new AppointmentSlot();
        adjacent.setStartMins(630);
        adjacent.setEndMins(660);

        assertTrue(candidate.hasClash(overlapping));
        assertFalse(candidate.hasClash(adjacent));
    }

    @Test
    public void cancelledReservationNoLongerClashes() {
        Reservation res = new Reservation("res1", candidate, recruiter, slot, booth, 10);
        res.confirm();
        candidate.addReservation(res);
        res.cancel(candidate.getUserId());
        assertFalse(candidate.hasClash(slot));
    }
}
