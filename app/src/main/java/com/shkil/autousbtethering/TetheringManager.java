package com.shkil.autousbtethering;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class TetheringManager {

    private static final String TAG = "TetheringManager";

    public void setUsbTethering(boolean enable) {
        int code;
        if (enable) {
            Log.v(TAG, "Enabling USB Tethering...");
            code = 1;
        } else {
            Log.v(TAG, "Disabling USB Tethering...");
            code = 0;
        }
        Process p = getps();
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        ;
        try {
            os.writeBytes("service call connectivity 30 i32 " + code);
            os.writeBytes("exit\n");
            os.flush();
            os.close();
        } catch (IOException e) {
            Log.e(TAG, "Error making service call", e);
        }
        waitps(p);
        Log.v(TAG, "Enabling/Disabling completed");
    }

    private Process getps() {
        return getps(true);
    }

    private Process getps(boolean isRoot) {
        Process p = null;
        try {
            // Preform su to get root privledges
            if (isRoot) {
                Log.v(TAG, "Creating root process");
                p = Runtime.getRuntime().exec("su");
            } else
                p = Runtime.getRuntime().exec("sh");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }

    private int waitps(Process p) {
        return waitps(p, false);
    }

    private int waitps(Process p, boolean onlywait) {
        int ev = -1;
        try {
            Log.v(TAG, "Waiting for p");
            p.waitFor();
            ev = p.exitValue();
        } catch (InterruptedException e) {
            p.destroy();
        }
        Log.v(TAG, "Process exit value: " + ev);
        if (onlywait) return ev;
        Log.v(TAG, "Printing streams: ");
        try {
            BufferedReader bis = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = bis.readLine()) != null)
                Log.v(TAG, "Output stream: " + line);
            bis.close();

            bis = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = bis.readLine()) != null)
                Log.v(TAG, "Error stream: " + line);
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ev;
    }
}
