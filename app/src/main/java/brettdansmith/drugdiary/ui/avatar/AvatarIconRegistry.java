package brettdansmith.drugdiary.ui.avatar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import brettdansmith.drugdiary.R;

public final class AvatarIconRegistry {
    private static final List<AvatarIconOption> OPTIONS;

    static {
        List<AvatarIconOption> options = new ArrayList<>();
        options.add(new AvatarIconOption("initials", R.string.avatar_option_initials, R.string.avatar_initials));
        options.add(new AvatarIconOption("male_head", R.string.avatar_icon_male_head, R.string.avatar_icon_male_head_desc));
        options.add(new AvatarIconOption("female_head", R.string.avatar_icon_female_head, R.string.avatar_icon_female_head_desc));
        options.add(new AvatarIconOption("eye", R.string.avatar_icon_eye, R.string.avatar_icon_eye_desc));
        options.add(new AvatarIconOption("portal", R.string.avatar_icon_portal, R.string.avatar_icon_portal_desc));
        options.add(new AvatarIconOption("spiral", R.string.avatar_icon_spiral, R.string.avatar_icon_spiral_desc));
        options.add(new AvatarIconOption("fingerprint", R.string.avatar_icon_fingerprint, R.string.avatar_icon_fingerprint_desc));
        options.add(new AvatarIconOption("snake", R.string.avatar_icon_snake, R.string.avatar_icon_snake_desc));
        options.add(new AvatarIconOption("butterfly", R.string.avatar_icon_butterfly, R.string.avatar_icon_butterfly_desc));
        options.add(new AvatarIconOption("owl", R.string.avatar_icon_owl, R.string.avatar_icon_owl_desc));
        options.add(new AvatarIconOption("wolf", R.string.avatar_icon_wolf, R.string.avatar_icon_wolf_desc));
        options.add(new AvatarIconOption("deer", R.string.avatar_icon_deer, R.string.avatar_icon_deer_desc));
        options.add(new AvatarIconOption("raven", R.string.avatar_icon_raven, R.string.avatar_icon_raven_desc));
        options.add(new AvatarIconOption("fox", R.string.avatar_icon_fox, R.string.avatar_icon_fox_desc));
        options.add(new AvatarIconOption("tree", R.string.avatar_icon_tree, R.string.avatar_icon_tree_desc));
        options.add(new AvatarIconOption("mushroom", R.string.avatar_icon_mushroom, R.string.avatar_icon_mushroom_desc));
        options.add(new AvatarIconOption("mountain", R.string.avatar_icon_mountain, R.string.avatar_icon_mountain_desc));
        options.add(new AvatarIconOption("moon", R.string.avatar_icon_moon, R.string.avatar_icon_moon_desc));
        options.add(new AvatarIconOption("sun", R.string.avatar_icon_sun, R.string.avatar_icon_sun_desc));
        options.add(new AvatarIconOption("star", R.string.avatar_icon_star, R.string.avatar_icon_star_desc));
        options.add(new AvatarIconOption("planet", R.string.avatar_icon_planet, R.string.avatar_icon_planet_desc));
        options.add(new AvatarIconOption("cloud", R.string.avatar_icon_cloud, R.string.avatar_icon_cloud_desc));
        options.add(new AvatarIconOption("wave", R.string.avatar_icon_wave, R.string.avatar_icon_wave_desc));
        options.add(new AvatarIconOption("flame", R.string.avatar_icon_flame, R.string.avatar_icon_flame_desc));
        options.add(new AvatarIconOption("crystal", R.string.avatar_icon_crystal, R.string.avatar_icon_crystal_desc));
        options.add(new AvatarIconOption("lotus", R.string.avatar_icon_lotus, R.string.avatar_icon_lotus_desc));
        options.add(new AvatarIconOption("sprout", R.string.avatar_icon_sprout, R.string.avatar_icon_sprout_desc));
        options.add(new AvatarIconOption("lantern", R.string.avatar_icon_lantern, R.string.avatar_icon_lantern_desc));
        options.add(new AvatarIconOption("compass", R.string.avatar_icon_compass, R.string.avatar_icon_compass_desc));
        options.add(new AvatarIconOption("anchor", R.string.avatar_icon_anchor, R.string.avatar_icon_anchor_desc));
        OPTIONS = Collections.unmodifiableList(options);
    }

    private AvatarIconRegistry() {
    }

    @NonNull
    public static List<AvatarIconOption> all() {
        return OPTIONS;
    }

    public static boolean isValidId(@Nullable String iconId) {
        return findById(iconId) != null;
    }

    @Nullable
    public static AvatarIconOption findById(@Nullable String iconId) {
        if (iconId == null || iconId.trim().isEmpty()) {
            return null;
        }
        for (AvatarIconOption option : OPTIONS) {
            if (option.id.equals(iconId)) {
                return option;
            }
        }
        return null;
    }

    public static final class AvatarIconOption {
        @NonNull public final String id;
        public final int labelRes;
        public final int contentDescriptionRes;

        AvatarIconOption(@NonNull String id, int labelRes, int contentDescriptionRes) {
            this.id = id;
            this.labelRes = labelRes;
            this.contentDescriptionRes = contentDescriptionRes;
        }
    }
}
