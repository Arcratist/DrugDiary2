package brettdansmith.drugdiary.network.reference;

import android.content.Context;

import org.json.JSONObject;

import brettdansmith.drugdiary.reference.DrugDatabaseClient;

public final class ChemblDataSource {
    public JSONObject lookup(Context context, String query) throws Exception {
        return DrugDatabaseClient.lookupChembl(context, query);
    }
}

