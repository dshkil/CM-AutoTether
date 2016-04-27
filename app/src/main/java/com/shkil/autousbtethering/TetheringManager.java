package com.shkil.autousbtethering;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import static android.app.PendingIntent.getBroadcast;
import static android.app.PendingIntent.getService;
import static com.shkil.autousbtethering.AppConfig.DEBUG;

public class TetheringManager {

    private static final String TAG = "TetheringManager";

    private static final long POLLING_INTERVAL = AppConfig.POLLING_INTERVAL;

    private static final String PREF_USB_TETHERING_ENABLED = "usbTetheringEnabled";

    private static volatile TetheringManager instance;

    private final Context context;

    private volatile boolean usbTetheringEnabled;
    private volatile boolean pollingStarted;

    public static TetheringManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TetheringManager.class) {
                if (instance == null) {
                    instance = new TetheringManager(context);
                }
            }
        }
        return instance;
    }

    private TetheringManager(Context context) {
        this.context = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
                if (PREF_USB_TETHERING_ENABLED.equals(key)) {
                    usbTetheringEnabled = preferences.getBoolean(PREF_USB_TETHERING_ENABLED, false);
                }
            }
        });
        this.usbTetheringEnabled = preferences.getBoolean(PREF_USB_TETHERING_ENABLED, false);
    }

    public void restoreTetheringState() {
        if (usbTetheringEnabled) {
            setUsbTethering(true);
        }
    }

    public void setUsbTethering(boolean enable) {
        int code;
        if (enable) {
            Log.v(TAG, "Enabling USB Tethering...");
            code = 1;
            if (!pollingStarted) {
                startPolling();
            }
        } else {
            Log.v(TAG, "Disabling USB Tethering...");
            code = 0;
            if (pollingStarted) {
                cancelPolling();
            }
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit()
                .putBoolean(PREF_USB_TETHERING_ENABLED, enable)
                .apply();
        Process p = getps();
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
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

    private void startPolling() {
        Log.v(TAG, "startPolling()");
        pollingStarted = true;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getBroadcast(context, 0,
                EventReceiver.createPollingIntent(context), PendingIntent.FLAG_CANCEL_CURRENT);
        long triggerAt = SystemClock.elapsedRealtime() + POLLING_INTERVAL;
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, POLLING_INTERVAL, pendingIntent);
    }

    private void cancelPolling() {
        Log.v(TAG, "cancelPolling()");
        pollingStarted = false;
        PendingIntent pendingIntent = getBroadcast(context, 0,
                EventReceiver.createPollingIntent(context), PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            if (DEBUG) {
                Log.d(TAG, "Cancelling polling...");
            }
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }
    }

    private Process getps() {
        return getps(true);
    }

    private Process getps(boolean isRoot) {
        Process p = null;
        try {
            // Preform su to get root privileges
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
