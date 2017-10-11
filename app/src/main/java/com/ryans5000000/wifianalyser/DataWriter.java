package com.ryans5000000.wifianalyser;

import android.os.Environment;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

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
            FileWriter fileWriter;
            BufferedWriter out;
            if (!file.exists()) {
                file.createNewFile();
                fileWriter = new FileWriter(file, true);
                out = new BufferedWriter(fileWriter);
                if (filename.equals("coverage.csv")) {
                    out.write("time,lat,long,accuracy,num_uniwide_aps,strength,protocol,speed,freq,bssid");
                } else if (filename.equals("blackspots.csv")) {
                    out.write("time,lat,long,accuracy,lat2,long2,accuracy2,type_of_disconnect,time_taken_to_regain,prev_bssid,new_bssid");
                }
                out.newLine();
                out.write(data);
                out.newLine();
                out.close();
                return;
            }
            fileWriter = new FileWriter(file, true);
            out = new BufferedWriter(fileWriter);
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

    synchronized public HashMap<String, Boolean> readLocations(String filename) {
        File file = new File(Environment.getExternalStorageDirectory().getPath()+"/"+filename);
        HashMap<String, Boolean> visited = new HashMap<>();
        if (!file.exists()) {
            return visited;
        }
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                if (line.contains("time")) {
                    continue;
                }
                String[] csvs = line.split(",");
                visited.put(csvs[1] + "," + csvs[2], true);
            }
            return visited;
        } catch (Exception e) {
            return visited;
        }
    }
}
