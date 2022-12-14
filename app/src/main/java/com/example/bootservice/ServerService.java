package com.example.bootservice;


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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;

public class ServerService extends Service {


    IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used
    boolean socketest = false;
    static boolean working= false;
    private boolean Block_Flag = false;
    private boolean Lock_Flag = false;
    private boolean Lost_Flag = false;
    private MyWebSocketClient websocket;
    private Thread Thread1 = null;

    private String pubIP;

    Timer timer = new Timer();
    Timer locktimer = new Timer();

    TimerTask task ;
    TimerTask locktask;

    //window setting
    private View floatView;
    private TextView screentext;

    //Lockscreen Manager,???????????????????????????
    private WindowManager.LayoutParams floatWindowLayoutParam;
    private WindowManager windowManager;
    private WindowInsetsController controller;
    private WindowInsetsController controller2;
    private String LockMessage;
    private String LostMessage;

    //blockinput ???????????????
    private WindowManager.LayoutParams params;
    private WindowManager lockManager;
    private MoniterView mMoniterView;
    private GestureDetector mGestureDetector;
    //????????????
    private DevicePolicyManager devicePolicyManager;

    //?????????
    private int StartLock = 0;
    private int StopLock = 0;
    private boolean timelock = false;
    private Calendar Gettime;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private ComponentName compName;

    private HomeKeyBroadcastReceiver mHomeKeyBroadcastReceiver;
    //SN
    private SharedPreferences sharedPreferences;
    public static String ID;


    HttpURLConnection connection;
    private String apiaddress;
    //api address
    URL url;
    //?????? ip
    URL ipurl = new URL("https://api.ipify.org?format=json");
    //websocket
    URI uri;
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

    //?????????
    private void initViews() throws IOException {
        //channel = new NotificationChannel("testopen", "test", NotificationManager.IMPORTANCE_HIGH);
        httpCall(ipurl.toString());
        //?????????SN
        sharedPreferences = getSharedPreferences("SN", 0);
        ID = sharedPreferences.getString("id","");

        //???websocket

        apiaddress = sharedPreferences.getString("apiaddress","");
        //?????????????????????API?????????
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

        //???????????????
        StartLock = sharedPreferences.getInt("Starttime",-1);
        StopLock = sharedPreferences.getInt("Endtime",-1);

        //???????????????????????????????????????
        LostMessage = sharedPreferences.getString("unitinfro","")+"\n????????????";
        LockMessage = "";
        Log.e("unitinfro",sharedPreferences.getString("unitinfro","null"));


        if(timerlockhandle(StartLock,StopLock))
        {
            timelock = true;
        }

        //??????lock flag,lost flag ????????????????????????????????????
        Lock_Flag = sharedPreferences.getBoolean("lock",false);
        Lost_Flag = sharedPreferences.getBoolean("lost",false);

        //?????????device Manager???????????????lock now ????????????
        mGestureDetector = new GestureDetector(null, new MyGestureDetectorListener(),null);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, MyAdmin.class);

        //?????????????????????????????????
        newlocktimer();
        locktimer.schedule(locktask,0,2*1000);

        //????????????socket
        Thread1 = new Thread(new Thread1());
    }
    //??????????????????????????????????????????
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
    //??????????????????????????????????????????
    private boolean timerlockhandle(int start,int end)
    {
        Gettime = Calendar.getInstance();
        //??????,???
        int hour = Gettime.get(Calendar.HOUR_OF_DAY);
        int minute = Gettime.get(Calendar.MINUTE);
        //????????????????????????????????????
        int minuteofDay = hour*60 + minute;

        if(minuteofDay == 0)
        {
            minuteofDay = 24*60;
        }
        //3??????????????????:
        // start ??? end ??? (???09:00-13:00) => ??? minuteofDay ???????????????????????????
        // start ??? end ??? (???23:00-01:00) => ??? minuteofDay>=start??????<=end???????????????//
        // start == end => minuteofDay == ?????????????????????//
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
    //??????????????????(????????????)
    private  int timetoint(String time)
    {

        int hour = Integer.parseInt(time.split("[:]")[0]);
        int minutes = Integer.parseInt(time.split("[:]")[1]);
        int minutesofDay = hour*60 + minutes;
        Log.e("time",Integer.toString(minutesofDay));
        return minutesofDay;
    }

    //??????????????????
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
    //????????????????????????
    private void newtimer() {
        //httppost
        task = new TimerTask() {
            @Override
            public void run() {

                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.connect();
                    Gettime = Calendar.getInstance();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


                    if(!pubIP.isEmpty())
                    {
                       pubIP= pubIP.replaceAll("\"","").replaceFirst("ip:","").replace("{","").replace("}","");
                       Log.e("IP",pubIP);
                    }
                    else
                    {
                        return;
                    }
                    Data data = new Data(pubIP, dtf.format(LocalDateTime.now()), ID,LockMessage);

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

                   OutputStream outputStream = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();
                    int responseCode = connection.getResponseCode();
                    Log.e("API","send");
                    if(responseCode == HttpURLConnection.HTTP_OK)
                    {

                        Log.e("HTTP","Ok");
                        handler.post(() -> Toast.makeText(getApplicationContext(),
                                "try post!",Toast.LENGTH_LONG).show());


                    }
                    else
                    {

                        Log.e("HTTP","not");
                        handler.post(() -> Toast.makeText(getApplicationContext(),
                                "fail!",Toast.LENGTH_LONG).show());
                    }
                    connection.disconnect();

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e("APIFAIL","API :" + url +" conn fail");
                }


            }

        };
        timer = new Timer();
    }
    //????????????????????????
    private void newlocktimer()
    {
        //????????????
        locktask = new TimerTask() {
            @Override
            public void run() {

                timelock = timerlockhandle(StartLock, StopLock);

                if(timelock)
                {
                    if(!Block_Flag)//?????????????????????????????????,false=?????????
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
                    if(!Block_Flag)//?????????????????????????????????,false=?????????
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
                    if(Block_Flag)//?????????????????????????????????
                    {
                        windowManager.removeView(floatView);
                        lockManager.removeView(mMoniterView);
                        unregisterReceiver(mHomeKeyBroadcastReceiver);
                        Block_Flag = false;
                        Log.v("TestService","unlock");


                    }
                }


            }
        };
        locktimer = new Timer();
    }
    //????????????Server?????????
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
    //?????????????????????
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
    //????????????????????????????????????
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
    //?????????IP
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
                Log.e("IP",pubIP);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("pubIP",pubIP).commit();
        }, error -> pubIP = "0.0.0.0");

        mRequestQueue.add(mStringRequest);
    }
    //??????statusBars
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

    //websocket
    public class MyWebSocketClient extends WebSocketClient
    {

        public MyWebSocketClient(URI serverUri) {
            super(serverUri,new Draft_6455());
            Log.e("websocket","oncreate");
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

                            if(Block_Flag )//?????????????????????????????????,false=?????????
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

                        if(Block_Flag)//?????????????????????????????????
                        {
                            windowManager.removeView(floatView);
                            lockManager.removeView(mMoniterView);
                            unregisterReceiver(mHomeKeyBroadcastReceiver);
                            Block_Flag = false;
                            Log.v("TestService","resettimer");

                        }
                        websocket.send(ID+" timereset!");
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

                //websocket.close();
                //initwebSocket();
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.e("websocketclose",reason+"|"+code);

            handler.post(() -> {
                Toast.makeText(getApplicationContext(),
                        "websocket conn fail",Toast.LENGTH_LONG).show();

            });
            closeReceiveConnect();
        }

        @Override
        public void onError(Exception ex) {
            Log.e("websocketerror",ex.toString());
            //closeReceiveConnect();

        }
    }

    //????????? websocket
    public void initwebSocket()
    {

        if(socketest)
        {
            //???????????????websocket
            uri = URI.create("ws://imoeedge20220914134800.azurewebsites.net/api/WebSoket?nickName="+ID);
            Log.e("test",uri.toString());
        }
        else
        {
            //?????????????????????????????????websocket
            uri = URI.create(sharedPreferences.getString("socketaddress","")+ID);
        }

        Log.e("socketaddress",sharedPreferences.getString("socketaddress","null"));
        if(null != websocket)
        {
            websocket = null;
        }
        websocket = new MyWebSocketClient(uri);
        try
        {

            websocket.connectBlocking();

        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            Log.e("testwebsocket","testfail");

        }

    }
    //?????????
    private  static  final long HEART_BEAT_RATE = 120*1000;
    private final Handler mHandler = new Handler();
    private final Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if(websocket != null)
            {
                if (websocket.isClosed())
                {
                    //????????????websocket
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
                //websocket???????????????
                websocket= null;
                initwebSocket();
            }



            mHandler.postDelayed(this,HEART_BEAT_RATE);
        }
    };

    //???????????? websocket ???????????????????????????thread
    class Thread1 implements Runnable {
        public void run() {
            //?????????????????????????????????
            timer.cancel();
            newtimer();
            timer.schedule(task,3*1000,60*1000);

            //????????? ?????????websocket??????
            initwebSocket();

            //???????????????
            mHandler.postDelayed(heartBeatRunnable,HEART_BEAT_RATE);

        }
    }
    //websocket ???????????????
    private  void closeReceiveConnect()
    {
        try{
            if(null != websocket)
            {
                websocket.close(1);
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
    //???????????????
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

    //????????????server?????????
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


    //Service ??????????????????????????????????????????????????????
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // The service is no longer used and is being destroyed
        if(Block_Flag)//?????????????????????????????????,true ????????????
        {
            windowManager.removeView(floatView);
            lockManager.removeView(mMoniterView);
            unregisterReceiver(mHomeKeyBroadcastReceiver);
            Block_Flag = false;
        }
        //??????websocket
        closeReceiveConnect();
        Log.e("TestService","I am died");
        //??????Service
        startForegroundService(new Intent(this, ServerService.class));
    }


}
