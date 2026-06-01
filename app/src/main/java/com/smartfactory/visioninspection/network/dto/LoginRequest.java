package com.smartfactory.visioninspection.network.dto;

public class LoginRequest {
    private final String operatorId;
    private final String password;

    public LoginRequest(String operatorId, String password) {
        this.operatorId = operatorId;
        this.password = password;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getPassword() {
        return password;
    }
}
