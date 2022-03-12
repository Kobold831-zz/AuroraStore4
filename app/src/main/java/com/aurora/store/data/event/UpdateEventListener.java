package com.aurora.store.data.event;

import java.util.EventListener;

public interface UpdateEventListener extends EventListener {
	void onUpdateApkDownloadComplete();
	void onUpdateAvailable(String mString);
	void onUpdateUnavailable();
	void onSupportAvailable();
	void onSupportUnavailable();
	void onDownloadError();
	void onConnectionError();
}