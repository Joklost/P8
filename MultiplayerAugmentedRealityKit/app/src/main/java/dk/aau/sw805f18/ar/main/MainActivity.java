package dk.aau.sw805f18.ar.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.argame.ArGameActivity;
import dk.aau.sw805f18.ar.fragments.AboutFragment;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;

import dk.aau.sw805f18.ar.fragments.HomeFragment;
import dk.aau.sw805f18.ar.fragments.ProfileFragment;
import dk.aau.sw805f18.ar.fragments.SettingsFragment;
import dk.aau.sw805f18.ar.fragments.StatsFragment;

public class MainActivity extends AppCompatActivity {
    private NavigationView mNavigationView;
    private DrawerLayout mDrawer;
    private TextView mName, mTroop;
    private Toolbar mToolbar;
    private FragmentOpener mFragmentOpener;

    // Fragment tags
    public static String CURRENT_TAG = HomeFragment.TAG_HOME;
    public static String CURRENT_FRAGMENT;

    public static int drawerItemIndex = 0;

    private Date mBackPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawer = findViewById(R.id.drawer);
        mNavigationView = findViewById(R.id.nav_view);

        View navHeader = mNavigationView.getHeaderView(0);
        mName = navHeader.findViewById(R.id.drawer_header_name_textview);
        mTroop = navHeader.findViewById(R.id.drawer_header_troop_textview);

        loadNavHeader();
        setUpNavigationView();

        mFragmentOpener = FragmentOpener.getInstance();
        mFragmentOpener.init(this);

        if (savedInstanceState == null) {
            drawerItemIndex = 0;
            CURRENT_TAG = HomeFragment.TAG_HOME;
            getSupportActionBar().setTitle(getString(R.string.home_title));
            loadHomeFragment();
        }

        SyncServiceHelper.init(this, syncService -> {
            syncService.setDeviceLocation(DeviceLocation.getInstance(this));
            syncService.init(this);

        });
    }

    private void loadHomeFragment() {
        selectNavMenu();

        Fragment active = getSupportFragmentManager().findFragmentByTag(CURRENT_TAG);

        if (active != null && active.isVisible()) {
            mDrawer.closeDrawers();
            return;
        }

        mFragmentOpener.open(getFragment(), CURRENT_TAG);

        mDrawer.closeDrawers();
        invalidateOptionsMenu();
    }

    private Fragment getFragment() {
        switch (drawerItemIndex) {
            case 0:
                return new HomeFragment();
            case 1:
                return new ProfileFragment();
            case 2:
                return new StatsFragment();
            case 3:
                return new SettingsFragment();
            case 4:
                return new AboutFragment();
            default:
                return new HomeFragment();
        }
    }

    private void selectNavMenu() {
        mNavigationView.getMenu().getItem(drawerItemIndex).setChecked(true);
    }

    private void setUpNavigationView() {
        mNavigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    drawerItemIndex = 0;
                    CURRENT_TAG = HomeFragment.TAG_HOME;
                    break;
                case R.id.nav_profile:
                    drawerItemIndex = 1;
                    CURRENT_TAG = ProfileFragment.TAG_PROFILE;
                    break;
                case R.id.nav_stats:
                    drawerItemIndex = 2;
                    CURRENT_TAG = StatsFragment.TAG_STATS;
                    break;
                case R.id.nav_settings:
                    drawerItemIndex = 3;
                    CURRENT_TAG = SettingsFragment.TAG_SETTINGS;
                    break;
                case R.id.nav_about:
                    drawerItemIndex = 4;
                    CURRENT_TAG = AboutFragment.TAG_ABOUT;
                    break;
                case R.id.nav_ar:
                    startActivity(new Intent(this, ArGameActivity.class));
                default:
                    drawerItemIndex = 0;
            }

            item.setChecked(!item.isChecked());
            loadHomeFragment();
            return true;
        });

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.openDrawer, R.string.closeDrawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                syncState();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                syncState();
            }
        };

        mDrawer.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private void loadNavHeader() {
        // TODO: change to load user information after login
        mName.setText("Jens Birkbak");
        mTroop.setText("Bluebirds Troop");
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawers();
            return;
        }

        if (drawerItemIndex != 0) {
            drawerItemIndex = 0;
            CURRENT_TAG = HomeFragment.TAG_HOME;
            loadHomeFragment();
            return;
        }

        if (CURRENT_FRAGMENT.equals(HomeFragment.TAG_HOME)) {
            Date now = new Date();
            if (mBackPressed == null || now.getTime() - mBackPressed.getTime() > 2000) {
                mBackPressed = now;
                Toast.makeText(this, R.string.double_press_back_exit, Toast.LENGTH_SHORT).show();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SyncServiceHelper.deinit(this);
    }
}

