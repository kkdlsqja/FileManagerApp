package com.filemanager.app.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST; // POST 임포트 필요
import retrofit2.http.Query;

public interface ApiService {

    // --- 새로 추가할 인증(Auth) API ---

    // 1. 회원가입 API
    @POST("api/auth/signup")
    Call<String> signup(@Body AuthRequest authRequest);

    // 2. 로그인 API
    @POST("api/auth/login")
    Call<String> login(@Body AuthRequest authRequest);


    // --- 기존 PC 관련 API (그대로 유지) ---

    @GET("api/pc/register")
    Call<String> registerPc(
            @Query("userId") Long userId,
            @Query("pcName") String pcName,
            @Query("pcIdentifier") String pcIdentifier
    );

    @GET("api/pc/list")
    Call<List<PcDevice>> getPcList(
            @Query("userId") Long userId
    );

    @GET("api/pc/connect")
    Call<String> connectPc(
            @Query("pcId") Long pcId
    );
}