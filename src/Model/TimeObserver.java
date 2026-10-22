package Model;

/** Notified whenever the simulated clock changes. */
public interface TimeObserver {
	/**
	 * @param day  current day number (starting at 1)
	 * @param mins current time as absolute minutes since Day 1, 00:00
	 */
	void updateTime(int day, int mins);
}
