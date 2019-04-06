declare module 'react-native-iot-wifi' {
  // tslint:disable:no-namespace
  type RemoveConnectArgs =
    | [string, (error: string) => void]
    | [string, boolean, (error: string) => void];

  type ConnectSecureArgs =
    | [string, (error: string) => void]
    | [string, boolean, (error: string) => void];

  export namespace RNWifi {
    function isAvailable(cb: (available: boolean) => void): void;
    function getSSID(cb: (ssid: string) => void): void;
    function connect(...args: RemoveConnectArgs): void;
    function connectSecure(...args: ConnectSecureArgs): void;
    function removeSSID(...args: RemoveConnectArgs): void;
  }

  export default RNWifi;
}
