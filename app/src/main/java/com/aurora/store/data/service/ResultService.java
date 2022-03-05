package com.aurora.store.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.aurora.store.R;
import com.aurora.store.data.event.InstallerEvent;

import org.greenrobot.eventbus.EventBus;

public class ResultService extends Service {

    protected IInstallResult.Stub mIInstallResultStub = new IInstallResult.Stub() {
        @Override
        public void InstallSuccess(String packageName) {
            EventBus.getDefault().post(new InstallerEvent.Success(packageName, getString(R.string.installer_status_success)));
        }

        @Override
        public void InstallFailure(String packageName, String errorString) {
            EventBus.getDefault().post(new InstallerEvent.Cancelled(packageName, errorString));
        }

        @Override
        public void InstallError(String packageName, String errorString, String extra) {
            EventBus.getDefault().post(new InstallerEvent.Failed(packageName, errorString, extra));
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mIInstallResultStub;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}