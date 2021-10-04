package com.aurora.store.check;

import static com.aurora.store.Common.Variable.REQUEST_UPDATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;

import com.aurora.store.Common;
import com.aurora.store.check.event.UpdateEventListener;
import com.aurora.store.check.event.UpdateEventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Updater {

    private int currentVersionCode;
    private int latestVersionCode;
    private final int code;

    private String latestDescription;
    private final String updateXmlUrl;

    private final UpdateEventListenerList updateListeners;
    private final Activity activity;

    public Updater(Activity activity, String updateXmlUrl, int code) {
        this.updateXmlUrl = updateXmlUrl;
        this.activity = activity;
        this.code = code;

        updateListeners = new UpdateEventListenerList();
        updateListeners.addEventListener((UpdateEventListener) activity);
    }

    private boolean updateAvailableCheck() {
        try {
            getCurrentVersionInfo();
            getLatestVersionInfo();

            if (currentVersionCode < latestVersionCode) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void getCurrentVersionInfo() throws Exception {

        PackageManager pm = activity.getPackageManager();
        String packageName = activity.getPackageName();
        PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);

        currentVersionCode = info.versionCode;
    }

    private void getLatestVersionInfo() throws Exception {

        HashMap<String, String> map = parseUpdateXml(updateXmlUrl);

        latestVersionCode = Integer.parseInt(map.get("versionCode"));
        Common.Variable.DOWNLOAD_FILE_URL = map.get("url");
        latestDescription = map.get("description");
    }

    public void updateCheck() {
        new updateCheckTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class updateCheckTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... arg0) {
            boolean updateAvailable = updateAvailableCheck();
            if (updateAvailable) {
                return new Object();
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if(result!=null){
                if (code == 0) updateListeners.updateAvailableNotify(latestDescription);
                if (code == 1) updateListeners.updateAvailableNotify1(latestDescription);
            }else {
                if (code == 0) updateListeners.updateUnavailableNotify();
                if (code == 1) updateListeners.updateUnavailableNotify1();
            }
        }

    }

    public void installApk(Context context) {
        File directory = new File(new File(context.getExternalCacheDir(), "update.apk").getPath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri dataUri = Uri.fromFile(directory);
        intent.setDataAndType(dataUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivityForResult(intent, REQUEST_UPDATE);
    }

    private HashMap<String, String> parseUpdateXml(String url) throws Exception {

        HashMap<String, String> map = new HashMap<>();

        InputStream is = new URL(url).openConnection().getInputStream();
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
    }
}