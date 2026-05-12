package brettdansmith.drugdiary.data.reference;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.domain.medication.MedicationCatalog;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.security.UserSession;

/**
 * Opportunistically warms the encrypted, profile-local public reference cache.
 *
 * This runs only after PIN unlock because reference cache writes live inside the profile vault.
 * It intentionally batches a small rotating set instead of downloading whole public databases:
 * keyless APIs are useful, but pulling everything on startup would be slow, impolite, and fragile.
 */
public final class DrugReferencePrewarmer {
    private static final String PREFS = "DrugDiaryReferencePrewarm";
    private static final String KEY_LAST_RUN_PREFIX = "last_run_";
    private static final long MIN_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final int MAX_LOOKUPS = 8;
    private static final String[] MEDICAL_SEEDS = {
            "Acetaminophen", "Ibuprofen", "Naproxen", "Aspirin", "Cetirizine", "Loratadine",
            "Sertraline", "Fluoxetine", "Escitalopram", "Venlafaxine", "Bupropion", "Mirtazapine",
            "Moclobemide", "Amitriptyline", "Quetiapine", "Lithium", "Lamotrigine",
            "Methylphenidate", "Lisdexamfetamine", "Modafinil", "Diazepam", "Alprazolam",
            "Pregabalin", "Gabapentin", "Naloxone"
    };
    private static final String[] RECREATIONAL_SEEDS = {
            "Caffeine", "Nicotine", "Alcohol", "Cannabis", "Cannabidiol", "MDMA",
            "Ketamine", "Psilocybin", "LSD", "Cocaine", "Amphetamine", "Methamphetamine",
            "Heroin", "Morphine", "Oxycodone", "Fentanyl", "GHB", "DMT", "Mescaline",
            "Nitrous oxide", "DXM", "Kratom"
    };

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private DrugReferencePrewarmer() {
    }

    public static void prewarmAfterUnlock(Context context) {
        Context appContext = context.getApplicationContext();
        if (!UserSession.getInstance().isActive()) return;
        String profile = UserSession.getInstance().getProfileName();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        String key = KEY_LAST_RUN_PREFIX + profile;
        if (now - prefs.getLong(key, 0) < MIN_INTERVAL_MS) return;
        prefs.edit().putLong(key, now).apply();

        EXECUTOR.execute(() -> {
            if (!UserSession.getInstance().isActive()) return;
            DrugReferenceRepository referenceRepository = new DrugReferenceRepository(appContext);
            for (String name : prewarmNames(appContext)) {
                try {
                    referenceRepository.lookupAll(name);
                } catch (Exception ignored) {
                    // Prewarm is best-effort. Interactive lookups still surface errors to the user.
                }
            }
        });
    }

    public static String warmNow(Context context, String query) throws Exception {
        if (!UserSession.getInstance().isActive()) return "No active profile session.";
        List<String> names = namesForWarmCommand(context, query);
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        StringBuilder result = new StringBuilder("=== Drug Reference Prewarm ===\n");
        int warmed = 0;
        for (String name : names) {
            try {
                repository.lookupAll(name);
                warmed++;
                result.append("- Warmed: ").append(name).append("\n");
            } catch (Exception e) {
                result.append("- Skipped: ").append(name).append(" (").append(e.getMessage()).append(")\n");
            }
        }
        result.append("- Total warmed: ").append(warmed).append(" of ").append(names.size()).append("\n");
        result.append("- Cache scope: active encrypted profile vault");
        return result.toString().trim();
    }

    private static List<String> prewarmNames(Context context) {
        List<String> names = new ArrayList<>();
        try {
            JSONArray meds = new MedicationRepository(context).list();
            for (int i = 0; i < meds.length() && names.size() < MAX_LOOKUPS; i++) {
                JSONObject med = meds.optJSONObject(i);
                if (med == null) continue;
                addUnique(names, med.optString("canonical_name", med.optString("name", "")));
            }
        } catch (Exception ignored) {
        }

        List<String> catalog = seedNames();
        int offset = (int) ((System.currentTimeMillis() / MIN_INTERVAL_MS) % Math.max(1, catalog.size()));
        for (int i = 0; names.size() < MAX_LOOKUPS && i < catalog.size(); i++) {
            addUnique(names, catalog.get((offset + i) % catalog.size()));
        }
        return names;
    }

    private static List<String> namesForWarmCommand(Context context, String query) {
        String mode = query == null ? "" : query.trim().toLowerCase(java.util.Locale.US);
        List<String> names = new ArrayList<>();
        if ("common".equals(mode) || "default".equals(mode) || "all".equals(mode)) {
            addAll(names, MEDICAL_SEEDS);
            addAll(names, RECREATIONAL_SEEDS);
        } else if ("medical".equals(mode) || "medicine".equals(mode) || "medications".equals(mode)) {
            addAll(names, MEDICAL_SEEDS);
        } else if ("recreational".equals(mode) || "substances".equals(mode) || "rec".equals(mode)) {
            addAll(names, RECREATIONAL_SEEDS);
        } else if ("saved".equals(mode) || "profile".equals(mode)) {
            try {
                JSONArray meds = new MedicationRepository(context).list();
                for (int i = 0; i < meds.length(); i++) {
                    JSONObject med = meds.optJSONObject(i);
                    if (med == null) continue;
                    addUnique(names, med.optString("canonical_name", med.optString("name", "")));
                }
            } catch (Exception ignored) {
            }
        } else {
            for (String candidate : MedicationQueryResolver.candidatesFor(context, query)) {
                addUnique(names, candidate);
            }
        }
        return names;
    }

    private static List<String> seedNames() {
        List<String> names = new ArrayList<>();
        addAll(names, MEDICAL_SEEDS);
        addAll(names, RECREATIONAL_SEEDS);
        return names;
    }

    private static void addAll(List<String> names, String[] values) {
        for (String value : values) addUnique(names, value);
    }

    private static void addUnique(List<String> names, String value) {
        if (value == null || value.trim().isEmpty()) return;
        String clean = MedicationCatalog.canonicalNameFor(value.trim());
        for (String existing : names) {
            if (existing.equalsIgnoreCase(clean)) return;
        }
        names.add(clean);
    }
}
