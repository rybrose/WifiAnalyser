package com.ryans5000000.wifianalyser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;

/**
 * Created by ryanc on 6/10/2017.
 */

public class LinkLayerAnalyser extends BroadcastReceiver {
    private WifiManager mWifiManager;

    public LinkLayerAnalyser(WifiManager w) {
        mWifiManager = w;
    }

    // Returns connection RSSI in dBm
    public int getStrength() {
        return mWifiManager.getConnectionInfo().getRssi();
    }

    // Returns a, b, g, n, ac
    public String getProtocol() {
        Float ghz = Float.valueOf(2);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ghz = Float.valueOf(mWifiManager.getConnectionInfo().getFrequency()) / 1000;
        }
        int linkSpeed = mWifiManager.getConnectionInfo().getLinkSpeed();
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
        return mWifiManager.getConnectionInfo().getLinkSpeed();
    }

    // Returns 2 or 5 GHz
    public double getFrequency() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return Math.floor(mWifiManager.getConnectionInfo().getFrequency() / 1000);
        } else return 2;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Scan results are available 
        if (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

            int uniwideAPs = 0;
            ArrayList<ScanResult> alScanResults = (ArrayList<ScanResult>) mWifiManager.getScanResults();
            // Count the number of unique Uniwide APs
            for (ScanResult s : alScanResults) {
                if (s.SSID == "Uniwide") {
                    uniwideAPs++;
                }
            }
        }
    }
}