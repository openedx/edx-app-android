package org.humana.mobile.tta.ui.deep_link.view_model;

import android.os.Bundle;

import org.humana.mobile.tta.Constants;
import org.humana.mobile.tta.data.enums.SourceType;
import org.humana.mobile.tta.data.local.db.table.Content;
import org.humana.mobile.tta.interfaces.OnResponseCallback;
import org.humana.mobile.tta.ui.base.mvvm.BaseVMActivity;
import org.humana.mobile.tta.ui.base.mvvm.BaseViewModel;
import org.humana.mobile.tta.ui.connect.ConnectDashboardActivity;
import org.humana.mobile.tta.ui.course.CourseDashboardActivity;
import org.humana.mobile.tta.ui.landing.LandingActivity;
import org.humana.mobile.tta.utils.ActivityUtil;

public class DeepLinkViewModel extends BaseViewModel {

    public DeepLinkViewModel(BaseVMActivity activity) {
        super(activity);
    }

    public void fetchContent(long contentId){

        mDataManager.getContent(contentId, new OnResponseCallback<Content>() {
            @Override
            public void onSuccess(Content data) {
                Bundle parameters = new Bundle();
                parameters.putParcelable(Constants.KEY_CONTENT, data);
                parameters.putBoolean(Constants.KEY_IS_PUSH, true);
                if (data.getSource().getType().equalsIgnoreCase(SourceType.course.name()) ||
                        data.getSource().getType().equalsIgnoreCase(SourceType.edx.name())) {
                    ActivityUtil.gotoPage(mActivity, CourseDashboardActivity.class, parameters);
                } else {
                    ActivityUtil.gotoPage(mActivity, ConnectDashboardActivity.class, parameters);
                }
                mActivity.finish();
            }

            @Override
            public void onFailure(Exception e) {
                ActivityUtil.gotoPage(mActivity, LandingActivity.class);
                mActivity.finish();
            }
        });

    }

    public void fetchContent(String sourceIdentity) {

        mDataManager.getContentFromSourceIdentity(sourceIdentity, new OnResponseCallback<Content>() {
            @Override
            public void onSuccess(Content data) {
                Bundle parameters = new Bundle();
                parameters.putParcelable(Constants.KEY_CONTENT, data);
                parameters.putBoolean(Constants.KEY_IS_PUSH, true);
                if (data.getSource().getType().equalsIgnoreCase(SourceType.course.name()) ||
                        data.getSource().getType().equalsIgnoreCase(SourceType.edx.name())) {
                    ActivityUtil.gotoPage(mActivity, CourseDashboardActivity.class, parameters);
                } else {
                    ActivityUtil.gotoPage(mActivity, ConnectDashboardActivity.class, parameters);
                }
                mActivity.finish();
            }

            @Override
            public void onFailure(Exception e) {
                ActivityUtil.gotoPage(mActivity, LandingActivity.class);
                mActivity.finish();
            }
        });

    }
}