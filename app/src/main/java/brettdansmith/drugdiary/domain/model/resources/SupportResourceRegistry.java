package brettdansmith.drugdiary.domain.model.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SupportResourceRegistry {
    private static final List<SupportResource> REGISTRY = build();

    private SupportResourceRegistry() {}

    public static List<SupportResource> all() {
        return REGISTRY;
    }

    public static SupportResource lookupById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        for (SupportResource resource : REGISTRY) {
            if (id.equalsIgnoreCase(resource.id)) return resource;
        }
        return null;
    }

    public static List<SupportResource> search(String query, ResourceRegion region, ResourceCategory category) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        List<SupportResource> out = new ArrayList<>();
        for (SupportResource resource : REGISTRY) {
            if (region != null && resource.region != region) continue;
            if (category != null && !resource.categories.contains(category)) continue;
            if (!q.isEmpty() && !matches(resource, q)) continue;
            out.add(resource);
        }
        return out;
    }

    public static List<SupportResource> suggestForAssistant(String userNeed, ResourceRegion preferredRegion, int limit) {
        List<SupportResource> scoped = search(userNeed, preferredRegion, null);
        if (scoped.isEmpty() && preferredRegion != null) {
            scoped = search(userNeed, ResourceRegion.WORLDWIDE, null);
        }
        if (scoped.isEmpty()) {
            scoped = search(userNeed, null, null);
        }
        if (limit <= 0 || scoped.size() <= limit) return scoped;
        return new ArrayList<>(scoped.subList(0, limit));
    }

    public static String buildAssistantSuggestionContext(String userNeed, ResourceRegion preferredRegion) {
        List<SupportResource> suggestions = suggestForAssistant(userNeed, preferredRegion, 5);
        StringBuilder sb = new StringBuilder("Support suggestions for user need: ")
                .append(safe(userNeed))
                .append(". Region preference: ")
                .append(preferredRegion == null ? "any" : preferredRegion.label())
                .append(". Suggested resources: ");
        for (int i = 0; i < suggestions.size(); i++) {
            SupportResource r = suggestions.get(i);
            if (i > 0) sb.append(" | ");
            sb.append(r.name).append(" [").append(r.region.label()).append("] ");
            sb.append("classifications=").append(classifications(r)).append("; ");
            sb.append("call=").append(safe(r.phone)).append("; ");
            sb.append("message=").append(safe(r.messageContact)).append("; ");
            sb.append("website=").append(safe(r.websiteUrl));
        }
        sb.append(". Keep advice focused on harm minimisation, safety, and help-seeking support.");
        return sb.toString();
    }

    public static String buildAssistantContext(SupportResource resource) {
        if (resource == null) return "";
        return "Resource context: " + resource.name
                + " | Classification: " + classifications(resource)
                + " | Region: " + resource.region.label() + (resource.regionDetails == null ? "" : " (" + resource.regionDetails + ")")
                + " | Availability: " + safe(resource.availability)
                + " | Contact: call=" + safe(resource.phone) + ", message=" + safe(resource.messageContact)
                + " | Link: " + safe(resource.websiteUrl)
                + " | Description: " + safe(resource.description)
                + " | Notes: " + safe(resource.notes)
                + ". Please provide harm-minimisation focused guidance only; no political discussion.";
    }

    public static String classifications(SupportResource resource) {
        if (resource == null || resource.categories.isEmpty()) return "General";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resource.categories.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(pretty(resource.categories.get(i)));
        }
        return sb.toString();
    }

    private static boolean matches(SupportResource r, String q) {
        return contains(r.name, q)
                || contains(r.description, q)
                || contains(r.regionDetails, q)
                || contains(r.phone, q)
                || contains(r.messageContact, q)
                || contains(r.websiteUrl, q)
                || contains(classifications(r), q);
    }

    private static boolean contains(String v, String q) {
        return v != null && v.toLowerCase(Locale.US).contains(q);
    }

    private static String pretty(ResourceCategory category) {
        return category.name().toLowerCase(Locale.US).replace('_', ' ');
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "n/a" : value.trim();
    }

    private static List<SupportResource> build() {
        List<SupportResource> resources = new ArrayList<>();

        resources.add(new SupportResource("us-988", "988 Suicide & Crisis Lifeline", "US crisis and suicide prevention support via call, text, and chat.", "988", "988", "https://988lifeline.org/", ResourceRegion.US, "United States", "24/7", "Official 988 service for emotional distress and suicide crises.", Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.MENTAL_HEALTH_CRISIS)));
        resources.add(new SupportResource("us-poison", "US Poison Control", "Expert poison and overdose guidance including medication, chemical, and substance exposure.", "1-800-222-1222", null, "https://www.poison.org/", ResourceRegion.US, "United States", "24/7", "Use early when exposure is suspected and severity is unclear.", Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.POISONING_OVERDOSE)));
        resources.add(new SupportResource("us-samhsa", "SAMHSA National Helpline", "Treatment referral and information for substance use and mental health services.", "1-800-662-4357", null, "https://www.samhsa.gov/find-help/national-helpline", ResourceRegion.US, "United States", "24/7, 365 days", "Confidential referral support in English and Spanish.", Arrays.asList(ResourceCategory.DRUG_ALCOHOL, ResourceCategory.MENTAL_HEALTH_CRISIS)));

        resources.add(new SupportResource("au-lifeline", "Lifeline Australia", "Crisis support and suicide prevention support for people in Australia.", "13 11 14", "0477 13 11 14", "https://www.lifeline.org.au/", ResourceRegion.AU, "Australia", "24/7", "Phone and text support options.", Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.MENTAL_HEALTH_CRISIS)));
        resources.add(new SupportResource("au-poisons", "Poisons Information Centre (AU)", "Clinical poison advice and triage guidance.", "13 11 26", null, "https://www.healthdirect.gov.au/poisons-information-centre", ResourceRegion.AU, "Australia", "24/7", "For poison exposure and overdose concerns.", Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.POISONING_OVERDOSE)));
        resources.add(new SupportResource("au-counselling", "Alcohol and Drug Support Directory (AU)", "Directory of state and territory alcohol and other drug support services.", null, null, "https://adf.org.au/reducing-risk/support-services/", ResourceRegion.AU, "Australia", "By state", "Includes phone and online support pathways.", Arrays.asList(ResourceCategory.DRUG_ALCOHOL, ResourceCategory.RECOVERY_MEETINGS)));

        resources.add(new SupportResource("uk-samaritans", "Samaritans (UK)", "Emotional support in distress.", "116 123", null, "https://www.samaritans.org/", ResourceRegion.UK, "United Kingdom", "24/7", "Confidential listening support.", Arrays.asList(ResourceCategory.MENTAL_HEALTH_CRISIS, ResourceCategory.EMERGENCY)));
        resources.add(new SupportResource("uk-frank", "FRANK", "Drug information and confidential advice.", "0300 123 6600", "82111", "https://www.talktofrank.com/", ResourceRegion.UK, "United Kingdom", "Daily", "UK drug education and support.", Arrays.asList(ResourceCategory.DRUG_ALCOHOL, ResourceCategory.POISONING_OVERDOSE)));
        resources.add(new SupportResource("uk-nhs", "NHS Mental Health Help", "NHS urgent mental health support pathways.", "111", null, "https://www.nhs.uk/nhs-services/mental-health-services/", ResourceRegion.UK, "United Kingdom", "24/7 via 111", "Escalate to emergency care where needed.", Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.MENTAL_HEALTH_CRISIS)));

        resources.add(new SupportResource("world-ifrc", "International Red Cross and Red Crescent Directory", "Find national society contacts for emergency and health support information.", null, null, "https://www.ifrc.org/national-societies-directory", ResourceRegion.WORLDWIDE, "Global", "Varies by country", "Useful for country-specific emergency support entry points.", Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.FAMILY_FRIENDS)));
        resources.add(new SupportResource("world-na", "Narcotics Anonymous", "Recovery meetings and peer support network.", null, null, "https://na.org/meetingsearch/", ResourceRegion.WORLDWIDE, "Global", "Varies", "Meeting finder by country and city.", Arrays.asList(ResourceCategory.RECOVERY_MEETINGS, ResourceCategory.FAMILY_FRIENDS)));
        resources.add(new SupportResource("world-aa", "Alcoholics Anonymous", "Alcohol recovery meetings and sponsor networks.", null, null, "https://www.aa.org/find-aa", ResourceRegion.WORLDWIDE, "Global", "Varies", "In-person and online meetings.", Arrays.asList(ResourceCategory.RECOVERY_MEETINGS)));
        resources.add(new SupportResource("world-smart", "SMART Recovery", "Science-based recovery meetings and tools.", null, null, "https://smartrecovery.org/", ResourceRegion.WORLDWIDE, "Global", "Varies", "Self-management focused recovery support.", Arrays.asList(ResourceCategory.RECOVERY_MEETINGS, ResourceCategory.DRUG_ALCOHOL)));

        resources.add(new SupportResource("world-dancesafe", "DanceSafe", "Drug checking education and harm reduction resources.", null, null, "https://dancesafe.org/", ResourceRegion.WORLDWIDE, "US + International chapters", "Online", "Event and festival harm reduction guidance.", Arrays.asList(ResourceCategory.DRUG_ALCOHOL, ResourceCategory.POISONING_OVERDOSE)));
        resources.add(new SupportResource("world-erowid", "Erowid", "Substance information library and experience reports.", null, null, "https://erowid.org/", ResourceRegion.WORLDWIDE, "Global usage, US-hosted", "Online", "Informational resource; not emergency triage.", Arrays.asList(ResourceCategory.DRUG_ALCOHOL)));
        resources.add(new SupportResource("world-psychonaut", "PsychonautWiki", "Dose, effects and risk profiles for psychoactive substances.", null, null, "https://psychonautwiki.org/", ResourceRegion.WORLDWIDE, "Global", "Online", "Reference data should be cross-checked with medical care.", Arrays.asList(ResourceCategory.DRUG_ALCOHOL, ResourceCategory.POISONING_OVERDOSE)));

        return Collections.unmodifiableList(resources);
    }
}
