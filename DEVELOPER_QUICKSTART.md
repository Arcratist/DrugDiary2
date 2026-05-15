# DrugDiary Android App - Developer Quick Start Guide

## Welcome! 👋

This guide will help you understand the new architecture and how to work with the refactored DrugDiary app.

---

## 30-Second Overview

The app now follows a **clean, layered architecture**:

```
Fragments (UI)
    ↓ use
ServiceLocator
    ↓ provides
Repository Interfaces
    ↓ implemented by
Repository Implementations
    ↓ wrap
Storage/Network APIs
```

**Key Rule**: Always use `ServiceLocator` to access data, never instantiate repositories directly.

---

## How to Access Data in a Fragment

### The Right Way (NEW ✅)
```java
public class MyFragment extends Fragment {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get the medication repository
        MedicationRepository medRepo = 
            ServiceLocator.getInstance(requireContext())
                .getMedicationRepository();
        
        // Use it
        try {
            List<MedicationRecord> medications = medRepo.listRecords();
            // Update UI
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
```

### The Old Way (DEPRECATED ❌)
```java
// DON'T DO THIS ANYMORE
MedicationRepository repo = new MedicationRepository(context);
```

---

## Available Repositories

### MedicationRepository
```java
ServiceLocator.getInstance(context)
    .getMedicationRepository()
```

**Methods**:
- `List<MedicationRecord> listRecords()` - Get all medications
- `MedicationRecord getRecord(String id)` - Get specific medication
- `void saveRecord(MedicationRecord record)` - Save/update medication
- `void deleteRecord(String id)` - Delete medication
- `int count()` - Get medication count

### SettingsRepository
```java
ServiceLocator.getInstance(context)
    .getSettingsRepository()
```

**Methods**:
- `int getTheme()` - Get app theme
- `void setTheme(int theme)` - Set app theme
- `AiProvider getAiProvider()` - Get current AI provider
- `void setAiProvider(AiProvider provider)` - Change AI provider
- `boolean isPrivateModeEnabled()` - Check private mode
- `void setPrivateModeEnabled(boolean)` - Toggle private mode

### DiaryRepository
```java
ServiceLocator.getInstance(context)
    .getDiaryRepository()
```

**Methods**:
- `List<DiaryEntry> listEntries()` - Get all diary entries
- `DiaryEntry getEntry(String id)` - Get specific entry
- `void saveEntry(DiaryEntry entry)` - Save entry
- `List<DiaryEntry> entriesBetween(long start, long end)` - Get entries by date range

### DrugReferenceRepository
```java
ServiceLocator.getInstance(context)
    .getDrugReferenceRepository()
```

**Methods**:
- `InteractionCheckResult checkInteraction(String drug1, String drug2)` - Check interaction
- `String checkMultipleInteractions(String[] drugs)` - Check multiple
- `String resolveDrugName(String drugName)` - Get canonical name
- `boolean drugExists(String drugName)` - Check if drug exists

### AiService
```java
ServiceLocator.getInstance(context)
    .getAiService()
```

**Methods**:
- `String sendMessage(String message)` - Send message to AI
- `void sendMessageStreaming(...)` - Send with streaming
- `AiProvider getCurrentProvider()` - Get active provider
- `void setCurrentProvider(AiProvider)` - Switch provider

---

## Common Patterns

### 1. Load Data and Update UI
```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    MedicationRepository repo = 
        ServiceLocator.getInstance(requireContext())
            .getMedicationRepository();
    
    try {
        List<MedicationRecord> medications = repo.listRecords();
        adapter.submitList(medications);
    } catch (Exception e) {
        showError(e.getMessage());
    }
}
```

### 2. Save Data
```java
private void saveMedication(MedicationRecord record) {
    MedicationRepository repo = 
        ServiceLocator.getInstance(requireContext())
            .getMedicationRepository();
    
    try {
        repo.saveRecord(record);
        Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
        showError(e.getMessage());
    }
}
```

### 3. Handle Async Operations
```java
private void loadDataAsync() {
    new Thread(() -> {
        try {
            List<MedicationRecord> medications = 
                ServiceLocator.getInstance(requireContext())
                    .getMedicationRepository()
                    .listRecords();
            
            requireActivity().runOnUiThread(() -> {
                adapter.submitList(medications);
            });
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> 
                showError(e.getMessage())
            );
        }
    }).start();
}
```

### 4. Check Settings
```java
private void updateTheme() {
    SettingsRepository settings = 
        ServiceLocator.getInstance(requireContext())
            .getSettingsRepository();
    
    int theme = settings.getTheme();
    AppCompatDelegate.setDefaultNightMode(theme);
}
```

---

## Architecture Layers Explained

### UI Layer (`ui/`)
- **Components**: Fragments, ViewModels, Adapters
- **Responsibility**: Display data, handle user input
- **Access Data Via**: ServiceLocator → Repository Interfaces
- **Never Access**: Storage directly, network calls directly

### Domain Layer (`domain/`)
- **Components**: Repository Interfaces, Models, Business Logic
- **Responsibility**: Define contracts, hold business rules
- **Framework**: Java only (no Android dependencies)
- **Tools**: Interfaces, enums, validators

### Data Layer (`data/`)
- **Components**: Repository Implementations, Data Sources
- **Responsibility**: Implement repository contracts, handle storage
- **Access**: EncryptedProfileStore, SharedPreferences, Network APIs
- **Pattern**: Wrapper adapters around existing implementations

### Storage/Network Layer
- **Components**: EncryptedProfileStore, SharedPreferences, HTTP APIs
- **Responsibility**: Actual data persistence and retrieval
- **Access**: Via repositories only

---

## Model Classes (Domain Models)

All models are in `domain.model.*` packages:

### Medication Models
- `MedicationRecord` - Main medication data
- `MedicationCategory` - Enum for categories (SAVED, ACTIVE, etc.)
- `MedicationSchedule` - Dosing schedule
- `MedicationInventory` - Inventory tracking
- `MedicationDoseLog` - Historical dose data

### Diary Models
- `DiaryEntry` - Main diary entry
- `MoodCheckIn` - Mood tracking (1-10 scale)
- `SleepLog` - Sleep data
- `SymptomLog` - Symptom tracking

### AI Models
- `AiResolvedConfig` - Resolved AI configuration
- `ProviderCapabilities` - What each AI provider can do
- `AiRequestOptions` - Options for AI requests

### Other Models
- `InteractionCheckResult` - Drug interaction result
- `ResourceCategory` - Support resource categories
- `SupportResource` - Support resource data

### Creating Domain Models

```java
// Models are immutable - use constructor
MedicationRecord medication = new MedicationRecord(
    "", // id (auto-generated if empty)
    "Aspirin",
    "Aspirin",
    "500 mg",
    "Tablet",
    "oral",
    MedicationCategory.MEDICAL,
    new LinkedHashSet<>(),
    true, // active
    false, // favorite
    true, // saved
    false, // prn
    "With food",
    null, // schedule (optional)
    null, // inventory (optional)
    new LinkedHashSet<>(), // aliases
    System.currentTimeMillis(), // created at
    System.currentTimeMillis() // updated at
);

// Save it
ServiceLocator.getInstance(context)
    .getMedicationRepository()
    .saveRecord(medication);
```

---

## Debugging Tips

### 1. Check ServiceLocator Initialization
```java
try {
    ServiceLocator locator = ServiceLocator.getInstance(context);
    Log.d("DEBUG", "ServiceLocator ready");
} catch (Exception e) {
    Log.e("DEBUG", "ServiceLocator error: " + e.getMessage());
}
```

### 2. Log Repository Operations
```java
try {
    List<MedicationRecord> meds = repo.listRecords();
    Log.d("DEBUG", "Loaded " + meds.size() + " medications");
} catch (Exception e) {
    Log.e("DEBUG", "Repository error", e);
}
```

### 3. Catch JSON Errors
```java
try {
    MedicationRecord record = MedicationRecord.fromJson(json);
} catch (JSONException e) {
    Log.e("DEBUG", "JSON parsing error: " + e.getMessage());
}
```

---

## Important Security Notes

⚠️ **DO**:
- ✅ Use repositories to access data
- ✅ Let repositories handle encryption/decryption
- ✅ Check private mode before showing sensitive data
- ✅ Validate all user inputs

⚠️ **DON'T**:
- ❌ Access EncryptedProfileStore directly
- ❌ Store API keys as plain text
- ❌ Log medication data
- ❌ Expose profile data in external requests

---

## Common Issues & Solutions

### Issue: "Cannot resolve symbol MedicationRepository"
**Solution**: Check import statement
```java
// DO use:
import brettdansmith.drugdiary.domain.repository.MedicationRepository;

// NOT:
import brettdansmith.drugdiary.data.medication.MedicationRepository;
```

### Issue: ServiceLocator returns null
**Solution**: Make sure context is not null and pass application context
```java
ServiceLocator.getInstance(getApplicationContext()) // Good
ServiceLocator.getInstance(this) // Maybe null if fragment detached
```

### Issue: Data not saving
**Solution**: Check for exceptions and verify you called saveRecord
```java
try {
    repo.saveRecord(medication); // Don't forget this!
    Log.d("DEBUG", "Saved successfully");
} catch (Exception e) {
    Log.e("DEBUG", "Save failed: " + e.getMessage());
}
```

### Issue: "Old model not found"
**Solution**: Old model packages were deleted - update imports to use `domain.model.*`
```java
// OLD (deleted packages):
// import brettdansmith.drugdiary.model.medication.*;

// NEW (use these):
import brettdansmith.drugdiary.domain.model.medication.*;
```

---

## Testing

### Unit Test Example
```java
@RunWith(JUnit4.class)
public class MedicationRepositoryTest {
    private MedicationRepository repository;
    
    @Before
    public void setup() {
        // Use mock or test implementation
        repository = new MockMedicationRepository();
    }
    
    @Test
    public void testListRecords() throws Exception {
        List<MedicationRecord> records = repository.listRecords();
        assertNotNull(records);
        assertTrue(records.size() >= 0);
    }
}
```

### Mock Repository Example
```java
public class MockMedicationRepository implements MedicationRepository {
    @Override
    public List<MedicationRecord> listRecords() throws Exception {
        return Arrays.asList(
            new MedicationRecord(/* test data */)
        );
    }
    
    // ... implement other methods
}
```

---

## Next Steps

1. **Read the Full Documentation**:
   - `ui/README.md` - UI component patterns
   - `domain/README.md` - Domain layer details
   - `data/README.md` - Data layer implementation

2. **Explore the Code**:
   - Look at ServiceLocator implementation
   - Check repository interfaces
   - Study existing repository implementations

3. **Try It Out**:
   - Use ServiceLocator in a fragment
   - Load some data
   - Save a record
   - Check the logs

4. **Join Phase 6**:
   - Help migrate fragments to use new pattern
   - Write unit tests
   - Optimize performance

---

## Quick Reference

| Need | Use | Location |
|------|-----|----------|
| Load medications | `getMedicationRepository()` | `domain.repository` |
| Load diary entries | `getDiaryRepository()` | `domain.repository` |
| Check settings | `getSettingsRepository()` | `domain.repository` |
| Check interactions | `getDrugReferenceRepository()` | `domain.repository` |
| AI operations | `getAiService()` | `domain.repository` |
| Get repository | `ServiceLocator.getInstance(context)` | `domain.service` |
| Medication data | `MedicationRecord` | `domain.model.medication` |
| Diary data | `DiaryEntry` | `domain.model.diary` |
| AI config | `AiResolvedConfig` | `domain.model.ai` |

---

## Getting Help

1. **Check the README files** in each layer (ui/, domain/, data/)
2. **Review implemented examples** in existing fragments
3. **Run tests** to verify your code works
4. **Ask questions** - the architecture is designed to be straightforward!

---

**Last Updated**: May 15, 2026
**Version**: 1.0 (Post-Phase 5 Refactoring)
**Maintained By**: Development Team

