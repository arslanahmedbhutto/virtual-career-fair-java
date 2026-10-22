package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Lobby {

    private VirtualRoom virtualRoom;
    private final List<Candidate> waiting = new ArrayList<>();

    public Lobby() {
        // empty default constructor
    }

    public Lobby(VirtualRoom virtualRoom) {
        this.virtualRoom = virtualRoom;
        if (virtualRoom != null) {
            virtualRoom.setLobby(this);
        }
    }

    public VirtualRoom getVirtualRoom() {
        return virtualRoom;
    }

    public void setVirtualRoom(VirtualRoom virtualRoom) {
        this.virtualRoom = virtualRoom;
        if (virtualRoom != null && virtualRoom.getLobby() != this) {
            virtualRoom.setLobby(this);
        }
    }

    /**
     * Add a candidate to the lobby queue.
     * Duplicate candidates are ignored.
     *
     * @param c candidate to add
     */
    public void addCandidate(Candidate c) {
        if (c == null || waiting.contains(c)) {
            return;
        }
        waiting.add(c);
        c.setCurrentLobby(this);
    }

    /**
     * Remove a candidate from the lobby queue.
     *
     * @param c candidate to remove
     */
    public void removeCandidate(Candidate c) {
        if (c == null) {
            return;
        }
        if (waiting.remove(c)) {
            c.setCurrentLobby(null);
        }
    }

    /**
     * Admit the waiting candidate at the given position if the expected session time has arrived.
     *
     * @param c index of the candidate in the waiting list
     * @param time current time in minutes
     */
    public void admitWhenReady(int c, int time) {
        if (c < 0 || c >= waiting.size()) {
            return;
        }
        Candidate candidate = waiting.get(c);
        if (isReadyToAdmit(candidate, time)) {
            removeCandidate(candidate);
        }
    }

    /**
     * Check all waiting candidates and admit any whose reservation time is now.
     *
     * @param time current time in minutes
     */
    public void checkAndAdmitAll(int time) {
        for (int i = waiting.size() - 1; i >= 0; i--) {
            Candidate candidate = waiting.get(i);
            if (isReadyToAdmit(candidate, time)) {
                removeCandidate(candidate);
            }
        }
    }

    public List<Candidate> getWaiting() {
        return Collections.unmodifiableList(waiting);
    }

    /**
     * Returns true when the candidate is currently in the lobby.
     *
     * @param c candidate to test
     * @return true if waiting
     */
    public boolean isWaiting(Candidate c) {
        return c != null && waiting.contains(c);
    }

    public int size() {
        return waiting.size();
    }

    private boolean isReadyToAdmit(Candidate candidate, int time) {
        if (candidate == null || virtualRoom == null || virtualRoom.getBooth() == null) {
            return false;
        }
        for (Reservation reservation : candidate.getConfirmedReservation()) {
            if (reservation == null || reservation.getSlot() == null) {
                continue;
            }
            if (Objects.equals(reservation.getBooth(), virtualRoom.getBooth())
                    && reservation.getSlot().getStartMins() <= time) {
                return true;
            }
        }
        return false;
    }
}
