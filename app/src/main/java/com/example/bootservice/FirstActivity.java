package com.example.bootservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
    Spinner URL_Spinner;
    Button btnSend;
    String SN,domain;
    boolean Hrecord = false;
    boolean webtest = true;
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

        //Spinner text
        URL_Spinner = findViewById(R.id.URL_Spinner);
        //Spinner value(URL)
        String[] link = getResources().getStringArray(R.array.URL_list_value);

        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(this,    //對應的Context
                        R.array.URL_list,                             //資料選項內容
                        R.layout.myspinner);  //預設Spinner未展開時的View(預設及選取後樣式)

        adapter.setDropDownViewResource(R.layout.myspinner_background);
        URL_Spinner.setAdapter(adapter);
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
                        et.setHint("SN碼");
                    }
                    InputMethodManager imm =  (InputMethodManager)FirstActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(imm != null) {
                        imm.hideSoftInputFromWindow(FirstActivity.this.getWindow().getDecorView().getWindowToken(), 0);
                    }
                }
            }
        });

        URL_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                et.clearFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SN = et.getText().toString().trim();
                et.clearFocus();
                //根據選擇提取對應的URL
                domain = link[URL_Spinner.getSelectedItemPosition()];
                Log.d("HTTP", domain);
                StringBuilder response = new StringBuilder();

                try {
                    if(webtest)
                    {
                        //server for testing function(debug)
                        url = new URL("http://imoeedge20220914134800.azurewebsites.net/api/UnitInfro");

                    }
                    else
                    {
                        //real server we used
                        //抓分區資訊輸入當連接link
                        url = new URL(domain);

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                //輸入欄位判斷
                if (!SN.isEmpty() && !domain.equals("0")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("SN", SN);
                                connection.setRequestProperty("domain", domain);
                                connection.connect();

                                //回傳判斷
                                int responseCode = connection.getResponseCode();
                                //database 存有本機SN
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
                                else//database不存在本機SN記錄
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
                            //API response result 處理
                            if(Hrecord) {
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                Intent intent = new Intent(FirstActivity.this, MainActivity.class);
                                intent.putExtra("SN", SN);
                                intent.putExtra("domain", domain);
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


                                // for testing
                                if(webtest)
                                {
                                    Intent intent = new Intent(FirstActivity.this, MainActivity.class);
                                    intent.putExtra("SN", SN);
                                        // add testing data
                                        intent.putExtra("socketaddress", "ws://imoeedge20220914134800.azurewebsites.net/api/WebSoket?nickName=");
                                        intent.putExtra("apiaddress", "http://imoeedge20220914134800.azurewebsites.net/api/Usertime");
                                        intent.putExtra("unitinfro", "基隆市市立八堵國小");

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(),
                                                    "測試",Toast.LENGTH_LONG).show();
                                            Log.e("HTTP","fail text");
                                        }
                                    });
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
                                                    "登記失敗，請檢查網路連線、確定輸入之SN碼無誤、以及選擇之所屬分區是否正確",Toast.LENGTH_LONG).show();
                                            Log.e("HTTP","fail text");
                                        }
                                    });
                                }

                            }
                        }
                    }

                    ).start();




                }
                else
                {
                    String errormessage = "缺失以下資訊:\n";
                    if(SN.isEmpty())
                    {
                        errormessage += "SN碼\n";
                    }
                    if(domain.equals("0"))
                    {
                        errormessage += "分區資訊\n";
                    }
                    Log.e("HTTP",errormessage);
                }

            }
        });
    }
}

