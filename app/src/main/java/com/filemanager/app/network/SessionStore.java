package com.filemanager.app.network;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionStore {

    private static final String PREFERENCES = "folderhelper_session";
    private static final String KEY_TOKEN = "access_token";

    private SessionStore() {
    }

    public static void saveToken(Context context, String token) {
        preferences(context)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply();
    }

    public static String getAuthorizationHeader(Context context) {
        String token = preferences(context).getString(KEY_TOKEN, null);

        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        return "Bearer " + token.trim();
    }

    public static void clear(Context context) {
        preferences(context)
                .edit()
                .remove(KEY_TOKEN)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
