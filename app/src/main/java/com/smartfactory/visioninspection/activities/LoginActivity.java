package com.smartfactory.visioninspection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.MockUsersUtil;
import com.smartfactory.visioninspection.utils.SessionManager;

/**
 * 로그인 화면 (LoginScreen.tsx 대응)
 * 사번 + 비밀번호 입력 → MockUsersUtil 검증 → MainActivity 이동
 */
public class LoginActivity extends AppCompatActivity {

    private EditText    etEmployeeId;
    private EditText    etPassword;
    private ImageButton btnTogglePassword;
    private Button      btnLogin;

    private boolean         passwordVisible = false;
    private SessionManager  sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // 이미 로그인된 세션이면 메인으로 즉시 이동
        /*
        if (sessionManager.isLoggedIn()) {
            goToMain();
           return;
        }
         */

        setContentView(R.layout.activity_login);

        etEmployeeId      = findViewById(R.id.et_employee_id);
        etPassword        = findViewById(R.id.et_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnLogin          = findViewById(R.id.btn_login);

        // 비밀번호 토글
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // 로그인 버튼
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    // ── 비밀번호 표시/숨김 전환 ───────────────────────────────
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setInputType(
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        } else {
            etPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        }
        // 커서를 텍스트 끝으로 유지
        etPassword.setSelection(etPassword.getText().length());
    }

    // ── 로그인 처리 ──────────────────────────────────────────
    private void handleLogin() {
        String empId = etEmployeeId.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (empId.isEmpty()) {
            etEmployeeId.setError(getString(R.string.error_empty_employee_id));
            etEmployeeId.requestFocus();
            return;
        }
        if (pass.isEmpty()) {
            etPassword.setError(getString(R.string.error_empty_password));
            etPassword.requestFocus();
            return;
        }

        User user = MockUsersUtil.validateLogin(empId, pass);

        if (user != null) {
            sessionManager.saveSession(user);
            goToMain();
        } else {
            Toast.makeText(this,
                    getString(R.string.error_invalid_credentials),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ── MainActivity 이동 (백스택 제거) ──────────────────────
    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
