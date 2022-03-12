package com.aurora.store.view.ui.preferences;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.R;
import com.aurora.store.util.Common;
import com.aurora.store.view.epoxy.views.UpdateModeView;
import com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService;

import java.util.ArrayList;
import java.util.List;

public class OtherPreference extends PreferenceFragmentCompat {

    IDeviceOwnerService mDeviceOwnerService;
    private DevicePolicyManager mDevicePolicyManager;

    Preference preferenceDisableOwner,
            preferenceNowSetOwnerApp,
            preferenceUpdateMode;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_other, rootKey);

        mDevicePolicyManager = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        preferenceDisableOwner = findPreference("pref_other_owner_disable");
        preferenceNowSetOwnerApp = findPreference("pref_other_now_owner_package");
        preferenceUpdateMode = findPreference("pref_other_update_mode");

        preferenceDisableOwner.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(getString(R.string.dialog_clear_device_owner))
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        mDevicePolicyManager.clearDeviceOwnerApp(requireActivity().getPackageName());
                        requireActivity().finishAffinity();
                    })
                    .setNegativeButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preferenceUpdateMode.setOnPreferenceClickListener(preference -> {
            View v = requireActivity().getLayoutInflater().inflate(R.layout.layout_update_list, null);
            List<String> list = new ArrayList<>();
            list.add("ADB");
            list.add("デバイスオーナー");
            list.add("CPad Customize Tool");
            List<UpdateModeView.AppData> dataList = new ArrayList<>();
            int i = 0;
            for (String str : list) {
                UpdateModeView.AppData data = new UpdateModeView.AppData();
                data.label = str;
                data.updateMode = i;
                dataList.add(data);
                i++;
            }
            ListView listView = v.findViewById(R.id.update_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new UpdateModeView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                switch (position) {
                    case 0:
                        Common.SET_UPDATE_MODE(requireActivity(), (int) id);
                        listView.invalidateViews();
                        break;
                    case 1:
                        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
                            Common.SET_UPDATE_MODE(requireActivity(), (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                    case 2:
                        if (bindDeviceOwnerService()) {
                            Common.SET_UPDATE_MODE(requireActivity(), 2);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                }
            });
            new AlertDialog.Builder(requireActivity())
                    .setView(v)
                    .setTitle(getString(R.string.dialog_title_select_mode))
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        if (getNowOwnerPackage() != null) {
            preferenceNowSetOwnerApp.setSummary("デバイスオーナーは" + getNowOwnerPackage() + "に設定されています");
        } else preferenceNowSetOwnerApp.setSummary("デバイスオーナーはデバイスに設定されていません");

        if (!mDevicePolicyManager.isDeviceOwnerApp(requireActivity().getPackageName())) {
            preferenceDisableOwner.setEnabled(false);
            preferenceDisableOwner.setSelectable(false);
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDeviceOwnerService = null;
        }
    };

    public boolean bindDeviceOwnerService() {
        try {
            return requireActivity().bindService(Common.BIND_CUSTOMIZE_TOOL, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) {
            return false;
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