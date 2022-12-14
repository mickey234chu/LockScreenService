package com.example.bootservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class FirstActivity extends AppCompatActivity {

    TextView textView;
    EditText et;
    Button btnSend;
    String SN;
    boolean Hrecord = false;
    boolean webtest = false;
    HttpURLConnection connection;
    URL url;
    JSONObject jsonObject = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
       // getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(FirstActivity.this, R.color.light_blue_400)));
        //getSupportActionBar().setDisplayShowTitleEnabled(false);
        Window window = FirstActivity.this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(FirstActivity.this, R.color.purple_500));

        textView = findViewById(R.id.textView);
        et = findViewById(R.id.et);
        btnSend = findViewById(R.id.btnSend);


        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    et.setHint("");

                }
                else
                {
                    if(et.getText().length()<=0)
                    {
                        et.setHint("SN???");
                    }
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SN = et.getText().toString().trim();

                StringBuilder response = new StringBuilder();

                try {
                    if(webtest)
                    {
                        //server for testing function(debug)
                        url = new URL("http://imoeedge20220914134800.azurewebsites.net/api/UnitInfro");

                    }
                    else
                    {
                        //real server we used(debug)
                        url = new URL("http://imoeedge20220914134800.azurewebsites.net/api/UnitInfro");

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                if (!SN.isEmpty()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("SN", SN);
                                connection.connect();


                                int responseCode = connection.getResponseCode();
                                //????????????SN
                                if(responseCode == HttpURLConnection.HTTP_OK )
                                {
                                    InputStream is = connection.getInputStream();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                                    String line;
                                    while ((line = reader.readLine()) != null) {

                                        response.append(line);
                                        response.append('\r');
                                    }
                                    jsonObject = new JSONObject(response.toString().trim());
                                    Log.e("HTTPJSONObject",jsonObject.toString());
                                    Log.e("HTTP","Ok");
                                    if(jsonObject.get("statusCode").toString().equals("00"))
                                    {
                                        Hrecord = true;
                                    }



                                }
                                else//???????????????SN??????
                                {


                                    Hrecord = false;
                                    Log.e("HTTP","Not Ok");
                                }
                                connection.disconnect();
                            }
                            catch(Exception e)
                            {
                                Hrecord = false;
                                e.printStackTrace();
                                Log.e("HTTP",e.toString());
                                Log.e("HTTP","conn fail");
                            }
                            if(Hrecord) {
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                Intent intent = new Intent(FirstActivity.this, MainActivity.class);
                                intent.putExtra("SN", SN);
                                try {
                                    intent.putExtra("socketaddress", jsonObject.getString("socketaddress"));
                                    intent.putExtra("apiaddress", jsonObject.getString("apiaddress"));
                                    intent.putExtra("unitinfro", jsonObject.getString("unitinfro"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                Log.e("SN", SN);
                                setResult(RESULT_OK, intent);
                                finish();
                            }
                            else
                            {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),
                                                "?????????????????????????????????????????????SN??????????????????",Toast.LENGTH_LONG).show();
                                        Log.e("HTTP","fail text");
                                    }
                                });
                                // for testing
                                if(webtest)
                                {
                                    Intent intent = new Intent(FirstActivity.this, MainActivity.class);
                                    intent.putExtra("SN", SN);

                                        intent.putExtra("socketaddress", "ws://imoeedge20220914134800.azurewebsites.net/api/WebSoket?nickName=");
                                        intent.putExtra("apiaddress", "http://imoeedge20220914134800.azurewebsites.net/api/Usertime");
                                        intent.putExtra("unitinfro", "???????????????????????????");


                                    Log.e("SN", SN);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }

                            }
                        }
                    }

                    ).start();




                }
                else
                {
                    Log.e("HTTP","Empty SN");
                }

            }
        });
    }
}

