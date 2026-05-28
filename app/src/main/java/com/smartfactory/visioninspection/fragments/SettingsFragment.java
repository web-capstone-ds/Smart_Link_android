package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.activities.MainActivity;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.SessionManager;

public class SettingsFragment extends Fragment {

    private TextView tvUserId;
    private TextView tvUserName;
    private TextView tvMqttChip;
    private TextView tvDeptRole;
    private TextView tvMesServer;

    private TextView tvProfileName;
    private TextView tvProfileEmpId;
    private TextView tvProfileDepartment;
    private TextView tvProfileRole;

    private Button btnLogout;
    private ImageButton btnHeaderLogout;
    private ImageButton btnThemeToggle;

    private boolean mqttConnected;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        bindUser();
        setupActions();
        renderConnectionState();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindUser();
        renderConnectionState();
    }

    private void bindViews(View view) {
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvMqttChip = view.findViewById(R.id.tv_mqtt_chip);
        tvDeptRole = view.findViewById(R.id.tv_dept_role);
        tvMesServer = view.findViewById(R.id.tv_mes_server);

        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmpId = view.findViewById(R.id.tv_profile_emp_id);
        tvProfileDepartment = view.findViewById(R.id.tv_profile_department);
        tvProfileRole = view.findViewById(R.id.tv_profile_role);

        btnHeaderLogout = view.findViewById(R.id.btn_logout);
        btnThemeToggle = view.findViewById(R.id.btn_theme_toggle);
        btnLogout = view.findViewById(R.id.btn_logout_bottom);
    }

    private void bindUser() {
        if (!(getActivity() instanceof MainActivity)) return;
        SessionManager sm = ((MainActivity) getActivity()).getSessionManager();
        User user = sm.getCurrentUser();
        if (user == null) return;

        tvUserId.setText(user.getEmployeeId());
        tvUserName.setText(user.getName());
        tvDeptRole.setText(user.getDepartmentRolePhoneLabel());

        tvProfileName.setText(user.getName());
        tvProfileEmpId.setText(user.getEmployeeId());
        tvProfileDepartment.setText(user.getDepartment());
        tvProfileRole.setText(user.getRole());
    }

    private void setupActions() {
        btnThemeToggle.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleThemeMode();
            }
        });

        View.OnClickListener logoutAction = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        };

        btnHeaderLogout.setOnClickListener(logoutAction);
        btnLogout.setOnClickListener(logoutAction);
    }

    public void setMqttConnected(boolean connected) {
        mqttConnected = connected;
        if (isAdded()) renderConnectionState();
    }

    private void renderConnectionState() {
        if (!isAdded()) return;
        if (mqttConnected) {
            tvMqttChip.setText("● MQTT 연결됨");
            tvMqttChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_pass));
            tvMqttChip.setBackgroundResource(R.drawable.bg_badge_pass);
            tvMesServer.setText("서버 MES 연결됨");
            tvMesServer.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        } else {
            tvMqttChip.setText("● MQTT 연결 안됨");
            tvMqttChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_fail));
            tvMqttChip.setBackgroundResource(R.drawable.bg_badge_fail);
            tvMesServer.setText("서버 MES 연결 안됨");
            tvMesServer.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_fail));
        }
    }
}
