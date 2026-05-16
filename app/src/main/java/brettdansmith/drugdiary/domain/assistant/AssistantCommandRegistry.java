package brettdansmith.drugdiary.domain.assistant;

import android.content.Context;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import brettdansmith.drugdiary.assistant.AssistantContextBuilder;
import brettdansmith.drugdiary.data.diary.DiaryRepository;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.reference.DrugReferenceRepository;
import brettdansmith.drugdiary.domain.medication.MedicationCatalog;
import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;
import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;
import brettdansmith.drugdiary.reference.DrugInteractionRepository;
import brettdansmith.drugdiary.reference.DrugDatabaseRepository;

public final class AssistantCommandRegistry {
    public enum PayloadMode { NONE, OPTIONAL, REQUIRED }

    public static final class CommandSpec {
        public final String name;
        public final List<String> aliases;
        public final PayloadMode payloadMode;
        public final String usage;
        public final String usageCases;
        public final String outputFormat;
        public final String aiContextHint;
        public final String specialEdgeCase;
        public final boolean showInAutocomplete;
        public final boolean supportsPayloadAutofill;

        CommandSpec(String name, List<String> aliases, PayloadMode payloadMode, String usage,
                    String usageCases, String outputFormat, String aiContextHint,
                    String specialEdgeCase, boolean showInAutocomplete, boolean supportsPayloadAutofill) {
            this.name = name;
            this.aliases = aliases;
            this.payloadMode = payloadMode;
            this.usage = usage;
            this.usageCases = usageCases;
            this.outputFormat = outputFormat;
            this.aiContextHint = aiContextHint;
            this.specialEdgeCase = specialEdgeCase;
            this.showInAutocomplete = showInAutocomplete;
            this.supportsPayloadAutofill = supportsPayloadAutofill;
        }
    }

    public interface UiActions {
        void createNewChat(String title);
    }

    private static final List<CommandSpec> COMMAND_SPECS = buildSpecs();
    private static final Map<String, CommandSpec> NAME_TO_SPEC = buildSpecMap(COMMAND_SPECS);

    private final Context appContext;
    private final UiActions actions;

    public AssistantCommandRegistry(Context context) {
        this(context, null);
    }

    public AssistantCommandRegistry(Context context, UiActions actions) {
        this.appContext = context.getApplicationContext();
        this.actions = actions;
    }

    public String executeUnified(String input) {
        if (input == null || input.trim().isEmpty()) return "";

        String cmd = input.trim();
        if (cmd.startsWith("/")) {
            return handleCommand(cmd);
        }

        return "";
    }

    public List<String> getAvailableCommands() {
        List<String> commands = new ArrayList<>();
        for (CommandSpec spec : COMMAND_SPECS) {
            if (spec.showInAutocomplete) commands.add("/" + spec.name);
        }
        return commands;
    }

    public static List<CommandSpec> allSpecs() {
        return COMMAND_SPECS;
    }

    public static CommandSpec specFor(String rawName) {
        if (rawName == null) return null;
        return NAME_TO_SPEC.get(rawName.toLowerCase(Locale.US));
    }

    public static String aiUsageLine() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COMMAND_SPECS.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("/").append(COMMAND_SPECS.get(i).name);
        }
        return sb.toString();
    }

    public static List<CommandSpec> matchingSpecs(String typedSlashToken) {
        String token = typedSlashToken == null ? "" : typedSlashToken.trim().toLowerCase(Locale.US);
        if (token.startsWith("/")) token = token.substring(1);
        List<CommandSpec> matches = new ArrayList<>();
        if (token.isEmpty()) {
            for (CommandSpec spec : COMMAND_SPECS) {
                if (spec.showInAutocomplete) matches.add(spec);
            }
            return matches;
        }
        for (CommandSpec spec : COMMAND_SPECS) {
            if (!spec.showInAutocomplete) continue;
            if (spec.name.startsWith(token)) {
                matches.add(spec);
                continue;
            }
            for (String alias : spec.aliases) {
                if (alias.startsWith(token)) {
                    matches.add(spec);
                    break;
                }
            }
        }
        return matches;
    }

    public static String toolCatalogText() {
        StringBuilder sb = new StringBuilder("TOOL_CATALOG:\n");
        for (CommandSpec spec : COMMAND_SPECS) {
            sb.append("- ").append(spec.usage).append(": ").append(spec.usageCases).append("\n");
            sb.append("  Output: ").append(spec.outputFormat).append("\n");
            sb.append("  AI Context: ").append(spec.aiContextHint).append("\n");
            sb.append("  Autofill: ").append(spec.supportsPayloadAutofill ? "Medication/query candidates supported." : "No payload autofill.").append("\n");
            if (!spec.specialEdgeCase.isEmpty()) {
                sb.append("  Edge Case: ").append(spec.specialEdgeCase).append("\n");
            }
        }
        sb.append("\nTo use a tool, output exactly: [[execute:/command_name args]]\n");
        return sb.toString();
    }

    public static String compactToolCatalogText() {
        StringBuilder sb = new StringBuilder("TOOL_CATALOG_COMPACT:\n");
        for (CommandSpec spec : COMMAND_SPECS) {
            sb.append("- ").append(spec.usage).append(": ").append(spec.usageCases).append("\n");
        }
        sb.append("Execution format: [[execute:/command_name args]]\n");
        return sb.toString();
    }

    public static String contextToolCatalogText() {
        StringBuilder sb = new StringBuilder("TOOL_CATALOG_CONTEXT:\n");
        for (CommandSpec spec : COMMAND_SPECS) {
            sb.append("- /").append(spec.name);
            if (!spec.aliases.isEmpty()) {
                sb.append(" aliases=").append(spec.aliases);
            }
            sb.append("; payload=").append(spec.payloadMode.name().toLowerCase(Locale.US));
            sb.append("; usage=").append(spec.usage);
            sb.append("; output=").append(spec.outputFormat);
            sb.append("; hint=").append(spec.aiContextHint);
            if (!spec.specialEdgeCase.isEmpty()) {
                sb.append("; edge=").append(spec.specialEdgeCase);
            }
            sb.append("\n");
        }
        sb.append("Execution format: [[execute:/command_name args]]\n");
        return sb.toString();
    }

    private String handleCommand(String command) {
        String lower = command.toLowerCase(Locale.US).trim();
        String payload = lower.contains(" ") ? command.substring(command.indexOf(" ")).trim() : "";
        String name = lower.substring(1);
        int split = name.indexOf(' ');
        if (split >= 0) name = name.substring(0, split);
        CommandSpec spec = specFor(name);
        if (spec == null) return "Command not recognized: " + command;

        if ("meds".equals(spec.name)) {
            return listMedications();
        } else if ("summary".equals(spec.name)) {
            return buildCurrentSummary();
        } else if ("addmed".equals(spec.name)) {
            return "Command NYI: /addmed requires complex parsing. Use the UI for now.";
        } else if ("interact".equals(spec.name)) {
            return checkInteractions(payload);
        } else if ("drugdata".equals(spec.name)) {
            return lookupDrugData(payload);
        } else if ("pubchem".equals(spec.name)) {
            return lookupSource(payload, "pubchem");
        } else if ("rxnorm".equals(spec.name)) {
            return lookupSource(payload, "rxnorm");
        } else if ("openfda".equals(spec.name)) {
            return lookupSource(payload, "openfda");
        } else if ("chembl".equals(spec.name)) {
            return lookupSource(payload, "chembl");
        } else if ("dailymed".equals(spec.name)) {
            return lookupSource(payload, "dailymed");
        } else if ("wikipedia".equals(spec.name)) {
            return lookupSource(payload, "wikipedia");
        } else if ("logs".equals(spec.name)) {
            return listRecentLogs();
        } else if ("context".equals(spec.name)) {
            return buildCurrentContext();
        } else if ("newchat".equals(spec.name)) {
            if (actions != null) {
                String title = payload.isEmpty() ? "New Chat" : payload;
                actions.createNewChat(title);
                return "Started new chat: " + title;
            }
            return "New chat command not supported in this context.";
        } else if ("help".equals(spec.name) || "commands".equals(spec.name) || "?".equals(spec.name)) {
            return toolCatalogText();
        }
        return "Command registered but not yet implemented: " + spec.usage;
    }

    private String lookupSource(String query, String source) {
        try {
            if (query.isEmpty()) return "Usage: /" + source + " <medication name>";
            switch (source) {
                case "pubchem": return DrugDatabaseRepository.getAllSummary(appContext, query);
                case "rxnorm": return DrugDatabaseRepository.getRxNormSummary(appContext, query);
                case "openfda": return DrugDatabaseRepository.getOpenFdaLabelSummary(appContext, query);
                case "chembl": return DrugDatabaseRepository.getChemblSummary(appContext, query);
                case "wikipedia": return DrugDatabaseRepository.getWikipediaSummary(appContext, query);
                case "dailymed": 
                    DrugReferenceRepository repo = new DrugReferenceRepository(appContext);
                    return repo.summary(repo.lookupDailyMed(query));
                default: return "Unknown source: " + source;
            }
        } catch (Exception e) {
            return "Error looking up " + source + ": " + e.getMessage();
        }
    }

    private String listMedications() {
        try {
            List<MedicationRecord> meds = new MedicationRepository(appContext).listRecords();
            if (meds.isEmpty()) return "No medications saved.";
            StringBuilder sb = new StringBuilder("Your Medications:\n");
            for (MedicationRecord med : meds) {
                sb.append("- ").append(med.name);
                if (!med.strength.isEmpty()) sb.append(" (").append(med.strength).append(")");
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error retrieving medications: " + e.getMessage();
        }
    }

    private String checkInteractions(String drugs) {
        try {
            if (drugs.isEmpty()) return "Usage: /interact <drug1>, <drug2>...";
            String[] parts = drugs.split("[,|;]");
            return DrugInteractionRepository.checkMultiple(appContext, parts);
        } catch (Exception e) {
            return "Error checking interactions: " + e.getMessage();
        }
    }

    private String lookupDrugData(String name) {
        try {
            if (name.isEmpty()) return "Usage: /drugdata <medication name>";
            DrugReferenceRepository repo = new DrugReferenceRepository(appContext);
            return repo.summary(repo.lookupAll(name));
        } catch (Exception e) {
            return "Error looking up drug data: " + e.getMessage();
        }
    }

    private String listRecentLogs() {
        try {
            List<DiaryEntry> entries = new DiaryRepository(appContext).listEntries();
            if (entries.isEmpty()) return "No diary entries found.";
            StringBuilder sb = new StringBuilder("Recent Logs:\n");
            for (int i = 0; i < Math.min(5, entries.size()); i++) {
                DiaryEntry e = entries.get(i);
                sb.append("- ").append(e.title).append(": ").append(e.notes).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error retrieving logs: " + e.getMessage();
        }
    }

    private String buildCurrentSummary() {
        try {
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            // Combine profile name, age, count of meds, and last log
            JSONObject profile = data.optJSONObject("profile");
            String name = profile != null ? profile.optString("name", "User") : "User";
            int meds = new MedicationRepository(appContext).listRecords().size();
            List<DiaryEntry> logs = new DiaryRepository(appContext).listEntries();
            String lastLog = logs.isEmpty() ? "None" : logs.get(0).title;
            
            return String.format(java.util.Locale.US, "Health Summary for %s:\n- Medications: %d active\n- Last Entry: %s\n- Status: Profile context attached.",
                    name, meds, lastLog);
        } catch (Exception e) {
            return "Error building summary: " + e.getMessage();
        }
    }

    private String buildCurrentContext() {
        try {
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            return AssistantContextBuilder.buildPlainText(appContext, data);
        } catch (Exception e) {
            return "Error building context: " + e.getMessage();
        }
    }

    private static List<CommandSpec> buildSpecs() {
        List<CommandSpec> specs = new ArrayList<>();
        specs.add(spec("help", aliases("commands", "?"), PayloadMode.OPTIONAL, "/help [command]",
                "List commands and optional per-command quick help.",
                "Formatted tool catalog with usage, output shape, and edge cases.",
                "No extra context required.", "When payload is unknown, return full catalog.", true, true));
        specs.add(spec("meds", aliases(), PayloadMode.OPTIONAL, "/meds [filter]",
                "List saved medications/substances.",
                "Bullet list of saved medication names and strengths.",
                "Reads medication records from encrypted local store.",
                "Optional filter can be ignored until implemented.", true, false));
        specs.add(spec("summary", aliases(), PayloadMode.NONE, "/summary",
                "Build quick profile/med/log summary.",
                "Compact status lines for name, medication count, and last log.",
                "Reads profile + medication + diary repositories.",
                "If profile missing, fallback to generic user label.", true, false));
        specs.add(spec("sources", aliases(), PayloadMode.OPTIONAL, "/sources [name]",
                "List or query data sources.", "Source catalog or source-specific details.",
                "Used by AI to choose lookup command families.",
                "Fallback for informational prompts.", false, false));
        specs.add(spec("interact", aliases(), PayloadMode.REQUIRED, "/interact <drug1>, <drug2>, ...",
                "Check interactions between multiple substances.",
                "Interaction report from local interaction repository.",
                "Uses candidate matching and interaction checks.",
                "Splits payload by comma, pipe, or semicolon.", true, true));
        specs.add(spec("drugdata", aliases(), PayloadMode.REQUIRED, "/drugdata <name>",
                "Fetch aggregated drug reference data.",
                "Combined summary across supported data sources.",
                "Runs multi-source lookup using local repository wrappers.",
                "Requires non-empty name payload.", true, true));
        specs.add(spec("pubchem", aliases(), PayloadMode.REQUIRED, "/pubchem <name>",
                "Fetch PubChem-focused summary.", "Source-specific text summary.",
                "Uses DrugDatabaseRepository PubChem path.",
                "Requires non-empty name payload.", false, true));
        specs.add(spec("rxnorm", aliases(), PayloadMode.REQUIRED, "/rxnorm <name>",
                "Fetch RxNorm-focused summary.", "Source-specific text summary.",
                "Uses DrugDatabaseRepository RxNorm path.",
                "Requires non-empty name payload.", false, true));
        specs.add(spec("openfda", aliases(), PayloadMode.REQUIRED, "/openfda <name>",
                "Fetch openFDA-focused summary.", "Source-specific text summary.",
                "Uses DrugDatabaseRepository openFDA label path.",
                "Requires non-empty name payload.", false, true));
        specs.add(spec("chembl", aliases(), PayloadMode.REQUIRED, "/chembl <name>",
                "Fetch ChEMBL-focused summary.", "Source-specific text summary.",
                "Uses DrugDatabaseRepository ChEMBL path.",
                "Requires non-empty name payload.", false, true));
        specs.add(spec("dailymed", aliases(), PayloadMode.REQUIRED, "/dailymed <name>",
                "Fetch DailyMed-focused summary.", "Source-specific text summary.",
                "Uses DrugReferenceRepository DailyMed lookup.",
                "Requires non-empty name payload.", false, true));
        specs.add(spec("wikipedia", aliases(), PayloadMode.REQUIRED, "/wikipedia <name>",
                "Fetch Wikipedia-focused summary.", "Source-specific text summary.",
                "Uses DrugDatabaseRepository Wikipedia path.",
                "Requires non-empty name payload.", false, true));
        specs.add(spec("logs", aliases(), PayloadMode.OPTIONAL, "/logs [recent]",
                "List recent diary entries.", "Up to five recent logs with title and notes.",
                "Reads diary entries from encrypted local store.",
                "If no logs exist, returns explicit empty-state text.", true, false));
        specs.add(spec("reminders", aliases(), PayloadMode.OPTIONAL, "/reminders [active]",
                "Inspect reminder records.", "Reminder status summary list.",
                "Reads reminder data from encrypted profile payload.",
                "Can accept lightweight filters like active.", false, false));
        specs.add(spec("reminder", aliases(), PayloadMode.REQUIRED, "/reminder <spec>",
                "Create or update reminder intent.", "Reminder action confirmation text.",
                "Local command action path for reminder mutations.",
                "Free-form spec parsing can be strict.", false, false));
        specs.add(spec("addmed", aliases(), PayloadMode.REQUIRED, "/addmed <spec>",
                "Create medication from inline spec.", "Medication create confirmation text.",
                "Mutates medication store using parsed fields.",
                "Ambiguous inputs should fail with usage guidance.", false, true));
        specs.add(spec("updatemed", aliases("editmed"), PayloadMode.REQUIRED, "/updatemed <name> | field=value",
                "Update medication fields.", "Medication update confirmation text.",
                "Mutates medication record fields by key/value pairs.",
                "Pipe-delimited payload preserves spaces in values.", false, true));
        specs.add(spec("removemed", aliases("deletemed", "delmed"), PayloadMode.REQUIRED, "/removemed <name>",
                "Remove medication record.", "Deletion confirmation text.",
                "Deletes medication by resolved name or identifier.",
                "Alias commands route to same delete path.", false, true));
        specs.add(spec("clearmeds", aliases(), PayloadMode.NONE, "/clearmeds",
                "Remove all medication records.", "Bulk deletion confirmation text.",
                "Performs full medication clear operation.",
                "Should require explicit user intent at execution layer.", false, false));
        specs.add(spec("importmeds", aliases(), PayloadMode.REQUIRED, "/importmeds <text>",
                "Bulk-import medication/substance list.", "Import summary with imported/skipped counts.",
                "Parses pasted list into medication records.",
                "Supports multiline payload preserving bullets.", false, false));
        specs.add(spec("alias", aliases(), PayloadMode.REQUIRED, "/alias <name>",
                "Resolve known aliases/synonyms.", "Canonical name and alias mapping output.",
                "Uses medication catalog and candidate resolver.",
                "Input can be brand or slang names.", false, true));
        specs.add(spec("dose", aliases(), PayloadMode.REQUIRED, "/dose <name>",
                "Return dosage-oriented reference context.", "Dose notes summary text.",
                "Uses known references and catalog metadata.",
                "Never emits diagnostic instructions.", false, true));
        specs.add(spec("medinfo", aliases(), PayloadMode.REQUIRED, "/medinfo <name>",
                "Return structured medication info.", "Medication profile summary text.",
                "Uses local+reference sources for concise profile.",
                "Unknown names return explicit not-found response.", false, true));
        specs.add(spec("placeholders", aliases(), PayloadMode.NONE, "/placeholders",
                "Show supported placeholder syntax.", "Placeholder list and examples.",
                "Static capability metadata for AI usage.",
                "No payload allowed.", false, false));
        specs.add(spec("settings", aliases(), PayloadMode.NONE, "/settings",
                "Show effective assistant settings.", "Settings snapshot text.",
                "Reads effective app settings state.",
                "No payload allowed.", false, false));
        specs.add(spec("privacy", aliases(), PayloadMode.NONE, "/privacy",
                "Show privacy/context sharing state.", "Privacy toggle summary.",
                "Reads current privacy and context gates.",
                "No payload allowed.", false, false));
        specs.add(spec("drugcache", aliases(), PayloadMode.OPTIONAL, "/drugcache [status|clear|warm <term>]",
                "Inspect or maintain local drug lookup cache.", "Cache status or maintenance result text.",
                "Executes cache diagnostics and warm/clear routines.",
                "Warm accepts named presets and free-form terms.", false, true));
        specs.add(spec("context", aliases(), PayloadMode.NONE, "/context",
                "Print currently shared assistant context.",
                "Full plain-text context payload currently sent to AI.",
                "Built by AssistantContextBuilder under current privacy toggles.",
                "Always uses effective settings to gate fields.", true, false));
        specs.add(spec("newchat", aliases(), PayloadMode.OPTIONAL, "/newchat [title]",
                "Create a new chat session.", "Confirmation text with chosen title.",
                "Uses UI action callback from ViewModel.",
                "If actions unavailable, return unsupported notice.", true, false));
        return Collections.unmodifiableList(specs);
    }

    private static CommandSpec spec(String name, List<String> aliases, PayloadMode payloadMode, String usage,
                                    String usageCases, String outputFormat, String aiContextHint,
                                    String specialEdgeCase, boolean showInAutocomplete, boolean supportsPayloadAutofill) {
        return new CommandSpec(name, aliases, payloadMode, usage, usageCases, outputFormat, aiContextHint, specialEdgeCase, showInAutocomplete, supportsPayloadAutofill);
    }

    private static List<String> aliases(String... aliases) {
        List<String> all = new ArrayList<>();
        if (aliases != null) {
            Collections.addAll(all, aliases);
        }
        return Collections.unmodifiableList(all);
    }

    private static Map<String, CommandSpec> buildSpecMap(List<CommandSpec> specs) {
        Map<String, CommandSpec> map = new LinkedHashMap<>();
        for (CommandSpec spec : specs) {
            map.put(spec.name, spec);
            for (String alias : spec.aliases) map.put(alias, spec);
        }
        return Collections.unmodifiableMap(map);
    }
}
