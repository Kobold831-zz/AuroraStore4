package com.aurora.store;

import static com.aurora.store.Common.GET_SETTINGS_FLAG;
import static com.aurora.store.Common.SET_SETTINGS_FLAG;
import static com.aurora.store.Common.Variable.REQUEST_UPDATE;
import static com.aurora.store.Common.Variable.SETTINGS_COMPLETED;
import static com.aurora.store.Common.Variable.SETTINGS_NOT_COMPLETED;
import static com.aurora.store.Common.Variable.SUPPORT_CHECK_URL;
import static com.aurora.store.Common.Variable.UPDATE_CHECK_URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.RequiresApi;

import com.aurora.store.check.AsyncFileDownload;
import com.aurora.store.check.Checker;
import com.aurora.store.check.ProgressHandler;
import com.aurora.store.check.Updater;
import com.aurora.store.check.event.UpdateEventListener;
import com.aurora.store.view.ui.onboarding.OnboardingActivity;

import java.io.File;

public class StartCheckActivity extends Activity implements UpdateEventListener {

    private DevicePolicyManager mDevicePolicyManager;
    private Handler handler;
    private Updater updater;
    private ProgressHandler progressHandler;
    private AsyncFileDownload asyncfiledownload;
    private ProgressDialog progress, loading;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        handler = new Handler();
        /* ネットワークチェック */
        if (isNetWork()) {
            UpdateCheck();
        } else {
            netWorkError();
        }
    }

    /* ネットワークの接続を確認 */
    private boolean isNetWork() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /* ネットワークエラー */
    private void netWorkError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage("通信エラーが発生しました\nネットワークに接続してください")
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                .show();
    }

    private void UpdateCheck() {
        updater = new Updater(this, UPDATE_CHECK_URL, 1);
        updater.updateCheck();
        showLoadingDialog();
    }

    private void checkSupport() {
        Checker checker = new Checker(this, SUPPORT_CHECK_URL);
        checker.supportCheck();
    }

    @Override
    public void onUpdateApkDownloadComplete() {
        handler.post(() -> updater.installApk(this));
    }

    @Override
    public void onUpdateAvailable(String d) {
    }

    @Override
    public void onUpdateUnavailable() {

    }

    public void onSupportAvailable() {
        cancelLoadingDialog();
        showSupportDialog();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onSupportUnavailable() {
        cancelLoadingDialog();
        checkModel();
    }

    @Override
    public void onUpdateAvailable1(String d) {
        cancelLoadingDialog();
        showUpdateDialog(d);
    }

    @Override
    public void onUpdateUnavailable1() {
        checkSupport();
    }

    @Override
    public void onDownloadError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage("エラーが発生しました")
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finish())
                .show();
    }

    private void showUpdateDialog(String d) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage("アップデートがあります\nアップデートしてください\n\n更新情報：\n" + d)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    progressHandler = new ProgressHandler();
                    initFileLoader();
                    showDialog(0);
                    progressHandler.progressDialog = progress;
                    progressHandler.asyncfiledownload = asyncfiledownload;

                    if (progress != null && asyncfiledownload != null) {
                        progress.setProgress(0);
                        progressHandler.sendEmptyMessage(0);
                    }
                })
                .show();
    }

    private void initFileLoader() {
        File mkdir = new File(getExternalCacheDir().getPath());
        File outputFile = new File(new File(getExternalCacheDir(), "update.apk").getPath());
        mkdir.mkdir();
        asyncfiledownload = new AsyncFileDownload(this, Common.Variable.DOWNLOAD_FILE_URL, outputFile);
        asyncfiledownload.execute();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 0) {
            progress = new ProgressDialog(this);
            progress.setTitle("アプリの更新");
            progress.setMessage("アップデートファイルをサーバーからダウンロード中・・・");
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        }
        return progress;
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage("このアプリは現在使用できません")
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                .show();
    }

    private void showLoadingDialog() {
            loading = ProgressDialog.show(this, "", "通信中です・・・", true);
    }

    private void cancelLoadingDialog() {
        if (loading != null) loading.cancel();
    }

    /* 端末チェック */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void checkModel() {
        switch (Build.MODEL) {
            case "TAB-A03-BR3":
            case "TAB-A04-BR3":
            case "TAB-A05-BD":
                if (!mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                    AlertDialog.Builder d = new AlertDialog.Builder(this);
                    d.setCancelable(false);
                    d.setTitle(R.string.dialog_title_common_error);
                    d.setMessage("DeviceOwnerではありません\nこのアプリは使用できません");
                    d.setPositiveButton("OK", (dialog, which) -> finish());
                    d.show();
                } else {
                    if (GET_SETTINGS_FLAG(this) == SETTINGS_NOT_COMPLETED) {
                        startCheck();
                    } else {
                        Intent intent = new Intent(this, OnboardingActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
                break;
            default:
                errorNotTab3OrNEO();
                break;
        }
    }

    /* 端末チェックエラー */
    private void errorNotTab3OrNEO() {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage(R.string.dialog_error_not_pad2)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                .show();
    }

    /* 初回起動お知らせ */
    public void startCheck() {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setCancelable(false)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_notice_start)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    SET_SETTINGS_FLAG(SETTINGS_COMPLETED, this);
                    Intent intent = new Intent(this, OnboardingActivity.class);
                    startActivity(intent);
                    finish();
                });
        d.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UPDATE) {
            finish();
        }
    }
}