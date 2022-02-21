package com.aurora.store.view.ui.preferences;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.BlockerActivity;
import com.aurora.store.R;

public class OwnerPreference extends PreferenceFragmentCompat {

    private DevicePolicyManager mDevicePolicyManager;

    Preference preferenceDisableOwner,
            preferenceBlockToUninstallSettings,
            preferenceNowSetOwnerApp;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_owner, rootKey);

        mDevicePolicyManager = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        preferenceDisableOwner = findPreference("pref_owner_disable");
        preferenceBlockToUninstallSettings = findPreference("intent_block_to_uninstall_settings");
        preferenceNowSetOwnerApp = findPreference("now_set_owner_package");

        preferenceDisableOwner.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage("DeviceOwnerを解除しますか？")
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        mDevicePolicyManager.clearDeviceOwnerApp(requireActivity().getPackageName());
                        requireActivity().finishAffinity();
                    })
                    .setNegativeButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preferenceBlockToUninstallSettings.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent(requireActivity(), BlockerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        if (getNowOwnerPackage() != null) {
            preferenceNowSetOwnerApp.setSummary("DeviceOwnerは" + getNowOwnerPackage() + "に設定されています");
        } else preferenceNowSetOwnerApp.setSummary("DeviceOwnerはデバイスに設定されていません");

        if (!mDevicePolicyManager.isDeviceOwnerApp(requireActivity().getPackageName())) {
            preferenceDisableOwner.setEnabled(false);
            preferenceDisableOwner.setSelectable(false);
        }
    }

    private String getNowOwnerPackage() {
        for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
            /* ユーザーアプリか確認 */
            if (app.sourceDir.startsWith("/data/app/")) {
                if (mDevicePolicyManager.isDeviceOwnerApp(app.packageName)) {
                    return app.loadLabel(requireActivity().getPackageManager()).toString();
                }
            }
        }
        return null;
    }
}