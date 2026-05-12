package brettdansmith.drugdiary.domain.medication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MedicationCatalogTest {
    @Test
    public void moclobemideAndBrandsResolveToSameCanonicalName() {
        assertEquals("Moclobemide", MedicationCatalog.canonicalNameFor("Moclobemide"));
        assertEquals("Moclobemide", MedicationCatalog.canonicalNameFor("Aurorix"));
        assertEquals("Moclobemide", MedicationCatalog.canonicalNameFor("Manerix"));
        assertEquals("Reversible MAO-A inhibitor", MedicationCatalog.categoryFor("Aurorix"));
    }

    @Test
    public void aliasesIncludeCanonicalAndBrandNames() {
        assertTrue(MedicationCatalog.nameSuggestions().contains("Moclobemide"));
        assertTrue(MedicationCatalog.nameSuggestions().contains("Aurorix"));
        assertTrue(MedicationCatalog.nameSuggestions().contains("Manerix"));
    }
}
