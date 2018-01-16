package com.tadasr.IOTWifi;

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
        configuration.SSID         = String.format("\"%s\"", ssid);
        configuration.preSharedKey = passphrase.equals("") ? null : String.format("\"%s\"", passphrase);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);


        if (passphrase.equals("")) {
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (isWEP) {
            configuration.wepKeys[0] = "\"" + passphrase + "\"";
            configuration.wepTxKeyIndex = 0;
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        } else { // WPA/WPA2
            configuration.preSharedKey = "\"" + passphrase + "\"";

            configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            configuration.status = WifiConfiguration.Status.ENABLED;

            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
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
            wifiManager.reconnect();
            callback.invoke();
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
