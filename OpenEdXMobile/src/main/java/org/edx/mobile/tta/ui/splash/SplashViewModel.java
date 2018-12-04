package org.edx.mobile.tta.ui.splash;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.inject.Inject;

import org.edx.mobile.tta.data.DataManager;
import org.edx.mobile.tta.ui.base.mvvm.BaseVMActivity;
import org.edx.mobile.tta.ui.base.mvvm.BaseViewModel;
import org.edx.mobile.tta.ui.login.LoginActivity;
import org.edx.mobile.tta.utils.ActivityUtil;


public class SplashViewModel extends BaseViewModel {

    private static final long DELAY = 2000;

    @Inject
    private DataManager dataManager;

    public SplashViewModel(BaseVMActivity activity) {
        super(activity);
        Log.d("__________LOG_________", "splash");
        startRouting(activity);
    }

    private void startRouting(Activity activity){

        new Handler().postDelayed(() -> {
            Log.d("__________LOG_________", "delay over");
            if (dataManager.getAppPref().isFirstLaunch()){
                Toast.makeText(activity, "First launch", Toast.LENGTH_SHORT).show();
                dataManager.getAppPref().setFirstLaunch(false);
            } else {
                activity.finish();
                ActivityUtil.gotoPage(activity, LoginActivity.class);
            }
        }, DELAY);

    }

}