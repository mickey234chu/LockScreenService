package com.example.bootservice;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class ServerService<Myboolean> extends Service {


    IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used
    boolean socketest = false;
    boolean functiontest = false;
    boolean issocketchange = false;
    boolean changesocket = false;
    static boolean working= false;
    private boolean Block_Flag = false;
    private boolean Lock_Flag = false;
    private boolean Lost_Flag = false;
    private MyWebSocketClient websocket;
    private Thread Thread1 = null;
    private static Thread Thread2 = null;

    private String pubIP;

    public static String owner;
    Timer timer = new Timer();
    Timer locktimer = new Timer();

    TimerTask task ;
    TimerTask locktask;

    //window setting
    private View floatView;
    private TextView screentext;

    //Lockscreen Manager,用來根據需要擋螢幕
    private WindowManager.LayoutParams floatWindowLayoutParam;
    private WindowManager windowManager;
    private WindowInsetsController controller;
    private WindowInsetsController controller2;
    private String LockMessage;
    private String LostMessage;

    //blockinput 用來擋按鈕
    private WindowManager.LayoutParams params;
    private WindowManager lockManager;
    private MoniterView mMoniterView;
    private GestureDetector mGestureDetector;
    //馬上關屏
    private DevicePolicyManager devicePolicyManager;

    //定時鎖
    private int StartLock = 0;
    private int StopLock = 0;
    private boolean timelock = false;
    private Calendar Gettime;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private ComponentName compName;
    private AlarmReceiver mAlarmBroadcastReceiver;

    private HomeKeyBroadcastReceiver mHomeKeyBroadcastReceiver;
    //SN
    private SharedPreferences sharedPreferences;
    public static String ID;


    HttpURLConnection connection;
    private String apiaddress;
    //api address
    URL url;
    //本地 ip
    URL ipurl = new URL("https://api.ipify.org?format=json");
    //websocket
    String websocketLink;
    //URI uri;
    public ServerService() throws MalformedURLException {
    }











    @Override
    public void onCreate()
    {
        // The service is being created
        super.onCreate();

        Log.v("TestService","start");
        startForeground(1,getNotification());
        Log.i("TestService","startForeground");




    }

    //初始化
    private void initViews() throws IOException {
        //channel = new NotificationChannel("testopen", "test", NotificationManager.IMPORTANCE_HIGH);
        httpCall(ipurl.toString());
        //取內存SN
        sharedPreferences = getSharedPreferences("SN", 0);
        ID = sharedPreferences.getString("id","");

        //取websocket link
        if(socketest)
        {
            //使用測試用websocket
            websocketLink = "ws://imoeedge20220914134800.azurewebsites.net/api/WebSoket?nickName="+ID;
            //uri = URI.create("ws://imoeedge20220914134800.azurewebsites.net/api/WebSoket?nickName="+ID);
            Log.e("test link",websocketLink);
        }
        else
        {
            //使用正常開放給本載具的websocket
            websocketLink = sharedPreferences.getString("socketaddress","")+ID;
            //uri = URI.create(websocketLink+ID);
            Log.e("not test link ",websocketLink);
        }
        apiaddress = sharedPreferences.getString("apiaddress","");
        //設定發送資料的API之連接
        if(socketest)
        {
            url = new URL("http://imoeedge20220914134800.azurewebsites.net/api/UserTime");
            Log.e("apiaddress","http://imoeedge20220914134800.azurewebsites.net/api/UserTime");


        }
        else
        {
            url = new URL(apiaddress.trim());
            Log.e("apiaddress",sharedPreferences.getString("apiaddress","null"));
        }

        //尋找時間鎖
        mAlarmBroadcastReceiver = new AlarmReceiver();
        IntentFilter mAlarmIntentFilter = new IntentFilter();
        mAlarmIntentFilter.addAction("activity_app");
        registerReceiver(mAlarmBroadcastReceiver,mAlarmIntentFilter);
        StartLock = sharedPreferences.getInt("Starttime",-1);
        StopLock = sharedPreferences.getInt("Endtime",-1);

        //取納管單位，初始化訊息內容
        owner = sharedPreferences.getString("unitinfro","");
        LostMessage = owner +"\n所屬載具";
        LockMessage = "";
        Log.e("unitinfro",sharedPreferences.getString("unitinfro","null"));


        if(timerlockhandle(StartLock,StopLock))
        {
            timelock = true;
        }

        //檢查lock flag,lost flag ，用於恢復關機前手機狀態
        Lock_Flag = sharedPreferences.getBoolean("lock",false);
        Lost_Flag = sharedPreferences.getBoolean("lost",false);

        //初始化device Manager，用於呼叫lock now 關螢幕用
        mGestureDetector = new GestureDetector(null, new MyGestureDetectorListener(),null);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, MyAdmin.class);

        //建立定時sendata
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Taipei")); //取得時間
        cal.add(Calendar.MINUTE, 1);
        add_alarm(ServerService.this,cal,"send_data",0);


        //開始連接socket
        Thread1 = new Thread(new Thread1());
    }
    //鎖定螢幕時，用來探測手指動作
    private static class MyGestureDetectorListener implements GestureDetector.OnGestureListener {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            Log.e("Listen","onDown");
            //devicePolicyManager.lockNow();
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
            Log.e("Listen","onShowPress");
            //devicePolicyManager.lockNow();

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            Log.e("Listen","onSingleTapUp");
            //devicePolicyManager.lockNow();
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            Log.e("Listen","onScroll");
            //devicePolicyManager.lockNow();
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            Log.e("Listen","onLongPress");
           // devicePolicyManager.lockNow();

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            Log.e("Listen","onFling");
            //devicePolicyManager.lockNow();
            return false;
        }
    }
    //檢查是否位於需鎖定的時間區間
    private boolean timerlockhandle(int start,int end)
    {
        Gettime = Calendar.getInstance();
        //取現在的時,分
        int hour = Gettime.get(Calendar.HOUR_OF_DAY);
        int minute = Gettime.get(Calendar.MINUTE);
        //統一換算分鐘，用來比大小
        int minuteofDay = hour*60 + minute;

        if(minuteofDay == 0)
        {
            minuteofDay = 24*60;
        }
        //3種可能的判斷:
        // start 比 end 小 (如09:00-13:00) => 當 minuteofDay 在它們之間，就要鎖
        // start 比 end 大 (如23:00-01:00) => 當 minuteofDay>=start，或<=end時，都要鎖//
        // start == end => minuteofDay == 該時間，就要鎖//
        if(start < end)
        {
            return minuteofDay >= start && minuteofDay <= end;
        }
        else if(start > end)
        {
            return minuteofDay >= start || minuteofDay <= end;
        }
        else
        {
            return minuteofDay == start;
        }
    }
    //統一輸入時間(時間換算)
    private  int timetoint(String time)
    {

        int hour = Integer.parseInt(time.split("[:]")[0]);
        int minutes = Integer.parseInt(time.split("[:]")[1]);
        int minutesofDay = hour*60 + minutes;
        Log.e("time",Integer.toString(minutesofDay));
        return minutesofDay;
    }

    //上傳資料格式
    static class Data
    {
        String publicip;
        String uploadtime;
        String serialnumber;
        String owner;
        public Data(String localIpAddress, String date, String id, String own) {

            publicip = localIpAddress;
            uploadtime = date;
            serialnumber = id;
            owner = own;
        }
    }



    //建立前台Server用通知
    private Notification getNotification()
    {
        String ID = "com.example.bootservice";
        String NAME = "Channel ONE";
        Intent newintent = new Intent(ServerService.this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivities(this,0, new Intent[]{newintent},PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification;
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(ID,NAME,manager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);
        notification = new NotificationCompat.Builder(ServerService.this,ID);
        notification.setContentTitle("Service start")
                .setContentText("Service is start")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(pendingIntent);

        return notification.build();
    }
    //用於打開懸浮窗
    private void setwindow( String text)
    {


        //get width and height

        int width ;
        int height ;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        metrics = getApplicationContext().getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;

        if(Build.VERSION.SDK_INT>=30) {
            WindowMetrics metrics2 = windowManager.getCurrentWindowMetrics();
            WindowInsets windowInsets = metrics2.getWindowInsets();
            Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()|WindowInsets.Type.displayCutout());
            width= metrics2.getBounds().width()+ insets.right+insets.left;
            height=metrics2.getBounds().height()+insets.top+insets.bottom;

        }
        Log.i("Width,Height:",width +","+height);

        //xml -> view
        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        //get lockscreen layout

        floatView = (ViewGroup) inflater.inflate(R.layout.lockscreen, null);
        screentext = floatView.findViewWithTag("locktext");
        if(text.equals(""))
        {
            screentext.setText(LostMessage);
        }
        else
        {
            screentext.setText(text);
        }
        if(Lost_Flag)
        {
            screentext.setText(LostMessage);
        }

        floatView.setKeepScreenOn(true);

        floatView.setOnTouchListener((view, motionEvent) -> {
               Log.d("TestService","testtouch");
               return mGestureDetector.onTouchEvent(motionEvent);

           });

        int LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        floatWindowLayoutParam = new WindowManager.LayoutParams(
                (int) width,
                (int)height,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE ,
                PixelFormat.TRANSLUCENT
        );

        // The Gravity of the Floating Window is set.
        // The Window will appear in the center of the screen


        floatWindowLayoutParam.gravity = Gravity.CENTER;
        floatWindowLayoutParam.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        floatWindowLayoutParam.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        floatWindowLayoutParam.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        floatWindowLayoutParam.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        // X and Y value of the window is set
        floatWindowLayoutParam.x = 0;
        floatWindowLayoutParam.y = 0;

        windowManager.addView(floatView, floatWindowLayoutParam);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            controller = floatView.getWindowInsetsController();
            controller.hide(android.view.WindowInsets.Type.statusBars()
                    | android.view.WindowInsets.Type.navigationBars());



        }


    }
    //用於打開懸浮窗時禁用按鈕
    private void setLockManager()
    {

        if(lockManager == null)
        {
            lockManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            mMoniterView = new MoniterView(ServerService.this);


        }

        if(null != lockManager)
        {
            params = new WindowManager.LayoutParams(
                    1,1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            //params.type = WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG;
            params.format = PixelFormat.RGBA_8888;
            params.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        mMoniterView.setFocusable(true);
        mMoniterView.setKeepScreenOn(true);

        try
        {
            lockManager.addView(mMoniterView,params);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                controller2 = mMoniterView.getWindowInsetsController();
                controller2.hide(android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars());



            }

        }
        catch (IllegalArgumentException e )
        {
            e.printStackTrace();
        }

    }
    //取本地IP
    public static String PIP;
    public void httpCall(String url) {
        //RequestQueue initialized
        //publicip
        RequestQueue mRequestQueue = Volley.newRequestQueue(this);

        //String Request initialized
        StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, response -> {
            Log.e("ip", response);
            pubIP = response;
            if(!pubIP.isEmpty())
            {
                pubIP= pubIP.replaceAll("\"","").replaceFirst("ip:","").replace("{","").replace("}","");
                PIP = pubIP;
                Log.e("IP",pubIP);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("pubIP",pubIP).commit();
        }, error -> pubIP = "0.0.0.0");

        mRequestQueue.add(mStringRequest);
    }
    //收起statusBars
    public  class HomeKeyBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context,Intent intent)
        {
            devicePolicyManager.lockNow();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
            }
        }
    }
    public static class AlarmReceiver extends BroadcastReceiver {
        HttpURLConnection Alarmconnection;
        URL SendUrl;
        {
            try {
                SendUrl = new URL("http://imoeedge20220914134800.azurewebsites.net/api/UserTime");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        Calendar Gettime;
        String SendIP = PIP;
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bData = intent.getExtras();
            if(bData.get("title").equals("timelock"))
            {
                //主要執行的程式

                Log.i("add_Alarm_check","receive sucess, time = "+bData.get("time"));
            }
            if(bData.get("title").equals("send_data"))
            {
                //主要執行的程式
                new Thread(new Thread2()).start();
                Log.i("add_Alarm_check","data send");
                Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Taipei")); //取得時間
                cal.add(Calendar.MINUTE, 1);
                add_alarm(context,cal,"send_data",0);
                Log.i("add_Alarm_check","resenddata");
            }
            Log.i("add_Alarm_check","working");
        }
    }
    /***    加入(與系統註冊)鬧鐘    ***/
    public static void add_alarm(Context context, Calendar cal,String title, int reqCode) {
        Log.d("add_Alarm_check", title + " alarm add time: " + String.valueOf(cal.get(Calendar.MONTH)) + "." + String.valueOf(cal.get(Calendar.DATE)) + " " + String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND));

        Intent intent = new Intent(context, AlarmReceiver.class);
        // 以日期字串組出不同的 category 以添加多個鬧鐘
        intent.addCategory("ID." + String.valueOf(cal.get(Calendar.MONTH)) + "." + String.valueOf(cal.get(Calendar.DATE)) + "-" + String.valueOf((cal.get(Calendar.HOUR_OF_DAY) )) + "." + String.valueOf(cal.get(Calendar.MINUTE)) + "." + String.valueOf(cal.get(Calendar.SECOND)));
        String AlarmTimeTag = "Alarmtime " + String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(cal.get(Calendar.MINUTE)) + ":" + String.valueOf(cal.get(Calendar.SECOND));

        intent.putExtra("title", title);
        intent.putExtra("time", AlarmTimeTag);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);       //註冊鬧鐘
    }
    private static void cancel_alarm(Context context, Calendar cal,String title,int reqCode) {
        Log.d("add_Alarm_check", "alarm cancel time: " + String.valueOf(cal.get(Calendar.MONTH)) + "." + String.valueOf(cal.get(Calendar.DATE)) + " " + String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND));

        Intent intent = new Intent(context, AlarmReceiver.class);
        // 以日期字串組出不同的 category 以添加多個鬧鐘
        intent.addCategory("ID." + String.valueOf(cal.get(Calendar.MONTH)) + "." + String.valueOf(cal.get(Calendar.DATE)) + "-" + String.valueOf((cal.get(Calendar.HOUR_OF_DAY) )) + "." + String.valueOf(cal.get(Calendar.MINUTE)) + "." + String.valueOf(cal.get(Calendar.SECOND)));
        String AlarmTimeTag = "Alarmtime " + String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(cal.get(Calendar.MINUTE)) + ":" + String.valueOf(cal.get(Calendar.SECOND));

        intent.putExtra("title", title);
        intent.putExtra("time", AlarmTimeTag);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.cancel(pi);    //取消鬧鐘，只差在這裡
    }

    //websocket
    public class MyWebSocketClient extends WebSocketClient
    {

        public MyWebSocketClient(URI serverUri) {
            super(serverUri,new Draft_6455());
            Log.e("websocket","oncreate,link = "+ serverUri.toString());
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
           Log.e("websocket","onOpen");
           //websocket.send(ID+"connect");
        }

        @Override
        public void onMessage(String message) {
            if (message != null) {
                Log.e("MessageS",message);
                if(message.contains(ID))
                {
                    if(message.contains(ID+"_lock") ) {


                        String text = message.replaceFirst(ID+"_lock","").replaceFirst(":","");

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if(text.length()>0)
                        {
                            handler.post(() -> {

                                LockMessage = text;
                                //screentext.setText(LockMessage);
                                Log.e("onMessage","savetext");
                            });
                        }
                        else
                        {
                            handler.post(() -> {
                                Lock_Flag = true;
                                Lost_Flag = true;
                                LockMessage = LostMessage;
                                //screentext.setText(LockMessage);
                                //LockMessage = LostMessage;
                            });

                        }
                        //editor.putString("message", text).commit();
                        try {
                            Log.i("TestService", "lockMessages");

                            if(Block_Flag )//檢查有沒有已開啟的鎖定,false=未打開
                            {
                                handler.post(() -> {
                                    Log.e("text",Integer.toString(text.length()));
                                    screentext.setText(LockMessage);
                                    if(text.length()>0) {

                                        websocket.send(ID+"set text!");
                                    }
                                });
                            }
                            else
                            {
                                Lock_Flag = true;
                                websocket.send(ID+"lock!");
                            }

                        }
                        catch (IOError e)
                        {
                            e.printStackTrace();
                        }
                        editor.putBoolean("lock", Lock_Flag).putBoolean("lost",Lost_Flag).commit();

                    }
                    else if(message.contains(ID+"_time:")) //input format=>time:HH:mm,HH:mm
                    {
                        try {
                            Log.i("TestService", "timeMessages");


                            String getTime = message.replaceFirst(ID+"_time:","");
                            int length = getTime.split("[,]").length;
                            Log.e("TestService", Integer.toString(length));
                            StartLock = timetoint(getTime.split("[,]")[0]);
                            StopLock = timetoint (getTime.split("[,]")[1]);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("Starttime",StartLock).putInt("Endtime",StopLock).commit();
                            Log.e("time",Integer.toString(StopLock));
                            websocket.send(ID+" set time!");


                        }
                        catch (IOError e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else if(message.contains(ID+"_unlock") )
                    {
                        Lock_Flag = false;
                        Lost_Flag = false;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        //editor.putBoolean("lock",Lock_Flag).commit();
                        editor.putBoolean("lock", Lock_Flag).putBoolean("lost",Lost_Flag).commit();
                        if(!timerlockhandle(StartLock,StopLock))
                        {
                            websocket.send(ID+" unlock!");
                        }

                    }
                    else if (message.contains(ID+"_timereset"))
                    {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("Starttime",-1).putInt("Endtime",-1).commit();
                        StartLock = -1;
                        StopLock = -1;
                        timelock = false;
                        if(Block_Flag)//檢查有沒有已開啟的鎖定
                        {
                            windowManager.removeView(floatView);
                            lockManager.removeView(mMoniterView);
                            unregisterReceiver(mHomeKeyBroadcastReceiver);
                            Block_Flag = false;
                            Log.v("TestService","resettimer");

                        }
                        websocket.send(ID+" timereset!");
                    }
                    else if (message.contains(ID+"_switch:"))//轉移新的websocket
                    {
                        websocketLink = message.replaceFirst(ID+"_switch","").replaceFirst(":","")+ID;
                        //更換連接
                        Log.e("websocket link switch to:",websocketLink);

                        //告知需要轉換socket
                        changesocket = true;

                    }
                    else
                    {
                        String text = message.replaceFirst(ID,"");

                        if(!Block_Flag) {
                            handler.post(() -> {
                                Toast.makeText(getApplicationContext(),
                                        text, Toast.LENGTH_LONG).show();
                                websocket.send(ID + " message!");
                            });
                        }
                        //Log.e("MESSAGE",text);
                    }
                }




            } else {
                Log.e("MessageS","nothing");

            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.e("websocketclose",reason+"|"+code);

            //非正常關閉
            if(code != 1 || code != 1000)
            {
                handler.post(() -> {
                    Toast.makeText(getApplicationContext(),
                            "websocket conn fail",Toast.LENGTH_LONG).show();

                });
            }


            closeReceiveConnect();
        }

        @Override
        public void onError(Exception ex) {
            Log.e("websocketerror",ex.toString());
            //closeReceiveConnect();

        }
    }

    //初始化 websocket
    public void initwebSocket()
    {

        if(null != websocket)
        {
            websocket = null;
        }
        //使用已取得的uri建立連接
        URI uri;
        uri = URI.create(websocketLink);
        Log.e("initwebsocket link:", uri.toString());
        websocket = new MyWebSocketClient(uri);
        try
        {

            websocket.connectBlocking();
            changesocket = false;

        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            Log.e("testwebsocket","testfail");

        }

    }
    //心跳包
    private  static  final long HEART_BEAT_RATE = 2*1000;
    private final Handler mHandler = new Handler();
    //定時檢查鎖定
    private final Runnable LockFlagRunnable = new Runnable() {
        @Override
        public void run() {
            timelock = timerlockhandle(StartLock,StopLock);
            if(timelock)
            {
                if(!Block_Flag)//檢查有沒有已開啟的鎖定,false=未打開
                {
                    if (Settings.canDrawOverlays(ServerService.this)) {
                        handler.post(() -> {
                            setLockManager();
                            setwindow(LostMessage);

                            IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                            mHomeKeyBroadcastReceiver = new HomeKeyBroadcastReceiver();
                            registerReceiver(mHomeKeyBroadcastReceiver, mIntentFilter);
                            Block_Flag=true;

                        });

                        Log.e("lock","lock");

                    } else {
                        Log.e("ERROR", "didn't get the permission");
                    }
                    Block_Flag = true;
                }
            }
            else if(Lock_Flag)
            {
                if(!Block_Flag)//檢查有沒有已開啟的鎖定,false=未打開
                {
                    if (Settings.canDrawOverlays(ServerService.this)) {
                        handler.post(() -> {
                            setLockManager();
                            setwindow(LockMessage);

                            IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                            mHomeKeyBroadcastReceiver = new HomeKeyBroadcastReceiver();
                            registerReceiver(mHomeKeyBroadcastReceiver, mIntentFilter);
                            Block_Flag=true;

                        });

                        Log.e("lock","lock");

                    } else {
                        Log.e("ERROR", "didn't get the permission");
                    }
                    Block_Flag = true;
                }
            }
            else
            {
                if(Block_Flag)//檢查有沒有已開啟的鎖定
                {
                    windowManager.removeView(floatView);
                    lockManager.removeView(mMoniterView);
                    unregisterReceiver(mHomeKeyBroadcastReceiver);
                    Block_Flag = false;
                    Log.v("TestService","unlock");


                }
            }
            if(changesocket)
            {
                closeReceiveConnect();
            }
            mHandler.postDelayed(this,2*1000);
        }
    };
    //定時心跳
    private final Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if(websocket != null)
            {
                if (websocket.isClosed())
                {
                    //重新連接websocket
                    reconnectWs();
                }
                else if (websocket.isOpen())
                {
                    //websocket.send(ID +"still alive");
                    Log.e("websocket","still alive");
                }
            }
            else
            {
                //websocket重新初始化
                websocket= null;
                initwebSocket();
            }



            mHandler.postDelayed(this,HEART_BEAT_RATE);
        }
    };

    //用來呼叫 websocket 連接和開啟定時器的thread
    class Thread1 implements Runnable {
        public void run() {
            //初始化發送資料的定時器
            if(!functiontest) {
                //初始化 並開啟websocket功能
                initwebSocket();
                //打開心跳包
                mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);
                //進行定時lockflag檢查
                mHandler.postDelayed(LockFlagRunnable,2*1000);

            }

        }
    }
    //Call api 上傳載具資料
    static class Thread2 implements  Runnable{
        @Override
        public void run() {
            HttpURLConnection Alarmconnection;
            URL SendUrl = null;
            try {
                SendUrl = new URL("http://imoeedge20220914134800.azurewebsites.net/api/UserTime");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Calendar Gettime;
            String SendIP = PIP;
            try {

                Alarmconnection = (HttpURLConnection) SendUrl.openConnection();
                Alarmconnection.setRequestMethod("POST");
                Alarmconnection.setDoInput(true);
                Alarmconnection.setDoOutput(true);
                Alarmconnection.setUseCaches(false);
                Alarmconnection.setRequestProperty("Content-Type", "application/json");
                Alarmconnection.connect();
                Gettime = Calendar.getInstance();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


                if(!SendIP.isEmpty())
                {
                    SendIP= SendIP.replaceAll("\"","").replaceFirst("ip:","").replace("{","").replace("}","");
                    Log.e("IP",SendIP);
                }
                else
                {
                    return;
                }
                Data data = new Data(SendIP, dtf.format(LocalDateTime.now()), ID,owner);

                JSONObject jsonObject = new JSONObject();
                try{

                    jsonObject.put("publicip",data.publicip);
                    jsonObject.put("uploadtime",data.uploadtime);
                    jsonObject.put("serialnumber",data.serialnumber);
                    jsonObject.put("owner",data.owner);
                    Log.e("HTTP",jsonObject.toString());


                }
                catch(JSONException e) {
                    e.printStackTrace();

                }

                OutputStream outputStream = Alarmconnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                writer.write(jsonObject.toString());
                writer.flush();
                writer.close();
                int responseCode = Alarmconnection.getResponseCode();
                Log.e("API","send");
                if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    Log.e("HTTP","Ok");

                }
                else
                {
                    Log.e("HTTP","not");
                }
                Alarmconnection.disconnect();

            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e("APIFAIL","API :" + SendUrl +" conn fail");
            }
        }
    }
    //websocket 關閉連接用
    private  void closeReceiveConnect()
    {
        try{
            if(null != websocket)
            {
                websocket.close(1000);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            websocket = null;
        }

    }
    //收到switch指令時用


    //重新連線用
    private  void reconnectWs(){
        mHandler.removeCallbacks(heartBeatRunnable);
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Log.e("reconn","reconn");
                    websocket.reconnectBlocking();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private static final Handler handler=new Handler();

    //開始各種server的地方
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // The service is starting, due to a call to startService()
        Log.e("TestService","restart");
        httpCall(ipurl.toString());
        if(!working)
        {
           // closeReceiveConnect();
            try {
                initViews();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            working = true;
            //compName = new ComponentName(ServerService.this,MyAdmin.class);
            boolean isActive = devicePolicyManager.isAdminActive(compName);

            if(isActive)
            {
                Thread1.start();

                Log.e("TestService","restart thread");
            }
            else
            {
                Intent intent1= new Intent();
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent1.setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent1.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,compName);
                startActivity(intent1);
            }

        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // A client is binding to the service with bindService()
        Log.v("TestService","bindService");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent)
    {
        // A client is binding to the service with bindService(), after onUnbind() has already been called
    }


    //Service 關閉時會跑，會重新呼叫自己，用來保活
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // The service is no longer used and is being destroyed
        if(Block_Flag)//檢查有沒有已開啟的鎖定,true 是打開了
        {
            windowManager.removeView(floatView);
            lockManager.removeView(mMoniterView);
            unregisterReceiver(mHomeKeyBroadcastReceiver);
            Block_Flag = false;
        }
        //關閉websocket
        closeReceiveConnect();
        Log.e("TestService","I am died");
        unregisterReceiver(mAlarmBroadcastReceiver);
        //重啟Service
        startForegroundService(new Intent(this, ServerService.class));
    }


}
