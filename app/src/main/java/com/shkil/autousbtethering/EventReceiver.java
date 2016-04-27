package com.shkil.autousbtethering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EventReceiver extends BroadcastReceiver {

    private static final String TAG = "EventReceiver";

    private static final boolean DEBUG = AppConfig.DEBUG;

    public static Intent createPollingIntent(Context context) {
        return new Intent(context, EventReceiver.class);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "onReceive(): intent=" + intent);
        }
        TetheringManager tetheringManager = TetheringManager.getInstance(context);
        tetheringManager.restoreTetheringState();
    }

}
