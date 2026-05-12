package brettdansmith.drugdiary.network.reference;

import android.content.Context;

import org.json.JSONObject;

import brettdansmith.drugdiary.reference.DrugDatabaseClient;

public final class OpenFdaDataSource {
    public JSONObject lookupLabel(Context context, String query) throws Exception {
        return DrugDatabaseClient.lookupOpenFdaLabel(context, query);
    }

    public JSONObject lookupEvent(Context context, String query) throws Exception {
        return DrugDatabaseClient.lookupOpenFdaEvent(context, query);
    }
}

