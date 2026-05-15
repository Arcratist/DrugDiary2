package brettdansmith.drugdiary.domain.repository;

import java.util.List;

import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;

/**
 * Repository interface for managing diary entries.
 * Defines the contract for all diary data operations.
 */
public interface DiaryRepository {
    /**
     * Retrieves all diary entries.
     *
     * @return List of all diary entries
     * @throws Exception if database access fails
     */
    List<DiaryEntry> listEntries() throws Exception;

    /**
     * Retrieves a specific diary entry by ID.
     *
     * @param id the entry ID
     * @return the diary entry, or null if not found
     * @throws Exception if database access fails
     */
    DiaryEntry getEntry(String id) throws Exception;

    /**
     * Saves or updates a diary entry.
     *
     * @param entry the diary entry to save
     * @throws Exception if database access fails
     */
    void saveEntry(DiaryEntry entry) throws Exception;

    /**
     * Deletes a diary entry.
     *
     * @param id the entry ID to delete
     * @throws Exception if database access fails
     */
    void deleteEntry(String id) throws Exception;

    /**
     * Retrieves entries for a specific date range.
     *
     * @param startTime start timestamp
     * @param endTime end timestamp
     * @return List of diary entries in the range
     * @throws Exception if database access fails
     */
    List<DiaryEntry> entriesBetween(long startTime, long endTime) throws Exception;
}

