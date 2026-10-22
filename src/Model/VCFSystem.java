package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VCFSystem implements TimeObserver {

	private SystemState state;
	private final List<Organization> organizations;
	private final List<Candidate> candidates;
	private final List<Recruiter> recruiters;
	private int bookingOpenTime;
	private int bookingCloseTime;
	private int fairStartTime;
	private int fairEndTime;
	private final List<AuditEntry> auditEntries;
	private final List<AttendanceRecord> attendanceRecords;
	private SystemTime systemTime;
	private final List<SubscriberInterface> subscribers;
	private int reservationCounter;

	public VCFSystem() {
		this.state = SystemState.DORMANT;
		this.organizations = new ArrayList<>();
		this.candidates = new ArrayList<>();
		this.recruiters = new ArrayList<>();
		this.auditEntries = new ArrayList<>();
		this.attendanceRecords = new ArrayList<>();
		this.subscribers = new ArrayList<>();
		setSystemTime(new SystemTime());
	}

	public void addSubscriber(SubscriberInterface sub) {
		if (sub != null && !subscribers.contains(sub)) {
			subscribers.add(sub);
		}
	}

	public void removeSubscriber(SubscriberInterface sub) {
		subscribers.remove(sub);
	}

	private void notifySubscribers() {
		for (SubscriberInterface sub : new ArrayList<>(subscribers)) {
			sub.update();
		}
	}

	public void advanceState() {
		SystemState[] order = SystemState.values();
		int i = state.ordinal();
		if (i + 1 < order.length) {
			changeState(order[i + 1]);
		}
	}

	public void resetFairData() {
		organizations.clear();
		candidates.clear();
		recruiters.clear();
		auditEntries.clear();
		attendanceRecords.clear();
		state = SystemState.DORMANT;
		notifySubscribers();
	}

	public void registerRecruiter(Recruiter r) {
		if (r != null && !recruiters.contains(r)) {
			recruiters.add(r);
			notifySubscribers();
		}
	}

	public Organization findOrganizationById(String id) {
		for (Organization o : organizations) {
			if (Objects.equals(o.getId(), id)) {
				return o;
			}
		}
		return null;
	}

	public Booth findBoothById(String id) {
		for (Booth b : getAllBooths()) {
			if (Objects.equals(b.getId(), id)) {
				return b;
			}
		}
		return null;
	}

	public Recruiter findRecruiterById(String id) {
		for (Recruiter r : recruiters) {
			if (Objects.equals(r.getUserId(), id)) {
				return r;
			}
		}
		for (Booth b : getAllBooths()) {
			for (Recruiter r : b.getRecruiters()) {
				if (Objects.equals(r.getUserId(), id)) {
					return r;
				}
			}
		}
		return null;
	}

	public Reservation findReservationById(String id) {
		for (Candidate c : candidates) {
			for (Reservation r : c.getReservations()) {
				if (Objects.equals(r.getId(), id)) {
					return r;
				}
			}
		}
		return null;
	}

	public AppointmentSlot findAppointmentSlotById(String id) {
		for (Booth b : getAllBooths()) {
			for (Offer o : b.getOffers()) {
				for (AppointmentSlot s : o.getSlots()) {
					if (Objects.equals(s.getId(), id)) {
						return s;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Locates a live or scheduled meeting session for the given reservation (e.g. for recording attendance).
	 */
	public MeetingSession findMeetingSessionForReservation(String reservationId) {
		if (reservationId == null) {
			return null;
		}
		for (Booth b : getAllBooths()) {
			if (b.getVirtualRoom() == null) {
				continue;
			}
			VirtualRoom vr = b.getVirtualRoom();
			MeetingSession cur = vr.getCurrentSession();
			if (cur != null && cur.getReservation() != null && reservationId.equals(cur.getReservation().getId())) {
				return cur;
			}
			for (MeetingSession s : vr.getScheduledSessions()) {
				if (s.getReservation() != null && reservationId.equals(s.getReservation().getId())) {
					return s;
				}
			}
			for (MeetingSession s : vr.getPendingAttendanceSessions()) {
				if (s.getReservation() != null && reservationId.equals(s.getReservation().getId())) {
					return s;
				}
			}
		}
		return null;
	}

	public void addAttendanceRecord(AttendanceRecord r) {
		if (r == null) {
			return;
		}
		String resId = r.getReservationId();
		if (resId != null) {
			attendanceRecords.removeIf(x -> resId.equals(x.getReservationId()));
		}
		attendanceRecords.add(r);
		notifySubscribers();
	}

	public AttendanceRecord findAttendanceRecordByReservationId(String reservationId) {
		if (reservationId == null) {
			return null;
		}
		for (int i = attendanceRecords.size() - 1; i >= 0; i--) {
			AttendanceRecord r = attendanceRecords.get(i);
			if (reservationId.equals(r.getReservationId())) {
				return r;
			}
		}
		return null;
	}

	public Candidate registerCandidate(String id, String name, String email) {
		Candidate c = new Candidate(id,name,email);
                c.setUserId(id);
		c.setName(name);
		c.setEmail(email);
		candidates.add(c);
		notifySubscribers();
		return c;
	}

	public Candidate getCandidateByEmail(String email) {
		for (Candidate c : candidates) {
			if (c.getEmail() != null && c.getEmail().equalsIgnoreCase(email)) {
				return c;
			}
		}
		return null;
	}

	public List<Organization> getOrganizations() {
		return Collections.unmodifiableList(organizations);
	}

	public List<Booth> getAllBooths() {
		List<Booth> booths = new ArrayList<>();
		for (Organization o : organizations) {
			booths.addAll(o.getBooths());
		}
		return booths;
	}

	public void notifyObservers(int time) {
		notifySubscribers();
	}

	public void addAuditEntry(AuditEntry e) {
		if (e != null) {
			auditEntries.add(e);
			notifySubscribers();
		}
	}

	public List<AuditEntry> getAuditEntries() {
		return Collections.unmodifiableList(auditEntries);
	}

	public SystemState getState() {
		return this.state;
	}

	public void setState(SystemState state) {
		this.state = Objects.requireNonNull(state);
		notifySubscribers();
	}

	public void addOrganization(Organization o) {
		if (o != null) {
			organizations.add(o);
			notifySubscribers();
		}
	}

	public void updateOrganization(Organization o) {
		if (o == null) {
			return;
		}
		int idx = -1;
		for (int i = 0; i < organizations.size(); i++) {
			if (Objects.equals(organizations.get(i).getId(), o.getId())) {
				idx = i;
				break;
			}
		}
		if (idx >= 0) {
			organizations.set(idx, o);
		} else {
			organizations.add(o);
		}
		notifySubscribers();
	}

	public List<Candidate> getCandidates() {
		return Collections.unmodifiableList(candidates);
	}

	public SystemTime getSystemTime() {
		return systemTime;
	}

	public void setSystemTime(SystemTime systemTime) {
		if (this.systemTime != null) {
			this.systemTime.removeObserver(this);
		}
		this.systemTime = systemTime;
		if (systemTime != null) {
			systemTime.registerObserver(this);
		}
		notifySubscribers();
	}

	public int getBookingOpenTime() {
		return bookingOpenTime;
	}

	public void setBookingOpenTime(int bookingOpenTime) {
		this.bookingOpenTime = bookingOpenTime;
		notifySubscribers();
	}

	public int getBookingCloseTime() {
		return bookingCloseTime;
	}

	public void setBookingCloseTime(int bookingCloseTime) {
		this.bookingCloseTime = bookingCloseTime;
		notifySubscribers();
	}

	public int getFairStartTime() {
		return fairStartTime;
	}

	public void setFairStartTime(int fairStartTime) {
		this.fairStartTime = fairStartTime;
		notifySubscribers();
	}

	public int getFairEndTime() {
		return fairEndTime;
	}

	public void setFairEndTime(int fairEndTime) {
		this.fairEndTime = fairEndTime;
		notifySubscribers();
	}

	// ------------------------------------------------------------------
	// Clock-driven behaviour
	// ------------------------------------------------------------------

	/** True once the administrator has configured the four fair timings. */
	public boolean hasTimings() {
		return fairEndTime > 0;
	}

	@Override
	public void updateTime(int day, int mins) {
		updateStateForTime(mins);
		if (state == SystemState.FAIR_LIVE) {
			for (Booth b : getAllBooths()) {
				b.getVirtualRoom().onTimeUpdate(mins, this);
			}
		}
		notifySubscribers();
	}

	/**
	 * Moves the fair to the phase that matches the clock. Only applies after the administrator has
	 * left DORMANT and configured the timings, so setup is never skipped automatically.
	 */
	public void updateStateForTime(int mins) {
		if (state == SystemState.DORMANT || !hasTimings()) {
			return;
		}
		SystemState target;
		if (mins >= fairEndTime) {
			target = SystemState.FAIR_CLOSED;
		} else if (mins >= fairStartTime) {
			target = SystemState.FAIR_LIVE;
		} else if (mins >= bookingCloseTime) {
			target = SystemState.BOOKINGS_CLOSED;
		} else if (mins >= bookingOpenTime) {
			target = SystemState.BOOKINGS_OPEN;
		} else {
			target = SystemState.PREPARING;
		}
		if (target != state) {
			SystemState from = state;
			changeState(target);
			addAuditEntry(AuditEntry.now(this, AuditEntry.ACTOR_SYSTEM, "SYSTEM", AuditEntry.ACTION_PHASE_TRANSITION,
					"from=" + from + ", to=" + target));
		}
	}

	private void changeState(SystemState next) {
		state = next;
		for (Booth b : getAllBooths()) {
			if (next == SystemState.FAIR_LIVE) {
				b.openVirtualRoom();
				b.getVirtualRoom().buildSessionsFromReservations();
			} else {
				b.closeVirtualRoom();
			}
		}
		notifySubscribers();
	}

	/** Short sequential reservation ids (RES-001, RES-002, ...). */
	public String nextReservationId() {
		return String.format("RES-%03d", ++reservationCounter);
	}

	private int now() {
		return systemTime != null ? systemTime.getTimeMins() : 0;
	}

	// ------------------------------------------------------------------
	// Queries used by the screens
	// ------------------------------------------------------------------

	public List<Recruiter> getRecruiters() {
		return Collections.unmodifiableList(recruiters);
	}

	public Candidate findCandidateById(String id) {
		for (Candidate c : candidates) {
			if (Objects.equals(c.getUserId(), id)) {
				return c;
			}
		}
		return null;
	}

	public List<Offer> getPublishedOffers(Booth booth) {
		List<Offer> result = new ArrayList<>();
		for (Offer o : booth.getOffers()) {
			if (o.isIsPublished()) {
				result.add(o);
			}
		}
		return result;
	}

	public Booth findBoothForRecruiter(Recruiter r) {
		for (Booth b : getAllBooths()) {
			if (b.getRecruiters().contains(r)) {
				return b;
			}
		}
		return null;
	}

	public List<AttendanceRecord> getAttendanceRecords() {
		return Collections.unmodifiableList(attendanceRecords);
	}

	// ------------------------------------------------------------------
	// Candidate actions
	// ------------------------------------------------------------------

	/** Registers a candidate with a generated id; rejects duplicate emails. */
	public Candidate registerCandidate(String name, String email) {
		if (name == null || name.isBlank() || email == null || !email.contains("@")) {
			throw new IllegalArgumentException("Enter a name and a valid email address.");
		}
		if (getCandidateByEmail(email.trim()) != null) {
			throw new IllegalStateException("A candidate with this email is already registered.");
		}
		return registerCandidate("CAN-" + (candidates.size() + 1), name.trim(), email.trim());
	}

	/** Books a single appointment slot for a candidate (manual booking use case). */
	public Reservation bookSlot(Candidate c, AppointmentSlot slot) {
		if (state != SystemState.BOOKINGS_OPEN) {
			throw new IllegalStateException("Bookings are not open (current phase: " + state + ").");
		}
		if (c == null || slot == null) {
			throw new IllegalArgumentException("Select a slot to book.");
		}
		if (slot.isFull() || !slot.isAvailable()) {
			throw new IllegalStateException("That slot is already fully booked.");
		}
		if (c.hasClash(slot)) {
			throw new IllegalStateException("That slot clashes with one of your existing bookings.");
		}
		Offer offer = slot.getOffer();
		Reservation r = new Reservation(nextReservationId(), c, offer.getRecruiter(), slot, offer.getBooth(), now());
		r.confirm();
		c.addReservation(r);
		if (offer.getRecruiter() != null) {
			offer.getRecruiter().trackReservation(r);
		}
		r.notifyOtherParty();
		addAuditEntry(AuditEntry.now(this, AuditEntry.ACTOR_CANDIDATE, c.getName(), "BOOK_SLOT",
				"reservationId=" + r.getId() + ", slot=" + SystemTime.format(slot.getStartMins())));
		return r;
	}

	/** Lets a candidate cancel one of their own active reservations. */
	public void cancelByCandidate(Candidate c, Reservation r) {
		if (r == null || r.getCandidate() != c || !r.isActive()) {
			throw new IllegalStateException("Select one of your active reservations.");
		}
		if (state == SystemState.FAIR_LIVE || state == SystemState.FAIR_CLOSED) {
			throw new IllegalStateException("Reservations can't be cancelled once the fair is live.");
		}
		r.cancel(c.getUserId());
		r.notifyOtherParty();
		addAuditEntry(AuditEntry.now(this, AuditEntry.ACTOR_CANDIDATE, c.getName(), "CANCEL_RESERVATION",
				"reservationId=" + r.getId()));
		notifySubscribers();
	}

	/**
	 * Candidate arrives for a reservation during the live fair: they wait in the booth's lobby and are
	 * admitted automatically when their slot starts (immediately if it has already started).
	 */
	public void joinSession(Candidate c, Reservation r) {
		if (state != SystemState.FAIR_LIVE) {
			throw new IllegalStateException("Sessions can only be joined while the fair is live.");
		}
		if (r == null || r.getStatus() != ReservationStatus.CONFIRMED || r.getCandidate() != c) {
			throw new IllegalStateException("Select one of your confirmed reservations.");
		}
		if (now() >= r.getSlot().getEndMins()) {
			throw new IllegalStateException("That appointment has already finished.");
		}
		VirtualRoom room = r.getBooth().getVirtualRoom();
		room.buildSessionsFromReservations();
		room.sendToLobby(c);
		room.onTimeUpdate(now(), this);
		notifySubscribers();
	}

	/** Candidate leaves the lobby or their live session early. */
	public void leaveSession(Candidate c) {
		MeetingSession s = c.getCurrentMeetingSession();
		if (s != null) {
			s.getVirtualRoom().endCurrentSession(now(), "Candidate left early", this);
		} else if (c.getCurrentLobby() != null) {
			c.getCurrentLobby().removeCandidate(c);
		}
		notifySubscribers();
	}

	/** Recruiter ends the session currently running in their booth's room. */
	public MeetingSession endSessionAt(Booth booth, String reason) {
		MeetingSession s = booth.getVirtualRoom().endCurrentSession(now(), reason, this);
		notifySubscribers();
		return s;
	}

	/** Recruiter publishes a new offer at their booth. */
	public Offer publishOffer(Recruiter r, String title, String description, int durationMins, int capacity,
			List<String> tags) {
		if (state != SystemState.PREPARING && state != SystemState.BOOKINGS_OPEN) {
			throw new IllegalStateException("Offers can be published while preparing or while bookings are open.");
		}
		Booth booth = findBoothForRecruiter(r);
		if (booth == null) {
			throw new IllegalStateException("This recruiter is not assigned to a booth yet.");
		}
		if (title == null || title.isBlank() || durationMins <= 0) {
			throw new IllegalArgumentException("An offer needs a title and a positive duration.");
		}
		Offer o = new Offer("OFF-" + (getAllOffers().size() + 1), title.trim(), description, durationMins,
				Math.max(1, capacity), r, booth, tags);
		booth.addOffer(o);
		r.publishOffer(o);
		addAuditEntry(AuditEntry.now(this, AuditEntry.ACTOR_RECRUITER, r.getName(), "PUBLISH_OFFER",
				"offerId=" + o.getId() + ", title=" + o.getTitle()));
		return o;
	}

	/** Recruiter adds availability for one of their offers; slots are generated from it. */
	public List<AppointmentSlot> addAvailability(Recruiter r, Offer o, int startMins, int endMins, int bufferMins) {
		if (o == null || o.getRecruiter() != r) {
			throw new IllegalArgumentException("Select one of your own offers.");
		}
		AvailabilityBlock block = new AvailabilityBlock(o, startMins, endMins, bufferMins);
		r.addAvailBlock(block);
		List<AppointmentSlot> slots = new ArrayList<>(o.getSlots());
		addAuditEntry(AuditEntry.now(this, AuditEntry.ACTOR_RECRUITER, r.getName(), "ADD_AVAILABILITY",
				"offerId=" + o.getId() + ", " + SystemTime.format(startMins) + "-" + SystemTime.formatClock(endMins)));
		return slots;
	}

	/** Recruiter cancels a reservation at their booth. */
	public void cancelByRecruiter(Recruiter r, Reservation res) {
		if (res == null || !r.getReservations().contains(res) || !res.isActive()) {
			throw new IllegalStateException("Select an active reservation from your schedule.");
		}
		r.cancelReservation(res);
		addAuditEntry(AuditEntry.now(this, AuditEntry.ACTOR_RECRUITER, r.getName(), "CANCEL_RESERVATION",
				"reservationId=" + res.getId()));
		notifySubscribers();
	}

	public List<Offer> getAllOffers() {
		List<Offer> all = new ArrayList<>();
		for (Booth b : getAllBooths()) {
			all.addAll(b.getOffers());
		}
		return all;
	}
}
