package dk.aau.sw805f18.ar.common.helpers;

import android.app.Activity;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.view.View;

public final class SnackbarHelper {
    private static final int BACKGROUND_COLOR = 0xbf323232;
    private Snackbar messageSnackbar;

    public boolean isShowing() {
        return messageSnackbar != null;
    }

    public void showMessage(Activity activity, String message) {
        show(activity, message, false);
    }

    public void showError(Activity activity, String errorMessage) {
        show(activity, errorMessage, /*finishOnDismiss=*/ true);
    }

    public void hide(Activity activity) {
        activity.runOnUiThread(() -> {
            if (messageSnackbar != null) {
                messageSnackbar.dismiss();
            }
            messageSnackbar = null;
        });
    }

    private void show(final Activity activity, final String message, final boolean finishOnDismiss) {
        activity.runOnUiThread(() -> {
            messageSnackbar = Snackbar.make(
                    activity.findViewById(android.R.id.content),
                    message,
                    Snackbar.LENGTH_INDEFINITE
            );
            messageSnackbar.getView().setBackgroundColor(BACKGROUND_COLOR);
            if (finishOnDismiss) {
                messageSnackbar.setAction("Dismiss", v -> messageSnackbar.dismiss());
                messageSnackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        activity.finish();
                    }
                });
            }
            messageSnackbar.show();
        });
    }
}
