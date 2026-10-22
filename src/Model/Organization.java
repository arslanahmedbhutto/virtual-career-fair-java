package Model;

import java.util.ArrayList;
import java.util.List;

public class Organization {
    private String id;
    private String name;
    private String description;
    private String websiteUrl;
    private List<Booth> booths;
    private List<Recruiter> recruiters;
    private List<Offer> offers;

    public Organization(String id, String name, String description, String websiteUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.websiteUrl = websiteUrl;
        this.booths = new ArrayList<>();
        this.recruiters = new ArrayList<>();
        this.offers = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public void addBooth(Booth booth) {
        if (booth != null && !booths.contains(booth)) {
            booths.add(booth);
        }
    }

    public List<Booth> getBooths() {
        return booths;
    }

    public void setBooths(List<Booth> booths) {
        this.booths = booths;
    }

    public List<Recruiter> getRecruiters() {
        return recruiters;
    }

    public void setRecruiters(List<Recruiter> recruiters) {
        this.recruiters = recruiters;
    }

    public List<Offer> getOffers() {
        return offers;
    }

    public void setOffers(List<Offer> offers) {
        this.offers = offers;
    }
}
