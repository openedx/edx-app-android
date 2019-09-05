package org.edx.mobile.tta.ui.programs.schedule.view_model;

import android.app.Dialog;
import android.content.Context;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.maurya.mx.mxlib.core.MxFiniteAdapter;
import com.maurya.mx.mxlib.core.MxInfiniteAdapter;
import com.maurya.mx.mxlib.core.OnRecyclerItemClickListener;

import org.edx.mobile.R;
import org.edx.mobile.databinding.TRowFilterDropDownBinding;
import org.edx.mobile.databinding.TRowScheduleBinding;
import org.edx.mobile.tta.Constants;
import org.edx.mobile.tta.data.local.db.table.Period;
import org.edx.mobile.tta.data.model.SuccessResponse;
import org.edx.mobile.tta.data.model.program.ProgramFilter;
import org.edx.mobile.tta.data.model.program.ProgramFilterTag;
import org.edx.mobile.tta.event.program.PeriodSavedEvent;
import org.edx.mobile.tta.interfaces.OnResponseCallback;
import org.edx.mobile.tta.tutorials.MxTooltip;
import org.edx.mobile.tta.ui.base.TaBaseFragment;
import org.edx.mobile.tta.ui.base.mvvm.BaseViewModel;
import org.edx.mobile.tta.ui.custom.DropDownFilterView;
import org.edx.mobile.tta.ui.programs.addunits.AddUnitsActivity;
import org.edx.mobile.tta.utils.ActivityUtil;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class ScheduleViewModel extends BaseViewModel {

    private static final int TAKE = 10;
    private static final int SKIP = 0;

    private List<ProgramFilter> allFilters;
    private List<ProgramFilter> filters;
    private List<Period> periodList;
    private List<ProgramFilterTag> tags;
    private List<DropDownFilterView.FilterItem> langTags;

    public ObservableBoolean filtersVisible = new ObservableBoolean();
    public ObservableBoolean emptyVisible = new ObservableBoolean();

    public ObservableInt initialPosition = new ObservableInt();
    public ObservableInt toolTipPosition = new ObservableInt();
    public ObservableInt toolTipGravity = new ObservableInt();
    public ObservableField<String> toolTiptext = new ObservableField<>();

    public FiltersAdapter filtersAdapter;
    public PeriodAdapter periodAdapter;
    public RecyclerView.LayoutManager gridLayoutManager;

    private boolean allLoaded;
    private boolean changesMade;
    private int take, skip;

    public MxInfiniteAdapter.OnLoadMoreListener loadMoreListener = page -> {
        if (allLoaded)
            return false;
        this.skip++;
        fetchData();
        return true;
    };
    private String lang;

    public ScheduleViewModel(Context context, TaBaseFragment fragment) {
        super(context, fragment);
        filters = new ArrayList<>();
        periodList = new ArrayList<>();
        tags = new ArrayList<>();

        filtersAdapter = new FiltersAdapter(mActivity);
        periodAdapter = new PeriodAdapter(mActivity);
        take = TAKE;
        skip = SKIP;
        allLoaded = false;
        changesMade = true;

        periodAdapter.setItems(periodList);
        periodAdapter.setItemClickListener((view, item) -> {
            Bundle parameters = new Bundle();
            parameters.putParcelable(Constants.KEY_PERIOD, item);
            ActivityUtil.gotoPage(mActivity, AddUnitsActivity.class, parameters);
        });

        mActivity.showLoading();
        getFilters();
        fetchData();
//        toolTiptext.set("test");
//        toolTipGravity.set(Gravity.TOP);
//        toolTipPosition.set(0);

    }

    private void fetchData() {

        if (changesMade) {
            changesMade = false;
            skip = 0;
            periodAdapter.reset(true);
            setFilters();
        }

        getPeriods();

    }

    private void setFilters() {
        filters.clear();
        if (tags.isEmpty() || allFilters == null || allFilters.isEmpty()) {
            return;
        }

        for (ProgramFilter filter : allFilters) {

            List<ProgramFilterTag> selectedTags = new ArrayList<>();
            for (ProgramFilterTag tag : filter.getTags()) {
                if (tags.contains(tag)) {
                    selectedTags.add(tag);
                }
            }

            if (!selectedTags.isEmpty()) {
                ProgramFilter pf = new ProgramFilter();
                pf.setDisplayName(filter.getDisplayName());
                pf.setInternalName(filter.getInternalName());
                pf.setId(filter.getId());
                pf.setOrder(filter.getOrder());
                pf.setShowIn(filter.getShowIn());
                pf.setTags(selectedTags);

                filters.add(pf);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        gridLayoutManager = new GridLayoutManager(mActivity, 2);
        fetchData();
    }

    private void getFilters() {
        langTags = new ArrayList<>();
        mDataManager.getProgramFilters(new OnResponseCallback<List<ProgramFilter>>() {
            @Override
            public void onSuccess(List<ProgramFilter> data) {
                List<ProgramFilter> removables = new ArrayList<>();
                for (ProgramFilter filter : data) {

                    if (filter.getInternalName().equalsIgnoreCase("lang")) {
                        langTags.clear();
                        langTags.add(new DropDownFilterView.FilterItem(filter.getDisplayName(), null,
                                true, R.color.primary_cyan, R.drawable.t_background_tag_hollow));

                        for (ProgramFilterTag tag : filter.getTags()) {
                            langTags.add(new DropDownFilterView.FilterItem(tag.getDisplayName(), tag,
                                    false, R.color.white, R.drawable.t_background_tag_filled));
                        }
                    }

                    if (filter.getShowIn() == null || filter.getShowIn().isEmpty() ||
                            !filter.getShowIn().contains("schedule")) {
                        removables.add(filter);
                    }
                }
                for (ProgramFilter filter : removables) {
                    data.remove(filter);
                }

                if (!data.isEmpty()) {
                    allFilters = data;
                    filtersVisible.set(true);
                    filtersAdapter.setItems(data);
                } else {
                    filtersVisible.set(false);
                }

            }

            @Override
            public void onFailure(Exception e) {
                filtersVisible.set(false);
            }
        });

    }


    private void getPeriods() {
        periodAdapter.reset(true);
        mDataManager.getPeriods(filters, mDataManager.getLoginPrefs().getProgramId(),
                mDataManager.getLoginPrefs().getSectionId(), take, skip, new OnResponseCallback<List<Period>>() {
                    @Override
                    public void onSuccess(List<Period> data) {
                        mActivity.hideLoading();
                        if (data.size() < take) {
                            allLoaded = true;
                        }
                        populatePeriods(data);
                        periodAdapter.setLoadingDone();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        mActivity.hideLoading();
                        allLoaded = true;
                        periodAdapter.setLoadingDone();
                        toggleEmptyVisibility();
                    }
                });
    }

    private void populatePeriods(List<Period> data) {
        boolean newItemsAdded = false;
        int n = 0;
        for (Period period : data) {
            if (!periodList.contains(period)) {
                periodList.add(period);
                newItemsAdded = true;
                n++;
            }
        }

        if (newItemsAdded) {
            periodAdapter.notifyItemRangeInserted(periodList.size() - n, n);
        }

        toggleEmptyVisibility();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PeriodSavedEvent event) {
        int position = periodAdapter.getItemPosition(event.getPeriod());
        if (position >= 0) {
            periodList.get(position).setTotalCount(event.getPeriod().getTotalCount());
            periodAdapter.notifyItemChanged(position);
        }
    }

    public void registerEventBus() {
        EventBus.getDefault().registerSticky(this);
    }

    public void unRegisterEventBus() {
        EventBus.getDefault().unregister(this);
    }

    private void toggleEmptyVisibility() {
        if (periodList == null || periodList.isEmpty()) {
            emptyVisible.set(true);
        } else {
            emptyVisible.set(false);
        }
    }

    public void addPeriod() {

        final Dialog dialog = new Dialog(mActivity);
        dialog.setContentView(R.layout.t_alert_add_period);
        Button dialogButton = (Button) dialog.findViewById(R.id.submit_button);
        EditText dialogText =  dialog.findViewById(R.id.et_period_name);
        DropDownFilterView drop = dialog.findViewById(R.id.filter_drop_down);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.FIRST_SUB_WINDOW;
        dialog.show();
        dialog.getWindow().setAttributes(lp);
        drop.setFilterItems(langTags);
        drop.setOnFilterItemListener((v, item, position, prev) -> {
            if (item.getItem() == null){
                lang = null;
            } else {
                lang = ((ProgramFilterTag) item.getItem()).getInternalName();
            }
        });
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(v ->
        {

            if (lang == null){
                mActivity.showLongSnack("Please select a language");
                return;
            }
            String periodName = dialogText.getText().toString();
            createPeriods(lang, periodName);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void createPeriods(String lang, String periodName) {
        lang = "hi";
        mActivity.showLoading();
        mDataManager.createPeriod(mDataManager.getLoginPrefs().getProgramId(),
                mDataManager.getLoginPrefs().getSectionId(), lang, periodName, new OnResponseCallback<SuccessResponse>() {
                    @Override
                    public void onSuccess(SuccessResponse data) {
                        if (data.getSuccess()) {
                            changesMade = true;
                            allLoaded = false;
                            fetchData();
                        } else {
                            mActivity.hideLoading();
                            mActivity.showLongSnack("Unable to create new periods");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        mActivity.hideLoading();
                        mActivity.showLongSnack(e.getLocalizedMessage());
                    }
                });
    }

    public class FiltersAdapter extends MxFiniteAdapter<ProgramFilter> {

        public FiltersAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBind(@NonNull ViewDataBinding binding, @NonNull ProgramFilter model,
                           @Nullable OnRecyclerItemClickListener<ProgramFilter> listener) {
            if (binding instanceof TRowFilterDropDownBinding) {
                TRowFilterDropDownBinding dropDownBinding = (TRowFilterDropDownBinding) binding;

                List<DropDownFilterView.FilterItem> items = new ArrayList<>();
                items.add(new DropDownFilterView.FilterItem(model.getDisplayName(), null,
                        true, R.color.primary_cyan, R.drawable.t_background_tag_hollow
                ));
                for (ProgramFilterTag tag : model.getTags()) {
                    items.add(new DropDownFilterView.FilterItem(tag.getDisplayName(), tag,
                            false, R.color.white, R.drawable.t_background_tag_filled
                    ));
                }
                dropDownBinding.filterDropDown.setFilterItems(items);

                dropDownBinding.filterDropDown.setOnFilterItemListener((v, item, position, prev) -> {
                    if (prev != null && prev.getItem() != null) {
                        tags.remove((ProgramFilterTag) prev.getItem());
                    }
                    if (item.getItem() != null) {
                        tags.add((ProgramFilterTag) item.getItem());
                    }

                    changesMade = true;
                    allLoaded = false;
                    mActivity.showLoading();
                    fetchData();
                });


            }
        }
    }

    public class PeriodAdapter extends MxInfiniteAdapter<Period> {

        public PeriodAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBind(@NonNull ViewDataBinding binding, @NonNull Period model,
                           @Nullable OnRecyclerItemClickListener<Period> listener) {
            if (binding instanceof TRowScheduleBinding) {
                TRowScheduleBinding scheduleBinding = (TRowScheduleBinding) binding;
                scheduleBinding.setPeriod(model);
                scheduleBinding.getRoot().setOnClickListener(v -> {
                    if (listener != null){
                        listener.onItemClick(v, model);
                    }
                });
            }
        }
    }
}