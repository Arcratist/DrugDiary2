package brettdansmith.drugdiary.reference;

import android.content.Context;

import org.json.JSONObject;

import brettdansmith.drugdiary.data.reference.DrugReferenceRepository;

public final class DrugDatabaseRepository {
    private DrugDatabaseRepository() {}

    public static JSONObject lookupRxNorm(Context context, String query) throws Exception {
        return new DrugReferenceRepository(context).lookupRxNorm(query);
    }

    public static JSONObject lookupOpenFdaLabel(Context context, String query) throws Exception {
        return new DrugReferenceRepository(context).lookupOpenFdaLabel(query);
    }

    public static JSONObject lookupOpenFdaEvent(Context context, String query) throws Exception {
        return new DrugReferenceRepository(context).lookupOpenFdaEvent(query);
    }

    public static JSONObject lookupChembl(Context context, String query) throws Exception {
        return new DrugReferenceRepository(context).lookupChembl(query);
    }

    public static JSONObject lookupWikipedia(Context context, String query) throws Exception {
        return DrugDatabaseClient.lookupWikipedia(context, query);
    }

    public static JSONObject lookupDailyMed(Context context, String query) throws Exception {
        return new DrugReferenceRepository(context).lookupDailyMed(query);
    }

    public static JSONObject lookupAll(Context context, String query) throws Exception {
        return new DrugReferenceRepository(context).lookupAll(query);
    }

    public static String getRxNormSummary(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.summary(repository.lookupRxNorm(query));
    }

    public static String getRxNormRaw(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.raw(repository.lookupRxNorm(query));
    }

    public static String getOpenFdaLabelSummary(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.summary(repository.lookupOpenFdaLabel(query));
    }

    public static String getOpenFdaLabelRaw(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.raw(repository.lookupOpenFdaLabel(query));
    }

    public static String getOpenFdaEventSummary(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.summary(repository.lookupOpenFdaEvent(query));
    }

    public static String getOpenFdaEventRaw(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.raw(repository.lookupOpenFdaEvent(query));
    }

    public static String getChemblSummary(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.summary(repository.lookupChembl(query));
    }

    public static String getChemblRaw(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.raw(repository.lookupChembl(query));
    }
    
    public static String getWikipediaSummary(Context context, String query) throws Exception {
        return DrugDatabaseClient.formatSummary(lookupWikipedia(context, query));
    }
    
    public static String getWikipediaRaw(Context context, String query) throws Exception {
        return DrugDatabaseClient.formatRaw(lookupWikipedia(context, query));
    }

    public static String getAllSummary(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.summary(repository.lookupAll(query));
    }

    public static String getAllRaw(Context context, String query) throws Exception {
        DrugReferenceRepository repository = new DrugReferenceRepository(context);
        return repository.raw(repository.lookupAll(query));
    }

    public static String clearDrugCache(Context context) throws Exception {
        return "=== Drug Data Cache ===\n- " + new DrugReferenceRepository(context).clearCache() + "\n- Scope: Current encrypted profile vault";
    }
}

