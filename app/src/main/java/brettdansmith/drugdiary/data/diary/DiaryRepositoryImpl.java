package brettdansmith.drugdiary.data.diary;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;
import brettdansmith.drugdiary.domain.repository.DiaryRepository;

/**
 * Implementation adapter for DiaryRepository interface.
 * Wraps the existing data-layer DiaryRepository to provide domain-level interface.
 */
public final class DiaryRepositoryImpl implements DiaryRepository {
    private final brettdansmith.drugdiary.data.diary.DiaryRepository delegate;

    public DiaryRepositoryImpl(Context context) {
        this.delegate = new brettdansmith.drugdiary.data.diary.DiaryRepository(context);
    }

    @Override
    public List<DiaryEntry> listEntries() throws Exception {
        return delegate.listEntries();
    }

    @Override
    public DiaryEntry getEntry(String id) throws Exception {
        List<DiaryEntry> entries = delegate.listEntries();
        for (DiaryEntry entry : entries) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public void saveEntry(DiaryEntry entry) throws Exception {
        delegate.addEntry(entry);
    }

    @Override
    public void deleteEntry(String id) throws Exception {
        // Note: DiaryRepository doesn't currently support delete
        // Implementation would be added when needed
        throw new UnsupportedOperationException("Diary entry deletion not yet implemented");
    }

    @Override
    public List<DiaryEntry> entriesBetween(long startTime, long endTime) throws Exception {
        List<DiaryEntry> filtered = new ArrayList<>();
        for (DiaryEntry entry : delegate.listEntries()) {
            if (entry.createdAt >= startTime && entry.createdAt <= endTime) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
}

