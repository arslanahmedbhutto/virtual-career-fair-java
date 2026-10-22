package Model;
import java.util.ArrayList;
import java.util.List;

public class Recruiter extends User {
    
    private List<Offer> offers;
    private List<AvailabilityBlock> availabilityBlock;
    private List<Reservation>reservations;
    
    
    public Recruiter(String id , String name, String email){
        super();
        this.offers = new ArrayList<>();
        this.availabilityBlock = new ArrayList<>();
        this.reservations = new ArrayList<>();
        setUserId(id);
        setName(name);
        setEmail(email);
    }

    public List<Offer> getOffers() {
        return offers;
    }

    public List<AvailabilityBlock> getAvailabilityBlocks() {
        return availabilityBlock;
    }

    /** Records a reservation made against one of this recruiter's slots. */
    public void trackReservation(Reservation reservation) {
        if (reservation != null && !reservations.contains(reservation)) {
            reservations.add(reservation);
        }
    }

	/**
	 * 
	 * @param offer
	 */
	public void publishOffer(Offer offer) {
            if (!this.offers.contains(offer)) {
                this.offers.add(offer);
            }
            offer.setRecruiter(this);
            offer.publish();
            
            
	}

	/**
	 * 
	 * @param block
	 */
	public void addAvailBlock(AvailabilityBlock block) {
            for (AvailabilityBlock existing : availabilityBlock) {
                if (existing.getStartMins() < block.getEndMins() && existing.getEndMins() > block.getStartMins()) {
                    throw new IllegalStateException("Availability overlaps an existing block.");
                }
            }
            this.availabilityBlock.add(block);
            block.generateSlots();
		
	}

	/**
	 * 
	 * @param reservation
	 */
	public void cancelReservation(Reservation reservation) {
		if(this.reservations.contains(reservation) && reservation.isActive()){
                    reservation.cancel(this.getUserId());
                    reservation.notifyOtherParty();
                }
	}

	public List<Reservation> getReservations() {
		return this.reservations;
	}

	/**
	 * 
	 * @param session
         * @param reason
         * @param timeLeave
	 */
        
	public void endSession(MeetingSession session ,int timeLeave, String reason) {
            session.end(timeLeave, reason);
	}

}