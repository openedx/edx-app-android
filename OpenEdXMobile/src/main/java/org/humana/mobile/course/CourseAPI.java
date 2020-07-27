package org.humana.mobile.course;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.humana.mobile.exception.CourseContentNotValidException;
import org.humana.mobile.http.callback.ErrorHandlingCallback;
import org.humana.mobile.http.notifications.ErrorNotification;
import org.humana.mobile.http.notifications.SnackbarErrorNotification;
import org.humana.mobile.interfaces.RefreshListener;
import org.humana.mobile.interfaces.SectionItemInterface;
import org.humana.mobile.model.Filter;
import org.humana.mobile.model.Page;
import org.humana.mobile.model.api.ChapterModel;
import org.humana.mobile.model.api.EnrolledCoursesResponse;
import org.humana.mobile.model.api.IPathNode;
import org.humana.mobile.model.api.LectureModel;
import org.humana.mobile.model.api.ProfileModel;
import org.humana.mobile.model.api.SectionEntry;
import org.humana.mobile.model.api.SectionItemModel;
import org.humana.mobile.model.api.SummaryModel;
import org.humana.mobile.model.api.SyncLastAccessedSubsectionResponse;
import org.humana.mobile.model.api.TranscriptModel;
import org.humana.mobile.model.api.VideoResponseModel;
import org.humana.mobile.model.course.BlockModel;
import org.humana.mobile.model.course.BlockType;
import org.humana.mobile.model.course.CourseComponent;
import org.humana.mobile.model.course.CourseStructureV1Model;
import org.humana.mobile.model.course.DiscussionBlockModel;
import org.humana.mobile.model.course.DiscussionData;
import org.humana.mobile.model.course.HasDownloadEntry;
import org.humana.mobile.model.course.HtmlBlockModel;
import org.humana.mobile.model.course.IBlock;
import org.humana.mobile.model.course.VideoBlockModel;
import org.humana.mobile.model.course.VideoData;
import org.humana.mobile.model.course.VideoInfo;
import org.humana.mobile.module.prefs.UserPrefs;
import org.humana.mobile.tta.scorm.PDFBlockModel;
import org.humana.mobile.tta.scorm.ScormBlockModel;
import org.humana.mobile.tta.scorm.ScormData;
import org.humana.mobile.tta.scorm.ScormManager;
import org.humana.mobile.util.Config;
import org.humana.mobile.view.common.TaskProgressCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;

import static org.humana.mobile.http.constants.TimeInterval.HOUR;
import static org.humana.mobile.http.util.CallUtil.executeStrict;

@Singleton
public class CourseAPI {

    @Inject
    protected Config config;

    @NonNull
    private final CourseService courseService;
    @NonNull
    private final UserPrefs userPrefs;

    @Inject
    @NonNull
    public static ScormManager scormManager;


    @Inject
    public CourseAPI(@NonNull CourseService courseService, @NonNull UserPrefs userPrefs) {
        this.courseService = courseService;
        this.userPrefs = userPrefs;
    }

    @NonNull
    public Call<Page<CourseDetail>> getCourseList(final int page) {
        return courseService.getCourseList(getUsername(), true, config.getOrganizationCode(), page);
    }

    @NonNull
    public Call<CourseDetail> getCourseDetail(@NonNull final String courseId) {
        // Empty courseId will return a 200 for a list of course details, instead of a single course
        if (TextUtils.isEmpty(courseId)) throw new IllegalArgumentException();
        return courseService.getCourseDetail(courseId, getUsername());
    }

    @NonNull
    public Call<ResponseBody> enrolInCourse(String courseId){
        return courseService.enrollInACourse(new CourseService.EnrollBody(courseId, false));
    }

    /**
     * @return Enrolled courses of given user.
     */
    @NonNull
    public Call<List<EnrolledCoursesResponse>> getEnrolledCourses() {
        return courseService.getEnrolledCourses(getUsername(), config.getOrganizationCode());
    }

    /**
     * @return Enrolled courses of given user, only from the cache.
     */
    @NonNull
    public Call<List<EnrolledCoursesResponse>> getEnrolledCoursesFromCache() {
        return courseService.getEnrolledCoursesFromCache(
                getUsername(), config.getOrganizationCode());
    }

    /**
     * @param courseId The course ID.
     * @return The course identified by the provided ID if available from the cache, null if no
     *         course is found.
     */
    @Nullable
    public EnrolledCoursesResponse getCourseById(@NonNull final String courseId) throws Exception {
        for (EnrolledCoursesResponse r : executeStrict(getEnrolledCoursesFromCache())) {
            if (r.getCourse().getId().equals(courseId)) {
                return r;
            }
        }
        return null;
    }

    public static abstract class GetCourseByIdCallback extends
            ErrorHandlingCallback<List<EnrolledCoursesResponse>> {
        @NonNull
        private final String courseId;

        public GetCourseByIdCallback(@NonNull final Context context,
                                     @NonNull final String courseId) {
            super(context);
            this.courseId = courseId;
        }

        public GetCourseByIdCallback(@NonNull final Context context,
                                     @NonNull final String courseId,
                                     @Nullable final TaskProgressCallback progressCallback) {
            super(context, progressCallback);
            this.courseId = courseId;
        }

        @Override
        protected final void onResponse(
                @NonNull final List<EnrolledCoursesResponse> courseResponses) {
            for (EnrolledCoursesResponse coursesResponse : courseResponses) {
                if (coursesResponse.getCourse().getId().equals(courseId)) {
                    onResponse(coursesResponse);
                    return;
                }
            }
            onFailure(new Exception("Course not found in user's enrolled courses."));
        }

        protected abstract void onResponse(@NonNull final EnrolledCoursesResponse coursesResponse);
    }

    @NonNull
    public Call<SyncLastAccessedSubsectionResponse> syncLastAccessedSubsection(
            @NonNull final String courseId,
            @NonNull final String lastVisitedModuleId) {
        return courseService.syncLastAccessedSubsection(getUsername(), courseId,
                new CourseService.SyncLastAccessedSubsectionBody(lastVisitedModuleId));
    }

    @NonNull
    public Call<SyncLastAccessedSubsectionResponse> getLastAccessedSubsection(
            @NonNull final String courseId) {
        return courseService.getLastAccessedSubsection(getUsername(), courseId);
    }

    @NonNull
    public Call<CourseStructureV1Model> getCourseStructureWithoutStale(@NonNull final String courseId) {
        return courseService.getCourseStructure(null, getUsername(), courseId);
    }

    @NonNull
    public Call<CourseStructureV1Model> getCourseStructure(@NonNull final String courseId) {
        return courseService.getCourseStructure("max-stale=" + HOUR, getUsername(), courseId);
    }

    @NonNull
    public CourseComponent getCourseStructureFromCache(@NonNull final String courseId)
            throws Exception {
        CourseStructureV1Model model = executeStrict(
                courseService.getCourseStructure("only-if-cached, max-stale", getUsername(), courseId));
        return (CourseComponent) normalizeCourseStructure(model, courseId);
    }

    @NonNull
    public Call<CourseStructureV1Model> getBlockComponentWithoutStale(@NonNull final String blockId, @NonNull final String courseId) {
        return courseService.getBlockComponent(blockId, null, getUsername(), courseId);
    }

    @NonNull
    public Call<CourseStructureV1Model> getBlockComponent(@NonNull final String blockId, @NonNull final String courseId) {
        return courseService.getBlockComponent(blockId, "max-stale=" + HOUR, getUsername(), courseId);
    }

    @NonNull
    public CourseComponent getBlockComponentFromCache(@NonNull final String blockId, @NonNull final String courseId)
            throws Exception {
        CourseStructureV1Model model = executeStrict(
                courseService.getBlockComponent(blockId, "only-if-cached, max-stale", getUsername(), courseId));
        return (CourseComponent) normalizeCourseStructure(model, courseId);
    }

    public String downloadScorm(String url, String file) throws Exception {
//        return api.downloadScorm(url,file);
        return null;
    }

    public static abstract class GetCourseStructureCallback
            extends ErrorHandlingCallback<CourseStructureV1Model> {
        @NonNull
        private final String courseId;

        public GetCourseStructureCallback(@NonNull final Context context,
                                          @NonNull final String courseId,
                                          @Nullable final TaskProgressCallback progressCallback) {
            super(context, progressCallback);
            this.courseId = courseId;
        }

        public GetCourseStructureCallback(@NonNull final Context context,
                                          @NonNull final String courseId,
                                          @Nullable final TaskProgressCallback progressCallback,
                                          @Nullable final ErrorNotification errorNotification,
                                          @Nullable final SnackbarErrorNotification snackbarErrorNotification,
                                          @Nullable final RefreshListener refreshListener) {
            super(context, progressCallback, errorNotification, snackbarErrorNotification, refreshListener);
            this.courseId = courseId;
        }

        @Override
        protected final void onResponse(@NonNull final CourseStructureV1Model model) {
            try {
                onResponse((CourseComponent) normalizeCourseStructure(model, courseId));
            } catch (CourseContentNotValidException e) {
                onFailure(e);
            }
        }

        protected abstract void onResponse(@NonNull final CourseComponent courseComponent);
    }

    @NonNull
    public List<SectionItemInterface> getLiveOrganizedVideosByChapter(
            @NonNull final String courseId, @NonNull final String chapter) throws Exception {
        CourseComponent course = this.getCourseStructureFromCache(courseId);
        if (course != null) {
            return mappingAllVideoResponseModelFrom(course, new Filter<VideoResponseModel>() {
                @Override
                public boolean apply(VideoResponseModel videoResponseModel) {
                    return videoResponseModel != null && videoResponseModel.getChapterName().equals(chapter);
                }
            });
        }

        List<VideoResponseModel> videos = executeStrict(courseService.getVideosByCourseId(courseId));

        ArrayList<SectionItemInterface> list = new ArrayList<SectionItemInterface>();

        // add chapter to the result
        ChapterModel c = new ChapterModel();
        c.name = chapter;
        list.add(c);

        HashMap<String, ArrayList<VideoResponseModel>> sections =
                new LinkedHashMap<String, ArrayList<VideoResponseModel>>();

        for (VideoResponseModel v : videos) {
            // filter videos by chapter
            if (v.getChapter().getDisplayName().equals(chapter)) {
                // this video is under the specified chapter

                // sort out the section of this video
                if (sections.containsKey(v.getSection().getDisplayName())) {
                    ArrayList<VideoResponseModel> sv = sections.get(v.getSection().getDisplayName());
                    if (sv == null) {
                        sv = new ArrayList<VideoResponseModel>();
                    }
                    sv.add(v);
                } else {
                    ArrayList<VideoResponseModel> vlist = new ArrayList<VideoResponseModel>();
                    vlist.add(v);
                    sections.put(v.getSection().getDisplayName(), vlist);
                }
            }
        }

        // now add sectioned videos to the result
        for (Map.Entry<String, ArrayList<VideoResponseModel>> entry : sections.entrySet()) {
            // add section to the result
            SectionItemModel s = new SectionItemModel();
            s.name = entry.getKey();
            list.add(s);

            // add videos to the result
            if (entry.getValue() != null) {
                for (VideoResponseModel v : entry.getValue()) {
                    list.add(v);
                }
            }
        }

        return list;
    }

    @NonNull
    public Map<String, SectionEntry> getCourseHierarchy(@NonNull final String courseId)
            throws Exception {
        CourseComponent course = this.getCourseStructureFromCache(courseId);
        if (course != null) {
            return mappingCourseHierarchyFrom(course);
        }

        List<VideoResponseModel> list = executeStrict(courseService.getVideosByCourseId(courseId));

        // create hierarchy with chapters, sections and subsections
        // HashMap<String, SectionEntry> chapterMap = new HashMap<String, SectionEntry>();
        Map<String, SectionEntry> chapterMap = new LinkedHashMap<String, SectionEntry>();
        for (VideoResponseModel m : list) {
            // add each video to its corresponding chapter and section

            // add this as a chapter
            String cname = m.getChapter().getDisplayName();

            // carry this courseId with video model
            m.setCourseId(courseId);

            SectionEntry s = null;
            if (chapterMap.containsKey(cname)) {
                s = chapterMap.get(cname);
            } else {
                s = new SectionEntry();
                s.chapter = cname;
                s.isChapter = true;
                s.section_url = m.getSectionUrl();
                chapterMap.put(cname, s);
            }

            // add this video to section inside in this chapter
            ArrayList<VideoResponseModel> videos = s.sections.get(m.getSection().getDisplayName());
            if (videos == null) {
                s.sections.put(m.getSection().getDisplayName(),
                        new ArrayList<VideoResponseModel>());
                videos = s.sections.get(m.getSection().getDisplayName());
            }

            videos.add(m);
        }

        return chapterMap;
    }

    @Nullable
    public VideoResponseModel getVideoById(@NonNull final String courseId,
                                           @NonNull final String videoId)
            throws Exception {
        CourseComponent course = this.getCourseStructureFromCache(courseId);
        if (course == null) {
            return getVideoById(course, videoId);
        }

        Map<String, SectionEntry> map = getCourseHierarchy(courseId);

        // iterate chapters
        for (Map.Entry<String, SectionEntry> chapterentry : map.entrySet()) {
            // iterate lectures
            for (Map.Entry<String, ArrayList<VideoResponseModel>> entry :
                    chapterentry.getValue().sections.entrySet()) {
                // iterate videos
                for (VideoResponseModel v : entry.getValue()) {

                    // identify the video
                    if (videoId.equals(v.getSummary().getId())) {
                        return v;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public TranscriptModel getTranscriptsOfVideo(@NonNull final String enrollmentId,
                                                 @NonNull final String videoId)
            throws Exception {
        TranscriptModel transcript;
        VideoResponseModel vidModel = getVideoById(enrollmentId, videoId);
        if (vidModel != null) {
            if (vidModel.getSummary() != null) {
                transcript = vidModel.getSummary().getTranscripts();
                return transcript;
            }
        }
        return null;
    }

    /**
     * we handle both name and id for backward compatibility. legacy code use name, it is not a good idea as name is not
     * grantee to be unique.
     */
    @Nullable
    public LectureModel getLecture(@NonNull final CourseComponent courseComponent,
                                   @NonNull final String chapterName,
                                   @NonNull final String chapterId,
                                   @NonNull final String lectureName,
                                   @NonNull final String lectureId)
            throws Exception {
        //TODO - we may use a generic filter to fetch the data?
        for(IBlock chapter : courseComponent.getChildren()){
            if ( chapter.getId().equals(chapterId) ){
                for(IBlock lecture : chapter.getChildren() ){
                    //TODO - check to see if need to compare id or not
                    if ( lecture.getId().equals(lectureId) ){
                        LectureModel lm = new LectureModel();
                        lm.name = lecture.getDisplayName();
                        lm.videos = (ArrayList) mappingAllVideoResponseModelFrom((CourseComponent)lecture, null );
                        return lm;
                    }
                }
            }
        }
        //if we can not find object by id, try to get by name.
        for(IBlock chapter : courseComponent.getChildren()){
            if ( chapter.getDisplayName().equals(chapterName) ){
                for(IBlock lecture : chapter.getChildren() ){
                    //TODO - check to see if need to compare id or not
                    if ( lecture.getDisplayName().equals(lectureName) ){
                        LectureModel lm = new LectureModel();
                        lm.name = lecture.getDisplayName();
                        lm.videos = (ArrayList) mappingAllVideoResponseModelFrom((CourseComponent)lecture, null );
                        return lm;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public VideoResponseModel getVideoById(@NonNull final CourseComponent courseComponent,
                                           @NonNull final String videoId)
            throws Exception {
        for(HasDownloadEntry item : courseComponent.getVideos()) {
            VideoBlockModel model = (VideoBlockModel)item;
            if (model.getId().equals(videoId))
                return mappingVideoResponseModelFrom((VideoBlockModel) item);
        }
        return null;
    }

    /**
     *
     * @param courseComponent
     * @param subsectionId
     * @return
     */
    @Nullable
    public VideoResponseModel getSubsectionById(@NonNull final CourseComponent courseComponent,
                                                @NonNull final String subsectionId) {
        ////TODO - we may use a generic filter to fetch the data?
        Map<String, SectionEntry> map = mappingCourseHierarchyFrom(courseComponent);
        for (Map.Entry<String, SectionEntry> chapterentry : map.entrySet()) {
            // iterate lectures
            for (Map.Entry<String, ArrayList<VideoResponseModel>> entry :
                    chapterentry.getValue().sections.entrySet()) {
                // iterate videos
                for (VideoResponseModel v : entry.getValue()) {
                    // identify the subsection (module) if id matches
                    IPathNode node = v.getSection();
                    if (node != null  && subsectionId.equals(node.getId())) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private String getUsername() {
        final ProfileModel profile = userPrefs.getProfile();
        return null == profile ? null : profile.username;
    }

    /**
     * Mapping from raw data structure from getCourseStructure() API
     * @param courseStructureV1Model
     * @return
     */
    @NonNull
    public static IBlock normalizeCourseStructure(
            @NonNull final CourseStructureV1Model courseStructureV1Model,
            @NonNull final String courseId) throws CourseContentNotValidException {
        BlockModel topBlock = courseStructureV1Model.getBlockById(courseStructureV1Model.root);
        if (topBlock == null) {
            throw new CourseContentNotValidException("Server didn't send a proper response for this course: " + courseStructureV1Model.root);
        }
        CourseComponent course = new CourseComponent(topBlock, null);
        course.setCourseId(courseId);
        for (BlockModel m : courseStructureV1Model.getDescendants(topBlock)) {
            normalizeCourseStructure(courseStructureV1Model, m, course);
        }
        return course;
    }

    private static void normalizeCourseStructure(
            @NonNull final CourseStructureV1Model courseStructureV1Model,
            @NonNull final BlockModel block,
            @NonNull final CourseComponent parent) {

        if (block.isContainer()) {
            CourseComponent child = new CourseComponent(block, parent);
            for (BlockModel m : courseStructureV1Model.getDescendants(block)) {
                normalizeCourseStructure(courseStructureV1Model, m, child);
            }
        } else {
            if (BlockType.VIDEO == block.type && block.data instanceof VideoData) {
                new VideoBlockModel(block, parent);
            } else if (BlockType.DISCUSSION == block.type && block.data instanceof DiscussionData) {
                new DiscussionBlockModel(block, parent);
            } else if (BlockType.SCORM == block.type && block.data instanceof ScormData) {
                new ScormBlockModel(block, parent);
            }
            //added by Arjun to integrate pdf xblock in android.
            else if (BlockType.PDF == block.type)//block.data instanceof ScormData
            {
                new PDFBlockModel(block, parent);
            } else { //everything else.. we fallback to html component
                new HtmlBlockModel(block, parent);
            }
        }
    }

    /**
     * we map the new course outline data to old data model.
     * TODO : Ideally we should update all the code to match the new data model.
     * @param courseComponent
     * @return
     */
    @NonNull
    private static List<SectionItemInterface> mappingAllVideoResponseModelFrom(
            @NonNull final CourseComponent courseComponent,
            @NonNull final Filter<VideoResponseModel> filter) {
        List<SectionItemInterface> items = new ArrayList<>();
        for(HasDownloadEntry item : courseComponent.getVideos()){
            VideoResponseModel model = mappingVideoResponseModelFrom((VideoBlockModel)item);
            if ( filter == null )
                items.add( model );
            else {
                if (filter.apply(model)){
                    items.add( model );
                }
            }
        }

        //Arjun: for binding scorm entries if they exist on android storage add them too.

        for(ScormBlockModel scromItem : courseComponent.getScorms()) {
            if (scormManager.has(scromItem.getId())) {
                VideoResponseModel model = mappingVideoResponseModelFrom(scromItem);

                if (filter == null)
                    items.add(model);
                else {
                    if (filter.apply(model)) {
                        items.add(model);
                    }
                }
            }
        }

        //Arjun: for binding PDF entries if they exist on android storage add them too.

        for(PDFBlockModel pdfItem : courseComponent.getPDFs()) {
            if (scormManager.has(pdfItem.getId())) {
                VideoResponseModel model = mappingVideoResponseModelFrom(pdfItem);

                if (filter == null)
                    items.add(model);
                else {
                    if (filter.apply(model)) {
                        items.add(model);
                    }
                }
            }
        }

        return items;
    }

    /**
     * from new VideoBlockModel to legacy VideoRsponseModel
     * @param videoBlockModel
     * @return
     */
    @NonNull
    private static VideoResponseModel mappingVideoResponseModelFrom(
            @NonNull final VideoBlockModel videoBlockModel) {
        VideoResponseModel model = new VideoResponseModel();
        model.setCourseId(videoBlockModel.getCourseId());
        SummaryModel summaryModel = mappingSummaryModelFrom(videoBlockModel);
        model.setSummary(summaryModel);
        model.videoBlockModel = videoBlockModel;
        model.setSectionUrl(videoBlockModel.getParent().getBlockUrl());
        model.setUnitUrl(videoBlockModel.getBlockUrl());

        return model;
    }

    @NonNull
    private static SummaryModel mappingSummaryModelFrom(
            @NonNull final VideoBlockModel videoBlockModel) {
        SummaryModel model = new SummaryModel();
        model.setType(videoBlockModel.getType());
        model.setDisplayName(videoBlockModel.getDisplayName());
        model.setDuration((int)videoBlockModel.getData().duration);
        model.setOnlyOnWeb(videoBlockModel.getData().onlyOnWeb);
        model.setId(videoBlockModel.getId());
        final VideoInfo videoInfo = videoBlockModel.getData().encodedVideos.getPreferredVideoInfo();
        if (null != videoInfo) {
            model.setVideoUrl(videoInfo.url);
            model.setSize(videoInfo.fileSize);
        }
        model.setTranscripts(videoBlockModel.getData().transcripts);
        //FIXME = is this field missing?
        // private EncodingsModel encodings;
        return model;
    }

    /**
     * from new CourseComponent to legacy data structure.
     * @param courseComponent
     * @return
     */
    @NonNull
    private static Map<String, SectionEntry> mappingCourseHierarchyFrom(
            @NonNull final CourseComponent courseComponent) {
        Map<String, SectionEntry> map = new HashMap<>();
        for(IBlock block : courseComponent.getChildren()){
            CourseComponent chapter = (CourseComponent)block;
            SectionEntry entry = new SectionEntry();
            entry.chapter = chapter.getDisplayName();
            entry.isChapter = true;
            entry.section_url = chapter.getBlockUrl();
            map.put(entry.chapter, entry);

            for( IBlock subBlock : chapter.getChildren() ){
                CourseComponent section = (CourseComponent)subBlock;

                entry.sections.put(section.getDisplayName(),
                        (ArrayList) mappingAllVideoResponseModelFrom(section, null));
            }
        }
        return map;
    }

    //for scrom:Arjun
    public static VideoResponseModel mappingVideoResponseModelFrom(ScormBlockModel scormBlockModel){
        VideoResponseModel model = new VideoResponseModel();
        model.setCourseId(scormBlockModel.getCourseId());
        SummaryModel summaryModel = mappingSummaryModelFrom(scormBlockModel);
        model.setSummary(summaryModel);
        model.scormBlockModel = scormBlockModel;
        model.setSectionUrl(scormBlockModel.getParent().getBlockUrl());
        model.setUnitUrl(scormBlockModel.getBlockUrl());

        return model;
    }

    //for PDF Xblock:Arjun
    public static VideoResponseModel mappingVideoResponseModelFrom(PDFBlockModel pdfBlockModel){
        VideoResponseModel model = new VideoResponseModel();
        model.setCourseId(pdfBlockModel.getCourseId());
        SummaryModel summaryModel = mappingSummaryModelFrom(pdfBlockModel);
        model.setSummary(summaryModel);
        model.scormBlockModel = pdfBlockModel;
        model.setSectionUrl(pdfBlockModel.getParent().getBlockUrl());
        model.setUnitUrl(pdfBlockModel.getBlockUrl());

        return model;
    }

    //for scrom:Arjun
    private static SummaryModel mappingSummaryModelFrom(ScormBlockModel scormBlockModel){
        SummaryModel model = new SummaryModel();
        model.setType(scormBlockModel.getType());
        model.setDisplayName(scormBlockModel.getDisplayName());
        model.setDuration(0);
        model.setOnlyOnWeb(true);
        model.setId(scormBlockModel.getId());

        //because we don't have info for scrom
        final VideoInfo videoInfo = null;
        if (null != videoInfo) {
            model.setVideoUrl(videoInfo.url);
            model.setSize(videoInfo.fileSize);
        }
        model.setTranscripts(null);
        //FIXME = is this field missing?
        // private EncodingsModel encodings;
        return model;
    }

    /*public
    @NonNull
    List<EnrolledCoursesResponse> getUserEnrolledCourses(@NonNull String username, boolean tryCache,Context ctx) throws Exception {

        String json = null;
        List<EnrolledCoursesResponse> ret = null;
        final String cacheKey = getUserEnrolledCoursesURL(username);

        //if only cash required
        try {
            json = cache.get(cacheKey);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.debug(e.toString());
        }

        //if cash is empty get it from web
        if (json == null)
        {
            json=getFromUserEnrolledCoursesfromWeb(username,ctx);
        }

        // We aren't use TypeToken here because it throws NoClassDefFoundError
        final JsonArray ary = gson.fromJson(json, JsonArray.class);
        //final List<EnrolledCoursesResponse> ret = new ArrayList<>(ary.size());
        ret = new ArrayList<>(ary.size());
        for (int cnt = 0; cnt < ary.size(); ++cnt) {
            ret.add(gson.fromJson(ary.get(cnt), EnrolledCoursesResponse.class));
        }
        return ret;
    }*/
}