package com.ryans5000000.wifianalyser;

import android.os.Environment;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

/**
 * Created by ryanc on 9/10/2017.
 */

public class DataWriter {
    private TextView log;
    public DataWriter(TextView log) {
        this.log = log;
    }
    synchronized public void write(String data, String filename) {
        try {
            if(data.contains("L4")) {
                log.setText("Mobility: TCP blackspot recorded.\n" + data);
            } else if(data.contains("L3")) {
                log.setText("Mobility: DHCP blackspot recorded.\n" + data);
            } else if(data.contains("L2")) {
                log.setText("Mobility: Total connection blackspot recorded.\n" + data);
            } else {
                log.setText("Link Layer: Data recorded.\n" + data);
            }
            File file = new File(Environment.getExternalStorageDirectory().getPath()+"/"+filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter out = new BufferedWriter(fileWriter);
            out.write(data);
            out.newLine();
            out.close();
        } catch (Exception e) {
            return;
        }
    }

    synchronized public void logAppend(String data) {
        log.setText(data + "\n" + log.getText());
    }
}
