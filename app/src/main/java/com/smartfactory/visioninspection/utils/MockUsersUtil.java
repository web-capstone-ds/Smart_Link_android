package com.smartfactory.visioninspection.utils;

import com.smartfactory.visioninspection.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MockUsersUtil {

    private static final List<User> SERVER_USERS;

    static {
        List<User> users = new ArrayList<>();
        users.add(new User("EMP001", "유민호", "1234", "A동", "관리자", "010-1789-6815"));
        users.add(new User("EMP002", "최민수", "5678", "B동", "운영자", "010-6582-7892"));
        users.add(new User("EMP003", "송기환", "9012", "C동", "운영자", "010-1567-4568"));
        users.add(new User("EMP004", "이재혁", "3456", "품질관리팀", "검사원", "010-3113-6985"));
        users.add(new User("EMP005", "정연우", "3456", "품질관리팀", "검사원", "010-3113-6323"));
        SERVER_USERS = Collections.unmodifiableList(users);
    }

    public static User findUserByEmployeeId(String employeeId) {
        for (User u : SERVER_USERS) {
            if (u.getEmployeeId().equals(employeeId)) return u;
        }
        return null;
    }

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
