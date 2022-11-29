package com.example.bootservice;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Service;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.Set;

public class MainActivity extends ComponentActivity {

    boolean getAllpermission;
    private  DevicePolicyManager devicePolicyManager;
    private SharedPreferences sharedPreferences;

    public String SN;
    public boolean First = true;



    TextView device;
    TextView os;
    TextView publicIP;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_data);

        device = findViewById(R.id.textView2);
        os = findViewById(R.id.textView3);
        publicIP = findViewById(R.id.textView4);

        devicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        sharedPreferences = getSharedPreferences("SN", 0);
        SN = sharedPreferences.getString("id", "");

        Log.e("SN",sharedPreferences.getAll().toString());
        Log.e("SN",SN);

        //檢查是否為初次登記
        if(SN.equals("") || SN.isEmpty())
        {
            First = true;
        }
        else
        {
            First = false;
        }



    }
    public class MyPrefsBackupAgent extends BackupAgentHelper {
        // The name of the SharedPreferences file
        static final String PREFS = "SN";

        // A key to uniquely identify the set of backup data
        static final String PREFS_BACKUP_KEY = "SN";

        // Allocate a helper and add it to the backup agent
        @Override
        public void onCreate() {
            SharedPreferencesBackupHelper helper =
                    new SharedPreferencesBackupHelper(this, PREFS);
            addHelper(PREFS_BACKUP_KEY, helper);
        }
    }
    @Override
    public  void onStart(){
        super.onStart();
        Log.i("start","starting");
    }
    @Override
    public  void onRestart()
    {
        super.onRestart();
    }
    @Override
    public void onPause() {
        // The service is no longer used and is being destroyed
        super.onPause();
        Log.v("MainActivity", "onPause");
    }
    @Override
    public void onResume() {

        super.onResume();

        Log.v("MainActivity", "onResume");

        if (First == false)
        {
            if (Settings.canDrawOverlays(MainActivity.this)) {

                ComponentName componentName = new ComponentName(MainActivity.this, MyAdmin.class);
                boolean isActive = devicePolicyManager.isAdminActive(componentName);
                //檢查管理員權限
                if (!isActive) {
                    Log.v("MainActivity", "get lock");
                    //要求打開管理員權限
                    Intent intent = new Intent();
                    intent.setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "You need to be a device admin to enable device admin.");
                    startActivity(intent);


                } else {
                    //拿到所有需要申請的權限，就開始背景執行service
                    startForegroundService(new Intent(this, ServerService.class));
                    //Log.e("start service","Test success");
                    //建立展示data View
                    String userDeviceName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
                    if(userDeviceName == null)
                        userDeviceName = Settings.Secure.getString(getContentResolver(), "bluetooth_name");
                    device.setText("載具名稱:"+userDeviceName);
                    switch(Build.VERSION.SDK_INT) {
                        case 29:
                            os.setText("作業系統:" + "Android 10");
                            break;
                        case 30:
                            os.setText("作業系統:" + "Android 11");
                            break;
                        case 31:
                        case 32:
                            os.setText("作業系統:" + "Android 12");
                            break;
                        case 33:
                            os.setText("作業系統:" + "Android 13");
                            break;
                        default:
                            os.setText("作業系統:" + "未支援版本");
                            break;
                    }

                    publicIP.setText("Public IP:"+sharedPreferences.getString("pubIP","抓取中"));
                    //finish();
                }
            } else {
                //要求打開懸浮窗權限
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                // ActivityCompat.requestPermissions(MainActivity.this, new String[]{"Settings.ACTION_MANAGE_OVERLAY_PERMISSION"},10);
                startActivity(intent);
                // finish();
            }

        }
        else
        {
            //打開登記SN碼頁面，進行上傳SN碼並取得連接資訊的動作
            Intent intent = new Intent(this, FirstActivity.class);
            someActivityResultLauncher.launch(intent);
        }
    }
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        SN = intent.getStringExtra("SN");
                        if(!SN.isEmpty())
                        {
                            Log.e("SN","SN = " + SN);
                        }
                        //把連接資訊儲存在app中
                        String socketaddress = intent.getStringExtra("socketaddress");
                        String apiaddress = intent.getStringExtra("apiaddress");
                        String unitinfro = intent.getStringExtra("unitinfro");
                        Log.e("Http information",socketaddress);
                        Log.e("Http information",apiaddress);
                        Log.e("Http information",unitinfro);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("id",SN).putString("socketaddress",socketaddress).putString("apiaddress",apiaddress).putString("unitinfro",unitinfro).commit();

                        //登記完成，修改bool，讓Agent能開始背景執行
                        First = false;
                        //do a post api to  get socket connect data,also save into SharedPreferences(use for lost)

                    }
                    finishActivity(result.getResultCode());
                }
            });

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed

        super.onDestroy();

        Log.v("MainActivity", "onDestroy");
    }
}