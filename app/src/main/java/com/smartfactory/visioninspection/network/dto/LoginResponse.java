package com.smartfactory.visioninspection.network.dto;

public class LoginResponse {
    private String token;
    private String operatorId;
    private String name;
    private String department;
    private String phone;
    private String role;
    private int expiresIn;
    private Data data;

    public String getToken() {
        if (hasText(token)) return token;
        return data == null ? null : data.accessToken;
    }

    public String getOperatorId() {
        if (hasText(operatorId)) return operatorId;
        return data == null || data.user == null ? null : data.user.operatorId;
    }

    public String getName() {
        if (hasText(name)) return name;
        return data == null || data.user == null ? null : data.user.name;
    }

    public String getDepartment() {
        if (hasText(department)) return department;
        return data == null || data.user == null ? null : data.user.department;
    }

    public String getPhone() {
        if (hasText(phone)) return phone;
        return data == null || data.user == null ? null : data.user.phone;
    }

    public String getRole() {
        if (hasText(role)) return role;
        return data == null || data.user == null ? null : data.user.role;
    }

    public int getExpiresIn() {
        if (expiresIn > 0) return expiresIn;
        if (data != null && data.expiresInSeconds > 0) return data.expiresInSeconds;
        return hasText(getToken()) ? 1800 : 0;
    }

    public String getRefreshToken() {
        return data == null ? null : data.refreshToken;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class Data {
        private String accessToken;
        private String refreshToken;
        private int expiresInSeconds;
        private UserInfo user;
    }

    private static class UserInfo {
        private String operatorId;
        private String name;
        private String department;
        private String phone;
        private String role;
    }
}
