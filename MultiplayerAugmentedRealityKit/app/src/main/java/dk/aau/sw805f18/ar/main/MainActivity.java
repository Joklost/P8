package dk.aau.sw805f18.ar.main;

import android.content.Intent;
import android.net.Uri;
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

import dk.aau.sw805f18.ar.MainFragment;
import dk.aau.sw805f18.ar.MapFragment;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.ArActivity;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener, MapFragment.OnFragmentInteractionListener {

    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainFragment mainFragment = new MainFragment();

        setContentView(R.layout.activity_main);
        FragmentTransaction fragmentTransactor = getSupportFragmentManager().beginTransaction();
        fragmentTransactor.add(R.id.fragment_container ,mainFragment).commit();
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        FragmentTransaction fragmentTransactorMmap  = getSupportFragmentManager().beginTransaction();
                        if(menuItem.isChecked()){
                            menuItem.setChecked(false);
                        }
                        else{
                            menuItem.setChecked(true);
                        }
                        switch (menuItem.getTitle().toString()) {
                            case "AR":
                                startAr();
                            case "Map":
                                MapFragment mapFragment = new MapFragment();
                                fragmentTransactorMmap.replace(R.id.fragment_container, mapFragment).addToBackStack(null).commit();
                        }
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

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

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
