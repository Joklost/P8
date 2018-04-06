package dk.aau.sw805f18.ar.main;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Date;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.ArActivity;
import dk.aau.sw805f18.ar.databinding.ActivityMainBinding;
import dk.aau.sw805f18.ar.fragments.FindCourseFragment;
import dk.aau.sw805f18.ar.fragments.MapFragment;
import dk.aau.sw805f18.ar.viewModels.FindCourseViewModel;
import dk.aau.sw805f18.ar.viewModels.MapViewModel;

public class MainActivity extends AppCompatActivity {
    private Date mBackPressed;
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SemiViewManager svm = SemiViewManager.getInstance();
        svm.init(this);
        svm.open(new FindCourseFragment(), new FindCourseViewModel());

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.navigation.setNavigationItemSelectedListener(
                menuItem -> {
                    switch (menuItem.getTitle().toString()) {
                        case "AR":
                            startAr();
                        case "Map":
                            SemiViewManager.getInstance().open(new MapFragment(), new MapViewModel());
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
    }

    @Override
    public void onBackPressed() {
        if (mBinding.drawer.isDrawerOpen(GravityCompat.START)) {
            mBinding.drawer.closeDrawer(GravityCompat.START);
            return;
        }
        if (SemiViewManager.getInstance().close()) {
            super.onBackPressed();
            return;
        }

        Date now = new Date();
        if (mBackPressed == null || now.getTime() - mBackPressed.getTime() > 2000) {
            mBackPressed = now;
            Toast.makeText(this, R.string.double_press_back_exit, Toast.LENGTH_SHORT).show();
        }
        else {
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

    public void startAr() {
        Intent intent = new Intent(this, ArActivity.class);
        startActivity(intent);
    }
}

