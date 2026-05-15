package brettdansmith.drugdiary.domain.model.resources;

import java.util.List;

public final class SupportResource {
    public final String name;
    public final String description;
    public final String phone;
    public final String website;
    public final String region;
    public final String availability;
    public final String notes;
    public final String sms;
    public final String websiteUrl;
    public final String meetingFinderUrl;
    public final List<ResourceCategory> categories;

    public SupportResource(String name, String description, String phone, String website, String region, String availability, String notes, String sms, String websiteUrl, String meetingFinderUrl, List<ResourceCategory> categories) {
        this.name = name;
        this.description = description;
        this.phone = phone;
        this.website = website;
        this.region = region;
        this.availability = availability;
        this.notes = notes;
        this.sms = sms;
        this.websiteUrl = websiteUrl;
        this.meetingFinderUrl = meetingFinderUrl;
        this.categories = categories;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPhone() {
        return phone;
    }

    public String getWebsite() {
        return website;
    }

    public String getRegion() {
        return region;
    }

    public String getAvailability() {
        return availability;
    }

    public String getNotes() {
        return notes;
    }

    public String getSms() {
        return sms;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getMeetingFinderUrl() {
        return meetingFinderUrl;
    }

    public List<ResourceCategory> getCategories() {
        return categories;
    }
}
