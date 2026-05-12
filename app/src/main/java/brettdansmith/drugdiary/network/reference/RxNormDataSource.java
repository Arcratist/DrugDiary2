package brettdansmith.drugdiary.network.reference;

import android.content.Context;

import org.json.JSONObject;

import brettdansmith.drugdiary.reference.DrugDatabaseClient;

public final class RxNormDataSource {
    public JSONObject lookup(Context context, String query) throws Exception {
        return DrugDatabaseClient.lookupRxNorm(context, query);
    }
}

