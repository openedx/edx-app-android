package org.edx.mobile.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.widget.FacebookDialog;
import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.exception.AuthException;
import org.edx.mobile.loader.AsyncTaskResult;
import org.edx.mobile.loader.CoursesAsyncLoader;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.module.prefs.PrefManager;
import org.edx.mobile.services.FetchCourseFriendsService;
import org.edx.mobile.social.facebook.FacebookProvider;
import org.edx.mobile.module.facebook.FacebookSessionUtil;
import org.edx.mobile.util.Config;
import org.edx.mobile.util.ResourceUtil;
import org.edx.mobile.view.dialog.InstallFacebookDialog;

import java.util.ArrayList;
import java.util.List;

@Deprecated // Not used right now, should be deleted
public class MyFriendsCoursesTabFragment extends CourseListTabFragment implements View.OnClickListener  {

    private static final String TAG = MyCourseListTabFragment.class.getSimpleName();

    private final int FREINDS_COURSES_LOADER_ID = 0x605000;
    private LinearLayout noFriendsLayout;

    @Inject
    Config config;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        environment.getSegment().trackScreenView(ISegment.Screens.MY_FRIENDS_COURSES);
    }

    @Override
    public void handleCourseClick(EnrolledCoursesResponse model) {

        try {

            Intent friendsInGroupIntent = new Intent(getActivity(),
                    FriendsInCourseActivity.class);
            friendsInGroupIntent.putExtra(FriendsInCourseActivity.EXTRA_COURSE, model.getCourse());
            friendsInGroupIntent.putExtra(FriendsInCourseActivity.EXTRA_FRIENDS_TAB_LINK, true);

            startActivity(friendsInGroupIntent);

        } catch(Exception ex) {
            logger.error(ex);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String platformName = config.getPlatformName();

        View view = super.onCreateView(inflater, container, savedInstanceState);
        noFriendsLayout = (LinearLayout) view.findViewById(R.id.friends_course_no_friends_layout);
        TextView shareBtn = (TextView) view.findViewById(R.id.friends_course_no_btn_share_app);
        CharSequence shareText = ResourceUtil.getFormattedString(getResources(), R.string.btn_share_app, "platform_name", platformName);
        shareBtn.setText(shareText);
        shareBtn.setOnClickListener(this);
        CharSequence errorText = ResourceUtil.getFormattedString(getResources(), R.string.error_no_friends_connected, "platform_name", platformName);
        TextView errorMessageView = (TextView)view.findViewById(R.id.error_message);
        errorMessageView.setText(errorText);
        return view;

    }

    @Override
    protected int getViewResourceID() {
        return R.layout.fragment_my_friends_course_list_tab;
    }

    protected void loadData(boolean forceRefresh,boolean showProgress) {

        if(forceRefresh){
            Intent clearFriends = new Intent(getActivity(), FetchCourseFriendsService.class);

            clearFriends.putExtra(FetchCourseFriendsService.TAG_FORCE_REFRESH, true);

            getActivity().startService(clearFriends);
        }

        Bundle args = new Bundle();
        args.putString(CoursesAsyncLoader.TAG_COURSE_OAUTH, FacebookSessionUtil.getAccessToken());

        getLoaderManager().restartLoader(FREINDS_COURSES_LOADER_ID, args, this);

    }


    @Override
    public Loader<AsyncTaskResult<List<EnrolledCoursesResponse>>> onCreateLoader(int i, Bundle bundle) {

        return new CoursesAsyncLoader(getActivity(), bundle, environment, environment.getServiceManager());

    }

    @Override
    public void onLoadFinished(Loader<AsyncTaskResult<List<EnrolledCoursesResponse>>> asyncTaskResultLoader, AsyncTaskResult<List<EnrolledCoursesResponse>> result) {

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        if(result.getEx() != null)
        {

            if(result.getEx() instanceof AuthException){
                PrefManager prefs = new PrefManager(getActivity(), PrefManager.Pref.LOGIN);
                prefs.clearAuth();

                logger.error(result.getEx());
                getActivity().finish();
            } else {
                adapter.clear();
                noFriendsLayout.setVisibility(View.VISIBLE);
                swipeLayout.setVisibility(View.GONE);
            }

        } else if (result.getResult() != null) {
            invalidateSwipeFunctionality();

            ArrayList<EnrolledCoursesResponse> newItems = new ArrayList<EnrolledCoursesResponse>(result.getResult());

            ((MyCoursesListActivity)getActivity()).updateDatabaseAfterDownload(newItems);

            if(result.getResult().size() == 0){
                adapter.clear();
                noFriendsLayout.setVisibility(View.VISIBLE);
                swipeLayout.setVisibility(View.GONE);
            } else {
                noFriendsLayout.setVisibility(View.GONE);
                swipeLayout.setVisibility(View.VISIBLE);
                adapter.setItems(newItems);
                adapter.notifyDataSetChanged();
            }

        } else {
            adapter.clear();
            noFriendsLayout.setVisibility(View.VISIBLE);
            swipeLayout.setVisibility(View.GONE);

        }

    }

    @Override
    public void onLoaderReset(Loader<AsyncTaskResult<List<EnrolledCoursesResponse>>> asyncTaskResultLoader) {
        adapter.clear();
        adapter.notifyDataSetChanged();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.friends_course_no_btn_share_app:
                FacebookProvider fbProvider = new FacebookProvider();
                FacebookDialog dialog = (FacebookDialog) fbProvider.shareApplication(getActivity());
                new InstallFacebookDialog().show(getFragmentManager(), null);
        }

    }

}
