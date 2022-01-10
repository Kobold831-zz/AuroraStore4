package com.aurora.store.check.event;

import java.util.EventListener;

public interface UpdateEventListener extends EventListener {
	void onUpdateAvailable(String mString);
	void onUpdateUnavailable();
	void onSupportAvailable();
	void onSupportUnavailable();
	void onUpdateAvailable1(String mString);
	void onUpdateUnavailable1();
	void onConnectionError();
}
