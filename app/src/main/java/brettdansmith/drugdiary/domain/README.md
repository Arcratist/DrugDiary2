# Domain Layer Architecture

## Overview
The domain layer contains business logic, repository interfaces, and use cases. It is completely independent of the Android framework and can be tested in pure JUnit tests.

## Directory Structure

```
domain/
├── repository/         # Repository interfaces (contracts)
├── service/           # Service locator for DI
├── model/             # Domain models (DTOs, value objects)
│   ├── ai/
│   ├── diary/
│   ├── medication/
│   ├── interaction/
│   └── resources/
├── assistant/         # Assistant logic
├── medication/        # Medication-related logic
├── validation/        # Validation rules
├── profile/          # Profile management logic
└── units/            # Unit conversion logic
```

## Repository Interface Pattern

Each domain repository interface defines contracts that implementations must fulfill:

### Example: MedicationRepository
```java
public interface MedicationRepository {
    List<MedicationRecord> listRecords() throws Exception;
    MedicationRecord getRecord(String id) throws Exception;
    void saveRecord(MedicationRecord record) throws Exception;
    void deleteRecord(String id) throws Exception;
    int count() throws Exception;
}
```

### Implementations
```
data/medication/MedicationRepositoryImpl.java
    └─ wraps brettdansmith.drugdiary.data.medication.MedicationRepository
```

## Service Locator Pattern

Centralized dependency injection without external frameworks:

```java
ServiceLocator.getInstance(context)
    .getMedicationRepository()
    .getDiaryRepository()
    .getSettingsRepository()
    .getDrugReferenceRepository()
    .getAiService()
```

### Initialization
```java
// In Application or MainActivity
ServiceLocator.getInstance(getApplicationContext());
```

### Thread Safety
- Uses double-checked locking pattern
- Safe for multi-threaded access
- Can be reset for testing: `ServiceLocator.reset()`

## Domain Models

### Characteristics
- **Immutable**: Final fields, no setters
- **Framework-agnostic**: No Android dependencies
- **Serializable**: Can convert to/from JSON
- **Type-safe**: Use enums and sealed types where appropriate

### Example: MedicationRecord
```java
public final class MedicationRecord {
    public final String id;
    public final String name;
    public final String canonicalName;
    public final String strength;
    public final String form;
    public final String route;
    public final String notes;
    public final MedicationCategory category;
    public final LinkedHashSet<MedicationCategory> categories;
    public final boolean active;
    public final boolean favorite;
    public final boolean saved;
    public final boolean prn;
    public final MedicationSchedule schedule;
    public final MedicationInventory inventory;
    public final LinkedHashSet<String> aliases;
    public final long createdAt;
    public final long updatedAt;
    
    public MedicationRecord(/* constructor parameters */) {
        // Constructor code
    }
    
    public JSONObject toJson() throws JSONException {
        // Serialize to JSON
    }
    
    public static MedicationRecord fromJson(JSONObject json) {
        // Deserialize from JSON
    }
}
```

## Business Logic

### Assistant Logic
- `AssistantCommandRegistry`: Parse and execute special commands
- `AssistantCommandParser`: Parse command syntax
- `AssistantContextBuilder`: Build context for AI requests

### Medication Logic
- `MedicationCatalog`: Canonical medication names
- `MedicationQueryResolver`: Resolve medication queries

### Validation Logic
- `ProfileValidator`: Validate profile data
- `MedicationValidator`: Validate medication data

### Unit Conversion
- `UnitPreferences`: Handle unit system preferences
- Unit conversion utilities for measurements

## Error Handling

### Exception Hierarchy
```
Exception
├─ IOException (network/storage errors)
├─ JSONException (parsing errors)
├─ IllegalArgumentException (invalid inputs)
└─ UnsupportedOperationException (not implemented)
```

### Pattern
```java
public class Repository implements DomainRepository {
    @Override
    public Object getData(String id) throws Exception {
        try {
            // Perform operation
        } catch (IOException e) {
            throw new Exception("Storage error: " + e.getMessage());
        }
    }
}
```

## Testing

### Unit Test Setup
```java
@RunWith(JUnit4.class)
public class MedicationRepositoryTest {
    private MedicationRepository repository;
    
    @Before
    public void setup() {
        // Create mock or test implementation
    }
    
    @Test
    public void testListRecords() throws Exception {
        List<MedicationRecord> records = repository.listRecords();
        assertNotNull(records);
    }
}
```

### Mock Implementation
```java
public class MockMedicationRepository implements MedicationRepository {
    @Override
    public List<MedicationRecord> listRecords() throws Exception {
        return Arrays.asList(
            new MedicationRecord(/* test data */)
        );
    }
}
```

## AI Service Interface

The AI service provides a unified interface for different AI providers:

```java
public interface AiService {
    String sendMessage(String message) throws Exception;
    void sendMessageStreaming(String message, StreamChunkCallback onChunk) 
        throws Exception;
    List<AiModelInfo> getAvailableModels() throws Exception;
    AiProvider getCurrentProvider();
    void setCurrentProvider(AiProvider provider);
    boolean testConnection();
    
    interface StreamChunkCallback {
        void onChunk(String chunk);
        void onError(String error);
        void onComplete();
    }
}
```

## Data Flow

### Example: Loading Medications
```
1. Fragment calls: repository.listRecords()
2. Repository (domain interface) delegates to:
3. RepositoryImpl (data layer) calls:
4. UnderlyingRepository (existing data layer) uses:
5. EncryptedProfileStore (storage) returns:
6. JSON data → Deserialized to MedicationRecord objects
7. Returned to Fragment for display
```

### Example: Saving Medication
```
1. Fragment gets data from UI
2. Creates MedicationRecord domain model
3. Calls: repository.saveRecord(record)
4. Repository converts to JSON via toJson()
5. RepositoryImpl delegates to underlying implementation
6. Data persisted to encrypted storage
```

## Best Practices

### 1. Use Repository Interfaces
- Define contracts, not implementations
- Easy to mock for testing
- Swap implementations without changing UI

### 2. Keep Models Immutable
- Prevents accidental mutations
- Thread-safe by default
- Clear intent of data

### 3. Error Handling
- Throw meaningful exceptions
- Document what exceptions can be thrown
- Handle errors gracefully in UI

### 4. Layering
- Domain layer has NO dependencies on UI or Android framework
- Data layer implements domain interfaces
- UI layer uses domain interfaces

### 5. Testing
- Test domain logic independently
- Mock repositories in unit tests
- Use integration tests for data flow

## Migration to Repository Pattern

### Current Status
✅ Repository interfaces created (domain layer)
✅ Service implementations created (data layer adapters)
✅ ServiceLocator set up for dependency injection
⏳ Gradual migration of UI fragments to use repositories

### Next Steps
1. Update existing fragments to use ServiceLocator
2. Move business logic to ViewModels
3. Add ViewModel implementations for each fragment
4. Write unit tests for repositories
5. Write integration tests for data flow

## Security Considerations

### Profile Sensitivity
- Always use ServiceLocator to access repositories
- Repositories handle encryption/decryption
- UI layer never sees raw encrypted data

### Data Validation
- Validate all inputs in domain layer
- Return meaningful error messages
- Don't expose internal structure

### API Keys
- Stored in SettingsRepository (SharedPreferences)
- Never logged or exposed
- Sanitized in error messages via AiDebugMetadata

