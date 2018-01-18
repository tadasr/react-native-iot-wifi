# react-native-iot-wifi
Wifi configuration.
This library was written to config iot devices. With iOS 11 Apple introduced NEHotspotConfiguration class for wifi configuration. Library supports same functioanllity on ios and android.

## iOS
> Important
> IOTWifi uses NEHotspotConfigurationManager. To use the NEHotspotConfigurationManager class, you must enable the Hotspot Configuration capability in [Xcode](NEHotspotConfigurationManager).

1. Drang an drop `IOTWifi.xcodeproj` to your workspace
2. Go to project -> `Build Phases` -> `Link Binary With Libraries` -> `+` -> add `libIOTWifi.a`
3. Go to project -> `Build Phases` -> `Link Binary With Libraries` -> `+` -> add `NetworkExtension.framework`
4. Go to project -> `Capabilities` -> enable `Hotspot Configuration`

## android


## Usage

```javascript
import Wifi from "react-native-iot-wifi";

Wifi.isAvaliable((avaliable) => {
  console.log(avaliable ? 'avaliable' : 'failed');
});

Wifi.getSSID((SSID) => {
  console.log(SSID);
});

Wifi.connect("wifi-name", (error) => {
  console.log(error ? 'error: ' + error : 'connected to wifi-name');
});

Wifi.removeSSID("wifi-name", (error)=>{
  console.log(error ? 'error: ' + error : 'removed wifi-name');
});
```