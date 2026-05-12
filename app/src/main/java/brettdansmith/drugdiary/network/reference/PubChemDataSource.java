package brettdansmith.drugdiary.network.reference;

import android.content.Context;

import org.json.JSONObject;

import brettdansmith.drugdiary.reference.PubChemRepository;

public final class PubChemDataSource {
    public JSONObject lookup(Context context, String query) throws Exception {
        return PubChemRepository.getCompoundByName(context, query);
    }
}

