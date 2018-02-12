package org.edx.mobile.services;

import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
import android.view.View;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.edx.mobile.R;
import org.edx.mobile.base.BaseFragmentActivity;
import org.edx.mobile.base.MainApplication;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.course.HasDownloadEntry;
import org.edx.mobile.model.db.DownloadEntry;
import org.edx.mobile.model.download.NativeDownloadModel;
import org.edx.mobile.module.analytics.AnalyticsRegistry;
import org.edx.mobile.module.db.DataCallback;
import org.edx.mobile.module.storage.IStorage;
import org.edx.mobile.task.EnqueueDownloadTask;
import org.edx.mobile.util.MediaConsentUtils;
import org.edx.mobile.util.MemoryUtil;
import org.edx.mobile.util.NetworkUtil;
import org.edx.mobile.view.dialog.DownloadSizeExceedDialog;
import org.edx.mobile.view.dialog.IDialogCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Singleton
public class VideoDownloadHelper {
    public interface DownloadManagerCallback {
        void onDownloadStarted(Long result);

        void onDownloadFailedToStart();

        void showProgressDialog(int numDownloads);

        void updateListUI();

        boolean showInfoMessage(String message);
    }

    public interface DownloadProgressCallback {
        void giveProgressStatus(NativeDownloadModel downloadModel);
        void startProgress();
        void stopProgress();
    }

    protected static final Logger logger = new Logger(VideoDownloadHelper.class.getName());

    private DownloadSizeExceedDialog downloadFragment;

    @Inject
    private IStorage storage;

    @Inject
    private AnalyticsRegistry analyticsRegistry;


    public void downloadVideos(final List<? extends HasDownloadEntry> model, final FragmentActivity activity,
                               final DownloadManagerCallback callback) {
        if (model == null || model.isEmpty()) {
            return;
        }
        try {
            IDialogCallback dialogCallback = new IDialogCallback() {
                @Override
                public void onPositiveClicked() {
                    startDownloadVideos(model, activity, callback);
                }

                @Override
                public void onNegativeClicked() {
                    callback.showInfoMessage(activity.getString(R.string.wifi_off_message));
                }
            };
            MediaConsentUtils.requestStreamMedia(activity, dialogCallback);

        } catch (Exception e) {
            logger.error(e);
        }

    }

    private void startDownloadVideos(List<? extends HasDownloadEntry> model, FragmentActivity activity, DownloadManagerCallback callback) {
        long downloadSize = 0;
        ArrayList<DownloadEntry> downloadList = new ArrayList<DownloadEntry>();
        int downloadCount = 0;
        for (HasDownloadEntry v : model) {
            DownloadEntry de = v.getDownloadEntry(storage);
            if (null == de
                    || de.downloaded == DownloadEntry.DownloadedState.DOWNLOADING
                    || de.downloaded == DownloadEntry.DownloadedState.DOWNLOADED
                    || de.isVideoForWebOnly) {
                continue;
            } else {
                downloadSize = downloadSize
                        + de.getSize();
                downloadList.add(de);
                downloadCount++;
            }
        }
        if (downloadSize > MemoryUtil
                .getAvailableExternalMemory(activity)) {
            ((BaseFragmentActivity) activity).showInfoMessage(activity.getString(R.string.file_size_exceeded));
            callback.updateListUI();
        } else {
            if (downloadSize < MemoryUtil.GB && !downloadList.isEmpty()) {
                startDownload(downloadList, activity, callback);

                final DownloadEntry downloadEntry = downloadList.get(0);
                analyticsRegistry.trackSubSectionBulkVideoDownload(downloadEntry.getSectionName(),
                        downloadEntry.getChapterName(), downloadEntry.getEnrollmentId(),
                        downloadCount);
            } else {
                showDownloadSizeExceedDialog(downloadList, downloadCount, activity, callback);
            }
        }
    }

    // Dialog fragment to display message to user regarding
    private void showDownloadSizeExceedDialog(final ArrayList<DownloadEntry> de,
                                              final int noOfDownloads, final FragmentActivity activity, final DownloadManagerCallback callback) {
        Map<String, String> dialogMap = new HashMap<String, String>();
        dialogMap.put("title", activity.getString(R.string.download_exceed_title));
        dialogMap.put("message_1", activity.getString(R.string.download_exceed_message));
        downloadFragment = DownloadSizeExceedDialog.newInstance(dialogMap,
                new IDialogCallback() {
                    @Override
                    public void onPositiveClicked() {
                        if (!de.isEmpty()) {
                            startDownload(de, activity, callback);

                            final DownloadEntry downloadEntry = de.get(0);
                            analyticsRegistry.trackSubSectionBulkVideoDownload(downloadEntry.getSectionName(),
                                    downloadEntry.getChapterName(), downloadEntry.getEnrollmentId(),
                                    noOfDownloads);
                        }
                    }

                    @Override
                    public void onNegativeClicked() {
                        //  updateList();
                        downloadFragment.dismiss();
                    }
                });
        downloadFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        downloadFragment.show(activity.getSupportFragmentManager(), "dialog");
        downloadFragment.setCancelable(false);
    }

    public void downloadVideo(DownloadEntry downloadEntry, final FragmentActivity activity, final DownloadManagerCallback callback) {
        List<DownloadEntry> downloadEntries = new ArrayList<>();
        downloadEntries.add(downloadEntry);
        startDownload(downloadEntries, activity, callback);
        analyticsRegistry.trackSingleVideoDownload(downloadEntry.getVideoId(),
                downloadEntry.getEnrollmentId(), downloadEntry.getVideoUrl());
    }

    private void startDownload(List<DownloadEntry> downloadList,
                               final FragmentActivity activity,
                               final DownloadManagerCallback callback) {
        if (downloadList.isEmpty()) return;

        EnqueueDownloadTask downloadTask = new EnqueueDownloadTask(activity, downloadList) {
            @Override
            public void onSuccess(Long result) {
                callback.onDownloadStarted(result);
            }

            @Override
            public void onException(Exception ex) {
                super.onException(ex);
                callback.onDownloadFailedToStart();
            }
        };

        callback.showProgressDialog(downloadList.size());
        downloadTask.setTaskProcessCallback(null);
        downloadTask.execute();
    }

    /**
     * Utility for subscribing to the downloads happening through {@link android.app.DownloadManager DownloadManager}.
     * <br/>
     * Note: Unregistering from download progress updates is the caller's responsibility by utilising
     * the returned object of this function.
     * <br/>
     * Auto unregister will only happen if the registering view is destroyed or the
     * subscribed downloads finish.
     *
     * @param courseId        Course's Id.
     * @param registeringView The view that's interested in getting download callbacks and updating itself accordingly.
     * @param callback        Callback to listen to specific events fired during the download is in progress.
     * @return The task that's listening to in progress downloads.
     */
    public static Runnable registerForDownloadProgress(@Nullable final String courseId,
                                                       @Nullable final View registeringView,
                                                       @Nullable final DownloadProgressCallback callback) {
        final IEdxEnvironment environment = MainApplication.getEnvironment(MainApplication.instance());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (registeringView != null && callback != null) {
                    if (!NetworkUtil.isConnected(registeringView.getContext()) ||
                            !environment.getDatabase().isAnyVideoDownloading(null)) {
                        callback.stopProgress();
                    } else {
                        callback.startProgress();
                        environment.getStorage().getDownloadProgressOfCourseVideos(courseId,
                                new DataCallback<NativeDownloadModel>() {
                                    @Override
                                    public void onResult(NativeDownloadModel result) {
                                        callback.giveProgressStatus(result);
                                    }

                                    @Override
                                    public void onFail(Exception ex) {
                                        logger.error(ex);
                                    }
                                });
                        registeringView.postDelayed(this, DateUtils.SECOND_IN_MILLIS);
                    }
                }
            }
        };
        runnable.run();
        return runnable;
    }
}
