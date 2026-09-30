package com.filemanager.app.network;

import android.content.Context;

import com.filemanager.app.ServerSettings;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static Context applicationContext;
    private static Retrofit retrofit;
    private static String currentBaseUrl;

    private RetrofitClient() {
    }

    public static synchronized void initialize(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public static synchronized Retrofit getClient() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                    "RetrofitClient가 초기화되지 않았습니다. "
                            + "AndroidManifest.xml의 Application 설정을 확인해 주세요."
            );
        }

        String configuredBaseUrl =
                ServerSettings.getBaseUrl(applicationContext);

        if (retrofit == null || !configuredBaseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = configuredBaseUrl;

            retrofit = new Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    // ScalarsConverterFactory를 GsonConverterFactory보다 먼저 둡니다.
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}
