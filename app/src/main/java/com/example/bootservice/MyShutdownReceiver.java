package com.example.bootservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


//偵測載具關機
public class MyShutdownReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_SHUTDOWN)){
            //Intent.ACTION_BOOT_COMPLETED == android.intent.action.BOOT_COMPLETED
            Log.v("TestService","close,we can try send data");
        }

    }
}