package com.smartfactory.visioninspection.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.smartfactory.visioninspection.models.User;

public class SessionManager {

    private static final String PREF_NAME = "SmartFactorySession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMP_ID = "empId";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    // 1. Context를 받는 생성자 (LoginActivity에서 에러 나던 부분 해결!)
    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // 2. 로그인 상태 저장
    public void saveSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        // User 모델 구조에 맞게 수정될 수 있지만 기본적으로 로그인 상태만 저장해도 작동합니다.
        editor.apply();
    }

    // 3. 로그인 여부 확인 (LoginActivity에서 에러 나던 부분 해결!)
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // 4. 현재 사용자 가져오기 (MainActivity 헤더용)
    public User getCurrentUser() {
        if (!isLoggedIn()) return null;

        User user = new User("EMP001", "김스마트", "관리자", "A동", "010-0000-0000");
        // 임시 더미 데이터 (실제 백엔드 연동 시 User 객체에 맞게 수정)
        return user;
    }

    // 5. 로그아웃 처리
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}