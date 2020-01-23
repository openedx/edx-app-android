package org.humana.mobile.tta.data;

import android.app.Activity;
import android.app.DownloadManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.humana.mobile.R;
import org.humana.mobile.authentication.AuthResponse;
import org.humana.mobile.core.IEdxDataManager;
import org.humana.mobile.core.IEdxEnvironment;
import org.humana.mobile.course.CourseAPI;
import org.humana.mobile.course.CourseService;
import org.humana.mobile.discussion.CourseTopics;
import org.humana.mobile.discussion.DiscussionComment;
import org.humana.mobile.discussion.DiscussionThread;
import org.humana.mobile.discussion.DiscussionTopic;
import org.humana.mobile.discussion.DiscussionTopicDepth;
import org.humana.mobile.http.callback.Callback;
import org.humana.mobile.model.Page;
import org.humana.mobile.model.api.EnrolledCoursesResponse;
import org.humana.mobile.model.api.ProfileModel;
import org.humana.mobile.model.course.CourseComponent;
import org.humana.mobile.model.course.CourseStructureV1Model;
import org.humana.mobile.model.course.HasDownloadEntry;
import org.humana.mobile.model.db.DownloadEntry;
import org.humana.mobile.module.db.DataCallback;
import org.humana.mobile.module.db.impl.DbHelper;
import org.humana.mobile.module.prefs.LoginPrefs;
import org.humana.mobile.module.registration.model.RegistrationOption;
import org.humana.mobile.services.CourseManager;
import org.humana.mobile.services.VideoDownloadHelper;
import org.humana.mobile.task.Task;
import org.humana.mobile.tta.Constants;
import org.humana.mobile.tta.analytics.Analytic;
import org.humana.mobile.tta.data.enums.CertificateStatus;
import org.humana.mobile.tta.data.enums.ScormStatus;
import org.humana.mobile.tta.data.enums.SourceName;
import org.humana.mobile.tta.data.enums.SourceType;
import org.humana.mobile.tta.data.enums.SurveyType;
import org.humana.mobile.tta.data.local.db.ILocalDataSource;
import org.humana.mobile.tta.data.local.db.LocalDataSource;
import org.humana.mobile.tta.data.local.db.TADatabase;
import org.humana.mobile.tta.data.local.db.operation.GetCourseContentsOperation;
import org.humana.mobile.tta.data.local.db.operation.GetWPContentsOperation;
import org.humana.mobile.tta.data.local.db.table.CalendarEvents;
import org.humana.mobile.tta.data.local.db.table.Category;
import org.humana.mobile.tta.data.local.db.table.Certificate;
import org.humana.mobile.tta.data.local.db.table.Content;
import org.humana.mobile.tta.data.local.db.table.ContentList;
import org.humana.mobile.tta.data.local.db.table.ContentStatus;
import org.humana.mobile.tta.data.local.db.table.Feed;
import org.humana.mobile.tta.data.local.db.table.Notification;
import org.humana.mobile.tta.data.local.db.table.Period;
import org.humana.mobile.tta.data.local.db.table.Program;
import org.humana.mobile.tta.data.local.db.table.Section;
import org.humana.mobile.tta.data.local.db.table.Source;
import org.humana.mobile.tta.data.local.db.table.Unit;
import org.humana.mobile.tta.data.local.db.table.UnitStatus;
import org.humana.mobile.tta.data.model.BaseResponse;
import org.humana.mobile.tta.data.model.CountResponse;
import org.humana.mobile.tta.data.model.EmptyResponse;
import org.humana.mobile.tta.data.model.HtmlResponse;
import org.humana.mobile.tta.data.model.StatusResponse;
import org.humana.mobile.tta.data.model.SuccessResponse;
import org.humana.mobile.tta.data.model.UpdateResponse;
import org.humana.mobile.tta.data.model.agenda.AgendaItem;
import org.humana.mobile.tta.data.model.agenda.AgendaList;
import org.humana.mobile.tta.data.model.authentication.FieldInfo;
import org.humana.mobile.tta.data.model.content.BookmarkResponse;
import org.humana.mobile.tta.data.model.content.CertificateStatusResponse;
import org.humana.mobile.tta.data.model.content.MyCertificatesResponse;
import org.humana.mobile.tta.data.model.content.TotalLikeResponse;
import org.humana.mobile.tta.data.model.feed.SuggestedUser;
import org.humana.mobile.tta.data.model.library.CollectionConfigResponse;
import org.humana.mobile.tta.data.model.library.CollectionItemsResponse;
import org.humana.mobile.tta.data.model.library.ConfigModifiedDateResponse;
import org.humana.mobile.tta.data.model.profile.ChangePasswordResponse;
import org.humana.mobile.tta.data.model.profile.FeedbackResponse;
import org.humana.mobile.tta.data.model.profile.FollowStatus;
import org.humana.mobile.tta.data.model.profile.UpdateMyProfileResponse;
import org.humana.mobile.tta.data.model.profile.UserAddressResponse;
import org.humana.mobile.tta.data.model.program.ProgramFilter;
import org.humana.mobile.tta.data.model.program.ProgramUser;
import org.humana.mobile.tta.data.model.program.SelectedFilter;
import org.humana.mobile.tta.data.model.search.FilterSection;
import org.humana.mobile.tta.data.model.search.SearchFilter;
import org.humana.mobile.tta.data.pref.AppPref;
import org.humana.mobile.tta.data.remote.IRemoteDataSource;
import org.humana.mobile.tta.data.remote.RetrofitServiceUtil;
import org.humana.mobile.tta.data.remote.api.MxCookiesAPI;
import org.humana.mobile.tta.data.remote.api.MxSurveyAPI;
import org.humana.mobile.tta.exception.NoConnectionException;
import org.humana.mobile.tta.exception.TaException;
import org.humana.mobile.tta.interfaces.OnResponseCallback;
import org.humana.mobile.tta.scorm.ScormBlockModel;
import org.humana.mobile.tta.scorm.ScormStartResponse;
import org.humana.mobile.tta.task.GetEnrolledCourseTask;
import org.humana.mobile.tta.task.GetEventCalenderTask;
import org.humana.mobile.tta.task.GetVersionUpdatedTask;
import org.humana.mobile.tta.task.agenda.GetMyAgendaContentTask;
import org.humana.mobile.tta.task.agenda.GetMyAgendaCountTask;
import org.humana.mobile.tta.task.agenda.GetStateAgendaContentTask;
import org.humana.mobile.tta.task.agenda.GetStateAgendaCountTask;
import org.humana.mobile.tta.task.authentication.GetGenericUserFieldInfoTask;
import org.humana.mobile.tta.task.authentication.LoginTask;
import org.humana.mobile.tta.task.content.GetContentFromSourceIdentityTask;
import org.humana.mobile.tta.task.content.GetContentTask;
import org.humana.mobile.tta.task.content.GetMyContentStatusTask;
import org.humana.mobile.tta.task.content.GetUserContentStatusTask;
import org.humana.mobile.tta.task.content.IsContentMyAgendaTask;
import org.humana.mobile.tta.task.content.IsLikeTask;
import org.humana.mobile.tta.task.content.SetBookmarkTask;
import org.humana.mobile.tta.task.content.SetLikeTask;
import org.humana.mobile.tta.task.content.SetLikeUsingSourceIdentityTask;
import org.humana.mobile.tta.task.content.SetUserContentTask;
import org.humana.mobile.tta.task.content.TotalLikeTask;
import org.humana.mobile.tta.task.content.course.GetCourseDataFromPersistableCacheTask;
import org.humana.mobile.tta.task.content.course.UserEnrollmentCourseFromCacheTask;
import org.humana.mobile.tta.task.content.course.UserEnrollmentCourseTask;
import org.humana.mobile.tta.task.content.course.certificate.GenerateCertificateTask;
import org.humana.mobile.tta.task.content.course.certificate.GetCertificateStatusTask;
import org.humana.mobile.tta.task.content.course.certificate.GetCertificateTask;
import org.humana.mobile.tta.task.content.course.certificate.GetMyCertificatesTask;
import org.humana.mobile.tta.task.content.course.discussion.CreateDiscussionCommentTask;
import org.humana.mobile.tta.task.content.course.discussion.CreateDiscussionThreadTask;
import org.humana.mobile.tta.task.content.course.discussion.GetCommentRepliesTask;
import org.humana.mobile.tta.task.content.course.discussion.GetDiscussionThreadsTask;
import org.humana.mobile.tta.task.content.course.discussion.GetDiscussionTopicsTask;
import org.humana.mobile.tta.task.content.course.discussion.GetThreadCommentsTask;
import org.humana.mobile.tta.task.content.course.discussion.LikeDiscussionCommentTask;
import org.humana.mobile.tta.task.content.course.discussion.LikeDiscussionThreadTask;
import org.humana.mobile.tta.task.content.course.scorm.GetUnitStatusTask;
import org.humana.mobile.tta.task.content.course.scorm.StartScormTask;
import org.humana.mobile.tta.task.feed.FollowUserTask;
import org.humana.mobile.tta.task.feed.GetFeedsTask;
import org.humana.mobile.tta.task.feed.GetSuggestedUsersTask;
import org.humana.mobile.tta.task.library.GetCollectionConfigTask;
import org.humana.mobile.tta.task.library.GetCollectionItemsTask;
import org.humana.mobile.tta.task.library.GetConfigModifiedDateTask;
import org.humana.mobile.tta.task.notification.CreateNotificationsTask;
import org.humana.mobile.tta.task.notification.GetNotificationsTask;
import org.humana.mobile.tta.task.notification.SendNotificationTask;
import org.humana.mobile.tta.task.notification.UpdateNotificationsTask;
import org.humana.mobile.tta.task.profile.ChangePasswordTask;
import org.humana.mobile.tta.task.profile.GetAccountTask;
import org.humana.mobile.tta.task.profile.GetFollowStatusTask;
import org.humana.mobile.tta.task.profile.GetProfileTask;
import org.humana.mobile.tta.task.profile.GetUserAddressTask;
import org.humana.mobile.tta.task.profile.SubmitFeedbackTask;
import org.humana.mobile.tta.task.profile.UpdateMyProfileTask;
import org.humana.mobile.tta.task.program.ApproveUnitTask;
import org.humana.mobile.tta.task.program.CreatePeriodTask;
import org.humana.mobile.tta.task.program.GetAllUnitsTask;
import org.humana.mobile.tta.task.program.GetBlockComponentFromCacheTask;
import org.humana.mobile.tta.task.program.GetBlockComponentFromServerTask;
import org.humana.mobile.tta.task.program.GetCourseComponentTask;
import org.humana.mobile.tta.task.program.GetPendingUnitsTask;
import org.humana.mobile.tta.task.program.GetPendingUsersTask;
import org.humana.mobile.tta.task.program.GetPeriodsTask;
import org.humana.mobile.tta.task.program.GetProgramFiltersTask;
import org.humana.mobile.tta.task.program.GetProgramsTask;
import org.humana.mobile.tta.task.program.GetSectionsTask;
import org.humana.mobile.tta.task.program.GetUnitsTask;
import org.humana.mobile.tta.task.program.GetUserStatusTask;
import org.humana.mobile.tta.task.program.GetUsersTask;
import org.humana.mobile.tta.task.program.RejectUnitTask;
import org.humana.mobile.tta.task.program.SavePeriodTask;
import org.humana.mobile.tta.task.program.SetProposedDateTask;
import org.humana.mobile.tta.task.program.SetSpecificSessionTask;
import org.humana.mobile.tta.task.program.UpdatePeriodTask;
import org.humana.mobile.tta.task.search.GetSearchFilterTask;
import org.humana.mobile.tta.task.search.SearchTask;
import org.humana.mobile.tta.utils.RxUtil;
import org.humana.mobile.tta.wordpress_client.model.Comment;
import org.humana.mobile.tta.wordpress_client.model.CustomComment;
import org.humana.mobile.tta.wordpress_client.model.Post;
import org.humana.mobile.tta.wordpress_client.model.User;
import org.humana.mobile.tta.wordpress_client.model.WPProfileModel;
import org.humana.mobile.tta.wordpress_client.rest.HttpServerErrorResponse;
import org.humana.mobile.tta.wordpress_client.rest.WordPressRestResponse;
import org.humana.mobile.tta.wordpress_client.rest.WpClientRetrofit;
import org.humana.mobile.user.Account;
import org.humana.mobile.user.ProfileImage;
import org.humana.mobile.user.SetAccountImageTask;
import org.humana.mobile.util.Config;
import org.humana.mobile.util.DateUtil;
import org.humana.mobile.util.NetworkUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;

import static android.content.Context.DOWNLOAD_SERVICE;
import static org.humana.mobile.tta.Constants.TA_DATABASE;

/**
 * Created by Arjun on 2018/9/18.
 */

public class DataManager extends BaseRoboInjector {
    private static DataManager mDataManager;
    @Inject
    public IEdxDataManager edxDataManager;
    private Context context;
    private IRemoteDataSource mRemoteDataSource;
    private ILocalDataSource mLocalDataSource;
    @com.google.inject.Inject
    private IEdxEnvironment edxEnvironment;

    @com.google.inject.Inject
    private Config config;

    @com.google.inject.Inject
    private CourseManager courseManager;

    @com.google.inject.Inject
    private CourseAPI courseApi;

    @com.google.inject.Inject
    private VideoDownloadHelper downloadManager;

    private WpClientRetrofit wpClientRetrofit;

    private AppPref mAppPref;
    private LoginPrefs loginPrefs;

    private DbHelper dbHelper;

    private DataManager(Context context, IRemoteDataSource remoteDataSource, ILocalDataSource localDataSource) {
        super(context);
        this.context = context;
        mRemoteDataSource = remoteDataSource;
        mLocalDataSource = localDataSource;

        mAppPref = new AppPref(context);
        loginPrefs = new LoginPrefs(context);

        dbHelper = new DbHelper(context);
    }

    public static DataManager getInstance(Context context) {
        if (mDataManager == null) {
            synchronized (DataManager.class) {
                if (mDataManager == null) {
                    mDataManager = new DataManager(context, RetrofitServiceUtil.create(context, true),
                            new LocalDataSource(Room.databaseBuilder(context, TADatabase.class, TA_DATABASE)
                                    .addMigrations(TADatabase.MIGRATION_5_6, TADatabase.MIGRATION_6_7)
                                    .fallbackToDestructiveMigration()
                                    .build()));
                }
            }
        }
        mDataManager.wpClientRetrofit = new WpClientRetrofit(true, false);
        return mDataManager;
    }

    public void refreshLocalDatabase() {
        mLocalDataSource = new LocalDataSource(
                Room.databaseBuilder(context, TADatabase.class, TA_DATABASE)
                        .addMigrations(TADatabase.MIGRATION_5_6, TADatabase.MIGRATION_6_7)
                        .fallbackToDestructiveMigration()
                        .build());
    }

    public IEdxEnvironment getEdxEnvironment() {
        return edxEnvironment;
    }

    public Config getConfig() {
        return config;
    }

    private <T> Observable<T> preProcess(Observable<BaseResponse<T>> observable) {
        return observable.compose(RxUtil.applyScheduler())
                .map(RxUtil.unwrapResponse(null));
    }

    private <T> Observable<T> preProcess(Observable<BaseResponse<T>> observable, Class<T> cls) {
        return observable.compose(RxUtil.applyScheduler())
                .map(RxUtil.unwrapResponse(cls));
    }

    private Observable<EmptyResponse> preEmptyProcess(Observable<BaseResponse<EmptyResponse>> observable) {
        return preProcess(observable, EmptyResponse.class);
    }

    public AppPref getAppPref() {
        return mAppPref;
    }

    public LoginPrefs getLoginPrefs() {
        return loginPrefs;
    }

    public void login(String username, String password, OnResponseCallback<AuthResponse> callback) {

        /*wpClientRetrofit.getAccessToken(username, password, new WordPressRestResponse<WpAuthResponse>() {
            @Override
            public void onSuccess(WpAuthResponse result) {
                loginPrefs.storeWPAuthTokenResponse(result);
                doEdxLogin(username, password, callback);
            }

            @Override
            public void onFailure(HttpServerErrorResponse errorResponse) {
                if (config.isWordpressAuthentication()) {
                    callback.onFailure(new TaException(errorResponse.getMessage()));
                } else {
                    doEdxLogin(username, password, callback);
                }
            }
        });*/

        doEdxLogin(username, password, callback);

    }

    private void doEdxLogin(String username, String password, OnResponseCallback<AuthResponse> callback) {

        new LoginTask(context, username, password) {
            @Override
            protected void onSuccess(AuthResponse authResponse) throws Exception {
                super.onSuccess(authResponse);
                callback.onSuccess(authResponse);
            }

            @Override
            protected void onException(Exception ex) {
                callback.onFailure(ex);
            }
        }.execute();

    }

    public void logout() {
        syncAnalytics();
        edxEnvironment.getRouter().performManualLogout(
                context,
                mDataManager.getEdxEnvironment().getAnalyticsRegistry(),
                mDataManager.getEdxEnvironment().getNotificationDelegate());

        new Thread() {
            @Override
            public void run() {
                mLocalDataSource.clear();
            }
        }.start();
    }

    public Observable<EmptyResponse> getEmpty() {
        return preEmptyProcess(mRemoteDataSource.getEmpty());
    }

    public void getCollectionConfig(OnResponseCallback<CollectionConfigResponse> callback) {

        //Mocking start
        /*List<Category> categories = new ArrayList<>();
        List<ContentList> contentLists = new ArrayList<>();
        List<Source> sources = new ArrayList<>();
        for (int i = 0; i < 5; i++){
            Category category = new Category();
            category.setId(i);
            switch (i){
                case 0:
                    category.setName(context.getString(R.string.all));
                    break;
                case 1:
                    category.setName(context.getString(R.string.course));
                    break;
                case 2:
                    category.setName(context.getString(R.string.chatshala));
                    break;
                case 3:
                    category.setName(context.getString(R.string.hois));
                    break;
                case 4:
                    category.setName(context.getString(R.string.toolkit));
                    break;
            }
            category.setOrder(i);
            category.setSource_id(i-1);
            categories.add(category);

            if (i == 0){
                ContentList contentList = new ContentList();
                contentList.setId(i);
                contentList.setCategory_id(i);
                contentList.setFormat_type(ContentListType.feature.toString());
                contentList.setOrder(i);
                contentList.setName("Featured");
                contentLists.add(contentList);
            }

            for (int j = 1; j < 5; j++) {
                ContentList contentList = new ContentList();
                contentList.setId(j);
                contentList.setCategory_id(i);
                contentList.setFormat_type(ContentListType.normal.toString());
                contentList.setOrder(j);
                switch (j){
                    case 1:
                        contentList.setName("Must See");
                        break;
                    case 2:
                        contentList.setName("Continue Watching");
                        break;
                    case 3:
                        contentList.setName("Recently Added");
                        break;
                    case 4:
                        contentList.setName("Favourites");
                        break;
                }
                contentLists.add(contentList);
            }

            if (i < 4){
                Source source = new Source();
                source.setId(i);
                switch (i){
                    case 0:
                        source.setName(context.getString(R.string.course));
                        break;
                    case 1:
                        source.setName(context.getString(R.string.chatshala));
                        break;
                    case 2:
                        source.setName(context.getString(R.string.hois));
                        break;
                    case 3:
                        source.setName(context.getString(R.string.toolkit));
                        break;
                }
                sources.add(source);
            }
        }
        CollectionConfigResponse response = new CollectionConfigResponse();
        response.setCategory(categories);
        response.setContent_list(contentLists);
        response.setSource_id(sources);
        callback.onSuccess(response);*/
        //Mocking end

        //Actual code   **Do not delete**
        if (NetworkUtil.isConnected(context)) {
            new GetCollectionConfigTask(context) {
                @Override
                protected void onSuccess(CollectionConfigResponse response) throws Exception {
                    super.onSuccess(response);
                    if (response != null) {
                        new Thread() {
                            @Override
                            public void run() {
                                mLocalDataSource.insertConfiguration(response);
                            }
                        }.start();
                    }
                    callback.onSuccess(response);
                }

                @Override
                protected void onException(Exception ex) {
                    getCollectionConfigFromLocal(callback, ex);
                }
            }.execute();
        } else {
            getCollectionConfigFromLocal(callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    private void getCollectionConfigFromLocal(OnResponseCallback<CollectionConfigResponse> callback, Exception ex) {
        new AsyncTask<Void, Void, CollectionConfigResponse>() {

            @Override
            protected CollectionConfigResponse doInBackground(Void... voids) {
                return mLocalDataSource.getConfiguration();
            }

            @Override
            protected void onPostExecute(CollectionConfigResponse collectionConfigResponse) {
                super.onPostExecute(collectionConfigResponse);
                if (collectionConfigResponse == null ||
                        collectionConfigResponse.getCategory() == null ||
                        collectionConfigResponse.getCategory().isEmpty()) {
                    callback.onFailure(ex);
                } else {
                    callback.onSuccess(collectionConfigResponse);
                }
            }
        }.execute();
        /*new Thread(){
            @Override
            public void run() {
                CollectionConfigResponse response = mLocalDataSource.getConfiguration();
                if (response == null || response.getCategory() == null || response.getCategory().isEmpty()) {
                    callback.onFailure(ex);
                } else {
                    callback.onSuccess(response);
                }
            }
        }.start();*/
    }

    public void getConfigModifiedDate(OnResponseCallback<ConfigModifiedDateResponse> callback) {

        if (NetworkUtil.isConnected(context)) {
            new GetConfigModifiedDateTask(context) {
                @Override
                protected void onSuccess(ConfigModifiedDateResponse configModifiedDateResponse) throws Exception {
                    super.onSuccess(configModifiedDateResponse);
                    callback.onSuccess(configModifiedDateResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void getCollectionItems(Long[] listIds, int skip, int take, OnResponseCallback<List<CollectionItemsResponse>> callback) {

        //Mocking start
        /*List<Content> contents = new ArrayList<>();
        List<Long> lists1 = new ArrayList<>();
        List<Long> lists2 = new ArrayList<>();
        lists1.add(0L);
        lists1.add(2L);
        lists1.add(3L);
        lists2.add(1L);
        lists2.add(3L);
        lists2.add(4L);
        for (int i = 0; i < 50; i++){
            Content content = new Content();
            content.setId(i);
            switch (i%4){
                case 0:
                    content.setName("संख्या की शुरूआती समझ");
                    break;
                case 1:
                    content.setName("भिन्न - एक परिचय");
                    break;
                case 2:
                    content.setName("बीजगणित की सोच बनाना");
                    break;
                case 3:
                    content.setName("स्थानीय मान की समझ");
                    break;
            }
            if (i%10 == 0){
                content.setLists(lists1);
                content.setIcon("http://theteacherapp.org/asset-v1:Mathematics+M01+201706_Mat_01+type@asset+block@Math_sample2.png");
            } else {
                content.setLists(lists2);
                content.setIcon("http://theteacherapp.org/asset-v1:Language+01+201706_Lan_01+type@asset+block@Emergent_Literacy_ICON_93_kb.png");
            }
            content.setSource_id(i%4);
            contents.add(content);
        }
        CollectionItemsResponse contentResponse = new CollectionItemsResponse();
        contentResponse.setContent(contents);
        callback.onSuccess(contentResponse);*/
        //Mocking end

        //Actual code   **Do not delete**
        if (NetworkUtil.isConnected(context)) {
            Bundle parameters = new Bundle();
            long[] listIds_long = new long[listIds.length];
            for (int i = 0; i < listIds.length; i++) {
                listIds_long[i] = listIds[i];
            }
            parameters.putLongArray(Constants.KEY_LIST_IDS, listIds_long);
            parameters.putInt(Constants.KEY_SKIP, skip);
            parameters.putInt(Constants.KEY_TAKE, take);
            new GetCollectionItemsTask(context, parameters) {
                @Override
                protected void onSuccess(List<CollectionItemsResponse> collectionItemsList) throws Exception {
                    super.onSuccess(collectionItemsList);
                    if (collectionItemsList != null && !collectionItemsList.isEmpty()) {

                        new Thread() {
                            @Override
                            public void run() {

                                for (CollectionItemsResponse itemsResponse : collectionItemsList) {
                                    if (itemsResponse.getContent() != null) {
                                        for (Content content : itemsResponse.getContent()) {
                                            if (content.getLists() == null) {
                                                content.setLists(new ArrayList<>());
                                            }

                                            Content localContent = mLocalDataSource.getContentById(content.getId());
                                            if (localContent != null && localContent.getLists() != null) {
                                                content.getLists().addAll(localContent.getLists());
                                            }

                                            if (!content.getLists().contains(itemsResponse.getId())) {
                                                content.getLists().add(itemsResponse.getId());
                                            }

                                            mLocalDataSource.insertContent(content);
                                        }
                                    }
                                }
                            }
                        }.start();
                    }
                    callback.onSuccess(collectionItemsList);
                }

                @Override
                protected void onException(Exception ex) {
                    getCollectionItemsFromLocal(listIds, callback, ex);
                }
            }.execute();
        } else {
            getCollectionItemsFromLocal(listIds, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    private void getCollectionItemsFromLocal(Long[] listIds, OnResponseCallback<List<CollectionItemsResponse>> callback, Exception ex) {
        new AsyncTask<Void, Void, List<CollectionItemsResponse>>() {

            @Override
            protected List<CollectionItemsResponse> doInBackground(Void... voids) {
                List<Content> contents = mLocalDataSource.getContents();
                List<CollectionItemsResponse> responses = new ArrayList<>();
                if (contents != null) {
                    for (Long listId : listIds) {
                        CollectionItemsResponse response = new CollectionItemsResponse();
                        response.setId(listId);
                        response.setContent(new ArrayList<>());
                        responses.add(response);
                    }
                    List<Long> requiredListIds = Arrays.asList(listIds);
                    for (Content content : contents) {
                        if (content.getLists() != null) {
                            for (long listId : content.getLists()) {
                                if (requiredListIds.contains(listId)) {
                                    for (CollectionItemsResponse response : responses) {
                                        if (response.getId() == listId) {
                                            response.getContent().add(content);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return responses;
            }

            @Override
            protected void onPostExecute(List<CollectionItemsResponse> responses) {
                super.onPostExecute(responses);

                if (responses == null || responses.isEmpty()) {
                    callback.onFailure(ex);
                } else {
                    callback.onSuccess(responses);
                }
            }
        }.execute();

        /*new Thread(){
            @Override
            public void run() {
                List<Content> contents = mLocalDataSource.getContents();
                List<CollectionItemsResponse> responses = new ArrayList<>();
                if (contents != null){
                    List<Long> requiredListIds = Arrays.asList(listIds);
                    List<Long> recordedListIds = new ArrayList<>();
                    for (Content content: contents){
                        for (long listId: content.getLists()){
                            if (requiredListIds.contains(listId)){
                                if (recordedListIds.contains(listId)){
                                    for (CollectionItemsResponse response: responses){
                                        if (response.getId() == listId){
                                            response.getContent().add(content);
                                            break;
                                        }
                                    }
                                } else {
                                    recordedListIds.add(listId);
                                    CollectionItemsResponse response = new CollectionItemsResponse();
                                    response.setId(listId);
                                    List<Content> temp = new ArrayList<>();
                                    temp.add(content);
                                    response.setContent(temp);
                                    responses.add(response);
                                }
                            }
                        }
                    }
                }

                if (responses.isEmpty()){
                    callback.onFailure(ex);
                } else {
                    callback.onSuccess(responses);
                }
            }
        }.start();*/
    }

    public void getStateAgendaCount(OnResponseCallback<List<AgendaList>> callback) {

        //Mocking start
        /*AgendaList agendaList1 = new AgendaList();
        AgendaList agendaList2 = new AgendaList();
        AgendaList agendaList3 = new AgendaList();
        agendaList1.setLevel("State");
        agendaList2.setLevel("District");
        agendaList3.setLevel("Block");
        List<AgendaItem> items = new ArrayList<>();
        for (int i = 0; i < 4; i++){
            AgendaItem item = new AgendaItem();
            item.setContent_count(10 - i);
            item.setSource_id(i);
            switch (i){
                case 0:
                    item.setSource_name("Course");
                    break;
                case 1:
                    item.setSource_name("Chatshala");
                    break;
                case 2:
                    item.setSource_name("HOIS");
                    break;
                default:
                    item.setSource_name("Toolkit");
                    break;
            }
            items.add(item);
        }
        agendaList1.setResult(items);
        agendaList2.setResult(items);
        agendaList3.setResult(items);

        List<AgendaList> agendaLists = new ArrayList<>();
        agendaLists.add(agendaList1);
        agendaLists.add(agendaList2);
        agendaLists.add(agendaList3);
        callback.onSuccess(agendaLists);*/
        //Mocking end

        //Actual code   **Do not delete**
        if (NetworkUtil.isConnected(context)) {
            new GetStateAgendaCountTask(context) {
                @Override
                protected void onSuccess(List<AgendaList> agendaLists) throws Exception {
                    super.onSuccess(agendaLists);
                    callback.onSuccess(agendaLists);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getMyAgendaCount(OnResponseCallback<AgendaList> callback) {

        //Mocking start
        /*AgendaList agendaList = new AgendaList();
        agendaList.setLevel("My");
        List<AgendaItem> items = new ArrayList<>();
        for (int i = 0; i < 3; i++){
            AgendaItem item = new AgendaItem();
            item.setContent_count(10 - i);
            item.setSource_id(i);
            switch (i){
                case 0:
                    item.setSource_title("कोर्स");
                    item.setSource_name("course");
                    break;
                case 1:
                    item.setSource_title("शिक्षण सामग्री");
                    item.setSource_name("toolkit");
                    break;
                default:
                    item.setSource_title("प्रेरणा स्त्रोत");
                    item.setSource_name("hois");
                    break;
            }
            items.add(item);
        }
        agendaList.setResult(items);
        callback.onSuccess(agendaList);*/
        //Mocking end

        //Actual code   **Do not delete**
        if (NetworkUtil.isConnected(context)) {
            new GetMyAgendaCountTask(context) {
                @Override
                protected void onSuccess(AgendaList agendaList) throws Exception {
                    super.onSuccess(agendaList);
                    callback.onSuccess(agendaList);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getDownloadAgendaCount(List<Source> sources, OnResponseCallback<AgendaList> callback) {

        //Mocking start
        /*AgendaList agendaList = new AgendaList();
        agendaList.setLevel("Download");
        List<AgendaItem> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            AgendaItem item = new AgendaItem();
            item.setContent_count(10 - i);
            item.setSource_id(i);
            switch (i) {
                case 0:
                    item.setSource_title("कोर्स");
                    item.setSource_name("course");
                    break;
                case 1:
                    item.setSource_title("Chatशाला");
                    item.setSource_name("chatshala");
                    break;
                case 2:
                    item.setSource_title("शिक्षण सामग्री");
                    item.setSource_name("toolkit");
                    break;
                default:
                    item.setSource_title("प्रेरणा स्त्रोत");
                    item.setSource_name("hois");
                    break;
            }
            items.add(item);
        }
        agendaList.setResult(items);
        callback.onSuccess(agendaList);*/
        //Mocking end

        //Actual code **Do not delete**
        Map<String, Boolean> receivedSources = new HashMap<>();
        for (Source source : sources) {
            receivedSources.put(source.getName(), false);
        }

        AgendaList agendaList = new AgendaList();
        agendaList.setLevel("Download");
        agendaList.setResult(new ArrayList<>());

        for (Source source : sources) {
            if (source.getType().equalsIgnoreCase(SourceType.edx.name()) ||
                    source.getType().equalsIgnoreCase(SourceType.course.name())) {

                getdownloadedCourseContents(new OnResponseCallback<List<Content>>() {
                    @Override
                    public void onSuccess(List<Content> data) {
                        addContentsToAgendaList(data, agendaList, source.getName(), source.getTitle());
                        receivedSources.put(source.getName(), true);
                        sendDownloadAgenda(receivedSources, agendaList, callback);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        addContentsToAgendaList(null, agendaList, source.getName(), source.getTitle());
                        receivedSources.put(source.getName(), true);
                        sendDownloadAgenda(receivedSources, agendaList, callback);
                    }
                });

            } else {

                getdownloadedWPContents(source.getName(), new OnResponseCallback<List<Content>>() {
                    @Override
                    public void onSuccess(List<Content> data) {
                        addContentsToAgendaList(data, agendaList, source.getName(), source.getTitle());
                        receivedSources.put(source.getName(), true);
                        sendDownloadAgenda(receivedSources, agendaList, callback);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        addContentsToAgendaList(null, agendaList, source.getName(), source.getTitle());
                        receivedSources.put(source.getName(), true);
                        sendDownloadAgenda(receivedSources, agendaList, callback);
                    }
                });

            }
        }

    }

    private void addContentsToAgendaList(List<Content> contents, AgendaList agendaList,
                                         String sourceName, String sourceTitle) {

        AgendaItem item = new AgendaItem();
        item.setContent_count(contents == null ? 0 : contents.size());
        item.setSource_name(sourceName);
        item.setSource_title(sourceTitle);
        agendaList.getResult().add(item);

    }

    private void sendDownloadAgenda(Map<String, Boolean> receivedSources, AgendaList agendaList,
                                    OnResponseCallback<AgendaList> callback) {

        if (receivedSources.values().contains(false)) {
            return;
        }
        callback.onSuccess(agendaList);
    }

    public void getdownloadedCourseContents(OnResponseCallback<List<Content>> callback) {

        new AsyncTask<Void, Void, List<Content>>() {
            @Override
            protected List<Content> doInBackground(Void... voids) {
                List<Long> contentIds = new GetCourseContentsOperation().execute(dbHelper.getDatabase());
                List<Content> contents = new ArrayList<>();
                if (contentIds != null) {
                    for (long id : contentIds) {
                        Content content = mLocalDataSource.getContentById(id);
                        if (content != null) {
                            contents.add(content);
                        }
                    }
                }
                return contents;
            }

            @Override
            protected void onPostExecute(List<Content> contents) {
                super.onPostExecute(contents);
                if (!contents.isEmpty()) {
                    callback.onSuccess(contents);
                } else {
                    callback.onFailure(new TaException("No content downloaded"));
                }
            }
        }.execute();

    }

    public void getdownloadedWPContents(String sourceName, OnResponseCallback<List<Content>> callback) {

        new AsyncTask<Void, Void, List<Content>>() {
            @Override
            protected List<Content> doInBackground(Void... voids) {
                List<Long> contentIds = new GetWPContentsOperation(sourceName).execute(dbHelper.getDatabase());
                List<Content> contents = new ArrayList<>();
                if (contentIds != null) {
                    for (long id : contentIds) {
                        Content content = mLocalDataSource.getContentById(id);
                        if (content != null) {
                            contents.add(content);
                        }
                    }
                }
                return contents;
            }

            @Override
            protected void onPostExecute(List<Content> contents) {
                super.onPostExecute(contents);
                if (!contents.isEmpty()) {
                    callback.onSuccess(contents);
                } else {
                    callback.onFailure(new TaException("No content downloaded"));
                }
            }
        }.execute();

    }

    public void getBlocks(OnResponseCallback<List<RegistrationOption>> callback, Bundle parameters,
                          @NonNull List<RegistrationOption> blocks) {

        new GetUserAddressTask(context, parameters) {
            @Override
            protected void onSuccess(UserAddressResponse userAddressResponse) throws Exception {
                super.onSuccess(userAddressResponse);
                blocks.clear();
                if (userAddressResponse != null && userAddressResponse.getBlock() != null) {
                    for (Object o : userAddressResponse.getBlock()) {
                        blocks.add(new RegistrationOption(o.toString(), o.toString()));
                    }
                    callback.onSuccess(blocks);
                }
            }

            @Override
            protected void onException(Exception ex) {
                callback.onFailure(ex);
            }
        }.execute();

    }

    public void getCourse(String courseId, OnResponseCallback<EnrolledCoursesResponse> callback) {

        if (NetworkUtil.isConnected(context)) {
            new UserEnrollmentCourseTask(context, courseId) {
                @Override
                protected void onSuccess(EnrolledCoursesResponse enrolledCoursesResponse) throws Exception {
                    super.onSuccess(enrolledCoursesResponse);
                    if (enrolledCoursesResponse == null ||
                            enrolledCoursesResponse.getMode() == null ||
                            enrolledCoursesResponse.getMode().equals("")
                    ) {
                        callback.onFailure(new TaException("Invalid Course"));
                    } else {
                        callback.onSuccess(enrolledCoursesResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getCourseFromLocal(courseId, callback, ex);
                }
            }.execute();
        } else {
            getCourseFromLocal(courseId, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getCourseFromLocal(String courseId, OnResponseCallback<EnrolledCoursesResponse> callback, Exception e) {
        new UserEnrollmentCourseFromCacheTask(context, courseId) {
            @Override
            protected void onSuccess(EnrolledCoursesResponse enrolledCoursesResponse) throws Exception {
                super.onSuccess(enrolledCoursesResponse);
                if (enrolledCoursesResponse == null ||
                        enrolledCoursesResponse.getMode() == null ||
                        enrolledCoursesResponse.getMode().equals("")
                ) {
                    callback.onFailure(new TaException("Invalid Course"));
                } else {
                    callback.onSuccess(enrolledCoursesResponse);
                }
            }

            @Override
            protected void onException(Exception ex) {
                callback.onFailure(e);
            }
        }.execute();
    }

    public void getTotalLikes(long contentId, OnResponseCallback<TotalLikeResponse> callback) {
        if (NetworkUtil.isConnected(context)) {
            new TotalLikeTask(context, contentId) {
                @Override
                protected void onSuccess(TotalLikeResponse totalLikeResponse) throws Exception {
                    super.onSuccess(totalLikeResponse);
                    if (totalLikeResponse == null) {
                        callback.onFailure(new TaException("No response for total likes."));
                    } else {
                        callback.onSuccess(totalLikeResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void isLike(long contentId, OnResponseCallback<StatusResponse> callback) {
        if (NetworkUtil.isConnected(context)) {
            new IsLikeTask(context, contentId) {
                @Override
                protected void onSuccess(StatusResponse statusResponse) throws Exception {
                    super.onSuccess(statusResponse);
                    if (statusResponse == null) {
                        callback.onFailure(new TaException("No response for is like."));
                    } else {
                        callback.onSuccess(statusResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void isContentMyAgenda(long contentId, OnResponseCallback<StatusResponse> callback) {
        if (NetworkUtil.isConnected(context)) {
            new IsContentMyAgendaTask(context, contentId) {
                @Override
                protected void onSuccess(StatusResponse statusResponse) throws Exception {
                    super.onSuccess(statusResponse);
                    if (statusResponse == null) {
                        callback.onFailure(new TaException("No response for is content my agenda."));
                    } else {
                        callback.onSuccess(statusResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void setLike(long contentId, OnResponseCallback<StatusResponse> callback) {
        if (NetworkUtil.isConnected(context)) {
            new SetLikeTask(context, contentId) {
                @Override
                protected void onSuccess(StatusResponse statusResponse) throws Exception {
                    super.onSuccess(statusResponse);
                    if (statusResponse == null) {
                        callback.onFailure(new TaException("Error occured. Couldn't like."));
                    } else {
                        callback.onSuccess(statusResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void setLikeUsingSourceIdentity(String sourceIdentity, OnResponseCallback<StatusResponse> callback) {
        if (NetworkUtil.isConnected(context)) {
            new SetLikeUsingSourceIdentityTask(context, sourceIdentity) {
                @Override
                protected void onSuccess(StatusResponse statusResponse) throws Exception {
                    super.onSuccess(statusResponse);
                    if (statusResponse == null) {
                        callback.onFailure(new TaException("Error occured. Couldn't like."));
                    } else {
                        callback.onSuccess(statusResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void setBookmark(long contentId, OnResponseCallback<BookmarkResponse> callback) {
        if (NetworkUtil.isConnected(context)) {
            new SetBookmarkTask(context, contentId) {
                @Override
                protected void onSuccess(BookmarkResponse bookmarkResponse) throws Exception {
                    super.onSuccess(bookmarkResponse);
                    if (bookmarkResponse == null) {
                        callback.onFailure(new TaException("Error occured. Couldn't add to My Agenda."));
                    } else {
                        callback.onSuccess(bookmarkResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void getCourseComponent(String courseId, OnResponseCallback<CourseComponent> callback) {

        CourseComponent courseComponent = courseManager.getComponentByCourseId(courseId);
        if (courseComponent != null) {
            // Course data exist in app session cache
            callback.onSuccess(courseComponent);
            return;
        }

        new GetCourseDataFromPersistableCacheTask(context, courseId) {
            @Override
            protected void onSuccess(CourseComponent courseComponent) throws Exception {
                super.onSuccess(courseComponent);
                courseComponent = courseManager.getComponentByCourseId(courseId);
                if (courseComponent != null) {
                    callback.onSuccess(courseComponent);
                } else {
                    getCourseComponentFromServer(courseId, callback);
                }
            }

            @Override
            protected void onException(Exception ex) {
                getCourseComponentFromServer(courseId, callback);
            }
        }.execute();

    }

    public void getCourseComponentFromServer(String courseId, OnResponseCallback<CourseComponent> callback) {

        if (!NetworkUtil.isConnected(context)) {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
            return;
        }

        Call<CourseStructureV1Model> getHierarchyCall = courseApi.getCourseStructureWithoutStale(courseId);
        getHierarchyCall.enqueue(new CourseAPI.GetCourseStructureCallback(context, courseId,
                null, null, null, null) {
            @Override
            protected void onResponse(@NonNull CourseComponent courseComponent) {
                courseManager.addCourseDataInAppLevelCache(courseId, courseComponent);
                courseComponent = courseManager.getComponentByCourseId(courseId);
                if (courseComponent != null) {
                    callback.onSuccess(courseComponent);
                } else {
                    callback.onFailure(new TaException("Empty course."));
                }
            }

            @Override
            protected void onFailure(@NonNull Throwable error) {
                super.onFailure(error);
                callback.onFailure(new TaException(error.getLocalizedMessage()));
            }
        });
    }

    @NonNull
    private CourseComponent validateCourseComponent(@NonNull CourseComponent courseComponent, String courseId, String courseComponentId) {
        final CourseComponent cached = courseManager.getComponentByIdFromAppLevelCache(
                courseId, courseComponentId);
        courseComponent = cached != null ? cached : courseComponent;
        return courseComponent;
    }


    public void downloadSingle(ScormBlockModel scorm,
                               long contentId,
                               FragmentActivity activity,
                               VideoDownloadHelper.DownloadManagerCallback callback) {
        DownloadEntry de = scorm.getDownloadEntry(edxEnvironment.getStorage());
        de.url = scorm.getDownloadUrl();
        de.title = scorm.getParent().getDisplayName();
        de.content_id = contentId;
        downloadManager.downloadVideo(de, activity, callback);
    }

    public void downloadSingle(ScormBlockModel scorm,
                               FragmentActivity activity,
                               VideoDownloadHelper.DownloadManagerCallback callback) {
        DownloadEntry de = scorm.getDownloadEntry(edxEnvironment.getStorage());
        de.url = scorm.getDownloadUrl();
        de.title = scorm.getParent().getDisplayName();
        downloadManager.downloadVideo(de, activity, callback);
    }

    public void downloadMultiple(List<? extends HasDownloadEntry> downloadEntries,
                                 long contentid,
                                 FragmentActivity activity,
                                 VideoDownloadHelper.DownloadManagerCallback callback) {
        downloadManager.downloadVideos(downloadEntries, contentid, activity, callback);
    }

    public void getDownloadedStateForVideoId(String videoId, DataCallback<DownloadEntry.DownloadedState> callback) {
        edxEnvironment.getDatabase().getDownloadedStateForVideoId(videoId, callback);
    }

    public boolean scormNotDownloaded(ScormBlockModel scorm) {
        return getScormStatus(scorm).equals(ScormStatus.not_downloaded);
    }

    public boolean scormDownloading(ScormBlockModel scorm) {
        return getScormStatus(scorm).equals(ScormStatus.downloading);
    }

    public ScormStatus getScormStatus(ScormBlockModel scorm) {

        DownloadEntry entry = scorm.getDownloadEntry(edxEnvironment.getStorage());
        return getDownloadStatus(entry);

    }


    private ScormStatus getDownloadStatus(DownloadEntry entry) {
        if (entry == null || entry.downloaded.equals(DownloadEntry.DownloadedState.ONLINE)) {
            return ScormStatus.not_downloaded;
        } else if (entry.downloaded.equals(DownloadEntry.DownloadedState.DOWNLOADING)) {
            return ScormStatus.downloading;
        } else if (entry.watched.equals(DownloadEntry.WatchedState.UNWATCHED)) {
            return ScormStatus.downloaded;
        } else if (entry.watched.equals(DownloadEntry.WatchedState.PARTIALLY_WATCHED)) {
            return ScormStatus.watching;
        } else {
            return ScormStatus.watched;
        }
    }

    public void deleteScorm(ScormBlockModel scormBlockModel) {
        DownloadEntry de = scormBlockModel.getDownloadEntry(edxEnvironment.getStorage());
        de.url = scormBlockModel.getDownloadUrl();
        de.title = scormBlockModel.getParent().getDisplayName();
        edxEnvironment.getStorage().removeDownload(de);
    }

    public void getHtmlFromUrl(HttpUrl absoluteUrl, OnResponseCallback<String> callback) {
        if (NetworkUtil.isConnected(context)) {

            new Task<HtmlResponse>(context) {
                @Override
                public HtmlResponse call() throws Exception {
                    IRemoteDataSource source = RetrofitServiceUtil.create(context, false);
                    return source.getHtmlFromUrl(absoluteUrl).execute().body();
                }

                @Override
                protected void onSuccess(HtmlResponse htmlResponse) throws Exception {
                    super.onSuccess(htmlResponse);
                    callback.onSuccess(htmlResponse.getContent());
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

            /*new GetHtmlFromUrlTask(context, absoluteUrl){
                @Override
                protected void onSuccess(Void s) throws Exception {
                    super.onSuccess(s);
                    callback.onSuccess(s.toString());
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();*/
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void getMyAgendaContent(long sourceId, OnResponseCallback<List<Content>> callback) {

        //Mocking start
        /*List<Content> contents = new ArrayList<>();
        for (int i=0;i<20;i++){
            Content content =  new Content();
            content.setName("course");
            content.setIcon("http://theteacherapp.org/asset-v1:Mathematics+M01+201706_Mat_01+type@asset+block@Math_sample2.png");
            Source source =  new Source();
            source.setType("humana");
            source.setTitle("course");
            source.setName("course");
            content.setSource(source);
            contents.add(content);
        }
        callback.onSuccess(contents);*/
        //Mocking end

        //Actual code   **Do not delete**
        if (NetworkUtil.isConnected(context)) {
            new GetMyAgendaContentTask(context, sourceId) {
                @Override
                protected void onSuccess(List<Content> response) throws Exception {
                    super.onSuccess(response);
                    if (response != null && response.size() > 0) {
                        callback.onSuccess(response);
                    } else {
                        callback.onFailure(new TaException("No data found"));
                    }

                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void getStateAgendaContent(long sourceId, long list_id, OnResponseCallback<List<Content>> callback) {

        //Mocking start
        /*List<Content> contents = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Content content = new Content();
            content.setName("course");
            content.setIcon("http://theteacherapp.org/asset-v1:Mathematics+M01+201706_Mat_01+type@asset+block@Math_sample2.png");
            Source source = new Source();
            source.setType("humana");
            source.setTitle("course");
            source.setName("course");
            content.setSource(source);
            contents.add(content);
        }
        callback.onSuccess(contents);*/
        //Mocking end

        //Actual code   **Do not delete**
        if (NetworkUtil.isConnected(context)) {
            new GetStateAgendaContentTask(context, sourceId, list_id) {
                @Override
                protected void onSuccess(List<Content> response) throws Exception {
                    super.onSuccess(response);
                    if (response != null && response.size() > 0) {
                        callback.onSuccess(response);
                    } else {
                        callback.onFailure(new TaException("No data found."));
                    }

                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void getDownloadedContent(String sourceName, OnResponseCallback<List<Content>> callback) {

        //Mocking start
        /*List<Content> contents = new ArrayList<>();
        for (int i=0;i<20;i++){
            Content content =  new Content();
            content.setName("course");
            content.setIcon("http://theteacherapp.org/asset-v1:Mathematics+M01+201706_Mat_01+type@asset+block@Math_sample2.png");
            Source source =  new Source();
            source.setType("humana");
            source.setTitle("course");
            source.setName("course");
            content.setSource(source);
            contents.add(content);
        }
        callback.onSuccess(contents);*/
        //Mocking end

        //Actual code   **Do not delete**
        if (sourceName.equalsIgnoreCase(SourceName.course.name())) {
            getdownloadedCourseContents(callback);
        } else {
            getdownloadedWPContents(sourceName, callback);
        }
    }

    public void getPostById(long postId, OnResponseCallback<Post> callback) {

        if (NetworkUtil.isConnected(context)) {

            wpClientRetrofit.getPost(postId, new WordPressRestResponse<Post>() {
                @Override
                public void onSuccess(Post result) {
                    if (result != null) {
                        callback.onSuccess(result);
                    } else {
                        callback.onFailure(new TaException("Invalid post"));
                    }
                }

                @Override
                public void onFailure(HttpServerErrorResponse errorResponse) {
                    callback.onFailure(new TaException(errorResponse.getMessage()));
                }
            });

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getPostBySlug(String slug, OnResponseCallback<Post> callback) {

        if (NetworkUtil.isConnected(context)) {

            wpClientRetrofit.getPostBySlug(slug, new WordPressRestResponse<List<Post>>() {
                @Override
                public void onSuccess(List<Post> result) {
                    if (result == null || result.isEmpty()) {
                        callback.onFailure(new TaException("Post not found"));
                    } else {
                        callback.onSuccess(result.get(0));
                    }
                }

                @Override
                public void onFailure(HttpServerErrorResponse errorResponse) {
                    callback.onFailure(new TaException(errorResponse.getMessage()));
                }
            });

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getCommentsByPost(long postId, int take, int page, OnResponseCallback<List<Comment>> callback) {

        if (NetworkUtil.isConnected(context)) {

            wpClientRetrofit.getCommentsByPost(postId, take, page, new WordPressRestResponse<List<Comment>>() {
                @Override
                public void onSuccess(List<Comment> result) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(HttpServerErrorResponse errorResponse) {
                    callback.onFailure(new TaException(errorResponse.getMessage()));
                }
            });

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getRepliesOnComment(long postId, long commentId, OnResponseCallback<List<Comment>> callback) {

        if (NetworkUtil.isConnected(context)) {

            wpClientRetrofit.getRepliesOnComment(postId, commentId, new WordPressRestResponse<List<Comment>>() {
                @Override
                public void onSuccess(List<Comment> result) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(HttpServerErrorResponse errorResponse) {
                    callback.onFailure(new TaException(errorResponse.getMessage()));
                }
            });

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void downloadPost(Post post, long contentId, String category_id, String category_name,
                             FragmentActivity activity,
                             VideoDownloadHelper.DownloadManagerCallback callback) {

        DownloadEntry videoData = new DownloadEntry();
        videoData.setDownloadEntryForPost(contentId, category_id, category_name, post);
        downloadManager.downloadVideo(videoData, activity, callback);

    }

    public void deletePost(Post post) {

        DownloadEntry entry = edxEnvironment.getStorage().getPostVideo(String.valueOf(post.getId()));
        if (entry != null)
            edxEnvironment.getStorage().removeDownload(entry);

    }

    public ScormStatus getPostDownloadStatus(Post post) {

        DownloadEntry entry = edxEnvironment.getStorage().getPostVideo(String.valueOf(post.getId()));
        return getDownloadStatus(entry);

    }

    public void addComment(String comment, int commentParentId, long postId, OnResponseCallback<Comment> callback) {
        if (loginPrefs.getWPCurrentUserProfile() == null || loginPrefs.getWPCurrentUserProfile().id == null) {
            callback.onFailure(new TaException("Not authenticated to comment."));
            return;
        }

        String ua = new WebView(context).getSettings().getUserAgentString();
        CustomComment obj = new CustomComment();
        obj.author = loginPrefs.getWPCurrentUserProfile().id;
        //obj.author_ip=ip;
        obj.author_url = "";
        obj.author_user_agent = ua;
        obj.content = comment;
        obj.date = DateUtil.getCurrentDateForServerLocal();
        obj.date_gmt = DateUtil.getCurrentDateForServerGMT();
        obj.parent = commentParentId;
        obj.post = postId;

        addComment(obj, callback);
    }

    private void addComment(CustomComment comment, OnResponseCallback<Comment> callback) {
        wpClientRetrofit.createComment(comment, new WordPressRestResponse<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(HttpServerErrorResponse errorResponse) {
                callback.onFailure(new TaException(errorResponse.getMessage()));
            }
        });
    }

    public void setWpProfileCache() {
        wpClientRetrofit.getUserMe(new WordPressRestResponse<User>() {
            @Override
            public void onSuccess(User result) {
                WPProfileModel model = new WPProfileModel();
                model.name = result.getName();
                model.username = result.getUsername();
                model.id = result.getId();
                if (result.getRoles() != null && result.getRoles().size() > 0)
                    model.roles = result.getRoles();
                loginPrefs.setWPCurrentUserProfileInCache(model);
            }

            @Override
            public void onFailure(HttpServerErrorResponse errorResponse) {
                if (config.isWordpressAuthentication() &&
                        !NetworkUtil.isLimitedAcess(errorResponse) && NetworkUtil.isUnauthorize(errorResponse)) {
                    logout();
                    Toast.makeText(context, "Session expire", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public DownloadEntry getDownloadedVideo(Post post, long contentId, String categoryId, String categoryName) {
        DownloadEntry videoData = new DownloadEntry();
        videoData.setDownloadEntryForPost(contentId, categoryId, categoryName, post);

        return edxEnvironment.getStorage().getPostVideo(videoData.videoId, videoData.url);

    }

    public void getSearchFilter(OnResponseCallback<SearchFilter> callback) {

        //Mocking start
        /*SearchFilter searchFilter = new SearchFilter();
        List<FilterSection> sections = new ArrayList<>();
        for (int i = 0; i < 5; i++){
            FilterSection section = new FilterSection();
            section.setName("Section " + (i+1));
            List<FilterTag> tags = new ArrayList<>();
            for (int j = 0; j < 5; j++){
                FilterTag tag = new FilterTag();
                tag.setDisplay_name("Tag " + (i+1) + (j+1));
                tags.add(tag);
            }
            section.setTags(tags);
            sections.add(section);
        }
        searchFilter.setResult(sections);
        callback.onSuccess(searchFilter);*/
        //Mocking start

        //Actual code   **Do not delete**
        if (NetworkUtil.isConnected(context)) {
            new GetSearchFilterTask(context) {
                @Override
                protected void onSuccess(SearchFilter searchFilter) throws Exception {
                    super.onSuccess(searchFilter);
                    if (searchFilter == null) {
                        callback.onFailure(new TaException("Cannot fetch filters"));
                    } else {
                        List<FilterSection> tempSections = new ArrayList<>();
                        for (FilterSection section : searchFilter.getResult()) {
                            if (section.getTags() == null || section.getTags().isEmpty()) {
                                tempSections.add(section);
                            }
                        }
                        for (FilterSection section : tempSections) {
                            searchFilter.getResult().remove(section);
                        }
                        Collections.sort(searchFilter.getResult());
                        callback.onSuccess(searchFilter);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getCategoryFromLocal(long sourceId, OnResponseCallback<Category> callback) {

        new AsyncTask<Void, Void, Category>() {
            @Override
            protected Category doInBackground(Void... voids) {
                return mLocalDataSource.getCategoryBySourceId(sourceId);
            }

            @Override
            protected void onPostExecute(Category category) {
                super.onPostExecute(category);
                if (category == null) {
                    callback.onFailure(new TaException("Category not found."));
                } else {
                    callback.onSuccess(category);
                }
            }
        }.execute();

    }

    public void getContentListsFromLocal(long categoryId, String mode, OnResponseCallback<List<ContentList>> callback) {

        new AsyncTask<Void, Void, List<ContentList>>() {
            @Override
            protected List<ContentList> doInBackground(Void... voids) {
                return mLocalDataSource.getContentListsByCategoryIdAndMode(categoryId, mode);
            }

            @Override
            protected void onPostExecute(List<ContentList> contentLists) {
                super.onPostExecute(contentLists);
                if (contentLists == null || contentLists.isEmpty()) {
                    callback.onFailure(new TaException("Content lists not found."));
                } else {
                    Collections.sort(contentLists);
                    callback.onSuccess(contentLists);
                }
            }
        }.execute();

    }

    public void search(int take, int skip, boolean isPriority, long listId, String searchText, List<FilterSection> sections,
                       OnResponseCallback<List<Content>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new SearchTask(context, take, skip, isPriority, listId, searchText, sections) {
                @Override
                protected void onSuccess(List<Content> contents) throws Exception {
                    super.onSuccess(contents);
                    if (contents == null) {
                        contents = new ArrayList<>();
                    }

                    if (!contents.isEmpty()) {
                        List<Content> finalContents = contents;
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    for (Content content : finalContents) {
                                        if (content.getLists() == null) {
                                            content.setLists(new ArrayList<>());
                                        }

                                        Content localContent = mLocalDataSource.getContentById(content.getId());
                                        if (localContent != null && localContent.getLists() != null) {
                                            content.getLists().addAll(localContent.getLists());
                                        }
                                        mLocalDataSource.insertContent(content);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }

                    callback.onSuccess(contents);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void submitFeedback(String msg, OnResponseCallback<FeedbackResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            Bundle parameters = new Bundle();
            parameters.putString(Constants.KEY_USERNAME, loginPrefs.getUsername());
            parameters.putString(Constants.KEY_FEEDBACK, msg);
            parameters.putString(Constants.KEY_DEVICE_INFO,
                    "API Level:" + Build.VERSION.RELEASE + "  Device:" + Build.DEVICE + "  Model no:" + Build.MODEL + "  Product:" + Build.PRODUCT);

            new SubmitFeedbackTask(context, parameters) {
                @Override
                protected void onSuccess(FeedbackResponse feedbackResponse) throws Exception {
                    super.onSuccess(feedbackResponse);
                    callback.onSuccess(feedbackResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void changePassword(String oldPass, String newPass, OnResponseCallback<ChangePasswordResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            Bundle parameters = new Bundle();
            parameters.putString(Constants.KEY_OLD_PASSWORD, oldPass);
            parameters.putString(Constants.KEY_NEW_PASSWORD, newPass);
            parameters.putString(Constants.KEY_USERNAME, loginPrefs.getUsername());

            new ChangePasswordTask(context, parameters) {
                @Override
                protected void onSuccess(ChangePasswordResponse changePasswordResponse) throws Exception {
                    super.onSuccess(changePasswordResponse);

                    if (changePasswordResponse != null && changePasswordResponse.isSuccess()) {
                        edxEnvironment.getRouter().resetAuthForChangePassword(context,
                                edxEnvironment.getAnalyticsRegistry(), edxEnvironment.getNotificationDelegate());
                        login(loginPrefs.getUsername(), newPass, new OnResponseCallback<AuthResponse>() {
                            @Override
                            public void onSuccess(AuthResponse data) {
                                callback.onSuccess(changePasswordResponse);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(context,
                                        "Password changed successfully. Please login to continue",
                                        Toast.LENGTH_LONG).show();
                                logout();
                            }
                        });
                    } else {
                        callback.onFailure(new TaException("Error in changing password"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getAccount(OnResponseCallback<Account> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetAccountTask(context, loginPrefs.getUsername()) {
                @Override
                protected void onSuccess(Account account) throws Exception {
                    super.onSuccess(account);
                    if (account != null) {
                        loginPrefs.setProfileImage(loginPrefs.getUsername(), account.getProfileImage());

                        getProfile(new OnResponseCallback<ProfileModel>() {
                            @Override
                            public void onSuccess(ProfileModel data) {
                                loginPrefs.setCurrentUserProfileInCache(data);
                                callback.onSuccess(account);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onSuccess(account);
                            }
                        });

                    } else {
                        callback.onFailure(new TaException("Invalid account"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void updateProfile(Bundle parameters, OnResponseCallback<UpdateMyProfileResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new UpdateMyProfileTask(context, parameters, loginPrefs.getUsername()) {
                @Override
                protected void onSuccess(UpdateMyProfileResponse updateMyProfileResponse) throws Exception {
                    super.onSuccess(updateMyProfileResponse);

                    if (updateMyProfileResponse == null) {
                        callback.onFailure(new TaException("Your action could not be completed"));
                        return;
                    }

                    //for cache consistency
                    ProfileModel profileModel = new ProfileModel();
                    profileModel.name = updateMyProfileResponse.getName();
                    profileModel.email = updateMyProfileResponse.getEmail();
                    profileModel.gender = updateMyProfileResponse.getGender();

                    profileModel.title = updateMyProfileResponse.getTitle();
                    profileModel.classes_taught = updateMyProfileResponse.getClasses_taught();
                    profileModel.state = updateMyProfileResponse.getState();
                    profileModel.district = updateMyProfileResponse.getDistrict();
                    profileModel.block = updateMyProfileResponse.getBlock();
                    profileModel.pmis_code = updateMyProfileResponse.getPMIS_code();
                    profileModel.diet_code = updateMyProfileResponse.getDIETCode();
                    profileModel.setTagLabel(updateMyProfileResponse.getTagLabel());
                    profileModel.setFollowers(updateMyProfileResponse.getFollowers());
                    profileModel.setFollowing(updateMyProfileResponse.getFollowing());

                    loginPrefs.setCurrentUserProfileInCache(profileModel);
                    loginPrefs.removeMxProfilePageCache();

                    callback.onSuccess(updateMyProfileResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void updateProfileImage(@NonNull Uri uri, @NonNull Rect cropRect,
                                   OnResponseCallback<ProfileImage> callback) {

        if (NetworkUtil.isConnected(context)) {

            new SetAccountImageTask(context, loginPrefs.getUsername(), uri, cropRect) {
                @Override
                protected void onSuccess(Void response) throws Exception {
                    super.onSuccess(response);

                    getAccount(new OnResponseCallback<Account>() {
                        @Override
                        public void onSuccess(Account data) {
                            loginPrefs.setProfileImage(loginPrefs.getUsername(), data.getProfileImage());
                            callback.onSuccess(data.getProfileImage());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getProfile(OnResponseCallback<ProfileModel> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetProfileTask(context) {
                @Override
                protected void onSuccess(ProfileModel profileModel) throws Exception {
                    super.onSuccess(profileModel);
                    callback.onSuccess(profileModel);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            getProfileFromLocal(new TaException(context.getString(R.string.no_connection_exception)), callback);
        }

    }

    public void getProfileFromLocal(Exception e, OnResponseCallback<ProfileModel> callback) {
        ProfileModel model = loginPrefs.getCurrentUserProfile();
        if (model != null) {
            callback.onSuccess(model);
        } else {
            callback.onFailure(e);
        }
    }

    public void getCertificateStatus(String courseId, OnResponseCallback<CertificateStatusResponse> callback) {

        getCertificateFromLocal(courseId, new OnResponseCallback<Certificate>() {
            @Override
            public void onSuccess(Certificate data) {
                CertificateStatusResponse response = new CertificateStatusResponse();
                response.setStatus(CertificateStatus.GENERATED.name());
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(Exception e) {

                if (NetworkUtil.isConnected(context)) {

                    new GetCertificateStatusTask(context, courseId) {
                        @Override
                        protected void onSuccess(CertificateStatusResponse certificateStatusResponse) throws Exception {
                            super.onSuccess(certificateStatusResponse);
                            if (certificateStatusResponse != null) {
                                callback.onSuccess(certificateStatusResponse);
                            } else {
                                callback.onFailure(new TaException("Status of certificate could not be fetched"));
                            }
                        }

                        @Override
                        protected void onException(Exception ex) {
                            callback.onFailure(ex);
                        }
                    }.execute();

                } else {
                    callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
                }

            }
        }, null);

    }

    public void getCertificate(String courseId, OnResponseCallback<Certificate> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetCertificateTask(context, courseId) {
                @Override
                protected void onSuccess(MyCertificatesResponse myCertificatesResponse) throws Exception {
                    super.onSuccess(myCertificatesResponse);
                    if (myCertificatesResponse == null || myCertificatesResponse.getCertificates() == null ||
                            myCertificatesResponse.getCertificates().isEmpty()) {
                        getCertificateFromLocal(courseId, callback, new TaException("Certificate not available"));
                    } else {
                        new Thread() {
                            @Override
                            public void run() {
                                Certificate certificate = myCertificatesResponse.getCertificates().get(0);
                                if (certificate.getUsername() == null) {
                                    certificate.setUsername(loginPrefs.getUsername());
                                }
                                mLocalDataSource.insertCertificate(certificate);
                            }
                        }.start();

                        callback.onSuccess(myCertificatesResponse.getCertificates().get(0));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getCertificateFromLocal(courseId, callback, ex);
                }
            }.execute();

        } else {
            getCertificateFromLocal(courseId, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getCertificateFromLocal(String courseId, OnResponseCallback<Certificate> callback, Exception e) {

        new AsyncTask<Void, Void, Certificate>() {
            @Override
            protected Certificate doInBackground(Void... voids) {
                return mLocalDataSource.getCertificate(courseId, loginPrefs.getUsername());
            }

            @Override
            protected void onPostExecute(Certificate certificate) {
                if (certificate != null) {
                    callback.onSuccess(certificate);
                } else {
                    callback.onFailure(e);
                }
            }
        }.execute();

    }

    public void getMyCertificates(OnResponseCallback<List<Certificate>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetMyCertificatesTask(context) {
                @Override
                protected void onSuccess(MyCertificatesResponse myCertificatesResponse) throws Exception {
                    super.onSuccess(myCertificatesResponse);
                    if (myCertificatesResponse == null || myCertificatesResponse.getCertificates() == null) {
                        getMyCertificatesFromLocal(callback, new TaException("Certificates not available"));
                    } else {
                        new Thread() {
                            @Override
                            public void run() {
                                List<Certificate> certificates = myCertificatesResponse.getCertificates();
                                for (Certificate certificate : certificates) {
                                    if (certificate.getUsername() == null) {
                                        certificate.setUsername(loginPrefs.getUsername());
                                    }
                                }
                                mLocalDataSource.insertCertificates(certificates);
                            }
                        }.start();

                        callback.onSuccess(myCertificatesResponse.getCertificates());
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getMyCertificatesFromLocal(callback, new TaException("Certificates not available"));
                }
            }.execute();

        } else {
            getMyCertificatesFromLocal(callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getMyCertificatesFromLocal(OnResponseCallback<List<Certificate>> callback, Exception e) {

        new AsyncTask<Void, Void, List<Certificate>>() {
            @Override
            protected List<Certificate> doInBackground(Void... voids) {
                return mLocalDataSource.getAllCertificates(loginPrefs.getUsername());
            }

            @Override
            protected void onPostExecute(List<Certificate> certificates) {
                if (certificates != null) {
                    callback.onSuccess(certificates);
                } else {
                    callback.onFailure(e);
                }
            }
        }.execute();

    }

    public void generateCertificate(String courseId, OnResponseCallback<CertificateStatusResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GenerateCertificateTask(context, courseId) {
                @Override
                protected void onSuccess(CertificateStatusResponse certificateStatusResponse) throws Exception {
                    super.onSuccess(certificateStatusResponse);
                    if (certificateStatusResponse != null) {
                        callback.onSuccess(certificateStatusResponse);
                    } else {
                        callback.onFailure(new TaException("Certificate could not be generated"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getContent(long contentId, OnResponseCallback<Content> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetContentTask(context, contentId) {
                @Override
                protected void onSuccess(Content content) throws Exception {
                    super.onSuccess(content);
                    if (content != null && content.getDetail() == null) {
                        new Thread() {
                            @Override
                            public void run() {
                                Content localContent = mLocalDataSource.getContentById(content.getId());
                                if (localContent == null) {
                                    mLocalDataSource.insertContent(content);
                                }
                            }
                        }.start();

                        callback.onSuccess(content);
                    } else {
                        getContentFromLocal(contentId, callback, new TaException("Content not found"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getContentFromLocal(contentId, callback, ex);
                }
            }.execute();

        } else {
            getContentFromLocal(contentId, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getContentFromLocal(long contentId, OnResponseCallback<Content> callback, Exception e) {

        new AsyncTask<Void, Void, Content>() {
            @Override
            protected Content doInBackground(Void... voids) {
                return mLocalDataSource.getContentById(contentId);
            }

            @Override
            protected void onPostExecute(Content content) {
                super.onPostExecute(content);
                if (content != null) {
                    callback.onSuccess(content);
                } else {
                    callback.onFailure(e);
                }
            }
        }.execute();

    }

    public void getSuggestedUsers(int take, int skip, OnResponseCallback<List<SuggestedUser>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetSuggestedUsersTask(context, take, skip) {
                @Override
                protected void onSuccess(List<SuggestedUser> suggestedUsers) throws Exception {
                    super.onSuccess(suggestedUsers);
                    if (suggestedUsers == null || suggestedUsers.isEmpty()) {
                        callback.onFailure(new TaException("No suggested users"));
                    } else {
                        List<SuggestedUser> emptyUsers = new ArrayList<>();
                        for (SuggestedUser user : suggestedUsers) {
                            if (user.getUsername() == null) {
                                emptyUsers.add(user);
                            }
                        }
                        for (SuggestedUser user : emptyUsers) {
                            suggestedUsers.remove(user);
                        }
                        if (!suggestedUsers.isEmpty()) {
                            callback.onSuccess(suggestedUsers);
                        } else {
                            callback.onFailure(new TaException("No suggested users"));
                        }
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void followUnfollowUser(String username, OnResponseCallback<StatusResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new FollowUserTask(context, username) {
                @Override
                protected void onSuccess(StatusResponse statusResponse) throws Exception {
                    super.onSuccess(statusResponse);
                    if (statusResponse == null) {
                        callback.onFailure(new TaException("Error occured while following"));
                    } else {
                        callback.onSuccess(statusResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getDiscussionTopics(String courseId, OnResponseCallback<List<DiscussionTopicDepth>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetDiscussionTopicsTask(context, courseId) {
                @Override
                protected void onSuccess(CourseTopics courseTopics) throws Exception {
                    super.onSuccess(courseTopics);
                    if (courseTopics == null ||
                            (courseTopics.getCoursewareTopics() == null && courseTopics.getNonCoursewareTopics() == null)) {
                        callback.onFailure(new TaException("No discussion topics available"));
                    } else {
                        List<DiscussionTopic> allTopics = new ArrayList<>();
                        if (courseTopics.getNonCoursewareTopics() != null) {
                            allTopics.addAll(courseTopics.getNonCoursewareTopics());
                        }
                        if (courseTopics.getCoursewareTopics() != null) {
                            allTopics.addAll(courseTopics.getCoursewareTopics());
                        }
                        if (!allTopics.isEmpty()) {
                            List<DiscussionTopicDepth> allTopicsWithDepth =
                                    DiscussionTopicDepth.createFromDiscussionTopics(allTopics);
                            callback.onSuccess(allTopicsWithDepth);
                        } else {
                            callback.onFailure(new TaException("No discussion topics available"));
                        }
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getDiscussionThreads(String courseId, List<String> topicIds, String view,
                                     String orderBy, int take, int page, List<String> requestedFields,
                                     OnResponseCallback<List<DiscussionThread>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetDiscussionThreadsTask(context, courseId, topicIds, view, orderBy, take, page, requestedFields) {
                @Override
                protected void onSuccess(Page<DiscussionThread> discussionThreadPage) throws Exception {
                    super.onSuccess(discussionThreadPage);
                    if (discussionThreadPage == null || discussionThreadPage.getResults().isEmpty()) {
                        callback.onFailure(new TaException("No discussion threads available"));
                    } else {
                        callback.onSuccess(discussionThreadPage.getResults());
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getThreadComments(String threadId, int take, int page, List<String> requestedFields, boolean isQuestionType,
                                  OnResponseCallback<List<DiscussionComment>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetThreadCommentsTask(context, threadId, take, page, requestedFields, isQuestionType) {
                @Override
                protected void onSuccess(Page<DiscussionComment> discussionCommentPage) throws Exception {
                    super.onSuccess(discussionCommentPage);
                    if (discussionCommentPage == null || discussionCommentPage.getResults().isEmpty()) {
                        callback.onFailure(new TaException("No comments available"));
                    } else {
                        callback.onSuccess(discussionCommentPage.getResults());
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getCommentReplies(String commentId, int take, int page, List<String> requestedFields,
                                  OnResponseCallback<List<DiscussionComment>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetCommentRepliesTask(context, commentId, take, page, requestedFields) {
                @Override
                protected void onSuccess(Page<DiscussionComment> discussionCommentPage) throws Exception {
                    super.onSuccess(discussionCommentPage);
                    if (discussionCommentPage == null || discussionCommentPage.getResults().isEmpty()) {
                        callback.onFailure(new TaException("No replies available"));
                    } else {
                        callback.onSuccess(discussionCommentPage.getResults());
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void createDiscussionThread(String courseId, String title, String body,
                                       String topicId, DiscussionThread.ThreadType type,
                                       OnResponseCallback<DiscussionThread> callback) {

        if (NetworkUtil.isConnected(context)) {

            new CreateDiscussionThreadTask(context, courseId, title, body, topicId, type) {
                @Override
                protected void onSuccess(DiscussionThread thread) throws Exception {
                    super.onSuccess(thread);
                    if (thread != null) {
                        callback.onSuccess(thread);
                    } else {
                        callback.onFailure(new TaException("Unable to create discussion thread"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void createDiscussionComment(String threadId, String comment, String parentCommentId,
                                        OnResponseCallback<DiscussionComment> callback) {

        if (NetworkUtil.isConnected(context)) {

            new CreateDiscussionCommentTask(context, threadId, comment, parentCommentId) {
                @Override
                protected void onSuccess(DiscussionComment comment) throws Exception {
                    super.onSuccess(comment);
                    if (comment != null) {
                        callback.onSuccess(comment);
                    } else {
                        callback.onFailure(new TaException("Unable to comment"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void likeDiscussionThread(String threadId, boolean liked, OnResponseCallback<DiscussionThread> callback) {

        if (NetworkUtil.isConnected(context)) {

            new LikeDiscussionThreadTask(context, threadId, liked) {
                @Override
                protected void onSuccess(DiscussionThread thread) throws Exception {
                    super.onSuccess(thread);
                    if (thread != null) {
                        callback.onSuccess(thread);
                    } else {
                        callback.onFailure(new TaException("Unable to like discussion thread"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void likeDiscussionComment(String commentId, boolean liked, OnResponseCallback<DiscussionComment> callback) {

        if (NetworkUtil.isConnected(context)) {

            new LikeDiscussionCommentTask(context, commentId, liked) {
                @Override
                protected void onSuccess(DiscussionComment comment) throws Exception {
                    super.onSuccess(comment);
                    if (comment != null) {
                        callback.onSuccess(comment);
                    } else {
                        callback.onFailure(new TaException("Unable to like comment"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void findContentForAssistant(String searchText, List<String> tags, OnResponseCallback<List<Content>> callback) {
        if (NetworkUtil.isConnected(context)) {

            edxDataManager.getTaAPI().assistantSearch(searchText, tags).enqueue(new Callback<List<Content>>() {
                @Override
                protected void onResponse(@NonNull List<Content> responseBody) {
                    if (callback != null)
                        callback.onSuccess(responseBody);
                }

                @Override
                protected void onFailure(@NonNull Throwable error) {
                    super.onFailure(error);
                    if (callback != null)
                        callback.onFailure(new TaException(error.getMessage()));

                }
            });

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void getSources(OnResponseCallback<List<Source>> callback) {

        new AsyncTask<Void, Void, List<Source>>() {
            @Override
            protected List<Source> doInBackground(Void... voids) {
                return mLocalDataSource.getSources();
            }

            @Override
            protected void onPostExecute(List<Source> sources) {
                if (sources == null || sources.isEmpty()) {
                    callback.onFailure(new TaException("No sources available"));
                } else {
                    callback.onSuccess(sources);
                }
            }
        }.execute();

    }

    public void updateNotifications(OnResponseCallback<CountResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new AsyncTask<Void, Void, List<Notification>>() {
                @Override
                protected List<Notification> doInBackground(Void... voids) {
                    return mLocalDataSource.getAllUnupdatedNotifications(loginPrefs.getUsername());
                }

                @Override
                protected void onPostExecute(List<Notification> notifications) {
                    super.onPostExecute(notifications);

                    if (notifications != null && !notifications.isEmpty()) {
                        List<String> notificationIds = new ArrayList<>();
                        for (Notification notification : notifications) {
                            notificationIds.add(notification.getId());
                        }

                        new UpdateNotificationsTask(context, notificationIds) {
                            @Override
                            protected void onSuccess(CountResponse countResponse) throws Exception {
                                super.onSuccess(countResponse);

                                for (Notification notification : notifications) {
                                    notification.setUpdated(true);
                                }
                                updateNotificationsInLocal(notifications);

                                if (callback != null) {
                                    callback.onSuccess(countResponse);
                                }
                            }

                            @Override
                            protected void onException(Exception ex) {
                                if (callback != null) {
                                    callback.onFailure(ex);
                                }
                            }
                        }.execute();
                    }
                }
            }.execute();

        } else {
            if (callback != null) {
                callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
            }
        }

    }

    public void updateNotificationsInLocal(List<Notification> notifications) {

        new Thread() {
            @Override
            public void run() {
                mLocalDataSource.updateNotifications(notifications);
            }
        }.start();

    }

   /* public void getNotifications(int take, int skip, OnResponseCallback<List<Notification>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetNotificationsTask(context, take, skip) {
                @Override
                protected void onSuccess(List<Notification> notifications) throws Exception {
                    super.onSuccess(notifications);

                    if (notifications != null) {

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                mLocalDataSource.insertNotifications(notifications);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);
                                getNotificationsFromLocal(take, skip, callback, new TaException("NotificationResponse not available"));
                            }
                        }.execute();

                    } else {
                        getNotificationsFromLocal(take, skip, callback, new TaException("NotificationResponse not available"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getNotificationsFromLocal(take, skip, callback, ex);
                }
            }.execute();

        } else {
            getNotificationsFromLocal(take, skip, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }
*/

    public void getNotifications(int take, int skip, String course_id, OnResponseCallback<NotificationResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetNotificationsTask(context, take, skip, course_id) {
                @Override
                protected void onSuccess(NotificationResponse notificationResponse) throws Exception {
                    super.onSuccess(notificationResponse);

                    if (notificationResponse != null) {

                        callback.onSuccess(notificationResponse);
                    }


                }
            }.execute();
        }
    }
    public void sendNotifications(String title, String type, String desc, String action,
                                  String action_id, String action_parent_id, String respondent,
                                  String unique_id,
                                  OnResponseCallback<SuccessResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new SendNotificationTask(context, title, type, desc, action, action_id, action_parent_id, respondent, unique_id) {
                @Override
                protected void onSuccess(SuccessResponse response) throws Exception {
                    super.onSuccess(response);

                }

                @Override
                protected void onException(Exception ex) {
                }
            }.execute();

        }

    }

    public void getNotificationsFromLocal(int take, int skip, OnResponseCallback<List<Notification>> callback, Exception e) {

        new AsyncTask<Void, Void, List<Notification>>() {
            @Override
            protected List<Notification> doInBackground(Void... voids) {
                return mLocalDataSource.getAllNotificationsInPage(loginPrefs.getUsername(), take, skip);
            }

            @Override
            protected void onPostExecute(List<Notification> notifications) {
                super.onPostExecute(notifications);
                if (notifications == null || notifications.isEmpty()) {
                    callback.onFailure(e);
                } else {
                    callback.onSuccess(notifications);
                }
            }
        }.execute();

    }

    public void createNotification(Notification notification) {

        if (NetworkUtil.isConnected(context)) {

            new CreateNotificationsTask(context, Collections.singletonList(notification)) {
                @Override
                protected void onSuccess(List<Notification> notifications) throws Exception {
                    super.onSuccess(notifications);
                    if (notifications != null && !notifications.isEmpty()) {
                        Notification n = notifications.get(0);
                        if (n.getId() != null) {
                            new Thread() {
                                @Override
                                public void run() {
                                    mLocalDataSource.insertNotification(n);
                                }
                            }.start();
                        }
                    }
                }

                @Override
                protected void onException(Exception ex) {

                }
            }.execute();

        }

    }

    public void onAppStart() {
        syncAnalytics();
    }

    public void syncAnalytics() {

        try {
            Analytic analytic = new Analytic(context);
            analytic.syncAnalytics();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getFeeds(int take, int skip, OnResponseCallback<List<Feed>> callback) {

        //Mocking start
        /*if (skip > 5){
            callback.onFailure(new TaException("No feeds available"));
            return;
        }

        List<Feed> feeds = new ArrayList<>();
        for (int i = 0; i < take; i++){
            Feed feed = new Feed();
            feed.setId(String.valueOf(take*skip + i));
            feed.setAction_by("Chirag");
            feed.setAction_on("1556014771");
            feed.setAction(Action.CourseLike.name());

            FeedMetadata metadata = new FeedMetadata();
            metadata.setIcon("http://theteacherapp.org/asset-v1:Pedagogy+01+2017_Ped_01+type@asset+block@Question_Logo.png");
            metadata.setComment_count("5");
            metadata.setLike_count("80");
            metadata.setId("course-v1:Pedagogy+01+2017_Ped_01");
            metadata.setSource("कोर्स");
            metadata.setText("सवाल पूछने के कौशल");

            feed.setMeta_data(metadata);

            if (i%2 == 0){
                metadata.setUser_name("Hermione Granger");
                metadata.setUser_icon("https://cdn.vox-cdn.com/thumbor/aiU71J02TAxm0F0h5HP-ELtk0To=/0x0:1024x768/920x613/filters:focal(408x210:570x372):format(webp)/cdn.vox-cdn.com/uploads/chorus_image/image/51000509/harry-potter-top-10-hermione-granger-moments-hermione-granger-358045.0.jpg");
                metadata.setTag_label("कक्षा_1st कक्षा_2nd कौशल_हिंदी कौशल_English भाषा_हिंदी भाषा_English");
            }

            if (i%3 == 0){
                metadata.setLiked(true);
            }

            feeds.add(feed);
        }
        callback.onSuccess(feeds);*/
        //Mocking end

        //Actual code    **DO NOT DELETE**
        if (NetworkUtil.isConnected(context)) {

            new GetFeedsTask(context, take, skip) {
                @Override
                protected void onSuccess(List<Feed> feeds) throws Exception {
                    super.onSuccess(feeds);

                    if (feeds != null && !feeds.isEmpty()) {

                        new Thread() {
                            @Override
                            public void run() {
                                for (Feed feed : feeds) {
                                    feed.setUsername(loginPrefs.getUsername());
                                }
                                mLocalDataSource.insertFeeds(feeds);
                            }
                        }.start();

                        callback.onSuccess(feeds);
                    } else {
                        callback.onFailure(new TaException("No feeds available"));
                    }

                }

                @Override
                protected void onException(Exception ex) {
                    getFeedsFromLocal(take, skip, callback, ex);
                }
            }.execute();

        } else {
            getFeedsFromLocal(take, skip, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getFeedsFromLocal(int take, int skip, OnResponseCallback<List<Feed>> callback, Exception e) {

        new AsyncTask<Void, Void, List<Feed>>() {
            @Override
            protected List<Feed> doInBackground(Void... voids) {
                return mLocalDataSource.getFeeds(loginPrefs.getUsername(), take, skip);
            }

            @Override
            protected void onPostExecute(List<Feed> feeds) {
                super.onPostExecute(feeds);
                if (feeds != null && !feeds.isEmpty()) {
                    callback.onSuccess(feeds);
                } else {
                    callback.onFailure(e);
                }
            }
        }.execute();

    }

    public void getContentFromSourceIdentity(String sourceIdentity, OnResponseCallback<Content> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetContentFromSourceIdentityTask(context, sourceIdentity) {
                @Override
                protected void onSuccess(Content content) throws Exception {
                    super.onSuccess(content);

                    if (content == null || content.getId() == 0) {
                        getLocalContentFromSourceIdentity(sourceIdentity, callback,
                                new TaException("Content not found"));
                    } else {
                        new Thread() {
                            @Override
                            public void run() {
                                Content localContent = mLocalDataSource.getContentById(content.getId());
                                if (localContent == null) {
                                    mLocalDataSource.insertContent(content);
                                }
                            }
                        }.start();
                        callback.onSuccess(content);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getLocalContentFromSourceIdentity(sourceIdentity, callback, ex);
                }
            }.execute();

        } else {
            getLocalContentFromSourceIdentity(sourceIdentity, callback,
                    new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getLocalContentFromSourceIdentity(String sourceIdentity, OnResponseCallback<Content> callback, Exception e) {

        new AsyncTask<Void, Void, Content>() {
            @Override
            protected Content doInBackground(Void... voids) {
                return mLocalDataSource.getContentBySourceIdentity(sourceIdentity);
            }

            @Override
            protected void onPostExecute(Content content) {
                super.onPostExecute(content);

                if (content == null) {
                    callback.onFailure(e);
                } else {
                    callback.onSuccess(content);
                }
            }
        }.execute();

    }

    public void getFeedFeatureList(OnResponseCallback<List<Content>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new AsyncTask<Void, Void, ContentList>() {
                @Override
                protected ContentList doInBackground(Void... voids) {
                    List<ContentList> contentLists = mLocalDataSource.getContentListsByRootCategory("feed");
                    if (contentLists == null || contentLists.isEmpty()) {
                        return null;
                    } else {
                        return contentLists.get(0);
                    }
                }

                @Override
                protected void onPostExecute(ContentList contentList) {
                    super.onPostExecute(contentList);

                    if (contentList != null) {
                        getCollectionItems(new Long[]{contentList.getId()}, 0, 5,
                                new OnResponseCallback<List<CollectionItemsResponse>>() {
                                    @Override
                                    public void onSuccess(List<CollectionItemsResponse> data) {
                                        if (data == null || data.isEmpty() ||
                                                data.get(0).getContent() == null ||
                                                data.get(0).getContent().isEmpty()
                                        ) {
                                            callback.onFailure(new TaException("Featured contents not available"));
                                        } else {
                                            callback.onSuccess(data.get(0).getContent());
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        callback.onFailure(e);
                                    }
                                });
                    } else {
                        callback.onFailure(new TaException("Feature list not available"));
                    }
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void setUserContent(List<ContentStatus> statuses, OnResponseCallback<List<ContentStatus>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new SetUserContentTask(context, statuses) {
                @Override
                protected void onSuccess(List<ContentStatus> contentStatuses) throws Exception {
                    super.onSuccess(contentStatuses);

                    if (contentStatuses != null && contentStatuses.size() == statuses.size()) {

                        List<ContentStatus> finalStatuses = new ArrayList<>();
                        for (ContentStatus status : contentStatuses) {
                            if (status.getError() == null) {
                                status.setUsername(loginPrefs.getUsername());
                                finalStatuses.add(status);
                            }
                        }

                        new Thread() {
                            @Override
                            public void run() {
                                mLocalDataSource.insertContentStatuses(finalStatuses);
                            }
                        }.start();

                        callback.onSuccess(finalStatuses);

                    } else {
                        callback.onFailure(new TaException("Could not set status of contents"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getMyContentStatuses(OnResponseCallback<List<ContentStatus>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetMyContentStatusTask(context) {
                @Override
                protected void onSuccess(List<ContentStatus> statuses) throws Exception {
                    super.onSuccess(statuses);

                    if (statuses == null) {
                        getMyContentStatusesFromLocal(callback, new TaException("Could not fetch status of contents"));
                    } else {

                        for (ContentStatus status : statuses) {
                            status.setUsername(loginPrefs.getUsername());
                        }

                        new Thread() {
                            @Override
                            public void run() {
                                mLocalDataSource.insertContentStatuses(statuses);
                            }
                        }.start();

                        callback.onSuccess(statuses);

                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getMyContentStatusesFromLocal(callback, ex);
                }
            }.execute();

        } else {
            getMyContentStatusesFromLocal(callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getMyContentStatusesFromLocal(OnResponseCallback<List<ContentStatus>> callback, Exception e) {

        new AsyncTask<Void, Void, List<ContentStatus>>() {
            @Override
            protected List<ContentStatus> doInBackground(Void... voids) {
                return mLocalDataSource.getMyContentStatuses(loginPrefs.getUsername());
            }

            @Override
            protected void onPostExecute(List<ContentStatus> statuses) {
                super.onPostExecute(statuses);

                if (statuses != null) {
                    callback.onSuccess(statuses);
                } else {
                    callback.onFailure(e);
                }
            }
        }.execute();

    }

    public void getUserContentStatus(List<Long> contentIds, OnResponseCallback<List<ContentStatus>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetUserContentStatusTask(context, contentIds) {
                @Override
                protected void onSuccess(List<ContentStatus> statuses) throws Exception {
                    super.onSuccess(statuses);

                    if (statuses == null) {
                        getUserContentStatusFromLocal(contentIds, callback, new TaException("Could not fetch status of contents"));
                    } else {

                        List<ContentStatus> finalStatuses = new ArrayList<>();
                        for (ContentStatus status : statuses) {
                            if (status.getError() == null) {
                                status.setUsername(loginPrefs.getUsername());
                                finalStatuses.add(status);
                            }
                        }

                        new Thread() {
                            @Override
                            public void run() {
                                mLocalDataSource.insertContentStatuses(finalStatuses);
                            }
                        }.start();

                        callback.onSuccess(finalStatuses);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getUserContentStatusFromLocal(contentIds, callback, ex);
                }
            }.execute();

        } else {
            getUserContentStatusFromLocal(contentIds, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getUserContentStatusFromLocal(List<Long> contentIds, OnResponseCallback<List<ContentStatus>> callback,
                                              Exception e) {

        new AsyncTask<Void, Void, List<ContentStatus>>() {
            @Override
            protected List<ContentStatus> doInBackground(Void... voids) {
                return mLocalDataSource.getContentStatusesByContentIds(contentIds, loginPrefs.getUsername());
            }

            @Override
            protected void onPostExecute(List<ContentStatus> statuses) {
                super.onPostExecute(statuses);

                if (statuses != null) {
                    callback.onSuccess(statuses);
                } else {
                    callback.onFailure(e);
                }
            }
        }.execute();

    }

    public void getUnitStatus(String courseId, OnResponseCallback<List<UnitStatus>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetUnitStatusTask(context, courseId) {
                @Override
                protected void onSuccess(List<UnitStatus> statuses) throws Exception {
                    super.onSuccess(statuses);
                    if (statuses != null) {

                        new Thread() {
                            @Override
                            public void run() {
                                for (UnitStatus status : statuses) {
                                    status.setCourse_id(courseId);
                                    status.setUsername(loginPrefs.getUsername());
                                }
                                mLocalDataSource.insertUnitStatuses(statuses);
                            }
                        }.start();

                        callback.onSuccess(statuses);
                    } else {
                        getUnitStatusFromLocal(courseId, callback, new TaException("Unable to fetch unit statuses"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getUnitStatusFromLocal(courseId, callback, ex);
                }
            }.execute();

        } else {
            getUnitStatusFromLocal(courseId, callback, new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getEventCalendar(String programId, String sectionId, String role, int take, int skip,
                                 int count, long eventCalendarDate, OnResponseCallback<List<CalendarEvents>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetEventCalenderTask(context, programId, sectionId, role, eventCalendarDate, count, take, skip) {
                @Override
                protected void onSuccess(List<CalendarEvents> statuses) throws Exception {
                    super.onSuccess(statuses);
                    if (statuses != null) {

                        new Thread() {
                            @Override
                            public void run() {

                            }
                        }.start();

                        callback.onSuccess(statuses);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                }
            }.execute();

        }

    }

    public void getUnitStatusFromLocal(String courseId, OnResponseCallback<List<UnitStatus>> callback, Exception e) {

        new AsyncTask<Void, Void, List<UnitStatus>>() {
            @Override
            protected List<UnitStatus> doInBackground(Void... voids) {
                return mLocalDataSource.getUnitStatusByCourse(loginPrefs.getUsername(), courseId);
            }

            @Override
            protected void onPostExecute(List<UnitStatus> statuses) {
                super.onPostExecute(statuses);

                if (statuses != null) {
                    callback.onSuccess(statuses);
                } else {
                    callback.onFailure(e);
                }
            }
        }.execute();

    }

    public void startScorm(String courseId, String blockId, OnResponseCallback<ScormStartResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new StartScormTask(context, courseId, blockId) {
                @Override
                protected void onSuccess(ScormStartResponse scormStartResponse) throws Exception {
                    super.onSuccess(scormStartResponse);
                    if (callback != null) {
                        callback.onSuccess(scormStartResponse);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    if (callback != null) {
                        callback.onFailure(ex);
                    }
                }
            }.execute();

        } else {
            if (callback != null) {
                callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
            }
        }

    }

    public void getOtherUserAccount(String username, OnResponseCallback<Account> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetAccountTask(context, username) {
                @Override
                protected void onSuccess(Account account) throws Exception {
                    super.onSuccess(account);
                    if (account != null) {
                        callback.onSuccess(account);
                    } else {
                        callback.onFailure(new TaException("Invalid account"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getFollowStatus(String username, OnResponseCallback<FollowStatus> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetFollowStatusTask(context, username) {
                @Override
                protected void onSuccess(FollowStatus followStatus) throws Exception {
                    super.onSuccess(followStatus);

                    if (followStatus == null) {
                        callback.onFailure(new TaException("Follow status could not be fetched"));
                    } else {
                        callback.onSuccess(followStatus);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void getWpUser(long userId, OnResponseCallback<User> callback) {

        if (NetworkUtil.isConnected(context)) {

            wpClientRetrofit.getUser(userId, new WordPressRestResponse<User>() {
                @Override
                public void onSuccess(User result) {
                    if (result == null || result.getUsername() == null) {
                        callback.onFailure(new TaException("User could not be fetched"));
                    } else {
                        callback.onSuccess(result);
                    }
                }

                @Override
                public void onFailure(HttpServerErrorResponse errorResponse) {
                    callback.onFailure(new TaException(errorResponse.getMessage()));
                }
            });

        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }

    }

    public void setCustomFieldAttributes(OnResponseCallback<FieldInfo> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetGenericUserFieldInfoTask(context) {
                @Override
                protected void onSuccess(FieldInfo fieldInfo) {
                    loginPrefs.setMxGenericFieldInfo(fieldInfo);
                    if (callback != null) {
                        callback.onSuccess(fieldInfo);
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    if (callback != null) {
                        callback.onFailure(ex);
                    }
                }
            }.execute();

        } else {
            if (callback != null) {
                callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
            }
        }

    }

    public void getCustomFieldAttributes(OnResponseCallback<FieldInfo> callback) {
        FieldInfo fieldInfo = loginPrefs.getMxGenericFieldInfo();
        if (fieldInfo != null) {
            callback.onSuccess(fieldInfo);
        } else {
            setCustomFieldAttributes(callback);
        }
    }

    public void setConnectCookies() {
        new MxCookiesAPI().execute();
    }

    public void checkSurvey(Activity activity, SurveyType surveyType) {
        new MxSurveyAPI(context, activity, surveyType).execute();
    }

    public void updateFirebaseToken() {
//        FirebaseHelper fireBaseHelper=new FirebaseHelper();
//        fireBaseHelper.updateFirebasetokenToServer(context,fireBaseHelper.getFireBaseParams(loginPrefs.getUsername()));
    }


    public void getenrolledCourseByOrg(String org, OnResponseCallback<List<EnrolledCoursesResponse>> callback) {
        if (NetworkUtil.isConnected(context)) {
            new GetEnrolledCourseTask(context, org, loginPrefs.getUsername()) {
                @Override
                protected void onSuccess(List<EnrolledCoursesResponse> response) throws Exception {
                    super.onSuccess(response);
                    if (response != null && response.size() > 0) {
                        callback.onSuccess(response);
                    } else {
                        callback.onFailure(new TaException("No data found"));
                    }

                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();
        } else {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
        }
    }

    public void getPrograms(OnResponseCallback<List<Program>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetProgramsTask(context) {
                @Override
                protected void onSuccess(List<Program> programs) throws Exception {
                    super.onSuccess(programs);
                    if (programs == null || programs.isEmpty()) {
                        callback.onFailure(new TaException("No programs available"));
                        return;
                    }

                    for (Program program : programs) {
                        program.setUsername(loginPrefs.getUsername());
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            mLocalDataSource.insertPrograms(programs);
                        }
                    }.start();

                    callback.onSuccess(programs);
                }

                @Override
                protected void onException(Exception ex) {
                    getProgramsFromLocal(callback, ex);
                }
            }.execute();

        } else {
            getProgramsFromLocal(callback, new NoConnectionException(context));
        }

    }

    private void getProgramsFromLocal(OnResponseCallback<List<Program>> callback, Exception e) {

        new Task<List<Program>>(context) {
            @Override
            public List<Program> call() {
                return mLocalDataSource.getPrograms(loginPrefs.getUsername());
            }

            @Override
            protected void onSuccess(List<Program> programs) throws Exception {
                super.onSuccess(programs);
                if (programs == null || programs.isEmpty()) {
                    callback.onFailure(e);
                    return;
                }

                callback.onSuccess(programs);
            }

            @Override
            protected void onException(Exception ex) {
                callback.onFailure(e);
            }
        }.execute();

    }

    public void getSections(String programId, OnResponseCallback<List<Section>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetSectionsTask(context, programId) {
                @Override
                protected void onSuccess(List<Section> sections) throws Exception {
                    super.onSuccess(sections);
                    if (sections == null || sections.isEmpty()) {
                        callback.onFailure(new TaException("No sections available"));
                        return;
                    }

                    for (Section section : sections) {
                        section.setUsername(loginPrefs.getUsername());
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            mLocalDataSource.insertSections(sections);
                        }
                    }.start();

                    callback.onSuccess(sections);
                }

                @Override
                protected void onException(Exception ex) {
                    getSectionsFromLocal(callback, ex);
                }
            }.execute();

        } else {
            getSectionsFromLocal(callback, new NoConnectionException(context));
        }

    }

    private void getSectionsFromLocal(OnResponseCallback<List<Section>> callback, Exception e) {

        new Task<List<Section>>(context) {
            @Override
            public List<Section> call() {
                return mLocalDataSource.getSections(loginPrefs.getUsername());
            }

            @Override
            protected void onSuccess(List<Section> sections) throws Exception {
                super.onSuccess(sections);
                if (sections == null || sections.isEmpty()) {
                    callback.onFailure(e);
                    return;
                }

                callback.onSuccess(sections);
            }

            @Override
            protected void onException(Exception ex) {
                callback.onFailure(e);
            }
        }.execute();

    }

    public void getProgramFilters(String program_id, String section_id, String show_in, List<ProgramFilter> filters,
                                  OnResponseCallback<List<ProgramFilter>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetProgramFiltersTask(context, program_id, show_in, section_id, filters) {
                @Override
                protected void onSuccess(List<ProgramFilter> programFilters) throws Exception {
                    super.onSuccess(programFilters);
                    if (programFilters == null || programFilters.isEmpty()) {
                        callback.onFailure(new TaException("Filters are not available"));
                        return;
                    }

                    List<ProgramFilter> removables = new ArrayList<>();
                    for (ProgramFilter filter : programFilters) {
                        if (filter.getTags() == null || filter.getTags().isEmpty()) {
                            removables.add(filter);
                        } else {
                            Collections.sort(filter.getTags());
                        }
                    }

                    for (ProgramFilter filter : removables) {
                        programFilters.remove(filter);
                    }

                    Collections.sort(programFilters);
                    loginPrefs.setProgramFilters(programFilters);
                    callback.onSuccess(programFilters);
                }

                @Override
                protected void onException(Exception ex) {
                    getProgramFiltersFromLocal(callback, ex);
                }
            }.execute();

        } else {
            getProgramFiltersFromLocal(callback, new NoConnectionException(context));
        }
    }

    private void getProgramFiltersFromLocal(OnResponseCallback<List<ProgramFilter>> callback, Exception e) {

        List<ProgramFilter> filters = loginPrefs.getProgramFilters();
        if (filters == null) {
            callback.onFailure(e);
        } else {
            callback.onSuccess(filters);
        }

    }

    public void getPeriods(List<ProgramFilter> filters, String programId, String sectionId,
                           String role, int take, int skip, OnResponseCallback<List<Period>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetPeriodsTask(context, filters, programId, sectionId, role, take, skip) {
                @Override
                protected void onSuccess(List<Period> periods) throws Exception {
                    super.onSuccess(periods);
                    if (periods == null || periods.isEmpty()) {
                        callback.onFailure(new TaException("No periods are created"));
                        return;
                    }

                    for (Period period : periods) {
                        period.setUsername(loginPrefs.getUsername());
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            mLocalDataSource.insertPeriods(periods);
                        }
                    }.start();

                    callback.onSuccess(periods);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void updatePeriods(String programId, String sectionId,
                              String periodId, String periodName, long startDate, long endDate,
                              OnResponseCallback<SuccessResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new UpdatePeriodTask(context, programId, sectionId, periodId, periodName, startDate, endDate) {
                @Override
                protected void onSuccess(SuccessResponse response) throws Exception {
                    super.onSuccess(response);

                    if (response != null && response.getSuccess()) {
                        callback.onSuccess(response);
                    } else {
                        callback.onFailure(new TaException("Error occurred. Unable to set date"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }


    public void getUnits(List<ProgramFilter> filters, String searchText, String programId, String sectionId,
                         String role, String student_username, long periodId, int take, int skip, long startDateTime, long endDateTime, OnResponseCallback<List<Unit>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetUnitsTask(context, filters, searchText, programId, sectionId, role, periodId, take, skip, student_username, startDateTime,
                    endDateTime) {
                @Override
                protected void onSuccess(List<Unit> units) throws Exception {
                    super.onSuccess(units);
                    if (units == null || units.isEmpty()) {
                        callback.onFailure(new TaException("No units are available"));
                        return;
                    }

                    for (Unit unit : units) {
                        unit.setProgramId(programId);
                        unit.setSectionId(sectionId);
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            mLocalDataSource.insertUnits(units);
                        }
                    }.start();

                    callback.onSuccess(units);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void getUserStatus(List<ProgramFilter> filters, String programId, String sectionId,
                              String role, String studentName, int take, int skip, OnResponseCallback<List<Unit>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetUserStatusTask(context, filters, programId, sectionId, role, studentName, take, skip) {
                @Override
                protected void onSuccess(List<Unit> units) throws Exception {
                    super.onSuccess(units);
                    if (units == null || units.isEmpty()) {
                        callback.onFailure(new TaException("No units are available"));
                        return;
                    }

                    for (Unit unit : units) {
                        unit.setProgramId(programId);
                        unit.setSectionId(sectionId);
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            mLocalDataSource.insertUnits(units);
                        }
                    }.start();

                    callback.onSuccess(units);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void getAllUnits(List<ProgramFilter> filters, String programId, String sectionId, String searchText,
                            long periodId, int take, int skip, OnResponseCallback<List<Unit>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetAllUnitsTask(context, filters, programId, sectionId, searchText, periodId, take, skip) {
                @Override
                protected void onSuccess(List<Unit> units) throws Exception {
                    super.onSuccess(units);
                    if (units == null || units.isEmpty()) {
                        callback.onFailure(new TaException("No units are available"));
                        return;
                    }

                    for (Unit unit : units) {
                        unit.setProgramId(programId);
                        unit.setSectionId(sectionId);
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            mLocalDataSource.insertUnits(units);
                        }
                    }.start();

                    callback.onSuccess(units);
                }

                @Override
                protected void onException(Exception ex) {
                    getAllUnitsFromLocal(programId, sectionId, take, skip, callback, ex);
                }
            }.execute();

        } else {
            getAllUnitsFromLocal(programId, sectionId, take, skip, callback, new NoConnectionException(context));
        }
    }

    public void getBlockUnit(String unit_id, OnResponseCallback<CourseComponent> callback) {
        if (NetworkUtil.isConnected(context)) {

            new GetCourseComponentTask(context, unit_id) {
                @Override
                protected void onSuccess(CourseComponent component) throws Exception {
                    super.onSuccess(component);
                    if (component == null) {
                        callback.onFailure(new TaException("No component are available"));
                        return;
                    }

                    new Thread() {
                        @Override
                        public void run() {
                        }
                    }.start();

                    callback.onSuccess(component);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }
    }

    private void getAllUnitsFromLocal(String programId, String sectionId, int take, int skip,
                                      OnResponseCallback<List<Unit>> callback, Exception e) {

        new Task<List<Unit>>(context) {
            @Override
            public List<Unit> call() {
                return mLocalDataSource.getUnits(programId, sectionId, take, skip);
            }

            @Override
            protected void onSuccess(List<Unit> units) throws Exception {
                super.onSuccess(units);
                if (units == null || units.isEmpty()) {
                    callback.onFailure(e);
                    return;
                }

                callback.onSuccess(units);
            }

            @Override
            protected void onException(Exception ex) {
                callback.onFailure(e);
            }
        }.execute();

    }

    public void getUsers(String programId, String sectionId, int take, int skip,
                         OnResponseCallback<List<ProgramUser>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetUsersTask(context, programId, sectionId, take, skip) {
                @Override
                protected void onSuccess(List<ProgramUser> programUsers) throws Exception {
                    super.onSuccess(programUsers);
                    if (programUsers == null || programUsers.isEmpty()) {
                        callback.onFailure(new TaException("No users are available"));
                        return;
                    }

                    callback.onSuccess(programUsers);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void getPendingUsers(String programId, String sectionId, int take, int skip,
                                OnResponseCallback<List<ProgramUser>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetPendingUsersTask(context, programId, sectionId, take, skip) {
                @Override
                protected void onSuccess(List<ProgramUser> programUsers) throws Exception {
                    super.onSuccess(programUsers);
                    if (programUsers == null || programUsers.isEmpty()) {
                        callback.onFailure(new TaException("No users are available with units pending for approval"));
                        return;
                    }

                    callback.onSuccess(programUsers);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void getPendingUnits(String programId, String sectionId, String username, int take, int skip,
                                OnResponseCallback<List<Unit>> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetPendingUnitsTask(context, programId, sectionId, username, take, skip) {
                @Override
                protected void onSuccess(List<Unit> units) throws Exception {
                    super.onSuccess(units);
                    if (units == null || units.isEmpty()) {
                        callback.onFailure(new TaException("No units are pending for approval"));
                        return;
                    }

                    callback.onSuccess(units);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void setSpecificSession(String role, String username, String url, String instructor_cookie, OnResponseCallback<SuccessResponse> callback) {
        if (NetworkUtil.isConnected(context)) {

            new SetSpecificSessionTask(context, role, username, url, instructor_cookie) {
                @Override
                protected void onSuccess(SuccessResponse response) throws Exception {
                    super.onSuccess(response);

                    callback.onSuccess(response);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }
    }

    public void createPeriod(String programId, String sectionId, String lang,
                             OnResponseCallback<SuccessResponse> callback) {

        if (NetworkUtil.isConnected(context)) {
            new CreatePeriodTask(context, programId, sectionId, lang) {
                @Override
                protected void onSuccess(SuccessResponse successResponse) throws Exception {
                    super.onSuccess(successResponse);
                    if (successResponse == null) {
                        callback.onFailure(new TaException("Unable to create periods"));
                        return;
                    }

                    callback.onSuccess(successResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void savePeriod(long periodId, List<String> addedIds, List<String> removedIds,
                           Map<String, Long> proposedDateModified, Map<String, Long> proposedDateAdded,
                           OnResponseCallback<SuccessResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new SavePeriodTask(context, periodId, addedIds, removedIds, proposedDateModified, proposedDateAdded) {
                @Override
                protected void onSuccess(SuccessResponse successResponse) throws Exception {
                    super.onSuccess(successResponse);
                    if (successResponse == null) {
                        callback.onFailure(new TaException("Unable to save period"));
                        return;
                    }

                    callback.onSuccess(successResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void approveUnit(String unitId, String username, String remarks, int rating, OnResponseCallback<SuccessResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new ApproveUnitTask(context, unitId, username, remarks, rating) {
                @Override
                protected void onSuccess(SuccessResponse successResponse) throws Exception {
                    super.onSuccess(successResponse);
                    if (successResponse == null) {
                        callback.onFailure(new TaException("Unable to approve unit"));
                        return;
                    }

                    callback.onSuccess(successResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void rejectUnit(String unitId, String username, String remarks, int rating, OnResponseCallback<SuccessResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new RejectUnitTask(context, unitId, username, remarks, rating) {
                @Override
                protected void onSuccess(SuccessResponse successResponse) throws Exception {
                    super.onSuccess(successResponse);
                    if (successResponse == null) {
                        callback.onFailure(new TaException("Unable to reject unit"));
                        return;
                    }

                    callback.onSuccess(successResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void getUpdatedVersion(OnResponseCallback<UpdateResponse> callback,
                                  String version_name, Long version_code) {

        if (NetworkUtil.isConnected(context)) {
            new GetVersionUpdatedTask(context, version_name, version_code) {
                @Override
                protected void onSuccess(UpdateResponse versionResponse) throws Exception {
                    super.onSuccess(versionResponse);
                    callback.onSuccess(versionResponse);
                }

                @Override
                protected void onException(Exception ex) {
                    Bundle parameters = new Bundle();
                    parameters.putString(Constants.KEY_CLASS_NAME, DataManager.class.getName());
                    parameters.putString(Constants.KEY_FUNCTION_NAME, "getUpdatedVersion");
                    parameters.putString(Constants.KEY_DATA, "version_name = " + version_name +
                            ", version_code = " + version_code);
//                    Logger.logCrashlytics(ex, parameters);
                    callback.onFailure(ex);
                }
            }.execute();
        }
    }


    public boolean checkUpdate() {
        boolean isupdate = false;
        Date lastSeenDate, current_date;
        current_date = Calendar.getInstance().getTime();

        String lastUpdatedDate_str = getAppPref().getUpdateSeenDate();
        if (lastUpdatedDate_str == null || lastUpdatedDate_str.isEmpty())
            return true;

        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        try {
            lastSeenDate = format.parse(lastUpdatedDate_str);

            if (lastSeenDate == null)
                return true;

            long different = lastSeenDate.getTime() - current_date.getTime();
            long elapsedDays;
            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;

            elapsedDays = different / daysInMilli;

            if (elapsedDays >= 1) {
                isupdate = true;
            } else {
                isupdate = false;
            }
        } catch (ParseException e) {
            Bundle parameters = new Bundle();
            parameters.putString(Constants.KEY_CLASS_NAME, DataManager.class.getName());
            parameters.putString(Constants.KEY_FUNCTION_NAME, "checkUpdate");
//            Logger.logCrashlytics(e, parameters);
            e.printStackTrace();
        }

        return isupdate;
    }

    // MX Ankit : To get version code and version name

    public Long getCurrent_vCode() {
        long v_code = 0L;
        PackageManager pm = context.getPackageManager();
        PackageInfo info;

        try {
            info = pm.getPackageInfo(context.getPackageName(), 0);
            v_code = (long) info.versionCode;

        } catch (PackageManager.NameNotFoundException e1) {
            Bundle parameters = new Bundle();
            parameters.putString(Constants.KEY_CLASS_NAME, DataManager.class.getName());
            parameters.putString(Constants.KEY_FUNCTION_NAME, "getCurrent_vCode");
//            Logger.logCrashlytics(e1, parameters);
            e1.printStackTrace();
        }

        return v_code;
    }

    public String getCurrentV_name() {
        String v_name = "";
        PackageManager pm = context.getPackageManager();
        PackageInfo info;

        try {
            info = pm.getPackageInfo(context.getPackageName(), 0);
            v_name = info.versionName;

        } catch (PackageManager.NameNotFoundException e1) {
            Bundle parameters = new Bundle();
            parameters.putString(Constants.KEY_CLASS_NAME, DataManager.class.getName());
            parameters.putString(Constants.KEY_FUNCTION_NAME, "getCurrentV_name");
//            Logger.logCrashlytics(e1, parameters);
            e1.printStackTrace();
        }

        return v_name;
    }

    public void getBlockComponent(String blockId, String courseId, OnResponseCallback<CourseComponent> callback) {

        if (NetworkUtil.isConnected(context)) {

            new GetBlockComponentFromServerTask(context, blockId, courseId) {
                @Override
                protected void onSuccess(CourseComponent courseComponent) throws Exception {
                    super.onSuccess(courseComponent);
                    if (courseComponent != null) {
                        courseManager.addBlockComponentInAppLevelCache(courseComponent, courseId);
                        callback.onSuccess(courseComponent);
                    } else {
                        getBlockComponentFromLocal(blockId, courseId, callback, new TaException("Unit not found"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    getBlockComponentFromLocal(blockId, courseId, callback,
                            new TaException("This unit will be available in few minutes"));
                }
            }.execute();

        } else {
            getBlockComponentFromLocal(blockId, courseId, callback, new NoConnectionException(context));
        }

    }

    private void getBlockComponentFromLocal(String blockId, String courseId,
                                            OnResponseCallback<CourseComponent> callback, Exception e) {

        CourseComponent component = courseManager.getBlockComponent(blockId, courseId);
        if (component != null) {
            callback.onSuccess(component);
            return;
        }

        new GetBlockComponentFromCacheTask(context, blockId, courseId) {
            @Override
            protected void onSuccess(CourseComponent courseComponent) throws Exception {
                super.onSuccess(courseComponent);
                if (courseComponent != null) {
                    callback.onSuccess(courseComponent);
                } else {
                    callback.onFailure(e);
                }
            }

            @Override
            protected void onException(Exception ex) {
                callback.onFailure(e);
            }
        }.execute();

    }

    public void enrolInCourse(String courseId, OnResponseCallback<ResponseBody> callback) {
        if (!NetworkUtil.isConnected(context)) {
            callback.onFailure(new TaException(context.getString(R.string.no_connection_exception)));
            return;
        }
        Call<ResponseBody> enrolCall = courseApi.enrolInCourse(courseId);
        enrolCall.enqueue(new CourseService.EnrollCallback(context) {
            @Override
            protected void onResponse(@NonNull ResponseBody responseBody) {
                super.onResponse(responseBody);
                callback.onSuccess(responseBody);
            }

            @Override
            protected void onFailure(@NonNull Throwable error) {
                super.onFailure(error);
                callback.onFailure(new TaException(error.getMessage()));

            }
        });
    }

    public void setProposedDate(String programId, String sectionId, long proposedDate, long periodId,
                                String unitId, OnResponseCallback<SuccessResponse> callback) {

        if (NetworkUtil.isConnected(context)) {

            new SetProposedDateTask(context, programId, sectionId, periodId, proposedDate, unitId) {
                @Override
                protected void onSuccess(SuccessResponse response) throws Exception {
                    super.onSuccess(response);
                    if (response != null && response.getSuccess()) {
                        callback.onSuccess(response);
                    } else {
                        callback.onFailure(new TaException("Error occurred. Unable to set proposed date"));
                    }
                }

                @Override
                protected void onException(Exception ex) {
                    callback.onFailure(ex);
                }
            }.execute();

        } else {
            callback.onFailure(new NoConnectionException(context));
        }

    }

    public void updateSelectedFilters(SelectedFilter selectedFilter) {
        List<SelectedFilter> sel_cachefilter = getSelectedFilters();

        if (sel_cachefilter == null) {
            sel_cachefilter = new ArrayList<>();
            sel_cachefilter.add(selectedFilter);
        } else {
            int index = 0;
            boolean isExist = false;
            for (SelectedFilter filter : sel_cachefilter) {
                if (filter.getInternal_name().equalsIgnoreCase(selectedFilter.getInternal_name())) {
                    sel_cachefilter.set(index, selectedFilter);
                    isExist = true;
                    break;
                }
                index++;
            }
            if (!isExist) {
                sel_cachefilter.add(selectedFilter);
            }
            loginPrefs.setCachedFilter(sel_cachefilter);
        }
    }


    public List<SelectedFilter> getSelectedFilters() {
        List<SelectedFilter> sel_filter = new ArrayList<>();
        if (loginPrefs.getCachedFilter() != null)
            sel_filter = loginPrefs.getCachedFilter();

        return sel_filter;
    }


//    public void downloadFile(Context context, String url, String title, long id) {
//        Uri Download_Uri = Uri.parse(url);
//        DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
//
//        //Restrict the types of networks over which this download may proceed.
//        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
//        //Set whether this download may proceed over a roaming connection.
//        request.setAllowedOverRoaming(false);
//        //Set the title of this download, to be displayed in notifications (if enabled).
//        request.setTitle("Downloading");
//        //Set a description of this download, to be displayed in notifications (if enabled)
//        request.setDescription("Downloading File");
//        //Set the local destination for the downloaded file to a path within the application's external files directory
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title + "_" + id);
//        request.allowScanningByMediaScanner();
//
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//        //Enqueue a new download and same the referenceId
//        ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
//    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public long downloadFile(Uri uri) {

        long downloadReference;
        DownloadManager downloadManager;

        // Create request for android download manager
        downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        //Setting title of request
        request.setTitle("Data Download");

        //Setting description of request
        request.setDescription("Android Data download using DownloadManager.");

        //Set the local destination for the downloaded file to a path
        //within the application's external files directory
        request.setDestinationInExternalFilesDir(context,
                Environment.DIRECTORY_DOWNLOADS, "AndroidTutorialPoint.pdf");


        //Enqueue download and save into referenceId
        downloadReference = downloadManager.enqueue(request);
        return downloadReference;
    }


    public void downloadStatus(Cursor cursor, long DownloadId) {

        //column for download  status
        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = cursor.getInt(columnIndex);
        //column for reason code if the download failed or paused
        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int reason = cursor.getInt(columnReason);
        //get the download filename
        int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
        String filename = cursor.getString(filenameIndex);

        String statusText = "";
        String reasonText = "";

        switch (status) {
            case DownloadManager.STATUS_FAILED:
                statusText = "STATUS_FAILED";
                Log.d("STATUS_FAILED",statusText);
                switch (reason) {
                    case DownloadManager.ERROR_CANNOT_RESUME:
                        reasonText = "ERROR_CANNOT_RESUME";
                        break;
                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                        reasonText = "ERROR_DEVICE_NOT_FOUND";
                        break;
                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                        reasonText = "ERROR_FILE_ALREADY_EXISTS";
                        break;
                    case DownloadManager.ERROR_FILE_ERROR:
                        reasonText = "ERROR_FILE_ERROR";
                        break;
                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                        reasonText = "ERROR_HTTP_DATA_ERROR";
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        reasonText = "ERROR_INSUFFICIENT_SPACE";
                        break;
                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                        reasonText = "ERROR_TOO_MANY_REDIRECTS";
                        break;
                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                        reasonText = "ERROR_UNHANDLED_HTTP_CODE";
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                        reasonText = "ERROR_UNKNOWN";
                        break;
                }
                break;
            case DownloadManager.STATUS_PAUSED:
                statusText = "STATUS_PAUSED";
                Log.d("STATUS_PAUSED",statusText);
                switch (reason) {
                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                        reasonText = "PAUSED_QUEUED_FOR_WIFI";
                        break;
                    case DownloadManager.PAUSED_UNKNOWN:
                        reasonText = "PAUSED_UNKNOWN";
                        break;
                    case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                        reasonText = "PAUSED_WAITING_FOR_NETWORK";
                        break;
                    case DownloadManager.PAUSED_WAITING_TO_RETRY:
                        reasonText = "PAUSED_WAITING_TO_RETRY";
                        break;
                }
                break;
            case DownloadManager.STATUS_PENDING:
                statusText = "STATUS_PENDING";
                Log.d("STATUS_PENDING",statusText);
                break;
            case DownloadManager.STATUS_RUNNING:
                statusText = "STATUS_RUNNING";
                Log.d("STATUS_RUNNING",statusText);

                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                statusText = "STATUS_SUCCESSFUL";
                Log.d("STATUS_SUCCESSFUL",statusText);

                reasonText = "Filename:\n" + filename;
                break;
        }

    }
}
