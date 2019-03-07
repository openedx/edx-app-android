package org.edx.mobile.tta.ui.library;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.edx.mobile.R;
import org.edx.mobile.tta.data.local.db.table.Category;
import org.edx.mobile.tta.data.model.library.CollectionConfigResponse;
import org.edx.mobile.tta.ui.base.TaBaseFragment;
import org.edx.mobile.tta.ui.interfaces.SearchPageOpenedListener;
import org.edx.mobile.tta.ui.library.view_model.LibraryTabViewModel;
import org.edx.mobile.util.PermissionsUtil;

public class LibraryTab extends TaBaseFragment {

    private CollectionConfigResponse cr;

    private Category category;

    private SearchPageOpenedListener searchPageOpenedListener;

    private LibraryTabViewModel viewModel;

    public static LibraryTab newInstance(CollectionConfigResponse cr, Category category,
                                         SearchPageOpenedListener searchPageOpenedListener){
        LibraryTab fragment = new LibraryTab();
        fragment.cr = cr;
        fragment.category = category;
        fragment.searchPageOpenedListener = searchPageOpenedListener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new LibraryTabViewModel(getActivity(), this, cr, category, searchPageOpenedListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = binding(inflater, container, R.layout.t_fragment_library_tab, viewModel)
                .getRoot();

        return view;
    }

    @Override
    public void onPermissionGranted(String[] permissions, int requestCode) {
        switch (requestCode){
            case PermissionsUtil.WRITE_STORAGE_PERMISSION_REQUEST:
                viewModel.showContentDashboard();
                break;
        }
    }
}
