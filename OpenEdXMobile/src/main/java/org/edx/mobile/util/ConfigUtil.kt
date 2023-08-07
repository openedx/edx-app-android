package org.edx.mobile.util

import android.content.Context
import android.text.TextUtils
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.edx.mobile.BuildConfig
import org.edx.mobile.model.CourseDatesCalendarSync
import org.edx.mobile.social.SocialAuthSource
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException

class ConfigUtil {
    companion object {
        /**
         * Utility method to check the status of the course upgrade.
         *
         * @param config   [Config]
         * @param listener [OnCourseUpgradeStatusListener] callback for the status of the course upgrade.
         */
        fun checkCourseUpgradeEnabled(
            config: Config,
            listener: OnCourseUpgradeStatusListener
        ) {
            // Check firebase enabled in config
            if (config.firebaseConfig.isEnabled) {
                val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
                firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
                    val whiteListedReleasesJson = firebaseRemoteConfig
                        .getString(AppConstants.FirebaseConstants.REV_934_WHITELISTED_RELEASES)
                    if (!TextUtils.isEmpty(whiteListedReleasesJson)) {
                        val whiteListedReleases =
                            Gson().fromJson<ArrayList<String>>(
                                whiteListedReleasesJson,
                                object : TypeToken<ArrayList<String>>() {}.type
                            )
                        // Check current release is white listed in firebase remote config
                        for (release in whiteListedReleases) {
                            if (BuildConfig.VERSION_NAME.equals(release, ignoreCase = true)) {
                                listener.onCourseUpgradeResult(true)
                                return@addOnCompleteListener
                            }
                        }
                    }
                    listener.onCourseUpgradeResult(false)
                }
            }
        }

        /**
         * Utility method to check the response of the calendar sync.
         *
         * @param config   [Config]
         * @param listener [OnCalendarSyncListener] callback for the status of the value prop.
         */
        fun checkCalendarSyncEnabled(
            config: Config,
            listener: OnCalendarSyncListener
        ) {
            // Check firebase enabled in config
            if (config.firebaseConfig.isEnabled) {
                val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
                firebaseRemoteConfig.fetch(0).addOnCompleteListener {
                    try {
                        val response = JSONObject(
                            firebaseRemoteConfig
                                .getString(AppConstants.FirebaseConstants.COURSE_DATES_CALENDAR_SYNC)
                        )
                        val androidResponse = Gson().fromJson<CourseDatesCalendarSync>(
                            response.getString(AppConstants.FirebaseConstants.KEY_ANDROID),
                            CourseDatesCalendarSync::class.java
                        )
                        listener.onCalendarSyncResponse(response = androidResponse)
                    } catch (e: InvocationTargetException) {
                        e.cause?.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        /**
         * Utility Method to get Agreement Urls based on [AgreementUrlType] and supported languages
         *
         * @param context   Current Context
         * @param config    [Config.AgreementUrlsConfig]
         * @param urlType   [AgreementUrlType] type of Url
         */
        @JvmStatic
        fun getAgreementUrl(
            context: Context,
            config: Config.AgreementUrlsConfig?,
            urlType: AgreementUrlType
        ): String? {
            if (config == null || TextUtils.isEmpty(config.getAgreementUrl(urlType))) {
                return urlType.getStringResId()?.let { context.resources.getString(it) }
            }
            if (config.supportedLanguages != null && config.supportedLanguages.isNotEmpty()) {
                val currentLocal = LocaleUtils.getCurrentDeviceLanguage(context)
                if (config.supportedLanguages.contains(currentLocal)) {
                    return UrlUtil.appendPathAfterAuthority(
                        config.getAgreementUrl(urlType),
                        currentLocal
                    )
                }
            }
            return config.getAgreementUrl(urlType)
        }

        /**
         * Determines whether a specific social feature is enabled based on the provided
         * social authentication source and configuration.
         *
         * @param type The social authentication source for which to check the feature's enablement.
         * @param config The global configuration containing individual social network configurations.
         * @return `true` if the specified social feature is enabled; otherwise, `false`.
         */
        @JvmStatic
        fun isSocialFeatureEnabled(
            type: SocialAuthSource,
            config: Config
        ): Boolean {
            return when (type) {
                SocialAuthSource.GOOGLE -> config.googleConfig.isEnabled
                SocialAuthSource.FACEBOOK -> config.facebookConfig.isEnabled
                SocialAuthSource.MICROSOFT -> config.microsoftConfig.isEnabled
                else -> true
            }
        }
    }

    /**
     * Interface to provide the callback for the status of the course upgrade.
     */
    interface OnCourseUpgradeStatusListener {
        /**
         * Callback to send course upgrade status result.
         */
        fun onCourseUpgradeResult(enabled: Boolean)
    }

    /**
     * Interface to provide the callback for the response of the remote config for Calendar Sync.
     */
    interface OnCalendarSyncListener {
        /**
         * Callback to send response of Calendar sync.
         */
        fun onCalendarSyncResponse(response: CourseDatesCalendarSync)
    }
}
