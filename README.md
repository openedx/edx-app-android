Deprecation Notice
==================

This repository is schedule for deprecation.  Please see:

https://github.com/openedx/edx-app-android/issues/1830

This application is being replaced by:

https://github.com/openedx/openedx-app-android

# edX Android

[<img align="right" alt="Get it on Google Play" height="128" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png">](https://play.google.com/store/apps/details?id=org.edx.mobile)

This is the source code for the edX mobile Android app. It is changing rapidly
and its structure should not be relied upon. See <http://code.edx.org> for other
parts of the edX code base.

It has been tested with the "Koa" release of open edX and may not function correctly with previous versions. See
<https://openedx.atlassian.net/wiki/display/COMM/Open+edX+Releases> for more
information.

License
=======
This software is licensed under version 2 of the Apache License unless
otherwise noted. Please see `LICENSE.txt` for details.

Building
========

This project is meant to be built using Android Studio. It can also be built from the `gradle` command line.

1. Check out the source code:

        git clone https://github.com/openedx/edx-app-android

2. Setup the Android Studio. The latest tested Android Studio version is Flamingo | 2022.2.1 Patch 2, you can download it from [the previous versions archive](https://developer.android.com/studio/archive).

3. Open Android Studio and choose **Open an Existing Android Studio Project**

4. Choose `edx-app-android`.

5. Click the **Run** button.

Configuration
=============
The edX mobile Android app is designed to connect to an Open edX instance. You
must configure the app with the correct server address and supply appropriate
OAuth credentials. We use a configuration file mechanism similar to that of the
Open edX platform. This mechanism is also used to make other values available
to the app at runtime and store secret keys for third party services.

There is a default configuration that points to an edX devstack instance
running on localhost. See the `default_config` directory. For the default
configuration to work, you must add OAuth credentials specific to your
installation.

Setup
-----
To use a custom configuration in place of the default configuration, you will need to complete these tasks:

1. Create your own configuration directory somewhere else on the file system. For example, create `my_config` as a sibling of the `edx-app-android` repository.

2. Create an `edx.properties` file inside the `OpenEdXMobile` directory of `edx-app-android`. In this `edx.properties` file, set the `edx.dir` property to the path to your configuration directory relative to the `OpenEdXMobile` directory. For example, if I stored my configuration side by side with the `edx-app-android` repository at `my_config` then I'd have the following `edx.properties`:

        edx.dir = '../../my_config'

3.  In the configuration directory that you added in step 1, create another
`edx.properties` file. This properties file contains a list of filenames. The files should be in YAML format and are for storing specific keys. These files are specified relative to the configuration directory. Keys in files earlier in the list will be overridden by keys from files later in the list. For example, if I had two files, one shared between ios and android called `shared.yaml` and one with Android specific keys called `android.yaml`, I would have the following `edx.properties`:

        edx.android {
            configFiles = ['shared.yaml', 'android.yaml']
        }


The full set of known keys can be found in the
`org/edx/mobile/util/Config.java` file or see [additional documentation](<https://openedx.atlassian.net/wiki/spaces/LEARNER/pages/48792067/App+Configuration+Flags>).


Build Variants
--------------------

There are 3 Build Variants in this project:

- **prodDebug**: Uses prod flavor for debug builds.
- **prodDebuggable**: Uses prod flavor for debug builds with debugging enabled.
- **prodRelease**: Uses prod flavor for release builds that'll work on devices with Android 5.0.x (Lollipop) and above.

Building For Release
--------------------
To build an APK for release, you have to specify an application ID and signing key.

#### Specifying your own Application ID
Application ID is the package identifier for your app.  
Edit the `constants.gradle` file inside the `edx-app-android` directory. For example:

    APPLICATION_ID=com.example.yourapp


#### Specifying the Signing Key
Place your keystore file inside the `OpenEdXMobile/signing` directory of `edx-app-android` & then create a `keystore.properties` file inside the same directory with the following configurations:

    RELEASE_STORE_FILE=signing/your_keystore_file.keystore
    RELEASE_STORE_PASSWORD=your store password here
    RELEASE_KEY_PASSWORD=your key password here
    RELEASE_KEY_ALIAS=your key alias here

Now you can build a release build from Android Studio. Or, in the directory of `edx-app-android` you can build a release build with this gradle command:

    ./gradlew assembleProdRelease

**Note:** For release branch naming convention take a look at this PR <https://github.com/openedx/edx-app-android/pull/774> that creates the versionCode automatically based on branch name. The output APK will be named with the version.


Customization
-------------
**Resources**

To customize images, colors, and layouts, you can specify a custom resource directory. Create or edit the `gradle.properties` file inside the `OpenEdXMobile` directory of `edx-app-android`. For example:


    RES_DIR = ../../path/to/your/res

Any resources placed here will override resources of the same name in the `OpenEdXMobile/res` directory.

To remove all edX branding, override the drawables for: `edx_logo`, `edx_logo_login`, `ic_launcher`, `ic_new_cert`, and `profile_photo_placeholder`. These come in a number of resolutions, be sure to replace them all.

If you need to make more in depth UI changes, most of the user interface is specified in standard Android XML files, which you can also override by placing new versions in your `RES_DIR`.

**Assets**

To customize assets such as the End User License Agreement (EULA) you can specify a custom assets directory. Create or edit the `gradle.properties` file inside the `OpenEdXMobile` directory of `edx-app-android`. For example:


    ASSETS = ../../path/to/your/assets

Any assets placed here will override resources of the same name in the `OpenEdXMobile/assets` directory.

Third Party Services
--------------------
The app relies on the presence of several third party services: Facebook, Google+, SegmentIO, and Crashlytics. You may need to remove services you choose not to use. You can comment out the lines of code that mention these services.

We're working on making it easier for Open edX installations to apply customizations and select third party services without modifying the repository itself.


Frequently Asked Questions
==========================
**Q:** I want to use Firebase in my project, where do I place my [google-services.json](https://developers.google.com/android/guides/google-services-plugin#adding_the_json_file) file?

**A:** You don’t need to place the [google-services.json](https://developers.google.com/android/guides/google-services-plugin#adding_the_json_file) into the project, we are generating it through gradle script ([AndroidHelper.gradle](https://github.com/openedx/edx-app-android/blob/master/OpenEdXMobile/gradle_scripts/AndroidHelper.gradle#L15)) that picks keys and values required in the [google-services.json](https://developers.google.com/android/guides/google-services-plugin#adding_the_json_file) file from the [app's configuration file](https://github.com/openedx/edx-app-android/blob/master/OpenEdXMobile/default_config/config.yaml). For configuration details [see](https://openedx.atlassian.net/wiki/spaces/LEARNER/pages/48792067/App+Configuration+Flags)
