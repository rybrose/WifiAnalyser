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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

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
    private HashMap<String, Boolean> visited;
    public boolean newLoc = true;
    private int bestUniWideRSSI = 0;

    public LinkLayerAnalyser(WifiManager w, LocationManager lm, DataWriter writer) {
        this.writer = writer;
        this.visited = writer.readLocations("coverage.csv");
        mWifiManager = w;
        mLocationManager = lm;
        info = w.getConnectionInfo();
        networks = new ArrayList<>();
    }

    public void saveData() {
        info = mWifiManager.getConnectionInfo();

        long timestamp = System.currentTimeMillis();
        Location netLocation;
        Location gpsLocation;

        try {
            gpsLocation = mLocationManager.getLastKnownLocation(mLocationManager.GPS_PROVIDER);
        } catch(SecurityException e) {
            return;
        }

        try {
            netLocation = mLocationManager.getLastKnownLocation(mLocationManager.NETWORK_PROVIDER);
        } catch(SecurityException e) {
            return;
        }

        Location location;
        if (gpsLocation != null && netLocation != null) {
            if (isBetterLocation(gpsLocation, netLocation)) {
                location = gpsLocation;
            } else {
                location = netLocation;
            }
        } else if (gpsLocation == null && netLocation != null) {
            location = netLocation;
        } else if (netLocation == null && gpsLocation != null) {
            location = gpsLocation;
        } else {
            return;
        }

        if (visited.containsKey(location.getLatitude() + "," + location.getLongitude())) {
            writer.logAppend("Note: Location already recorded, skipping.\n");
            return;
        } else {
            visited.put(location.getLatitude() + "," + location.getLongitude(), true);
        }

        String strLoc = location.getLatitude() + "," + location.getLongitude() + "," + location.getAccuracy();
        String data = timestamp + "," + strLoc + "," + uniwideAPs + "," + getStrength() + "," + this.bestUniWideRSSI + "," + getProtocol()  + "," + getSpeed() + "," + getFrequency() + "," + getBSSID() + "," + getIpAddress();
        writer.write(data, "coverage.csv");
    }

    public String getBSSID() {
        return info.getBSSID();
    }

    // Returns connection RSSI in dBm
    public int getStrength() {
        return info.getRssi();
    }

    // Return IP address
    public String getIpAddress() {
        int ipAddress = info.getIpAddress();
        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = "";
        }

        return ipAddressString;
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
                bestUniWideRSSI = -100;
                ArrayList<ScanResult> alScanResults = (ArrayList<ScanResult>) mWifiManager.getScanResults();
                // Count the number of unique Uniwide APs
                for (ScanResult s : alScanResults) {
                    networks.add(s);
                    if (s.SSID.equals("uniwide")) {
                        if (s.level > this.bestUniWideRSSI) {
                            this.bestUniWideRSSI = s.level;
                        }
                        uniwideAPs++;
                    }
                }
                saveData();

        }
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}