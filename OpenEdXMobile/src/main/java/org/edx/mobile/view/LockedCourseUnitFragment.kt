package org.edx.mobile.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import org.edx.mobile.R
import org.edx.mobile.databinding.FragmentLockedCourseUnitBinding
import org.edx.mobile.extenstion.parcelableOrThrow
import org.edx.mobile.extenstion.serializableOrThrow
import org.edx.mobile.model.api.CourseUpgradeResponse
import org.edx.mobile.model.api.EnrolledCoursesResponse
import org.edx.mobile.model.course.CourseComponent
import org.edx.mobile.module.analytics.Analytics
import org.edx.mobile.module.analytics.AnalyticsRegistry
import javax.inject.Inject

@AndroidEntryPoint
class LockedCourseUnitFragment : CourseUnitFragment() {

    @Inject
    lateinit var analyticsRegistry: AnalyticsRegistry

    companion object {
        @JvmStatic
        fun newInstance(
            unit: CourseComponent,
            courseData: EnrolledCoursesResponse,
            courseUpgradeData: CourseUpgradeResponse
        ): LockedCourseUnitFragment {
            val fragment = LockedCourseUnitFragment()
            val bundle = Bundle()
            bundle.putSerializable(Router.EXTRA_COURSE_UNIT, unit)
            bundle.putSerializable(Router.EXTRA_COURSE_DATA, courseData)
            bundle.putParcelable(Router.EXTRA_COURSE_UPGRADE_DATA, courseUpgradeData)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLockedCourseUnitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val courseUpgradeData =
            arguments.parcelableOrThrow<CourseUpgradeResponse>(Router.EXTRA_COURSE_UPGRADE_DATA)
        val courseData =
            arguments.serializableOrThrow<EnrolledCoursesResponse>(Router.EXTRA_COURSE_DATA)
        loadPaymentBannerFragment(courseData, courseUpgradeData)
        analyticsRegistry.trackScreenView(Analytics.Screens.COURSE_UNIT_LOCKED)
    }

    private fun loadPaymentBannerFragment(
        courseData: EnrolledCoursesResponse,
        courseUpgradeData: CourseUpgradeResponse
    ) {
        PaymentsBannerFragment.loadPaymentsBannerFragment(
            R.id.fragment_container, courseData, unit,
            courseUpgradeData, false, childFragmentManager, false
        )
    }
}
