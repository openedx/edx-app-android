package org.humana.mobile.http.interceptor;

import android.content.Context;
import android.support.annotation.NonNull;

import org.humana.mobile.logger.Logger;
import org.humana.mobile.module.prefs.LoginPrefs;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import roboguice.RoboGuice;

/**
 * Injects OAuth token - if present - into Authorization header
 **/
public final class OauthHeaderRequestInterceptor implements Interceptor {
    protected final Logger logger = new Logger(getClass().getName());

    @NonNull
    private final LoginPrefs loginPrefs;

    public OauthHeaderRequestInterceptor(@NonNull Context context) {
        loginPrefs = RoboGuice.getInjector(context).getInstance(LoginPrefs.class);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request.Builder builder = chain.request().newBuilder();
        final String token = loginPrefs.getAuthorizationHeader();
        if (token != null) {
            builder.addHeader("Authorization", token);

            //Mx Chirag: Added header to resolve “java.net.ProtocolException: Unexpected status line”
            builder.addHeader("Connection", "close");
        }
        return chain.proceed(builder.build());
    }

}