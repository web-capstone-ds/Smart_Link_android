package com.smartfactory.visioninspection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.MockUsersUtil;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmployeeId;
    private EditText etPassword;
    private ImageButton btnTogglePw;
    private TextView tvError;
    private Button btnLogin;
    private TextView btnChangePassword;

    private boolean isPasswordVisible = false;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // 이미 로그인되어 있으면 메인으로 이동
        if (sessionManager.isLoggedIn()) {
            goToMain();
            return;
        }

        etEmployeeId = findViewById(R.id.etEmployeeId);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePw = findViewById(R.id.btnTogglePw);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnTogglePw.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> handleLogin());

        // 현재는 임시 처리 (나중에 비밀번호 변경 화면 연결)
        btnChangePassword.setOnClickListener(v ->
                Toast.makeText(this, "비밀번호 변경 기능은 다음 단계에서 연결합니다.", Toast.LENGTH_SHORT).show()
        );
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePw.setImageResource(R.drawable.ic_visibility);
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePw.setImageResource(R.drawable.ic_visibility_off);
        }

        // 커서를 텍스트 끝으로 유지
        etPassword.setSelection(etPassword.getText().length());
    }

    private void handleLogin() {
        tvError.setVisibility(View.GONE);

        String empId = etEmployeeId.getText().toString().trim().toUpperCase(Locale.ROOT);
        String pw = etPassword.getText().toString().trim();

        if (empId.isEmpty()) {
            showError("사번을 입력해주세요.");
            return;
        }

        if (pw.isEmpty()) {
            showError("비밀번호를 입력해주세요.");
            return;
        }

        User user = MockUsersUtil.validateLogin(empId, pw);

        if (user != null) {
            sessionManager.saveSession(user);
            goToMain();
        } else {
            showError("사번 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}