package Model;

public class MeetingSession {

	private String id;
	private Reservation reservation;
	private VirtualRoom virtualRoom;
	private int joinTimeMins;
	private int leaveTimeMins;
	private SessionStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public VirtualRoom getVirtualRoom() {
        return virtualRoom;
    }

    public void setVirtualRoom(VirtualRoom virtualRoom) {
        this.virtualRoom = virtualRoom;
    }

    public int getJoinTimeMins() {
        return joinTimeMins;
    }

    public void setJoinTimeMins(int joinTimeMins) {
        this.joinTimeMins = joinTimeMins;
    }

    public int getLeaveTimeMins() {
        return leaveTimeMins;
    }

    public void setLeaveTimeMins(int leaveTimeMins) {
        this.leaveTimeMins = leaveTimeMins;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

	/**
	 * 
	 * @param joinTime
	 */
	public void start(int joinTime) {
		this.joinTimeMins = joinTime;
		this.status = SessionStatus.IN_PROGRESS;
	}

	/**
	 * 
	 * @param leaveTime
	 * @param reason
	 */
	public void end(int leaveTime, String reason) {
		this.leaveTimeMins = leaveTime;
		if (reason == null || reason.trim().isEmpty()) {
			this.status = SessionStatus.ATTENDED;
		} else {
			this.status = SessionStatus.ENDED_EARLY;
		}
	}

	public void markNoShow() {
		this.status = SessionStatus.NO_SHOW;
	}

	public AttendanceRecord createAttendanceRecord() {
		AttendanceRecord record = new AttendanceRecord();
		record.setId("ATT-" + System.nanoTime());
		record.setReservationId(this.reservation.getId());
		record.setSession(this);
		record.setCandidate(this.reservation.getCandidate());
		record.setJoinTimeMins(this.joinTimeMins);
		record.setLeaveTimeMins(this.leaveTimeMins);
		// Determine outcome based on status
		switch (this.status) {
			case ATTENDED:
				record.setOutcome(AttendanceOutcome.ATTENDED);
				break;
			case NO_SHOW:
				record.setOutcome(AttendanceOutcome.NO_SHOW);
				break;
			case ENDED_EARLY:
				record.setOutcome(AttendanceOutcome.ENDED_EARLY);
				break;
			case SCHEDULED:
			case IN_PROGRESS:
			default:
				record.setOutcome(AttendanceOutcome.ATTENDED); // default
				break;
		}
		return record;
	}

	/**
	 * 
	 * @param sys
	 */
	public void addAuditEntry(VCFSystem sys) {
		String details = "Meeting session for reservation " + this.reservation.getId() + " - Status: " + this.status;
		AuditEntry entry = AuditEntry.now(sys, AuditEntry.ACTOR_SYSTEM, "SYSTEM", AuditEntry.ACTION_MEETING_SESSION, details);
		sys.addAuditEntry(entry);
	}

	public Candidate getCandidate() {
		return this.reservation.getCandidate();
	}

	public AppointmentSlot getSlot() {
		return this.reservation.getSlot();
	}

}