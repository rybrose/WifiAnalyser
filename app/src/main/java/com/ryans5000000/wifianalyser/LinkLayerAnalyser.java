package com.ryans5000000.wifianalyser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Created by ryanc on 6/10/2017.
 */

public class LinkLayerAnalyser extends BroadcastReceiver {
    private WifiManager mWifiManager;
    private LocationManager mLocationManager;
    private WifiInfo info;
    public ArrayList<ScanResult> networks;
    private DataWriter writer;
    private int uniwideAPs = 0;
    public LinkLayerAnalyser(WifiManager w, LocationManager lm, DataWriter writer) {
        mWifiManager = w;
        mLocationManager = lm;
        info = w.getConnectionInfo();
        networks = new ArrayList<>();
        this.writer = writer;
    }

    public void saveData() {
        info = mWifiManager.getConnectionInfo();
        long timestamp = System.currentTimeMillis();
        Location location;
        try {
            location = mLocationManager.getLastKnownLocation(mLocationManager.GPS_PROVIDER);
        } catch(SecurityException e) {
            return;
        }

        if (location == null) {
            try {
                location = mLocationManager.getLastKnownLocation(mLocationManager.NETWORK_PROVIDER);
            } catch(SecurityException e) {
                return;
            }
            if(location == null) {
                return;
            }
        }

        String strLoc = location.getLatitude() + "," + location.getLongitude() + "," + location.getAccuracy();
        String data = timestamp + "," + strLoc + "," + uniwideAPs + "," + getStrength()  + "," + getProtocol()  + "," + getSpeed() + "," + getFrequency() + "," + getBSSID();
        writer.write(data, "coverage.csv");
        //UniwideLocation data = new UniwideLocation(timestamp, location, uniwideAPs, getStrength(), getProtocol(), getSpeed(), getFrequency(), getBSSID());
    }

    public String getBSSID() {
        return info.getBSSID();
    }

    // Returns connection RSSI in dBm
    public int getStrength() {
        return info.getRssi();
    }

    // Returns a, b, g, n, ac
    public String getProtocol() {
        Float ghz = Float.valueOf(2);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ghz = Float.valueOf(info.getFrequency()) / 1000;
        }
        int linkSpeed = info.getLinkSpeed();
        String protocol = "";
        if (linkSpeed <= 11) {
            protocol += "b (DSSS)";
        } else if (linkSpeed > 11 && linkSpeed <= 54) {
            protocol += "g (OFDM)";
        } else if (linkSpeed > 54 && linkSpeed <= 72 && ghz < 3) {
            protocol += "n (MIMO-OFDM)";
        } else if (linkSpeed > 54 && linkSpeed <= 150 && ghz < 3) {
            protocol += "n (MIMO-OFDM)";
        } else if (linkSpeed > 54 && linkSpeed <= 300 && ghz >= 5) {
            protocol += "n (MIMO-OFDM)";
        } else if (linkSpeed > 150) {
            protocol += "ac (MIMO-OFDM)";
        }
        return protocol;
    }

    // Returns link speed in Mbps
    public int getSpeed() {
        return info.getLinkSpeed();
    }

    // Returns 2 or 5 GHz
    public double getFrequency() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return Math.floor(info.getFrequency() / 1000);
        } else return 2;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Scan results are available 
        if (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

            uniwideAPs = 0;
            ArrayList<ScanResult> alScanResults = (ArrayList<ScanResult>) mWifiManager.getScanResults();
            // Count the number of unique Uniwide APs
            for (ScanResult s : alScanResults) {
                networks.add(s);
                if (s.SSID.equals("uniwide")) {
                    uniwideAPs++;
                }
            }
            saveData();
            context.unregisterReceiver(this);
            writer.logAppend("Note: Scanning pauses when location does not change.\n");
        }
    }
}