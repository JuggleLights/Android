package com.shiznatix.lightclubs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

class Preferences {
    private static final String LOG_TAG = "JL_" + Preferences.class.getName();

    private static final String PREFERENCES_KEY = "juggleLights";
    private static final String AUDIO_FILE_KEY = "audioFile";
    private static final String SCRIPT_FILE_KEY = "scriptFile";

    private SharedPreferences mSharedPreferences;

    Preferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    String getAudioFile() {
        return get(AUDIO_FILE_KEY);
    }

    void setAudioFile(String fileName) {
        put(AUDIO_FILE_KEY, fileName);
    }

    String getScriptFile() {
        return get(SCRIPT_FILE_KEY);
    }

    void setScriptFile(String fileName) {
        put(SCRIPT_FILE_KEY, fileName);
    }

    private String get(String key) {
        return mSharedPreferences.getString(key, null);
    }

    private void put(String key, String value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();

        Log.i(LOG_TAG, "Preferences saved: " + key + " / " + value);
    }
}
