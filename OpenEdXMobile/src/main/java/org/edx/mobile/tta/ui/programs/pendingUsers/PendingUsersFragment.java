package org.edx.mobile.tta.ui.programs.pendingUsers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.edx.mobile.R;
import org.edx.mobile.tta.ui.base.TaBaseFragment;
import org.edx.mobile.tta.ui.library.LibraryFragment;
import org.edx.mobile.tta.ui.programs.pendingUsers.viewModel.PendingUsersViewModel;

public class PendingUsersFragment extends TaBaseFragment{
    public static final String TAG = LibraryFragment.class.getCanonicalName();

    private PendingUsersViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new PendingUsersViewModel(getActivity(), this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = binding(inflater, container, R.layout.t_fragment_pending_users, viewModel).getRoot();
        viewModel.fetchUsers();
        return rootView;
    }
}
