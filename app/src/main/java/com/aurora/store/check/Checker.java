package com.aurora.store.check;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;

import com.aurora.store.check.event.UpdateEventListener;
import com.aurora.store.check.event.UpdateEventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Checker {
    private int supportCode;

    private final String supportXmlUrl;

    private final UpdateEventListenerList supportListeners;

    public Checker(Activity activity, String supportXmlUrl) {
        this.supportXmlUrl = supportXmlUrl;

        supportListeners = new UpdateEventListenerList();
        supportListeners.addEventListener((UpdateEventListener) activity);
    }

    private boolean supportAvailableCheck(){
        try {
            getSupportInfo();
            if(supportCode==1) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void getSupportInfo() throws Exception{

        HashMap<String, String> map = parseSupportXml(supportXmlUrl);

        supportCode = Integer.parseInt(Objects.requireNonNull(map.get("supportCode")));
    }

    public void supportCheck(){
        new supportCheckTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class supportCheckTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... arg0) {
            boolean supportAvailable = supportAvailableCheck();
            if(supportAvailable){
                return new Object();
            }else{
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if(result!=null){
                supportListeners.supportAvailableNotify();
            }else {
                supportListeners.supportUnavailableNotify();
            }
        }

    }

    private HashMap<String, String> parseSupportXml(String url) throws Exception {

        HashMap<String, String> map = new HashMap<>();

        InputStream is = new URL(url).openConnection().getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);

        DocumentBuilderFactory document_builder_factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder document_builder = document_builder_factory.newDocumentBuilder();
        Document document = document_builder.parse(bis);
        Element root = document.getDocumentElement();

        if(root.getTagName().equals("support")){
            NodeList nodelist = root.getChildNodes();
            for (int j=0;j<nodelist.getLength();j++){
                Node node = nodelist.item(j);
                if(node.getNodeType()==Node.ELEMENT_NODE){
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
