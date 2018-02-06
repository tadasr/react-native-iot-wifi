package com.tadasr.IOTWifi;

import com.facebook.react.bridge.*;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class IOTWifiModule extends ReactContextBaseJavaModule {
    WifiManager wifiManager;
    Network wifiNetwork;
    ReactApplicationContext context;
    
    public IOTWifiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        wifiManager = (WifiManager) getReactApplicationContext().getSystemService(Context.WIFI_SERVICE);
        context = (ReactApplicationContext) getReactApplicationContext();
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
        configuration.SSID = String.format("\"%s\"", ssid);
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
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    @ReactMethod
    public void useWifiRequests(final boolean useRequests, final Promise promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            promise.reject("COMPATIBILITY", new Error("Android version is too low"));
            return;
        }
        if (!useRequests) {
            wifiNetwork = null;
            promise.resolve(true);
            return;
        }

        final ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder;
        builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        manager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if ((wifiNetwork = network) != null){
                    promise.resolve(true);
                } else {
                    promise.reject("COMPATIBILITY", new Error("Android version is too low"));
                }
            }
        });

    }

    @ReactMethod
    public void request(final String urlString, final Promise promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            promise.reject("COMPATIBILITY", new Error("Android version is too low"));
            return;
        }
        if (wifiNetwork == null) {
            promise.reject("USAGE", new Error("Force enable wifi usage before calling this method"));
            return;
        }

        try {
            URL url = new URL(urlString);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            url = new URL(uri.toASCIIString());
            URLConnection connection = wifiNetwork.openConnection(url);
            connection.setConnectTimeout(3000);
//            if (connection.connected) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine  = "", line;
                while ((line = in.readLine()) != null)
                    inputLine += line;
                in.close();
                promise.resolve(inputLine);
//            } else {
//                promise.reject("USAGE", new Error("Failed to establish connection"));
//            }
        } catch (Exception e) {
            promise.reject("USAGE", e);
        }
    }
}

