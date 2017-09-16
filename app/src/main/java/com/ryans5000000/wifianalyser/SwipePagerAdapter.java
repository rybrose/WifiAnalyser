package com.ryans5000000.wifianalyser;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Ryan Ambrose on 16/09/2017.
 */

public class SwipePagerAdapter extends FragmentPagerAdapter {

    private final String[] TITLES = { "WiFi Location Mapper"};

    public SwipePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment f;
        switch (position) {
            default:
                f = new WiFiLocationMapper();
        }
        return f;
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }

}