/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.nfcprovisioning;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.AsyncTaskLoader;

import com.example.android.common.logger.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

/**
 * Loads default values for NFC provisioning.
 * <p/>
 * This loader first tries to load values from a config file in SD card. Then it fills in missing
 * values using constants and settings on the programming device.
 */
public class ProvisioningValuesLoader extends AsyncTaskLoader<Map<String, String>> {

    private static final String FILENAME = "nfcprovisioning.txt";
    private static final String TAG = "LoadProvisioningValuesTask";

    private Map<String, String> mValues;

    public ProvisioningValuesLoader(Context context) {
        super(context);
    }

    @Override
    public Map<String, String> loadInBackground() {
        HashMap<String, String> values = new HashMap<>();
        loadFromDisk(values);
        gatherAdminExtras(values);
        loadSystemValues(values);
        return values;
    }

    @Override
    public void deliverResult(Map<String, String> values) {
        if (isReset()) {
            return;
        }
        mValues = values;
        super.deliverResult(values);
    }

    @Override
    protected void onStartLoading() {
        if (mValues != null) {
            deliverResult(mValues);
        }
        if (takeContentChanged() || mValues == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        mValues = null;
    }

    private void loadFromDisk(HashMap<String, String> values) {
        File directory = Environment.getExternalStorageDirectory();
        File file = new File(directory, FILENAME);
        if (!file.exists()) {
            return;
        }
        Log.d(TAG, "Loading the config file...");
        try {
            loadFromFile(values, file);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error loading data from " + file, e);
        }
    }

    private void loadFromFile(HashMap<String, String> values, File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.startsWith("#")) {
                    continue;
                }
                int position = line.indexOf("=");
                if (position < 0) { // Not found
                    continue;
                }
                String key = line.substring(0, position);
                String value = line.substring(position + 1);
                values.put(key, value);
                Log.d(TAG, key + "=" + value);
            }
        }
    }

    private void gatherAdminExtras(HashMap<String, String> values) {
        Properties props = new Properties();
        Set<String> keys = new HashSet<>(values.keySet());
        for (String key : keys) {
            if (key.startsWith("android.app.extra")) {
                continue;
            }
            props.put(key, values.get(key));
            values.remove(key);
        }
        StringWriter sw = new StringWriter();
        try{
            props.store(sw, "admin extras bundle");
            values.put(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                    sw.toString());
            Log.d(TAG, "Admin extras bundle=" + values.get(
                    DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE));
        } catch (IOException e) {
            Log.e(TAG, "Unable to build admin extras bundle");
        }
    }

    private void loadSystemValues(HashMap<String, String> values) {
        Context context = getContext();
        //noinspection deprecation
        /*putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
                "com.afwsamples.testdpc");
        if (Build.VERSION.SDK_INT >= 23) {
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                    "com.afwsamples.testdpc/com.afwsamples.testdpc.DeviceAdminReceiver");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION,
                    "https://testdpc-latest-apk.appspot.com");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM,
                    "gJD2YwtOiWJHkSMkkIfLRlj-quNqG1fb6v100QmzM9w=");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION,
                    "true");
        }*/

        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
                "com.afwsamples.testdpc");
        if (Build.VERSION.SDK_INT >= 23) {
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                    "com.afwsamples.testdpc/com.afwsamples.testdpc.DeviceAdminReceiver");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION,
                    "http://52.9.54.230/NFC/dpc.apk");
//            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION,
//                    "http://192.168.100.20:8383/zubair/android6/dpc.apk");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM,
                    "HUWAKsNQ3cj4O7MHeLyskMYrQgR86QykJKR-o5RHrjQ=");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, "true");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, "true");
        }
        /*putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
                "com.google.android.apps.work.clouddpc");
        if (Build.VERSION.SDK_INT >= 23) {
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                    "com.google.android.apps.work.clouddpc/.receivers.CloudDeviceAdminReceiver");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION,
                    "https://play.google.com/managed/downloadManagingApp?identifier=setup");
            putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM,
                    "I5YvS0O5hXY46mb01BlRjq4oJJGs2kuUcHvVkAPEXlg");
        }*/

        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_LOCALE,
                CompatUtils.getPrimaryLocale(context.getResources().getConfiguration()).toString());
        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_TIME_ZONE,
                TimeZone.getDefault().getID());
//        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE, "WPA");
//        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PASSWORD, "987654321c");

        if (!values.containsKey(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID)) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Activity.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info.getNetworkId() != -1) { // Connected to network
                values.put(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID,
                        trimSsid(info.getSSID()));
            }
        }
    }

    /**
     * {@link WifiInfo#getSSID} returns the WiFi SSID surrounded by double quotation marks. This
     * method removes them if wifiSsid contains them.
     */
    private static String trimSsid(String wifiSsid) {
        int head = wifiSsid.startsWith("\"") ? 1 : 0;
        int tail = wifiSsid.endsWith("\"") ? 1 : 0;
        return wifiSsid.substring(head, wifiSsid.length() - tail);
    }

    private static <Key, Value> void putIfMissing(HashMap<Key, Value> map, Key key, Value value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }

}
