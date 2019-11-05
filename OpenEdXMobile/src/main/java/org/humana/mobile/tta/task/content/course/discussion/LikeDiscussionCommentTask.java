package org.humana.mobile.tta.task.content.course.discussion;

import android.content.Context;

import com.google.inject.Inject;

import org.humana.mobile.discussion.DiscussionComment;
import org.humana.mobile.task.Task;
import org.humana.mobile.tta.data.remote.api.DiscussionApi;

public class LikeDiscussionCommentTask extends Task<DiscussionComment> {

    private String commentId;
    private boolean liked;

    @Inject
    private DiscussionApi discussionApi;

    public LikeDiscussionCommentTask(Context context, String commentId, boolean liked) {
        super(context);
        this.commentId = commentId;
        this.liked = liked;
    }

    @Override
    public DiscussionComment call() throws Exception {
        return discussionApi.likeComment(commentId, liked).execute().body();
    }
}