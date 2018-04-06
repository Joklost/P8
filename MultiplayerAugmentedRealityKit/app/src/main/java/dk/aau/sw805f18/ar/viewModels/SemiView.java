package dk.aau.sw805f18.ar.viewModels;

import android.support.v4.app.Fragment;

public class SemiView {
    private final Fragment mFragment;
    private final ViewModel mViewModel;

    public SemiView(Fragment fragment, ViewModel viewModel) {
        mFragment = fragment;
        mViewModel = viewModel;
    }

    public String getFragmentTag() {
        return mFragment.getTag();
    }

    public final void close() {
        mViewModel.onBackPressed();
    }

    public Fragment getFragment() {
        return mFragment;
    }
}
