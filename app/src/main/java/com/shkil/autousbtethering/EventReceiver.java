package com.shkil.autousbtethering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EventReceiver extends BroadcastReceiver {

    private static final String TAG = "Receiver";
    private static TetheringManager tetheringManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive(): intent=" + intent);
        if (tetheringManager == null) {
            tetheringManager = new TetheringManager();
        }
        /*try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

        tetheringManager.setUsbTethering(true);

    }
}
