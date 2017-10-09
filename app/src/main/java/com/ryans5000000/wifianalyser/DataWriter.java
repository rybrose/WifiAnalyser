package com.ryans5000000.wifianalyser;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

/**
 * Created by ryanc on 9/10/2017.
 */

public class DataWriter {
    synchronized public void write(String data, String filename) {
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath()+"/"+filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter out = new BufferedWriter(fileWriter);
            out.write(data);
            out.newLine();
            out.close();
        } catch (Exception e) {
            return;
        }
    }
}
