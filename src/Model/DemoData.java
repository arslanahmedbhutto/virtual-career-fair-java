package Model;

import java.util.List;

/**
 * Seeds the system with a small, fictional career fair so every screen has something to show:
 * three organisations, three booths with recruiters, five published offers with interview slots,
 * and three candidates (two of them already booked). The clock starts on Day 1 at 10:00, while
 * bookings are open.
 */
public final class DemoData {

	private static final int DAY = SystemTime.MINS_PER_DAY;

	private DemoData() {
	}

	public static void seed(VCFSystem system, Administrator admin) {
		admin.setSystem(system);
		if (admin.getName() == null) {
			admin.setName("Fair Administrator");
		}

		// Bookings: Day 1 09:00-17:00. Live fair: Day 2 09:00-13:00.
		admin.setupFairTimings(9 * 60, 17 * 60, DAY + 9 * 60, DAY + 13 * 60);
		admin.advanceSystemState(); // DORMANT -> PREPARING

		Recruiter priya = addCompany(system, admin, "Northwind Analytics", "Data and AI consultancy",
				"https://northwind.example.com", "Data Science Booth", "Priya Shah", "priya.shah@northwind.example.com");
		Recruiter daniel = addCompany(system, admin, "Bluepeak Software", "Cloud software products",
				"https://bluepeak.example.com", "Engineering Booth", "Daniel Okafor", "daniel.okafor@bluepeak.example.com");
		Recruiter sofia = addCompany(system, admin, "Greenline Finance", "Retail and investment banking",
				"https://greenline.example.com", "Graduate Programme Booth", "Sofia Marin", "sofia.marin@greenline.example.com");

		Offer analyst = system.publishOffer(priya, "Graduate Data Analyst", "SQL, dashboards and reporting.", 20, 1,
				List.of("data", "sql"));
		Offer ml = system.publishOffer(priya, "Machine Learning Intern", "Python and model evaluation.", 30, 1,
				List.of("ml", "python", "data"));
		Offer java = system.publishOffer(daniel, "Junior Java Developer", "Backend services in Java.", 30, 1,
				List.of("java", "backend"));
		Offer frontend = system.publishOffer(daniel, "Frontend Developer", "React and TypeScript.", 20, 1,
				List.of("react", "frontend"));
		Offer finance = system.publishOffer(sofia, "Graduate Analyst Programme", "Two-year rotational programme.", 30, 2,
				List.of("finance", "data"));

		int fair = DAY + 9 * 60;
		system.addAvailability(priya, analyst, fair, fair + 2 * 60, 5);
		system.addAvailability(priya, ml, fair + 2 * 60, fair + 4 * 60, 0);
		system.addAvailability(daniel, java, fair, fair + 2 * 60, 10);
		system.addAvailability(daniel, frontend, fair + 2 * 60, fair + 3 * 60, 5);
		system.addAvailability(sofia, finance, fair + 30, fair + 3 * 60 + 30, 0);

		// Open bookings by moving the clock to Day 1, 10:00.
		system.getSystemTime().jumpTo(1, 10 * 60);

		Candidate ava = system.registerCandidate("Ava Thompson", "ava.thompson@example.com");
		Candidate leo = system.registerCandidate("Leo Martins", "leo.martins@example.com");
		system.registerCandidate("Hana Ito", "hana.ito@example.com");

		system.bookSlot(ava, analyst.getSlots().get(0));   // Day 2, 09:00
		system.bookSlot(ava, java.getSlots().get(2));      // Day 2, 10:20
		system.bookSlot(leo, java.getSlots().get(0));      // Day 2, 09:00
		system.bookSlot(leo, finance.getSlots().get(1));   // Day 2, 10:00
	}

	private static Recruiter addCompany(VCFSystem system, Administrator admin, String org, String description,
			String url, String boothName, String recruiterName, String email) {
		Organization o = new Organization(null, org, description, url);
		admin.addOrganisation(o);
		o.setId("ORG-" + system.getOrganizations().size());
		Booth b = new Booth("BOOTH-" + system.getOrganizations().size(), boothName, o);
		admin.addBooth(b, o);
		Recruiter r = new Recruiter("REC-" + system.getOrganizations().size(), recruiterName, email);
		admin.assignRecruiter(r, b);
		return r;
	}
}
