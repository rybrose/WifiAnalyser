package com.ryans5000000.wifianalyser;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Ryan Ambrose on 16/09/2017.
 */

public class SwipePagerAdapter extends FragmentPagerAdapter {

    static final int WIFI_LOCATION_MAPPER = 0;

    private final String[] TITLES = { "WIFI_LOCATION_MAPPER" };
    private Fragment[] fragments;

    public SwipePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment f;
        switch (position) {
            case WIFI_LOCATION_MAPPER:
                f = new WiFiLocationMapper();
                break;
            default:
                f = new WiFiLocationMapper();
        }
        return f;
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }

    public Fragment getFragment (int position) {
        return fragments[position];
    }
}