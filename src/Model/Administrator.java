package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Administrator extends User {

	private VCFSystem system;

	public void setupFairTimings(int bookingsOpen, int bookingsClose, int fairStart, int fairEnd) {
		ensureSystem();
		requireState(SystemState.DORMANT, "Fair timings can only be configured in DORMANT state.");
		if (!(bookingsOpen <= bookingsClose && bookingsClose <= fairStart && fairStart <= fairEnd)) {
			throw new IllegalArgumentException("Timings must satisfy open <= close <= start <= end.");
		}
		system.setBookingOpenTime(bookingsOpen);
		system.setBookingCloseTime(bookingsClose);
		system.setFairStartTime(fairStart);
		system.setFairEndTime(fairEnd);
		addAudit(AuditEntry.ACTION_CONFIGURE_FAIR_TIMINGS,
				"open=" + bookingsOpen + ", close=" + bookingsClose + ", start=" + fairStart + ", end=" + fairEnd);
	}

	public void addOrganisation(Organization o) {
		ensureSystem();
		requireStateIn("Manage organizations/booths", SystemState.DORMANT, SystemState.PREPARING);
		if (o == null) {
			throw new IllegalArgumentException("Organization cannot be null.");
		}
		if (o.getName() == null || o.getName().isBlank()) {
			throw new IllegalArgumentException("Organization name is required.");
		}
		for (Organization existing : system.getOrganizations()) {
			if (existing.getName() != null && existing.getName().equalsIgnoreCase(o.getName())) {
				throw new IllegalStateException("Duplicate organization name: " + o.getName());
			}
		}
		if (o.getId() == null || o.getId().isBlank()) {
			o.setId(generateId("ORG"));
		}
		system.addOrganization(o);
		addAudit(AuditEntry.ACTION_ADD_ORGANISATION, "orgId=" + o.getId() + ", name=" + o.getName());
	}

	public void updateOrganisation(Organization o) {
		ensureSystem();
		requireStateIn("Manage organizations/booths", SystemState.DORMANT, SystemState.PREPARING);
		if (o == null || o.getId() == null || o.getId().isBlank()) {
			throw new IllegalArgumentException("Organization ID is required for update.");
		}
		system.updateOrganization(o);
		addAudit(AuditEntry.ACTION_UPDATE_ORGANISATION, "orgId=" + o.getId() + ", name=" + o.getName());
	}

	public void addBooth(Booth b, Organization org) {
		ensureSystem();
		requireStateIn("Manage organizations/booths", SystemState.DORMANT, SystemState.PREPARING);
		if (b == null || org == null) {
			throw new IllegalArgumentException("Booth and organization are required.");
		}
		if (org.getId() == null || system.findOrganizationById(org.getId()) == null) {
			throw new IllegalStateException("Organization must exist in system before adding booth.");
		}
		if (b.getId() == null || b.getId().isBlank()) {
			b.setId(generateId("BOOTH"));
		}
		org.addBooth(b);
		b.setOrganisation(org);
		addAudit(AuditEntry.ACTION_ADD_BOOTH, "boothId=" + b.getId() + ", orgId=" + org.getId());
	}

	public void assignRecruiter(Recruiter r, Booth b) {
		ensureSystem();
		requireStateIn("Manage organizations/booths", SystemState.DORMANT, SystemState.PREPARING);
		if (r == null || b == null) {
			throw new IllegalArgumentException("Recruiter and booth are required.");
		}
		if (r.getUserId() == null || r.getUserId().isBlank()) {
			r.setUserId(generateId("REC"));
		}
		system.registerRecruiter(r);
		b.assignRecruiter(r);
		addAudit(AuditEntry.ACTION_ASSIGN_RECRUITER, "recruiterId=" + r.getUserId() + ", boothId=" + b.getId());
	}

	public void resetFairData() {
		ensureSystem();
		requireState(SystemState.DORMANT, "Reset fair data is only allowed in DORMANT state.");
		system.resetFairData();
		addAudit(AuditEntry.ACTION_RESET_FAIR_DATA, "Fair data reset requested by administrator.");
	}

	public void advanceSystemState() {
		ensureSystem();
		SystemState from = system.getState();
		system.advanceState();
		SystemState to = system.getState();
		addAudit(AuditEntry.ACTION_ADVANCE_SYSTEM_STATE, "from=" + from + ", to=" + to);
	}

	public List<AuditEntry> viewAuditLog() {
		if (system == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(system.getAuditEntries());
	}

	public List<AuditEntry> viewAuditLog(int fromInclusive, int toInclusive, String actionContains) {
		if (system == null) {
			return Collections.emptyList();
		}
		List<AuditEntry> filtered = new ArrayList<>();
		for (AuditEntry entry : system.getAuditEntries()) {
			boolean inRange = entry.getTimestamp() >= fromInclusive && entry.getTimestamp() <= toInclusive;
			boolean actionMatches = entry.actionContains(actionContains);
			if (inRange && actionMatches) {
				filtered.add(entry);
			}
		}
		return filtered;
	}

	public String getUsername() {
		return getName();
	}

	public void setUsername(String username) {
		setName(username);
	}

	public VCFSystem getSystem() {
		return system;
	}

	public void setSystem(VCFSystem system) {
		this.system = system;
	}

	private void ensureSystem() {
		if (system == null) {
			throw new IllegalStateException("VCFSystem is not set on Administrator.");
		}
	}

	private void requireState(SystemState required, String message) {
		if (system.getState() != required) {
			throw new IllegalStateException(message + " Current state: " + system.getState());
		}
	}

	private void requireStateIn(String action, SystemState... allowedStates) {
		SystemState current = system.getState();
		for (SystemState state : allowedStates) {
			if (current == state) {
				return;
			}
		}
		throw new IllegalStateException(action + " not allowed in state " + current);
	}

	private String generateId(String prefix) {
		return prefix + "-" + System.currentTimeMillis();
	}

	private void addAudit(String action, String details) {
		if (system == null) {
			return;
		}
		AuditEntry entry = AuditEntry.now(system, AuditEntry.ACTOR_ADMINISTRATOR,
				Objects.requireNonNullElse(getName(), "unknown-admin"),
				Objects.requireNonNullElse(action, "ADMIN_ACTION"),
				Objects.requireNonNullElse(details, ""));
		system.addAuditEntry(entry);
	}
}
