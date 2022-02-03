package com.aurora.store;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;

public class BlockerActivity extends Activity {

    private ComponentName administratorComponent;
    private DevicePolicyManager mDevicePolicyManager;

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.block_un_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        administratorComponent = getAdministratorComponent(this);
        mDevicePolicyManager = (DevicePolicyManager)this.getSystemService("device_policy");

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
            mDevicePolicyManager.setUninstallBlocked(administratorComponent, selectPackage.replace("package:", ""), !mDevicePolicyManager.isUninstallBlocked(administratorComponent, selectPackage.replace("package:", "")));
            /* listviewの更新 */
            listView.invalidateViews();
        });

        final AppListAdapter appListAdapter = new AppListAdapter(this, dataList);

        /* ボタンが押されたならスイッチ一括変更 */
        /* 無効 */
        unDisableButton.setOnClickListener(v -> {
            for (AppData appData : dataList) {
                mDevicePolicyManager.setUninstallBlocked(administratorComponent, appData.packName, false);
            }
            ((Switch) AppListAdapter.view.findViewById(R.id.un_switch)).setChecked(false);
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* 有効 */
        unEnableButton.setOnClickListener(v -> {
            for (AppData appData : dataList) {
                mDevicePolicyManager.setUninstallBlocked(administratorComponent, appData.packName, true);
            }
            ((Switch) AppListAdapter.view.findViewById(R.id.un_switch)).setChecked(true);
            /* listviewの更新 */
            listView.invalidateViews();
        });
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

        @SuppressLint("StaticFieldLeak")
        public static View view;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.block_un_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            administratorComponent = getAdministratorComponent(context);
            dpm = (DevicePolicyManager)context.getSystemService(DEVICE_POLICY_SERVICE);
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

            ((Switch) convertView.findViewById(R.id.un_switch)).setChecked(dpm.isUninstallBlocked(administratorComponent, data.packName));

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView textLabel;
        ImageView imageIcon;
    }

    private static ComponentName getAdministratorComponent(Context context) {
        return new ComponentName(context, com.aurora.store.data.receiver.AdministratorReceiver.class);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
