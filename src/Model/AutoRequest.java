package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Candidate auto-book request: tag/org preferences, search for offers, then confirm {@link ReservationStatus#CONFIRMED}
 * bookings with audit and notifications for the auto-book use case.
 */
public class AutoRequest {

	/** Upper bound for max appointments per request (policy extension point). */
	public static final int SYSTEM_POLICY_MAX_APPOINTMENTS = 10;

	private String id;
	private Candidate candidate;
	private final List<String> desiredTags;
	private final List<String> preferredOrgs;
	private int maxAppointments;
	private RequestStatus status;
	private final List<Reservation> suggestions;
	private final List<SubscriberInterface> subscribers;

	public AutoRequest() {
		this.desiredTags = new ArrayList<>();
		this.preferredOrgs = new ArrayList<>();
		this.suggestions = new ArrayList<>();
		this.subscribers = new ArrayList<>();
		this.status = RequestStatus.PENDING;
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
	 * Searches published offers while system is {@link SystemState#BOOKINGS_OPEN}; fills {@link #suggestions} with
	 * {@link ReservationStatus#PENDING} prototypes (candidate-visible). Respects tag/org filters, clashes, capacity,
	 * and {@link #maxAppointments} capped by {@link #SYSTEM_POLICY_MAX_APPOINTMENTS}.
	 *
	 * @return unmodifiable view of suggestions (may be empty)
         * @param sys
	 */
	public List<Reservation> generate(VCFSystem sys) {
		suggestions.clear();
		notifySubscribers();
		if (!canSearch(sys)) {
			return Collections.unmodifiableList(suggestions);
		}
		int cap = effectiveMaxAppointments();
		List<AppointmentSlot> seenSlots = new ArrayList<>();
		for (Organization org : sys.getOrganizations()) {
			if (!preferredOrgs.isEmpty()) {
				boolean orgOk = org.getName() != null && preferredOrgs.stream()
						.anyMatch(p -> p != null && p.equalsIgnoreCase(org.getName()));
				if (!orgOk) {
					continue;
				}
			}
			for (Booth b : org.getBooths()) {
				for (Offer o : b.getOffers()) {
					if (!o.isIsPublished() || !matchesOffer(o)) {
						continue;
					}
					for (AppointmentSlot slot : o.getAvailableSlots()) {
						if (suggestions.size() >= cap) {
							setStatusAfterGenerate();
							notifySubscribers();
							return Collections.unmodifiableList(suggestions);
						}
						if (slotTakenOrSeen(seenSlots, slot)) {
							continue;
						}
						if (candidate.hasClash(slot)) {
							continue;
						}
						seenSlots.add(slot);
						Reservation r = newReservationPrototype(slot, b, sys);
						suggestions.add(r);
					}
				}
			}
		}
		setStatusAfterGenerate();
		notifySubscribers();
		return Collections.unmodifiableList(suggestions);
	}

	private void setStatusAfterGenerate() {
		if (suggestions.isEmpty()) {
			//status = RequestStatus.NO_MATCH;
                        status = RequestStatus.REJECTED;
		} else {
			//status = RequestStatus.AWAITING_CONFIRMATION;
                        status = RequestStatus.PENDING;
		}
	}

	private boolean slotTakenOrSeen(List<AppointmentSlot> seen, AppointmentSlot slot) {
		if (slot == null) {
			return true;
		}
		for (AppointmentSlot s : seen) {
			if (s == slot || (s.getId() != null && s.getId().equals(slot.getId()))) {
				return true;
			}
		}
		return false;
	}

	private Reservation newReservationPrototype(AppointmentSlot slot, Booth booth, VCFSystem sys) {
		Reservation r = new Reservation();
		r.setId(sys.nextReservationId());
		r.setCandidate(candidate);
		r.setRecruiter(slot.getOffer() != null ? slot.getOffer().getRecruiter() : null);
		r.setSlot(slot);
		r.setBooth(booth);
		r.setStatus(ReservationStatus.PENDING);
		if (sys.getSystemTime() != null) {
			r.setCreatedAtMins(sys.getSystemTime().getTimeMins());
		}
		return r;
	}

	private int effectiveMaxAppointments() {
		int m = maxAppointments;
		if (m <= 0) {
			m = 1;
		}
		return Math.min(m, SYSTEM_POLICY_MAX_APPOINTMENTS);
	}

	private boolean canSearch(VCFSystem sys) {
		return sys != null && candidate != null && sys.getState() == SystemState.BOOKINGS_OPEN;
	}

	/**
	 * Confirms suggestions as {@link ReservationStatus#CONFIRMED}, marks slots, notifies parties, writes audit.
	 * If nothing can be booked (all slots invalid), suggestions are restored and {@code false} is returned.
	 *
	 * @return {@code true} if at least one reservation was confirmed
	 */
	public boolean confirmAutoBook(VCFSystem sys) {
		if (!canConfirm(sys)) {
			return false;
		}
		List<Reservation> work = new ArrayList<>(suggestions);
		suggestions.clear();
		List<Reservation> booked = new ArrayList<>();
		List<Reservation> failed = new ArrayList<>();
		int ts = sys.getSystemTime() != null ? sys.getSystemTime().getTimeMins() : 0;
		for (Reservation r : work) {
			if (tryConfirmOne(r, sys, ts)) {
				booked.add(r);
			} else {
				failed.add(r);
			}
		}
		if (booked.isEmpty()) {
			suggestions.addAll(work);
			notifySubscribers();
			return false;
		}
		suggestions.addAll(failed);
		status = failed.isEmpty() ? RequestStatus.ACCEPTED/*RequestStatus.COMPLETED*/ : RequestStatus.PENDING;//RequestStatus.AWAITING_CONFIRMATION;
		writeAuditAndNotify(sys, booked, ts);
		notifySubscribers();
		return true;
	}

	private boolean canConfirm(VCFSystem sys) {
		return sys != null && candidate != null && sys.getState() == SystemState.BOOKINGS_OPEN
				&& !suggestions.isEmpty();
	}

	private boolean tryConfirmOne(Reservation r, VCFSystem sys, int ts) {
		AppointmentSlot slot = r.getSlot();
		if (slot == null || r.getBooth() == null) {
			return false;
		}
		if (!slot.isAvailable() || slot.isFull()) {
			return false;
		}
		if (candidate.hasClash(slot)) {
			return false;
		}
		if (r.getId() == null || r.getId().isBlank()) {
			r.setId("AR-" + System.nanoTime());
		}
		r.setCreatedAtMins(ts);
		r.confirm();
		candidate.addReservation(r);
		Recruiter rec = slot.getOffer() != null ? slot.getOffer().getRecruiter() : null;
		if (rec != null) {
			rec.trackReservation(r);
		}
		r.notifyOtherParty();
		return true;
	}

	private void writeAuditAndNotify(VCFSystem sys, List<Reservation> booked, int ts) {
		StringBuilder ids = new StringBuilder();
		for (int i = 0; i < booked.size(); i++) {
			if (i > 0) {
				ids.append(",");
			}
			ids.append(booked.get(i).getId());
		}
		String actorName = candidate.getName() != null ? candidate.getName() : "candidate";
		String detail = "count=" + booked.size() + " reservationIds=" + ids + " requestId=" + id;
		sys.addAuditEntry(AuditEntry.at(ts, AuditEntry.ACTOR_CANDIDATE, actorName, AuditEntry.ACTION_AUTO_BOOK, detail));
		Notification n = new Notification();
		n.setRecipient(candidate);
		n.setType("RESERVATION");
		n.setMessage("Auto-book confirmed: " + booked.size() + " reservation(s).");
		n.setTimestamp(ts);
		candidate.addNotification(n);
	}

	/** Candidate declined auto-book suggestions. */
	public void reject() {
		suggestions.clear();
		status = RequestStatus.REJECTED;
		notifySubscribers();
	}

	/**
	 * @deprecated Use {@link #confirmAutoBook(VCFSystem)} for use-case behaviour (CONFIRMED reservations + audit).
	 */
	@Deprecated
	public void accept() {
		status = RequestStatus.ACCEPTED;//RequestStatus.COMPLETED;
		notifySubscribers();
	}

	public boolean matchesOffer(Offer o) {
		if (o == null) {
			return false;
		}
		if (desiredTags.isEmpty()) {
			return true;
		}
		for (String t : desiredTags) {
			if (t != null && o.hasTag(t)) {
				return true;
			}
		}
		return false;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		notifySubscribers();
	}

	public Candidate getCandidate() {
		return candidate;
	}

	public void setCandidate(Candidate candidate) {
		this.candidate = candidate;
		notifySubscribers();
	}

	public List<String> getDesiredTags() {
		return Collections.unmodifiableList(desiredTags);
	}

	public void addDesiredTag(String tag) {
		if (tag != null && !tag.isBlank()) {
			desiredTags.add(tag.trim());
			notifySubscribers();
		}
	}

	public List<String> getPreferredOrgs() {
		return Collections.unmodifiableList(preferredOrgs);
	}

	public void addPreferredOrg(String name) {
		if (name != null && !name.isBlank()) {
			preferredOrgs.add(name.trim());
			notifySubscribers();
		}
	}

	public int getMaxAppointments() {
		return maxAppointments;
	}

	public void setMaxAppointments(int maxAppointments) {
		this.maxAppointments = maxAppointments;
		notifySubscribers();
	}

	public RequestStatus getStatus() {
		return status;
	}

	public List<Reservation> getSuggestions() {
		return Collections.unmodifiableList(suggestions);
	}
}
