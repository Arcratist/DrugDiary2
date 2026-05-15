package brettdansmith.drugdiary.data.medication;

import android.content.Context;

import java.util.List;

import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;
import brettdansmith.drugdiary.domain.repository.MedicationRepository;

/**
 * Implementation adapter for MedicationRepository interface.
 * Wraps the existing data-layer MedicationRepository to provide domain-level interface.
 */
public final class MedicationRepositoryImpl implements MedicationRepository {
    private final brettdansmith.drugdiary.data.medication.MedicationRepository delegate;

    public MedicationRepositoryImpl(Context context) {
        this.delegate = new brettdansmith.drugdiary.data.medication.MedicationRepository(context);
    }

    @Override
    public List<MedicationRecord> listRecords() throws Exception {
        return delegate.listRecords();
    }

    @Override
    public MedicationRecord getRecord(String id) throws Exception {
        List<MedicationRecord> records = delegate.listRecords();
        for (MedicationRecord record : records) {
            if (record.id.equals(id)) {
                return record;
            }
        }
        return null;
    }

    @Override
    public void saveRecord(MedicationRecord record) throws Exception {
        delegate.upsertRecord(record);
    }

    @Override
    public void deleteRecord(String id) throws Exception {
        delegate.delete(id);
    }

    @Override
    public int count() throws Exception {
        return delegate.listRecords().size();
    }
}

