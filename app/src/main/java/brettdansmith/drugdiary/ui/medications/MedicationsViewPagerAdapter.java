package brettdansmith.drugdiary.ui.medications;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MedicationsViewPagerAdapter extends FragmentStateAdapter {

    public MedicationsViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MedicationsViewModel.FilterMode[] modes = MedicationsViewModel.FilterMode.values();
        if (position < 0 || position >= modes.length) {
            throw new IllegalStateException("Unexpected position: " + position);
        }
        return MedicationListFragment.newInstance(modes[position].name());
    }

    @Override
    public int getItemCount() {
        return MedicationsViewModel.FilterMode.values().length;
    }
}
