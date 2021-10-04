package com.aurora.store.check.event;

import java.util.HashSet;
import java.util.Set;


public class UpdateEventListenerList {
	private final Set<UpdateEventListener> listeners = new HashSet<>();

	public void addEventListener(UpdateEventListener l) {
		listeners.add(l);
	}

	public void downloadCompleteNotify() {
		for (UpdateEventListener listener : listeners) {
			listener.onUpdateApkDownloadComplete();
		}
	}

	public void updateAvailableNotify(String d) {
		for (UpdateEventListener listener : listeners) {
			listener.onUpdateAvailable(d);
		}
	}

	public void updateUnavailableNotify() {
		for (UpdateEventListener listener : listeners) {
			listener.onUpdateUnavailable();
		}
	}

	public void updateAvailableNotify1(String d) {
		for (UpdateEventListener listener : listeners) {
			listener.onUpdateAvailable1(d);
		}
	}

	public void updateUnavailableNotify1() {
		for (UpdateEventListener listener : listeners) {
			listener.onUpdateUnavailable1();
		}
	}

	public void downloadErrorNotify() {
		for (UpdateEventListener listener : listeners) {
			listener.onDownloadError();
		}
	}

	public void supportAvailableNotify() {
		for (UpdateEventListener listener : listeners) {
			listener.onSupportAvailable();
		}
	}

	public void supportUnavailableNotify() {
		for (UpdateEventListener listener : listeners) {
			listener.onSupportUnavailable();
		}
	}
}
