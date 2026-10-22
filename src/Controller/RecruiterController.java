package Controller;

import Model.*;
import View.RecruiterScreen;

import java.util.ArrayList;
import java.util.List;

/** Handles recruiter actions: publishing offers, availability, schedule, cancellations and live sessions. */
public class RecruiterController implements SubscriberInterface {

	private final VCFSystem system;
	private Recruiter recruiter;
	private RecruiterScreen view;

	public RecruiterController(VCFSystem system) {
		this.system = system;
		this.system.addSubscriber(this);
	}

	public void setView(RecruiterScreen view) {
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

	public Recruiter getRecruiter() {
		return recruiter;
	}

	public Booth getBooth() {
		return recruiter == null ? null : system.findBoothForRecruiter(recruiter);
	}

	public void handleSelectRecruiter(Recruiter r) {
		this.recruiter = r;
		refreshView();
	}

	public void handlePublishOffer(String title, String description, int durationMins, int capacity, String tags) {
		if (!requireRecruiter()) {
			return;
		}
		List<String> tagList = new ArrayList<>();
		if (tags != null) {
			for (String t : tags.split(",")) {
				if (!t.isBlank()) {
					tagList.add(t.trim().toLowerCase());
				}
			}
		}
		try {
			Offer o = system.publishOffer(recruiter, title, description, durationMins, capacity, tagList);
			showInfo("Offer \"" + o.getTitle() + "\" published. Add availability to create interview slots.");
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	public void handleAddAvailBlock(Offer offer, int start, int end, int buffer) {
		if (!requireRecruiter()) {
			return;
		}
		try {
			int before = offer == null ? 0 : offer.getSlots().size();
			system.addAvailability(recruiter, offer, start, end, buffer);
			int created = offer.getSlots().size() - before;
			showInfo(created + " slot(s) created for \"" + offer.getTitle() + "\" between "
					+ SystemTime.format(start) + " and " + SystemTime.formatClock(end) + ".");
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	/** All reservations at this recruiter's slots, ordered by time. */
	public List<Reservation> getSchedule() {
		List<Reservation> list = new ArrayList<>();
		if (recruiter != null) {
			list.addAll(recruiter.getReservations());
			list.sort((a, b) -> Integer.compare(a.getSlot().getStartMins(), b.getSlot().getStartMins()));
		}
		return list;
	}

	public void handleCancelReservation(Reservation r) {
		if (!requireRecruiter()) {
			return;
		}
		try {
			system.cancelByRecruiter(recruiter, r);
			showInfo("Reservation with " + r.getCandidate().getName() + " cancelled. The candidate has been notified.");
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	public MeetingSession getCurrentSession() {
		Booth b = getBooth();
		return b == null ? null : b.getVirtualRoom().getCurrentSession();
	}

	public void handleEndSession(String reason) {
		Booth b = getBooth();
		if (b == null || b.getVirtualRoom().getCurrentSession() == null) {
			showError("There is no session in progress at your booth.");
			return;
		}
		MeetingSession s = system.endSessionAt(b, reason);
		showInfo("Session with " + s.getCandidate().getName() + " ended (" + s.getStatus() + ").");
	}

	public void handleMarkNotificationsRead() {
		if (recruiter != null) {
			for (Notification n : recruiter.getNotifications()) {
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

	private boolean requireRecruiter() {
		if (recruiter == null) {
			showError("Choose a recruiter first.");
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
