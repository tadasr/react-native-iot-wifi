package com.tadasr.IOTWifi;

import com.facebook.react.bridge.*;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.text.TextUtils;

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
    public void connect(String ssid, Boolean bindNetwork, Callback callback) {
        connectSecure(ssid, "", false, bindNetwork, callback);
    }

    @ReactMethod
    public void connectSecure(final String ssid, final String passphrase, final Boolean isWEP, final Boolean bindNetwork, final Callback callback) {
        new Thread(new Runnable() {
            public void run() {
                connectToWifi(ssid, passphrase, isWEP, bindNetwork, callback);
            }
        }).start();
    }

    private void connectToWifi(String ssid, String passphrase, Boolean isWEP, Boolean bindNetwork, Callback callback) {
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
                if (bindNetwork) {
                    bindToNetwork(ssid);
                }
                callback.invoke();
            } catch (InterruptedException e) {
                callback.invoke("Fail");
            }
        } else {
            callback.invoke("Failed to add network configuration");
        }
    }

    private void bindToNetwork(final String ssid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            connectivityManager
                .requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                        private boolean bound = false;

                        @Override
                        public void onAvailable(Network network) {
                            WifiInfo info = wifiManager.getConnectionInfo();
                            String offeredSSID = info.getSSID();
                            if (offeredSSID.startsWith("\"") && offeredSSID.endsWith("\"")) {
                                offeredSSID = offeredSSID.substring(1, offeredSSID.length() - 1);
                            }

                            if (!bound && offeredSSID.equals(ssid)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    connectivityManager.bindProcessToNetwork(network);
                                } else {
                                    ConnectivityManager.setProcessDefaultNetwork(network);
                                }
                                try {
                                    bound = true;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onLost(Network network) {
                            if (bound) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    connectivityManager.bindProcessToNetwork(null);
                                } else {
                                    ConnectivityManager.setProcessDefaultNetwork(null);
                                }
                                connectivityManager.unregisterNetworkCallback(this);
                            }
                        }
                    });
        }
    }

    public void unbindNetwork() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(null);
        } else {
            ConnectivityManager.setProcessDefaultNetwork(null);
        }
    }

    @ReactMethod
    public void removeSSID(String ssid, Boolean unbind, Callback callback) {
        removeSSID(ssid);
        if (unbind) {
            unbindNetwork();
        }

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
        Log.d("wifi", ssid);

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
