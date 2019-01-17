/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View} from 'react-native';
import Wifi from "react-native-iot-wifi";

const ssid = 'Sun';
const passphase = 'letitshine';

export default class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isApiAvaliable: false,
      ssid: '',
      connected: false,
      error: null
    };
    
    this.testWifi();
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.instructions}>{"\n"}
        API avaliable: {this.state.isApiAvaliable ? "yes" : "no"} {"\n"}
        ~{"\n"}
        SSID: {this.state.ssid} {"\n"}
        ~{"\n"}
        Connected to {ssid}: {this.state.connected  ? "yes" : "no"} {"\n"}
        Error: {this.state.error}{"\n"}
        </Text>
      </View>
    );
  }

  testWifi(){
    Wifi.isAvaliable((avaliable) => {
      
      this.setState({isApiAvaliable: avaliable});
      
      if (!avaliable) {
        return;
      }

      
      Wifi.getSSID((SSID) => {
        this.setState({ssid: SSID});
      });

      Wifi.connectSecure(ssid, passphase, false, (error) => {
        this.setState({error: error});
        this.setState({connected: error == null});
        
        Wifi.getSSID((SSID) => {
          this.setState({ssid: SSID});
        });
      });
      
      // Wifi.connect(ssid, (error) => {
      //   this.setState({error: error});
      //   this.setState({connected: error == null});

      //   Wifi.getSSID((SSID) => {
      //     this.setState({ssid: SSID});
      //   });

        // Wifi.removeSSID(ssid, (error)=>{
        //   this.setState({error: error});
        // });
      // });
    });
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: '#54998E',
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
