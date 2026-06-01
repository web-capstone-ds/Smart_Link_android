package com.smartfactory.visioninspection.utils;

import com.smartfactory.visioninspection.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Display-only mock user list.
 * Do not use for authentication.
 */
public class MockUsersUtil {

    private static final List<User> MOCK_USERS;

    static {
        List<User> users = new ArrayList<>();
        users.add(new User("EMP001", "유민호", "A동", "ADMIN", "010-1789-6815"));
        users.add(new User("EMP002", "최민수", "B동", "OPERATOR", "010-6582-7892"));
        users.add(new User("EMP003", "송기환", "C동", "OPERATOR", "010-1567-4568"));
        users.add(new User("EMP004", "이재혁", "품질관리팀", "INSPECTOR", "010-3113-6985"));
        users.add(new User("EMP005", "정연우", "품질관리팀", "INSPECTOR", "010-3113-6323"));
        MOCK_USERS = Collections.unmodifiableList(users);
    }

    public static User findUserByEmployeeId(String employeeId) {
        for (User user : MOCK_USERS) {
            if (user.getEmployeeId().equals(employeeId)) return user;
        }
        return null;
    }

    /**
     * Deprecated by server-side auth (/auth/login).
     * Kept for compatibility only.
     */
    @Deprecated
    public static User validateLogin(String employeeId, String password) {
        return null;
    }

    public static List<User> getAllUsers() {
        return MOCK_USERS;
    }
}
