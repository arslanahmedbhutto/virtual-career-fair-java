package Test;

import Model.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the Lobby model.
 *
 * These tests verify that the lobby queue behaves correctly when candidates
 * are added, removed, and admitted based on reservation timing.
 */
public class LobbyTest {

    private Candidate candidate1;
    private Candidate candidate2;
    private VirtualRoom virtualRoom;
    private Lobby lobby;

    /**
     * Create a fresh lobby and candidate fixtures before each test.
     */
    @Before
    public void setUp() {
        candidate1 = new Candidate("can1", "Marvin", "marv@example.com");
        candidate2 = new Candidate("can2", "Ava", "ava@example.com");
        virtualRoom = new VirtualRoom();
        lobby = new Lobby(virtualRoom);
    }

    /**
     * Verify that the Lobby constructor links the lobby to the virtual room
     * and that the virtual room also references the lobby.
     */
    @Test
    public void testLobbyConstructorSetsRoomReference() {
        System.out.println("Running: testLobbyConstructorSetsRoomReference");
        assertSame("Lobby should keep reference to the virtual room.", virtualRoom, lobby.getVirtualRoom());
        assertSame("VirtualRoom should reference its lobby.", lobby, virtualRoom.getLobby());
        System.out.println("Passed: testLobbyConstructorSetsRoomReference");
    }

    /**
     * Verify that addCandidate enqueues a candidate, marks them as waiting,
     * and stores the current lobby on the candidate.
     */
    @Test
    public void testAddCandidateAddsToQueueAndSetsCurrentLobby() {
        System.out.println("Running: testAddCandidateAddsToQueueAndSetsCurrentLobby");
        lobby.addCandidate(candidate1);

        assertTrue("Candidate should be waiting after add.", lobby.isWaiting(candidate1));
        assertEquals("Lobby should contain one candidate.", 1, lobby.size());
        assertSame("Candidate current lobby should be set.", lobby, candidate1.getCurrentLobby());
        System.out.println("Passed: testAddCandidateAddsToQueueAndSetsCurrentLobby");
    }

    /**
     * Verify duplicate candidates are not added to the waiting queue.
     */
    @Test
    public void testAddCandidateDoesNotAddDuplicates() {
        System.out.println("Running: testAddCandidateDoesNotAddDuplicates");
        lobby.addCandidate(candidate1);
        lobby.addCandidate(candidate1);

        assertEquals("Duplicate candidate should not be added.", 1, lobby.size());
        System.out.println("Passed: testAddCandidateDoesNotAddDuplicates");
    }

    /**
     * Verify removeCandidate removes a candidate from the queue and clears
     * the candidate's current lobby reference.
     */
    @Test
    public void testRemoveCandidateRemovesAndClearsCurrentLobby() {
        System.out.println("Running: testRemoveCandidateRemovesAndClearsCurrentLobby");
        lobby.addCandidate(candidate1);
        lobby.removeCandidate(candidate1);

        assertFalse("Candidate should no longer be waiting.", lobby.isWaiting(candidate1));
        assertEquals("Lobby should be empty after removal.", 0, lobby.size());
        assertNull("Candidate current lobby should be cleared.", candidate1.getCurrentLobby());
        System.out.println("Passed: testRemoveCandidateRemovesAndClearsCurrentLobby");
    }

    /**
     * Verify the waiting list is exposed as an unmodifiable view.
     */
    @Test
    public void testGetWaitingReturnsUnmodifiableList() {
        System.out.println("Running: testGetWaitingReturnsUnmodifiableList");
        lobby.addCandidate(candidate1);
        try {
            lobby.getWaiting().add(candidate2);
            fail("Waiting list should be unmodifiable.");
        } catch (UnsupportedOperationException expected) {
            // expected because getWaiting returns an unmodifiable list.
        }
        System.out.println("Passed: testGetWaitingReturnsUnmodifiableList");
    }

    /**
     * Verify admitWhenReady does not remove a candidate if they are not
     * ready based on current time.
     */
    @Test
    public void testAdmitWhenReadyDoesNothingWhenCandidateNotReady() {
        System.out.println("Running: testAdmitWhenReadyDoesNothingWhenCandidateNotReady");
        lobby.addCandidate(candidate1);
        lobby.admitWhenReady(0, 100);

        assertTrue("Candidate should remain in the lobby when not ready.", lobby.isWaiting(candidate1));
        assertEquals("Lobby size should remain unchanged.", 1, lobby.size());
        System.out.println("Passed: testAdmitWhenReadyDoesNothingWhenCandidateNotReady");
    }

    /**
     * Verify checkAndAdmitAll does not remove candidates when the reservation
     * time has not arrived.
     */
    @Test
    public void testCheckAndAdmitAllDoesNothingWhenNoReadyCandidate() {
        System.out.println("Running: testCheckAndAdmitAllDoesNothingWhenNoReadyCandidate");
        lobby.addCandidate(candidate1);
        lobby.checkAndAdmitAll(100);

        assertTrue("Candidate should remain waiting.", lobby.isWaiting(candidate1));
        assertEquals("Lobby size should remain unchanged.", 1, lobby.size());
        System.out.println("Passed: testCheckAndAdmitAllDoesNothingWhenNoReadyCandidate");
    }

    /**
     * Verify checkAndAdmitAll removes a candidate once their confirmed
     * reservation start time has arrived.
     */
    @Test
    public void testCheckAndAdmitAllRemovesReadyCandidate() {
        System.out.println("Running: testCheckAndAdmitAllRemovesReadyCandidate");
        Organization organization = new Organization("org1", "Test Org", "Description", "https://example.com");
        Booth booth = new Booth("b1", "Test Booth", organization);
        virtualRoom.setBooth(booth);

        AppointmentSlot slot = new AppointmentSlot();
        slot.setStartMins(50);
        slot.setEndMins(80);

        Recruiter recruiter = new Recruiter("rec1", "Rita", "rita@example.com");
        Reservation reservation = new Reservation("res1", candidate1, recruiter, slot, booth, 0);
        reservation.confirm();
        candidate1.addReservation(reservation);

        lobby.addCandidate(candidate1);
        lobby.checkAndAdmitAll(50);

        assertFalse("Ready candidate should be removed from lobby.", lobby.isWaiting(candidate1));
        assertNull("Candidate current lobby should be cleared.", candidate1.getCurrentLobby());
        System.out.println("Passed: testCheckAndAdmitAllRemovesReadyCandidate");
    }
}
