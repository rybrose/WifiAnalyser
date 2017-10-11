package com.ryans5000000.wifianalyser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

/**
 * Created by ryanc on 6/10/2017.
 */

public class MobilityAnalyser extends BroadcastReceiver {
    private WifiManager mWifiManager;
    private Long conn_gained = System.currentTimeMillis();
    private Long conn_lost = Long.valueOf(0);
    private Long dhcp_gained = System.currentTimeMillis();
    private Long dhcp_lost = Long.valueOf(0);
    private boolean has_network = false;
    private boolean has_dhcp = false;
    private String prev_bssid;
    private String prev_ip;
    private LocationManager mLocationManager;
    String dhcp_lost_loc = "";
    String dhcp_regained_loc = "";
    String conn_lost_loc = "";
    String conn_regained_loc = "";
    final private DataWriter writer;
    private Thread pingWorker;

    public MobilityAnalyser(WifiManager w, LocationManager lm, final DataWriter wr, Context context) {
        mWifiManager = w;
        mLocationManager = lm;
        this.writer = wr;
        final Context mContext = context;
        prev_bssid = w.getConnectionInfo().getBSSID();
        prev_ip = getIpAddress(w.getConnectionInfo().getIpAddress());
        pingWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                Long tcp_gained = System.currentTimeMillis();
                Long tcp_lost = Long.valueOf(0);
                String tcp_lost_loc = "";
                String tcp_regained_loc = "";


                while (true) {
                    if (has_network) {
                        try {
                            URL url = new URL("http://www.google.com");
                            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                            urlc.setConnectTimeout(1000);
                            urlc.setReadTimeout(1000); // 1 s.
                            urlc.connect();
                            if (urlc.getResponseCode() == 200) {        // 200 = "OK" code (http connection is fine).
                                if (tcp_lost != 0) {
                                    tcp_gained = System.currentTimeMillis();
                                    Long time_taken_to_regain = tcp_gained - tcp_lost;
                                    tcp_lost = Long.valueOf(0);
                                    try {
                                        Location lkl;
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

                                        if (gpsLocation != null && netLocation != null) {
                                            if (isBetterLocation(gpsLocation, netLocation)) {
                                                lkl = gpsLocation;
                                            } else {
                                                lkl = netLocation;
                                            }
                                        } else if (gpsLocation == null && netLocation != null) {
                                            lkl = netLocation;
                                        } else if (netLocation == null && gpsLocation != null) {
                                            lkl = gpsLocation;
                                        } else {
                                            return;
                                        }
                                        tcp_regained_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                                    } catch (SecurityException e) {
                                        tcp_regained_loc = "?,?,?";
                                    }
                                    String data = System.currentTimeMillis()  + "," + tcp_lost_loc  + "," + tcp_regained_loc + ",L4," + time_taken_to_regain   + "," + prev_bssid  + "," + mWifiManager.getConnectionInfo().getBSSID() + "," + prev_ip + "," + getIpAddress(mWifiManager.getConnectionInfo().getIpAddress());
                                    writer.write(data, "blackspots.csv");

                                    prev_bssid = mWifiManager.getConnectionInfo().getBSSID();
                                    prev_ip = getIpAddress(mWifiManager.getConnectionInfo().getIpAddress());
                                    //showDebugAlert("L4: TCP lost for " + time_taken_to_regain + " ms.", mContext);
                                }
                            } else {
                                if (tcp_lost == 0) {
                                    tcp_lost = System.currentTimeMillis();
                                    try {
                                        Location lkl;
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

                                        if (gpsLocation != null && netLocation != null) {
                                            if (isBetterLocation(gpsLocation, netLocation)) {
                                                lkl = gpsLocation;
                                            } else {
                                                lkl = netLocation;
                                            }
                                        } else if (gpsLocation == null && netLocation != null) {
                                            lkl = netLocation;
                                        } else if (netLocation == null && gpsLocation != null) {
                                            lkl = gpsLocation;
                                        } else {
                                            return;
                                        }
                                        tcp_lost_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                                    } catch (SecurityException e) {
                                        tcp_lost_loc = "?,?,?";
                                    }
                                }
                            }
                        } catch (MalformedURLException e1) {
                            if (tcp_lost == 0) {
                                tcp_lost = System.currentTimeMillis();
                                try {
                                    Location lkl;
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

                                    if (gpsLocation != null && netLocation != null) {
                                        if (isBetterLocation(gpsLocation, netLocation)) {
                                            lkl = gpsLocation;
                                        } else {
                                            lkl = netLocation;
                                        }
                                    } else if (gpsLocation == null && netLocation != null) {
                                        lkl = netLocation;
                                    } else if (netLocation == null && gpsLocation != null) {
                                        lkl = gpsLocation;
                                    } else {
                                        return;
                                    }
                                    tcp_lost_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                                } catch (SecurityException e) {
                                    tcp_lost_loc = "?,?,?";
                                }
                            }
                        } catch (IOException e) {
                            if (tcp_lost == 0) {
                                tcp_lost = System.currentTimeMillis();
                                try {
                                    Location lkl;
                                    Location netLocation;
                                    Location gpsLocation;

                                    try {
                                        gpsLocation = mLocationManager.getLastKnownLocation(mLocationManager.GPS_PROVIDER);
                                    } catch(SecurityException ee) {
                                        return;
                                    }

                                    try {
                                        netLocation = mLocationManager.getLastKnownLocation(mLocationManager.NETWORK_PROVIDER);
                                    } catch(SecurityException ee) {
                                        return;
                                    }

                                    if (gpsLocation != null && netLocation != null) {
                                        if (isBetterLocation(gpsLocation, netLocation)) {
                                            lkl = gpsLocation;
                                        } else {
                                            lkl = netLocation;
                                        }
                                    } else if (gpsLocation == null && netLocation != null) {
                                        lkl = netLocation;
                                    } else if (netLocation == null && gpsLocation != null) {
                                        lkl = gpsLocation;
                                    } else {
                                        return;
                                    }
                                    tcp_lost_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                                } catch (SecurityException ee) {
                                    tcp_lost_loc = "?,?,?";
                                }
                            }
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        });

        pingWorker.start();
    }

    @Override
    synchronized public void onReceive(Context context, Intent intent) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();

        if (intent.getAction() == ConnectivityManager.CONNECTIVITY_ACTION) {
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                has_network = true;
                if (conn_lost != 0) {
                    conn_gained = System.currentTimeMillis();
                    Long time_taken_to_regain = conn_gained - conn_lost;
                    conn_lost = Long.valueOf(0);
                    try {
                        Location lkl;
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

                        if (gpsLocation != null && netLocation != null) {
                            if (isBetterLocation(gpsLocation, netLocation)) {
                                lkl = gpsLocation;
                            } else {
                                lkl = netLocation;
                            }
                        } else if (gpsLocation == null && netLocation != null) {
                            lkl = netLocation;
                        } else if (netLocation == null && gpsLocation != null) {
                            lkl = gpsLocation;
                        } else {
                            return;
                        }

                        conn_regained_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                    } catch (SecurityException e) {
                        conn_regained_loc = "?,?,?";
                    }
                    String data = System.currentTimeMillis()  + "," + conn_lost_loc  + "," + conn_regained_loc + ",L2," + time_taken_to_regain   + "," + prev_bssid  + "," + mWifiManager.getConnectionInfo().getBSSID() + "," + prev_ip + "," + getIpAddress(mWifiManager.getConnectionInfo().getIpAddress());
                    prev_bssid = mWifiManager.getConnectionInfo().getBSSID();
                    prev_ip = getIpAddress(mWifiManager.getConnectionInfo().getIpAddress());
                    //showDebugAlert("L2: Reassociated in " + time_taken_to_regain + " ms.", context);
                }
            } else {
                conn_lost = System.currentTimeMillis();
                has_network = false;
                try {
                    Location lkl;
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

                    if (gpsLocation != null && netLocation != null) {
                        if (isBetterLocation(gpsLocation, netLocation)) {
                            lkl = gpsLocation;
                        } else {
                            lkl = netLocation;
                        }
                    } else if (gpsLocation == null && netLocation != null) {
                        lkl = netLocation;
                    } else if (netLocation == null && gpsLocation != null) {
                        lkl = gpsLocation;
                    } else {
                        return;
                    }
                    conn_lost_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                } catch (SecurityException e) {
                    conn_lost_loc = "?,?,?";
                }
            }
        } else if (intent.getAction() == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            if (netInfo != null && mWifiManager.getDhcpInfo() != null && mWifiManager.getDhcpInfo().ipAddress != 0) {
                has_dhcp = true;
                if (dhcp_lost != 0) {
                    dhcp_gained = System.currentTimeMillis();
                    Long time_taken_to_regain = dhcp_gained - dhcp_lost;
                    dhcp_lost = Long.valueOf(0);
                    try {
                        Location lkl;
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

                        if (gpsLocation != null && netLocation != null) {
                            if (isBetterLocation(gpsLocation, netLocation)) {
                                lkl = gpsLocation;
                            } else {
                                lkl = netLocation;
                            }
                        } else if (gpsLocation == null && netLocation != null) {
                            lkl = netLocation;
                        } else if (netLocation == null && gpsLocation != null) {
                            lkl = gpsLocation;
                        } else {
                            return;
                        }
                        dhcp_regained_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                    } catch (SecurityException e) {
                        dhcp_regained_loc = "?,?,?";
                    }
                    String data = System.currentTimeMillis()  + "," + dhcp_lost_loc  + "," + dhcp_regained_loc + ",L3," + time_taken_to_regain   + "," + prev_bssid  + "," + mWifiManager.getConnectionInfo().getBSSID() + "," + prev_ip + "," + getIpAddress(mWifiManager.getConnectionInfo().getIpAddress());
                    writer.write(data, "blackspots.csv");
                    prev_bssid = mWifiManager.getConnectionInfo().getBSSID();
                    prev_ip = getIpAddress(mWifiManager.getConnectionInfo().getIpAddress());
                    //showDebugAlert("L3: DHCP regained in " + time_taken_to_regain + " ms.", context);
                }
            } else {
                dhcp_lost = System.currentTimeMillis();
                has_dhcp = false;
                try {
                    Location lkl;
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

                    if (gpsLocation != null && netLocation != null) {
                        if (isBetterLocation(gpsLocation, netLocation)) {
                            lkl = gpsLocation;
                        } else {
                            lkl = netLocation;
                        }
                    } else if (gpsLocation == null && netLocation != null) {
                        lkl = netLocation;
                    } else if (netLocation == null && gpsLocation != null) {
                        lkl = gpsLocation;
                    } else {
                        return;
                    }
                    dhcp_lost_loc = String.valueOf(lkl.getLatitude()) + "," + String.valueOf(lkl.getLongitude() + "," + lkl.getAccuracy());
                } catch (SecurityException e) {
                    dhcp_lost_loc = "?,?,?";
                }
            }
        }
    }

    private void showDebugAlert(final String message, final Context context) {
        Activity a = (Activity) context;
        a.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                alertDialog.setTitle("Connection Alert");
                alertDialog.setMessage(message);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });

    }

    public void stopThread() {
        this.pingWorker.interrupt();
    }

    // Return IP address
    private String getIpAddress(int ipAddress) {
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