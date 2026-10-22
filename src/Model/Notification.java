package Model;

public class Notification {

	private String id;
	private User recipient;
	private String message;
	private int timestamp;
	private boolean isRead;
	private String type;

	public Notification() {
		this.id = "NOTE-" + System.nanoTime();
	}

	public Notification(String type, String message, int timestamp) {
		this();
		this.type = type;
		this.message = message;
		this.timestamp = timestamp;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isIsRead() {
		return isRead;
	}

	public void setIsRead(boolean isRead) {
		this.isRead = isRead;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void markRead() {
		this.isRead = true;
	}

	public boolean isRead() {
		return isRead;
	}

	public User getRecipient() {
		return recipient;
	}

	public void setRecipient(User recipient) {
		this.recipient = recipient;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
