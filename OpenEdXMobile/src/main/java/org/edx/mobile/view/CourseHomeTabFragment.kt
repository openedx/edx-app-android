package org.edx.mobile.view

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.edx.mobile.R
import org.edx.mobile.base.BaseFragment.PermissionListener
import org.edx.mobile.base.BaseFragmentActivity
import org.edx.mobile.databinding.FragmentCourseHomeBinding
import org.edx.mobile.deeplink.DeepLink
import org.edx.mobile.deeplink.ScreenDef
import org.edx.mobile.event.CourseDashboardRefreshEvent
import org.edx.mobile.event.CourseOutlineRefreshEvent
import org.edx.mobile.event.LogoutEvent
import org.edx.mobile.event.MediaStatusChangeEvent
import org.edx.mobile.event.NetworkConnectivityChangeEvent
import org.edx.mobile.exception.CourseContentNotValidException
import org.edx.mobile.extenstion.setVisibility
import org.edx.mobile.http.HttpStatus
import org.edx.mobile.http.HttpStatusException
import org.edx.mobile.http.notifications.FullScreenErrorNotification
import org.edx.mobile.logger.Logger
import org.edx.mobile.model.api.CourseUpgradeResponse
import org.edx.mobile.model.api.EnrolledCoursesResponse
import org.edx.mobile.model.course.CourseComponent
import org.edx.mobile.model.course.HasDownloadEntry
import org.edx.mobile.model.course.SectionRow
import org.edx.mobile.model.course.VideoBlockModel
import org.edx.mobile.module.storage.DownloadCompletedEvent
import org.edx.mobile.module.storage.DownloadedVideoDeletedEvent
import org.edx.mobile.services.VideoDownloadHelper
import org.edx.mobile.services.VideoDownloadHelper.DownloadManagerCallback
import org.edx.mobile.util.AppConstants
import org.edx.mobile.util.NetworkUtil
import org.edx.mobile.util.NonNullObserver
import org.edx.mobile.util.PermissionsUtil
import org.edx.mobile.util.UiUtils
import org.edx.mobile.util.observer.EventObserver
import org.edx.mobile.view.adapters.CourseHomeAdapter
import org.edx.mobile.viewModel.CourseViewModel
import org.edx.mobile.viewModel.CourseViewModel.CoursesRequestType
import org.edx.mobile.viewModel.VideoViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@AndroidEntryPoint
class CourseHomeTabFragment : OfflineSupportBaseFragment(), DownloadManagerCallback,
    PermissionListener, CourseHomeAdapter.OnItemClickListener {

    private val logger = Logger(javaClass.name)
    private lateinit var binding: FragmentCourseHomeBinding
    private lateinit var adapter: CourseHomeAdapter

    private val courseViewModel: CourseViewModel by viewModels()

    @Inject
    lateinit var downloadManager: VideoDownloadHelper

    private var courseUpgradeData: CourseUpgradeResponse? = null
    private lateinit var courseData: EnrolledCoursesResponse
    private var courseComponentId: String? = null
    private var downloadEntries: MutableList<HasDownloadEntry>? = null

    private lateinit var errorNotification: FullScreenErrorNotification
    private var screenName: String? = null
    private var refreshOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            courseData = this.getSerializable(Router.EXTRA_COURSE_DATA) as EnrolledCoursesResponse
            courseUpgradeData = this.getParcelable(Router.EXTRA_COURSE_UPGRADE_DATA)
            courseComponentId = this.getString(Router.EXTRA_COURSE_COMPONENT_ID)
            screenName = this.getString(DeepLink.Keys.SCREEN_NAME)
        } ?: run {
            throw IllegalStateException("No arguments available")
        }
        permissionListener = this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCourseHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        errorNotification = FullScreenErrorNotification(binding.swipeContainer)
        initCourseObservers()
        initVideoObserver()
        fetchCourseComponents()
        binding.swipeContainer.setOnRefreshListener {
            fetchCourseComponents(coursesRequestType = CoursesRequestType.LIVE)
        }
        adapter = CourseHomeAdapter(environment.database, this@CourseHomeTabFragment)
        binding.rvCourseSections.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (refreshOnResume) {
            fetchCourseComponents(coursesRequestType = CoursesRequestType.LIVE)
            refreshOnResume = false
        }
    }

    override fun onRevisit() {
        super.onRevisit()
        courseViewModel.getCourseStatusInfo(courseData.courseId)
    }

    private fun fetchCourseComponents(
        coursesRequestType: CoursesRequestType = CoursesRequestType.APP_LEVEL_CACHE
    ) {
        // Prepare the loader. Either re-connect with an existing one or start a new one.
        if (environment.loginPrefs.isUserLoggedIn) {
            val courseId: String = courseData.courseId
            courseViewModel.getCourseData(
                courseId,
                showProgress = true,
                swipeRefresh = false,
                coursesRequestType = coursesRequestType
            )
            courseViewModel.getCourseStatusInfo(courseId)
        } else {
            EventBus.getDefault().post(LogoutEvent())
        }
    }

    private fun initCourseObservers() {
        courseViewModel.courseComponent.observe(
            viewLifecycleOwner,
            EventObserver { courseComponent ->
                if (isAdded) {
                    loadData(courseComponent)
                }
            })

        courseViewModel.lastAccessedComponent.observe(
            viewLifecycleOwner,
            EventObserver { lastAccessedComponent ->
                if (isAdded) {
                    showResumeCourseView(lastAccessedComponent)
                }
            })

        courseViewModel.showProgress.observe(viewLifecycleOwner, NonNullObserver { showProgress ->
            binding.loadingIndicator.loadingIndicator.setVisibility(showProgress)
        })

        courseViewModel.swipeRefresh.observe(viewLifecycleOwner, NonNullObserver { swipeRefresh ->
            binding.swipeContainer.isRefreshing = swipeRefresh
        })

        courseViewModel.handleError.observe(viewLifecycleOwner, NonNullObserver { throwable ->
            if (!isAdded) {
                return@NonNullObserver
            }
            if (throwable is HttpStatusException && throwable.statusCode == HttpStatus.UNAUTHORIZED) {
                EventBus.getDefault().post(LogoutEvent())
            } else {
                if (throwable is CourseContentNotValidException) {
                    errorNotification.showError(requireContext(), throwable)
                    logger.error(throwable, true)
                } else {
                    errorNotification.showError(
                        requireContext(),
                        throwable,
                        R.string.lbl_reload
                    ) {
                        if (NetworkUtil.isConnected(requireContext())) {
                            onRefresh()
                        }
                    }
                }

                if (!EventBus.getDefault().isRegistered(this)) {
                    EventBus.getDefault().register(this)
                }
            }
        })
    }

    /**
     * Adds resume course item view in the ListView.
     *
     * @param lastAccessedComponent The last accessed component
     */
    private fun showResumeCourseView(lastAccessedComponent: CourseComponent) {
        val sections = adapter.currentList.toMutableList()
        val resumeCourseItem = adapter.currentList.getOrNull(0)
        // if lastAccessedComponent is already exist
        if (resumeCourseItem?.type == SectionRow.RESUME_COURSE_ITEM) {
            sections[0] = SectionRow(SectionRow.RESUME_COURSE_ITEM, lastAccessedComponent)
        } else {
            sections.add(0, SectionRow(SectionRow.RESUME_COURSE_ITEM, lastAccessedComponent))
        }
        adapter.submitList(sections) {
            binding.rvCourseSections.smoothScrollToPosition(0)
        }
    }

    private fun loadData(courseComponent: CourseComponent) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        // retain the existing lastAccessedComponent
        val sectionList = courseComponent.sectionData.toMutableList()
        val resumeCourseItem = adapter.currentList.getOrNull(0)
        if (resumeCourseItem?.type == SectionRow.RESUME_COURSE_ITEM) {
            sectionList.add(0, resumeCourseItem)
        }
        adapter.submitList(sectionList)
        updateListUI()
    }

    override fun onSectionItemClick(
        itemView: LinearLayout, parentPosition: Int, childPosition: Int
    ) {
        val component = adapter.getItem(parentPosition, childPosition)
        if (component?.isContainer == true) {
            environment.router.showCourseContainerOutline(
                this@CourseHomeTabFragment,
                REQUEST_SHOW_COURSE_UNIT_DETAIL,
                courseData,
                courseUpgradeData,
                component.id,
                null,
                false
            )
        }
    }

    override fun resumeCourseClicked(lastAccessedComponent: CourseComponent) {
        environment.router.showCourseUnitDetail(
            this@CourseHomeTabFragment,
            REQUEST_SHOW_COURSE_UNIT_DETAIL,
            courseData,
            courseUpgradeData,
            lastAccessedComponent.id,
            false
        )
        environment.analyticsRegistry.trackResumeCourseBannerTapped(
            lastAccessedComponent.courseId,
            lastAccessedComponent.id
        )
    }

    override fun onSectionItemLongClick(
        itemView: LinearLayout, parentPosition: Int, childPosition: Int
    ) {
        val bulkDownloadIcon: View? = itemView.findViewById(R.id.bulk_download)
        if (bulkDownloadIcon?.tag as Int? == R.drawable.ic_download_done) {
            val checkItemPosition = Pair(parentPosition, childPosition)
            VideoMoreOptionsBottomSheet.newInstance(checkItemPosition)
                .show(childFragmentManager, null)
            adapter.setItemChecked(
                parentPosition,
                childPosition
            )
        }
    }

    override fun viewDownloadsStatus() {
        environment.router.showDownloads(activity)
    }

    override fun download(downloadableVideos: List<HasDownloadEntry>) {
        downloadEntries = downloadableVideos.toMutableList()
        askForPermission(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PermissionsUtil.WRITE_STORAGE_PERMISSION_REQUEST
        )
    }

    private fun initVideoObserver() {
        val videoViewModel = ViewModelProvider(this).get(
            VideoViewModel::class.java
        )
        videoViewModel.selectedVideosPosition.observe(
            viewLifecycleOwner,
            EventObserver { position: Pair<Int, Int> ->
                if (position.first != RecyclerView.NO_POSITION) {
                    deleteDownloadedVideosAtPosition(position)
                }
            })
        videoViewModel.clearChoices.observe(
            viewLifecycleOwner,
            EventObserver { shouldClear: Boolean ->
                if (shouldClear) {
                    adapter.clearChoices()
                }
            })
    }

    private fun deleteDownloadedVideosAtPosition(position: Pair<Int, Int>) {
        // Change the icon to download icon immediately
        val outerViewHolder =
            binding.rvCourseSections.findViewHolderForLayoutPosition(position.first)
        val innerViewHolder =
            outerViewHolder?.itemView?.findViewById<RecyclerView>(R.id.rv_sub_section)
                ?.findViewHolderForAdapterPosition(position.second)
        if (innerViewHolder?.itemView != null) {
            // rowView will be null, if the user scrolls away from the checked item
            val bulkDownloadIcon =
                innerViewHolder.itemView.findViewById<AppCompatImageView>(R.id.bulk_download)
            bulkDownloadIcon.setImageDrawable(
                UiUtils.getDrawable(requireContext(), R.drawable.ic_download)
            )
            bulkDownloadIcon.tag = R.drawable.ic_download
        }
        val rowItem = adapter.getItem(position.first, position.second)
        rowItem?.let {
            val videos = rowItem.getVideos(true)
            environment.analyticsRegistry.trackSubsectionVideosDelete(
                courseData.courseId,
                rowItem.id
            )
            showVideosDeletedSnackBar(rowItem, videos)
        }
    }

    private fun showVideosDeletedSnackBar(
        courseComponent: CourseComponent,
        videos: List<CourseComponent>
    ) {
        /*
          The android docs have NOT been updated yet, but if you jump into the source code you'll
          notice that the parameter to the method setDuration(int duration) can either be one of
          LENGTH_SHORT, LENGTH_LONG, LENGTH_INDEFINITE or a custom duration in milliseconds.

          https://stackoverflow.com/a/30552666
          https://github.com/material-components/material-components-android/commit/2cb77c9331cc3c6a5034aace0238b96508acf47d
         */
        @SuppressLint("WrongConstant") val snackBar = Snackbar.make(
            binding.rvCourseSections,
            resources.getQuantityString(
                R.plurals.delete_video_snackbar_msg,
                videos.size,
                videos.size
            ),
            AppConstants.SNACKBAR_SHOWTIME_MS
        )
        snackBar.setAction(R.string.label_undo) { }
        snackBar.addCallback(object : BaseCallback<Snackbar?>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                // SnackBar is being dismissed by any action other than its action button's press
                if (event != DISMISS_EVENT_ACTION) {
                    val storage = environment.storage
                    for (video in videos) {
                        val videoBlockModel = video as VideoBlockModel
                        val downloadEntry = videoBlockModel.getDownloadEntry(storage)
                        if (downloadEntry != null && downloadEntry.isDownloaded) {
                            // This check is necessary because, this callback gets called multiple
                            // times when SnackBar is about to dismiss and the activity finishes
                            storage.removeDownload(downloadEntry)
                        } else {
                            return
                        }
                    }
                } else {
                    environment.analyticsRegistry.trackUndoingSubsectionVideosDelete(
                        courseData.courseId, courseComponent.id
                    )
                }
                updateListUI()
            }
        })
        snackBar.show()
    }

    override fun onPermissionGranted(permissions: Array<out String>?, requestCode: Int) {
        when (requestCode) {
            PermissionsUtil.WRITE_STORAGE_PERMISSION_REQUEST ->
                downloadManager.downloadVideos(downloadEntries, activity, this)
        }
    }

    override fun onPermissionDenied(permissions: Array<out String>?, requestCode: Int) {
        if (downloadEntries != null) {
            downloadEntries?.clear()
            downloadEntries = null
        }
    }

    override fun onDownloadStarted(result: Long) {
        updateListUI()
    }

    override fun onDownloadFailedToStart() {
        updateListUI()
    }

    override fun showProgressDialog(numDownloads: Int) {
        // nothing to do.
    }

    override fun updateListUI() {
        adapter.updateList()
    }

    override fun showInfoMessage(message: String?): Boolean {
        return activity is BaseFragmentActivity &&
                (activity as BaseFragmentActivity).showInfoMessage(message)
    }

    fun onRefresh() {
        EventBus.getDefault().post(CourseDashboardRefreshEvent())
    }

    override fun isShowingFullScreenError(): Boolean {
        return errorNotification.isShowing
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(sticky = true)
    fun onEventMainThread(event: MediaStatusChangeEvent) {
        updateListUI()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadCompletedEvent) {
        updateListUI()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadedVideoDeletedEvent) {
        updateListUI()
    }

    @Subscribe(sticky = true)
    fun onEvent(event: NetworkConnectivityChangeEvent) {
        onNetworkConnectivityChangeEvent(event)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(sticky = true)
    fun onEvent(event: CourseDashboardRefreshEvent) {
        errorNotification.hideError()
        fetchCourseComponents()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(sticky = true)
    fun onEvent(event: CourseOutlineRefreshEvent) {
        errorNotification.hideError()
        refreshOnResume = true
    }

    companion object {
        @JvmStatic
        fun makeArguments(
            model: EnrolledCoursesResponse,
            courseComponentId: String?,
            @ScreenDef screenName: String?
        ): Bundle {
            val courseBundle = Bundle()
            courseBundle.putSerializable(Router.EXTRA_COURSE_DATA, model)
            courseBundle.putString(Router.EXTRA_COURSE_COMPONENT_ID, courseComponentId)
            courseBundle.putString(Router.EXTRA_SCREEN_NAME, screenName)
            return courseBundle
        }
    }
}
