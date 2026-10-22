package Model;

import java.util.ArrayList;
import java.util.List;

/**
 * A window of time a recruiter is available for an offer. It is split into back-to-back
 * appointment slots of the offer's duration, separated by an optional buffer.
 */
public class AvailabilityBlock {

	private String id;
	private Offer offer;
	private int startMins;
	private int endMins;
	private int bufferMins;

	public AvailabilityBlock() {
	}

	public AvailabilityBlock(Offer offer, int startMins, int endMins, int bufferMins) {
		if (endMins <= startMins) {
			throw new IllegalArgumentException("Availability must end after it starts.");
		}
		this.id = "AVB-" + System.nanoTime();
		this.offer = offer;
		this.startMins = startMins;
		this.endMins = endMins;
		this.bufferMins = Math.max(0, bufferMins);
	}

	/** Creates the appointment slots for this block and adds them to the offer. */
	public List<AppointmentSlot> generateSlots() {
		List<AppointmentSlot> created = new ArrayList<>();
		if (offer == null || offer.getDurationMins() <= 0) {
			return created;
		}
		int duration = offer.getDurationMins();
		for (int t = startMins; t + duration <= endMins; t += duration + bufferMins) {
			AppointmentSlot slot = new AppointmentSlot();
			slot.setId(offer.getId() + "-S" + (offer.getSlots().size() + 1));
			slot.setStartMins(t);
			slot.setEndMins(t + duration);
			slot.setMaxBookings(Math.max(1, offer.getCandidateCapacity()));
			slot.setOffer(offer);
			offer.addSlot(slot);
			created.add(slot);
		}
		return created;
	}

	public boolean checkClash(AppointmentSlot other) {
		return other != null && startMins < other.getEndMins() && endMins > other.getStartMins();
	}

	public String getId() {
		return id;
	}

	public Offer getOffer() {
		return offer;
	}

	public int getStartMins() {
		return this.startMins;
	}

	public int getEndMins() {
		return this.endMins;
	}

	public int getBufferMins() {
		return bufferMins;
	}
}
