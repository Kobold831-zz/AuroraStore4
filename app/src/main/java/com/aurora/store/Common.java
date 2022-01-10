package com.aurora.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public final class Common {

    public static final class Variable {

        public static final int REQUEST_UPDATE = 10;
        public static final boolean SETTINGS_NOT_COMPLETED = false;
        public static final boolean SETTINGS_COMPLETED = true;

        public static Toast toast;

        public static String DOWNLOAD_FILE_URL;
        public static String UPDATE_CHECK_URL = "https://raw.githubusercontent.com/Kobold831/Server/main/AuroraStore_Update.xml";
        public static String SUPPORT_CHECK_URL = "https://raw.githubusercontent.com/Kobold831/Server/main/AuroraStore_Support.xml";
        public static String UPDATE_INFO_URL = "https://docs.google.com/document/d/19GmLgF7rf6WblrY8MCJjqTKbSwLqMRzH_MyvMR38d2o";
        public static String UPDATE_URL = "https://is.gd/GLTPxY";
    }

    /* データ管理 */
    public static void SET_SETTINGS_FLAG(boolean FLAG, Context context) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("settings", FLAG).apply();
    }

    public static boolean GET_SETTINGS_FLAG(Context context) {
        boolean bl;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        bl = sp.getBoolean("settings", false);
        return bl;
    }
}

