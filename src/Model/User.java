package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class User {

	private String userId;
	private String name;
	private String email;
	private final List<Notification> notifications;
	private final List<SubscriberInterface> subscribers;

	public User() {
		this.notifications = new ArrayList<>();
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
		notifySubscribers();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
		notifySubscribers();
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
		notifySubscribers();
	}

	public void addNotification(Notification n) {
		if (n != null) {
			if (n.getRecipient() == null) {
				n.setRecipient(this);
			}
			notifications.add(n);
			notifySubscribers();
		}
	}

	public List<Notification> getNotifications() {
		return Collections.unmodifiableList(notifications);
	}

	/** Newest-first by {@link Notification#getTimestamp()} (view notifications use cases). */
	public List<Notification> getNotificationsNewestFirst() {
		List<Notification> copy = new ArrayList<>(notifications);
		copy.sort(Comparator.comparingInt(Notification::getTimestamp).reversed());
		return Collections.unmodifiableList(copy);
	}

	public int getUnreadCount() {
		int c = 0;
		for (Notification n : notifications) {
			if (!n.isRead()) {
				c++;
			}
		}
		return c;
	}
}
