package dk.aau.sw805f18.ar.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import dk.aau.sw805f18.ar.MainFragment;
import dk.aau.sw805f18.ar.MapFragment;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.ArActivity;

public class MainActivity extends AppCompatActivity {
    private boolean mDoubleBackToExitPressedOnce = false;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainFragment mainFragment = new MainFragment();

        setContentView(R.layout.activity_main);
        FragmentTransaction fragmentTransactorInitial = getSupportFragmentManager().beginTransaction();
        fragmentTransactorInitial.add(R.id.fragment_container, mainFragment).commit();
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem menuItem) {
                // set item as selected to persist highlight
                FragmentTransaction fragmentTransactor = getSupportFragmentManager().beginTransaction();
                menuItem.setChecked(!menuItem.isChecked());

                switch (menuItem.getTitle().toString()) {
                    case "AR":
                        startAr();
                    case "Map":
                        MapFragment mapFragment = new MapFragment();
                        fragmentTransactor.replace(R.id.fragment_container, mapFragment).addToBackStack(null);
                }

                mDrawerLayout.closeDrawers();
                fragmentTransactor.commit();
                return true;
                }
            });


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout layout = findViewById(R.id.drawer_layout);
        if (layout.isDrawerOpen(GravityCompat.START)) {
            layout.closeDrawer(GravityCompat.START);
            return;
        } else if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.double_press_back_exit, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                mDoubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startAr() {
        Intent intent = new Intent(this, ArActivity.class);
        startActivity(intent);
    }
}
