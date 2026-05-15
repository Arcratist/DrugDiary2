package brettdansmith.drugdiary.domain.repository;

import brettdansmith.drugdiary.domain.model.interaction.InteractionCheckResult;

/**
 * Repository interface for drug reference data and interaction checking.
 * Defines the contract for reference data operations.
 */
public interface DrugReferenceRepository {
    /**
     * Checks interactions between two drugs.
     *
     * @param firstDrug name of first drug
     * @param secondDrug name of second drug
     * @return interaction check result
     * @throws Exception if lookup fails
     */
    InteractionCheckResult checkInteraction(String firstDrug, String secondDrug) throws Exception;

    /**
     * Checks interactions between multiple drugs.
     *
     * @param drugs array of drug names
     * @return formatted interaction analysis
     * @throws Exception if lookup fails
     */
    String checkMultipleInteractions(String[] drugs) throws Exception;

    /**
     * Gets drug information including aliases and categories.
     *
     * @param drugName the drug name or alias
     * @return canonical drug name, or empty string if not found
     * @throws Exception if lookup fails
     */
    String resolveDrugName(String drugName) throws Exception;

    /**
     * Checks if a drug is in the reference database.
     *
     * @param drugName the drug name to check
     * @return true if drug exists
     * @throws Exception if lookup fails
     */
    boolean drugExists(String drugName) throws Exception;
}

