'use strict';

import { NativeModules } from 'react-native';

function connect(...args) {
  if (args.length < 2 || args.length > 3) {
    throw new TypeError('invalid arguments');
  } else if (args.length === 2) {
    NativeModules.connect(args[0], false, args[1]);
  } else {
    NativeModules.connect(...args);
  }
}

function connectSecure(...args) {
  if (args.length < 4 || args.length > 5) {
    throw new TypeError('invalid arguments');
  } else if (args.length === 4) {
    NativeModules.connectSecure(args[0], args[1], args[2], false, args[3]);
  } else {
    NativeModules.connectSecure(...args);
  }
}

function removeSSID(...args) {
  if (args.length < 2 || args.length > 3) {
    throw new TypeError('invalid arguments');
  } else if (args.length === 2) {
    NativeModules.removeSSID(args[0], false, args[1]);
  } else {
    NativeModules.removeSSID(...args);
  }
}

module.exports = Object.assign(
  {},
  NativeModules.IOTWifi,
  {
    connect: connect,
    connectSecure: connectSecure,
    removeWifi: removeWifi,
  },
);

/**
 * (un)bindNetwork only affects Android
 * isAvaliable(Callback callback)
 * connect(String ssid, Boolean bindNetwork = false, Callback callback)
 * todo: connectSecure(String ssid, String passphrase, Boolean isWEP, Boolean bindNetwork = false, Callback callback)
 * removeSSID(String ssid, Boolean unbindNetwork = false, Callback callback)
 * getSSID(Callback callback)
 * Android only: forceWifiUsage()
 */
