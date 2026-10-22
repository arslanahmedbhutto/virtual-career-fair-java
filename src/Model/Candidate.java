package Model;
import java.util.ArrayList;
import java.util.List;

public class Candidate extends User {
    
	private List<Reservation> reservation;
	private MeetingSession currentMeetingSession;
	private Lobby currentLobby;
        
        
        public Candidate(String id , String name, String email){
            super();
            this.reservation= new ArrayList<>();
            setUserId(id);
            setName(name);
            setEmail(email);
        }

    public MeetingSession getCurrentMeetingSession() {
        return currentMeetingSession;
    }

    public void setCurrentMeetingSession(MeetingSession currentMeetingSession) {
        this.currentMeetingSession = currentMeetingSession;
    }

    /** All reservations, including cancelled ones (newest last). */
    public List<Reservation> getReservations() {
        return java.util.Collections.unmodifiableList(reservation);
    }

    public Lobby getCurrentLobby() {
        return currentLobby;
    }

    public void setCurrentLobby(Lobby currentLobby) {
        this.currentLobby = currentLobby;
    }
        
        
        

	// Adds a new reservation to the candidate's list
	// @param r - the reservation to add
	public void addReservation(Reservation r) {
		if (r != null) {
			reservation.add(r);
		}
	}

	// Removes a reservation from the candidate's list
	// @param r the reservation to remove
	public void removeReservation(Reservation r) {
		if (r != null) {
			reservation.remove(r);
		}
	}

	// Checks if the given appointment slot clashes with any existing reservation
	// @param s - the appointment slot to check
	// @return true if there is a conflict in time and false otherwise
	public boolean hasClash(AppointmentSlot s) {
		if (s == null) return false;
		// Loop through all reservations
		for (Reservation r : reservation) {
			if (!r.isActive()) continue;
			AppointmentSlot existing = r.getSlot();
			// Check if the existing slot overlaps with the new slot
			if (existing != null && existing.checkClash(s)) {
				return true;
			}
		}
		return false;
	}
	// This returns a list of only confirmed reservations
	// @return list of confirmed reservations
	public List<Reservation> getConfirmedReservation() {
		List<Reservation> confirmed = new ArrayList<>();
		// Filter reservations by CONFIRMED status
		for (Reservation r : reservation) {
			if (r.getStatus() == ReservationStatus.CONFIRMED) {
				confirmed.add(r);
			}
		}
		return confirmed;
	}

	// Registers the candidate by setting name and email
	// @param name - candidate's name
	// @param email - candidate's email
	public void register(String name, String email) {
		this.setName(name);
		this.setEmail(email);
	}

}