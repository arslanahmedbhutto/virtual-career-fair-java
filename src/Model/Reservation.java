package Model;


public class Reservation {

	private String id;
	private Candidate candidate;
        private Recruiter recruiter;
	private AppointmentSlot slot;
	private Booth booth;
	private ReservationStatus status;
	private int createdAtMins;
	private String cancelledBy;
        
        
        public Reservation() {
            this.status = ReservationStatus.PENDING;
        }

        public Reservation(String id, Candidate candidate , Recruiter recruiter, AppointmentSlot slot , Booth booth , int createdAtMins ){
            this.id=id;
            this.candidate=candidate;
            this.recruiter=recruiter;
            this.slot=slot;
            this.booth=booth;
            this.status=ReservationStatus.PENDING;
            this.createdAtMins = createdAtMins;
            this.cancelledBy = null;
        
        }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(Recruiter recruiter) {
        this.recruiter = recruiter;
    }

    public AppointmentSlot getSlot() {
        return slot;
    }

    public void setSlot(AppointmentSlot slot) {
        this.slot = slot;
    }

    public Booth getBooth() {
        return booth;
    }

    public void setBooth(Booth booth) {
        this.booth = booth;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public int getCreatedAtMins() {
        return createdAtMins;
    }

    public void setCreatedAtMins(int createdAtMins) {
        this.createdAtMins = createdAtMins;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

	public void confirm() {
            if (this.status == ReservationStatus.CONFIRMED) {
                return;
            }
            this.status = ReservationStatus.CONFIRMED;
            if (this.slot != null) {
                this.slot.markBooked();
            }
	
	}

	/**
	 * 
	 * @param cancelledBy
	 */
	public void cancel(String cancelledBy) {
            if (!isActive()) {
                return;
            }
            boolean wasConfirmed = this.status == ReservationStatus.CONFIRMED;
            this.cancelledBy = cancelledBy;
            if(candidate != null && java.util.Objects.equals(cancelledBy, candidate.getUserId())){
                this.status = ReservationStatus.CANCELLED_BY_CANDIDATE;
            }else{ this.status = ReservationStatus.CANCELLED_BY_RECRUITER;
            }
            
            if (wasConfirmed && this.slot != null) {
                this.slot.releaseBooking();
            }
		
	}

	public void notifyOtherParty() {
            int ts = createdAtMins;
            String when = slot != null ? SystemTime.format(slot.getStartMins()) : "unknown time";
            String boothName = booth != null ? booth.getName() : "the booth";
            if (this.cancelledBy == null) {
                if (recruiter != null && candidate != null) {
                    recruiter.addNotification(new Notification("RESERVATION",
                            candidate.getName() + " booked " + when + " at " + boothName + ".", ts));
                }
            } else if (candidate != null && this.cancelledBy.equals(candidate.getUserId())) {
                if (recruiter != null) {
                    recruiter.addNotification(new Notification("CANCELLATION",
                            candidate.getName() + " cancelled their " + when + " appointment.", ts));
                }
            } else if (candidate != null) {
                candidate.addNotification(new Notification("CANCELLATION",
                        "Your " + when + " appointment at " + boothName + " was cancelled by the recruiter.", ts));
                
           
            }
		
	}

	public boolean isActive() {
		return this.status == ReservationStatus.PENDING 
                || this.status == ReservationStatus.CONFIRMED;
	}

	public String getDetails() {
        return String.format("Reservation [%s]: %s meeting %s at Booth %s. Status: %s", 
                              this.id, this.candidate.getName(), this.recruiter.getName(), 
                              this.booth.getName(), this.status);
    }

}