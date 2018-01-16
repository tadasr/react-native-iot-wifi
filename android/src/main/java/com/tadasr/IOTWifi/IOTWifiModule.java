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

import java.util.List;

public class IOTWifiModule extends ReactContextBaseJavaModule {
    WifiManager wifiManager;
    ReactApplicationContext context;
    Boolean flag_is_permission_set = false;
    
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
            callback.invoke();
        } else {
            callback.invoke("Failed to add network configuration");
        }
    }
    
    //Receives a boolean to enable forceWifiUsage if true, and disable if false.
    //Is important to enable only when communicating with the device via wifi
    //and remember to disable it when disconnecting from device.
    @ReactMethod
    public void forceWifiUsage(boolean useWifi) {
        boolean canWriteFlag = false;
        
        if (useWifi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    canWriteFlag = Settings.System.canWrite(context);
                    
                    if (!canWriteFlag) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        
                        context.startActivity(intent);
                    }
                }
                
                if (((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && canWriteFlag) || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M))) {
                    final ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkRequest.Builder builder;
                    builder = new NetworkRequest.Builder();
                    //set the transport type do WIFI
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                    manager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                manager.bindProcessToNetwork(network);
                            } else {
                                //This method was deprecated in API level 23
                                ConnectivityManager.setProcessDefaultNetwork(network);
                            }
                            try {
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            manager.unregisterNetworkCallback(this);
                        }
                    });
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
                manager.bindProcessToNetwork(null);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ConnectivityManager.setProcessDefaultNetwork(null);
            }
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

