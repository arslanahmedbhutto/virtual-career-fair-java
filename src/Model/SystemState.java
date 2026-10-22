package Model;

/** Lifecycle of the virtual career fair, in the order the administrator moves through it. */
public enum SystemState {
	DORMANT,
	PREPARING,
	BOOKINGS_OPEN,
	BOOKINGS_CLOSED,
	FAIR_LIVE,
	FAIR_CLOSED
}
