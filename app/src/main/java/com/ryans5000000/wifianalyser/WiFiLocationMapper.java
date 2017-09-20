package com.ryans5000000.wifianalyser;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Ryan Ambrose on 16/09/2017.
 */

public class WiFiLocationMapper extends Fragment {

    private TextView tvDebug;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private WifiManager mWifiManager;
    private WifiReceiver mWifiReceiver;

    private Button btnWifiScan;
    private ArrayAdapter ardWifi;
    private ArrayList<ScanResult> alScanResults;
    private ArrayList<String> alWifiResults;
    private ListView lvWifi;

    private Button btnLocation;
    private ArrayAdapter ardLocation;
    private ArrayList<String> alLocation;
    private ListView lvLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.wifi_location_mapper_layout,
                container, false);

        tvDebug = ((WifiAnalyser)getActivity()).getDebug();

        // Initialise wifi scan and set adapter to listview
        mWifiManager = ((WifiAnalyser)getActivity()).getWifiManager();
        mWifiReceiver = new WifiReceiver();
        getActivity().registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        alWifiResults = new ArrayList<>();
        ardWifi = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, alWifiResults);
        lvWifi = (ListView) view.findViewById(R.id.lvWifiResults);
        lvWifi.setAdapter(ardWifi);
        btnWifiScan = (Button) view.findViewById(R.id.btnWifiScan);
        btnWifiScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alWifiResults.clear();
                mWifiManager.startScan();
            }
        });


        // Initialise location services and set adapter to listview
        mLocationManager = ((WifiAnalyser)getActivity()).getLocationManager();
        mLocationListener = new WiFiLocationListener();
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ((WifiAnalyser) getActivity()).requestLocationPermissions();
        }
        mLocationManager.requestLocationUpdates("gps", 2000, 0, mLocationListener);
        alLocation = new ArrayList<>();
        ardLocation = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, alLocation);
        lvLocation = (ListView) view.findViewById(R.id.lvLocation);
        lvLocation.setAdapter(ardLocation);

        return view;
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contains("wifi.SCAN_RESULTS")) {
                tvDebug.append("\n"+intent.getAction());
                alScanResults = (ArrayList<ScanResult>) mWifiManager.getScanResults();
                // Sort by signal strength
                Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult lhs, ScanResult rhs) {
                        return (lhs.level>rhs.level ? -1 : (lhs.level==rhs.level ? 0 : 1));
                    }
                };
                Collections.sort(alScanResults,comparator);
                for (ScanResult s : alScanResults) {
                    alWifiResults.add(s.SSID+"\n"+s.level+" dB\n"+s.BSSID);
                }
                ardWifi.notifyDataSetChanged();
            }
        }
    }

    private class WiFiLocationListener implements LocationListener {

        private android.location.Location prevlocation;

        @Override
        public void onLocationChanged(android.location.Location location) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            alLocation.add(0,Double.toString(lat)+"\n"+Double.toString(lon));
            ardLocation.notifyDataSetChanged();
            this.prevlocation = location;
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
        getActivity().unregisterReceiver(mWifiReceiver);
    }

    @Override
    public void onResume() {
        // Always call the superclass method first
        super.onResume();
        getActivity().registerReceiver(mWifiReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

}

