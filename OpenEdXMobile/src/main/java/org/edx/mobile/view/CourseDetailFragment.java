package org.edx.mobile.view;

import static org.edx.mobile.http.util.CallUtil.executeStrict;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;

import org.edx.mobile.R;
import org.edx.mobile.base.BaseFragment;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.course.CourseAPI;
import org.edx.mobile.http.callback.CallTrigger;
import org.edx.mobile.http.callback.ErrorHandlingCallback;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.model.course.CourseDetail;
import org.edx.mobile.util.UiUtils;
import org.edx.mobile.util.WebViewUtil;
import org.edx.mobile.util.images.CourseCardUtils;
import org.edx.mobile.util.images.TopAnchorFillWidthTransformation;
import org.edx.mobile.view.common.TaskMessageCallback;
import org.edx.mobile.view.common.TaskProgressCallback;
import org.edx.mobile.view.custom.EdxWebView;
import org.edx.mobile.view.custom.URLInterceptorWebViewClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * Main fragment that populates the course detail screen. The course card fragment is created first
 * and then the additional items are added if given.
 */
@AndroidEntryPoint
public class CourseDetailFragment extends BaseFragment {

    private TextView mCourseTextName;
    private TextView mCourseTextDetails;
    private AppCompatImageView mHeaderImageView;
    private AppCompatImageView mHeaderPlayIcon;

    private LinearLayout mCourseDetailLayout;

    private TextView mShortDescription;

    private LinearLayout courseDetailFieldLayout;
    private FrameLayout courseAbout;
    private EdxWebView courseAboutWebView;

    private Button mEnrollButton;
    private boolean mEnrolled = false;

    boolean emailOptIn = true;

    static public final String COURSE_DETAIL = "course_detail";

    CourseDetail courseDetail;

    protected final Logger logger = new Logger(getClass().getName());

    @Inject
    CourseAPI courseApi;

    @Inject
    IEdxEnvironment environment;

    private final ActivityResultLauncher<Intent> loginRequestLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    enrollInCourse();
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseExtras();
    }

    /**
     * Sets the view for the Course Card and play button if there is an intro video.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Course Card View
        final View view = inflater.inflate(R.layout.fragment_course_dashboard, container, false);
        mCourseTextName = view.findViewById(R.id.course_detail_name);
        mCourseTextDetails = view.findViewById(R.id.course_detail_extras);
        mHeaderImageView = view.findViewById(R.id.header_image_view);
        mHeaderPlayIcon = view.findViewById(R.id.header_play_icon);
        mCourseDetailLayout = view.findViewById(R.id.dashboard_detail);

        mHeaderPlayIcon.setOnClickListener(v -> {
            Uri uri = Uri.parse(courseDetail.media.course_video.uri);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Populates the course details.
     * Creates and populates the short description if given,
     * Creates and populates fields such as "effort", "duration" if any. This is handled on a case
     * by case basis rather than using a list view.
     * Sets the view for About this Course which is retrieved in a later api call.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Short Description
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View child = inflater.inflate(R.layout.fragment_course_detail, mCourseDetailLayout, false);
        mShortDescription = child.findViewById(R.id.course_detail_short_description);
        if (courseDetail.short_description == null || courseDetail.short_description.isEmpty()) {
            ((ViewGroup) mShortDescription.getParent()).removeView(mShortDescription);
        }
        mCourseDetailLayout.addView(child);

        // Enrollment Button
        mEnrollButton = child.findViewById(R.id.button_enroll_now);
        configureEnrollButton();

        // Course Detail Fields - Each field will be created manually.

        courseDetailFieldLayout = view.findViewById(R.id.course_detail_fields);
        if (courseDetail.effort != null && !courseDetail.effort.isEmpty()) {
            ViewHolder holder = createCourseDetailFieldViewHolder(inflater, mCourseDetailLayout);
            holder.rowIcon.setImageDrawable(UiUtils.INSTANCE.getDrawable(requireContext(), R.drawable.ic_dashboard));
            holder.rowFieldName.setText(R.string.effort_field_name);
            holder.rowFieldText.setText(courseDetail.effort);
        }

        //  About this Course
        courseAbout = view.findViewById(R.id.course_detail_course_about);
        courseAboutWebView = courseAbout.findViewById(R.id.course_detail_course_about_webview);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setCourseImage();
        setCourseVideoButton();
        setCourseCardText();
        mShortDescription.setText(courseDetail.short_description);
        populateAboutThisCourse();
    }

    private void setCourseCardText() {
        String formattedDate = CourseCardUtils.getFormattedDate(
                getActivity(),
                courseDetail.start,
                courseDetail.end,
                courseDetail.start_type,
                courseDetail.start_display);
        mCourseTextDetails.setText(CourseCardUtils.getDescription(courseDetail.org, courseDetail.number, formattedDate));
        mCourseTextName.setText(courseDetail.name);
    }

    private void parseExtras() {
        courseDetail = getArguments().getParcelable(COURSE_DETAIL);
    }

    private void setCourseImage() {
        final String headerImageUrl = courseDetail.media.course_image.getUri(environment.getConfig().getApiHostURL());
        Glide.with(CourseDetailFragment.this)
                .load(headerImageUrl)
                .placeholder(R.drawable.placeholder_course_card_image)
                .transform(new TopAnchorFillWidthTransformation())
                .into(mHeaderImageView);
    }

    /**
     * Shows and enables the play button if the video url was provided.
     */
    private void setCourseVideoButton() {
        if (courseDetail.media.course_video.uri == null || courseDetail.media.course_video.uri.isEmpty()) {
            mHeaderPlayIcon.setEnabled(false);
        } else {
            mHeaderPlayIcon.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Makes a call the the course details api and sets the overview if given. If there is no
     * overview, remove the courseAbout view.
     */
    private void populateAboutThisCourse() {
        final Call<CourseDetail> getCourseDetailCall = courseApi.getCourseDetail(courseDetail.course_id);
        final Activity activity = getActivity();
        final TaskProgressCallback pCallback = activity instanceof TaskProgressCallback ? (TaskProgressCallback) activity : null;
        final TaskMessageCallback mCallback = activity instanceof TaskMessageCallback ? (TaskMessageCallback) activity : null;
        getCourseDetailCall.enqueue(new ErrorHandlingCallback<>(requireActivity(),
                pCallback, mCallback, CallTrigger.LOADING_CACHED) {
            @Override
            protected void onResponse(@NonNull final CourseDetail courseDetail) {
                if (courseDetail.overview != null && !courseDetail.overview.isEmpty()) {
                    populateAboutThisCourse(courseDetail.overview);
                } else {
                    courseAbout.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Takes a string which can include html and then renders it into the courseAbout webview.
     *
     * @param overview A string that can contain html tags
     */
    private void populateAboutThisCourse(String overview) {
        courseAbout.setVisibility(View.VISIBLE);
        URLInterceptorWebViewClient client = new URLInterceptorWebViewClient(requireActivity(),
                courseAboutWebView, false, null);
        client.setAllLinksAsExternal(true);

        StringBuilder buff = WebViewUtil.getIntialWebviewBuffer(getActivity(), logger);

        buff.append("<body>");
        buff.append("<div class=\"header\">");
        buff.append(overview);
        buff.append("</div>");
        buff.append("</body>");

        courseAboutWebView.loadDataWithBaseURL(environment.getConfig().getApiHostURL(), buff.toString(), "text/html", StandardCharsets.UTF_8.name(), null);
    }

    /**
     * Creates a ViewHolder for a course detail field such as "effort" or "duration" and then adds
     * it to the top of the list.
     */
    private ViewHolder createCourseDetailFieldViewHolder(LayoutInflater inflater, LinearLayout parent) {
        ViewHolder holder = new ViewHolder();
        holder.rowView = inflater.inflate(R.layout.course_detail_field, parent, false);

        holder.rowIcon = holder.rowView.findViewById(R.id.course_detail_field_icon);
        holder.rowFieldName = holder.rowView.findViewById(R.id.course_detail_field_name);
        holder.rowFieldText = holder.rowView.findViewById(R.id.course_detail_field_text);

        courseDetailFieldLayout.addView(holder.rowView, 0);
        return holder;
    }

    private static class ViewHolder {
        View rowView;
        AppCompatImageView rowIcon;
        TextView rowFieldName;
        TextView rowFieldText;
    }


    /**
     * Sets the onClickListener and the text for the enrollment button.
     * <br/>
     * If the current course is found in the list of cached course enrollment list, the button will
     * be for viewing a course, otherwise, it will be used to enroll in a course. One clicked, user
     * is then taken to the dashboard for target course.
     */
    private void configureEnrollButton() {
        // This call should already be cached, if not, set button as if not enrolled.
        try {
            List<EnrolledCoursesResponse> enrolledCoursesResponse =
                    executeStrict(courseApi.getEnrolledCoursesFromCache()).getEnrollments();
            for (EnrolledCoursesResponse course : enrolledCoursesResponse) {
                if (course.getCourse().getId().equals(courseDetail.course_id)) {
                    mEnrolled = true;
                    break;
                }
            }
        } catch (Exception ex) {
            logger.debug("Unable to get cached enrollments list");
        }

        if (mEnrolled) {
            mEnrollButton.setText(R.string.view_course_button_text);
        } else if (courseDetail.invitation_only != null && courseDetail.invitation_only) {
            mEnrollButton.setText(R.string.invitation_only_button_text);
            mEnrollButton.setEnabled(false);
        } else {
            mEnrollButton.setText(R.string.enroll_now_button_text);
        }

        mEnrollButton.setOnClickListener(v -> {
            if (!mEnrolled) {
                enrollInCourse();
            } else {
                openCourseDashboard();
            }
        });
    }

    /**
     * Enroll in a course, Then open the course dashboard of the enrolled course.
     */
    public void enrollInCourse() {
        if (!environment.getLoginPrefs().isUserLoggedIn()) {
            loginRequestLauncher.launch(environment.getRouter().getRegisterIntent());
            return;
        }
        environment.getAnalyticsRegistry().trackEnrollClicked(courseDetail.course_id, emailOptIn);
        courseApi.enrollInACourse(courseDetail.course_id, emailOptIn)
                .enqueue(new CourseAPI.EnrollCallback(requireActivity(), null) {
                    @Override
                    protected void onResponse(@NonNull final ResponseBody responseBody) {
                        super.onResponse(responseBody);
                        mEnrolled = true;
                        logger.debug("Enrollment successful: " + courseDetail.course_id);
                        mEnrollButton.setText(R.string.view_course_button_text);
                        Toast.makeText(getActivity(), R.string.you_are_now_enrolled, Toast.LENGTH_SHORT).show();

                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                courseApi.getEnrolledCourses().enqueue(new CourseAPI.GetCourseByIdCallback(
                                        requireActivity(),
                                        courseDetail.course_id,
                                        null) {
                                    @Override
                                    protected void onResponse(@NonNull EnrolledCoursesResponse course) {
                                        environment.getRouter().showCourseDashboardTabs(requireActivity(), course);
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    protected void onFailure(@NonNull final Throwable error) {
                        Toast.makeText(getActivity(), R.string.enrollment_failure, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Open course dashboard for given course from the enrollments list cache.
     */
    private void openCourseDashboard() {
        try {
            List<EnrolledCoursesResponse> enrolledCoursesResponse =
                    executeStrict(courseApi.getEnrolledCoursesFromCache()).getEnrollments();
            for (EnrolledCoursesResponse course : enrolledCoursesResponse) {
                if (course.getCourse().getId().equals(courseDetail.course_id)) {
                    environment.getRouter().showCourseDashboardTabs(requireActivity(), course);
                }
            }
        } catch (Exception exception) {
            logger.debug(exception.toString());
            Toast.makeText(getContext(), R.string.cannot_show_dashboard, Toast.LENGTH_SHORT).show();
        }
    }
}
