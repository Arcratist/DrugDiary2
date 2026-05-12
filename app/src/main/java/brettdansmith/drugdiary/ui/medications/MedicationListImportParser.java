package brettdansmith.drugdiary.ui.medications;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brettdansmith.drugdiary.domain.medication.MedicationCatalog;
import brettdansmith.drugdiary.model.medication.MedicationCategory;
import brettdansmith.drugdiary.model.medication.MedicationInventory;
import brettdansmith.drugdiary.model.medication.MedicationRecord;
import brettdansmith.drugdiary.model.medication.MedicationSchedule;

public final class MedicationListImportParser {
    private static final Pattern DETAILS_SPLIT = Pattern.compile("\\s+[\\u2014\\u2013-]\\s+");
    private static final Pattern STRENGTH = Pattern.compile("([0-9]+(?:\\.[0-9]+)?\\s*(?:mcg|µg|mg|g|ml|mL|iu|IU|units?))");

    private MedicationListImportParser() {
    }

    public static Result parse(String raw, boolean activeOverride) {
        List<MedicationRecord> records = new ArrayList<>();
        int skipped = 0;
        if (raw == null || raw.trim().isEmpty()) return new Result(records, skipped);

        String[] lines = raw.split("\\r?\\n");
        LinkedHashSet<MedicationCategory> currentGroups = new LinkedHashSet<>();
        currentGroups.add(MedicationCategory.SAVED);

        for (String line : lines) {
            String clean = line == null ? "" : line.trim();
            if (clean.isEmpty()) continue;

            LinkedHashSet<MedicationCategory> heading = resolveHeading(clean);
            if (!heading.isEmpty()) {
                currentGroups = heading;
                continue;
            }

            clean = stripBullet(clean);
            if (clean.isEmpty()) continue;

            String[] parts = DETAILS_SPLIT.split(clean, 2);
            String left = parts.length > 0 ? parts[0].trim() : "";
            String right = parts.length > 1 ? parts[1].trim() : "";
            if (left.isEmpty()) {
                skipped++;
                continue;
            }

            String name = normalizeName(left);
            if (name.isEmpty()) {
                skipped++;
                continue;
            }

            String strength = parseStrength(right);
            String form = parseForm(right);
            String route = parseRoute(right);
            String notes = buildNotes(right, left);
            LinkedHashSet<MedicationCategory> groups = new LinkedHashSet<>(currentGroups);
            MedicationCategory primary = groups.iterator().next();
            boolean active = activeOverride && !groups.contains(MedicationCategory.WISHLIST) && !groups.contains(MedicationCategory.ARCHIVED);
            if (groups.contains(MedicationCategory.HAVE_ACCESS) || groups.contains(MedicationCategory.WISHLIST)) {
                active = false;
            }

            MedicationRecord record = new MedicationRecord(
                    "",
                    name,
                    MedicationCatalog.canonicalNameFor(name),
                    strength,
                    form,
                    route,
                    primary,
                    groups,
                    active,
                    false,
                    true,
                    false,
                    notes,
                    new MedicationSchedule("scheduled", "", 0),
                    new MedicationInventory(0, "units", 0, 0),
                    MedicationCatalog.aliasesFor(name),
                    System.currentTimeMillis(),
                    System.currentTimeMillis());
            records.add(record);
        }

        return new Result(records, skipped);
    }

    private static LinkedHashSet<MedicationCategory> resolveHeading(String line) {
        LinkedHashSet<MedicationCategory> groups = new LinkedHashSet<>();
        String clean = line.toLowerCase(Locale.US).replace(":", "").replace("•", "").trim();
        if (clean.equals("medical")) groups.add(MedicationCategory.MEDICAL);
        if (clean.equals("supplements") || clean.equals("supplement")) groups.add(MedicationCategory.SUPPLEMENT);
        if (clean.equals("recreational")) groups.add(MedicationCategory.RECREATIONAL);
        if (clean.contains("have access")) groups.add(MedicationCategory.HAVE_ACCESS);
        if (clean.contains("yet to acquire") || clean.contains("still relevant") || clean.contains("wishlist")) {
            groups.add(MedicationCategory.WISHLIST);
        }
        if (clean.contains("archived") || clean.contains("inactive")) groups.add(MedicationCategory.ARCHIVED);
        if (clean.contains("active")) groups.add(MedicationCategory.ACTIVE);
        if (clean.contains("saved") || clean.contains("favorites") || clean.contains("favourites")) groups.add(MedicationCategory.SAVED);
        if (groups.contains(MedicationCategory.WISHLIST)) groups.remove(MedicationCategory.HAVE_ACCESS);
        return groups;
    }

    private static String stripBullet(String line) {
        String clean = line;
        if (clean.startsWith("•")) clean = clean.substring(1).trim();
        if (clean.startsWith("-")) clean = clean.substring(1).trim();
        if (clean.startsWith("*")) clean = clean.substring(1).trim();
        return clean;
    }

    private static String normalizeName(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) return "";
        return clean;
    }

    private static String parseStrength(String details) {
        if (details == null) return "";
        Matcher matcher = STRENGTH.matcher(details);
        if (!matcher.find()) return "";
        return matcher.group(1).replaceAll("\\s+", " ").trim().replace("mL", "ml").replace("µg", "mcg");
    }

    private static String parseForm(String details) {
        if (details == null) return "";
        String lower = details.toLowerCase(Locale.US);
        if (lower.contains("tablet")) return "Tablet";
        if (lower.contains("capsule")) return "Capsule";
        if (lower.contains("liquid")) return "Liquid";
        if (lower.contains("powder")) return "Powder";
        if (lower.contains("flower")) return "Flower";
        if (lower.contains("tabs")) return "Tabs";
        if (lower.contains("bottle")) return "Bottle";
        if (lower.contains("juice")) return "Liquid";
        return "";
    }

    private static String parseRoute(String details) {
        if (details == null) return "";
        String lower = details.toLowerCase(Locale.US);
        if (lower.contains("vape") || lower.contains("smoke") || lower.contains("flower")) return "Inhaled";
        if (lower.contains("paper tabs")) return "Sublingual";
        if (lower.contains("powder") || lower.contains("tablet") || lower.contains("capsule") || lower.contains("juice")) return "Oral";
        return "";
    }

    private static String buildNotes(String details, String left) {
        StringBuilder notes = new StringBuilder();
        if (left.contains("(") && left.contains(")")) {
            int start = left.indexOf('(');
            int end = left.indexOf(')', start + 1);
            if (start >= 0 && end > start) {
                String alias = left.substring(start + 1, end).trim();
                if (!alias.isEmpty()) notes.append("Alias: ").append(alias);
            }
        }
        if (details != null && !details.trim().isEmpty()) {
            String extra = details.trim();
            if (notes.length() > 0) notes.append(" • ");
            notes.append(extra);
        }
        return notes.toString();
    }

    public static final class Result {
        final List<MedicationRecord> records;
        final int skipped;

        Result(List<MedicationRecord> records, int skipped) {
            this.records = records == null ? new ArrayList<>() : records;
            this.skipped = skipped;
        }

        public List<MedicationRecord> records() {
            return records;
        }

        public int skipped() {
            return skipped;
        }
    }
}
