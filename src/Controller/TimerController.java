package Controller;

import Model.*;
import View.SystemTimerScreen;

/**
 * Drives the simulated clock. Every change notifies the system, which moves the fair to the right
 * phase and runs the live sessions, and then refreshes all screens.
 */
public class TimerController implements SubscriberInterface {

	private final SystemTime timer;
	private final VCFSystem system;
	private SystemTimerScreen view;

	public TimerController(SystemTime timer, VCFSystem system) {
		this.timer = timer;
		this.system = system;
		this.system.addSubscriber(this);
	}

	public void setView(SystemTimerScreen view) {
		this.view = view;
		refreshAllViews();
	}

	@Override
	public void update() {
		refreshAllViews();
	}

	public SystemTime getTimer() {
		return timer;
	}

	public VCFSystem getSystem() {
		return system;
	}

	public void handleAdvance5Min() {
		timer.advance5Mins();
	}

	public void handleAdvanceHour() {
		timer.jumpToAbsolute(timer.getTimeMins() + 60);
	}

	public void handleAdvanceDay() {
		timer.advanceDay();
	}

	public void handleJumpTo(int day, int mins) {
		timer.jumpTo(day, mins);
	}

	public void handleJumpToBookingsOpen() {
		jumpToTiming(system.getBookingOpenTime());
	}

	public void handleJumpToFairStart() {
		jumpToTiming(system.getFairStartTime());
	}

	public void handleJumpToFairEnd() {
		jumpToTiming(system.getFairEndTime());
	}

	private void jumpToTiming(int mins) {
		if (!system.hasTimings()) {
			if (view != null) {
				view.showMessage("Configure the fair timings in the Admin screen first.");
			}
			return;
		}
		timer.jumpToAbsolute(mins);
	}

	public void refreshAllViews() {
		if (view != null) {
			view.refresh();
		}
	}
}
