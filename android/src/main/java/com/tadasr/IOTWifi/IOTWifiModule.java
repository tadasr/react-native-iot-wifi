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

import java.util.List;


class FailureCodes {
    static int SYSTEM_ADDED_CONFIG_EXISTS = 1;
    static int FAILED_TO_CONNECT = 2;
    static int FAILED_TO_ADD_CONFIG = 3;
    static int FAILED_TO_BIND_CONFIG = 4;
}

public class IOTWifiModule extends ReactContextBaseJavaModule {
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ReactApplicationContext context;

    public IOTWifiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getReactApplicationContext().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        context = getReactApplicationContext();
    }

    private String errorFromCode(int errorCode) {
        return "ErrorCode: " + errorCode;
    }

    @Override
    public String getName() {
        return "IOTWifi";
    }

    @ReactMethod
    public void isApiAvailable(final Callback callback) {
        callback.invoke(true);
    }

    @ReactMethod
    public void connect(String ssid, Boolean bindNetwork, Callback callback) {
        connectSecure(ssid, "", false, bindNetwork, callback);
    }

    @ReactMethod
    public void connectSecure(final String ssid, final String passphrase, final Boolean isWEP,
                              final Boolean bindNetwork, final Callback callback) {
        new Thread(new Runnable() {
            public void run() {
                connectToWifi(ssid, passphrase, isWEP, bindNetwork, callback);
            }
        }).start();
    }

    private void connectToWifi(String ssid, String passphrase, Boolean isWEP, Boolean bindNetwork, Callback callback) {
        if (!removeSSID(ssid)) {
            callback.invoke(errorFromCode(FailureCodes.SYSTEM_ADDED_CONFIG_EXISTS));
            return;
        }

        WifiConfiguration configuration = createWifiConfiguration(ssid, passphrase, isWEP);
        int networkId = wifiManager.addNetwork(configuration);

        if (networkId != -1) {
            // Enable it so that android can connect
            wifiManager.disconnect();
            boolean success =  wifiManager.enableNetwork(networkId, true);
            if (!success) {
                callback.invoke(errorFromCode(FailureCodes.FAILED_TO_ADD_CONFIG));
                return;
            }
            success = wifiManager.reconnect();
            if (!success) {
                callback.invoke(errorFromCode(FailureCodes.FAILED_TO_CONNECT));
                return;
            }
            boolean connected = pollForValidSSSID(10, ssid);
            if (!connected) {
                callback.invoke(errorFromCode(FailureCodes.FAILED_TO_CONNECT));
                return;
            }
            if (!bindNetwork) {
                callback.invoke();
                return;
            }
            try {
                bindToNetwork(ssid, callback);
            } catch (Exception e) {
                Log.d("IoTWifi", "Failed to bind to Wifi: " + ssid);
                callback.invoke();
            }
        } else {
            callback.invoke(errorFromCode(FailureCodes.FAILED_TO_ADD_CONFIG));
        }
    }

    private WifiConfiguration createWifiConfiguration(String ssid, String passphrase, Boolean isWEP) {
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
        return configuration;
    }

    private boolean pollForValidSSSID(int maxSeconds, String expectedSSID) {
        try {
            for (int i = 0; i < maxSeconds; i++) {
                String ssid = this.getWifiSSID();
                if (ssid != null && ssid.equalsIgnoreCase(expectedSSID)) {
                    return true;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            return false;
        }
        return false;
    }

    private void bindToNetwork(final String ssid, final Callback callback) {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        connectivityManager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {

            private boolean bound = false;

            @Override
            public void onAvailable(Network network) {
                String offeredSSID = getWifiSSID();

                if (!bound && offeredSSID.equals(ssid)) {
                    try {
                        bindProcessToNetwork(network);
                        bound = true;
                        callback.invoke();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                callback.invoke(errorFromCode(FailureCodes.FAILED_TO_BIND_CONFIG));
            }

            @Override
            public void onLost(Network network) {
                if (bound) {
                    bindProcessToNetwork(null);
                    connectivityManager.unregisterNetworkCallback(this);
                }
            }
        });
    }

    private void bindProcessToNetwork(final Network network) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(network);
        } else {
            ConnectivityManager.setProcessDefaultNetwork(network);
        }
    }

    @ReactMethod
    public void removeSSID(String ssid, Boolean unbind, Callback callback) {
        if (!removeSSID(ssid)) {
            callback.invoke(errorFromCode(FailureCodes.SYSTEM_ADDED_CONFIG_EXISTS));
            return;
        }
        if (unbind) {
            bindProcessToNetwork(null);
        }

        callback.invoke();
    }


    private boolean removeSSID(String ssid) {
        boolean success = true;
        // Remove the existing configuration for this network
        WifiConfiguration existingNetworkConfigForSSID = getExistingNetworkConfig(ssid);

        //No Config found
        if (existingNetworkConfigForSSID == null) {
            return success;
        }
        int existingNetworkId = existingNetworkConfigForSSID.networkId;
        if (existingNetworkId == -1) {
            return success;
        }
        success = wifiManager.removeNetwork(existingNetworkId) && wifiManager.saveConfiguration();
        //If not our config then success would be false
        return success;
    }

    @ReactMethod
    public void getSSID(Callback callback) {
        String ssid = this.getWifiSSID();
        callback.invoke(ssid);
    }

    private String getWifiSSID() {
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();

        if (ssid == null || ssid.equalsIgnoreCase("<unknown ssid>")) {
            NetworkInfo nInfo = connectivityManager.getActiveNetworkInfo();
            if (nInfo != null && nInfo.isConnected()) {
                ssid = nInfo.getExtraInfo();
            }
        }

        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }

    private WifiConfiguration getExistingNetworkConfig(String ssid) {
        WifiConfiguration existingNetworkConfigForSSID = null;
        List<WifiConfiguration> configList = wifiManager.getConfiguredNetworks();
        String comparableSSID = ('"' + ssid + '"'); // Add quotes because wifiConfig.SSID has them
        if (configList != null) {
            for (WifiConfiguration wifiConfig : configList) {
                if (wifiConfig.SSID.equals(comparableSSID)) {
                    Log.d("IoTWifi", "Found Matching Wifi: "+ wifiConfig.toString());
                    existingNetworkConfigForSSID = wifiConfig;
                    break;

                }
            }
        }
        return existingNetworkConfigForSSID;
    }
}
