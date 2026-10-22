package Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulated clock for the fair. Time is stored as minutes since Day 1, 00:00 so that
 * appointment slots, fair timings and the clock can be compared directly.
 */
public class SystemTime {

	public static final int MINS_PER_DAY = 24 * 60;

	private int timeMins;
	private final List<TimeObserver> observers = new ArrayList<>();

	public void advance5Mins() {
		timeMins += 5;
		notifyObservers();
	}

	public void advanceDay() {
		timeMins += MINS_PER_DAY;
		notifyObservers();
	}

	/**
	 * @param day  day number, starting at 1
	 * @param mins minutes after midnight on that day
	 */
	public void jumpTo(int day, int mins) {
		jumpToAbsolute((Math.max(day, 1) - 1) * MINS_PER_DAY + mins);
	}

	public void jumpToAbsolute(int absoluteMins) {
		timeMins = Math.max(0, absoluteMins);
		notifyObservers();
	}

	public int getTimeMins() {
		return timeMins;
	}

	public int getDay() {
		return timeMins / MINS_PER_DAY + 1;
	}

	public int getMinsOfDay() {
		return timeMins % MINS_PER_DAY;
	}

	/** Formats an absolute minute value as "Day 2, 09:30". */
	public static String format(int absoluteMins) {
		int day = absoluteMins / MINS_PER_DAY + 1;
		int m = absoluteMins % MINS_PER_DAY;
		return String.format("Day %d, %02d:%02d", day, m / 60, m % 60);
	}

	/** Formats only the time of day, e.g. "09:30". */
	public static String formatClock(int absoluteMins) {
		int m = absoluteMins % MINS_PER_DAY;
		return String.format("%02d:%02d", m / 60, m % 60);
	}

	public void registerObserver(TimeObserver o) {
		if (o != null && !observers.contains(o)) {
			observers.add(o);
		}
	}

	public void removeObserver(TimeObserver o) {
		observers.remove(o);
	}

	public void notifyObservers() {
		for (TimeObserver o : new ArrayList<>(observers)) {
			o.updateTime(getDay(), timeMins);
		}
	}

	@Override
	public String toString() {
		return format(timeMins);
	}
}
