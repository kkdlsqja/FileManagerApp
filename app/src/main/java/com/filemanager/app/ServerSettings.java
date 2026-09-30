package com.filemanager.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

public final class ServerSettings {

    private static final String PREFERENCES_NAME = "server_settings";
    private static final String KEY_BASE_URL = "base_url";

    // Android 에뮬레이터에서 개발 PC의 localhost에 접속하는 기본 주소
    private static final String DEFAULT_BASE_URL =
            "http://10.0.2.2:8080/";

    private ServerSettings() {
    }

    public static String getBaseUrl(Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        String savedUrl = preferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL);

        try {
            return normalizeBaseUrl(savedUrl);
        } catch (IllegalArgumentException exception) {
            return DEFAULT_BASE_URL;
        }
    }

    public static String saveBaseUrl(Context context, String input) {
        String normalizedUrl = normalizeBaseUrl(input);

        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        preferences.edit()
                .putString(KEY_BASE_URL, normalizedUrl)
                .apply();

        return normalizedUrl;
    }

    public static String normalizeBaseUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("서버 주소를 입력해 주세요.");
        }

        String address = input.trim();

        if (!address.startsWith("http://")
                && !address.startsWith("https://")) {
            address = "http://" + address;
        }

        if (!address.endsWith("/")) {
            address += "/";
        }

        Uri uri = Uri.parse(address);
        String scheme = uri.getScheme();

        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException(
                    "주소는 http:// 또는 https://로 시작해야 합니다."
            );
        }

        if (TextUtils.isEmpty(uri.getHost())) {
            throw new IllegalArgumentException(
                    "올바른 서버 주소를 입력해 주세요. 예: http://192.168.0.15:8080/"
            );
        }

        if (uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "서버 주소에 계정 정보, 쿼리 또는 # 문자를 넣을 수 없습니다."
            );
        }

        int port = uri.getPort();
        if (port == 0 || port > 65535) {
            throw new IllegalArgumentException("포트 번호가 올바르지 않습니다.");
        }

        return address;
    }
}
