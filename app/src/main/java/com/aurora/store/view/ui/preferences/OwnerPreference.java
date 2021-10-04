package com.aurora.store.view.ui.preferences;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.R;

public class OwnerPreference extends PreferenceFragmentCompat {

    private DevicePolicyManager mDevicePolicyManager;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_owner, rootKey);

        mDevicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        Preference disableOwner = findPreference("pref_owner_disable");

        disableOwner.setOnPreferenceClickListener(preference -> {
            mDevicePolicyManager.clearDeviceOwnerApp(getActivity().getPackageName());
            getActivity().finishAffinity();
            return false;
        });
    }
}