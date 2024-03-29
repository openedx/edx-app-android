package org.edx.mobile.view;

import static org.edx.mobile.view.Router.EXTRA_BUNDLE;
import static org.edx.mobile.view.Router.EXTRA_COURSE_COMPONENT_ID;
import static org.edx.mobile.view.Router.EXTRA_COURSE_DATA;
import static org.edx.mobile.view.Router.EXTRA_COURSE_UPGRADE_DATA;
import static org.edx.mobile.view.Router.EXTRA_IS_VIDEOS_MODE;
import static org.edx.mobile.view.Router.EXTRA_LAST_ACCESSED_ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import org.edx.mobile.base.BaseSingleFragmentActivity;
import org.edx.mobile.event.CourseUpgradedEvent;
import org.edx.mobile.model.api.CourseUpgradeResponse;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.greenrobot.eventbus.Subscribe;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CourseOutlineActivity extends BaseSingleFragmentActivity {

    public static Intent newIntent(Activity activity,
                                   EnrolledCoursesResponse courseData,
                                   CourseUpgradeResponse courseUpgradeData,
                                   String courseComponentId, String lastAccessedId,
                                   boolean isVideosMode) {
        final Bundle courseBundle = new Bundle();
        courseBundle.putSerializable(EXTRA_COURSE_DATA, courseData);
        courseBundle.putParcelable(EXTRA_COURSE_UPGRADE_DATA, courseUpgradeData);
        courseBundle.putString(EXTRA_COURSE_COMPONENT_ID, courseComponentId);

        final Intent intent = new Intent(activity, CourseOutlineActivity.class);
        intent.putExtra(EXTRA_BUNDLE, courseBundle);
        intent.putExtra(EXTRA_LAST_ACCESSED_ID, lastAccessedId);
        intent.putExtra(EXTRA_IS_VIDEOS_MODE, isVideosMode);

        return intent;
    }

    @Override
    public Fragment getFirstFragment() {
        final Fragment fragment = new CourseOutlineFragment();
        fragment.setArguments(getIntent().getExtras());
        return fragment;
    }

    @Subscribe
    public void onEvent(CourseUpgradedEvent event) {
        finish();
    }
}
