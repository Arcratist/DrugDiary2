package brettdansmith.drugdiary.ui.profile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import brettdansmith.drugdiary.domain.service.ServiceLocator;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;
import brettdansmith.drugdiary.data.profile.ProfileAvatarDataStore;
import brettdansmith.drugdiary.util.JsonUtils;
import brettdansmith.drugdiary.util.UnitConverter;

public class ProfileViewModel extends ViewModel {
    private final Context appContext;
    private final MutableLiveData<JSONObject> profileData = new MutableLiveData<>();
    private final MutableLiveData<ProfileAvatar> avatarLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ProfileViewModel(ServiceLocator serviceLocator) {
        this.appContext = serviceLocator.getContext();
    }

    public LiveData<JSONObject> getProfileData() {
        return profileData;
    }

    public LiveData<ProfileAvatar> getAvatar() {
        return avatarLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public void loadProfile() {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                profileData.postValue(data);
                if (data != null) {
                    JSONObject profile = data.optJSONObject(ProfileJson.KEY_PROFILE);
                    if (profile != null) {
                        avatarLiveData.postValue(ProfileAvatar.fromProfileJson(profile));
                    }
                }
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void saveProfile(JSONObject data, ProfileAvatar avatar, String profileName) {
        saveProfile(data, avatar, profileName, null);
    }

    public void saveProfile(JSONObject data, ProfileAvatar avatar, String profileName, Runnable onComplete) {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                ProfileAvatarDataStore.writeToData(data, avatar);
                EncryptedProfileStore.saveProfileData(appContext, data);
                ProfileAvatarDataStore.writeToPrefs(appContext, profileName, avatar);
                profileData.postValue(data);
                avatarLiveData.postValue(avatar);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
