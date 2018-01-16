# react-native-iot-wifi
Wifi configuration.
This library was written to config iot devices. With iOS 11 Apple introduced NEHotspotConfiguration class for wifi configuration. Library supports same functioanllity on ios and android.

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