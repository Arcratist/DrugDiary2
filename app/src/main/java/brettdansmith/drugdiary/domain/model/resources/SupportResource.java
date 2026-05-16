package brettdansmith.drugdiary.domain.model.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SupportResource {
    public final String id;
    public final String name;
    public final String description;
    public final String phone;
    public final String messageContact;
    public final String websiteUrl;
    public final ResourceRegion region;
    public final String regionDetails;
    public final String availability;
    public final String notes;
    public final List<ResourceCategory> categories;

    public SupportResource(
            String id,
            String name,
            String description,
            String phone,
            String messageContact,
            String websiteUrl,
            ResourceRegion region,
            String regionDetails,
            String availability,
            String notes,
            List<ResourceCategory> categories) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.phone = phone;
        this.messageContact = messageContact;
        this.websiteUrl = websiteUrl;
        this.region = region;
        this.regionDetails = regionDetails;
        this.availability = availability;
        this.notes = notes;
        this.categories = categories == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(categories));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPhone() { return phone; }
    public String getMessageContact() { return messageContact; }
    public String getWebsiteUrl() { return websiteUrl; }
    public ResourceRegion getRegion() { return region; }
    public String getRegionDetails() { return regionDetails; }
    public String getAvailability() { return availability; }
    public String getNotes() { return notes; }
    public List<ResourceCategory> getCategories() { return categories; }
}
