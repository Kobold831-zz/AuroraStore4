package com.aurora.store;

import static com.aurora.store.Common.*;
import static com.aurora.store.Common.Variable.*;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aurora.store.check.Checker;
import com.aurora.store.check.Updater;
import com.aurora.store.check.event.UpdateEventListener;
import com.aurora.store.view.ui.onboarding.OnboardingActivity;
import com.aurora.store.view.ui.sheets.TOSSheet;

public class StartCheckActivity extends AppCompatActivity implements UpdateEventListener {

    private DevicePolicyManager mDevicePolicyManager;
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
                .setIcon(R.drawable.alert)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage(R.string.dialog_wifi_error)
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
        new Updater(this, UPDATE_CHECK_URL, 1).updateCheck();
    }

    private void supportCheck() {
        new Checker(this, SUPPORT_CHECK_URL).supportCheck();
    }

    @Override
    public void onUpdateAvailable(String string) {
    }

    @Override
    public void onUpdateUnavailable() {
    }

    public void onSupportAvailable() {
        cancelLoadingDialog();
        showSupportDialog();
    }

    public void onSupportUnavailable() {
        cancelLoadingDialog();
        checkModel();
    }

    @Override
    public void onUpdateAvailable1(String string) {
        cancelLoadingDialog();
        showUpdateDialog(string);
    }

    @Override
    public void onUpdateUnavailable1() {
        supportCheck();
    }

    @Override
    public void onConnectionError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setIcon(R.drawable.alert)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage(R.string.dialog_connection_error)
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

    private void showUpdateDialog(String string) {
        View view = getLayoutInflater().inflate(R.layout.update_dialog, null);
        TextView mTextView = view.findViewById(R.id.update_information);
        mTextView.setText(string);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(UPDATE_INFO_URL)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                if (toast != null) toast.cancel();
                toast = Toast.makeText(this, R.string.toast_unknown_activity, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_update)
                        .setMessage(R.string.dialog_update_caution)
                        .setPositiveButton(R.string.dialog_common_yes, (dialog2, which2) -> {
                            try {
                                startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(UPDATE_URL)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), REQUEST_UPDATE);
                            } catch (ActivityNotFoundException ignored) {
                                if (toast != null) toast.cancel();
                                toast = Toast.makeText(this, R.string.toast_unknown_activity, Toast.LENGTH_SHORT);
                                toast.show();
                                if (isNetWork()) {
                                    showLoadingDialog();
                                    supportCheck();
                                } else netWorkError();
                            }
                        })
                        .show())
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setIcon(R.drawable.alert)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage(R.string.dialog_not_use)
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
        loadingDialog = ProgressDialog.show(this, "", "通信中です・・・", true);
        loadingDialog.show();
    }

    private void cancelLoadingDialog() {
        try {
            if (loadingDialog != null) loadingDialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    /* 端末チェック */
    public void checkModel() {
        if (!"TAB-A05-BD".equals(Build.MODEL)) {
            errorNotNEO();
            return;
        }
        if (!mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setIcon(R.drawable.alert)
                    .setTitle(R.string.dialog_title_common_error)
                    .setMessage(R.string.dialog_not_device_owner)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                    .show();
        } else {
            if (GET_SETTINGS_FLAG(this) == SETTINGS_NOT_COMPLETED) {
                startCheck();
            } else {
                startActivity(new Intent(this, OnboardingActivity.class));
                finish();
            }
        }
    }

    /* 端末チェックエラー */
    private void errorNotNEO() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
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
        TOSSheet tos = TOSSheet.newInstance();
        tos.setCancelable(false);
        tos.show(getSupportFragmentManager(), "");

        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setIcon(R.drawable.alert)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_notice_start)
                .setPositiveButton(R.string.dialog_agree, (dialog, which) -> {
                    SET_SETTINGS_FLAG(SETTINGS_COMPLETED, this);
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UPDATE) {
            finishAndRemoveTask();
        }
    }
}