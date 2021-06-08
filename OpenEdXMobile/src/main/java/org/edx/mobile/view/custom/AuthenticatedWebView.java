package org.edx.mobile.view.custom;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.event.CourseDashboardRefreshEvent;
import org.edx.mobile.event.FileSelectionEvent;
import org.edx.mobile.event.MainDashboardRefreshEvent;
import org.edx.mobile.event.NetworkConnectivityChangeEvent;
import org.edx.mobile.event.SessionIdRefreshEvent;
import org.edx.mobile.http.HttpStatus;
import org.edx.mobile.http.notifications.FullScreenErrorNotification;
import org.edx.mobile.interfaces.RefreshListener;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.module.prefs.LoginPrefs;
import org.edx.mobile.services.EdxCookieManager;
import org.edx.mobile.util.NetworkUtil;
import org.edx.mobile.util.WebViewUtil;

import de.greenrobot.event.EventBus;
import roboguice.RoboGuice;
import roboguice.inject.InjectView;

import static org.edx.mobile.util.WebViewUtil.EMPTY_HTML;

/**
 * A custom webview which authenticates the user before loading a page,
 * Javascript can also be passed in arguments for evaluation.
 */
public class AuthenticatedWebView extends FrameLayout implements RefreshListener {
    protected final Logger logger = new Logger(getClass().getName());

    @Inject
    private LoginPrefs loginPrefs;

    @InjectView(R.id.loading_indicator)
    private ProgressBar progressWheel;

    @InjectView(R.id.webview)
    protected WebView webView;

    private FullScreenErrorNotification fullScreenErrorNotification;
    private URLInterceptorWebViewClient webViewClient;
    private String url;
    private String javascript;
    private boolean pageIsLoaded;
    private boolean didReceiveError;
    private boolean isManuallyReloadable;

    public AuthenticatedWebView(Context context) {
        super(context);
        init();
    }

    public AuthenticatedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuthenticatedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.authenticated_webview, this);
        RoboGuice.injectMembers(getContext(), this);
        RoboGuice.getInjector(getContext()).injectViewMembers(this);
        fullScreenErrorNotification = new FullScreenErrorNotification(webView);
    }

    public URLInterceptorWebViewClient getWebViewClient() {
        return webViewClient;
    }

    public WebView getWebView() {
        return webView;
    }

    /**
     * Initialize the webview (must call it before loading some url).
     *
     * @param fragmentActivity     Reference of fragment activity.
     * @param isAllLinksExternal   A flag to treat every link as external link and open in external
     *                             web browser.
     * @param isManuallyReloadable A flag that decides if we should give show/hide reload button.
     * @param interceptAjaxRequest A flag that decides if webview intercept the webpage ajax request.
     * @param completionCallback
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void initWebView(@NonNull FragmentActivity fragmentActivity, boolean isAllLinksExternal,
                            boolean isManuallyReloadable, boolean interceptAjaxRequest,
                            URLInterceptorWebViewClient.CompletionCallback completionCallback) {
        this.isManuallyReloadable = isManuallyReloadable;
        webView.getSettings().setJavaScriptEnabled(true);
        webViewClient = new URLInterceptorWebViewClient(fragmentActivity, webView, interceptAjaxRequest,
                completionCallback) {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                didReceiveError = true;
                hideLoadingProgress();
                pageIsLoaded = false;
                showErrorMessage(R.string.network_error_message, R.drawable.ic_error);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                // If error occurred for web page request
                if (request.getUrl().toString().equals(view.getUrl())) {
                    didReceiveError = true;
                    switch (errorResponse.getStatusCode()) {
                        case HttpStatus.FORBIDDEN:
                        case HttpStatus.UNAUTHORIZED:
                        case HttpStatus.NOT_FOUND:
                            EdxCookieManager.getSharedInstance(getContext())
                                    .tryToRefreshSessionCookie();
                            break;
                        default:
                            hideLoadingProgress();
                            break;
                    }
                    pageIsLoaded = false;
                    showErrorMessage(R.string.network_error_message, R.drawable.ic_error);
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }

            public void onPageFinished(WebView view, String url) {
                if (!NetworkUtil.isConnected(getContext())) {
                    showErrorView(getResources().getString(R.string.reset_no_network_message),
                            R.drawable.ic_wifi);
                    hideLoadingProgress();
                    pageIsLoaded = false;
                    return;
                }
                if (didReceiveError) {
                    didReceiveError = false;
                    return;
                }
                if (url != null && url.equals("data:text/html," + EMPTY_HTML)) {
                    //we load a local empty html page to release the memory
                } else {
                    pageIsLoaded = true;
                    hideErrorMessage();
                }

                if (pageIsLoaded && !TextUtils.isEmpty(javascript)) {
                    evaluateJavascript(javascript, null);
                } else {
                    hideLoadingProgress();
                }
                super.onPageFinished(view, url);
            }
        };

        webViewClient.setAllLinksAsExternal(isAllLinksExternal);
    }

    public void loadUrl(boolean forceLoad, @NonNull String url) {
        loadUrlWithJavascript(forceLoad, url, null);
    }

    public void loadUrlWithJavascript(boolean forceLoad, @NonNull final String url, @Nullable final String javascript) {
        this.url = url;
        this.javascript = javascript;
        if (!TextUtils.isEmpty(javascript)) {
            webView.addJavascriptInterface(new JsInterface(), "JsInterface");
        }
        tryToLoadWebView(forceLoad);
    }

    public void evaluateJavascript(String javascript, ValueCallback<String> listener) {
        if (listener == null) {
            listener = value -> hideLoadingProgress();
        }
        webView.evaluateJavascript(javascript, listener);
    }

    private void tryToLoadWebView(boolean forceLoad) {
        System.gc(); //there is a well known Webview Memory Issue With Galaxy S3 With 4.3 Update

        if ((!forceLoad && pageIsLoaded) || progressWheel == null) {
            return;
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (!NetworkUtil.isConnected(getContext())) {
            showErrorMessage(R.string.reset_no_network_message, R.drawable.ic_wifi);
            return;
        }

        showLoadingProgress();

        if (!TextUtils.isEmpty(url)) {
            // Requery the session cookie if unavailable or expired.
            final EdxCookieManager cookieManager = EdxCookieManager.getSharedInstance(getContext());
            if (cookieManager.isSessionCookieMissingOrExpired()) {
                cookieManager.tryToRefreshSessionCookie();
            } else {
                didReceiveError = false;
                webView.loadUrl(url);
            }
        }
    }

    public void tryToClearWebView() {
        pageIsLoaded = false;
        WebViewUtil.clearWebviewHtml(webView);
    }

    private void showLoadingProgress() {
        if (!TextUtils.isEmpty(javascript)) {
            // Hide webview to disable a11y during loading page, disabling a11y is not working in this case
            webView.setVisibility(View.GONE);
        }
        progressWheel.setVisibility(View.VISIBLE);
    }

    private void hideLoadingProgress() {
        progressWheel.setVisibility(View.GONE);
        if (didReceiveError) {
            webView.setVisibility(View.GONE);
        } else {
            webView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows the error message with the given icon, if the web page failed to load
     *
     * @param errorMsg  The error message to show
     * @param errorIconResId The resource id of the error icon to show with the error message
     */
    private void showErrorMessage(@StringRes int errorMsg, @DrawableRes int errorIconResId) {
        if (!pageIsLoaded) {
            tryToClearWebView();
            showErrorView(getResources().getString(errorMsg), errorIconResId);
        }
    }

    private void showErrorView(@NonNull String errorMsg, @DrawableRes int errorIconResId) {
        if (isManuallyReloadable) {
            fullScreenErrorNotification.showError(errorMsg, errorIconResId, R.string.lbl_reload, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRefresh();
                }
            });
        } else {
            fullScreenErrorNotification.showError(errorMsg, errorIconResId, 0, null);
        }
    }

    /**
     * Hides the error message view and reloads the web page if it wasn't already loaded
     */
    private void hideErrorMessage() {
        fullScreenErrorNotification.hideError();
        if (!pageIsLoaded || didReceiveError) {
            tryToLoadWebView(true);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NetworkConnectivityChangeEvent event) {
        if (NetworkUtil.isConnected(getContext())) {
            // If manual reloading is enabled, we don't want the error to disappear and screen to load automatically
            if (!isManuallyReloadable) {
                onRefresh();
            }
        } else {
            showErrorMessage(R.string.reset_no_network_message, R.drawable.ic_wifi);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(SessionIdRefreshEvent event) {
        if (event.success) {
            tryToLoadWebView(false);
        } else {
            hideLoadingProgress();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CourseDashboardRefreshEvent event) {
        onRefresh();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(MainDashboardRefreshEvent event) {
        onRefresh();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(FileSelectionEvent event) {
        if (webViewClient != null) {
            webViewClient.onFilesSelection(event.getFiles());
        }
    }

    public void onResume() {
        if (webView != null) {
            webView.onResume();
        }
    }

    public void onPause() {
        if (webView != null) {
            webView.onPause();
        }
    }

    public void onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    public void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
    }

    @Override
    public void onRefresh() {
        pageIsLoaded = false;
        hideErrorMessage();
    }

    /**
     * Javascript interface class to define android functions which could be called from javascript.
     */
    private class JsInterface {
        @JavascriptInterface
        public void showErrorMessage(@NonNull final String errorMsg) {
            if (!TextUtils.isEmpty(errorMsg)) {
                webView.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                didReceiveError = true;
                                webView.setVisibility(View.GONE);
                                showErrorView(errorMsg, R.drawable.ic_error);
                            }
                        }
                );
            }
        }
    }

    public boolean isShowingError() {
        return fullScreenErrorNotification != null && fullScreenErrorNotification.isShowing();
    }

    public boolean isPageLoaded() {
        return pageIsLoaded;
    }
}
