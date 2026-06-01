package com.smartfactory.visioninspection.network;

import com.smartfactory.visioninspection.network.dto.LoginRequest;
import com.smartfactory.visioninspection.network.dto.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}
