package com.aurora.store.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Common {

    public static Intent BIND_CUSTOMIZE_TOOL = new Intent("com.saradabar.cpadcustomizetool.data.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool");
    public static String DOWNLOAD_FILE_URL;

    /* データ管理 */
    public static void SET_SETTINGS_FLAG(boolean flag, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("settings", flag).apply();
    }

    public static boolean GET_SETTINGS_FLAG(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings", false);
    }

    public static void SET_UPDATE_MODE(Context context, int i) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("update_mode", i).apply();
    }

    public static int GET_UPDATE_MODE(Context context) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt("update_mode", 0);
    }
}