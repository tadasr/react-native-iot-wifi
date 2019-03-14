package com.tadasr.IOTWifi;

import com.facebook.react.bridge.*;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.List;

public class IOTWifiModule extends ReactContextBaseJavaModule {
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;
    ReactApplicationContext context;
    
    public IOTWifiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        context = getReactApplicationContext();
    }
    
    @Override
    public String getName() {
        return "IOTWifi";
    }
    
    @ReactMethod
    public void isAvaliable(Callback callback) {
        callback.invoke(true);
    }

    @ReactMethod
    public void connect(String ssid, Callback callback) {
        connectSecure(ssid, "", false, callback);
    }

    @ReactMethod
    public void connectSecure(final String ssid, final String passphrase, final Boolean isWEP, final Callback callback) {
        new Thread(new Runnable() {
            public void run() {
                connectToWifi(ssid, passphrase, isWEP, callback);
            }
        }).start();
    }

    private void connectToWifi(String ssid, String passphrase, Boolean isWEP, Callback callback) {
        if (Build.VERSION.SDK_INT > 28) {
            callback.invoke("Fail");
            return;
        }
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = String.format("\"%s\"", ssid);

        if (passphrase.equals("")) {
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (isWEP) {
            configuration.wepKeys[0] = "\"" + passphrase + "\"";
            configuration.wepTxKeyIndex = 0;
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        } else { // WPA/WPA2
            configuration.preSharedKey = "\"" + passphrase + "\"";
        }

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        removeSSID(ssid);

        // Add configuration to Android wifi manager settings...
        int networkId = wifiManager.addNetwork(configuration);

        if (networkId != -1) {
            // Enable it so that android can connect
            wifiManager.disconnect();
            wifiManager.enableNetwork(networkId, true);
            boolean success = wifiManager.reconnect();
            if (!success) {
                callback.invoke("Fail");
                return;
            }
            try {
                Thread.sleep(3000);
                callback.invoke();
            } catch (InterruptedException e) {
                callback.invoke("Fail");
            }
        } else {
            callback.invoke("Failed to add network configuration");
        }
    }

    @ReactMethod
    public void removeSSID(String ssid, Callback callback) {
        removeSSID(ssid);
        callback.invoke();
    }

    public void removeSSID(String ssid) {
        // Remove the existing configuration for this netwrok
        List<WifiConfiguration> configList = wifiManager.getConfiguredNetworks();
        String comparableSSID = ('"' + ssid + '"'); // Add quotes because wifiConfig.SSID has them
        if (configList != null) {
            for (WifiConfiguration wifiConfig : configList) {
                if (wifiConfig.SSID.equals(comparableSSID)) {
                    Log.d("wifi", wifiConfig.toString());
                    int networkId = wifiConfig.networkId;
                    wifiManager.removeNetwork(networkId);
                    wifiManager.saveConfiguration();
                }
            }
        }
    }

    @ReactMethod
    public void getSSID(Callback callback) {
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();

        if (ssid == null || ssid == "<unknown ssid>") {
            NetworkInfo nInfo = connectivityManager.getActiveNetworkInfo();
            if (nInfo != null && nInfo.isConnected()) {
                ssid = nInfo.getExtraInfo();
            }
        }

        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        callback.invoke(ssid);
    }
}
