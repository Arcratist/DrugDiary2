package brettdansmith.drugdiary.domain.repository;

import java.util.List;

import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;

/**
 * Repository interface for managing medications.
 * Defines the contract for all medication data operations.
 */
public interface MedicationRepository {
    /**
     * Retrieves all medication records.
     *
     * @return List of all medication records
     * @throws Exception if database access fails
     */
    List<MedicationRecord> listRecords() throws Exception;

    /**
     * Retrieves a specific medication by ID.
     *
     * @param id the medication ID
     * @return the medication record, or null if not found
     * @throws Exception if database access fails
     */
    MedicationRecord getRecord(String id) throws Exception;

    /**
     * Saves or updates a medication record.
     *
     * @param record the medication to save
     * @throws Exception if database access fails
     */
    void saveRecord(MedicationRecord record) throws Exception;

    /**
     * Deletes a medication record.
     *
     * @param id the medication ID to delete
     * @throws Exception if database access fails
     */
    void deleteRecord(String id) throws Exception;

    /**
     * Counts total medications matching criteria.
     *
     * @return the count of medications
     * @throws Exception if database access fails
     */
    int count() throws Exception;
}

