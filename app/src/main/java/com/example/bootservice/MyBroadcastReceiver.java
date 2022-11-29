package com.example.bootservice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;



//偵測載具開機訊號，用於進行Agent自啟動
public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            //Intent.ACTION_BOOT_COMPLETED == android.intent.action.BOOT_COMPLETED
            Log.v("TestService","Boot,start app and try send data");
            Intent intent1 = new Intent(context , MainActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent1);

        }

    }
}
