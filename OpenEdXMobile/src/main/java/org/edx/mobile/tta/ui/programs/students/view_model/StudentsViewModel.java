package org.edx.mobile.tta.ui.programs.students.view_model;

import android.content.Context;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.bumptech.glide.Glide;
import com.maurya.mx.mxlib.core.MxFiniteAdapter;
import com.maurya.mx.mxlib.core.MxInfiniteAdapter;
import com.maurya.mx.mxlib.core.OnRecyclerItemClickListener;

import org.edx.mobile.R;
import org.edx.mobile.databinding.CommonFilterItemBinding;
import org.edx.mobile.databinding.TRowStudentFilterBinding;
import org.edx.mobile.databinding.TRowStudentsGridBinding;
import org.edx.mobile.databinding.TRowSuggestedTeacherGridBinding;
import org.edx.mobile.tta.data.local.db.table.Period;
import org.edx.mobile.tta.data.model.feed.SuggestedUser;
import org.edx.mobile.tta.data.model.program.ProgramFilter;
import org.edx.mobile.tta.data.model.program.ProgramUser;
import org.edx.mobile.tta.interfaces.OnResponseCallback;
import org.edx.mobile.tta.ui.base.TaBaseFragment;
import org.edx.mobile.tta.ui.base.mvvm.BaseViewModel;
import org.edx.mobile.tta.ui.custom.DropDownFilterView;
import org.edx.mobile.tta.ui.programs.schedule.view_model.ScheduleViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StudentsViewModel extends BaseViewModel {

    public UsersAdapter adapter;
    public RecyclerView.LayoutManager layoutManager;
    public RecyclerView.LayoutManager filterLayoutManager;
    public List<ProgramUser> users;

    public List<ProgramFilter> filterList;

    public DropDownFilterView.OnFilterClickListener typeListener = (v, item, position, prev) -> {

    };

    public DropDownFilterView.OnFilterClickListener sessionListener = (v, item, position, prev) -> {

    };

    public FiltersAdapter filtersAdapter;

    public StudentsViewModel(Context context, TaBaseFragment fragment) {
        super(context, fragment);

        adapter = new UsersAdapter(mActivity);
        filtersAdapter = new FiltersAdapter(mActivity);

//        fetchStudents();
    }

    @Override
    public void onResume() {
        super.onResume();
        layoutManager = new GridLayoutManager(mActivity, 2);
        filterLayoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false);
    }


    public void getUsers(){
        mDataManager.getUsers("", "", 0, 0, new OnResponseCallback<List<ProgramUser>>() {
            @Override
            public void onSuccess(List<ProgramUser> data) {
                users = data;
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
        adapter.setItems(users);
    }

    public void getFilters(){
        mDataManager.getProgramFilters(new OnResponseCallback<List<ProgramFilter>>() {
            @Override
            public void onSuccess(List<ProgramFilter> data) {
                filterList = data;
            }

            @Override
            public void onFailure(Exception e) {

            }
        });

        filtersAdapter.setItems(filterList);
    }
//    private void fetchStudents() {
//
//        List<SuggestedUser> users = new ArrayList<>();
//        String name = mDataManager.getLoginPrefs().getUsername() != null &&
//                mDataManager.getLoginPrefs().getUsername().equalsIgnoreCase("staff") ?
//                "Staff" : "Student";
//        for (int i = 0; i < 20; i++){
//            SuggestedUser user = new SuggestedUser();
//            user.setName(name + " - " + (i+1));
//            users.add(user);
//        }
//        adapter.setItems(users);
//
//    }

    public class UsersAdapter extends MxInfiniteAdapter<ProgramUser> {

        public UsersAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBind(@NonNull ViewDataBinding binding, @NonNull ProgramUser model,
                           @Nullable OnRecyclerItemClickListener<ProgramUser> listener) {
            if (binding instanceof TRowStudentsGridBinding) {
                TRowStudentsGridBinding itemBinding = (TRowStudentsGridBinding) binding;
//                teacherBinding.setViewModel(model);
                Glide.with(getContext())
                        .load(users)
                        .placeholder(R.drawable.profile_photo_placeholder)
                        .into(itemBinding.userImage);

                if(Objects.equals(mDataManager.getLoginPrefs().getUsername(), "staff")){
                    itemBinding.followBtn.setVisibility(View.GONE);
                }

//                teacherBinding.followBtn.setOnClickListener(v -> {
//                    if (listener != null) {
//                        listener.onItemClick(v, model);
//                    }
//                });
//
//                teacherBinding.getRoot().setOnClickListener(v -> {
//                    if (listener != null) {
//                        listener.onItemClick(v, model);
//                    }
//                });
            }
        }
    }

    public class FiltersAdapter extends MxFiniteAdapter<ProgramFilter> {
        public FiltersAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBind(@NonNull ViewDataBinding binding, @NonNull ProgramFilter model,
                           @Nullable OnRecyclerItemClickListener<ProgramFilter> listener) {
            if (binding instanceof TRowStudentFilterBinding){
                TRowStudentFilterBinding itemBinding = (TRowStudentFilterBinding) binding;

            }
        }

    }
}
