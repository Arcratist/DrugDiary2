package brettdansmith.drugdiary.reference;

import android.content.Context;

import org.json.JSONObject;

public final class PubChemRepository {
    private PubChemRepository() {}

    public static JSONObject getCompoundByName(Context context, String name) throws Exception {
        return PubChemClient.lookupCompound(context, name);
    }

    public static String getCompoundSummaryByName(Context context, String name) throws Exception {
        return PubChemClient.formatForAssistant(getCompoundByName(context, name));
    }

    public static String getCompoundRawByName(Context context, String name) throws Exception {
        return PubChemClient.formatRawSections(getCompoundByName(context, name));
    }
}

