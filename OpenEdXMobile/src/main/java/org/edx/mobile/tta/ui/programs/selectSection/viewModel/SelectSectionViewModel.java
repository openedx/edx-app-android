package org.edx.mobile.tta.ui.programs.selectSection.viewModel;

import android.content.Context;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.maurya.mx.mxlib.core.MxInfiniteAdapter;
import com.maurya.mx.mxlib.core.OnRecyclerItemClickListener;

import org.edx.mobile.R;
import org.edx.mobile.databinding.TRowSelectProgSectionBinding;
import org.edx.mobile.tta.data.local.db.table.Section;
import org.edx.mobile.tta.interfaces.OnResponseCallback;
import org.edx.mobile.tta.ui.base.mvvm.BaseVMActivity;
import org.edx.mobile.tta.ui.base.mvvm.BaseViewModel;
import org.edx.mobile.tta.ui.landing.LandingActivity;
import org.edx.mobile.tta.ui.programs.selectSection.SelectSectionActivity;
import org.edx.mobile.tta.ui.programs.selectprogram.SelectProgramActivity;
import org.edx.mobile.tta.utils.ActivityUtil;

import java.util.ArrayList;
import java.util.List;

public class SelectSectionViewModel extends BaseViewModel {
    public List<Section> section;
    public List<Section> selectedSections;
    private String sectionId;
    private Section sectionItem;
    private View itemView;
    public SectionAdapter sectionAdapter;
    public RecyclerView.LayoutManager layoutManager;
    public ObservableField<String> programId = new ObservableField<>();
    public ObservableField<String> programForSection = new ObservableField<>();
    public ObservableBoolean fabPrevVisibility = new ObservableBoolean();



    public SelectSectionViewModel(BaseVMActivity activity) {
        super(activity);
//        programId.set(mDataManager.getLoginPrefs().getProgramId());
        sectionId = "";
        section = new ArrayList<>();
        layoutManager = new LinearLayoutManager(mActivity);
        sectionAdapter = new SectionAdapter(mActivity);
        selectedSections = new ArrayList<>();
        mActivity.showLoading();

        fetchSections();
        sectionAdapter.setItems(section);
        sectionAdapter.setItemClickListener((view, item) -> {
            if (selectedSections.size() > 0) {
                if (view == itemView) {
                    selectedSections.clear();
                    itemView.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.white));
                } else {
                    selectedSections.clear();
                    selectedSections.add(item);
                    itemView.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.white));
                    view.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.secondary_blue_light));
                    itemView = view;
                    sectionId = item.getId();
                    mDataManager.getLoginPrefs().setRole(item.getRole());

                }
            } else {
                selectedSections.add(item);
                itemView = view;
                sectionItem = item;
                sectionId = item.getId();
                mDataManager.getLoginPrefs().setRole(item.getRole());
                view.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.secondary_blue_light));
            }

//            sectionAdapter.notifyItemChanged(sectionAdapter.getItemPosition(item));
        });
    }


    public void fetchSections() {

        mActivity.showLoading();
        programForSection.set("Select section for : " + mDataManager.getLoginPrefs().getProgramTitle());
        mDataManager.getSections(mDataManager.getLoginPrefs().getProgramId(), new OnResponseCallback<List<Section>>() {
            @Override
            public void onSuccess(List<Section> data) {
                mActivity.hideLoading();
                populateSection(data);
//                for (Section unit: data){
//                    if (!selectedSections.contains(unit)){
//                        selectedSections.add(unit);
//                    }
//                }
                sectionAdapter.setLoadingDone();
            }

            @Override
            public void onFailure(Exception e) {
                mActivity.hideLoading();
            }
        });

    }

    public void save() {
        mActivity.showLoading();
        if (!sectionId.isEmpty()) {
            mDataManager.getLoginPrefs().setProgramId(programId.get());
            mDataManager.getLoginPrefs().setSectionId(sectionId);
            ActivityUtil.gotoPage(mActivity, LandingActivity.class);
            mActivity.finish();
        } else {
            mActivity.hideLoading();
            mActivity.showShortSnack("Please select a section");
        }

    }


    @Override
    public void onResume() {
        super.onResume();
//        fetchSections();
    }

    private void populateSection(List<Section> data) {
        mActivity.showLoading();
        boolean newItemsAdded = false;
        int n = 0;
        if (data.size() == 1){
            sectionId = data.get(0).getId();
            save();
        }
        for (Section user : data) {
            if (!section.contains(user)) {
                section.add(user);
                newItemsAdded = true;
                n++;
            }
        }

        if (newItemsAdded) {
            sectionAdapter.notifyItemRangeInserted(section.size() - n, n);
        }
        mActivity.hideLoading();
    }


    public class SectionAdapter extends MxInfiniteAdapter<Section> {

        public SectionAdapter(Context context) {
            super(context);

        }

        @Override
        public void onBind(@NonNull ViewDataBinding binding, @NonNull Section model,
                           @Nullable OnRecyclerItemClickListener<Section> listener) {
            if (binding instanceof TRowSelectProgSectionBinding) {
                TRowSelectProgSectionBinding itemBinding = (TRowSelectProgSectionBinding) binding;
                itemBinding.textPrograms.setText(model.getTitle());

                itemBinding.llProg.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(v, model);
                    }
                });
//                itemBinding.llProg.setOnClickListener(v -> {
//                        sectionId = model.getId();
//                        itemBinding.llProg.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.secondary_blue_light));
//
//                });

            }
        }

    }

    public void selectProg() {
        Bundle b = new Bundle();
        b.putBoolean("isPrev", true);
        ActivityUtil.gotoPage(mActivity, SelectProgramActivity.class, b);
        mActivity.finish();
    }
}