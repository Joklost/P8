package dk.aau.sw805f18.ar.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.viewModels.SemiView;
import dk.aau.sw805f18.ar.viewModels.ViewModel;

public class SemiViewManager {
    private static SemiViewManager mInstance;
    private android.support.v4.app.FragmentManager mFragmentManager;
    private Boolean mHasOpenFragment = false;
    private SemiView mCurrent;

    public static SemiViewManager getInstance() {
        if (mInstance == null)
            mInstance = new SemiViewManager();
        return mInstance;
    }

    public boolean close() {
        if (mCurrent != null){
            mCurrent.close();
            mCurrent = null;
            return true;
        }
        return false;
    }

    public void open(Fragment fragment, ViewModel viewModel) {
        SemiView semiView = new SemiView(fragment, viewModel);
        if (mHasOpenFragment) {

            FragmentTransaction fragmentTransactor = mFragmentManager.beginTransaction();
            fragmentTransactor.replace(R.id.fragment_container, semiView.getFragment(), semiView.getFragmentTag())
                    .addToBackStack(null)
                    .commit();
        }
        else {
            mHasOpenFragment = true;
            FragmentTransaction fragmentTransactorInitial = mFragmentManager.beginTransaction();
            fragmentTransactorInitial.add(R.id.fragment_container, semiView.getFragment(), semiView.getFragmentTag())
                    .commit();
        }
    }



    public void init(MainActivity mainActivity) {
        mFragmentManager = mainActivity.getSupportFragmentManager();
    }
}
