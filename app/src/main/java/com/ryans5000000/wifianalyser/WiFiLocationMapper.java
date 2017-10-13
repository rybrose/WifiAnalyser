package com.ryans5000000.wifianalyser;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ryan Ambrose on 16/09/2017.
 */

public class WiFiLocationMapper extends Fragment {

    private TextView tvDebug;
    private MobilityAnalyser mobilityAnalyser;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private android.location.Location mLocation;
    private WifiManager mWifiManager;
    private LinkLayerAnalyser mLinkLayerAnalyser;

    private Button btnWifiScan;
    private ArrayAdapter ardWifi;
    private ListView lvWifi;
    private Button btnLocation;
    private ArrayAdapter ardLocation;
    private ArrayList<String> alLocation;
    private DataWriter writer;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.wifi_location_mapper_layout,
                container, false);

        tvDebug = ((WifiAnalyser)getActivity()).getDebug();
        btnWifiScan = (Button) view.findViewById(R.id.btnWifiScan);
        btnWifiScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mLinkLayerAnalyser.networks.clear();
                mWifiManager.startScan();
                Button forceBtn = (Button) v;
                forceBtn.setEnabled(false);
                forceBtn.setText("Scanning...");
                ((WifiAnalyser)getActivity()).setDebugText("Warning: Scanning started manually.");
            }
        });

        writer = new DataWriter(tvDebug);

        // Initialise location services and set adapter to listview
        mLocationManager = ((WifiAnalyser)getActivity()).getLocationManager();
        mLocationListener = new WiFiLocationListener(btnWifiScan);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ((WifiAnalyser) getActivity()).requestLocationPermissions();
        }
        mLocationManager.requestLocationUpdates(mLocationManager.GPS_PROVIDER, 2000, 0, mLocationListener);
        mLocationManager.requestLocationUpdates(mLocationManager.NETWORK_PROVIDER, 2000, 0, mLocationListener);
        alLocation = new ArrayList<>();
        ardLocation = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, alLocation);


        // Initialise wifi scan and set adapter to listview
        mWifiManager = ((WifiAnalyser)getActivity()).getWifiManager();
        mLinkLayerAnalyser = new LinkLayerAnalyser(mWifiManager, mLocationManager, writer);
        getActivity().registerReceiver(mLinkLayerAnalyser, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        ardWifi = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, mLinkLayerAnalyser.networks);

        ///////////////////////////////////////
        mobilityAnalyser = new MobilityAnalyser(mWifiManager, mLocationManager, writer, this.getContext());
        IntentFilter mobilityIntentFilter = new IntentFilter();
        mobilityIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mobilityIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        getActivity().registerReceiver(mobilityAnalyser, mobilityIntentFilter);



        /*
        // We need an Editor object to make preference changes.
        settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();

        // Read the saved hash back from sorage
        String str = settings.getString("MappedSignals",null);
        tvDebug.setText(str);
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, ArrayList<Integer>>>(){}.getType();
        if (str == null) {
            tvDebug.setText("is null");
            mappedSignals = new HashMap<String, ArrayList<Integer>>();
        } else {
            mappedSignals = gson.fromJson(str, type);
            tvDebug.setText(mappedSignals.toString());
        } */


        /*
        if (mappedSignals == null) {
            mappedSignals = new HashMap<String, ArrayList<Integer>>();
            tvDebug.setText("Empty");
        }

        tvDebug.setText(mappedSignals.toString());
        */

        return view;
    }

    /*private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contains("wifi.SCAN_RESULTS")) {

                alScanResults = (ArrayList<ScanResult>) mWifiManager.getScanResults();
                // Sort by signal strength
                Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult lhs, ScanResult rhs) {
                        return (lhs.level>rhs.level ? -1 : (lhs.level==rhs.level ? 0 : 1));
                    }
                };
                Collections.sort(alScanResults,comparator);
                ScanResult wiFlySam = null;
                for (ScanResult s : alScanResults) {
                    alWifiResults.add(s.SSID+"\n"+s.level+" dB\n"+s.BSSID);
                    if (s.SSID.contains("WiFly-Sam")) {
                        wiFlySam = s;
                    }
                }
                //ardWifi.notifyDataSetChanged();

                // Map to the current location
                if(mLocation != null) {
                    DecimalFormat df = new DecimalFormat("#.####");
                    String coords = df.format(mLocation.getLatitude()) + "," + df.format(mLocation.getLongitude());
                    tvDebug.append("\n" + coords);
                    if (mappedSignals.get(coords) == null) {
                        mappedSignals.put(coords, new ArrayList<Integer>());
                    }
                    mappedSignals.get(coords).add(wiFlySam.level);
                    Gson gson = new Gson();
                    String str = gson.toJson(mappedSignals);
                    //editor.putString("MappedSignals", str);
                    //tvDebug.setText(str);
                    //editor.commit();
                }
            }
        }
    }*/

    private class WiFiLocationListener implements LocationListener {

        private android.location.Location prevlocation;
        Button forceBtn;
        public WiFiLocationListener(Button btn) {
            forceBtn = btn;
        }

        @Override
        public void onLocationChanged(android.location.Location location) {
            this.forceBtn.setEnabled(false);
            this.forceBtn.setText("Scanning...");

            if (location.getAccuracy() > 40) {
                return;
            }

            if (prevlocation == null || location.getLatitude() != prevlocation.getLatitude() || location.getLongitude() != prevlocation.getLongitude()) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                alLocation.add(0,Double.toString(lat)+"\n"+Double.toString(lon));
                //ardLocation.notifyDataSetChanged();
                this.prevlocation = location;
                mLocation = location;
                mLinkLayerAnalyser.networks.clear();
                mLinkLayerAnalyser.newLoc = true;
                /*try {
                    getActivity().registerReceiver(mLinkLayerAnalyser, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                } catch (Exception e) {

                }
                mWifiManager.startScan();*/
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }

    @Override
    public void onPause() {
        // Always call the superclass method first
        super.onPause();

    }

    @Override
    public void onResume() {
        // Always call the superclass method first
        super.onResume();
    }

}

