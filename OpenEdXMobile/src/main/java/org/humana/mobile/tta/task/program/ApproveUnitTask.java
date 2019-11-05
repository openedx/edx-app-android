package org.humana.mobile.tta.task.program;

import android.content.Context;

import com.google.inject.Inject;

import org.humana.mobile.task.Task;
import org.humana.mobile.tta.data.model.SuccessResponse;
import org.humana.mobile.tta.data.remote.api.TaAPI;

public class ApproveUnitTask extends Task<SuccessResponse> {

    private String unitId, username;

    @Inject
    private TaAPI taAPI;

    public ApproveUnitTask(Context context, String unitId, String username) {
        super(context);
        this.unitId = unitId;
        this.username = username;
    }

    @Override
    public SuccessResponse call() throws Exception {
        return taAPI.approveUnit(unitId, username).execute().body();
    }
}