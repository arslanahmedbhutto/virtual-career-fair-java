package Model;
import java.util.List;
import java.util.ArrayList;

public class Offer{

	private String id;
	private String title;
	private String description;
	private int durationMins;
	private int candidateCapacity;
	private List<String> tags;
	private Recruiter recruiter;
	private Booth booth;
	private boolean isPublished;
        private List<AppointmentSlot> slots;
        
        
        public Offer(String id,String title,String description,int durationMins,int candidateCapacity,Recruiter recruiter,Booth booth, List<String> tags){
            this.title = title;
            this.id = id;
            this.description = description;
            this.durationMins = durationMins;
            this.candidateCapacity = candidateCapacity;
            this.tags=tags;
            this.recruiter=recruiter;
            this.booth=booth;
            this.isPublished = false;
            this.slots = new ArrayList<>();
            
        }

	public void publish() {
		this.isPublished = true;
	}

	public List<AppointmentSlot> getAvailableSlots() {
		List<AppointmentSlot> available = new ArrayList<>();
                for(AppointmentSlot slot : this.slots){
                    if(!slot.isFull()){
                        available.add(slot);
                    }
                }
                return available;
	}

	/**
	 * 
	 * @param tag
	 */
	public boolean hasTag(String tag) {
		if(this.tags == null)return false;
                
                for(String t: this.tags){
                    if(t.equalsIgnoreCase(tag))
                        return true;
                }
            return false;
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCandidateCapacity() {
        return candidateCapacity;
    }

    public void setCandidateCapacity(int candidateCapacity) {
        this.candidateCapacity = candidateCapacity;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(Recruiter recruiter) {
        this.recruiter = recruiter;
    }

    public Booth getBooth() {
        return booth;
    }

    public void setBooth(Booth booth) {
        this.booth = booth;
    }

    public boolean isIsPublished() {
        return isPublished;
    }

    public void setIsPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }

    public List<AppointmentSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<AppointmentSlot> slots) {
        this.slots = slots;
    }
        
        public void addSlot(AppointmentSlot slot){
            this.slots.add(slot);
        }

	public int getDurationMins() {
		return this.durationMins;
	}

}