package com.aurora.store;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.service.IDeviceOwnerService;

import java.util.ArrayList;
import java.util.List;

public class BlockerActivity extends Activity {

    private ComponentName administratorComponent;
    private boolean result;

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.block_un_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        administratorComponent = getAdministratorComponent(this);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager)this.getSystemService("device_policy");

        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            result = true;
        } else {
            if (bindDeviceOwnerService()) {
                result = false;
            } else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setIcon(R.drawable.alert)
                        .setMessage("エラーが発生しました")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                        .show();
            }
        }

        final PackageManager pm = getPackageManager();
        final List<ApplicationInfo> installedAppList = pm.getInstalledApplications(0);

        final List<AppData> dataList = new ArrayList<>();
        for (ApplicationInfo app : installedAppList) {
            /* ユーザーアプリか確認 */
            if (app.sourceDir.startsWith("/data/app/")) {
                AppData data = new AppData();
                data.label = app.loadLabel(pm).toString();
                data.icon = app.loadIcon(pm);
                data.packName = app.packageName;
                dataList.add(data);
            }
        }

        final ListView listView = findViewById(R.id.un_list);
        Button unDisableButton = findViewById(R.id.un_button_disable);
        Button unEnableButton = findViewById(R.id.un_button_enable);
        listView.setAdapter(new AppListAdapter(this, dataList));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppData item = dataList.get(position);
            String selectPackage = Uri.fromParts("package", item.packName, null).toString();
            if (result) {
                devicePolicyManager.setUninstallBlocked(administratorComponent, selectPackage.replace("package:", ""), !devicePolicyManager.isUninstallBlocked(administratorComponent, selectPackage.replace("package:", "")));
                /* listviewの更新 */
                listView.invalidateViews();
            } else {
                bindService(new Intent("com.saradabar.cpadcustomizetool.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool"), new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        IDeviceOwnerService mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(service);
                        try {
                            mDeviceOwnerService.setUninstallBlocked(selectPackage.replace("package:", ""), !mDeviceOwnerService.isUninstallBlocked(selectPackage.replace("package:", "")));
                            /* listviewの更新 */
                            listView.invalidateViews();
                        } catch (RemoteException ignored) {
                        }
                        unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        unbindService(this);
                    }
                }, Context.BIND_AUTO_CREATE);
            }
        });

        /* ボタンが押されたならスイッチ一括変更 */
        /* 無効 */
        unDisableButton.setOnClickListener(v -> {
            for (AppData appData : dataList) {
                if (result) {
                    devicePolicyManager.setUninstallBlocked(administratorComponent, appData.packName, false);
                } else {
                    bindService(new Intent("com.saradabar.cpadcustomizetool.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool"), new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            IDeviceOwnerService mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(service);
                            try {
                                mDeviceOwnerService.setUninstallBlocked(appData.packName, false);
                            } catch (RemoteException ignored) {
                            }
                            unbindService(this);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            unbindService(this);
                        }
                    }, Context.BIND_AUTO_CREATE);
                }
            }
            ((Switch) AppListAdapter.view.findViewById(R.id.un_switch)).setChecked(false);
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* 有効 */
        unEnableButton.setOnClickListener(v -> {
            for (AppData appData : dataList) {
                if (result) {
                    devicePolicyManager.setUninstallBlocked(administratorComponent, appData.packName, true);
                } else {
                    bindService(new Intent("com.saradabar.cpadcustomizetool.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool"), new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            IDeviceOwnerService mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(service);
                            try {
                                mDeviceOwnerService.setUninstallBlocked(appData.packName, true);
                            } catch (RemoteException ignored) {
                            }
                            unbindService(this);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            unbindService(this);
                        }
                    }, Context.BIND_AUTO_CREATE);
                }
            }
            ((Switch) AppListAdapter.view.findViewById(R.id.un_switch)).setChecked(true);
            /* listviewの更新 */
            listView.invalidateViews();
        });
    }

    public boolean bindDeviceOwnerService() {
        return bindService(new Intent("com.saradabar.cpadcustomizetool.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool"), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                IDeviceOwnerService mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(service);
                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                unbindService(this);
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private static class AppData {
        String label;
        Drawable icon;
        String packName;
    }

    private static class AppListAdapter extends ArrayAdapter<AppData> {

        private final LayoutInflater mInflater;
        private final DevicePolicyManager dpm;
        private final ComponentName administratorComponent;
        private boolean result;

        @SuppressLint("StaticFieldLeak")
        public static View view;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.block_un_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            administratorComponent = getAdministratorComponent(context);
            dpm = (DevicePolicyManager)context.getSystemService(DEVICE_POLICY_SERVICE);
            if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                result = true;
            } else {
                if (bindDeviceOwnerService()) {
                    result = false;
                }
            }
            addAll(dataList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.block_un_item, parent, false);
                holder.textLabel = convertView.findViewById(R.id.un_label);
                holder.imageIcon = convertView.findViewById(R.id.un_icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            view = convertView;

            final AppData data = getItem(position);

            holder.textLabel.setText(data.label);
            holder.imageIcon.setImageDrawable(data.icon);

            View finalConvertView = convertView;
            if (result) {
                ((Switch)convertView.findViewById(R.id.un_switch)).setChecked(dpm.isUninstallBlocked(administratorComponent, data.packName));
            } else {
                getContext().bindService(new Intent("com.saradabar.cpadcustomizetool.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool"), new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        IDeviceOwnerService mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(service);
                        try {
                            ((Switch) finalConvertView.findViewById(R.id.un_switch)).setChecked(mDeviceOwnerService.isUninstallBlocked(data.packName));
                        } catch (RemoteException ignored) {
                        }
                        getContext().unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        getContext().unbindService(this);
                    }
                }, Context.BIND_AUTO_CREATE);
            }
            return convertView;
        }

        public boolean bindDeviceOwnerService() {
            return getContext().bindService(new Intent("com.saradabar.cpadcustomizetool.service.DeviceOwnerService").setPackage("com.saradabar.cpadcustomizetool"), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    IDeviceOwnerService mDeviceOwnerService = IDeviceOwnerService.Stub.asInterface(service);
                    getContext().unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    getContext().unbindService(this);
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }

    private static class ViewHolder {
        TextView textLabel;
        ImageView imageIcon;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static ComponentName getAdministratorComponent(Context context) {
        return new ComponentName(context, com.aurora.store.data.receiver.AdministratorReceiver.class);
    }
}