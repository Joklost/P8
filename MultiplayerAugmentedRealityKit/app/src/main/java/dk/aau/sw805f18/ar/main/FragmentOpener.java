package dk.aau.sw805f18.ar.main;

import android.content.res.Resources;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.fragments.AboutFragment;
import dk.aau.sw805f18.ar.fragments.CreateCourseFragment;
import dk.aau.sw805f18.ar.fragments.FindCourseFragment;
import dk.aau.sw805f18.ar.fragments.HomeFragment;
import dk.aau.sw805f18.ar.fragments.LobbyFragment;
import dk.aau.sw805f18.ar.fragments.ProfileFragment;
import dk.aau.sw805f18.ar.fragments.SettingsFragment;
import dk.aau.sw805f18.ar.fragments.StatsFragment;

public class FragmentOpener {
    private static FragmentOpener mInstance;
    private android.support.v4.app.FragmentManager mFragmentManager;
    private Boolean mHasOpenFragment = false;
    private Handler mHandler;
    private ActionBar mActionBar;

    public static FragmentOpener getInstance() {
        if (mInstance == null)
            mInstance = new FragmentOpener();
        return mInstance;
    }

    public boolean close() {
        if (mHasOpenFragment) {
            mHasOpenFragment = false;
            return true;
        }
        return false;
    }

    public void open(Fragment fragment, String tag) {
        if (mHasOpenFragment) {
            Runnable mPendingRunnable = () -> {
                FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            };

            if (mPendingRunnable != null) mHandler.post(mPendingRunnable);
        } else {
            mHasOpenFragment = true;
            FragmentTransaction fragmentTransactorInitial = mFragmentManager.beginTransaction();
            fragmentTransactorInitial.add(R.id.fragment_container, fragment, tag)
                    .commit();
        }
    }

    public void init(MainActivity mainActivity) {
        mFragmentManager = mainActivity.getSupportFragmentManager();
        mHandler = new Handler();
        mActionBar = mainActivity.getSupportActionBar();
        mFragmentManager.addOnBackStackChangedListener(() -> {
            String title = "";

            Resources r = mainActivity.getResources();
            switch (MainActivity.CURRENT_FRAGMENT) {
                case HomeFragment.TAG_HOME:
                    title = r.getString(R.string.home_title);
                    break;
                case FindCourseFragment.TAG_FIND:
                    title = r.getString(R.string.find_course_title);
                    break;
                case CreateCourseFragment.TAG_CREATE:
                    title = r.getString(R.string.create_course_title);
                    break;
                case LobbyFragment.TAG_LOBBY:
                    title = r.getString(R.string.lobby_title);
                    break;
                case AboutFragment.TAG_ABOUT:
                    title = r.getString(R.string.about_title);
                    break;
                case ProfileFragment.TAG_PROFILE:
                    title = r.getString(R.string.profile_title);
                    break;
                case SettingsFragment.TAG_SETTINGS:
                    title = r.getString(R.string.settings_title);
                    break;
                case StatsFragment.TAG_STATS:
                    title = r.getString(R.string.stats_title);
                    break;
                default:
                    break;
            }
            mActionBar.setTitle(title);
        });
    }
}
