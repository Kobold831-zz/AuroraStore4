package com.aurora.store.view.ui.first;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aurora.Constants;
import com.aurora.extensions.ContextKt;
import com.aurora.extensions.ToastKt;
import com.aurora.store.R;
import com.aurora.store.data.connection.AsyncFileDownload;
import com.aurora.store.data.connection.Checker;
import com.aurora.store.data.connection.Updater;
import com.aurora.store.data.event.UpdateEventListener;
import com.aurora.store.data.handler.ProgressHandler;
import com.aurora.store.util.Common;
import com.aurora.store.view.epoxy.views.UpdateModeView;
import com.aurora.store.view.ui.onboarding.OnboardingActivity;
import com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StartActivity extends AppCompatActivity implements UpdateEventListener {

    IDeviceOwnerService mDeviceOwnerService;
    DevicePolicyManager mDevicePolicyManager;
    ProgressDialog loadingDialog;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        /* ネットワークチェック */
        if (!isNetWork()) {
            netWorkError();
            return;
        }
        updateCheck();
    }

    /* ネットワークの接続を確認 */
    private boolean isNetWork() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /* ネットワークエラー */
    private void netWorkError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_start_error)
                .setMessage(R.string.dialog_error_wifi)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_clear_device_owner)
                                .setPositiveButton(R.string.dialog_common_yes, (dialog2, which2) -> {
                                    mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void updateCheck() {
        showLoadingDialog();
        new Updater(this).updateCheck();
    }

    private void supportCheck() {
        new Checker(this, Constants.SUPPORT_CHECK_URL).supportCheck();
    }

    public void onSupportAvailable() {
        cancelLoadingDialog();
        showSupportDialog();
    }

    public void onSupportUnavailable() {
        cancelLoadingDialog();
        if (checkModel()) {
            if (!mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                if (bindDeviceOwnerService()) {
                    Runnable runnable = this::isDeviceOwner;
                    new Handler().postDelayed(runnable, 1000);
                } else {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setTitle(R.string.dialog_title_start_error)
                            .setMessage(R.string.dialog_error_failure_bind)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                            .show();
                }
            } else {
                if (Common.GET_SETTINGS_FLAG(this) == Constants.SETTINGS_NOT_COMPLETED) {
                    startCheck();
                } else {
                    startActivity(new Intent(this, OnboardingActivity.class));
                    finish();
                }
            }
        } else {
            errorNotNEO();
        }
    }

    @Override
    public void onDownloadError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage(R.string.dialog_error)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finishAffinity())
                .show();
    }

    public void isDeviceOwner() {
        try {
            if (mDeviceOwnerService.isDeviceOwnerApp()) {
                if (Common.GET_SETTINGS_FLAG(this) == Constants.SETTINGS_NOT_COMPLETED) {
                    startCheck();
                } else {
                    startActivity(new Intent(this, OnboardingActivity.class));
                    finish();
                }
            } else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_start_error)
                        .setMessage(R.string.dialog_error_bind_no_owner)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                        .show();
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_start_error)
                    .setMessage(getResources().getString(R.string.dialog_error) + "\n" + e.getMessage())
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                    .show();
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
            return bindService(Common.BIND_CUSTOMIZE_TOOL, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void onUpdateApkDownloadComplete() {
        new Handler().post(() -> new Updater(this).installApk());
    }

    @Override
    public void onUpdateAvailable(String string) {
        cancelLoadingDialog();
        showUpdateDialog(string);
    }

    @Override
    public void onUpdateUnavailable() {
        supportCheck();
    }

    @Override
    public void onConnectionError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_start_error)
                .setMessage(R.string.dialog_error_connection)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_clear_device_owner)
                                .setPositiveButton(R.string.dialog_common_yes, (dialog2, which2) -> {
                                    mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void showUpdateDialog(String str) {
        View view = getLayoutInflater().inflate(R.layout.update_dialog, null);
        TextView mTextView = view.findViewById(R.id.update_information);
        mTextView.setText(str);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.UPDATE_INFO_URL)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                ToastKt.toast(this, R.string.toast_unknown_activity);
            }
        });

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    AsyncFileDownload asyncFileDownload = initFileLoader();
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle(R.string.dialog_title_update);
                    progressDialog.setMessage(getString(R.string.progress_state_downloading_update_file));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_common_cancel), (dialog2, which2) -> {
                        asyncFileDownload.cancel(true);
                        finishAffinity();
                    });
                    if (!progressDialog.isShowing()) progressDialog.show();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.progressDialog = progressDialog;
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> finishAffinity())
                .setNeutralButton(R.string.dialog_title_settings, (dialog, which) -> {
                    dialog.dismiss();
                    setUpdateMode(str);
                })
                .show();
    }

    private AsyncFileDownload initFileLoader() {
        AsyncFileDownload asyncfiledownload = new AsyncFileDownload(this, Common.DOWNLOAD_FILE_URL, new File(getExternalCacheDir(), "update.apk"));
        asyncfiledownload.execute();
        return asyncfiledownload;
    }

    private void setUpdateMode(String s) {
        View v = getLayoutInflater().inflate(R.layout.layout_update_list, null);
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
        listView.setAdapter(new UpdateModeView.AppListAdapter(this, dataList));
        listView.setOnItemClickListener((parent, mView, position, id) -> {
            switch (position) {
                case 0:
                    Common.SET_UPDATE_MODE(this, (int) id);
                    listView.invalidateViews();
                    break;
                case 1:
                    if (((DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(getPackageName())) {
                        Common.SET_UPDATE_MODE(this, (int) id);
                        listView.invalidateViews();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage(getString(R.string.dialog_error_not_work_mode))
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                    break;
                case 2:
                    if (bindDeviceOwnerService()) {
                        Common.SET_UPDATE_MODE(this, 2);
                        listView.invalidateViews();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage(getString(R.string.dialog_error_not_work_mode))
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                    break;
            }
        });
        new AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(false)
                .setTitle(getString(R.string.dialog_title_select_mode))
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    dialog.dismiss();
                    showUpdateDialog(s);
                })
                .show();
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_start_error)
                .setMessage(R.string.dialog_error_not_use)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_clear_device_owner)
                                .setPositiveButton(R.string.dialog_common_yes, (dialog2, which2) -> {
                                    mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    private void showLoadingDialog() {
        loadingDialog = ProgressDialog.show(this, "", getString(R.string.progress_state_connection), true);
        loadingDialog.show();
    }

    private void cancelLoadingDialog() {
        try {
            if (loadingDialog != null) loadingDialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    /* 端末チェック */
    public boolean checkModel() {
        String[] modelName = {"TAB-A05-BD", "TAB-A05-BA1"};
        for (String string : modelName) {
            if (Objects.equals(string, Build.MODEL)) {
                return true;
            }
        }
        return false;
    }

    /* 端末チェックエラー */
    private void errorNotNEO() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_start_error)
                .setMessage(R.string.dialog_error_not_neo)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_clear_device_owner)
                                .setPositiveButton(R.string.dialog_common_yes, (dialog2, which2) -> {
                                    mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());
                                    finishAndRemoveTask();
                                })
                                .setNegativeButton(R.string.dialog_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                .show();
                    } else finishAndRemoveTask();
                })
                .show();
    }

    /* 初回起動お知らせ */
    public void startCheck() {
        View view = getLayoutInflater().inflate(R.layout.sheet_tos, null);
        Button button = view.findViewById(R.id.btn_primary);
        Button button1 = view.findViewById(R.id.btn_secondary);
        Button button2 = view.findViewById(R.id.btn_read);
        button.setEnabled(false);
        CheckBox checkBox = view.findViewById(R.id.checkbox_accept);
        checkBox.setOnCheckedChangeListener((compoundButton, bool) -> button.setEnabled(bool));
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.MyTheme);
        alertDialog.setView(view).setCancelable(false);
        AlertDialog alertDialog1 = alertDialog.create();
        alertDialog1.show();
        button.setOnClickListener(view1 -> {
            alertDialog1.dismiss();
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_notice_start)
                    .setMessage(R.string.dialog_notice_start)
                    .setPositiveButton(R.string.dialog_agree, (dialog, which) -> {

                        Common.SET_SETTINGS_FLAG(Constants.SETTINGS_COMPLETED, this);
                        startActivity(new Intent(this, OnboardingActivity.class));
                        finish();
                    })
                    .setNegativeButton(R.string.dialog_disagree, (dialog, which) -> {
                        if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setMessage(R.string.dialog_clear_device_owner)
                                    .setPositiveButton(R.string.dialog_common_yes, (dialog2, which2) -> {
                                        mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());
                                        finishAndRemoveTask();
                                    })
                                    .setNegativeButton(R.string.dialog_common_no, (dialog2, which2) -> finishAndRemoveTask())
                                    .show();
                        } else finishAndRemoveTask();
                    })
                    .show();
        });
        button1.setOnClickListener(view12 -> finishAndRemoveTask());
        button2.setOnClickListener(view13 -> ContextKt.browse(this, Constants.TOS_URL));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_UPDATE) {
            finishAndRemoveTask();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDeviceOwnerService != null) unbindService(mServiceConnection);
    }
}