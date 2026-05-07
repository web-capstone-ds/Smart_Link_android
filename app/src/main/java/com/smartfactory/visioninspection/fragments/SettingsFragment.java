package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.activities.MainActivity;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.SessionManager;

/**
 * 설정 탭 (Settings.tsx 대응)
 * 현재 로그인 사용자 정보 표시 + 로그아웃 버튼
 */
public class SettingsFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName   = view.findViewById(R.id.tv_user_name);
        TextView tvEmpId  = view.findViewById(R.id.tv_emp_id);
        TextView tvDept   = view.findViewById(R.id.tv_department);
        TextView tvRole   = view.findViewById(R.id.tv_role);
        Button   btnLogout = view.findViewById(R.id.btn_logout);

        // 현재 로그인 사용자 정보 표시
        if (getActivity() instanceof MainActivity) {
            SessionManager sm   = ((MainActivity) getActivity()).getSessionManager();
            User           user = sm.getCurrentUser();
            if (user != null) {
                tvName.setText(user.getName());
                tvEmpId.setText(user.getEmployeeId());
                tvDept.setText(user.getDepartment());
                tvRole.setText(user.getRole());
            }
        }

        // 로그아웃
        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
    }
}
