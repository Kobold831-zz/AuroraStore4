package com.aurora.store.check.event;

import java.util.EventListener;

public interface UpdateEventListener extends EventListener {
	void onUpdateApkDownloadComplete();
	void onUpdateAvailable(String d);
	void onUpdateUnavailable();
	void onSupportAvailable();
	void onSupportUnavailable();
	void onUpdateAvailable1(String d);
	void onUpdateUnavailable1();
	void onDownloadError();
}
