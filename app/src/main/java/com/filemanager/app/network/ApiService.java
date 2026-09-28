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
    Call<String> signup(@Query("email") String email, @Query("password") String password);

    // 2. 로그인 API
    @POST("api/auth/login")
    Call<String> login(@Query("email") String email, @Query("password") String password);


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

    // 원격 파일 목록 가져오기 API
    @GET("api/files/list")
    Call<List<RemoteFile>> getFileList(
            @Query("pcId") Long pcId,
            @Query("path") String path  // 최상위 경로는 "/" 또는 ""로 전달
    );
}