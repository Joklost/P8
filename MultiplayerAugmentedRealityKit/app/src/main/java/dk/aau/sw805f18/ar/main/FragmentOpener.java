package dk.aau.sw805f18.ar.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import dk.aau.sw805f18.ar.R;

public class FragmentOpener {
    private static FragmentOpener mInstance;
    private android.support.v4.app.FragmentManager mFragmentManager;
    private Boolean mHasOpenFragment = false;

    public static FragmentOpener getInstance() {
        if (mInstance == null)
            mInstance = new FragmentOpener();
        return mInstance;
    }

    public boolean close() {
        if (mHasOpenFragment){
            mHasOpenFragment = false;
            return true;
        }
        return false;
    }

    public void open(Fragment fragment) {
        if (mHasOpenFragment) {

            FragmentTransaction fragmentTransactor = mFragmentManager.beginTransaction();
            fragmentTransactor.replace(R.id.fragment_container, fragment, fragment.getTag())
                    .addToBackStack(null)
                    .commit();
        }
        else {
            mHasOpenFragment = true;
            FragmentTransaction fragmentTransactorInitial = mFragmentManager.beginTransaction();
            fragmentTransactorInitial.add(R.id.fragment_container, fragment, fragment.getTag())
                    .commit();
        }
    }



    public void init(MainActivity mainActivity) {
        mFragmentManager = mainActivity.getSupportFragmentManager();
    }
}
