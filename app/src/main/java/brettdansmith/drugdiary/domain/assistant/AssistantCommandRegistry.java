package brettdansmith.drugdiary.domain.assistant;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import brettdansmith.drugdiary.assistant.AssistantContextBuilder;
import brettdansmith.drugdiary.assistant.AssistantPlaceholders;
import brettdansmith.drugdiary.data.diary.DiaryRepository;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.reference.DrugReferenceCacheStore;
import brettdansmith.drugdiary.data.reference.DrugReferencePrewarmer;
import brettdansmith.drugdiary.data.reference.DrugReferenceRepository;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.domain.medication.MedicationCatalog;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.model.diary.DiaryEntry;
import brettdansmith.drugdiary.model.medication.MedicationCategory;
import brettdansmith.drugdiary.model.medication.MedicationRecord;
import brettdansmith.drugdiary.network.ai.capabilities.AiCapabilityRegistry;
import brettdansmith.drugdiary.reference.DrugDatabaseRepository;
import brettdansmith.drugdiary.reference.DrugInteractionRepository;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.ui.medications.MedicationListImportParser;

public final class AssistantCommandRegistry {
    private static final String LOOKUP_REJECTED = "That does not look like a medication, substance, compound, or drug reference query.";
    private static final String PRIVACY_REJECTED = "This command requires medication context to be enabled in Assistant privacy settings.";

    public interface UiActions {
        void createNewChat(String title);
    }

    public interface ProgressCallback {
        void onProgress(String update);
    }

    private final Context appContext;
    private final UiActions actions;

    public AssistantCommandRegistry(Context context, UiActions actions) {
        this.appContext = context.getApplicationContext();
        this.actions = actions;
    }

    public boolean isAsyncCommand(String command) {
        String lower = normalize(command);
        return lower.startsWith("/pubchem ")
                || lower.startsWith("/rxnorm ")
                || lower.startsWith("/openfda ")
                || lower.startsWith("/chembl ")
                || lower.startsWith("/dailymed ")
                || lower.startsWith("/wikipedia ")
                || lower.startsWith("/drugdata ")
                || lower.startsWith("/interact ")
                || lower.startsWith("/drugcache warm ")
                || lower.equals("/drugcache clear")
                || lower.equals("/drugcache status");
    }

    public String handleCommand(String rawCommand) {
        String command = rawCommand.trim();
        if (!AssistantCommandParser.isKnownCommand(command)) {
            return "Unknown command. Use [[command:/help]] for available commands.";
        }
        String lower = normalize(command);

        if (isMedicationCommand(command) && !AppSettings.state(appContext).assistantMedicationContext) {
            return PRIVACY_REJECTED;
        }

        if (lower.startsWith("/help") || lower.startsWith("/commands") || lower.startsWith("/?")) {
            String topic = command.length() > commandName(command).length() + 1
                    ? command.substring(commandName(command).length() + 1).trim()
                    : "";
            return helpText(topic);
        }
        if (lower.startsWith("/context")) {
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            return AssistantContextBuilder.buildPlainText(appContext, data);
        }
        if (lower.startsWith("/settings")) {
            return settingsText();
        }
        if (lower.startsWith("/privacy")) {
            return privacyText();
        }
        if (lower.startsWith("/placeholders")) {
            return AssistantPlaceholders.helpText();
        }
        if (lower.startsWith("/newchat")) {
            String title = command.length() > 8 ? command.substring(8).trim() : "New chat";
            actions.createNewChat(title.isEmpty() ? "New chat" : title);
            return "Started a new chat: " + title;
        }
        if (lower.startsWith("/meds")) {
            return medicationsText(command);
        }
        if (lower.equals("/clearmeds")) {
            return clearMedications();
        }
        if (lower.startsWith("/addmed ")) {
            return addMedication(command.substring("/addmed ".length()).trim());
        }
        if (lower.startsWith("/importmeds")) {
            String payload = extractPayload(command);
            return importMedications(payload);
        }
        if (lower.startsWith("/updatemed ") || lower.startsWith("/editmed ")) {
            int split = command.indexOf(' ');
            return updateMedication(split < 0 ? "" : command.substring(split + 1).trim());
        }
        if (lower.startsWith("/removemed ") || lower.startsWith("/deletemed ") || lower.startsWith("/delmed ")) {
            int split = command.indexOf(' ');
            return removeMedication(split < 0 ? "" : command.substring(split + 1).trim());
        }
        if (lower.startsWith("/logs")) {
            return recentLogsText();
        }
        if (lower.startsWith("/sources")) {
            return sourcesText();
        }
        if (lower.startsWith("/reminders")) {
            return remindersText();
        }
        if (lower.startsWith("/reminder ")) {
            return addReminder(command.substring("/reminder ".length()).trim());
        }
        if (lower.startsWith("/alias ")) {
            return aliasText(command.substring("/alias ".length()).trim());
        }
        if (lower.startsWith("/dose ")) {
            return doseText(command.substring("/dose ".length()).trim());
        }
        if (lower.startsWith("/medinfo ")) {
            return medInfoText(command.substring("/medinfo ".length()).trim());
        }

        return "This command requires background lookup. Run it directly or allow assistant auto-run.";
    }

    public String handleAsyncCommand(String command, ProgressCallback callback) throws Exception {
        String lower = normalize(command);
        if (isMedicationCommand(command) && !AppSettings.state(appContext).assistantMedicationContext) {
            return PRIVACY_REJECTED;
        }

        if (lower.startsWith("/interact ")) {
            callback.onProgress("Checking interactions against local rules and public sources...");
            String payload = command.substring("/interact ".length()).trim();
            if ("saved".equalsIgnoreCase(payload)) {
                return DrugInteractionRepository.checkSaved(appContext);
            }
            List<String> entries = splitEntries(payload);
            return DrugInteractionRepository.checkMultiple(appContext, entries.toArray(new String[0]));
        }
        if (lower.startsWith("/drugdata ")) {
            String query = command.substring("/drugdata ".length()).trim();
            if (!brettdansmith.drugdiary.reference.DrugDatabaseClient.looksLikeDrugQuery(query)) return LOOKUP_REJECTED;
            callback.onProgress("Fetching aggregated drug data...");
            DrugReferenceRepository repository = new DrugReferenceRepository(appContext);
            return repository.summary(repository.lookupAll(query));
        }
        if (lower.startsWith("/pubchem ")) {
            String query = command.substring("/pubchem ".length()).trim();
            if (!brettdansmith.drugdiary.reference.DrugDatabaseClient.looksLikeDrugQuery(query)) return LOOKUP_REJECTED;
            callback.onProgress("Fetching PubChem payload...");
            return DrugDatabaseRepository.getAllSummary(appContext, query);
        }
        if (lower.startsWith("/rxnorm ")) {
            String query = command.substring("/rxnorm ".length()).trim();
            if (!brettdansmith.drugdiary.reference.DrugDatabaseClient.looksLikeDrugQuery(query)) return LOOKUP_REJECTED;
            callback.onProgress("Looking up RxNorm identity...");
            return DrugDatabaseRepository.getRxNormSummary(appContext, query);
        }
        if (lower.startsWith("/openfda ")) {
            String query = command.substring("/openfda ".length()).trim();
            if (!brettdansmith.drugdiary.reference.DrugDatabaseClient.looksLikeDrugQuery(query)) return LOOKUP_REJECTED;
            callback.onProgress("Reading FDA label snippets...");
            return DrugDatabaseRepository.getOpenFdaLabelSummary(appContext, query);
        }
        if (lower.startsWith("/chembl ")) {
            String query = command.substring("/chembl ".length()).trim();
            if (!brettdansmith.drugdiary.reference.DrugDatabaseClient.looksLikeDrugQuery(query)) return LOOKUP_REJECTED;
            callback.onProgress("Looking up ChEMBL pharmacology...");
            return DrugDatabaseRepository.getChemblSummary(appContext, query);
        }
        if (lower.startsWith("/dailymed ")) {
            String query = command.substring("/dailymed ".length()).trim();
            if (!brettdansmith.drugdiary.reference.DrugDatabaseClient.looksLikeDrugQuery(query)) return LOOKUP_REJECTED;
            callback.onProgress("Loading DailyMed label metadata...");
            DrugReferenceRepository repository = new DrugReferenceRepository(appContext);
            return repository.summary(repository.lookupDailyMed(query));
        }
        if (lower.startsWith("/wikipedia ")) {
            String query = command.substring("/wikipedia ".length()).trim();
            if (!brettdansmith.drugdiary.reference.DrugDatabaseClient.looksLikeDrugQuery(query)) return LOOKUP_REJECTED;
            callback.onProgress("Fetching Wikipedia summary...");
            return DrugDatabaseRepository.getWikipediaSummary(appContext, query);
        }
        if (lower.equals("/drugcache status")) {
            return new DrugReferenceCacheStore(appContext).status();
        }
        if (lower.equals("/drugcache clear")) {
            return new DrugReferenceRepository(appContext).clearCache();
        }
        if (lower.startsWith("/drugcache warm ")) {
            String query = command.substring("/drugcache warm ".length()).trim();
            callback.onProgress("Prewarming drug reference cache...");
            return DrugReferencePrewarmer.warmNow(appContext, query);
        }

        return "Unsupported async command.";
    }

    private boolean isMedicationCommand(String command) {
        String name = commandName(command);
        return name.equals("meds") || name.equals("addmed") || name.equals("updatemed") || name.equals("editmed")
                || name.equals("removemed") || name.equals("deletemed") || name.equals("delmed") || name.equals("clearmeds")
                || name.equals("importmeds")
                || name.equals("interact") || name.equals("drugdata")
                || name.equals("pubchem") || name.equals("rxnorm") || name.equals("openfda") || name.equals("chembl")
                || name.equals("dailymed") || name.equals("wikipedia") || name.equals("alias") || name.equals("dose");
    }

    private String addMedication(String payload) {
        try {
            String[] parts = payload.split("\\|");
            String name = parts.length > 0 ? parts[0].trim() : "";
            if (name.isEmpty()) return "Usage: [[command:/addmed <Name> | [Strength] | [Form] | [Groups comma separated]]]";

            String canonical = MedicationCatalog.canonicalNameFor(name);
            LinkedHashSet<MedicationCategory> groups = parts.length > 3
                    ? MedicationCategory.parseMany(parts[3].trim())
                    : new LinkedHashSet<>();
            if (groups.isEmpty()) groups.add(MedicationCategory.SAVED);
            MedicationCategory primary = groups.iterator().next();
            MedicationRecord med = new MedicationRecord(
                    "med_" + System.currentTimeMillis(),
                    name,
                    canonical,
                    parts.length > 1 ? parts[1].trim() : "",
                    parts.length > 2 ? parts[2].trim() : "",
                    "",
                    primary,
                    groups,
                    true,
                    false,
                    true,
                    false,
                    "Added via assistant command",
                    new brettdansmith.drugdiary.model.medication.MedicationSchedule("scheduled", "", 0),
                    new brettdansmith.drugdiary.model.medication.MedicationInventory(0, "units", 0, 0),
                    MedicationCatalog.aliasesFor(name),
                    System.currentTimeMillis(),
                    System.currentTimeMillis());

            MedicationRepository repository = new MedicationRepository(appContext);
            repository.upsertRecord(med);
            if (findMedicationById(repository.listRecords(), med.id) == null) {
                return "Tried to add '" + name + "', but verification failed. Use [[command:/meds]] to check your list.";
            }
            return "Added and verified '" + name + "' in your medication list.";
        } catch (JSONException e) {
            return "Failed to add medication: " + e.getMessage();
        }
    }

    private String medicationsText(String command) {
        String filter = command.length() > 5 ? command.substring(5).trim().toLowerCase(Locale.US) : "";
        List<MedicationRecord> meds = new MedicationRepository(appContext).listRecords();
        if (meds.isEmpty()) {
            return "You have no medications saved. Add one using [[command:/addmed <name>]].";
        }

        StringBuilder sb = new StringBuilder("### Medications\n");
        int shown = 0;
        for (MedicationRecord med : meds) {
            if (!matchesFilter(med, filter)) continue;
            sb.append("- **").append(med.name).append("**");
            if (!med.strength.isEmpty()) sb.append(" ").append(med.strength);
            if (!med.form.isEmpty()) sb.append(" (").append(med.form).append(")");
            sb.append(" - ").append(med.categoryLabels());
            if (med.favorite) sb.append(" - favourite");
            if (!med.active) sb.append(" - archived");
            if (med.prn) sb.append(" - PRN");
            sb.append("\n");
            shown++;
        }
        if (shown == 0) {
            return "No medications matched that filter.";
        }
        return sb.toString().trim();
    }

    private String updateMedication(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return "Usage: [[command:/updatemed <name|id> | field=value ...]]";
        }
        String[] parts = payload.split("\\|");
        String target = parts.length > 0 ? parts[0].trim() : "";
        if (target.isEmpty()) {
            return "Usage: [[command:/updatemed <name|id> | field=value ...]]";
        }

        MedicationRepository repository = new MedicationRepository(appContext);
        ResolveResult resolve = resolveMedication(repository, target);
        if (resolve.record == null) {
            return resolve.message;
        }

        Map<String, String> updates = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String section = parts[i] == null ? "" : parts[i].trim();
            if (section.isEmpty()) continue;
            int equals = section.indexOf('=');
            if (equals > 0 && equals < section.length() - 1) {
                String key = section.substring(0, equals).trim().toLowerCase(Locale.US);
                String value = section.substring(equals + 1).trim();
                if (!key.isEmpty()) updates.put(key, value);
            }
        }
        if (updates.isEmpty() && parts.length > 1) {
            if (parts.length > 1 && !parts[1].trim().isEmpty()) updates.put("strength", parts[1].trim());
            if (parts.length > 2 && !parts[2].trim().isEmpty()) updates.put("form", parts[2].trim());
            if (parts.length > 3 && !parts[3].trim().isEmpty()) updates.put("category", parts[3].trim());
            if (parts.length > 4 && !parts[4].trim().isEmpty()) updates.put("notes", parts[4].trim());
        }
        if (updates.isEmpty()) {
            return "No changes parsed. Example: [[command:/updatemed " + resolve.record.name + " | strength=25 mg | favorite=true]]";
        }

        MedicationRecord source = resolve.record;
        String name = source.name;
        String strength = source.strength;
        String form = source.form;
        String route = source.route;
        MedicationCategory category = source.category;
        LinkedHashSet<MedicationCategory> categories = new LinkedHashSet<>(source.categories);
        boolean active = source.active;
        boolean favorite = source.favorite;
        boolean saved = source.saved;
        boolean prn = source.prn;
        String notes = source.notes;
        List<String> applied = new ArrayList<>();
        List<String> invalid = new ArrayList<>();

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("name".equals(key)) {
                if (!value.trim().isEmpty()) {
                    name = value.trim();
                    applied.add("name");
                }
                continue;
            }
            if ("strength".equals(key) || "dose".equals(key)) {
                strength = value;
                applied.add("strength");
                continue;
            }
            if ("form".equals(key)) {
                form = value;
                applied.add("form");
                continue;
            }
            if ("route".equals(key)) {
                route = value;
                applied.add("route");
                continue;
            }
            if ("category".equals(key) || "group".equals(key)) {
                LinkedHashSet<MedicationCategory> parsed = MedicationCategory.parseMany(value);
                if (parsed.isEmpty()) parsed.add(MedicationCategory.from(value));
                categories = parsed;
                category = categories.iterator().next();
                applied.add("category");
                continue;
            }
            if ("notes".equals(key) || "note".equals(key)) {
                notes = value;
                applied.add("notes");
                continue;
            }
            if ("active".equals(key)) {
                Boolean parsed = parseBoolean(value);
                if (parsed == null) invalid.add(key + "=" + value); else {
                    active = parsed;
                    applied.add("active");
                }
                continue;
            }
            if ("favorite".equals(key) || "favourite".equals(key)) {
                Boolean parsed = parseBoolean(value);
                if (parsed == null) invalid.add(key + "=" + value); else {
                    favorite = parsed;
                    applied.add("favorite");
                }
                continue;
            }
            if ("saved".equals(key)) {
                Boolean parsed = parseBoolean(value);
                if (parsed == null) invalid.add(key + "=" + value); else {
                    saved = parsed;
                    applied.add("saved");
                }
                continue;
            }
            if ("prn".equals(key)) {
                Boolean parsed = parseBoolean(value);
                if (parsed == null) invalid.add(key + "=" + value); else {
                    prn = parsed;
                    applied.add("prn");
                }
                continue;
            }
            invalid.add(key + "=" + value);
        }

        if (applied.isEmpty()) {
            return "No valid fields were updated. Supported fields: name, strength, form, route, category/groups, notes, active, favorite, saved, prn.";
        }

        try {
            if (categories.contains(MedicationCategory.WISHLIST)) active = false;
            if (active) categories.add(MedicationCategory.ACTIVE); else categories.remove(MedicationCategory.ACTIVE);
            if (favorite) categories.add(MedicationCategory.FAVORITE); else categories.remove(MedicationCategory.FAVORITE);
            if (saved) categories.add(MedicationCategory.SAVED); else categories.remove(MedicationCategory.SAVED);
            MedicationRecord updated = new MedicationRecord(
                    source.id,
                    name,
                    MedicationCatalog.canonicalNameFor(name),
                    strength,
                    form,
                    route,
                    category,
                    categories,
                    active,
                    favorite,
                    saved,
                    prn,
                    notes,
                    source.schedule,
                    source.inventory,
                    source.aliases,
                    source.createdAt,
                    System.currentTimeMillis());
            repository.upsertRecord(updated);
            MedicationRecord verified = findMedicationById(repository.listRecords(), source.id);
            if (verified == null) {
                return "Update was attempted, but verification failed for '" + source.name + "'.";
            }
            if (!fieldsVerified(verified, updated, applied)) {
                return "Update for '" + source.name + "' saved partially. Re-check with [[command:/meds]].";
            }
        } catch (JSONException e) {
            return "Could not update medication: " + e.getMessage();
        }

        StringBuilder response = new StringBuilder("Updated **").append(name).append("**: ").append(String.join(", ", applied)).append(".");
        if (!invalid.isEmpty()) {
            response.append("\nIgnored fields: ").append(String.join(", ", invalid));
        }
        return response.toString();
    }

    private String removeMedication(String payload) {
        String target = payload == null ? "" : payload.trim();
        if (target.isEmpty()) return "Usage: [[command:/removemed <name|id>]]";

        MedicationRepository repository = new MedicationRepository(appContext);
        ResolveResult resolve = resolveMedication(repository, target);
        if (resolve.record == null) {
            return resolve.message;
        }

        try {
            repository.delete(resolve.record.id);
            if (findMedicationById(repository.listRecords(), resolve.record.id) != null) {
                return "Removal was attempted, but **" + resolve.record.name + "** is still present.";
            }
            return "Removed and verified **" + resolve.record.name + "** from your medication list.";
        } catch (JSONException e) {
            return "Could not remove medication: " + e.getMessage();
        }
    }

    private String clearMedications() {
        MedicationRepository repository = new MedicationRepository(appContext);
        List<MedicationRecord> all = repository.listRecords();
        if (all.isEmpty()) return "Medication list is already empty.";

        int deleted = 0;
        for (MedicationRecord record : all) {
            try {
                repository.delete(record.id);
                deleted++;
            } catch (JSONException ignored) {
            }
        }
        int remaining = repository.listRecords().size();
        if (remaining > 0) {
            return "Attempted to clear medications. Deleted " + deleted + ", but " + remaining + " remain.";
        }
        return "Cleared and verified " + deleted + " medication entr" + (deleted == 1 ? "y." : "ies.");
    }

    private String importMedications(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return "Usage: [[command:/importmeds <paste list with one item per line>]]";
        }
        MedicationListImportParser.Result parsed = MedicationListImportParser.parse(payload, false);
        if (parsed.records().isEmpty()) {
            return "No valid list entries were parsed for import.";
        }
        MedicationRepository repository = new MedicationRepository(appContext);
        int imported = 0;
        for (MedicationRecord record : parsed.records()) {
            try {
                repository.upsertRecord(record);
                imported++;
            } catch (Exception ignored) {
            }
        }
        return "Imported and verified " + imported + " entries. Skipped " + parsed.skipped() + ".";
    }

    private String extractPayload(String command) {
        int firstSpace = command.indexOf(' ');
        if (firstSpace > 0) {
            return command.substring(firstSpace + 1).trim();
        }
        int firstNewline = command.indexOf('\n');
        if (firstNewline > 0) {
            return command.substring(firstNewline + 1).trim();
        }
        return "";
    }

    private MedicationRecord findMedicationById(List<MedicationRecord> medications, String id) {
        if (medications == null || id == null || id.trim().isEmpty()) return null;
        for (MedicationRecord med : medications) {
            if (med != null && id.equals(med.id)) return med;
        }
        return null;
    }

    private boolean fieldsVerified(MedicationRecord actual, MedicationRecord expected, List<String> applied) {
        if (actual == null || expected == null || applied == null || applied.isEmpty()) return false;
        for (String field : applied) {
            if ("name".equals(field) && !actual.name.equals(expected.name)) return false;
            if ("strength".equals(field) && !actual.strength.equals(expected.strength)) return false;
            if ("form".equals(field) && !actual.form.equals(expected.form)) return false;
            if ("route".equals(field) && !actual.route.equals(expected.route)) return false;
            if ("category".equals(field) && !actual.categories.equals(expected.categories)) return false;
            if ("notes".equals(field) && !actual.notes.equals(expected.notes)) return false;
            if ("active".equals(field) && actual.active != expected.active) return false;
            if ("favorite".equals(field) && actual.favorite != expected.favorite) return false;
            if ("saved".equals(field) && actual.saved != expected.saved) return false;
            if ("prn".equals(field) && actual.prn != expected.prn) return false;
        }
        return true;
    }

    private ResolveResult resolveMedication(MedicationRepository repository, String query) {
        String clean = query == null ? "" : query.trim();
        if (clean.isEmpty()) {
            return new ResolveResult(null, "No medication target provided.");
        }

        List<MedicationRecord> all = repository.listRecords();
        if (all.isEmpty()) {
            return new ResolveResult(null, "You have no medications saved.");
        }

        for (MedicationRecord med : all) {
            if (med.id.equalsIgnoreCase(clean)
                    || med.name.equalsIgnoreCase(clean)
                    || med.canonicalName.equalsIgnoreCase(clean)) {
                return new ResolveResult(med, "");
            }
        }

        String normalized = clean.toLowerCase(Locale.US);
        List<MedicationRecord> matches = new ArrayList<>();
        for (MedicationRecord med : all) {
            if (med.name.toLowerCase(Locale.US).contains(normalized)
                    || med.canonicalName.toLowerCase(Locale.US).contains(normalized)) {
                matches.add(med);
            }
        }

        if (matches.isEmpty()) {
            return new ResolveResult(null, "No medication matched '" + clean + "'. Use [[command:/meds]] to list current entries.");
        }
        if (matches.size() > 1) {
            return new ResolveResult(null, "Multiple medications matched '" + clean + "': " + summarizeMatches(matches) + ". Use a more specific name.");
        }
        return new ResolveResult(matches.get(0), "");
    }

    private String summarizeMatches(List<MedicationRecord> matches) {
        StringBuilder out = new StringBuilder();
        int limit = Math.min(matches.size(), 5);
        for (int i = 0; i < limit; i++) {
            if (i > 0) out.append(", ");
            out.append(matches.get(i).name);
        }
        if (matches.size() > limit) out.append(" +").append(matches.size() - limit).append(" more");
        return out.toString();
    }

    private Boolean parseBoolean(String value) {
        if (value == null) return null;
        String clean = value.trim().toLowerCase(Locale.US);
        if (clean.isEmpty()) return null;
        if ("true".equals(clean) || "yes".equals(clean) || "y".equals(clean) || "on".equals(clean) || "1".equals(clean)) {
            return true;
        }
        if ("false".equals(clean) || "no".equals(clean) || "n".equals(clean) || "off".equals(clean) || "0".equals(clean)) {
            return false;
        }
        return null;
    }

    private static final class ResolveResult {
        final MedicationRecord record;
        final String message;

        ResolveResult(MedicationRecord record, String message) {
            this.record = record;
            this.message = message == null ? "" : message;
        }
    }

    private boolean matchesFilter(MedicationRecord med, String filter) {
        if (filter == null || filter.isEmpty() || "all".equals(filter)) return true;
        if ("favorites".equals(filter) || "favourites".equals(filter)) return med.favorite;
        if ("active".equals(filter)) return med.active;
        if ("inactive".equals(filter) || "archived".equals(filter)) return !med.active;
        if ("prn".equals(filter)) return med.prn;
        return med.name.toLowerCase(Locale.US).contains(filter)
                || med.canonicalName.toLowerCase(Locale.US).contains(filter)
                || med.categoryLabels().toLowerCase(Locale.US).contains(filter);
    }

    private String aliasText(String payload) {
        if (payload.isEmpty()) return "Usage: [[command:/alias <name>]]";
        List<String> candidates = MedicationQueryResolver.candidatesFor(appContext, payload);
        StringBuilder sb = new StringBuilder("Alias resolution for **").append(payload).append("**\n");
        for (int i = 0; i < Math.min(12, candidates.size()); i++) {
            sb.append("- ").append(candidates.get(i)).append("\n");
        }
        if (candidates.isEmpty()) sb.append("- No aliases resolved");
        return sb.toString().trim();
    }

    private String doseText(String payload) {
        if (payload.isEmpty()) return "Usage: [[command:/dose <name>]]";
        List<String> doses = MedicationCatalog.doseSuggestions(payload);
        return "Dose suggestions for **" + payload + "**\n- " + String.join("\n- ", doses);
    }

    private String medInfoText(String payload) {
        if (payload.isEmpty()) return "Usage: [[command:/medinfo <name>]]";
        String canonical = MedicationCatalog.canonicalNameFor(payload);
        String category = MedicationCatalog.categoryFor(payload);
        List<String> notes = MedicationCatalog.noteSuggestions(payload);
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(canonical).append("\n");
        sb.append("- Category: ").append(category).append("\n");
        sb.append("- Doses: ").append(String.join(", ", MedicationCatalog.doseSuggestions(payload))).append("\n");
        sb.append("- Notes:\n");
        for (int i = 0; i < Math.min(6, notes.size()); i++) {
            sb.append("  - ").append(notes.get(i)).append("\n");
        }
        sb.append("- For source-backed facts, run [[command:/drugdata ").append(canonical).append("]]");
        return sb.toString().trim();
    }

    private String recentLogsText() {
        List<DiaryEntry> entries = new DiaryRepository(appContext).listEntries();
        if (entries.isEmpty()) return "No diary entries yet.";
        StringBuilder sb = new StringBuilder("### Recent diary entries\n");
        for (int i = 0; i < Math.min(8, entries.size()); i++) {
            DiaryEntry entry = entries.get(i);
            sb.append("- ").append(entry.title);
            if (!entry.notes.isEmpty()) sb.append(": ").append(entry.notes);
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String sourcesText() {
        return "Reference priority\n"
                + "- RxNorm / RxNav\n"
                + "- openFDA labels / events\n"
                + "- ChEMBL\n"
                + "- PubChem\n"
                + "- DailyMed\n"
                + "- Wikipedia fallback\n"
                + "AI explanations should be grounded in these sources when available.";
    }

    private String remindersText() {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONArray reminders = data.optJSONArray("reminders");
        if (reminders == null || reminders.length() == 0) return "No reminders saved.";
        StringBuilder sb = new StringBuilder("### Reminders\n");
        for (int i = 0; i < reminders.length(); i++) {
            JSONObject item = reminders.optJSONObject(i);
            if (item == null) continue;
            sb.append("- ").append(item.optString("title", "Reminder"));
            if (!item.optBoolean("enabled", true)) sb.append(" (disabled)");
            String notes = item.optString("notes", "");
            if (!notes.isEmpty()) sb.append(": ").append(notes);
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String addReminder(String payload) {
        if (payload.trim().isEmpty()) return "Usage: [[command:/reminder <title> | [notes]]]";
        String[] parts = payload.split("\\|", 2);
        String title = parts[0].trim();
        String notes = parts.length > 1 ? parts[1].trim() : "";
        try {
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            JSONArray reminders = data.optJSONArray("reminders");
            if (reminders == null) reminders = new JSONArray();
            reminders.put(new JSONObject()
                    .put("id", "rem_" + System.currentTimeMillis())
                    .put("title", title)
                    .put("notes", notes)
                    .put("enabled", true)
                    .put("created_at", System.currentTimeMillis()));
            data.put("reminders", reminders);
            EncryptedProfileStore.saveProfileData(appContext, data);
            return "Reminder added: " + title;
        } catch (JSONException e) {
            return "Could not save reminder.";
        }
    }

    private String settingsText() {
        SettingsRepository repository = new SettingsRepository(appContext);
        SettingsState state = repository.getState();
        ProviderSettings provider = repository.getProviderSettings(state.assistantProvider);
        brettdansmith.drugdiary.model.ai.ProviderCapabilities caps = AiCapabilityRegistry.forProvider(state.assistantProvider);

        StringBuilder sb = new StringBuilder("### Effective assistant settings\n");
        sb.append("- Provider: ").append(state.assistantProvider.displayName()).append("\n");
        sb.append("- Model: ").append(provider.model).append("\n");
        sb.append("- Enabled: ").append(provider.enabled).append("\n");
        sb.append("- Streaming: ").append(provider.allowStreaming).append(" (capability: ").append(caps.streaming).append(")\n");
        sb.append("- Web search: ").append(repository.isAiWebSearchEnabled() && provider.allowWebSearch).append(" (capability: ").append(caps.webSearch).append(")\n");
        sb.append("- Citations required: ").append(repository.isAiCitationsRequired() && provider.requireCitations).append(" (capability: ").append(caps.citations).append(")\n");
        sb.append("- Timeout seconds: ").append(provider.timeoutSeconds).append("\n");
        sb.append("- Max retries: ").append(provider.maxRetries).append("\n");
        sb.append("- Fallback enabled: ").append(repository.isAiFallbackEnabled()).append("\n");
        if (repository.getAiFallbackOrder() != null && !repository.getAiFallbackOrder().trim().isEmpty()) {
            sb.append("- Fallback order: ").append(repository.getAiFallbackOrder().trim()).append("\n");
        }
        sb.append("- Attachments supported: ").append(caps.supportsAttachments).append("\n");
        sb.append("- Usage metadata support: ").append(caps.supportsUsageMetadata);
        return sb.toString();
    }

    private String privacyText() {
        SettingsState state = AppSettings.state(appContext);
        StringBuilder sb = new StringBuilder("### Assistant privacy\n");
        sb.append("- Assistant memory: ").append(state.assistantMemory).append("\n");
        sb.append("- Profile context: ").append(state.assistantProfileContext).append("\n");
        sb.append("- Medication context: ").append(state.assistantMedicationContext).append("\n");
        sb.append("- Diary/log context: ").append(state.assistantLogContext).append("\n");
        sb.append("- Private mode: ").append(AppSettings.privateModeEnabled(appContext)).append("\n");
        sb.append("- Dashboard sensitive hidden: ").append(AppSettings.hideDashboardSensitive(appContext));
        return sb.toString();
    }

    private List<String> splitEntries(String raw) {
        List<String> entries = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return entries;
        String normalized = raw.replace("\n", "|").replace(" and ", "|");
        String[] parts = normalized.split("\\||,|;");
        for (String part : parts) {
            String clean = part == null ? "" : part.trim();
            if (clean.isEmpty()) continue;
            if (!entries.contains(clean)) entries.add(clean);
        }
        return entries;
    }

    private String normalize(String command) {
        return command == null ? "" : command.trim().toLowerCase(Locale.US);
    }

    private String commandName(String command) {
        String body = command.startsWith("//") ? command.substring(2) : command.substring(1);
        return body.split("\\s+", 2)[0].toLowerCase(Locale.US);
    }

    private String helpText(String topic) {
        if ("meds".equalsIgnoreCase(topic)) {
            return "### /meds\n- [[command:/meds]]\n- [[command:/meds favorites]]\n- [[command:/meds active]]\n- [[command:/meds prn]]\n"
                    + "- [[command:/updatemed <name|id> | field=value ...]]\n"
                    + "- [[command:/removemed <name|id>]]\n"
                    + "- [[command:/clearmeds]]\n"
                    + "- [[command:/importmeds <paste list>]]";
        }
        if ("drugdata".equalsIgnoreCase(topic)) {
            return "### /drugdata\nSource-backed aggregate lookup from RxNorm, openFDA, ChEMBL, PubChem, DailyMed, and Wikipedia fallback.";
        }
        return "### Available commands\n"
                + "- [[command:/help [topic]]]\n"
                + "- [[command:/context]]\n"
                + "- [[command:/settings]]\n"
                + "- [[command:/privacy]]\n"
                + "- [[command:/meds [filter]]]\n"
                + "- [[command:/addmed <name> | [strength] | [form] | [groups comma separated]]]\n"
                + "- [[command:/updatemed <name|id> | field=value ...]] or [[command:/editmed <name|id> | ...]]\n"
                + "- [[command:/removemed <name|id>]] or [[command:/deletemed <name|id>]]\n"
                + "- [[command:/clearmeds]]\n"
                + "- [[command:/importmeds <paste list>]]\n"
                + "- [[command:/logs]]\n"
                + "- [[command:/sources]]\n"
                + "- [[command:/reminders]]\n"
                + "- [[command:/reminder <title> | [notes]]]\n"
                + "- [[command:/alias <name>]]\n"
                + "- [[command:/dose <name>]]\n"
                + "- [[command:/medinfo <name>]]\n"
                + "- [[command:/interact <a|b|...>]] or [[command:/interact saved]]\n"
                + "- [[command:/drugdata <name>]]\n"
                + "- [[command:/pubchem <name>]], [[command:/rxnorm <name>]], [[command:/openfda <name>]], [[command:/chembl <name>]], [[command:/dailymed <name>]], [[command:/wikipedia <name>]]\n"
                + "- [[command:/drugcache status]], [[command:/drugcache clear]], [[command:/drugcache warm <mode|name>]]\n"
                + "- [[command:/newchat [title]]]\n"
                + "- [[command:/placeholders]]";
    }
}

