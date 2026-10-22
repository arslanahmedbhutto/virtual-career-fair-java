
package Model;
import java.util.List;
import java.util.ArrayList;
public class Booth {

	private String id;
	private String name;
	private Organization organisation;
	private List<Recruiter> recruiters;
	private List<Offer> offers;
	private VirtualRoom virtualRoom;
        
        
       // 1. Removed recruiter, offers, and virtualRoom from the parameters!
    public Booth(String id, String name, Organization organisation) {
        this.id = id;
        this.name = name;
        this.organisation = organisation;
        
        // 2. These initialize perfectly on their own!
        this.recruiters = new ArrayList<>();
        this.offers = new ArrayList<>();
        this.virtualRoom = new VirtualRoom();
        this.virtualRoom.setBooth(this);
    
        }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Recruiter> getRecruiters() {
        return recruiters;
    }

    public void setRecruiters(List<Recruiter> recruiters) {
        this.recruiters = recruiters;
    }

	/**
	 * 
	 * @param r
	 */
	public void assignRecruiter(Recruiter r) {
		if(!this.recruiters.contains(r)){
                    this.recruiters.add(r);
                }
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

	/**
	 * 
	 * @param o
	 */
	public void addOffer(Offer o) {
            if(!this.offers.contains(o)){
                this.offers.add(o);
                o.setBooth(this);
            }
		
	}

	public List<Offer> getOffers() {
		return this.offers;
	}

    public Organization getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organization organisation) {
        this.organisation = organisation;
    }

	public VirtualRoom getVirtualRoom() {
		return this.virtualRoom;
	}

	public void openVirtualRoom() {
		if(this.virtualRoom!=null){
                    this.virtualRoom.open();
                }
	}

	public void closeVirtualRoom() {
		if(this.virtualRoom!=null){
                    this.virtualRoom.close();
                }
	}
}