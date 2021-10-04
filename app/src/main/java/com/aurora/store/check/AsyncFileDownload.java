package com.aurora.store.check;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;

import com.aurora.store.check.event.UpdateEventListener;
import com.aurora.store.check.event.UpdateEventListenerList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class AsyncFileDownload extends AsyncTask<String, Void, Boolean> {
	private final UpdateEventListenerList updateListeners;

	@SuppressLint("StaticFieldLeak")
	public Activity owner;
	private final int BUFFER_SIZE = 1024;

	private final String urlString;
	private final File outputFile;
	private FileOutputStream fileOutputStream;
	private BufferedInputStream bufferedInputStream;

	private int totalByte = 0;
	private int currentByte = 0;

	private final byte[] buffer = new byte[BUFFER_SIZE];

	public AsyncFileDownload(Activity activity, String url, File oFile) {
		updateListeners = new UpdateEventListenerList();
		updateListeners.addEventListener((UpdateEventListener) activity);
		owner = activity;
		urlString = url;
		outputFile = oFile;
	}

	@Override
	protected Boolean doInBackground(String... url) {
		try{
			connect();
		}catch(IOException e){
			return null;
		}

		if(isCancelled()){
			return false;
		}
		if (bufferedInputStream !=  null){
			try{
				int len;
				while((len = bufferedInputStream.read(buffer)) != -1){
					fileOutputStream.write(buffer, 0, len);
					currentByte += len;
					if(isCancelled()){
						break;
					}
				}
			}catch(IOException e){
				return false;
			}
		}

		try{
			close();
		}catch(IOException ignored){
		}
		return true;
	}

	@Override
	protected void onPreExecute(){
	}

	@Override
	protected void onPostExecute(Boolean result){
		if (result != null) {
			updateListeners.downloadCompleteNotify();
		}else {
			updateListeners.downloadErrorNotify();
		}
	}

	@Override
	protected void onProgressUpdate(Void... progress){
	}

	private void connect() throws IOException
	{
		URL url = new URL(urlString);
		URLConnection urlConnection = url.openConnection();
		int TIMEOUT_READ = 5000;
		urlConnection.setReadTimeout(TIMEOUT_READ);
		int TIMEOUT_CONNECT = 30000;
		urlConnection.setConnectTimeout(TIMEOUT_CONNECT);
		InputStream inputStream = urlConnection.getInputStream();
		bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
		fileOutputStream = new FileOutputStream(outputFile);

		totalByte = urlConnection.getContentLength();
		currentByte = 0;
	}

	private void close() throws IOException
	{
		fileOutputStream.flush();
		fileOutputStream.close();
		bufferedInputStream.close();
	}

	public int getLoadedBytePercent()
	{
		if(totalByte <= 0){
			return 0;
		}
		return (int)Math.floor(100 * currentByte/totalByte);
	}

}