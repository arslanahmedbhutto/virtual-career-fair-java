package View;

import Model.SystemState;
import Model.SystemTime;
import Model.VCFSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Shared look and feel helpers so the four screens are consistent. */
final class UiKit {

	static final Color PRIMARY = new Color(0x1F4E79);
	static final Color MUTED = new Color(0x5B6770);
	static final Color OK = new Color(0x1B7F3B);
	static final Color ERROR = new Color(0xB42318);

	private UiKit() {
	}

	/** Title bar with a subtitle and the live clock/phase badge on the right. */
	static JPanel header(String title, String subtitle, JLabel statusBadge) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(PRIMARY);
		p.setBorder(new EmptyBorder(10, 14, 10, 14));
		JLabel t = new JLabel(title);
		t.setForeground(Color.WHITE);
		t.setFont(t.getFont().deriveFont(Font.BOLD, 18f));
		JLabel s = new JLabel(subtitle);
		s.setForeground(new Color(0xD6E4F0));
		JPanel left = new JPanel(new GridLayout(2, 1));
		left.setOpaque(false);
		left.add(t);
		left.add(s);
		p.add(left, BorderLayout.WEST);
		p.add(statusBadge, BorderLayout.EAST);
		return p;
	}

	static JLabel statusBadge() {
		JLabel l = new JLabel(" ");
		l.setOpaque(true);
		l.setForeground(Color.WHITE);
		l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
		l.setBorder(new EmptyBorder(6, 12, 6, 12));
		return l;
	}

	static void updateStatus(JLabel badge, VCFSystem sys) {
		SystemState st = sys.getState();
		badge.setText(SystemTime.format(sys.getSystemTime().getTimeMins()) + "   |   " + st.name().replace('_', ' '));
		badge.setBackground(phaseColor(st));
	}

	static Color phaseColor(SystemState st) {
		switch (st) {
			case PREPARING: return new Color(0x6B5BA8);
			case BOOKINGS_OPEN: return new Color(0x1B7F3B);
			case BOOKINGS_CLOSED: return new Color(0xB25E09);
			case FAIR_LIVE: return new Color(0xC0262D);
			case FAIR_CLOSED: return new Color(0x475467);
			default: return new Color(0x667085);
		}
	}

	/** Status line at the bottom of each screen for success and error messages. */
	static JLabel messageBar() {
		JLabel l = new JLabel(" ");
		l.setBorder(new EmptyBorder(8, 14, 8, 14));
		l.setOpaque(true);
		l.setBackground(new Color(0xF2F4F7));
		return l;
	}

	static void setMessage(JLabel bar, String msg, boolean error) {
		bar.setText(msg == null ? " " : msg);
		bar.setForeground(error ? ERROR : OK);
		bar.setBackground(error ? new Color(0xFEF3F2) : new Color(0xECFDF3));
	}

	static JTable table(String... columns) {
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		JTable t = new JTable(model);
		t.setRowHeight(24);
		t.setFillsViewportHeight(true);
		t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		t.getTableHeader().setReorderingAllowed(false);
		return t;
	}

	/** Replaces all rows while keeping the selected row (when it still exists). */
	static void setRows(JTable table, List<Object[]> rows) {
		int selected = table.getSelectedRow();
		DefaultTableModel m = (DefaultTableModel) table.getModel();
		m.setRowCount(0);
		for (Object[] r : rows) {
			m.addRow(r);
		}
		if (selected >= 0 && selected < rows.size()) {
			table.setRowSelectionInterval(selected, selected);
		}
	}

	static void setWidths(JTable table, int... widths) {
		for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
		}
	}

	static JScrollPane scroll(Component c) {
		JScrollPane sp = new JScrollPane(c);
		sp.setBorder(BorderFactory.createLineBorder(new Color(0xD0D5DD)));
		return sp;
	}

	static JPanel padded(JComponent c) {
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.setBorder(new EmptyBorder(10, 10, 10, 10));
		p.add(c, BorderLayout.CENTER);
		return p;
	}

	static JLabel sectionTitle(String text) {
		JLabel l = new JLabel(text);
		l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
		l.setForeground(PRIMARY);
		l.setBorder(new EmptyBorder(4, 0, 6, 0));
		return l;
	}

	static JLabel hint(String text) {
		JLabel l = new JLabel(text);
		l.setForeground(MUTED);
		return l;
	}

	/** Builds a two-column form from label/component pairs. */
	static JPanel form(Object... labelsAndFields) {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(4, 4, 4, 4);
		g.anchor = GridBagConstraints.WEST;
		for (int i = 0; i < labelsAndFields.length; i += 2) {
			g.gridy = i / 2;
			g.gridx = 0;
			g.weightx = 0;
			g.fill = GridBagConstraints.NONE;
			Object label = labelsAndFields[i];
			p.add(label instanceof Component ? (Component) label : new JLabel(String.valueOf(label)), g);
			g.gridx = 1;
			g.weightx = 1;
			g.fill = GridBagConstraints.HORIZONTAL;
			p.add((Component) labelsAndFields[i + 1], g);
		}
		// Keep forms at their natural height inside vertical BoxLayouts.
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
		return p;
	}

	static JPanel buttons(JComponent... comps) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		for (JComponent c : comps) {
			p.add(c);
		}
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
		return p;
	}

	/** Day + HH:MM picker that converts to absolute minutes. */
	static final class TimeInput extends JPanel {
		private final JSpinner day = new JSpinner(new SpinnerNumberModel(1, 1, 7, 1));
		private final JTextField time = new JTextField("09:00", 5);

		TimeInput(int absoluteMins) {
			super(new FlowLayout(FlowLayout.LEFT, 4, 0));
			add(new JLabel("Day"));
			add(day);
			add(time);
			setMins(absoluteMins);
		}

		void setMins(int abs) {
			day.setValue(abs / SystemTime.MINS_PER_DAY + 1);
			time.setText(SystemTime.formatClock(abs));
		}

		int getMins() {
			String[] parts = time.getText().trim().split(":");
			if (parts.length != 2) {
				throw new IllegalArgumentException("Times must look like 09:30.");
			}
			int h = Integer.parseInt(parts[0].trim());
			int m = Integer.parseInt(parts[1].trim());
			if (h < 0 || h > 23 || m < 0 || m > 59) {
				throw new IllegalArgumentException("Times must be between 00:00 and 23:59.");
			}
			return ((Integer) day.getValue() - 1) * SystemTime.MINS_PER_DAY + h * 60 + m;
		}
	}
}
