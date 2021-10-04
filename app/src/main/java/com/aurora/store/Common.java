package com.aurora.store;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public final class Common {

    public static final class Variable {

        public static final int REQUEST_UPDATE = 10;
        public static final int SETTINGS_NOT_COMPLETED = 0;
        public static final int SETTINGS_COMPLETED = 1;

        public static String DOWNLOAD_FILE_URL;
        public static String UPDATE_CHECK_URL = "https://raw.githubusercontent.com/Kobold831/Server/main/update.xml";
        public static String SUPPORT_CHECK_URL = "https://raw.githubusercontent.com/Kobold831/Server/main/support.xml";
    }

    /* データ管理 */
    public static void SET_SETTINGS_FLAG(int FLAG, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("SETTINGS_FLAG", FLAG).apply();
    }

    public static int GET_SETTINGS_FLAG(Context context) {
        int SETTINGS_FLAG;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SETTINGS_FLAG = sp.getInt("SETTINGS_FLAG", 0);
        return SETTINGS_FLAG;
    }
}

