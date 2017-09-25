package com.ryans5000000.wifianalyser;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.security.AccessController.getContext;

public class WifiAnalyser extends android.support.v4.app.FragmentActivity {

    private TextView tvDebug;
    private ViewPager viewPager;
    SwipePagerAdapter swipePagerAdapter;
    private boolean locationPermissions;
    private LocationManager mLocationManager;
    final int LOCATION_REQUEST = 10;
    private WifiManager mWifiManager;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    public static final String PREFS_NAME = "WifiAnalyserPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_analyser_layout);
        tvDebug = (TextView) findViewById(R.id.tvDebug);

        //Create and set view pager adapter
        viewPager = (ViewPager) findViewById(R.id.vpSwipe);
        swipePagerAdapter = new SwipePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(swipePagerAdapter);

        // Verify that location permissions have been granted
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        requestLocationPermissions();
        if (!this.locationPermissions) { requestLocationPermissions(); }

        // Initiate wifi manager
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mWifiManager.setWifiEnabled(true);

        // We need an Editor object to make preference changes.
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();


    }

    public void requestLocationPermissions () {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                },LOCATION_REQUEST);
            }
            Toast.makeText(this,"Location Requested",Toast.LENGTH_LONG).show();
        } else {
            this.locationPermissions = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        switch (requestCode) {
            case 10:
                this.locationPermissions = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    public TextView getDebug() {
        return this.tvDebug;
    }

    public WifiManager getWifiManager() {
        return this.mWifiManager;
    }

    public LocationManager getLocationManager() {
        return this.mLocationManager;
    }


    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

}
