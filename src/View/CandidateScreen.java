package View;

import Controller.CandidateController;
import Model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Candidate portal: register, browse booths, book or auto-book interviews, and join live sessions. */
public class CandidateScreen extends JFrame {

	private final CandidateController controller;
	private final JLabel statusBadge = UiKit.statusBadge();
	private final JLabel messageBar = UiKit.messageBar();
	private final JTabbedPane tabs = new JTabbedPane();
	private final JComboBox<Object> userCombo = new JComboBox<>();
	private boolean updatingCombo;

	private final DefaultListModel<Booth> boothModel = new DefaultListModel<>();
	private final JList<Booth> boothList = new JList<>(boothModel);
	private final JTable slotTable = UiKit.table("Time", "Offer", "Recruiter", "Tags", "Availability");
	private List<AppointmentSlot> shownSlots = new ArrayList<>();

	private final JTextField tagsField = new JTextField("data, java", 16);
	private final JComboBox<String> orgPref = new JComboBox<>();
	private final JSpinner maxSpinner = new JSpinner(new SpinnerNumberModel(2, 1, AutoRequest.SYSTEM_POLICY_MAX_APPOINTMENTS, 1));
	private final JTable suggestionTable = UiKit.table("Time", "Offer", "Booth", "Organisation");

	private final JTable reservationTable = UiKit.table("Time", "Offer", "Booth", "Recruiter", "Status");
	private List<Reservation> shownReservations = new ArrayList<>();
	private final JLabel liveStatus = new JLabel();

	private final JTable notifTable = UiKit.table("Time", "Type", "Message", "");

	public CandidateScreen(CandidateController controller) {
		this.controller = controller;
		setTitle("Virtual Career Fair - Candidate");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(860, 580);
		setLayout(new BorderLayout());

		JPanel top = new JPanel(new BorderLayout());
		top.add(UiKit.header("Candidate portal", "Find employers, book interviews and join live sessions", statusBadge),
				BorderLayout.NORTH);
		JPanel userBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		userBar.add(new JLabel("Signed in as:"));
		userBar.add(userCombo);
		top.add(userBar, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);

		tabs.addTab("Register", registerTab());
		tabs.addTab("Browse & book", browseTab());
		tabs.addTab("Auto-book", autoTab());
		tabs.addTab("My reservations", reservationsTab());
		tabs.addTab("Notifications", notificationsTab());
		add(tabs, BorderLayout.CENTER);
		add(messageBar, BorderLayout.SOUTH);

		userCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
				Object shown = v instanceof Candidate ? ((Candidate) v).getName() + "  <" + ((Candidate) v).getEmail() + ">" : v;
				return super.getListCellRendererComponent(l, shown, i, s, f);
			}
		});
		userCombo.addActionListener(e -> {
			if (!updatingCombo && userCombo.getSelectedItem() instanceof Candidate) {
				controller.handleSelectCandidate((Candidate) userCombo.getSelectedItem());
			}
		});
	}

	private JComponent registerTab() {
		JTextField name = new JTextField(20);
		JTextField email = new JTextField(20);
		JButton register = new JButton("Register");
		register.addActionListener(e -> {
			controller.handleRegister(name.getText(), email.getText());
			if (controller.getCandidate() != null && controller.getCandidate().getEmail().equalsIgnoreCase(email.getText().trim())) {
				name.setText("");
				email.setText("");
				tabs.setSelectedIndex(1);
			}
		});
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(UiKit.sectionTitle("Create a candidate account"));
		p.add(UiKit.form("Full name", name, "Email", email));
		p.add(UiKit.buttons(register));
		p.add(UiKit.hint("Each email can only be registered once. Existing demo candidates are in the menu above."));
		for (Component c : p.getComponents()) {
			((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.add(p, BorderLayout.NORTH);
		return UiKit.padded(wrap);
	}

	private JComponent browseTab() {
		boothList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boothList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
				Booth b = (Booth) v;
				String text = "<html><b>" + b.getOrganisation().getName() + "</b><br>" + b.getName() + "</html>";
				JLabel c = (JLabel) super.getListCellRendererComponent(l, text, i, s, f);
				c.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
				return c;
			}
		});
		boothList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				refreshSlots();
			}
		});
		UiKit.setWidths(slotTable, 150, 180, 110, 110, 130);
		JButton book = new JButton("Book selected slot");
		book.addActionListener(e -> {
			int row = slotTable.getSelectedRow();
			if (row < 0) {
				showMessage("Select a slot to book.", true);
			} else {
				controller.handleManualBook(shownSlots.get(row));
			}
		});
		JScrollPane boothScroll = UiKit.scroll(boothList);
		boothScroll.setPreferredSize(new Dimension(220, 10));
		JPanel right = new JPanel(new BorderLayout(6, 6));
		right.add(UiKit.scroll(slotTable), BorderLayout.CENTER);
		right.add(UiKit.buttons(book), BorderLayout.SOUTH);
		JPanel p = new JPanel(new BorderLayout(10, 0));
		p.add(boothScroll, BorderLayout.WEST);
		p.add(right, BorderLayout.CENTER);
		return UiKit.padded(p);
	}

	private JComponent autoTab() {
		JButton find = new JButton("Find matching slots");
		find.addActionListener(e -> controller.handleAutoRequest(tagsField.getText(), (String) orgPref.getSelectedItem(),
				(Integer) maxSpinner.getValue()));
		JButton confirm = new JButton("Confirm all");
		confirm.addActionListener(e -> controller.handleAcceptSuggestions());
		JButton discard = new JButton("Discard");
		discard.addActionListener(e -> controller.handleRejectSuggestions());
		UiKit.setWidths(suggestionTable, 110, 220, 190, 170);
		JPanel form = new JPanel(new BorderLayout());
		form.add(UiKit.hint("Tell the system what you're looking for and it will pick non-clashing slots for you."),
				BorderLayout.NORTH);
		form.add(UiKit.form("Interests (tags)", tagsField, "Organisation", orgPref, "Max interviews", maxSpinner),
				BorderLayout.CENTER);
		form.add(UiKit.buttons(find), BorderLayout.SOUTH);
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(form, BorderLayout.NORTH);
		p.add(UiKit.scroll(suggestionTable), BorderLayout.CENTER);
		p.add(UiKit.buttons(confirm, discard), BorderLayout.SOUTH);
		return UiKit.padded(p);
	}

	private JComponent reservationsTab() {
		UiKit.setWidths(reservationTable, 110, 200, 180, 120, 170);
		JButton join = new JButton("Join session");
		join.addActionListener(e -> {
			Reservation r = selectedReservation();
			if (r != null) {
				controller.handleJoinSession(r);
			}
		});
		JButton leave = new JButton("Leave session");
		leave.addActionListener(e -> controller.handleLeaveSession());
		JButton cancel = new JButton("Cancel reservation");
		cancel.addActionListener(e -> {
			Reservation r = selectedReservation();
			if (r != null) {
				controller.handleCancelReservation(r);
			}
		});
		liveStatus.setOpaque(true);
		liveStatus.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		liveStatus.setFont(liveStatus.getFont().deriveFont(Font.BOLD, 13f));
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(liveStatus, BorderLayout.NORTH);
		p.add(UiKit.scroll(reservationTable), BorderLayout.CENTER);
		p.add(UiKit.buttons(join, leave, cancel), BorderLayout.SOUTH);
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

	private Reservation selectedReservation() {
		int row = reservationTable.getSelectedRow();
		if (row < 0) {
			showMessage("Select a reservation first.", true);
			return null;
		}
		return shownReservations.get(row);
	}

	public void showMessage(String msg, boolean error) {
		UiKit.setMessage(messageBar, msg, error);
	}

	public void selectTab(int index) {
		tabs.setSelectedIndex(index);
	}

	public void selectBooth(int index) {
		boothList.setSelectedIndex(index);
	}

	public void selectReservationRow(int row) {
		reservationTable.setRowSelectionInterval(row, row);
	}

	public void setAutoRequestInputs(String tags, int max) {
		tagsField.setText(tags);
		maxSpinner.setValue(max);
	}

	public void refresh() {
		VCFSystem sys = controller.getSystem();
		Candidate me = controller.getCandidate();
		UiKit.updateStatus(statusBadge, sys);

		updatingCombo = true;
		userCombo.removeAllItems();
		userCombo.addItem("(not signed in)");
		for (Candidate c : sys.getCandidates()) {
			userCombo.addItem(c);
		}
		userCombo.setSelectedItem(me != null ? me : "(not signed in)");
		updatingCombo = false;

		Booth selectedBooth = boothList.getSelectedValue();
		boothModel.clear();
		for (Booth b : sys.getAllBooths()) {
			boothModel.addElement(b);
		}
		if (selectedBooth != null && boothModel.contains(selectedBooth)) {
			boothList.setSelectedValue(selectedBooth, false);
		}
		refreshSlots();

		Object selOrg = orgPref.getSelectedItem();
		orgPref.removeAllItems();
		orgPref.addItem("Any organisation");
		for (Organization o : sys.getOrganizations()) {
			orgPref.addItem(o.getName());
		}
		if (selOrg != null) {
			orgPref.setSelectedItem(selOrg);
		}

		List<Object[]> sugg = new ArrayList<>();
		for (Reservation r : controller.getSuggestions()) {
			sugg.add(new Object[] {SystemTime.format(r.getSlot().getStartMins()), r.getSlot().getOffer().getTitle(),
					r.getBooth().getName(), r.getBooth().getOrganisation().getName()});
		}
		UiKit.setRows(suggestionTable, sugg);

		shownReservations = new ArrayList<>();
		List<Object[]> res = new ArrayList<>();
		List<Object[]> notes = new ArrayList<>();
		if (me != null) {
			shownReservations.addAll(me.getReservations());
			shownReservations.sort((a, b) -> Integer.compare(a.getSlot().getStartMins(), b.getSlot().getStartMins()));
			for (Reservation r : shownReservations) {
				res.add(new Object[] {SystemTime.format(r.getSlot().getStartMins()), r.getSlot().getOffer().getTitle(),
						r.getBooth().getName(), r.getRecruiter() != null ? r.getRecruiter().getName() : "",
						describe(r, sys)});
			}
			for (Notification n : me.getNotificationsNewestFirst()) {
				notes.add(new Object[] {SystemTime.format(n.getTimestamp()), n.getType(), n.getMessage(),
						n.isRead() ? "" : "NEW"});
			}
		}
		UiKit.setRows(reservationTable, res);
		UiKit.setRows(notifTable, notes);
		int unread = me == null ? 0 : me.getUnreadCount();
		tabs.setTitleAt(4, unread > 0 ? "Notifications (" + unread + ")" : "Notifications");
		updateLiveStatus(me, sys);
	}

	private void refreshSlots() {
		Booth b = boothList.getSelectedValue();
		shownSlots = controller.getOpenSlots(b);
		Candidate me = controller.getCandidate();
		List<Object[]> rows = new ArrayList<>();
		for (AppointmentSlot s : shownSlots) {
			String availability;
			if (me != null && bookedByMe(me, s)) {
				availability = "Booked by you";
			} else if (s.isFull()) {
				availability = "Full";
			} else if (me != null && me.hasClash(s)) {
				availability = "Clashes with a booking";
			} else {
				int free = s.getMaxBookings() - s.getCurrentBookings();
				availability = free + (free == 1 ? " place left" : " places left");
			}
			rows.add(new Object[] {SystemTime.format(s.getStartMins()) + "-" + SystemTime.formatClock(s.getEndMins()),
					s.getOffer().getTitle(), s.getOffer().getRecruiter().getName(),
					String.join(", ", s.getOffer().getTags()), availability});
		}
		UiKit.setRows(slotTable, rows);
	}

	private static boolean bookedByMe(Candidate me, AppointmentSlot s) {
		for (Reservation r : me.getConfirmedReservation()) {
			if (r.getSlot() == s) {
				return true;
			}
		}
		return false;
	}

	private static String describe(Reservation r, VCFSystem sys) {
		if (r.getStatus() == ReservationStatus.CANCELLED_BY_CANDIDATE) {
			return "Cancelled by you";
		}
		if (r.getStatus() == ReservationStatus.CANCELLED_BY_RECRUITER) {
			return "Cancelled by recruiter";
		}
		AttendanceRecord rec = sys.findAttendanceRecordByReservationId(r.getId());
		if (rec != null) {
			return "Done: " + rec.getOutcome();
		}
		MeetingSession live = r.getBooth().getVirtualRoom().getCurrentSession();
		if (live != null && live.getReservation() == r) {
			return "LIVE NOW";
		}
		return "Confirmed";
	}

	private void updateLiveStatus(Candidate me, VCFSystem sys) {
		Color bg = new Color(0xF2F4F7);
		Color fg = UiKit.MUTED;
		String text;
		if (me == null) {
			text = "Sign in or register to see your interviews.";
		} else if (me.getCurrentMeetingSession() != null) {
			MeetingSession s = me.getCurrentMeetingSession();
			text = "LIVE: in session with " + s.getReservation().getRecruiter().getName() + " at "
					+ s.getReservation().getBooth().getName() + " until " + SystemTime.formatClock(s.getSlot().getEndMins());
			bg = new Color(0xFEE4E2);
			fg = UiKit.ERROR;
		} else if (me.getCurrentLobby() != null) {
			Lobby lobby = me.getCurrentLobby();
			text = "In the lobby for " + lobby.getVirtualRoom().getBooth().getName() + " (" + lobby.size()
					+ " waiting). You'll be admitted when your slot starts.";
			bg = new Color(0xFEF0C7);
			fg = new Color(0x93370D);
		} else if (sys.getState() == SystemState.FAIR_LIVE) {
			text = "The fair is live. Select a confirmed reservation and click Join session.";
			bg = new Color(0xECFDF3);
			fg = UiKit.OK;
		} else {
			text = me.getConfirmedReservation().size() + " confirmed interview(s). Sessions open when the fair goes live.";
		}
		liveStatus.setText(text);
		liveStatus.setBackground(bg);
		liveStatus.setForeground(fg);
	}
}
