package com.aurora.store.data.event;

import com.aurora.store.data.event.UpdateEventListener;

import java.util.HashSet;
import java.util.Set;

public class UpdateEventListenerList {

	private final Set<UpdateEventListener> listeners = new HashSet<>();

	public void addEventListener(UpdateEventListener l) {
		listeners.add(l);
	}

	public void downloadCompleteNotify() {
		for (UpdateEventListener listener : listeners) listener.onUpdateApkDownloadComplete();
	}

	public void updateAvailableNotify(String mString) {
		for (UpdateEventListener listener : listeners) listener.onUpdateAvailable(mString);
	}

	public void updateUnavailableNotify() {
		for (UpdateEventListener listener : listeners) listener.onUpdateUnavailable();
	}

	public void supportAvailableNotify() {
		for (UpdateEventListener listener : listeners) listener.onSupportAvailable();
	}

	public void supportUnavailableNotify() {
		for (UpdateEventListener listener : listeners) listener.onSupportUnavailable();
	}

	public void connectionErrorNotify() {
		for (UpdateEventListener listener : listeners) listener.onConnectionError();
	}

	public void downloadErrorNotify() {
		for (UpdateEventListener listener : listeners) listener.onDownloadError();
	}
}