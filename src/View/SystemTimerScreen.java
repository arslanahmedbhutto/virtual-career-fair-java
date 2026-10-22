package View;

import Controller.TimerController;
import Model.SystemState;
import Model.SystemTime;
import Model.VCFSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Simulation clock: move time forward to drive the fair through its phases and live sessions. */
public class SystemTimerScreen extends JFrame {

	private final TimerController controller;
	private final JLabel clock = new JLabel("", SwingConstants.CENTER);
	private final JLabel phase = new JLabel("", SwingConstants.CENTER);
	private final JLabel timeline = new JLabel();
	private final JLabel message = new JLabel(" ");

	public SystemTimerScreen(TimerController controller) {
		this.controller = controller;
		setTitle("Virtual Career Fair - Simulation clock");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(440, 400);
		setLayout(new BorderLayout());

		clock.setFont(clock.getFont().deriveFont(Font.BOLD, 30f));
		clock.setForeground(Color.WHITE);
		phase.setFont(phase.getFont().deriveFont(Font.BOLD, 14f));
		phase.setForeground(Color.WHITE);
		JPanel display = new JPanel(new GridLayout(2, 1));
		display.setBackground(UiKit.PRIMARY);
		display.setBorder(new EmptyBorder(14, 10, 14, 10));
		display.add(clock);
		display.add(phase);
		add(display, BorderLayout.NORTH);

		JPanel buttons = new JPanel(new GridLayout(3, 2, 8, 8));
		buttons.add(button("+5 minutes", controller::handleAdvance5Min));
		buttons.add(button("+1 hour", controller::handleAdvanceHour));
		buttons.add(button("+1 day", controller::handleAdvanceDay));
		buttons.add(button("Jump to bookings open", controller::handleJumpToBookingsOpen));
		buttons.add(button("Jump to fair start", controller::handleJumpToFairStart));
		buttons.add(button("Jump to fair end", controller::handleJumpToFairEnd));

		JPanel centre = new JPanel(new BorderLayout(8, 10));
		centre.setBorder(new EmptyBorder(12, 12, 8, 12));
		centre.add(buttons, BorderLayout.NORTH);
		timeline.setVerticalAlignment(SwingConstants.TOP);
		centre.add(timeline, BorderLayout.CENTER);
		add(centre, BorderLayout.CENTER);

		message.setBorder(new EmptyBorder(6, 12, 8, 12));
		message.setForeground(UiKit.ERROR);
		add(message, BorderLayout.SOUTH);
	}

	private JButton button(String text, Runnable action) {
		JButton b = new JButton(text);
		b.addActionListener(e -> {
			message.setText(" ");
			action.run();
		});
		return b;
	}

	public void showMessage(String msg) {
		message.setText(msg);
	}

	public void refresh() {
		VCFSystem sys = controller.getSystem();
		SystemTime t = controller.getTimer();
		clock.setText(SystemTime.format(t.getTimeMins()));
		SystemState st = sys.getState();
		phase.setText("Phase: " + st.name().replace('_', ' '));
		phase.setOpaque(true);
		phase.setBackground(UiKit.phaseColor(st));
		if (sys.hasTimings()) {
			timeline.setText("<html><table cellpadding=2>"
					+ row("Bookings open", sys.getBookingOpenTime(), t.getTimeMins())
					+ row("Bookings close", sys.getBookingCloseTime(), t.getTimeMins())
					+ row("Fair starts", sys.getFairStartTime(), t.getTimeMins())
					+ row("Fair ends", sys.getFairEndTime(), t.getTimeMins())
					+ "</table></html>");
		} else {
			timeline.setText("<html><i>Fair timings have not been configured yet.</i></html>");
		}
	}

	private static String row(String label, int at, int now) {
		String mark = now >= at ? "&#10003;" : "";
		return "<tr><td>" + label + "</td><td><b>" + SystemTime.format(at) + "</b></td><td>" + mark + "</td></tr>";
	}
}
