package Model;

import java.util.ArrayList;
import java.util.List;

public class AppointmentSlot {

	private String id;
	private Offer offer;
	private int startMins;
	private int endMins;
	private boolean available;
	private int currentBookings;
	private int maxBookings;
	private final List<SubscriberInterface> subscribers;

	public AppointmentSlot() {
		this.maxBookings = 1;
		this.available = true;
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

	public boolean checkClash(AppointmentSlot other) {
		if (other == null) {
			return false;
		}
		return this.startMins < other.endMins && this.endMins > other.startMins;
	}

	public void markBooked() {
		currentBookings++;
		if (currentBookings >= maxBookings) {
			available = false;
		}
		notifySubscribers();
	}

	/** Frees one booking after a cancellation. */
	public void releaseBooking() {
		if (currentBookings > 0) {
			currentBookings--;
		}
		available = currentBookings < maxBookings;
		notifySubscribers();
	}

	public void markAvailable() {
		available = true;
		notifySubscribers();
	}

	public boolean isFull() {
		return currentBookings >= maxBookings;
	}

	public int getStartMins() {
		return this.startMins;
	}

	public void setStartMins(int startMins) {
		this.startMins = startMins;
		notifySubscribers();
	}

	public int getEndMins() {
		return this.endMins;
	}

	public void setEndMins(int endMins) {
		this.endMins = endMins;
		notifySubscribers();
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
		notifySubscribers();
	}

	public int getCurrentBookings() {
		return currentBookings;
	}

	public void setCurrentBookings(int currentBookings) {
		this.currentBookings = currentBookings;
		notifySubscribers();
	}

	public int getMaxBookings() {
		return maxBookings;
	}

	public void setMaxBookings(int maxBookings) {
		this.maxBookings = maxBookings;
		notifySubscribers();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		notifySubscribers();
	}

	public Offer getOffer() {
		return offer;
	}

	public void setOffer(Offer offer) {
		this.offer = offer;
		notifySubscribers();
	}
}
