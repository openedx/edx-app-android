package org.edx.mobile.authentication;

import android.support.annotation.NonNull;

import com.google.inject.Inject;

import org.edx.mobile.http.constants.ApiConstants;
import org.edx.mobile.http.constants.ApiConstants.TokenType;
import org.edx.mobile.model.api.ProfileModel;
import org.edx.mobile.model.api.ResetPasswordResponse;
import org.edx.mobile.module.prefs.LoginPrefs;
import org.edx.mobile.module.registration.model.RegistrationDescription;
import org.edx.mobile.tta.ui.logistration.model.SendOTPResponse;
import org.edx.mobile.tta.ui.logistration.model.UserAddressResponse;
import org.edx.mobile.tta.ui.otp.model.VerifyOTPForgotedPasswordResponse;
import org.edx.mobile.tta.ui.otp.model.VerifyOTPResponse;
import org.edx.mobile.tta.ui.reset_password.model.MobileNumberVerificationResponse;
import org.edx.mobile.tta.ui.reset_password.model.ResetForgotedPasswordResponse;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import static org.edx.mobile.http.constants.ApiConstants.URL_MY_USER_INFO;

public interface LoginService {

    /**
     * A RoboGuice TaProvider implementation for LoginService.
     */
    class Provider implements com.google.inject.Provider<LoginService> {
        @Inject
        private Retrofit retrofit;

        @Override
        public LoginService get() {
            return retrofit.create(LoginService.class);
        }
    }

    /**
     * If there are form validation errors, this call will fail with 400 or 409 error code.
     * In case of validation errors the response body will be {@link org.edx.mobile.model.api.FormFieldMessageBody}.
     */
    @NonNull
    @FormUrlEncoded
    @POST(ApiConstants.URL_REGISTRATION)
    Call<ResponseBody> register(@FieldMap Map<String, String> parameters);

    @NonNull
    @GET(ApiConstants.URL_REGISTRATION)
    Call<RegistrationDescription> getRegistrationForm();

    /**
     * Depending on the query parameters for this endpoint, a different action will be triggered
     * on the server side. In this case, we are sending a user and password to get the AuthResponse.
     */
    @NonNull
    @FormUrlEncoded
    @POST(ApiConstants.URL_ACCESS_TOKEN)
    Call<AuthResponse> getAccessToken(@Field("grant_type") String grant_type,
                                      @Field("client_id") String client_id,
                                      @Field("username") String username,
                                      @Field("password") String password);

    /**
     * Depending on the query parameters for this endpoint, a different action will be triggered
     * on the server side. In this case, we are using our refresh_token to get a new AuthResponse.
     */
    @NonNull
    @FormUrlEncoded
    @POST(ApiConstants.URL_ACCESS_TOKEN)
    Call<AuthResponse> refreshAccessToken(@Field("grant_type") String grant_type,
                                          @Field("client_id") String client_id,
                                          @Field("refresh_token") String refresh_token);


    /**
     * Authenticate with edX using an access token from a third party OAuth provider.
     *
     * @param accessToken access token retrieved from third party OAuth provider (i.e. Facebook, Google)
     * @param clientId    edX OAuth client ID from config
     * @param groupId     Group ID as returned from {@link ApiConstants#getOAuthGroupIdForAuthBackend(LoginPrefs.AuthBackend)}
     */
    @NonNull
    @FormUrlEncoded
    @POST(ApiConstants.URL_EXCHANGE_ACCESS_TOKEN)
    Call<AuthResponse> exchangeAccessToken(@Field("access_token") String accessToken,
                                           @Field("client_id") String clientId,
                                           @Path(ApiConstants.GROUP_ID) String groupId);

    /**
     * Revoke the specified refresh or access token, along with all other tokens based on the same
     * application grant.
     *
     * @param clientId      The client ID
     * @param token         The refresh or access token to be revoked
     * @param tokenTypeHint The type of the token to be revoked; This should be either
     *                      'access_token' or 'refresh_token'
     */
    @NonNull
    @FormUrlEncoded
    @POST(ApiConstants.URL_REVOKE_TOKEN)
    Call<ResponseBody> revokeAccessToken(@Field("client_id") String clientId,
                                         @Field("token") String token,
                                         @Field("token_type_hint") @TokenType String tokenTypeHint);

    /**
     * Reset password for account associated with an email address.
     */
    @NonNull
    @FormUrlEncoded
    @POST(ApiConstants.URL_PASSWORD_RESET)
    Call<ResetPasswordResponse> resetPassword(@Field("email") String email);

    @POST(ApiConstants.URL_LOGIN)
    Call<RequestBody> login();

    /**
     * @return basic profile information for currently authenticated user.
     */
    @NonNull
    @GET(URL_MY_USER_INFO)
    Call<ProfileModel> getProfile();

    @FormUrlEncoded
    @POST(ApiConstants.URL_MX_MOBILE_NUMBER_VERIFICATION)
    Call<MobileNumberVerificationResponse> mxMobileNumberVerification(@FieldMap Map<String, String> parameters);

    @FormUrlEncoded
    @POST(ApiConstants.URL_MX_VERIFY_OTP_FOR_FORGOTED_PASSWORD)
    Call<VerifyOTPForgotedPasswordResponse> mxOTPVerification_For_ForgotedPassword(@FieldMap Map<String, String> parameters);

    @FormUrlEncoded
    @POST(ApiConstants.URL_MX_RESET_FORGOTED_PASSWORD)
    Call<ResetForgotedPasswordResponse> mxResetForgotedPassword(@FieldMap Map<String, String> parameters);

    @FormUrlEncoded
    @POST(ApiConstants.URL_MX_GENERATE_OTP)
    Call<SendOTPResponse> mxGenerateOTP(@FieldMap Map<String, String> parameters);

    @FormUrlEncoded
    @POST(ApiConstants.URL_MX_VERIFY_OTP)
    Call<VerifyOTPResponse> mxVerifyOTP(@FieldMap Map<String, String> parameters);

    @FormUrlEncoded
    @POST(ApiConstants.URL_MX_GET_USER_ADDRESS)
    Call<UserAddressResponse> mxGetUserAddress(@FieldMap Map<String, String> parameters);
}
