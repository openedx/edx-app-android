package org.edx.mobile.social;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;

import org.edx.mobile.R;
import org.edx.mobile.exception.LoginErrorMessage;
import org.edx.mobile.exception.LoginException;
import org.edx.mobile.http.Api;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.api.AuthResponse;
import org.edx.mobile.model.api.ProfileModel;
import org.edx.mobile.module.prefs.PrefManager;
import org.edx.mobile.social.facebook.FacebookProvider;
import org.edx.mobile.social.google.GoogleOauth2;
import org.edx.mobile.task.Task;

/**
 * Code refactored from Login Activity, for the logic of login to social site are the same
 * for both login and registration.
 *
 * Created by hanning on 3/11/15.
 */
public class SocialLoginDelegate {

    protected final Logger logger = new Logger(getClass().getName());

    private Activity activity;
    private MobileLoginCallback callback;
    private SocialUserInfoCallback userInfoCallback;
    private ISocial google, facebook;

    private String userEmail;

    private ISocial.Callback googleCallback = new ISocial.Callback() {
        @Override
        public void onLogin(String accessToken) {
            logger.debug("Google logged in; token= " + accessToken);
            onSocialLoginSuccess(accessToken, PrefManager.Value.BACKEND_GOOGLE);
        }

    };

    private ISocial.Callback facebookCallback = new ISocial.Callback() {

        @Override
        public void onLogin(String accessToken) {
            logger.debug("Facebook logged in; token= " + accessToken);
            onSocialLoginSuccess(accessToken, PrefManager.Value.BACKEND_FACEBOOK);
        }
    };




    public SocialLoginDelegate(Activity activity, Bundle savedInstanceState, MobileLoginCallback callback){

        this.activity = activity;
        this.callback = callback;

        google = SocialFactory.getInstance(activity, SocialFactory.TYPE_GOOGLE);
        google.setCallback(googleCallback);

        facebook = SocialFactory.getInstance(activity, SocialFactory.TYPE_FACEBOOK);
        facebook.setCallback(facebookCallback);

        google.onActivityCreated(activity, savedInstanceState);
        facebook.onActivityCreated(activity, savedInstanceState);
    }

    public void onActivityDestroyed(){
        google.onActivityDestroyed(activity);
        facebook.onActivityDestroyed(activity);
    }

    public void onActivitySaveInstanceState(Bundle outState){
        google.onActivitySaveInstanceState(activity, outState);
        facebook.onActivitySaveInstanceState(activity, outState);
    }

    public void onActivityStarted(){
        google.onActivityStarted(activity);
        facebook.onActivityStarted(activity);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        google.onActivityResult(requestCode, resultCode, data);
        facebook.onActivityResult(requestCode, resultCode, data);
    }

    public void onActivityStopped() {
        google.onActivityStopped(activity);
        facebook.onActivityStopped(activity);
    }

    public void socialLogin(int socialType){
        if ( socialType == SocialFactory.TYPE_FACEBOOK )
           facebook.login();
        else if ( socialType == SocialFactory.TYPE_GOOGLE )
            google.login();
    }

    public void socialLogout(int socialType){
        if ( socialType == SocialFactory.TYPE_FACEBOOK )
            facebook.logout();
        else if ( socialType == SocialFactory.TYPE_GOOGLE )
            google.logout();
    }

    /**
     * called with you to use social login
     * @param accessToken
     * @param backend
     */
    public void onSocialLoginSuccess(String accessToken, String backend) {
        PrefManager pref = new PrefManager(activity, PrefManager.Pref.LOGIN);
        pref.put(PrefManager.Key.AUTH_TOKEN_SOCIAL, accessToken);
        pref.put(PrefManager.Key.AUTH_TOKEN_BACKEND, backend);

        Task<?> task = new ProfileTask(activity);
        callback.onSocialLoginSuccess(accessToken, backend, task);
        task.execute(accessToken, backend);
    }


    public void setUserEmail(String email){
        this.userEmail = email;
    }

    public String getUserEmail(){
        return this.userEmail;
    }


    public void getUserInfo(int socialType, final SocialUserInfoCallback userInfoCallback){

        if ( socialType == SocialFactory.TYPE_FACEBOOK ) {
            SocialProvider socialProvider = new FacebookProvider();
            socialProvider.getUser(activity, new SocialProvider.Callback<SocialMember>() {
                @Override
                public void onSuccess(SocialMember response) {
                    userInfoCallback.setSocialUserInfo(response.email, response.fullName);
                }

                @Override
                public void onError(SocialProvider.SocialError err) {
                    //TODO - should we pass error to UI?
                }
            });
        } else if ( socialType == SocialFactory.TYPE_GOOGLE ) {
            userInfoCallback.setSocialUserInfo(((GoogleOauth2)google).getEmail(), null);
        }

    }


    private class ProfileTask extends Task<ProfileModel> {

        private  String accessToken;
        private  String backend;

        public ProfileTask(Context context) {
            super(context);
        }

        @Override
        public void onFinish(ProfileModel result) {
            if (result != null) {
                try {
                    if (result.email == null) {
                        // handle this error, show error message
                        LoginErrorMessage errorMsg =
                                new LoginErrorMessage(
                                        context.getString(R.string.login_error),
                                        context.getString(R.string.login_failed));
                        throw new LoginException(errorMsg);
                    }

                    callback.onUserLoginSuccess(result);
                } catch (LoginException ex) {
                    logger.error(ex);
                    handle(ex);
                }
            }
        }

        @Override
        public void onException(Exception ex) {
            callback.onUserLoginFailure(ex, this.accessToken, this.backend);
        }

        @Override
        protected ProfileModel doInBackground(Object... params) {
            try {
                this.accessToken = (String) params[0];
                this.backend = (String) params[1];

                Api api = new Api(context);

                // do SOCIAL LOGIN first
                AuthResponse social = null;
                if (backend.equalsIgnoreCase(PrefManager.Value.BACKEND_FACEBOOK)) {
                    social = api.loginByFacebook(accessToken);

                    if ( social.error != null && social.error.equals("401") ) {
                        throw new LoginException(new LoginErrorMessage(
                                context.getString(R.string.error_account_not_linked_title_fb),
                                context.getString(R.string.error_account_not_linked_desc_fb)));
                    }
                } else if (backend.equalsIgnoreCase(PrefManager.Value.BACKEND_GOOGLE)) {
                    social = api.loginByGoogle(accessToken);

                    if ( social.error != null && social.error.equals("401") ) {
                        throw new LoginException(new LoginErrorMessage(
                                context.getString(R.string.error_account_not_linked_title_google),
                                context.getString(R.string.error_account_not_linked_desc_google)));
                    }
                }

                if (social.isSuccess()) {

                    // we got a valid accessToken so profile can be fetched
                    ProfileModel profile =  api.getProfile();

                    // store profile json
                    if (profile != null ) {
                        PrefManager pref = new PrefManager(context, PrefManager.Pref.LOGIN);
                        pref.put(PrefManager.Key.PROFILE_JSON,  profile.json);
                        pref.put(PrefManager.Key.AUTH_TOKEN_BACKEND, null);
                        pref.put(PrefManager.Key.AUTH_TOKEN_SOCIAL, null);
                    }

                    if (profile.email != null) {
                        // we got valid profile information
                        return profile;
                    }
                }
                throw new LoginException(new LoginErrorMessage(
                        context.getString(R.string.login_error),
                        context.getString(R.string.login_failed)));
            } catch (Exception e) {
                logger.error(e);
                handle(e);
            }
            return null;
        }

    }



    public interface MobileLoginCallback {
        void onSocialLoginSuccess(String accessToken, String backend, Task task);
        void onUserLoginFailure(Exception ex, String accessToken, String backend);
        void onUserLoginSuccess(ProfileModel profile) throws LoginException;
    }

    public interface SocialUserInfoCallback{
        void setSocialUserInfo(String email, String name);
    }

}
