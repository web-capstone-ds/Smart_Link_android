package com.smartfactory.visioninspection.network;

import com.smartfactory.visioninspection.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class AuthClient {
    private static volatile AuthApi authApi;

    private AuthClient() {}

    public static AuthApi getApi() {
        if (authApi == null) {
            synchronized (AuthClient.class) {
                if (authApi == null) {
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(normalizeBaseUrl(BuildConfig.AUTH_BASE_URL))
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    authApi = retrofit.create(AuthApi.class);
                }
            }
        }
        return authApi;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://13.209.10.148:8080/api/v1/";
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed : (trimmed + "/");
    }
}
