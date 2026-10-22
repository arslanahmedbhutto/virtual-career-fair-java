package View;

import Controller.RecruiterController;
import Model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Recruiter console: publish offers, add availability, manage the schedule and run live sessions. */
public class RecruiterScreen extends JFrame {

	private final RecruiterController controller;
	private final JLabel statusBadge = UiKit.statusBadge();
	private final JLabel messageBar = UiKit.messageBar();
	private final JTabbedPane tabs = new JTabbedPane();
	private final JComboBox<Recruiter> recruiterCombo = new JComboBox<>();
	private final JLabel boothLabel = new JLabel();
	private boolean updatingCombo;

	private final JTable offerTable = UiKit.table("Offer", "Duration", "Places/slot", "Tags", "Slots", "Booked");
	private final JComboBox<Offer> offerCombo = new JComboBox<>();
	private final UiKit.TimeInput availFrom = new UiKit.TimeInput(SystemTime.MINS_PER_DAY + 13 * 60);
	private final UiKit.TimeInput availTo = new UiKit.TimeInput(SystemTime.MINS_PER_DAY + 14 * 60);
	private final JSpinner bufferSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 60, 5));
	private final JTable slotTable = UiKit.table("Slot", "Bookings");

	private final JTable scheduleTable = UiKit.table("Time", "Candidate", "Offer", "Status");
	private List<Reservation> shownSchedule = new ArrayList<>();

	private final JLabel sessionInfo = new JLabel();
	private final DefaultListModel<String> lobbyModel = new DefaultListModel<>();
	private final JTable attendanceTable = UiKit.table("Slot", "Candidate", "Outcome", "Joined", "Left");

	private final JTable notifTable = UiKit.table("Time", "Type", "Message", "");

	public RecruiterScreen(RecruiterController controller) {
		this.controller = controller;
		setTitle("Virtual Career Fair - Recruiter");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(860, 580);
		setLayout(new BorderLayout());

		JPanel top = new JPanel(new BorderLayout());
		top.add(UiKit.header("Recruiter console", "Publish offers, manage your interview schedule and run sessions",
				statusBadge), BorderLayout.NORTH);
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		bar.add(new JLabel("Recruiter:"));
		bar.add(recruiterCombo);
		bar.add(boothLabel);
		top.add(bar, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);

		tabs.addTab("My offers", offersTab());
		tabs.addTab("Availability", availabilityTab());
		tabs.addTab("Schedule", scheduleTab());
		tabs.addTab("Live session", liveTab());
		tabs.addTab("Notifications", notificationsTab());
		add(tabs, BorderLayout.CENTER);
		add(messageBar, BorderLayout.SOUTH);

		recruiterCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
				return super.getListCellRendererComponent(l, v instanceof Recruiter ? ((Recruiter) v).getName() : v, i, s, f);
			}
		});
		recruiterCombo.addActionListener(e -> {
			if (!updatingCombo && recruiterCombo.getSelectedItem() != null) {
				controller.handleSelectRecruiter((Recruiter) recruiterCombo.getSelectedItem());
			}
		});
		offerCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
				return super.getListCellRendererComponent(l, v instanceof Offer ? ((Offer) v).getTitle() : v, i, s, f);
			}
		});
		offerCombo.addActionListener(e -> {
			if (!updatingCombo) {
				refreshSlotTable();
			}
		});
	}

	private JComponent offersTab() {
		JTextField title = new JTextField(18);
		JTextField desc = new JTextField(18);
		JSpinner duration = new JSpinner(new SpinnerNumberModel(30, 10, 120, 5));
		JSpinner capacity = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
		JTextField tags = new JTextField(18);
		JButton publish = new JButton("Publish offer");
		publish.addActionListener(e -> {
			controller.handlePublishOffer(title.getText(), desc.getText(), (Integer) duration.getValue(),
					(Integer) capacity.getValue(), tags.getText());
			title.setText("");
			desc.setText("");
			tags.setText("");
		});
		UiKit.setWidths(offerTable, 220, 70, 80, 160, 50, 60);
		JPanel form = new JPanel(new BorderLayout());
		form.add(UiKit.sectionTitle("Publish a new offer"), BorderLayout.NORTH);
		JPanel fields = new JPanel(new GridLayout(1, 2, 12, 0));
		fields.add(UiKit.form("Title", title, "Description", desc, "Tags (comma separated)", tags));
		fields.add(UiKit.form("Interview length (min)", duration, "Candidates per slot", capacity));
		form.add(fields, BorderLayout.CENTER);
		form.add(UiKit.buttons(publish), BorderLayout.SOUTH);
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(UiKit.scroll(offerTable), BorderLayout.CENTER);
		p.add(form, BorderLayout.SOUTH);
		return UiKit.padded(p);
	}

	private JComponent availabilityTab() {
		JButton add = new JButton("Add availability");
		add.addActionListener(e -> {
			try {
				controller.handleAddAvailBlock((Offer) offerCombo.getSelectedItem(), availFrom.getMins(), availTo.getMins(),
						(Integer) bufferSpinner.getValue());
			} catch (RuntimeException ex) {
				showMessage(ex.getMessage(), true);
			}
		});
		JPanel form = new JPanel(new BorderLayout());
		form.add(UiKit.hint("Availability is split into interview slots of the offer's length, with a buffer between them."),
				BorderLayout.NORTH);
		form.add(UiKit.form("Offer", offerCombo, "From", availFrom, "To", availTo, "Buffer (min)", bufferSpinner),
				BorderLayout.CENTER);
		form.add(UiKit.buttons(add), BorderLayout.SOUTH);
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(form, BorderLayout.NORTH);
		p.add(UiKit.scroll(slotTable), BorderLayout.CENTER);
		return UiKit.padded(p);
	}

	private JComponent scheduleTab() {
		UiKit.setWidths(scheduleTable, 120, 160, 220, 200);
		JButton cancel = new JButton("Cancel reservation");
		cancel.addActionListener(e -> {
			int row = scheduleTable.getSelectedRow();
			if (row < 0) {
				showMessage("Select a reservation first.", true);
			} else {
				controller.handleCancelReservation(shownSchedule.get(row));
			}
		});
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(UiKit.scroll(scheduleTable), BorderLayout.CENTER);
		p.add(UiKit.buttons(cancel), BorderLayout.SOUTH);
		return UiKit.padded(p);
	}

	private JComponent liveTab() {
		sessionInfo.setOpaque(true);
		sessionInfo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		sessionInfo.setVerticalAlignment(SwingConstants.TOP);
		JTextField reason = new JTextField(22);
		JButton end = new JButton("End session");
		end.addActionListener(e -> {
			controller.handleEndSession(reason.getText());
			reason.setText("");
		});
		JList<String> lobbyList = new JList<>(lobbyModel);
		JPanel lobby = new JPanel(new BorderLayout());
		lobby.add(UiKit.sectionTitle("Lobby"), BorderLayout.NORTH);
		lobby.add(UiKit.scroll(lobbyList), BorderLayout.CENTER);
		lobby.setPreferredSize(new Dimension(230, 10));

		JPanel current = new JPanel(new BorderLayout(6, 6));
		current.add(sessionInfo, BorderLayout.CENTER);
		current.add(UiKit.buttons(new JLabel("Reason (optional, ends early):"), reason, end), BorderLayout.SOUTH);

		JPanel topRow = new JPanel(new BorderLayout(10, 0));
		topRow.add(current, BorderLayout.CENTER);
		topRow.add(lobby, BorderLayout.EAST);
		topRow.setPreferredSize(new Dimension(10, 170));

		UiKit.setWidths(attendanceTable, 120, 160, 120, 80, 80);
		JPanel history = new JPanel(new BorderLayout());
		history.add(UiKit.sectionTitle("Attendance at this booth"), BorderLayout.NORTH);
		history.add(UiKit.scroll(attendanceTable), BorderLayout.CENTER);

		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(topRow, BorderLayout.NORTH);
		p.add(history, BorderLayout.CENTER);
		return UiKit.padded(p);
	}

	private JComponent notificationsTab() {
		UiKit.setWidths(notifTable, 110, 110, 520, 50);
		JButton read = new JButton("Mark all as read");
		read.addActionListener(e -> controller.handleMarkNotificationsRead());
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(UiKit.scroll(notifTable), BorderLayout.CENTER);
		p.add(UiKit.buttons(read), BorderLayout.SOUTH);
		return UiKit.padded(p);
	}

	public void showMessage(String msg, boolean error) {
		UiKit.setMessage(messageBar, msg, error);
	}

	public void selectTab(int index) {
		tabs.setSelectedIndex(index);
	}

	public void refresh() {
		VCFSystem sys = controller.getSystem();
		Recruiter me = controller.getRecruiter();
		Booth booth = controller.getBooth();
		UiKit.updateStatus(statusBadge, sys);

		updatingCombo = true;
		recruiterCombo.removeAllItems();
		for (Recruiter r : sys.getRecruiters()) {
			recruiterCombo.addItem(r);
		}
		recruiterCombo.setSelectedItem(me);
		boothLabel.setText(booth == null ? "  (no booth assigned)"
				: "   Booth: " + booth.getName() + "  |  " + booth.getOrganisation().getName());

		Object selOffer = offerCombo.getSelectedItem();
		offerCombo.removeAllItems();
		List<Object[]> offers = new ArrayList<>();
		if (me != null) {
			for (Offer o : me.getOffers()) {
				offerCombo.addItem(o);
				int booked = 0;
				for (AppointmentSlot s : o.getSlots()) {
					booked += s.getCurrentBookings();
				}
				offers.add(new Object[] {o.getTitle(), o.getDurationMins() + " min", o.getCandidateCapacity(),
						String.join(", ", o.getTags()), o.getSlots().size(), booked});
			}
		}
		if (selOffer != null) {
			offerCombo.setSelectedItem(selOffer);
		}
		updatingCombo = false;
		UiKit.setRows(offerTable, offers);
		refreshSlotTable();

		shownSchedule = controller.getSchedule();
		List<Object[]> sched = new ArrayList<>();
		for (Reservation r : shownSchedule) {
			AttendanceRecord rec = sys.findAttendanceRecordByReservationId(r.getId());
			String status = rec != null ? "Done: " + rec.getOutcome() : r.getStatus().name().replace('_', ' ');
			sched.add(new Object[] {SystemTime.format(r.getSlot().getStartMins()), r.getCandidate().getName(),
					r.getSlot().getOffer().getTitle(), status});
		}
		UiKit.setRows(scheduleTable, sched);

		refreshLive(sys, booth);

		List<Object[]> notes = new ArrayList<>();
		if (me != null) {
			for (Notification n : me.getNotificationsNewestFirst()) {
				notes.add(new Object[] {SystemTime.format(n.getTimestamp()), n.getType(), n.getMessage(),
						n.isRead() ? "" : "NEW"});
			}
		}
		UiKit.setRows(notifTable, notes);
		int unread = me == null ? 0 : me.getUnreadCount();
		tabs.setTitleAt(4, unread > 0 ? "Notifications (" + unread + ")" : "Notifications");
	}

	private void refreshSlotTable() {
		Offer o = (Offer) offerCombo.getSelectedItem();
		List<Object[]> rows = new ArrayList<>();
		if (o != null) {
			for (AppointmentSlot s : o.getSlots()) {
				rows.add(new Object[] {SystemTime.format(s.getStartMins()) + "-" + SystemTime.formatClock(s.getEndMins()),
						s.getCurrentBookings() + " / " + s.getMaxBookings()});
			}
		}
		UiKit.setRows(slotTable, rows);
	}

	private void refreshLive(VCFSystem sys, Booth booth) {
		lobbyModel.clear();
		List<Object[]> attendance = new ArrayList<>();
		if (booth == null) {
			sessionInfo.setText("Select a recruiter with a booth.");
			sessionInfo.setBackground(new Color(0xF2F4F7));
			UiKit.setRows(attendanceTable, attendance);
			return;
		}
		VirtualRoom room = booth.getVirtualRoom();
		for (Candidate c : room.getLobby().getWaiting()) {
			lobbyModel.addElement(c.getName());
		}
		MeetingSession s = room.getCurrentSession();
		if (s != null) {
			sessionInfo.setText("<html><span style='font-size:13px'><b>LIVE</b> with <b>" + s.getCandidate().getName()
					+ "</b></span><br><br>Offer: " + s.getSlot().getOffer().getTitle() + "<br>Slot: "
					+ SystemTime.format(s.getSlot().getStartMins()) + " to " + SystemTime.formatClock(s.getSlot().getEndMins())
					+ "<br>Joined at: " + SystemTime.formatClock(s.getJoinTimeMins()) + "</html>");
			sessionInfo.setBackground(new Color(0xFEE4E2));
		} else if (sys.getState() == SystemState.FAIR_LIVE) {
			sessionInfo.setText("<html><b>Room open</b>: no session in progress.<br><br>Candidates who join are admitted"
					+ " automatically when their slot starts.</html>");
			sessionInfo.setBackground(new Color(0xECFDF3));
		} else {
			sessionInfo.setText("<html><b>Room closed</b>.<br><br>The virtual room opens when the fair goes live.</html>");
			sessionInfo.setBackground(new Color(0xF2F4F7));
		}
		for (MeetingSession done : room.getCompletedSessions()) {
			boolean noShow = done.getStatus() == SessionStatus.NO_SHOW;
			attendance.add(new Object[] {SystemTime.format(done.getSlot().getStartMins()), done.getCandidate().getName(),
					done.getStatus(), noShow ? "-" : SystemTime.formatClock(done.getJoinTimeMins()),
					noShow ? "-" : SystemTime.formatClock(done.getLeaveTimeMins())});
		}
		UiKit.setRows(attendanceTable, attendance);
	}
}
