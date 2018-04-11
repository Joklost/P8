package dk.aau.sw805f18.ar.main;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.ArActivity;
import dk.aau.sw805f18.ar.databinding.ActivityMainBinding;
import dk.aau.sw805f18.ar.fragments.HomeFragment;
import dk.aau.sw805f18.ar.fragments.MapFragment;

public class MainActivity extends AppCompatActivity {
    private Date mBackPressed;
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentOpener fragmentOpener = FragmentOpener.getInstance();
        fragmentOpener.init(this);
        fragmentOpener.open(new HomeFragment());

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.navView.setNavigationItemSelectedListener(
                menuItem -> {
                    switch (menuItem.getTitle().toString()) {
                        case "AR":
                            startActivity(new Intent(this, ArActivity.class));
                            break;
                        case "Map":
                            FragmentOpener.getInstance().open(new MapFragment());
                            break;
                    }

                    mBinding.drawer.closeDrawers();
                    return true;
                });

        setSupportActionBar(mBinding.toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
        }
        setDrawerInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (mBinding.drawer.isDrawerOpen(GravityCompat.START)) {
            mBinding.drawer.closeDrawer(GravityCompat.START);
            return;
        }
        if (FragmentOpener.getInstance().close()) {
            super.onBackPressed();
            return;
        }

        Date now = new Date();
        if (mBackPressed == null || now.getTime() - mBackPressed.getTime() > 2000) {
            mBackPressed = now;
            Toast.makeText(this, R.string.double_press_back_exit, Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mBinding.drawer.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setDrawerInfo() {
        NavigationView navView = findViewById(R.id.nav_view);
        View headerView = navView.getHeaderView(0);
        TextView navName = headerView.findViewById(R.id.drawer_header_name_textview);
        TextView navTroop = headerView.findViewById(R.id.drawer_header_troop_textview);
        navName.setText("Jens Birkbak");
        navTroop.setText("Blue birds");
    }
}

