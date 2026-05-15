# Data Layer Architecture

## Overview
The data layer handles all data persistence and retrieval. It bridges between domain interfaces and underlying storage mechanisms (encrypted profile store, shared preferences, network APIs).

## Directory Structure

```
data/
├── medication/         # Medication data operations
├── diary/             # Diary entry data operations
├── reference/         # Drug reference data (caching, lookups)
├── settings/          # Global settings (SharedPreferences)
├── profile/           # Profile data (encrypted vault)
└── assistant/         # Assistant state persistence
```

## Repository Implementations

### Adapter Pattern
Each domain repository interface has a corresponding implementation that wraps existing data layer classes:

```
Domain Interface (domain.repository.*)
    ↓
Implementation Adapter (data.*.RepositoryImpl)
    ↓
Existing Data Layer (data.*.Repository)
    ↓
Underlying Storage (EncryptedProfileStore, SharedPreferences, Network)
```

### Example: MedicationRepositoryImpl
```java
public final class MedicationRepositoryImpl implements MedicationRepository {
    private final brettdansmith.drugdiary.data.medication.MedicationRepository delegate;
    
    @Override
    public List<MedicationRecord> listRecords() throws Exception {
        return delegate.listRecords();
    }
    
    @Override
    public void saveRecord(MedicationRecord record) throws Exception {
        delegate.upsertRecord(record);
    }
}
```

## Storage Systems

### 1. Encrypted Profile Store
**Purpose**: Sensitive profile data (medications, diary, custom settings)

```java
// Load profile data
JSONObject data = EncryptedProfileStore.loadProfileData(context);

// Save profile data
EncryptedProfileStore.saveProfileData(context, data);
```

**Features**
- End-to-end encryption
- Biometric/PIN protection
- Automatic encryption/decryption
- Profile-specific isolation

**Structure**
```json
{
  "profile": {
    "id": "profile_xxx",
    "name": "User Name",
    "avatar": {}
  },
  "trackers": {
    "medications": [...],
    "diary_entries": [...],
    "dose_logs": [...]
  }
}
```

### 2. SharedPreferences
**Purpose**: Global app settings (non-sensitive)

```java
SettingsRepository settings = new SettingsRepository(context);

// Get/set theme
settings.setThemeMode(AppCompatDelegate.MODE_NIGHT_YES);

// Get/set AI provider
settings.setAiProvider(AiProvider.OPENAI);
settings.setProviderApiKey(AiProvider.OPENAI, apiKey);
```

**Data Stored**
- Theme preference
- Language selection
- Unit system
- AI provider settings
- API keys
- Feature flags

### 3. Network APIs
**Purpose**: External data sources (drug databases, AI providers)

```
Network Layer
├── ai/
│   ├── AssistantApiClient (unified AI interface)
│   ├── OpenAiProviderClient
│   ├── DeepseekProviderClient
│   └── (other AI providers)
└── reference/
    ├── PubChemDataSource
    ├── RxNormDataSource
    ├── OpenFdaDataSource
    └── ChemblDataSource
```

## Data Flow Patterns

### Read Operation
```
UI Fragment
  ↓ calls repository
ServiceLocator.getMedicationRepository()
  ↓ returns MedicationRepository interface
Domain Repository (interface)
  ↓ delegates to
RepositoryImpl (data layer)
  ↓ calls
Underlying Repository
  ↓ reads from
EncryptedProfileStore / SharedPreferences
  ↓ returns
Deserialized Domain Model Objects
  ↓ returns to
UI Fragment (updates display)
```

### Write Operation
```
UI Fragment (creates/edits data)
  ↓ creates
Domain Model (MedicationRecord)
  ↓ calls
repository.saveRecord(record)
  ↓ RepositoryImpl calls
delegate.upsertRecord(record)
  ↓ Underlying repository
record.toJson() → JSON
  ↓ persists to
EncryptedProfileStore
  ↓ encrypts and saves
Device Storage
```

## Key Classes

### EncryptedProfileStore
```java
// Load encrypted profile
JSONObject data = EncryptedProfileStore.loadProfileData(context);

// Save encrypted profile
EncryptedProfileStore.saveProfileData(context, data);

// Get current profile ID
String profileId = EncryptedProfileStore.getCurrentProfileId(context);

// Switch profiles
EncryptedProfileStore.setCurrentProfileId(context, profileId);
```

### SettingsRepository (data layer)
```java
SettingsRepository settings = new SettingsRepository(context);

// Get current settings state
SettingsState state = settings.getState();

// Modify settings
settings.setThemeMode(theme);
settings.setAiProvider(provider);
settings.setProviderApiKey(provider, apiKey);
```

### ProfileJson (utility)
```java
// Safe JSON object access
JSONObject trackers = ProfileJson.object(data, ProfileJson.KEY_TRACKERS);

// Safe JSON array access
JSONArray medications = ProfileJson.array(trackers, ProfileJson.KEY_MEDICATIONS);
```

## Implementing New Repositories

### Step 1: Create Domain Interface
```java
// domain/repository/FooRepository.java
public interface FooRepository {
    FooData getData(String id) throws Exception;
    void saveData(FooData data) throws Exception;
}
```

### Step 2: Check Existing Data Layer
```java
// data/foo/FooRepository.java (existing)
public final class FooRepository {
    public FooData getData(String id) { ... }
    public void saveData(FooData data) { ... }
}
```

### Step 3: Create Implementation Adapter
```java
// data/foo/FooRepositoryImpl.java
public final class FooRepositoryImpl implements FooRepository {
    private final brettdansmith.drugdiary.data.foo.FooRepository delegate;
    
    public FooRepositoryImpl(Context context) {
        this.delegate = new brettdansmith.drugdiary.data.foo.FooRepository(context);
    }
    
    @Override
    public FooData getData(String id) throws Exception {
        return delegate.getData(id);
    }
    
    @Override
    public void saveData(FooData data) throws Exception {
        delegate.saveData(data);
    }
}
```

### Step 4: Register in ServiceLocator
```java
// domain/service/ServiceLocator.java
private FooRepository fooRepo;

// In constructor
this.fooRepo = new FooRepositoryImpl(context);

// Add public getter
public FooRepository getFooRepository() {
    return fooRepo;
}
```

### Step 5: Use in UI
```java
FooRepository repo = ServiceLocator.getInstance(context).getFooRepository();
FooData data = repo.getData(id);
```

## Caching Strategies

### In-Memory Cache
```java
private Map<String, CachedData> cache = new HashMap<>();

public Data getData(String key) {
    if (cache.containsKey(key)) {
        return cache.get(key);
    }
    Data data = loadFromStorage(key);
    cache.put(key, data);
    return data;
}
```

### Time-Based Cache Invalidation
```java
private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours

public Data getCachedData(String key) {
    Long lastUpdate = lastUpdateTimes.get(key);
    if (lastUpdate != null && 
        System.currentTimeMillis() - lastUpdate < CACHE_DURATION_MS) {
        return cache.get(key);
    }
    return refreshCache(key);
}
```

## Error Handling

### Network Errors
```java
public JSONObject lookup(String query) throws Exception {
    try {
        return performNetworkRequest(query);
    } catch (IOException e) {
        // Handle network error
        throw new Exception("Network unavailable: " + e.getMessage());
    } catch (TimeoutException e) {
        // Handle timeout
        throw new Exception("Request timeout: " + e.getMessage());
    }
}
```

### Storage Errors
```java
public void saveData(ProfileData data) throws Exception {
    try {
        JSONObject json = data.toJson();
        EncryptedProfileStore.saveProfileData(context, json);
    } catch (JSONException e) {
        throw new Exception("Data format error: " + e.getMessage());
    } catch (Exception e) {
        throw new Exception("Storage error: " + e.getMessage());
    }
}
```

## Performance Considerations

### 1. Async Operations
Use ExecutorService for long-running operations:

```java
private ExecutorService executor = Executors.newFixedThreadPool(2);

public void loadDataAsync(Callback callback) {
    executor.execute(() -> {
        try {
            Data data = loadData();
            callback.onSuccess(data);
        } catch (Exception e) {
            callback.onError(e);
        }
    });
}
```

### 2. Batch Operations
```java
public void saveManyRecords(List<Record> records) throws Exception {
    JSONObject data = EncryptedProfileStore.loadProfileData(context);
    JSONArray array = ProfileJson.array(data, KEY_RECORDS);
    
    for (Record record : records) {
        array.put(record.toJson());
    }
    
    EncryptedProfileStore.saveProfileData(context, data);
}
```

### 3. Pagination
```java
public List<Item> getItems(int offset, int limit) {
    List<Item> items = loadAll();
    return items.subList(offset, Math.min(offset + limit, items.size()));
}
```

## Security Best Practices

### 1. API Key Management
- Store in SettingsRepository (SharedPreferences with MODE_PRIVATE)
- Never log full API keys
- Sanitize in error messages using AiDebugMetadata

### 2. Encryption
- Profile data encrypted by EncryptedProfileStore
- Automatic decryption on load
- Automatic encryption on save

### 3. Access Control
- Validate user has active profile
- Check permissions before data access
- Audit data access (if needed)

### 4. Data Validation
```java
public void saveRecord(Record record) throws Exception {
    if (record == null || record.id.isEmpty()) {
        throw new IllegalArgumentException("Invalid record data");
    }
    // Proceed with save
}
```

## Testing Data Layer

### Mock Repository
```java
public class MockFooRepository implements FooRepository {
    @Override
    public FooData getData(String id) throws Exception {
        return new FooData(id, "Test Data");
    }
}
```

### Integration Test
```java
@RunWith(JUnit4.class)
public class FooRepositoryTest {
    private FooRepository repository;
    
    @Before
    public void setup(Context context) {
        repository = new FooRepositoryImpl(context);
    }
    
    @Test
    public void testSaveAndLoad() throws Exception {
        FooData original = new FooData("id", "data");
        repository.saveData(original);
        
        FooData loaded = repository.getData("id");
        assertEquals(original.value, loaded.value);
    }
}
```

## Current Status

✅ SettingsRepository implemented (wraps existing settings)
✅ MedicationRepository implemented (wraps existing medication repo)
✅ DiaryRepository implemented (wraps existing diary repo)
✅ DrugReferenceRepository implemented (wraps reference APIs)
✅ All registered in ServiceLocator

⏳ UI fragments transitioning to use repositories
⏳ Deprecating direct repository access from UI

