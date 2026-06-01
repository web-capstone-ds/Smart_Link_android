package com.smartfactory.visioninspection.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.network.AuthClient;
import com.smartfactory.visioninspection.network.dto.LoginRequest;
import com.smartfactory.visioninspection.network.dto.LoginResponse;
import com.smartfactory.visioninspection.utils.MockUsersUtil;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.io.IOException;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmployeeId;
    private EditText etPassword;
    private ImageButton btnTogglePw;
    private TextView tvError;
    private Button btnLogin;
    private TextView btnChangePassword;

    private boolean isPasswordVisible = false;
    private boolean isLoginRequestInFlight = false;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn() && sessionManager.isTokenValid()) {
            goToMain();
            return;
        }
        if (sessionManager.isLoggedIn() && !sessionManager.isTokenValid()) {
            sessionManager.clearSession();
        }

        etEmployeeId = findViewById(R.id.etEmployeeId);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePw = findViewById(R.id.btnTogglePw);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        if (btnChangePassword != null) {
            btnChangePassword.setVisibility(View.GONE);
        }

        btnTogglePw.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> handleLogin());
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

        etPassword.setSelection(etPassword.getText().length());
    }

    private void handleLogin() {
        if (isLoginRequestInFlight) return;

        tvError.setVisibility(View.GONE);

        String operatorId = etEmployeeId.getText().toString().trim().toUpperCase(Locale.ROOT);
        String password = etPassword.getText().toString().trim();

        if (operatorId.isEmpty()) {
            showError("사번을 입력해 주세요.");
            return;
        }

        if (password.isEmpty()) {
            showError("비밀번호를 입력해 주세요.");
            return;
        }

        setLoginInFlight(true);
        LoginRequest request = new LoginRequest(operatorId, password);

        AuthClient.getApi().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                setLoginInFlight(false);
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveSession(response.body());
                    goToMain();
                    return;
                }

                if (response.code() >= 500 && tryLocalBypassLogin(operatorId, password)) {
                    return;
                }

                String serverMessage = extractErrorMessage(response);
                if (response.code() == 401) {
                    if (serverMessage.toLowerCase(Locale.ROOT).contains("inactive")) {
                        showError("비활성 계정입니다. 관리자에게 문의해 주세요.");
                    } else {
                        showError("사번 또는 비밀번호가 올바르지 않습니다.");
                    }
                } else {
                    showError("로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                setLoginInFlight(false);

                if (tryLocalBypassLogin(operatorId, password)) {
                    return;
                }

                showError("네트워크 오류입니다. 연결 상태를 확인해 주세요.");
            }
        });
    }

    private boolean tryLocalBypassLogin(String operatorId, String password) {
        if (!isDebugBuild()) return false;

        User user = MockUsersUtil.findUserByEmployeeId(operatorId);
        if (user != null && "1234".equals(password)) {
            sessionManager.saveLocalSession(user);
            goToMain();
            return true;
        }
        return false;
    }

    private boolean isDebugBuild() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void setLoginInFlight(boolean inFlight) {
        isLoginRequestInFlight = inFlight;
        btnLogin.setEnabled(!inFlight);
        btnLogin.setText(inFlight ? "로그인 중..." : "로그인");
        etEmployeeId.setEnabled(!inFlight);
        etPassword.setEnabled(!inFlight);
        btnTogglePw.setEnabled(!inFlight);
    }

    private String extractErrorMessage(Response<?> response) {
        if (response == null || response.errorBody() == null) return "";
        try {
            String body = response.errorBody().string();
            return body == null ? "" : body.trim();
        } catch (IOException e) {
            return "";
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