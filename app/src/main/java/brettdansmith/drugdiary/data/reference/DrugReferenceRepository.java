package brettdansmith.drugdiary.data.reference;

import android.content.Context;

import org.json.JSONObject;

import brettdansmith.drugdiary.domain.medication.DrugReferenceFormatter;
import brettdansmith.drugdiary.network.reference.ChemblDataSource;
import brettdansmith.drugdiary.network.reference.OpenFdaDataSource;
import brettdansmith.drugdiary.network.reference.PubChemDataSource;
import brettdansmith.drugdiary.network.reference.RxNormDataSource;
import brettdansmith.drugdiary.reference.DrugDatabaseClient;

/**
 * App-wide public reference facade. Sources are limited to free/keyless APIs:
 * PubChem, RxNorm/RxNav, openFDA, ChEMBL, and existing local references.
 */
public final class DrugReferenceRepository {
    private final Context appContext;
    private final PubChemDataSource pubChem = new PubChemDataSource();
    private final RxNormDataSource rxNorm = new RxNormDataSource();
    private final OpenFdaDataSource openFda = new OpenFdaDataSource();
    private final ChemblDataSource chembl = new ChemblDataSource();

    public DrugReferenceRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public JSONObject lookupRxNorm(String query) throws Exception {
        return rxNorm.lookup(appContext, query);
    }

    public JSONObject lookupOpenFdaLabel(String query) throws Exception {
        return openFda.lookupLabel(appContext, query);
    }

    public JSONObject lookupOpenFdaEvent(String query) throws Exception {
        return openFda.lookupEvent(appContext, query);
    }

    public JSONObject lookupChembl(String query) throws Exception {
        return chembl.lookup(appContext, query);
    }

    public JSONObject lookupPubChem(String query) throws Exception {
        return pubChem.lookup(appContext, query);
    }

    public JSONObject lookupDailyMed(String query) throws Exception {
        return DrugDatabaseClient.lookupDailyMed(appContext, query);
    }

    public JSONObject lookupAll(String query) throws Exception {
        return DrugDatabaseClient.lookupAll(appContext, query);
    }

    public String summary(JSONObject payload) {
        return DrugReferenceFormatter.summary(payload);
    }

    public String raw(JSONObject payload) {
        return DrugReferenceFormatter.raw(payload);
    }

    public String clearCache() throws Exception {
        int cleared = new DrugReferenceCacheStore(appContext).clear();
        return "Cleared " + cleared + " cached public drug database entr" + (cleared == 1 ? "y." : "ies.");
    }
}

