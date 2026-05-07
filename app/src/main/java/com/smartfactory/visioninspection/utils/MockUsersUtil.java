package com.smartfactory.visioninspection.utils;

import com.smartfactory.visioninspection.models.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 서버 저장 사용자 목록 (mockUsers.ts 대응)
 * 실제 환경에서는 REST API / JWT 인증으로 대체
 */
public class MockUsersUtil {

    private static final List<User> SERVER_USERS;

    static {
        List<User> users = new ArrayList<>();
        users.add(new User("EMP001", "김민준", "1234", "품질관리팀", "관리자"));
        users.add(new User("EMP002", "이서윤", "5678", "생산관리팀", "운영자"));
        users.add(new User("EMP003", "박도현", "9012", "설비관리팀", "운영자"));
        users.add(new User("EMP004", "최지아", "3456", "품질관리팀", "검사원"));
        SERVER_USERS = Collections.unmodifiableList(users);
    }

    /** 사번으로 사용자 조회 */
    public static User findUserByEmployeeId(String employeeId) {
        for (User u : SERVER_USERS) {
            if (u.getEmployeeId().equals(employeeId)) return u;
        }
        return null;
    }

    /** 사번 + 비밀번호 검증 → 성공 시 User 반환, 실패 시 null */
    public static User validateLogin(String employeeId, String password) {
        for (User u : SERVER_USERS) {
            if (u.getEmployeeId().equals(employeeId)
                    && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    public static List<User> getAllUsers() {
        return SERVER_USERS;
    }
}
