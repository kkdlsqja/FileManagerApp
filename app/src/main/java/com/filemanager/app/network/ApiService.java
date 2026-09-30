package com.filemanager.app.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/auth/signup")
    Call<String> signup(@Body AuthRequest request);

    @POST("api/auth/login")
    Call<String> login(@Body AuthRequest request);

    @GET("api/auth/verify")
    Call<String> verifyToken(
            @Header("Authorization") String authorization
    );

    @POST("api/auth/logout")
    Call<String> logout(
            @Header("Authorization") String authorization
    );

    @POST("api/pc/register")
    Call<PcDevice> registerPc(
            @Header("Authorization") String authorization,
            @Query("pcName") String pcName,
            @Query("pcIdentifier") String pcIdentifier
    );

    @GET("api/pc/list")
    Call<List<PcDevice>> getPcList(
            @Header("Authorization") String authorization
    );

    @GET("api/pc/connect")
    Call<String> connectPc(
            @Header("Authorization") String authorization,
            @Query("pcId") Long pcId
    );

    @GET("api/files/list")
    Call<List<RemoteFile>> getFileList(
            @Header("Authorization") String authorization,
            @Query("pcId") Long pcId,
            @Query("path") String path
    );

    @GET("api/files/search")
    Call<List<RemoteFile>> searchFiles(
            @Header("Authorization") String authorization,
            @Query("pcId") Long pcId,
            @Query("query") String query
    );

    @POST("api/files/move")
    Call<String> moveFile(
            @Header("Authorization") String authorization,
            @Query("pcId") Long pcId,
            @Query("sourcePath") String sourcePath,
            @Query("destinationPath") String destinationPath
    );

    @GET("api/files/history")
    Call<List<FileOperationLogItem>> getFileHistory(
            @Header("Authorization") String authorization,
            @Query("pcId") Long pcId
    );
}
