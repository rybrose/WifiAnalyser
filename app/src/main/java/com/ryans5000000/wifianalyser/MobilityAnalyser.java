package com.ryans5000000.wifianalyser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ryanc on 6/10/2017.
 */

public class MobilityAnalyser extends BroadcastReceiver {
    WifiManager mWifiManager;
    Long conn_gained = System.currentTimeMillis();
    Long conn_lost = Long.valueOf(0);
    Long dhcp_gained = System.currentTimeMillis();
    Long dhcp_lost = Long.valueOf(0);
    boolean has_network = false;
    boolean has_dhcp = false;
    String prev_bssid;


    public MobilityAnalyser(WifiManager w, Context context) {
        mWifiManager = w;
        final Context mContext = context;
        prev_bssid = String.valueOf(w.getConnectionInfo().getBSSID());
        Thread pingWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                Long tcp_gained = System.currentTimeMillis();
                Long tcp_lost = Long.valueOf(0);

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
                                    showDebugAlert("L4: TCP lost for " + time_taken_to_regain + " ms.", mContext);
                                }
                            } else {
                                if (tcp_lost == 0) {
                                    tcp_lost = System.currentTimeMillis();
                                }
                            }
                        } catch (MalformedURLException e1) {
                            if (tcp_lost == 0) {
                                tcp_lost = System.currentTimeMillis();
                            }
                        } catch (IOException e) {
                            if (tcp_lost == 0) {
                                tcp_lost = System.currentTimeMillis();
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
                    showDebugAlert("L2: Reassociated in " + time_taken_to_regain + " ms.", context);
                }
            } else {
                conn_lost = System.currentTimeMillis();
                has_network = false;
            }
        } else if (intent.getAction() == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            if (netInfo != null && mWifiManager.getDhcpInfo() != null && mWifiManager.getDhcpInfo().ipAddress != 0) {
                has_dhcp = true;
                if (dhcp_lost != 0) {
                    dhcp_gained = System.currentTimeMillis();
                    Long time_taken_to_regain = dhcp_gained - dhcp_lost;
                    dhcp_lost = Long.valueOf(0);
                    showDebugAlert("L3: DHCP regained in " + time_taken_to_regain + " ms.", context);
                }
            } else {
                dhcp_lost = System.currentTimeMillis();
                has_dhcp = false;
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
}