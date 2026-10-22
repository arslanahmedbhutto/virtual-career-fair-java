package Model;

import java.util.ArrayList;
import java.util.List;

/**
 * The online meeting room behind a booth. During the live fair it turns confirmed reservations
 * into meeting sessions, admits candidates from the lobby when their slot starts, and records
 * attendance when a session ends or the candidate never shows up.
 */
public class VirtualRoom {

	private Booth booth;
	private boolean isOpen;
	private MeetingSession currentSession;
	private List<MeetingSession> scheduledSessions;
	private List<MeetingSession> pendingAttendanceSessions;
	private final List<MeetingSession> completedSessions;
	private Lobby lobby;

	public VirtualRoom() {
		this.scheduledSessions = new ArrayList<>();
		this.pendingAttendanceSessions = new ArrayList<>();
		this.completedSessions = new ArrayList<>();
		this.lobby = new Lobby(this);
	}

	public void open() {
		isOpen = true;
	}

	public void close() {
		isOpen = false;
	}

	public Booth getBooth() {
		return booth;
	}

	public void setBooth(Booth booth) {
		this.booth = booth;
	}

	public boolean isIsOpen() {
		return isOpen;
	}

	public void setIsOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public MeetingSession getCurrentSession() {
		return currentSession;
	}

	public void setCurrentSession(MeetingSession currentSession) {
		this.currentSession = currentSession;
	}

	public List<MeetingSession> getScheduledSessions() {
		return scheduledSessions;
	}

	public void setScheduledSessions(List<MeetingSession> scheduledSessions) {
		this.scheduledSessions = scheduledSessions;
	}

	public List<MeetingSession> getPendingAttendanceSessions() {
		return pendingAttendanceSessions;
	}

	public void setPendingAttendanceSessions(List<MeetingSession> pendingAttendanceSessions) {
		this.pendingAttendanceSessions = pendingAttendanceSessions;
	}

	public List<MeetingSession> getCompletedSessions() {
		return completedSessions;
	}

	public Lobby getLobby() {
		return lobby;
	}

	public void setLobby(Lobby lobby) {
		this.lobby = lobby;
	}

	/** Admits the candidate at position {@code c} in the lobby into their session, if it is due. */
	public void admitCandidate(int c, int time) {
		if (lobby == null || c < 0 || c >= lobby.size()) {
			return;
		}
		Candidate candidate = lobby.getWaiting().get(c);
		MeetingSession session = findDueSession(candidate, time);
		if (session != null && currentSession == null) {
			lobby.removeCandidate(candidate);
			startSession(session, time);
		}
	}

	/** Puts a candidate in the waiting queue for this room. */
	public void sendToLobby(Candidate c) {
		if (lobby != null) {
			lobby.addCandidate(c);
		}
	}

	/** Ends the session in progress; a non-empty reason marks it as ended early. */
	public MeetingSession endCurrentSession(int time, String reason, VCFSystem sys) {
		MeetingSession s = currentSession;
		if (s == null) {
			return null;
		}
		s.end(time, reason);
		finish(s, sys);
		currentSession = null;
		return s;
	}

	/** Called on every clock tick while the fair is live. */
	public void onTimeUpdate(int mins, VCFSystem sys) {
		buildSessionsFromReservations();
		// 1. Close the current session once its slot is over.
		if (currentSession != null && mins >= currentSession.getSlot().getEndMins()) {
			currentSession.end(currentSession.getSlot().getEndMins(), null);
			finish(currentSession, sys);
			currentSession = null;
		}
		// 2. Candidates who never arrived before their slot ended are no-shows.
		for (MeetingSession s : new ArrayList<>(scheduledSessions)) {
			if (mins >= s.getSlot().getEndMins()) {
				s.markNoShow();
				if (lobby != null) {
					lobby.removeCandidate(s.getCandidate());
				}
				finish(s, sys);
			}
		}
		// 3. Admit the next waiting candidate whose slot has started.
		if (currentSession == null && lobby != null) {
			for (int i = 0; i < lobby.size(); i++) {
				Candidate c = lobby.getWaiting().get(i);
				MeetingSession due = findDueSession(c, mins);
				if (due != null) {
					lobby.removeCandidate(c);
					startSession(due, Math.max(mins, due.getSlot().getStartMins()));
					break;
				}
			}
		}
	}

	public void onDayChange(int day) {
		// Sessions are tied to absolute slot times, so nothing needs resetting at midnight.
	}

	/** Creates a scheduled session for every confirmed reservation at this booth that has none yet. */
	public void buildSessionsFromReservations() {
		if (booth == null) {
			return;
		}
		for (Recruiter r : booth.getRecruiters()) {
			for (Reservation res : r.getReservations()) {
				if (res.getStatus() != ReservationStatus.CONFIRMED || res.getBooth() != booth || hasSession(res)) {
					continue;
				}
				MeetingSession s = new MeetingSession();
				s.setId("SES-" + res.getId());
				s.setReservation(res);
				s.setVirtualRoom(this);
				s.setStatus(SessionStatus.SCHEDULED);
				scheduledSessions.add(s);
			}
		}
		scheduledSessions.sort((a, b) -> Integer.compare(a.getSlot().getStartMins(), b.getSlot().getStartMins()));
	}

	/** The scheduled session for this candidate whose slot is running at {@code time}, if any. */
	public MeetingSession findDueSession(Candidate c, int time) {
		for (MeetingSession s : scheduledSessions) {
			AppointmentSlot slot = s.getSlot();
			if (s.getCandidate() == c && slot.getStartMins() <= time && time < slot.getEndMins()) {
				return s;
			}
		}
		return null;
	}

	private boolean hasSession(Reservation res) {
		if (currentSession != null && currentSession.getReservation() == res) {
			return true;
		}
		for (List<MeetingSession> list : List.of(scheduledSessions, pendingAttendanceSessions, completedSessions)) {
			for (MeetingSession s : list) {
				if (s.getReservation() == res) {
					return true;
				}
			}
		}
		return false;
	}

	private void startSession(MeetingSession s, int time) {
		scheduledSessions.remove(s);
		s.start(time);
		currentSession = s;
		s.getCandidate().setCurrentMeetingSession(s);
	}

	private void finish(MeetingSession s, VCFSystem sys) {
		scheduledSessions.remove(s);
		pendingAttendanceSessions.remove(s);
		completedSessions.add(s);
		s.getCandidate().setCurrentMeetingSession(null);
		if (sys != null) {
			sys.addAttendanceRecord(s.createAttendanceRecord());
			s.addAuditEntry(sys);
			String booth = this.booth != null ? this.booth.getName() : "booth";
			s.getCandidate().addNotification(new Notification("ATTENDANCE",
					"Your session at " + booth + " was recorded as " + s.getStatus() + ".",
					sys.getSystemTime().getTimeMins()));
		}
	}
}
