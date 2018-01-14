package com.nectarsun.IOTWifi;

import com.facebook.react.bridge.*;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.content.Context;

import java.util.List;

public class IOTWifiModule extends ReactContextBaseJavaModule {
    WifiManager wifiManager;
    
    public IOTWifiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        wifiManager = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
    
    //    @ReactMethod
    public void connectSecure(String ssid, String passphrase, Boolean isWEP, Callback callback) {
        WifiConfiguration configuration = new WifiConfiguration();
        
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedGroupCiphers.clear();
        configuration.allowedKeyManagement.clear();
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedProtocols.clear();
        configuration.SSID         = String.format("\"%s\"", ssid);
        configuration.preSharedKey = String.format("\"%s\"", passphrase);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        
        removeSSID(ssid);
        
        // Add configuration to Android wifi manager settings...
        int networkId = wifiManager.addNetwork(configuration);
        
        // Enable it so that android can connect
        wifiManager.disconnect();
        wifiManager.enableNetwork(networkId, true);
        wifiManager.reconnect();
        
        callback.invoke();
    }
    
    //    @ReactMethod
    //    public void removeSSID(String ssid, Callback callback) {
    //        callback.invoke(null);
    //    }
    //
    public void removeSSID(String ssid) {
        // Remove the existing configuration for this netwrok
        List<WifiConfiguration> configList = wifiManager.getConfiguredNetworks();
        String comparableSSID = ('"' + ssid + '"'); //Add quotes because wifiConfig.SSID has them
        if (configList != null) {
            for (WifiConfiguration wifiConfig : configList) {
                if (wifiConfig.SSID.equals(comparableSSID)) {
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
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        callback.invoke(ssid);
    }
}

