package com.ryans5000000.wifianalyser;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import static java.security.AccessController.getContext;

public class WifiAnalyser extends android.support.v4.app.FragmentActivity {

    private TextView tvDebug;
    private ViewPager viewPager;
    private boolean locationPermitted;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_analyser_layout);
        tvDebug = (TextView) findViewById(R.id.tvDebug);

        //Create and set view pager adapter
        viewPager = (ViewPager) findViewById(R.id.vpSwipe);
        SwipePagerAdapter swipePagerAdapter = new SwipePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(swipePagerAdapter);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Initiate the location listener
        mLocationListener = new WiFiLocationListener() ;
        requestLocation(mLocationManager);
        // Verify that location permissions have been granted
        if (!this.locationPermitted) { return; }
    }

    public void requestLocation (LocationManager mLocationManager) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                },10);
            }
            Toast.makeText(this,"Location Requested",Toast.LENGTH_LONG).show();
        } else {
            this.locationPermitted = true;
            mLocationManager.requestLocationUpdates("gps", 2000, 0, mLocationListener);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        switch (requestCode) {
            case 10:
                this.locationPermitted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    private class WiFiLocationListener implements LocationListener {

        private android.location.Location prevlocation;

        @Override
        public void onLocationChanged(android.location.Location location) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
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
