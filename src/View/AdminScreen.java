package View;

import Controller.AdminController;
import Model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Administrator console: fair timings and phases, organisations, booths, recruiters and the audit log. */
public class AdminScreen extends JFrame {

	private final AdminController controller;
	private final JLabel statusBadge = UiKit.statusBadge();
	private final JLabel messageBar = UiKit.messageBar();
	private final JTabbedPane tabs = new JTabbedPane();

	private final UiKit.TimeInput bookingOpen = new UiKit.TimeInput(9 * 60);
	private final UiKit.TimeInput bookingClose = new UiKit.TimeInput(17 * 60);
	private final UiKit.TimeInput fairStart = new UiKit.TimeInput(SystemTime.MINS_PER_DAY + 9 * 60);
	private final UiKit.TimeInput fairEnd = new UiKit.TimeInput(SystemTime.MINS_PER_DAY + 13 * 60);
	private final JLabel summary = new JLabel();

	private final JTable orgTable = UiKit.table("Organisation", "Description", "Website", "Booths");
	private final JTable boothTable = UiKit.table("Booth", "Organisation", "Recruiters", "Offers", "Slots", "Booked");
	private final JTable auditTable = UiKit.table("Time", "Actor", "Name", "Action", "Details");
	private final JComboBox<Organization> orgCombo = new JComboBox<>();
	private final JComboBox<Booth> boothCombo = new JComboBox<>();

	public AdminScreen(AdminController controller) {
		this.controller = controller;
		setTitle("Virtual Career Fair - Administrator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(820, 560);
		setLayout(new BorderLayout());
		add(UiKit.header("Administrator", "Set up the fair and move it through its phases", statusBadge), BorderLayout.NORTH);
		tabs.addTab("Fair setup", setupTab());
		tabs.addTab("Organisations", orgTab());
		tabs.addTab("Booths & recruiters", boothTab());
		tabs.addTab("Audit log", auditTab());
		add(tabs, BorderLayout.CENTER);
		add(messageBar, BorderLayout.SOUTH);
		orgCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
				return super.getListCellRendererComponent(l, v instanceof Organization ? ((Organization) v).getName() : v, i, s, f);
			}
		});
		boothCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
				return super.getListCellRendererComponent(l, v instanceof Booth ? ((Booth) v).getName() : v, i, s, f);
			}
		});
	}

	private JComponent setupTab() {
		JButton save = new JButton("Save timings");
		save.addActionListener(e -> {
			try {
				controller.handleSetupFair(bookingOpen.getMins(), bookingClose.getMins(), fairStart.getMins(), fairEnd.getMins());
			} catch (RuntimeException ex) {
				showMessage(ex.getMessage(), true);
			}
		});
		JButton advance = new JButton("Advance phase");
		advance.addActionListener(e -> controller.handleAdvanceState());
		JButton reset = new JButton("Reset all data");
		reset.addActionListener(e -> {
			if (JOptionPane.showConfirmDialog(this, "Delete all organisations, candidates and bookings?",
					"Reset fair data", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				controller.handleResetData();
			}
		});

		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		left.add(UiKit.sectionTitle("Fair timings"));
		left.add(UiKit.form("Bookings open", bookingOpen, "Bookings close", bookingClose,
				"Fair starts", fairStart, "Fair ends", fairEnd));
		left.add(UiKit.buttons(save));
		left.add(UiKit.hint("Timings can only be changed while the fair is DORMANT."));
		left.add(Box.createVerticalStrut(14));
		left.add(UiKit.sectionTitle("Fair phase"));
		left.add(UiKit.hint("DORMANT > PREPARING > BOOKINGS OPEN > BOOKINGS CLOSED > FAIR LIVE > FAIR CLOSED"));
		left.add(UiKit.hint("After PREPARING, the clock moves the fair through the phases automatically."));
		left.add(UiKit.buttons(advance, reset));
		for (Component c : left.getComponents()) {
			((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		JPanel right = new JPanel(new BorderLayout());
		right.add(UiKit.sectionTitle("At a glance"), BorderLayout.NORTH);
		summary.setVerticalAlignment(SwingConstants.TOP);
		right.add(summary, BorderLayout.CENTER);
		right.setPreferredSize(new Dimension(250, 10));

		JPanel p = new JPanel(new BorderLayout(16, 0));
		p.add(left, BorderLayout.CENTER);
		p.add(right, BorderLayout.EAST);
		return UiKit.padded(p);
	}

	private JComponent orgTab() {
		JTextField name = new JTextField(18);
		JTextField desc = new JTextField(18);
		JTextField url = new JTextField(18);
		JButton add = new JButton("Add organisation");
		add.addActionListener(e -> {
			controller.handleAddOrg(name.getText(), desc.getText(), url.getText());
			name.setText("");
			desc.setText("");
			url.setText("");
		});
		UiKit.setWidths(orgTable, 170, 240, 200, 60);
		JPanel form = new JPanel(new BorderLayout());
		form.add(UiKit.form("Name", name, "Description", desc, "Website", url), BorderLayout.CENTER);
		form.add(UiKit.buttons(add), BorderLayout.SOUTH);
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(UiKit.scroll(orgTable), BorderLayout.CENTER);
		p.add(form, BorderLayout.SOUTH);
		return UiKit.padded(p);
	}

	private JComponent boothTab() {
		JTextField boothName = new JTextField(16);
		JButton addBooth = new JButton("Add booth");
		addBooth.addActionListener(e -> {
			controller.handleAddBooth((Organization) orgCombo.getSelectedItem(), boothName.getText());
			boothName.setText("");
		});
		JTextField recName = new JTextField(14);
		JTextField recEmail = new JTextField(16);
		JButton assign = new JButton("Assign recruiter");
		assign.addActionListener(e -> {
			controller.handleAssignRecruiter((Booth) boothCombo.getSelectedItem(), recName.getText(), recEmail.getText());
			recName.setText("");
			recEmail.setText("");
		});
		UiKit.setWidths(boothTable, 180, 160, 200, 50, 50, 60);
		JPanel forms = new JPanel(new GridLayout(1, 2, 12, 0));
		JPanel b = new JPanel(new BorderLayout());
		b.add(UiKit.sectionTitle("New booth"), BorderLayout.NORTH);
		b.add(UiKit.form("Organisation", orgCombo, "Booth name", boothName), BorderLayout.CENTER);
		b.add(UiKit.buttons(addBooth), BorderLayout.SOUTH);
		JPanel r = new JPanel(new BorderLayout());
		r.add(UiKit.sectionTitle("Assign a recruiter"), BorderLayout.NORTH);
		r.add(UiKit.form("Booth", boothCombo, "Name", recName, "Email", recEmail), BorderLayout.CENTER);
		r.add(UiKit.buttons(assign), BorderLayout.SOUTH);
		forms.add(b);
		forms.add(r);
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(UiKit.scroll(boothTable), BorderLayout.CENTER);
		p.add(forms, BorderLayout.SOUTH);
		return UiKit.padded(p);
	}

	private JComponent auditTab() {
		UiKit.setWidths(auditTable, 110, 100, 130, 170, 320);
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(UiKit.hint("Every administrator, recruiter, candidate and system action is recorded here (newest first)."),
				BorderLayout.NORTH);
		p.add(UiKit.scroll(auditTable), BorderLayout.CENTER);
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
		UiKit.updateStatus(statusBadge, sys);
		if (sys.hasTimings() && sys.getState() != SystemState.DORMANT) {
			bookingOpen.setMins(sys.getBookingOpenTime());
			bookingClose.setMins(sys.getBookingCloseTime());
			fairStart.setMins(sys.getFairStartTime());
			fairEnd.setMins(sys.getFairEndTime());
		}

		List<Object[]> orgRows = new ArrayList<>();
		List<Object[]> boothRows = new ArrayList<>();
		int booked = 0;
		int slots = 0;
		for (Organization o : sys.getOrganizations()) {
			orgRows.add(new Object[] {o.getName(), o.getDescription(), o.getWebsiteUrl(), o.getBooths().size()});
			for (Booth b : o.getBooths()) {
				int bSlots = 0;
				int bBooked = 0;
				for (Offer of : b.getOffers()) {
					for (AppointmentSlot s : of.getSlots()) {
						bSlots++;
						bBooked += s.getCurrentBookings();
					}
				}
				slots += bSlots;
				booked += bBooked;
				List<String> names = new ArrayList<>();
				for (Recruiter r : b.getRecruiters()) {
					names.add(r.getName());
				}
				boothRows.add(new Object[] {b.getName(), o.getName(), String.join(", ", names), b.getOffers().size(),
						bSlots, bBooked});
			}
		}
		UiKit.setRows(orgTable, orgRows);
		UiKit.setRows(boothTable, boothRows);

		Object selOrg = orgCombo.getSelectedItem();
		orgCombo.removeAllItems();
		for (Organization o : sys.getOrganizations()) {
			orgCombo.addItem(o);
		}
		if (selOrg != null) {
			orgCombo.setSelectedItem(selOrg);
		}
		Object selBooth = boothCombo.getSelectedItem();
		boothCombo.removeAllItems();
		for (Booth b : sys.getAllBooths()) {
			boothCombo.addItem(b);
		}
		if (selBooth != null) {
			boothCombo.setSelectedItem(selBooth);
		}

		List<Object[]> audit = new ArrayList<>();
		List<AuditEntry> entries = controller.getAuditLog();
		for (int i = entries.size() - 1; i >= 0; i--) {
			AuditEntry e = entries.get(i);
			audit.add(new Object[] {SystemTime.format(e.getTimestamp()), e.getActorType(), e.getActorName(),
					e.getAction(), e.getDetails()});
		}
		UiKit.setRows(auditTable, audit);

		summary.setText("<html><table cellpadding=3>"
				+ row("Organisations", sys.getOrganizations().size())
				+ row("Booths", sys.getAllBooths().size())
				+ row("Recruiters", sys.getRecruiters().size())
				+ row("Published offers", sys.getAllOffers().size())
				+ row("Interview slots", slots)
				+ row("Booked places", booked)
				+ row("Candidates", sys.getCandidates().size())
				+ row("Attendance records", sys.getAttendanceRecords().size())
				+ row("Audit entries", entries.size())
				+ "</table></html>");
	}

	private static String row(String label, int value) {
		return "<tr><td>" + label + "</td><td align=right><b>" + value + "</b></td></tr>";
	}
}
