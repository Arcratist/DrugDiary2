package brettdansmith.drugdiary.domain.medication;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local medication/substance knowledge used for autocomplete and alias grouping.
 *
 * This is not a medical database and should not be used as clinical truth. It is a fast local
 * index of common generic names, brand names, and aliases so user-entered names can be grouped
 * consistently before public reference APIs are queried.
 */
public final class MedicationCatalog {
    private static final Map<String, Entry> BY_ALIAS = new LinkedHashMap<>();
    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        add("Acetaminophen", "Analgesic", doses("500 mg", "1 g"), notes("Avoid duplicate paracetamol/acetaminophen products", "Check liver risk and alcohol use"), "Paracetamol", "Tylenol", "Panadol");
        add("Ibuprofen", "NSAID", doses("200 mg", "400 mg"), notes("Take with food", "Avoid with some ulcers/kidney disease unless prescribed"), "Advil", "Nurofen", "Motrin");
        add("Naproxen", "NSAID", doses("220 mg", "250 mg", "500 mg"), notes("Longer acting NSAID", "Avoid combining with other NSAIDs"), "Aleve", "Naprosyn");
        add("Aspirin", "NSAID / antiplatelet", doses("81 mg", "100 mg", "300 mg"), notes("Bleeding risk", "Avoid in children unless directed"), "Acetylsalicylic acid", "ASA");
        add("Paracetamol", "Analgesic", doses("500 mg", "1 g"), notes("Avoid duplicate acetaminophen/paracetamol products"), "Panadol", "Acetaminophen");
        add("Cetirizine", "Antihistamine", doses("10 mg"), notes("May cause drowsiness"), "Zyrtec");
        add("Loratadine", "Antihistamine", doses("10 mg"), notes("Usually non-drowsy"), "Claritin");
        add("Fexofenadine", "Antihistamine", doses("120 mg", "180 mg"), notes("Usually non-drowsy"), "Allegra", "Telfast");
        add("Diphenhydramine", "Antihistamine", doses("25 mg", "50 mg"), notes("Sedating antihistamine"), "Benadryl");
        add("Doxylamine", "Antihistamine", doses("12.5 mg", "25 mg"), notes("Sedating antihistamine often used for sleep"), "Unisom");
        add("Promethazine", "Antihistamine", doses("10 mg", "25 mg"), notes("Sedating antihistamine"), "Phenergan");
        add("Loperamide", "GI antidiarrheal", doses("2 mg"), notes("Avoid high-dose misuse"), "Imodium");
        add("Esomeprazole", "GI proton pump inhibitor", doses("20 mg", "40 mg"), notes("Use as directed"), "Nexium");
        add("Hyoscine butylbromide", "GI antispasmodic", doses("10 mg"), notes("Cramping/spasm support"), "Buscopan");
        add("Amoxicillin", "Antibiotic", doses("250 mg", "500 mg"), notes("Use only prescribed course"));
        add("Mefenamic acid", "NSAID", doses("250 mg", "500 mg"), notes("Avoid combining with other NSAIDs"), "Ponstan");
        add("Indometacin", "NSAID", doses("25 mg", "50 mg"), notes("NSAID side effects can be significant"), "Indomethacin");
        add("Celecoxib", "NSAID", doses("100 mg", "200 mg"), notes("COX-2 selective NSAID"), "Celebrex");
        add("Propranolol", "Beta blocker", doses("10 mg", "40 mg"), notes("Can reduce tremor/anxiety symptoms"), "Inderal");
        add("Rosuvastatin", "Statin", doses("5 mg", "10 mg", "20 mg"), notes("Lipid-lowering medication"), "Crestor");
        add("Iron", "Supplement", doses("100 mg elemental"), notes("Can cause constipation"), "Maltofer");
        add("Vitamin C", "Supplement", doses("500 mg", "1000 mg"), notes("General supplement"), "Ascorbic acid");
        add("Vitamin D3", "Supplement", doses("25 mcg", "1000 IU"), notes("General supplement"), "Cholecalciferol", "Ostelin");
        add("Magnesium glycinate", "Supplement", doses("100 mg", "200 mg"), notes("Magnesium supplement"));
        add("Magnesium threonate", "Supplement", doses("1 g"), notes("Magnesium supplement"));
        add("Magnesium taurate", "Supplement", doses("1 g"), notes("Magnesium supplement"));
        add("Creatine monohydrate", "Supplement", doses("3 g", "5 g"), notes("Performance supplement"));
        add("N-acetylcysteine", "Supplement", doses("600 mg"), notes("Often listed as NAC"), "NAC");
        add("L-theanine", "Supplement", doses("100 mg", "200 mg"), notes("Calming supplement"), "Theanine");
        add("CDP-choline", "Supplement", doses("250 mg", "500 mg"), notes("Also called citicoline"), "Citicoline");
        add("Glycine", "Supplement", doses("1 g", "3 g"), notes("Amino acid supplement"));
        add("Taurine", "Supplement", doses("500 mg", "1000 mg"), notes("Amino acid supplement"));
        add("Agmatine", "Supplement", doses("250 mg", "500 mg"), notes("Supplement"));
        add("Electrolytes", "Supplement", doses("as directed"), notes("Hydration support"));
        add("Activated charcoal", "Harm-reduction supply", doses("as directed"), notes("Use only when appropriate and safe"));
        add("Ural urinary alkaliser", "Support product", doses("as directed"), notes("Urinary alkaliser"), "Ural");
        add("Grapefruit juice", "Food/drink interaction", doses("as used"), notes("Can interact with medications"));

        add("Sertraline", "SSRI antidepressant", doses("25 mg", "50 mg", "100 mg"), notes("Do not stop suddenly", "Check serotonergic combinations"), "Zoloft");
        add("Fluoxetine", "SSRI antidepressant", doses("10 mg", "20 mg", "40 mg"), notes("Long half-life", "Check serotonergic combinations"), "Prozac");
        add("Escitalopram", "SSRI antidepressant", doses("5 mg", "10 mg", "20 mg"), notes("Check QT/serotonergic combinations"), "Lexapro", "Cipralex");
        add("Citalopram", "SSRI antidepressant", doses("10 mg", "20 mg", "40 mg"), notes("QT caution at higher doses"), "Celexa");
        add("Venlafaxine", "SNRI antidepressant", doses("37.5 mg", "75 mg", "150 mg"), notes("Withdrawal can be rough", "Monitor blood pressure"), "Effexor");
        add("Duloxetine", "SNRI antidepressant", doses("30 mg", "60 mg"), notes("Check liver risk and serotonergic combinations"), "Cymbalta");
        add("Bupropion", "NDRI antidepressant", doses("150 mg", "300 mg"), notes("Seizure-threshold caution", "Stimulant-like side effects possible"), "Wellbutrin", "Zyban");
        add("Mirtazapine", "Antidepressant", doses("15 mg", "30 mg", "45 mg"), notes("Sedation and appetite changes are common"), "Remeron", "Avanza");
        add("Moclobemide", "Reversible MAO-A inhibitor", doses("150 mg", "300 mg"), notes("MAOI interaction caution", "Check serotonergic/stimulant combinations before mixing"), "Aurorix", "Manerix", "Moclodura");
        add("Amitriptyline", "Tricyclic antidepressant", doses("10 mg", "25 mg", "50 mg"), notes("Sedating", "Overdose and interaction risk"), "Endep", "Elavil");
        add("Quetiapine", "Atypical antipsychotic", doses("25 mg", "50 mg", "100 mg"), notes("Sedation common", "Metabolic monitoring may matter"), "Seroquel");
        add("Risperidone", "Atypical antipsychotic", doses("0.5 mg", "1 mg", "2 mg"), notes("Movement/prolactin side effects possible"), "Risperdal");
        add("Olanzapine", "Atypical antipsychotic", doses("2.5 mg", "5 mg", "10 mg"), notes("Sedation and metabolic effects possible"), "Zyprexa");
        add("Lithium", "Mood stabilizer", doses("250 mg", "300 mg", "450 mg"), notes("Blood-level monitoring", "Hydration/salt consistency matters"), "Lithium carbonate", "Lithicarb");
        add("Lamotrigine", "Mood stabilizer / anticonvulsant", doses("25 mg", "100 mg", "200 mg"), notes("Slow titration", "Rash needs urgent review"), "Lamictal");

        add("Methylphenidate", "Stimulant", doses("5 mg", "10 mg", "18 mg", "36 mg"), notes("Monitor sleep/appetite/heart rate"), "Ritalin", "Concerta", "Rubifen");
        add("Lisdexamfetamine", "Stimulant", doses("20 mg", "30 mg", "50 mg", "70 mg"), notes("Long acting", "Avoid stimulant stacking"), "Vyvanse", "Elvanse");
        add("Dextroamphetamine", "Stimulant", doses("5 mg", "10 mg"), notes("Avoid stimulant stacking", "Monitor sleep/heart rate"), "Dexedrine", "Adderall", "Amfexa");
        add("Modafinil", "Wakefulness-promoting agent", doses("100 mg", "200 mg"), notes("Sleep disruption possible", "Check contraceptive interaction warnings"), "Provigil");
        add("Armodafinil", "Wakefulness-promoting agent", doses("150 mg", "250 mg"), notes("Longer acting modafinil isomer"), "Nuvigil");

        add("Diazepam", "Benzodiazepine", doses("2 mg", "5 mg", "10 mg"), notes("Do not mix with alcohol/opioids", "Dependence and sedation risk"), "Valium");
        add("Alprazolam", "Benzodiazepine", doses("0.25 mg", "0.5 mg", "1 mg"), notes("High dependence/sedation risk", "Do not mix with alcohol/opioids"), "Xanax");
        add("Clonazepam", "Benzodiazepine", doses("0.5 mg", "1 mg"), notes("Long acting", "Do not mix with alcohol/opioids"), "Klonopin", "Rivotril");
        add("Lorazepam", "Benzodiazepine", doses("0.5 mg", "1 mg", "2 mg"), notes("Sedation risk", "Do not mix with alcohol/opioids"), "Ativan");
        add("Pregabalin", "Gabapentinoid", doses("25 mg", "75 mg", "150 mg"), notes("Sedation risk", "Caution with opioids/alcohol"), "Lyrica");
        add("Gabapentin", "Gabapentinoid", doses("100 mg", "300 mg", "600 mg"), notes("Sedation risk", "Caution with opioids/alcohol"), "Neurontin");

        add("Caffeine", "Stimulant", doses("50 mg", "100 mg", "200 mg"), notes("Sleep and anxiety effects", "Watch total daily intake"), "Coffee", "Energy drink");
        add("Nicotine", "Stimulant", doses("1 mg", "2 mg", "4 mg"), notes("Dependence risk", "Track route and frequency"), "Tobacco", "Vape");
        add("Alcohol", "Depressant", doses("1 standard drink"), notes("Avoid with sedatives/opioids", "Track units/standard drinks"), "Ethanol");
        add("Cannabis", "Cannabinoid", doses("as used"), notes("Track THC/CBD and route", "Can affect anxiety, sleep, and coordination"), "THC", "Marijuana", "Weed");
        add("CBD", "Cannabinoid", doses("5 mg", "10 mg", "25 mg"), notes("Can interact with some medications"), "Cannabidiol");
        add("MDMA", "Entactogen/stimulant", doses("not recorded"), notes("Hydration/overheating risk", "Avoid serotonergic combinations"), "Ecstasy", "Molly", "3,4-MDMA");
        add("Ketamine", "Dissociative", doses("not recorded"), notes("Avoid depressant combinations", "Bladder risk with frequent use"), "Special K");
        add("Dextromethorphan", "Dissociative / cough suppressant", doses("15 mg/5 ml"), notes("Interaction risk with serotonergic medications"), "DXM");
        add("DMT", "Psychedelic", doses("not recorded"), notes("Short-acting psychedelic"));
        add("Psilocybin", "Psychedelic", doses("not recorded"), notes("Set/setting and mental health context matter"), "Magic mushrooms", "Mushrooms");
        add("LSD", "Psychedelic", doses("not recorded"), notes("Long duration", "Set/setting and mental health context matter"), "Acid", "Lysergic acid diethylamide");
        add("Nitrous oxide", "Dissociative", doses("not recorded"), notes("Can impact B12 with repeated use"), "N2O", "NOS");
        add("Cocaine", "Stimulant", doses("not recorded"), notes("Cardiovascular risk"));
        add("Naloxone", "Opioid overdose reversal", doses("nasal spray", "injection"), notes("Emergency overdose response", "Still call emergency services"), "Narcan");
    }

    private MedicationCatalog() {
    }

    public static List<String> nameSuggestions() {
        List<String> values = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            values.add(entry.canonical);
            values.addAll(Arrays.asList(entry.aliases));
        }
        return values;
    }

    public static List<String> doseSuggestions(String name) {
        Entry entry = entryFor(name);
        if (entry == null) return Arrays.asList("5 mg", "10 mg", "20 mg", "25 mg", "50 mg", "100 mg", "200 mg", "500 mg", "1 tablet", "as prescribed");
        return Arrays.asList(entry.doseSuggestions);
    }

    public static List<String> noteSuggestions(String name) {
        List<String> defaults = new ArrayList<>();
        defaults.add("Prescribed medication");
        defaults.add("Take with food");
        defaults.add("Avoid alcohol");
        defaults.add("Morning only");
        defaults.add("Evening only");
        defaults.add("As needed");
        defaults.add("Monitor side effects");
        Entry entry = entryFor(name);
        if (entry != null) defaults.addAll(Arrays.asList(entry.noteSuggestions));
        return defaults;
    }

    public static String canonicalNameFor(String name) {
        Entry entry = entryFor(name);
        if (entry != null) return entry.canonical;
        return name == null ? "" : name.trim();
    }

    public static String categoryFor(String name) {
        Entry entry = entryFor(name);
        return entry == null ? "Uncategorised" : entry.category;
    }

    public static JSONArray aliasesFor(String name) {
        JSONArray aliases = new JSONArray();
        Entry entry = entryFor(name);
        if (entry != null) {
            putAlias(aliases, entry.canonical);
            for (String alias : entry.aliases) putAlias(aliases, alias);
        }
        putAlias(aliases, name);
        return aliases;
    }

    public static boolean matches(String name, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.toLowerCase(Locale.US).trim();
        if (name != null && name.toLowerCase(Locale.US).contains(q)) return true;
        Entry entry = entryFor(name);
        if (entry == null) return false;
        if (entry.canonical.toLowerCase(Locale.US).contains(q) || entry.category.toLowerCase(Locale.US).contains(q)) return true;
        for (String alias : entry.aliases) {
            if (alias.toLowerCase(Locale.US).contains(q)) return true;
        }
        return false;
    }

    private static Entry entryFor(String name) {
        if (name == null) return null;
        return BY_ALIAS.get(normalize(name));
    }

    private static void add(String canonical, String category, String[] doses, String[] notes, String... aliases) {
        Entry entry = new Entry(canonical, category, doses, notes, aliases);
        ENTRIES.add(entry);
        BY_ALIAS.put(normalize(canonical), entry);
        for (String alias : aliases) BY_ALIAS.put(normalize(alias), entry);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    private static String[] doses(String... values) {
        return values;
    }

    private static String[] notes(String... values) {
        return values;
    }

    private static void putAlias(JSONArray aliases, String value) {
        if (value == null || value.trim().isEmpty()) return;
        for (int i = 0; i < aliases.length(); i++) {
            if (value.equalsIgnoreCase(aliases.optString(i))) return;
        }
        aliases.put(value.trim());
    }

    public static final class Entry {
        public final String canonical;
        public final String category;
        public final String[] doseSuggestions;
        public final String[] noteSuggestions;
        public final String[] aliases;

        Entry(String canonical, String category, String[] doseSuggestions, String[] noteSuggestions, String[] aliases) {
            this.canonical = canonical;
            this.category = category;
            this.doseSuggestions = doseSuggestions;
            this.noteSuggestions = noteSuggestions;
            this.aliases = aliases;
        }
    }
}
