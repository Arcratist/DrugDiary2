package brettdansmith.drugdiary.model.medication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MedicationRecordTest {
    @Test
    public void constructorNormalizesIdsAndNames() {
        MedicationRecord record = new MedicationRecord(
                "",
                "Ibuprofen",
                "",
                "200 mg",
                "Tablet",
                "oral",
                MedicationCategory.MEDICAL,
                true,
                true,
                true,
                false,
                "With food",
                new MedicationSchedule("scheduled", "daily", 0),
                new MedicationInventory(12, "tablets", 3, 0),
                null,
                0,
                0);

        assertTrue(record.id.startsWith("med_"));
        assertEquals("Ibuprofen", record.canonicalName);
        assertTrue(record.favorite);
        assertEquals(MedicationCategory.MEDICAL, record.category);
        assertTrue(record.categories.contains(MedicationCategory.MEDICAL));
    }

    @Test
    public void inventoryAndScheduleDefaultsArePresent() {
        MedicationRecord record = new MedicationRecord(
                "id",
                "Name",
                "Name",
                "",
                "",
                "",
                MedicationCategory.SAVED,
                true,
                false,
                true,
                false,
                "",
                null,
                null,
                null,
                1,
                1);

        assertEquals("scheduled", record.schedule.type);
        assertEquals("units", record.inventory.unit);
    }

    @Test
    public void groupsNormalizeWithoutContradictions() {
        java.util.LinkedHashSet<MedicationCategory> groups = new java.util.LinkedHashSet<>();
        groups.add(MedicationCategory.MEDICAL);
        groups.add(MedicationCategory.HAVE_ACCESS);
        groups.add(MedicationCategory.WISHLIST);

        MedicationRecord record = new MedicationRecord(
                "id2",
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                "Capsule",
                "oral",
                MedicationCategory.MEDICAL,
                groups,
                false,
                false,
                true,
                false,
                "",
                null,
                null,
                null,
                1,
                1);

        assertTrue(record.hasCategory(MedicationCategory.MEDICAL));
        assertTrue(record.hasCategory(MedicationCategory.WISHLIST));
        assertTrue(!record.hasCategory(MedicationCategory.HAVE_ACCESS));
    }
}
