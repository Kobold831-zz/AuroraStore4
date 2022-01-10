package com.aurora.store.check;

import static com.aurora.store.Common.Variable.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;

import com.aurora.store.check.event.UpdateEventListener;
import com.aurora.store.check.event.UpdateEventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Updater {

    private final int code;
    private int currentVersionCode, latestVersionCode;
    private String latestDescription;
    private final String updateCheckUrl;
    private final UpdateEventListenerList updateListeners;
    private final Activity activity;

    public Updater(Activity mActivity, String url, int i) {
        updateCheckUrl = url;
        activity = mActivity;
        code = i;
        updateListeners = new UpdateEventListenerList();
        updateListeners.addEventListener((UpdateEventListener) activity);
    }

    private int updateAvailableCheck() {

        try {
            getCurrentVersionInfo();
            getLatestVersionInfo();

            if (latestVersionCode == -99) return -1;

            if (currentVersionCode < latestVersionCode) return 1;
            else return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void getCurrentVersionInfo() throws Exception {
        currentVersionCode = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_META_DATA).versionCode;
    }

    private void getLatestVersionInfo() {

        HashMap<String, String> map = parseUpdateXml(updateCheckUrl);

        if (map != null) {
            latestVersionCode = Integer.parseInt(map.get("versionCode"));
            DOWNLOAD_FILE_URL = map.get("url");
            latestDescription = map.get("description");
        } else latestVersionCode = -99;
    }

    public void updateCheck() {
        new updateCheckTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class updateCheckTask extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... arg0) {
            return updateAvailableCheck();
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case -1:
                    updateListeners.connectionErrorNotify();
                    break;
                case 0:
                    if (code == 0) updateListeners.updateUnavailableNotify();
                    if (code == 1) updateListeners.updateUnavailableNotify1();
                    break;
                case 1:
                    if (code == 0) updateListeners.updateAvailableNotify(latestDescription);
                    if (code == 1) updateListeners.updateAvailableNotify1(latestDescription);
                    break;
            }
        }
    }

    public void installApk(Context context) {
        Uri dataUri = Uri.fromFile(new File(new File(context.getExternalCacheDir(), "update.apk").getPath()));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(dataUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivityForResult(intent, REQUEST_UPDATE);
    }

    private HashMap<String, String> parseUpdateXml(String url) {

        HashMap<String, String> map = new HashMap<>();
        HttpURLConnection mHttpURLConnection;

        try {
            mHttpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            mHttpURLConnection.setConnectTimeout(5000);
            InputStream is = mHttpURLConnection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            DocumentBuilderFactory document_builder_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder document_builder = document_builder_factory.newDocumentBuilder();
            Document document = document_builder.parse(bis);
            Element root = document.getDocumentElement();

            if (root.getTagName().equals("update")) {
                NodeList nodelist = root.getChildNodes();
                for (int j = 0; j < nodelist.getLength(); j++) {
                    Node node = nodelist.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String name = element.getTagName();
                        String value = element.getTextContent().trim();
                        map.put(name, value);
                    }
                }
            }
            return map;
        } catch (SocketTimeoutException | MalformedURLException ignored) {
            return null;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }
}