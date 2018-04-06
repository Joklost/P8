package dk.aau.sw805f18.ar.viewModels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

public abstract class ViewModel extends BaseObservable {
    void onBackPressed() {}
    @Bindable
    public String getTitle() { return ""; }
}
