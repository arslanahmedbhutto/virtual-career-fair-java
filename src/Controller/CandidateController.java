package Controller;

import Model.*;
import View.CandidateScreen;

import java.util.ArrayList;
import java.util.List;

/** Handles candidate actions: registration, booking, auto-booking, cancelling and joining sessions. */
public class CandidateController implements SubscriberInterface {

	private final VCFSystem system;
	private Candidate candidate;
	private AutoRequest autoRequest;
	private CandidateScreen view;

	public CandidateController(VCFSystem system) {
		this.system = system;
		this.system.addSubscriber(this);
	}

	public void setView(CandidateScreen view) {
		this.view = view;
		refreshView();
	}

	@Override
	public void update() {
		refreshView();
	}

	public VCFSystem getSystem() {
		return system;
	}

	public Candidate getCandidate() {
		return candidate;
	}

	/** Switches the screen to an existing candidate (the demo has several registered users). */
	public void handleSelectCandidate(Candidate c) {
		this.candidate = c;
		this.autoRequest = null;
		refreshView();
	}

	public void handleRegister(String name, String email) {
		try {
			candidate = system.registerCandidate(name, email);
			autoRequest = null;
			showInfo("Welcome, " + candidate.getName() + "! Browse the booths to book an interview.");
			refreshView();
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	public List<AppointmentSlot> getOpenSlots(Booth booth) {
		List<AppointmentSlot> slots = new ArrayList<>();
		if (booth == null) {
			return slots;
		}
		for (Offer o : system.getPublishedOffers(booth)) {
			slots.addAll(o.getSlots());
		}
		slots.sort((a, b) -> Integer.compare(a.getStartMins(), b.getStartMins()));
		return slots;
	}

	public void handleManualBook(AppointmentSlot slot) {
		if (!requireCandidate()) {
			return;
		}
		try {
			Reservation r = system.bookSlot(candidate, slot);
			showInfo("Booked " + slot.getOffer().getTitle() + " at " + SystemTime.format(slot.getStartMins())
					+ " (" + r.getId() + ").");
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	/** Searches published offers that match the candidate's tags/organisation and suggests slots. */
	public List<Reservation> handleAutoRequest(String tags, String preferredOrg, int max) {
		if (!requireCandidate()) {
			return List.of();
		}
		autoRequest = new AutoRequest();
		autoRequest.setId("AUTO-" + System.nanoTime());
		autoRequest.setCandidate(candidate);
		autoRequest.setMaxAppointments(max);
		if (tags != null) {
			for (String t : tags.split(",")) {
				autoRequest.addDesiredTag(t);
			}
		}
		if (preferredOrg != null && !preferredOrg.isBlank() && !preferredOrg.startsWith("Any")) {
			autoRequest.addPreferredOrg(preferredOrg);
		}
		List<Reservation> suggestions = autoRequest.generate(system);
		if (system.getState() != SystemState.BOOKINGS_OPEN) {
			showError("Auto-booking only works while bookings are open (current phase: " + system.getState() + ").");
		} else if (suggestions.isEmpty()) {
			showError("No available slots match those preferences.");
		} else {
			showInfo(suggestions.size() + " slot(s) suggested. Review them and click Confirm to book.");
		}
		refreshView();
		return suggestions;
	}

	public List<Reservation> getSuggestions() {
		return autoRequest == null ? List.of() : autoRequest.getSuggestions();
	}

	public void handleAcceptSuggestions() {
		if (autoRequest == null || autoRequest.getSuggestions().isEmpty()) {
			showError("Generate suggestions first.");
			return;
		}
		int before = candidate.getConfirmedReservation().size();
		if (autoRequest.confirmAutoBook(system)) {
			int booked = candidate.getConfirmedReservation().size() - before;
			showInfo("Auto-book confirmed " + booked + " reservation(s).");
		} else {
			showError("None of the suggested slots could be booked.");
		}
		refreshView();
	}

	public void handleRejectSuggestions() {
		if (autoRequest != null) {
			autoRequest.reject();
			showInfo("Suggestions discarded.");
		}
		refreshView();
	}

	public void handleCancelReservation(Reservation r) {
		if (!requireCandidate()) {
			return;
		}
		try {
			system.cancelByCandidate(candidate, r);
			showInfo("Reservation " + r.getId() + " cancelled.");
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	public void handleJoinSession(Reservation r) {
		if (!requireCandidate()) {
			return;
		}
		try {
			system.joinSession(candidate, r);
			if (candidate.getCurrentMeetingSession() != null) {
				showInfo("You're in your live session at " + r.getBooth().getName() + ".");
			} else {
				showInfo("You're in the lobby for " + r.getBooth().getName() + ". You'll be admitted at "
						+ SystemTime.formatClock(r.getSlot().getStartMins()) + ".");
			}
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	public void handleLeaveSession() {
		if (!requireCandidate()) {
			return;
		}
		system.leaveSession(candidate);
		showInfo("You left the session.");
	}

	public void handleMarkNotificationsRead() {
		if (candidate != null) {
			for (Notification n : candidate.getNotifications()) {
				n.markRead();
			}
			refreshView();
		}
	}

	public void refreshView() {
		if (view != null) {
			view.refresh();
		}
	}

	private boolean requireCandidate() {
		if (candidate == null) {
			showError("Register or choose a candidate first.");
			return false;
		}
		return true;
	}

	private void showInfo(String msg) {
		if (view != null) {
			view.showMessage(msg, false);
		}
	}

	private void showError(String msg) {
		if (view != null) {
			view.showMessage(msg, true);
		}
	}
}
