package brettdansmith.drugdiary.domain.model.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SupportResourceProvider {
    private SupportResourceProvider() {
    }

    public static List<SupportResource> getResources() {
        List<SupportResource> resources = new ArrayList<>();

        // Emergency & Crisis Support
        resources.add(new SupportResource(
                "988 Lifeline",
                "24/7 call or text for mental health crisis support",
                "988",
                "Call or text",
                "US",
                "24/7",
                "Free confidential support. Trained professionals ready to help.",
                null,
                "https://988lifeline.org/",
                null,
                Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.MENTAL_HEALTH_CRISIS)
        ));

        resources.add(new SupportResource(
                "Poison Help",
                "Immediate poison control assistance",
                "1-800-222-1222",
                "Call",
                "US",
                "24/7",
                "Free. Expert help for poisoning emergencies.",
                null,
                "https://poisonhelp.hrsa.gov/",
                null,
                Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.POISONING_OVERDOSE)
        ));

        // International Resources
        resources.add(new SupportResource(
                "Lifeline Australia",
                "24/7 crisis line for emotional support",
                "13 11 14",
                "Call",
                "Australia",
                "24/7",
                "Free Australian crisis support service",
                null,
                "https://www.lifeline.org.au/",
                null,
                Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.MENTAL_HEALTH_CRISIS)
        ));

        resources.add(new SupportResource(
                "Poisons Information Centre Australia",
                "Poison emergency and information",
                "13 11 26",
                "Call",
                "Australia",
                "24/7",
                "Free guidance for poison-related emergencies",
                null,
                "https://www.healthdirect.gov.au/poisons-information-centre",
                null,
                Arrays.asList(ResourceCategory.EMERGENCY, ResourceCategory.POISONING_OVERDOSE)
        ));

        // Substance Abuse Treatment
        resources.add(new SupportResource(
                "SAMHSA National Helpline",
                "Substance abuse and mental health treatment referral",
                "1-800-662-4357",
                "Call",
                "US",
                "24/7",
                "Free, confidential referral and information service.",
                null,
                "https://www.samhsa.gov/find-help/helplines/national-helpline",
                null,
                Arrays.asList(ResourceCategory.DRUG_ALCOHOL)
        ));

        resources.add(new SupportResource(
                "Find Treatment",
                "Locate treatment facilities near you",
                null,
                "Website",
                "US",
                "Online",
                "Search for treatment providers in your area",
                null,
                "https://findtreatment.gov/",
                null,
                Arrays.asList(ResourceCategory.DRUG_ALCOHOL)
        ));

        // Drug Information Resources
        resources.add(new SupportResource(
                "NIDA Drug Information",
                "National Institute on Drug Abuse research and data",
                null,
                "Website",
                "US",
                "Online",
                "Evidence-based drug information and research",
                null,
                "https://nida.nih.gov/research-topics/commonly-used-drugs-charts",
                null,
                Arrays.asList(ResourceCategory.DRUG_ALCOHOL)
        ));

        resources.add(new SupportResource(
                "DanceSafe",
                "Drug safety and harm reduction community",
                null,
                "Website",
                "International",
                "Online",
                "Peer-led harm reduction at events and online",
                null,
                "https://dancesafe.org/",
                null,
                Arrays.asList(ResourceCategory.DRUG_ALCOHOL)
        ));

        resources.add(new SupportResource(
                "Erowid",
                "Comprehensive drug experience and safety database",
                null,
                "Website",
                "International",
                "Online",
                "Factual drug information and user experiences",
                null,
                "https://erowid.org/",
                null,
                Arrays.asList(ResourceCategory.DRUG_ALCOHOL)
        ));

        resources.add(new SupportResource(
                "PsychonautWiki",
                "Psychoactive substance information database",
                null,
                "Website",
                "International",
                "Online",
                "Detailed pharmacological and safety information",
                null,
                "https://psychonautwiki.org/",
                null,
                Arrays.asList(ResourceCategory.DRUG_ALCOHOL)
        ));

        // Recovery Support - Meeting Resources
        resources.add(new SupportResource(
                "Narcotics Anonymous",
                "Peer support for recovery from drug addiction",
                null,
                "Meeting Finder",
                "International",
                "Varies",
                "12-step program with meetings worldwide",
                null,
                null,
                "https://www.na.org/meetingsearch/",
                Arrays.asList(ResourceCategory.RECOVERY_MEETINGS)
        ));

        resources.add(new SupportResource(
                "Alcoholics Anonymous",
                "Peer support for recovery from alcohol addiction",
                null,
                "Meeting Finder",
                "International",
                "Varies",
                "12-step program with meetings worldwide",
                null,
                null,
                "https://www.aa.org/the-aa-meeting/",
                Arrays.asList(ResourceCategory.RECOVERY_MEETINGS)
        ));

        resources.add(new SupportResource(
                "SMART Recovery",
                "Self-management and recovery training",
                null,
                "Meeting Finder",
                "International",
                "Varies",
                "Science-based, non-12-step recovery approach",
                null,
                null,
                "https://www.smartrecovery.org/",
                Arrays.asList(ResourceCategory.RECOVERY_MEETINGS)
        ));

        // Family and Friend Support
        resources.add(new SupportResource(
                "Nar-Anon",
                "Support for families of those with addiction",
                null,
                "Meeting Finder",
                "International",
                "Varies",
                "Fellowship for friends and family members",
                null,
                null,
                "https://www.nar-anon.org/",
                Arrays.asList(ResourceCategory.FAMILY_FRIENDS)
        ));

        resources.add(new SupportResource(
                "Al-Anon",
                "Support for families of alcoholics",
                null,
                "Meeting Finder",
                "International",
                "Varies",
                "Fellowship for loved ones of people with alcohol addiction",
                null,
                null,
                "https://www.al-anon.org/",
                Arrays.asList(ResourceCategory.FAMILY_FRIENDS)
        ));

        return resources;
    }
}
