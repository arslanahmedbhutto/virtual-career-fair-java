package Controller;

import Model.*;
import View.AdminScreen;

import java.util.List;

/** Handles administrator actions: fair setup, organisations, booths, recruiters and the audit log. */
public class AdminController implements SubscriberInterface {

	private final VCFSystem system;
	private final Administrator admin;
	private AdminScreen view;

	public AdminController(VCFSystem system, Administrator admin) {
		this.system = system;
		this.admin = admin;
		this.admin.setSystem(system);
		this.system.addSubscriber(this);
	}

	public void setView(AdminScreen view) {
		this.view = view;
		refreshView();
	}

	@Override
	public void update() {
		refreshView();
	}

	public VCFSystem getSystem() {
		return system;
	}

	public void handleSetupFair(int bookingOpen, int bookingClose, int fairStart, int fairEnd) {
		run(() -> admin.setupFairTimings(bookingOpen, bookingClose, fairStart, fairEnd),
				"Fair timings saved: bookings " + SystemTime.format(bookingOpen) + " to " + SystemTime.format(bookingClose)
						+ ", fair " + SystemTime.format(fairStart) + " to " + SystemTime.format(fairEnd) + ".");
	}

	public void handleAddOrg(String name, String description, String url) {
		run(() -> admin.addOrganisation(new Organization(null, name == null ? null : name.trim(), description, url)),
				"Organisation \"" + name + "\" added.");
	}

	public void handleAddBooth(Organization org, String boothName) {
		if (boothName == null || boothName.isBlank()) {
			showError("Enter a booth name.");
			return;
		}
		run(() -> admin.addBooth(new Booth(null, boothName.trim(), org), org), "Booth \"" + boothName + "\" added.");
	}

	public void handleAssignRecruiter(Booth booth, String name, String email) {
		if (booth == null || name == null || name.isBlank() || email == null || !email.contains("@")) {
			showError("Select a booth and enter the recruiter's name and a valid email.");
			return;
		}
		Recruiter r = new Recruiter("REC-" + (system.getRecruiters().size() + 1), name.trim(), email.trim());
		run(() -> admin.assignRecruiter(r, booth), name + " assigned to " + booth.getName() + ".");
	}

	public void handleAdvanceState() {
		SystemState before = system.getState();
		run(admin::advanceSystemState, null);
		if (system.getState() != before) {
			showInfo("Fair phase advanced from " + before + " to " + system.getState() + ".");
		} else {
			showError("The fair is already in its final phase.");
		}
	}

	public void handleResetData() {
		run(admin::resetFairData, "All fair data has been reset.");
	}

	public List<AuditEntry> getAuditLog() {
		return admin.viewAuditLog();
	}

	public void refreshView() {
		if (view != null) {
			view.refresh();
		}
	}

	private void run(Runnable action, String successMessage) {
		try {
			action.run();
			if (successMessage != null) {
				showInfo(successMessage);
			}
		} catch (RuntimeException e) {
			showError(e.getMessage());
		}
	}

	private void showInfo(String msg) {
		if (view != null) {
			view.showMessage(msg, false);
		}
	}

	private void showError(String msg) {
		if (view != null) {
			view.showMessage(msg, true);
		}
	}
}
