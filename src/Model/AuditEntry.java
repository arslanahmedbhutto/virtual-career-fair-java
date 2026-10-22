package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuditEntry {

	public static final String ACTOR_ADMINISTRATOR = "ADMINISTRATOR";
	public static final String ACTOR_RECRUITER = "RECRUITER";
	public static final String ACTOR_CANDIDATE = "CANDIDATE";
	public static final String ACTOR_SYSTEM = "SYSTEM";

	public static final String ACTION_CONFIGURE_FAIR_TIMINGS = "CONFIGURE_FAIR_TIMINGS";
	public static final String ACTION_ADD_ORGANISATION = "ADD_ORGANISATION";
	public static final String ACTION_UPDATE_ORGANISATION = "UPDATE_ORGANISATION";
	public static final String ACTION_ADD_BOOTH = "ADD_BOOTH";
	public static final String ACTION_ASSIGN_RECRUITER = "ASSIGN_RECRUITER";
	public static final String ACTION_RESET_FAIR_DATA = "RESET_FAIR_DATA";
	public static final String ACTION_ADVANCE_SYSTEM_STATE = "ADVANCE_SYSTEM_STATE";
	public static final String ACTION_ATTENDANCE_OUTCOME = "ATTENDANCE_OUTCOME";
	public static final String ACTION_MEETING_SESSION = "MEETING_SESSION";
	public static final String ACTION_PHASE_TRANSITION = "PHASE_TRANSITION";
	public static final String ACTION_AUTO_BOOK = "AUTO_BOOK";

	private String id;
	private int timestamp;
	private String actorName;
	private String actorType;
	private String action;
	private String details;
	private final List<SubscriberInterface> subscribers;

	public AuditEntry() {
		this.subscribers = new ArrayList<>();
	}

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

	/**
	 * Unique id for list models and de-duplication; assigned automatically by {@link VCFSystem#addAuditEntry}
	 * if left blank.
	 */
	public static String newId() {
		return "AUD-" + System.nanoTime();
	}

	public static AuditEntry at(int timestampMins, String actorType, String actorName, String action, String details) {
		AuditEntry e = new AuditEntry();
		e.timestamp = timestampMins;
		e.actorType = actorType != null ? actorType : "UNKNOWN";
		e.actorName = actorName != null ? actorName : "";
		e.action = action != null ? action : "UNKNOWN";
		e.details = details != null ? details : "";
		e.notifySubscribers();
		return e;
	}

	/**
	 * Entry timestamped with the current {@link VCFSystem} simulated clock, or 0 if unavailable.
	 */
	public static AuditEntry now(VCFSystem sys, String actorType, String actorName, String action, String details) {
		int ts = 0;
		if (sys != null && sys.getSystemTime() != null) {
			ts = sys.getSystemTime().getTimeMins();
		}
		return at(ts, actorType, actorName, action, details);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		notifySubscribers();
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
		notifySubscribers();
	}

	public String getActorName() {
		return actorName;
	}

	public void setActorName(String actorName) {
		this.actorName = actorName;
		notifySubscribers();
	}

	public String getActorType() {
		return actorType;
	}

	public void setActorType(String actorType) {
		this.actorType = actorType;
		notifySubscribers();
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
		notifySubscribers();
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
		notifySubscribers();
	}

	/** Case-insensitive match for filtered audit views. */
	public boolean actionContains(String fragment) {
		if (fragment == null || fragment.isBlank()) {
			return true;
		}
		return action != null && action.toLowerCase().contains(fragment.toLowerCase());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AuditEntry auditEntry = (AuditEntry) o;
		return Objects.equals(id, auditEntry.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public String toString() {
		return "[" + timestamp + "] " + actorType + " " + actorName + " " + action + ": " + details;
	}
}
