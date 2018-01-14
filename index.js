'use strict';

import { NativeModules } from 'react-native';
module.exports = NativeModules.IOTWifi;

/**
 * isAvaliable(Callback callback)
 * connect(String ssid, Callback callback)
 * todo: connectSecure(String ssid, String passphrase, Boolean isWEP, Callback callback)
 * todo: removeSSID(String ssid, Callback callback)
 * getSSID(Callback callback)
 */