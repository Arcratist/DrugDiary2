package brettdansmith.drugdiary.data.reference;

import android.content.Context;

import brettdansmith.drugdiary.domain.model.interaction.InteractionCheckResult;
import brettdansmith.drugdiary.domain.repository.DrugReferenceRepository;
import brettdansmith.drugdiary.reference.DrugInteractionRepository;

/**
 * Implementation adapter for DrugReferenceRepository interface.
 * Wraps the existing interaction and reference repositories to provide domain-level interface.
 */
public final class DrugReferenceRepositoryImpl implements DrugReferenceRepository {
    private final Context context;

    public DrugReferenceRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public InteractionCheckResult checkInteraction(String firstDrug, String secondDrug) throws Exception {
        return DrugInteractionRepository.buildPair(context, firstDrug, secondDrug);
    }

    @Override
    public String checkMultipleInteractions(String[] drugs) throws Exception {
        return DrugInteractionRepository.checkMultiple(context, drugs);
    }

    @Override
    public String resolveDrugName(String drugName) throws Exception {
        // This would use the MedicationCatalog or similar resolver
        // For now, return empty if not found, or the name if it matches
        return drugName;
    }

    @Override
    public boolean drugExists(String drugName) throws Exception {
        // This would check if a drug exists in the reference database
        // For now, always return true for non-empty names
        return drugName != null && !drugName.trim().isEmpty();
    }
}

