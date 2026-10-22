import Controller.*;
import Model.*;
import View.*;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point. Builds the model, seeds the demo fair, creates one controller per role and opens
 * the four screens side by side. Each controller subscribes to {@link VCFSystem}, so any change
 * made in one window (or by the clock) refreshes all of them (observer pattern).
 *
 * <p>Run with {@code --empty} to start from a blank, DORMANT fair instead of the demo data.
 */
public class Main {

	/** Handles to the running application (also used by the screenshot tour). */
	public static final class App {
		public VCFSystem system;
		public AdminController adminController;
		public CandidateController candidateController;
		public RecruiterController recruiterController;
		public TimerController timerController;
		public AdminScreen adminScreen;
		public CandidateScreen candidateScreen;
		public RecruiterScreen recruiterScreen;
		public SystemTimerScreen timerScreen;
	}

	public static void main(String[] args) {
		boolean demo = args.length == 0 || !"--empty".equals(args[0]);
		SwingUtilities.invokeLater(() -> {
			App app = launch(demo);
			tile(app);
			app.adminScreen.setVisible(true);
			app.recruiterScreen.setVisible(true);
			app.candidateScreen.setVisible(true);
			app.timerScreen.setVisible(true);
			System.out.println("Virtual Career Fair is running: " + app.system.getState() + " at "
					+ app.system.getSystemTime());
		});
	}

	/** Creates the model, controllers and (hidden) screens. Must be called on the Swing event thread. */
	public static App launch(boolean demoData) {
		useNimbusLookAndFeel();
		App app = new App();
		app.system = new VCFSystem();
		Administrator admin = new Administrator();
		admin.setName("Fair Administrator");
		if (demoData) {
			DemoData.seed(app.system, admin);
		}

		app.adminController = new AdminController(app.system, admin);
		app.candidateController = new CandidateController(app.system);
		app.recruiterController = new RecruiterController(app.system);
		app.timerController = new TimerController(app.system.getSystemTime(), app.system);

		app.adminScreen = new AdminScreen(app.adminController);
		app.candidateScreen = new CandidateScreen(app.candidateController);
		app.recruiterScreen = new RecruiterScreen(app.recruiterController);
		app.timerScreen = new SystemTimerScreen(app.timerController);

		app.adminController.setView(app.adminScreen);
		app.candidateController.setView(app.candidateScreen);
		app.recruiterController.setView(app.recruiterScreen);
		app.timerController.setView(app.timerScreen);

		if (!app.system.getCandidates().isEmpty()) {
			app.candidateController.handleSelectCandidate(app.system.getCandidates().get(0));
		}
		if (!app.system.getRecruiters().isEmpty()) {
			app.recruiterController.handleSelectRecruiter(app.system.getRecruiters().get(0));
		}
		return app;
	}

	/** Arranges the four windows in a grid that fits the screen. */
	private static void tile(App app) {
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int w = screen.width / 2;
		int h = screen.height / 2;
		place(app.adminScreen, screen.x, screen.y, w, h);
		place(app.recruiterScreen, screen.x + w, screen.y, w, h);
		place(app.candidateScreen, screen.x, screen.y + h, w, h);
		place(app.timerScreen, screen.x + w, screen.y + h, Math.min(w, 460), Math.min(h, 420));
	}

	private static void place(Window win, int x, int y, int maxW, int maxH) {
		win.setBounds(x, y, Math.min(win.getWidth(), maxW), Math.min(win.getHeight(), maxH));
	}

	private static void useNimbusLookAndFeel() {
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					return;
				}
			}
		} catch (Exception ignored) {
			// fall back to the default look and feel
		}
	}
}
