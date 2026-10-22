package Model;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRecord {

	private String id;
	private String reservationId;
	private MeetingSession session;
	private Candidate candidate;
	private AttendanceOutcome outcome;
	private int joinTimeMins;
	private int leaveTimeMins;
	private String notes;
	private final List<SubscriberInterface> subscribers = new ArrayList<>();

	public void addSubscriber(SubscriberInterface sub) {
		if (sub != null && !subscribers.contains(sub)) {
			subscribers.add(sub);
		}
	}

	public void removeSubscriber(SubscriberInterface sub) {
		subscribers.remove(sub);
	}

	protected void notifySubscribers() {
		for (SubscriberInterface sub : new ArrayList<>(subscribers)) {
			sub.update();
		}
	}

	public AttendanceOutcome getOutcome() {
		return this.outcome;
	}

	public void setOutcome(AttendanceOutcome outcome) {
		this.outcome = outcome;
		notifySubscribers();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		notifySubscribers();
	}

	public String getReservationId() {
		return reservationId;
	}

	public void setReservationId(String reservationId) {
		this.reservationId = reservationId;
		notifySubscribers();
	}

	public MeetingSession getSession() {
		return session;
	}

	public void setSession(MeetingSession session) {
		this.session = session;
		notifySubscribers();
	}

	public Candidate getCandidate() {
		return candidate;
	}

	public void setCandidate(Candidate candidate) {
		this.candidate = candidate;
		notifySubscribers();
	}

	public int getJoinTimeMins() {
		return joinTimeMins;
	}

	public void setJoinTimeMins(int joinTimeMins) {
		this.joinTimeMins = joinTimeMins;
		notifySubscribers();
	}

	public int getLeaveTimeMins() {
		return leaveTimeMins;
	}

	public void setLeaveTimeMins(int leaveTimeMins) {
		this.leaveTimeMins = leaveTimeMins;
		notifySubscribers();
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
		notifySubscribers();
	}

	@Override
	public String toString() {
		return "AttendanceRecord{" + "id='" + id + '\'' + ", reservationId='" + reservationId + '\''
				+ ", outcome=" + outcome + ", join=" + joinTimeMins + ", leave=" + leaveTimeMins
				+ ", candidate=" + (candidate != null ? candidate.getName() : "null") + '}';
	}
}
