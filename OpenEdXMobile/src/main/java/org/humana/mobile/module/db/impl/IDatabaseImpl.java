package org.humana.mobile.module.db.impl;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang.ArrayUtils;
import org.humana.mobile.model.VideoModel;
import org.humana.mobile.model.course.CourseComponent;
import org.humana.mobile.model.db.DownloadEntry.DownloadedState;
import org.humana.mobile.model.db.DownloadEntry.WatchedState;
import org.humana.mobile.module.db.DataCallback;
import org.humana.mobile.module.db.DbStructure;
import org.humana.mobile.module.db.IDatabase;
import org.humana.mobile.module.prefs.LoginPrefs;
import org.humana.mobile.tta.analytics.AnalyticModel;
import org.humana.mobile.tta.analytics.analytics_enums.Action;
import org.humana.mobile.tta.analytics.db_operations.DbOperationGetAnalytic;
import org.humana.mobile.tta.data.local.db.operation.DbOperationGetTinCanPayload;
import org.humana.mobile.tta.tincan.model.Resume;
import org.humana.mobile.util.Sha1Util;
import org.humana.mobile.util.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class IDatabaseImpl extends IDatabaseBaseImpl implements IDatabase {

    @Inject
    private final LoginPrefs loginPrefs;

    @Inject
    public IDatabaseImpl(Context context, LoginPrefs loginPrefs) {
        super(context);
        this.loginPrefs = loginPrefs;
    }

    @Nullable
    private String username() {
        final String username = loginPrefs.getUsername();
        return (username != null) ? Sha1Util.SHA1(username) : null;
    }

    @Override
    public Boolean isAnyVideoDownloading(final DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.USERNAME + "=? AND " + DbStructure.Column.DOWNLOADED + "=?",
                new String[]{username(), String.valueOf(DownloadedState.DOWNLOADING.ordinal())},
                null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public List<Long> getAllDownloadingVideosDmidList(final DataCallback<List<Long>> callback) {
        DbOperationGetColumn<Long> op = new DbOperationGetColumn<Long>(true,
                DbStructure.Table.DOWNLOADS, new String[]{DbStructure.Column.DM_ID},
                DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADING.ordinal()), username()},
                null, Long.class);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer updateAllVideosAsDeactivated(final DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.IS_COURSE_ACTIVE, false);

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.USERNAME + "=?", new String[]{username()});
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Integer updateVideosActivatedForCourse(String enrollmentId,
                                                  final DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.IS_COURSE_ACTIVE, true);

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.EID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{enrollmentId, username()});
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public List<VideoModel> getAllDeactivatedVideos(final DataCallback<List<VideoModel>> callback) {
        DbOperationGetVideos op = new DbOperationGetVideos(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.IS_COURSE_ACTIVE + "=? AND "
                        + DbStructure.Column.USERNAME + "=? ",
                new String[]{"0", username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer updateVideoAsOnlineByVideoId(String videoId,
                                                final DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.DM_ID, 0);
        values.put(DbStructure.Column.FILEPATH, "");
        values.put(DbStructure.Column.DOWNLOADED, DownloadedState.ONLINE.ordinal());

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoId, username()});
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Integer getVideoCountBydmId(long dmId, final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.DM_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(dmId), username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Boolean isVideoDownloadedInChapter(String enrollmentId,
                                              String chapter,
                                              final DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.CHAPTER + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.USERNAME
                        + "=?", new String[]{chapter, enrollmentId,
                String.valueOf(DownloadedState.DOWNLOADED.ordinal()), username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Integer getVideosCountByChapter(String enrollmentId, String chapter,
                                           final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.CHAPTER + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.DOWNLOADED + "!=? AND " + DbStructure.Column.USERNAME
                        + "=?", new String[]{chapter, enrollmentId,
                String.valueOf(DownloadedState.ONLINE.ordinal()), username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer getWebOnlyVideosCountByChapter(String enrollmentId, String chapter,
                                                  final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.CHAPTER + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.VIDEO_FOR_WEB_ONLY + "==1 AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{chapter, enrollmentId, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Boolean isVideoDownloadingInChapter(String enrollmentId,
                                               String chapter,
                                               final DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.CHAPTER + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.USERNAME
                        + "=?", new String[]{chapter, enrollmentId,
                String.valueOf(DownloadedState.DOWNLOADING.ordinal()), username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public List<Long> getDownloadingVideoDmIdsForChapter(String enrollmentId,
                                                         String chapter,
                                                         final DataCallback<List<Long>> callback) {
        DbOperationGetColumn<Long> op = new DbOperationGetColumn<Long>(true,
                DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.DM_ID},
                DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.CHAPTER + "=? AND " + DbStructure.Column.USERNAME
                        + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADING.ordinal()), enrollmentId,
                        chapter, username()}, null, Long.class);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Boolean isVideoDownloadingInSection(String enrollmentId,
                                               String chapter, String section,
                                               final DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.SECTION + "=? AND " + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.EID + "=? AND " + DbStructure.Column.DOWNLOADED
                        + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{section, chapter, enrollmentId,
                        String.valueOf(DownloadedState.DOWNLOADING.ordinal()), username()}, null);
        op.setCallback(callback);
        return enqueue(op);

    }

    @Override
    public long[] getDownloadingVideoDmIdsForSection(String enrollmentId, String chapter,
                                                     String section,
                                                     final DataCallback<List<Long>> callback) {
        DbOperationGetColumn<Long> op = new DbOperationGetColumn<Long>(true,
                DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.DM_ID},
                DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.SECTION + "=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADING.ordinal()),
                        enrollmentId, chapter, section, username()}, null, Long.class);
        op.setCallback(callback);
        List<Long> downloadingList = enqueue(op);
        if (callback != null) {
            return null;
        } else {
            return ArrayUtils.toPrimitive(
                    downloadingList.toArray(new Long[downloadingList.size()]));
        }
    }

    @Override
    public int getDownloadingVideosCountForSection(String enrollmentId, String chapter,
                                                   String section,
                                                   final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.DM_ID},
                DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.SECTION + "=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADING.ordinal()),
                        enrollmentId, chapter, section, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Boolean isVideoDownloadedInSection(String enrollmentId,
                                              String chapter, String section,
                                              final DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.SECTION + "=? AND " + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.DOWNLOADED + "=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{section, chapter, enrollmentId,
                        String.valueOf(DownloadedState.DOWNLOADED.ordinal()), username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public long[] getDownloadedVideoDmIdsForSection(String enrollmentId, String chapter,
                                                    String section,
                                                    final DataCallback<List<Long>> callback) {
        DbOperationGetColumn<Long> op = new DbOperationGetColumn<Long>(true,
                DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.DM_ID},
                DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.SECTION + "=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADED.ordinal()),
                        enrollmentId, chapter, section, username()}, null, Long.class);
        op.setCallback(callback);
        List<Long> downloadedList = enqueue(op);
        if (callback != null) {
            return null;
        } else {
            return ArrayUtils.toPrimitive(downloadedList.toArray(new Long[downloadedList.size()]));
        }
    }

    @Override
    public int getDownloadedVideosCountForSection(String enrollmentId, String chapter,
                                                  String section,
                                                  final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.DM_ID},
                DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.SECTION + "=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADED.ordinal()),
                        enrollmentId, chapter, section, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    //Added by Arjun to get downloaded scrom conut in device
    @Override
    public Integer getDownloadedScromCountByCourse(String courseId,
                                                   final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(true, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},


                DbStructure.Column.EID + "=? AND " + DbStructure.Column.DOWNLOADED + "=? AND "+DbStructure.Column.FILEPATH+"=? AND "
                        + DbStructure.Column.USERNAME + "=?",


                new String[]{courseId, String.valueOf(DownloadedState.DOWNLOADED.ordinal()),"Scrom",
                        username()}, null);

        op.setCallback(callback);
        return enqueue(op);
    }



    //Added by Arjun to get downloaded Pdf conut in device
    @Override
    public Integer getDownloadedPdfCountByCourse(String courseId,
                                                 final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(true, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},


                DbStructure.Column.EID + "=? AND " + DbStructure.Column.DOWNLOADED + "=? AND "+DbStructure.Column.FILEPATH+"=? AND "
                        + DbStructure.Column.USERNAME + "=?",


                new String[]{courseId, String.valueOf(DownloadedState.DOWNLOADED.ordinal()),"Pdf",
                        username()}, null);

        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer getVideosCountBySection(String enrollmentId, String chapter,
                                           String section, final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.SECTION + "=? AND " + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.DOWNLOADED + "!=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{section, chapter, enrollmentId,
                        String.valueOf(DownloadedState.ONLINE.ordinal()), username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer getWebOnlyVideosCountBySection(String enrollmentId, String chapter,
                                                  String section,
                                                  final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.SECTION + "=? AND " + DbStructure.Column.CHAPTER + "=? AND "
                        + DbStructure.Column.EID + "=? AND "
                        + DbStructure.Column.VIDEO_FOR_WEB_ONLY + "==1 AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{section, chapter, enrollmentId, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer updateVideoWatchedState(String videoId, WatchedState status,
                                           final DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.WATCHED, status.ordinal());

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoId, username()});
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Integer updateVideoLastPlayedOffset(String videoId, int offset,
                                               final DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.LAST_PLAYED_OFFSET, offset);

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoId, username()});
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Long addVideoData(final VideoModel de, final DataCallback<Long> callback) {
        VideoModel result = getVideoEntryByVideoId(de.getVideoId(), null);
        if (result == null) {
            ContentValues values = new ContentValues();
            values.put(DbStructure.Column.USERNAME, username());
            values.put(DbStructure.Column.TITLE, de.getTitle());
            values.put(DbStructure.Column.VIDEO_ID, de.getVideoId());
            values.put(DbStructure.Column.SIZE, de.getSize());
            values.put(DbStructure.Column.DURATION, de.getDuration());
            values.put(DbStructure.Column.FILEPATH, de.getFilePath());
            values.put(DbStructure.Column.URL, de.getVideoUrl());
            values.put(DbStructure.Column.URL_HLS, de.getHLSVideoUrl());
            values.put(DbStructure.Column.URL_HIGH_QUALITY, de.getHighQualityVideoUrl());
            values.put(DbStructure.Column.URL_LOW_QUALITY, de.getLowQualityVideoUrl());
            values.put(DbStructure.Column.URL_YOUTUBE, de.getYoutubeVideoUrl());
            values.put(DbStructure.Column.WATCHED, de.getWatchedStateOrdinal());
            values.put(DbStructure.Column.DOWNLOADED, de.getDownloadedStateOrdinal());
            values.put(DbStructure.Column.DM_ID, de.getDmId());
            values.put(DbStructure.Column.EID, de.getEnrollmentId());
            values.put(DbStructure.Column.CHAPTER, de.getChapterName());
            values.put(DbStructure.Column.SECTION, de.getSectionName());
            values.put(DbStructure.Column.LAST_PLAYED_OFFSET, de.getLastPlayedOffset());
            values.put(DbStructure.Column.UNIT_URL, de.getLmsUrl());
            values.put(DbStructure.Column.IS_COURSE_ACTIVE, de.isCourseActive());
            values.put(DbStructure.Column.VIDEO_FOR_WEB_ONLY, de.isVideoForWebOnly());
            values.put(DbStructure.Column.TYPE, de.getDownloadType());
            values.put(DbStructure.Column.CONTENT_ID, de.getContent_id());

            DbOperationInsert op = new DbOperationInsert(DbStructure.Table.DOWNLOADS, values);
            op.setCallback(callback);
            return enqueue(op);
        } else {
            if (callback != null) {
                callback.sendResult(0L);
            }
            logger.warn("Not inserting, this seems a duplicate record");
        }

        return 0L;
    }


    /**
     * Returns download entry for given video id.
     *
     * @param videoId
     * @return
     */
    public VideoModel getVideoEntryByVideoId(String videoId,
                                             final DataCallback<VideoModel> callback) {
        DbOperationGetVideo op = new DbOperationGetVideo(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoId, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public VideoModel getVideoByVideoUrl(String videoUrl,
                                         DataCallback<VideoModel> callback) {
        DbOperationGetVideo op = new DbOperationGetVideo(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.URL + "=? AND " + DbStructure.Column.DOWNLOADED + "!=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{videoUrl, String.valueOf(DownloadedState.ONLINE.ordinal()),
                        username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer deleteVideoByVideoId(VideoModel video, DataCallback<Integer> callback) {
        DbOperationDelete op = new DbOperationDelete(DbStructure.Table.DOWNLOADS,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{video.getVideoId(), username()});
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer deleteVideoByVideoId(VideoModel video, String username,
                                        DataCallback<Integer> callback) {
        DbOperationDelete op = new DbOperationDelete(DbStructure.Table.DOWNLOADS,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{video.getVideoId(), username});
        op.setCallback(callback);
        return enqueue(op);
    }

    //Arjun:To remove scrom entry from db
    @Override
    public Integer deleteScromEntryByScromId(String scormBlockId, DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.DOWNLOADED, DownloadedState.ONLINE.ordinal());
        values.put(DbStructure.Column.DM_ID, -1);
        values.put(DbStructure.Column.FILEPATH, "");
        values.put(DbStructure.Column.VIDEO_ID, "");

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{scormBlockId, username()});
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Boolean isVideoFilePresentByUrl(String videoUrl, final DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.URL + "=? AND " + DbStructure.Column.DOWNLOADED + "=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{videoUrl, String.valueOf(DownloadedState.DOWNLOADED.ordinal()),
                        username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer updateDownloadingVideoInfoByVideoId(VideoModel model,
                                                       DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.DM_ID, model.getDmId());
        values.put(DbStructure.Column.DOWNLOADED, model.getDownloadedStateOrdinal());
        values.put(DbStructure.Column.DURATION, model.getDuration());
        values.put(DbStructure.Column.FILEPATH, model.getFilePath());
        values.put(DbStructure.Column.SIZE, model.getSize());
        values.put(DbStructure.Column.IS_COURSE_ACTIVE, model.isCourseActive());

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{model.getVideoId(), username()});
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Integer updateAsDownloadingByVideoId(VideoModel model,
                                                DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.DM_ID, model.getDmId());
        values.put(DbStructure.Column.DOWNLOADED, DownloadedState.DOWNLOADING.ordinal());

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{model.getVideoId(), username()});
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public List<VideoModel> getListOfOngoingDownloads(
            final DataCallback<List<VideoModel>> callback) {
        DbOperationGetVideos op = new DbOperationGetVideos(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.DOWNLOADED + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADING.ordinal()), username()},
                null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public List<VideoModel> getListOfOngoingDownloadsByCourseId(@Nullable String courseId,
                                                                DataCallback<List<VideoModel>> callback) {
        final StringBuilder whereClause = new StringBuilder();
        final List<String> whereArgs = new ArrayList<>();
        whereClause.append(DbStructure.Column.DOWNLOADED).append("=? AND ");
        whereClause.append(DbStructure.Column.USERNAME).append("=?");
        whereArgs.add(String.valueOf(DownloadedState.DOWNLOADING.ordinal()));
        whereArgs.add(username());
        if (courseId != null) {
            whereClause.append(" AND ").append(DbStructure.Column.EID).append("=?");
            whereArgs.add(courseId);
        }
        final DbOperationGetVideos op = new DbOperationGetVideos(false, DbStructure.Table.DOWNLOADS, null,
                whereClause.toString(), whereArgs.toArray(new String[whereArgs.size()]), null);
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public Integer getVideosDownloadedCount(final DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                null, DbStructure.Column.DOWNLOADED + "=? AND "
                + DbStructure.Column.USERNAME + "=?",
                new String[]{String.valueOf(DownloadedState.DOWNLOADED.ordinal()),
                        username()}, null);
        op.setCallback(callback);
        return enqueue(op);

    }

    @Override
    public VideoModel getIVideoModelByVideoUrl(String videoUrl,
                                               final DataCallback<VideoModel> callback) {
        DbOperationGetVideo op = new DbOperationGetVideo(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.URL + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoUrl, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Boolean isDmIdExists(long dmId, final DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.USERNAME + "=? AND " + DbStructure.Column.DM_ID + "=?",
                new String[]{username(), String.valueOf(dmId)}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer updateDownloadCompleteInfoByDmId(long dmId,
                                                    VideoModel model,
                                                    DataCallback<Integer> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.SIZE, model.getSize());
        values.put(DbStructure.Column.DURATION, model.getDuration());
        values.put(DbStructure.Column.FILEPATH, model.getFilePath());
        values.put(DbStructure.Column.URL, model.getVideoUrl());
        values.put(DbStructure.Column.URL_HLS, model.getHLSVideoUrl());
        values.put(DbStructure.Column.URL_HIGH_QUALITY, model.getHighQualityVideoUrl());
        values.put(DbStructure.Column.URL_LOW_QUALITY, model.getLowQualityVideoUrl());
        values.put(DbStructure.Column.URL_YOUTUBE, model.getYoutubeVideoUrl());
        values.put(DbStructure.Column.DOWNLOADED, model.getDownloadedStateOrdinal());
        values.put(DbStructure.Column.DOWNLOADED_ON, model.getDownloadedOn());

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.DOWNLOADS, values,
                DbStructure.Column.DM_ID + "=? AND " + DbStructure.Column.DOWNLOADED + "!=?",
                new String[]{String.valueOf(dmId),
                        String.valueOf(DownloadedState.ONLINE.ordinal())});
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public List<VideoModel> getAllVideos(String username,
                                         final DataCallback<List<VideoModel>> callback) {
        DbOperationGetVideos op = new DbOperationGetVideos(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.USERNAME + "=?", new String[]{username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public List<VideoModel> getAllVideosByCourse(@NonNull String courseId,
                                                 @Nullable DataCallback<List<VideoModel>> callback) {
        DbOperationGetVideos op = new DbOperationGetVideos(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.EID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{courseId, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public List<VideoModel> getVideosByVideoIds(@NonNull List<CourseComponent> videoComponents,
                                                @Nullable DownloadedState downloadedState,
                                                @Nullable DataCallback<List<VideoModel>> callback) {
        final List<String> whereArgs = new ArrayList<>();
        whereArgs.add(username());
        for (CourseComponent component : videoComponents) {
            whereArgs.add(component.getId());
        }
        if (downloadedState != null) {
            whereArgs.add(String.valueOf(downloadedState.ordinal()));
        }

        final CharSequence placeholders = TextUtils.join(",",
                Collections.<CharSequence>nCopies(videoComponents.size(), "?"));

        DbOperationGetVideos op = new DbOperationGetVideos(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.USERNAME + "=? AND " +
                        DbStructure.Column.VIDEO_ID + " IN (" + placeholders + ")" +
                        (downloadedState == null ? "" : " AND " + DbStructure.Column.DOWNLOADED + "=?"),
                whereArgs.toArray(new String[0]), null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public void clearDataByUser(String username) {
        DbOperationDelete op = new DbOperationDelete(DbStructure.Table.DOWNLOADS,
                DbStructure.Column.USERNAME + "=?",
                new String[]{username()});
        enqueue(op);
    }

    @Override
    public void release() {
        super.release();
    }


    @Override
    public WatchedState getWatchedStateForVideoId(String videoId,
                                                  final DataCallback<WatchedState> dataCallback) {
        DbOperationGetColumn<Integer> op = new DbOperationGetColumn<Integer>(false,
                DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.WATCHED},
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoId, username()}, null, Integer.class);
        op.setCallback(new DataCallback<List<Integer>>() {
            @Override
            public void onResult(List<Integer> ordinals) {
                if (ordinals != null && !ordinals.isEmpty()) {
                    dataCallback.sendResult(WatchedState.values()[ordinals.get(0)]);
                } else {
                    dataCallback.sendResult(WatchedState.UNWATCHED);
                }
            }

            @Override
            public void onFail(Exception ex) {
                dataCallback.sendException(ex);
            }
        });
        List<Integer> ordinals = enqueue(op);
        if (ordinals != null && !ordinals.isEmpty()) {
            return WatchedState.values()[ordinals.get(0)];
        }
        return WatchedState.UNWATCHED;
    }

    @Override
    public Integer getVideoCountByVideoUrl(String videoUrl, DataCallback<Integer> callback) {
        DbOperationGetCount op = new DbOperationGetCount(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.URL + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoUrl, username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public VideoModel getDownloadEntryByDmId(long dmId,
                                             DataCallback<VideoModel> callback) {
        DbOperationGetVideo op = new DbOperationGetVideo(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.DM_ID + "=? AND " + DbStructure.Column.DOWNLOADED + "=?",
                new String[]{String.valueOf(dmId), String.valueOf(DownloadedState
                        .DOWNLOADING.ordinal())}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    /*@Override
    public Boolean isVideoDownloadingByVideoId(String videoId,
                                               DataCallback<Boolean> callback) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS,
                new String[]{DbStructure.Column.VIDEO_ID},
                DbStructure.Column.VIDEO_ID + "=? "
                        + DbStructure.Column.DOWNLOADED + "=? AND "
                        + DbStructure.Column.USERNAME + "=?",
                new String[]{videoId, String.valueOf(DownloadedState.DOWNLOADING.ordinal())
                        , username()}, null);
        op.setCallback(callback);
        return enqueue(op);
    }*/

    @Override
    public DownloadedState getDownloadedStateForVideoId(String videoId,
                                                        final DataCallback<DownloadedState> dataCallback) {
        DbOperationGetColumn<Integer> op = new DbOperationGetColumn<Integer>(false,
                DbStructure.Table.DOWNLOADS, new String[]{DbStructure.Column.DOWNLOADED},
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=?",
                new String[]{videoId, username()}, null, Integer.class);
        op.setCallback(new DataCallback<List<Integer>>() {
            @Override
            public void onResult(List<Integer> ordinals) {
                if (ordinals != null && !ordinals.isEmpty()) {
                    dataCallback.sendResult(DownloadedState.values()[ordinals.get(0)]);
                } else {
                    dataCallback.sendResult(DownloadedState.ONLINE);
                }
            }

            @Override
            public void onFail(Exception ex) {
                dataCallback.sendException(ex);
            }
        });
        List<Integer> ordinals = enqueue(op);
        if (ordinals != null && !ordinals.isEmpty()) {
            return DownloadedState.values()[ordinals.get(0)];
        }
        return DownloadedState.ONLINE;
    }

    @Override
    public Boolean isAnyVideoDownloadingInCourse(final DataCallback<Boolean> callback,
                                                 String courseId) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.EID + "=? AND " + DbStructure.Column.USERNAME + "=? AND "
                        + DbStructure.Column.DOWNLOADED + "=?",
                new String[]{courseId, username(),
                        String.valueOf(DownloadedState.DOWNLOADING.ordinal())}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Boolean isAnyVideoDownloadingInSection(final DataCallback<Boolean> callback,
                                                  String courseId, String section) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.EID + "=? AND " + DbStructure.Column.CHAPTER + "=? AND " +
                        DbStructure.Column.USERNAME + "=? AND " +
                        DbStructure.Column.DOWNLOADED + "=?",
                new String[]{courseId, section, username(),
                        String.valueOf(DownloadedState.DOWNLOADING.ordinal())}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Boolean isAnyVideoDownloadingInSubSection(final DataCallback<Boolean> callback,
                                                     String courseId, String section,
                                                     String subSection) {
        DbOperationExists op = new DbOperationExists(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.EID + "=? AND " + DbStructure.Column.CHAPTER + "=? AND " +
                        DbStructure.Column.SECTION + "=? AND " +
                        DbStructure.Column.USERNAME + "=? AND " +
                        DbStructure.Column.DOWNLOADED + "=?",
                new String[]{courseId, section, subSection, username(),
                        String.valueOf(DownloadedState.DOWNLOADING.ordinal())}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    /**
     * update assessment unit access record
     */
    public synchronized Integer updateAccess(DataCallback<Integer> callback, String unitId,
                                             boolean visited) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.ASSESSMENT_TB_UNIT_WATCHED, visited);

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.ASSESSMENT, values,
                DbStructure.Column.ASSESSMENT_TB_UNIT_ID + "=? AND " +
                        DbStructure.Column.ASSESSMENT_TB_USERNAME + "=?",
                new String[]{unitId, username()});
        op.setCallback(callback);
        return enqueue(op);
    }

    /**
     * get assessment unit access status
     */
    public synchronized boolean isUnitAccessed(final DataCallback<Boolean> callback,
                                               String unitId) {
        DbOperationGetColumn<Boolean> op = new DbOperationGetColumn<Boolean>(false,
                DbStructure.Table.ASSESSMENT,
                new String[]{DbStructure.Column.ASSESSMENT_TB_UNIT_WATCHED},
                DbStructure.Column.ASSESSMENT_TB_UNIT_ID + "=? AND "
                        + DbStructure.Column.ASSESSMENT_TB_USERNAME + "=?",
                new String[]{unitId, username()}, null, Boolean.class);
        if (callback != null) {
            op.setCallback(new DataCallback<List<Boolean>>() {
                @Override
                public void onResult(List<Boolean> ordinals) {
                    if (ordinals != null && !ordinals.isEmpty()) {
                        callback.sendResult(ordinals.get(0));
                    } else {
                        //if no record, it also means not accessed before.
                        callback.sendResult(false);
                    }
                }

                @Override
                public void onFail(Exception ex) {
                    callback.sendException(ex);
                }
            });
        }
        List<Boolean> result = enqueue(op);
        return result != null && result.size() > 0 ? result.get(0) : false;
    }

    @Override
    public VideoModel getPostVideo(String postId) {
        DbOperationGetVideo op = new DbOperationGetVideo(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.VIDEO_ID + "=? AND " + DbStructure.Column.USERNAME + "=? ",
                new String[]{postId, username()}, null);
        return enqueue(op);
    }

    @Override
    public VideoModel getPostVideo(String p_id , String video_url , final DataCallback<VideoModel> callback) {
        video_url=video_url.replace("'","''");
        DbOperationGetVideo op = new DbOperationGetVideo(false, DbStructure.Table.DOWNLOADS, null,
                DbStructure.Column.VIDEO_ID + "=? AND "
                        + DbStructure.Column.USERNAME + "=? AND " +
                        DbStructure.Column.URL + "=? AND "
                        +DbStructure.Column.DOWNLOADED + "=? AND "
                        + DbStructure.Column.FILEPATH +" IS NOT NULL AND "+DbStructure.Column.FILEPATH +" !='' ",
                new String[]{p_id, username(),video_url,String.valueOf(DownloadedState.DOWNLOADED.ordinal())}, null);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Long addAnalyticData(AnalyticModel de, DataCallback<Long> callback) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.USER_ID, de.getUser_Id());
        values.put(DbStructure.Column.ACTION, de.getAction());
        values.put(DbStructure.Column.METADATA, de.getMetadata());
        values.put(DbStructure.Column.PAGE, de.getPage());
        values.put(DbStructure.Column.STATUS, de.getStatus());
        values.put(DbStructure.Column.EVENT_DATE, de.getEvent_timestamp());
        values.put(DbStructure.Column.NAV, de.getNav());
        values.put(DbStructure.Column.ACTION_ID, de.getAction_id());

        DbOperationInsert op = new DbOperationInsert(DbStructure.Table.ANALYTIC, values);
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Integer deleteAnalyticByAnalyticId(String[] ids,String INQueryParams, DataCallback<Integer> callback) {


        DbOperationDelete op=new DbOperationDelete(DbStructure.Table.ANALYTIC,DbStructure.Column.ANALYTIC_TB_ID+ " IN ("+INQueryParams+")",ids);
        op.setCallback(callback);
        return enqueue(op);
    }


    @Override
    public ArrayList<AnalyticModel> getAnalytics(int batch_count, int status,
                                                 final DataCallback<ArrayList<AnalyticModel>> callback) {
        DbOperationGetAnalytic op = new DbOperationGetAnalytic(false, DbStructure.Table.ANALYTIC, null,
                DbStructure.Column.STATUS + "=? AND " + DbStructure.Column.USER_ID +"=? AND "+ DbStructure.Column.ACTION + "!=?",
                new String[]{""+status, loginPrefs.getUsername(),String.valueOf(Action.TinCanObject)},null, String.valueOf(batch_count));
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public ArrayList<AnalyticModel> getTincanAnalytics(int batch_count, int status, DataCallback<ArrayList<AnalyticModel>> callback) {
        DbOperationGetAnalytic op = new DbOperationGetAnalytic(false, DbStructure.Table.ANALYTIC, null,
                DbStructure.Column.STATUS + "=? AND " + DbStructure.Column.USER_ID + "=? AND "+ DbStructure.Column.ACTION + "=?",
                new String[]{""+status, loginPrefs.getUsername(), String.valueOf(Action.TinCanObject)},null, String.valueOf(batch_count));
        op.setCallback(callback);
        return enqueue(op);
    }

    @Override
    public Long addResumePayload(Resume resume) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.USER_ID, resume.getUser_Id());
        values.put(DbStructure.Column.COURSE_ID, resume.getCourse_Id());
        values.put(DbStructure.Column.UNIT_ID, resume.getUnit_id());
        values.put(DbStructure.Column.RESUME_PAYLOAD, resume.getResume_Payload());

        DbOperationInsert op = new DbOperationInsert(DbStructure.Table.TINCAN, values);
        return enqueue(op);
    }

    @Override
    public Integer updateResumePayload(Resume resume) {
        ContentValues values = new ContentValues();
        values.put(DbStructure.Column.USER_ID, resume.getUser_Id());
        values.put(DbStructure.Column.COURSE_ID, resume.getCourse_Id());
        values.put(DbStructure.Column.UNIT_ID, resume.getUnit_id());
        values.put(DbStructure.Column.RESUME_PAYLOAD, resume.getResume_Payload());

        DbOperationUpdate op = new DbOperationUpdate(DbStructure.Table.TINCAN, values,
                DbStructure.Column.USER_ID + "=? AND "+
                        DbStructure.Column.COURSE_ID + "=? AND "+
                        DbStructure.Column.UNIT_ID + "=?",
                new String[]{loginPrefs.getUsername(),resume.getCourse_Id(),resume.getUnit_id()});
        return enqueue(op);
    }

    @Override
    public Integer deleteResumePayload(String course_id, String unit_id) {

        DbOperationDelete op=new DbOperationDelete(DbStructure.Table.TINCAN,
                DbStructure.Column.COURSE_ID+ "=? AND "+
                        DbStructure.Column.USER_ID+" =? AND "+
                        DbStructure.Column.UNIT_ID+" =? ",
                new String[]{course_id,loginPrefs.getUsername(),unit_id});
        return enqueue(op);
    }

    @Override
    public Resume getResumeInfo(String course_id, String unit_id) {
        DbOperationGetTinCanPayload op = new DbOperationGetTinCanPayload(false,
                DbStructure.Table.TINCAN, null,
                DbStructure.Column.USER_ID + "=? AND " +
                        DbStructure.Column.COURSE_ID +"=? AND "+
                        DbStructure.Column.UNIT_ID + " =? ",
                new String[]{loginPrefs.getUsername(),course_id,unit_id},null);
        return enqueue(op);
    }
}