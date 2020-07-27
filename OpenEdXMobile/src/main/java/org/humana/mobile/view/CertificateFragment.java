package org.humana.mobile.view;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.google.inject.Inject;

import org.humana.mobile.R;
import org.humana.mobile.base.BaseFragment;
import org.humana.mobile.model.api.EnrolledCoursesResponse;
import org.humana.mobile.module.analytics.Analytics;
import org.humana.mobile.module.analytics.AnalyticsRegistry;
import org.humana.mobile.services.EdxCookieManager;
import org.humana.mobile.util.AppConstants;
import org.humana.mobile.util.ResourceUtil;
import org.humana.mobile.util.images.ShareUtils;
import org.humana.mobile.view.custom.URLInterceptorWebViewClient;

import java.util.HashMap;
import java.util.Map;

import roboguice.inject.InjectExtra;

public class CertificateFragment extends BaseFragment {

    static public final String TAG = CertificateFragment.class.getCanonicalName();
    static public final String ENROLLMENT = "enrollment";

    @Inject
    AnalyticsRegistry analyticsRegistry;

    @InjectExtra(ENROLLMENT)
    EnrolledCoursesResponse courseData;

    private WebView webview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        analyticsRegistry.trackScreenView(Analytics.Screens.CERTIFICATE);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.share_certificate, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share: {
                final Map<String, CharSequence> shareTextParams = new HashMap<>();
                shareTextParams.put("platform_name", getString(R.string.platform_name));
                shareTextParams.put("certificate_url", courseData.getCertificateURL());
                final String shareText = ResourceUtil.getFormattedString(getResources(), R.string.share_certificate_message, shareTextParams).toString();
                ShareUtils.showShareMenu(
                        getActivity(),
                        ShareUtils.newShareIntent(shareText),
                        getActivity().findViewById(R.id.menu_item_share),
                        new ShareUtils.ShareMenuItemListener() {
                            @Override
                            public void onMenuItemClick(@NonNull ComponentName componentName, @NonNull ShareUtils.ShareType shareType) {
                                analyticsRegistry.certificateShared(courseData.getCourse().getId(), courseData.getCertificateURL(), shareType);
                                final Intent intent = ShareUtils.newShareIntent(shareText);
                                intent.setComponent(componentName);
                                startActivity(intent);
                            }
                        });
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_certificate, container, false);
        webview = (WebView) view.findViewById(R.id.webview);
        final View loadingIndicator = view.findViewById(R.id.loading_indicator);
        final URLInterceptorWebViewClient client = new URLInterceptorWebViewClient(getActivity(), webview);
        client.setPageStatusListener(new URLInterceptorWebViewClient.IPageStatusListener() {
            @Override
            public void onPageStarted() {
                loadingIndicator.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished() {
                loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onPageLoadError(WebView view, int errorCode, String description, String failingUrl) {
                loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onPageLoadError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse,
                                        boolean isMainRequestFailure) {
                if (isMainRequestFailure) {
                    loadingIndicator.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageLoadProgressChanged(WebView view, int progress) {
                if (progress > AppConstants.PAGE_LOAD_THRESHOLD) loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void openFile(WebView webView, ValueCallback<Uri[]> uploadMsg, WebChromeClient.FileChooserParams fileChooserParams) {

            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Clear cookies before loading so that social sharing buttons are not displayed inside web view
        EdxCookieManager.getSharedInstance(getContext()).clearWebWiewCookie();

        webview.loadUrl(courseData.getCertificateURL());
    }

    @Override
    public void onResume() {
        super.onResume();
        webview.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        webview.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webview.destroy();
    }
}