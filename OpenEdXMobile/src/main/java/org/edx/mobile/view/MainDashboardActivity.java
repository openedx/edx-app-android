package org.edx.mobile.view;

import static org.edx.mobile.view.Router.EXTRA_DEEP_LINK;
import static org.edx.mobile.view.Router.EXTRA_PATH_ID;
import static org.edx.mobile.view.Router.EXTRA_SCREEN_NAME;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;

import org.edx.mobile.BuildConfig;
import org.edx.mobile.R;
import org.edx.mobile.databinding.ActivityMainDashboardBinding;
import org.edx.mobile.deeplink.DeepLink;
import org.edx.mobile.deeplink.ScreenDef;
import org.edx.mobile.event.MainDashboardRefreshEvent;
import org.edx.mobile.event.NewVersionAvailableEvent;
import org.edx.mobile.http.notifications.SnackbarErrorNotification;
import org.edx.mobile.module.notification.NotificationDelegate;
import org.edx.mobile.module.prefs.InfoPrefs;
import org.edx.mobile.module.prefs.LoginPrefs;
import org.edx.mobile.util.AppConstants;
import org.edx.mobile.util.AppStoreUtils;
import org.edx.mobile.util.IntentFactory;
import org.edx.mobile.util.ResourceUtil;
import org.edx.mobile.util.Version;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainDashboardActivity extends OfflineSupportBaseActivity<ActivityMainDashboardBinding> {

    @Inject
    NotificationDelegate notificationDelegate;

    public static Intent newIntent(@Nullable @ScreenDef String screenName, @Nullable String pathId) {
        // These flags will make it so we only have a single instance of this activity,
        // but that instance will not be restarted if it is already running
        return IntentFactory.newIntentForComponent(MainDashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_SCREEN_NAME, screenName)
                .putExtra(EXTRA_PATH_ID, pathId);
    }

    public static Intent newIntent(@Nullable DeepLink deepLink) {
        return IntentFactory.newIntentForComponent(MainDashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_DEEP_LINK, deepLink);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.fragment_container_view, getMainTabsDashboardFragment(), null);
            fragmentTransaction.disallowAddToBackStack();
            fragmentTransaction.commit();
        }

        initWhatsNew();
        showIfRegistrationBecameLogin();
    }

    public int getViewResourceId() {
        return R.layout.activity_main_dashboard;
    }

    public Object getRefreshEvent() {
        return new MainDashboardRefreshEvent();
    }

    private void initWhatsNew() {
        if (environment.getConfig().isWhatsNewEnabled()) {
            boolean shouldShowWhatsNew = false;
            final InfoPrefs infoPrefs = environment.getInfoPrefs();
            final String lastWhatsNewShownVersion = infoPrefs.getWhatsNewShownVersion();
            if (lastWhatsNewShownVersion == null) {
                shouldShowWhatsNew = true;
            } else {
                try {
                    final Version oldVersion = new Version(lastWhatsNewShownVersion);
                    final Version newVersion = new Version(BuildConfig.VERSION_NAME);
                    if (oldVersion.isNMinorVersionsDiff(newVersion,
                            AppConstants.MINOR_VERSIONS_DIFF_REQUIRED_FOR_WHATS_NEW)) {
                        shouldShowWhatsNew = true;
                    }
                } catch (ParseException e) {
                    shouldShowWhatsNew = false;
                    logger.error(e);
                }
            }
            if (shouldShowWhatsNew) {
                environment.getRouter().showWhatsNewActivity(this);
            }
        }
    }

    /**
     * Shows a logged-in message when a registered user tries to register again using social auth
     */
    private void showIfRegistrationBecameLogin() {
        if (environment.getLoginPrefs().getAlreadyRegisteredLoggedIn()) {
            environment.getLoginPrefs().setAlreadyRegisteredLoggedIn(false);

            if (environment.getLoginPrefs().getAuthBackendType() != null &&
                    !environment.getLoginPrefs().getAuthBackendType().equals(LoginPrefs.AuthBackend.PASSWORD.value())) {

                final Map<String, CharSequence> map = new HashMap<>();
                map.put(AppConstants.PLATFORM_NAME, environment.getConfig().getPlatformName());
                map.put(AppConstants.SOCIAL_PROVIDER, environment.getLoginPrefs().getAuthBackendType());

                String loginMsg = ResourceUtil.getFormattedString(getResources(),
                        R.string.social_registration_became_login_message, map).toString();

                new SnackbarErrorNotification(binding.getRoot())
                        .showRegistrationBecameLoginSnackbar(loginMsg, !isLandscape());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            /* This is the main Activity, and is where the new version availability
             * notifications are being posted. These events are posted as sticky so
             * that they can be compared against new instances of them to be posted
             * in order to determine whether it has new information content. The
             * events have an intrinsic property to mark them as consumed, in order
             * to not have to remove the sticky events (and thus lose the last
             * posted event information). Finishing this Activity should be
             * considered as closing the current session, and the notifications
             * should be reposted on a new session. Therefore, we clear the session
             * information by removing the sticky new version availability events
             * from the event bus.
             */
            EventBus.getDefault().removeStickyEvent(NewVersionAvailableEvent.class);
        }
    }

    public Fragment getMainTabsDashboardFragment() {
        final Fragment fragment = new MainTabsDashboardFragment();
        final Bundle bundle = getIntent().getExtras();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationDelegate.checkAppUpgrade();
    }

    /**
     * Event bus callback for new app version availability event.
     *
     * @param newVersionAvailableEvent The new app version availability event.
     */
    @Subscribe
    public void onEvent(@NonNull final NewVersionAvailableEvent newVersionAvailableEvent) {
        if (!newVersionAvailableEvent.isConsumed()) {
            final Snackbar snackbar = Snackbar.make(binding.getRoot(),
                    newVersionAvailableEvent.getNotificationString(this),
                    Snackbar.LENGTH_INDEFINITE);
            if (AppStoreUtils.canUpdate(this)) {
                snackbar.setAction(R.string.label_update,
                        AppStoreUtils.OPEN_APP_IN_APP_STORE_CLICK_LISTENER);
            }
            snackbar.setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    newVersionAvailableEvent.markAsConsumed();
                }
            });
            snackbar.show();
        }
    }

    @Override
    protected void configureActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowHomeEnabled(false);
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setIcon(android.R.color.transparent);
        }
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getResources().getString(titleId));
    }
}
