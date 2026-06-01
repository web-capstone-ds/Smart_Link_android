package com.smartfactory.visioninspection.network.dto;

public class LoginResponse {
    private String token;
    private String operatorId;
    private String name;
    private String department;
    private String phone;
    private String role;
    private int expiresIn;

    public String getToken() {
        return token;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public int getExpiresIn() {
        return expiresIn;
    }
}
