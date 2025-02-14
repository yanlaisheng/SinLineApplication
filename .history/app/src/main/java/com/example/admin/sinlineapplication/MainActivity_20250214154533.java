package com.example.admin.sinlineapplication;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.admin.sinlineapplication.line.SineLineWave;
import com.example.admin.sinlineapplication.line.BrokenLineWave;
import com.example.admin.sinlineapplication.util.HttpUtil;
import com.example.admin.sinlineapplication.wifi.MyWifiManager;
import com.example.admin.sinlineapplication.wifi.PermissionsChecker;
import com.example.admin.sinlineapplication.wifi.WifiListBean;
import com.example.admin.sinlineapplication.serial.ModbusDataManager;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import com.example.admin.sinlineapplication.wifi.WifiAdmin;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;

    private SharedPreferences userSettings;
    private TextView title, time, num_u, num_v, num_w;

    private EditText frequency, speed, extremum, amplifier_u, amplifier_v, amplifier_w;
    private Button setting, change;

    private RelativeLayout line_view;

    public static final String SILENCE_ROOT_INSTALL = "SilenceRootInstall";
    private String apkUrl = "http://dakaapp.troila.com/download/daka.apk?v=3.0";

    private PermissionsChecker mPermissionsChecker; // 权限检测器
    private final int RESULT_CODE_LOCATION = 0x001;
    // 定位权限,获取app内常用权限
    String[] permsLocation = {
            "android.permission.ACCESS_WIFI_STATE", "android.permission.CHANGE_WIFI_STATE",
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION" };
    private WifiManager mWifiManager;
    private WifiBroadcastReceiver wifiReceiver;

    private List<ScanResult> mScanResultList;// wifi列表
    private List<WifiListBean> wifiListBeanList;
    private String wifiName;

    private int interval = 0;

    private final Handler handler = new Handler();
    private Runnable updateRunnable;

    private HttpUtil httpUtil = new HttpUtil();
    public static double tempDouble;
    public static double tempDouble2;

    private final Runnable task = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            updateTime();
            interval++;
            handler.postDelayed(this, 1000);
        }

        // 待开发内容：

        // 提高原文的阅读速度

    };
    private BrokenLineWave sw = null;
    private SineLineWave bw = null;

    private ModbusDataManager modbusDataManager;
    private WifiAdmin wifiAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (mWifiManager == null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        userSettings = getSharedPreferences("setting", 0);
        bw = (SineLineWave) findViewById(R.id.main_bw);
        sw = (BrokenLineWave) findViewById(R.id.main_sw);

        if (userSettings.contains("extremum") &&
                userSettings.contains("amplifier_u") &&
                userSettings.contains("amplifier_v") &&
                userSettings.contains("amplifier_w") &&
                userSettings.contains("frequency") &&
                userSettings.contains("speed")) {
            sw.Set(
                    userSettings.getFloat("extremum", sw.GetExtremum()),
                    userSettings.getFloat("amplifier_u", sw.getAmplifier_u()),
                    userSettings.getFloat("amplifier_v", sw.getAmplifier_v()),
                    userSettings.getFloat("amplifier_w", sw.getAmplifier_w()),
                    userSettings.getFloat("frequency", sw.GetFrequency()),
                    userSettings.getFloat("speed", sw.GetSpeed()));
        }
        if (userSettings.contains("extremumA") &&
                userSettings.contains("max_u") &&
                userSettings.contains("max_v") &&
                userSettings.contains("max_w") &&
                // userSettings.contains("frequency") &&
                userSettings.contains("speedA")) {
            bw.Set(
                    userSettings.getFloat("extremumA", bw.GetExtremum()),
                    userSettings.getFloat("max_u", bw.getAmplifier_u()),
                    userSettings.getFloat("max_v", bw.getAmplifier_v()),
                    userSettings.getFloat("max_w", bw.getAmplifier_w()),
                    // userSettings.getFloat("frequency", sw.GetFrequency()),
                    userSettings.getFloat("speedA", bw.GetSpeed()));
        }
        sw.setVisibility(View.GONE);

        title = (TextView) findViewById(R.id.title);
        time = (TextView) findViewById(R.id.time);
        num_u = (TextView) findViewById(R.id.num_u);
        num_v = (TextView) findViewById(R.id.num_v);
        num_w = (TextView) findViewById(R.id.num_w);

        frequency = findViewById(R.id.frequency);
        speed = findViewById(R.id.speed);
        extremum = findViewById(R.id.extremum);
        amplifier_u = findViewById(R.id.amplifier_u);
        amplifier_v = findViewById(R.id.amplifier_v);
        amplifier_w = findViewById(R.id.amplifier_w);

        change = findViewById(R.id.parameter_change);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bw.setVisibility(bw.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                sw.setVisibility(sw.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                initView();
            }

        });

        AtomicInteger count = new AtomicInteger();

        ArrayList<String> numberList = new ArrayList<>();

        findViewById(R.id.getBtn).setOnClickListener(view -> {
            // 获取温度和湿度数据
            Map<String, String> resMap = httpUtil.getWenDuAndShiDu(findViewById(R.id.x_1), findViewById(R.id.y_1));
            Runnable runnable = null;
            handler.removeCallbacks(runnable);
            // 获取温度和湿度的字符串值
            String tempValue = resMap.get("tempValue");
            String humidityValue = resMap.get("humidityValue");

            // 将 tempValue 转换为 double 类型
            tempDouble = Double.parseDouble(tempValue);
            tempDouble2 = Double.parseDouble(humidityValue);

            // 计算温度的多个值，每次点击都会重新计算
            String[] numbers1 = {
                    String.valueOf(tempDouble + 0.3),
                    String.valueOf(tempDouble + 0.5),
                    String.valueOf(tempDouble + 0.6),
                    String.valueOf(tempDouble + 0.8),
                    String.valueOf(tempDouble + 0.9),
                    String.valueOf(tempDouble + 1.1),
                    String.valueOf(tempDouble + 1.3),
                    String.valueOf(tempDouble + 0.9),
                    String.valueOf(tempDouble + 0.6),
                    String.valueOf(tempDouble + 0.4)

            };
            String[] numbers5 = {
                    String.valueOf(tempDouble - 0.6),
                    String.valueOf(tempDouble - 0.9),
                    String.valueOf(tempDouble - 1.6),
                    String.valueOf(tempDouble - 1.5),
                    String.valueOf(tempDouble - 1.1),
                    String.valueOf(tempDouble - 0.4),
                    String.valueOf(tempDouble - 0.9),
                    String.valueOf(tempDouble - 0.7),
                    String.valueOf(tempDouble - 0.6),
                    String.valueOf(tempDouble - 0.3)

            };
            String[] numbers4 = {
                    String.valueOf(tempDouble + 0.6),
                    String.valueOf(tempDouble + 0.9),
                    String.valueOf(tempDouble + 0.3),
                    String.valueOf(tempDouble + 3.1),
                    String.valueOf(tempDouble + 4.2),
                    String.valueOf(tempDouble + 1.6),
                    String.valueOf(tempDouble + 0.9),
                    String.valueOf(tempDouble - 0.4),
                    String.valueOf(tempDouble - 0.6),
                    String.valueOf(tempDouble - 0.7)
            };
            String[] numbers2 = { "16A", "12.8A", "9.6A", "6.4A"
            };
            String[] numbers3 = { "14%", "18%", "14%", "14%", "14%"
            };

            String[] numbers = {
                    String.valueOf(tempDouble2 + 0.2),
                    String.valueOf(tempDouble2 + 0.1),
                    String.valueOf(tempDouble2 + 0.4),
                    String.valueOf(tempDouble2 + 0.1),
                    String.valueOf(tempDouble2 + 0.5),
                    String.valueOf(tempDouble2 + 0.3),
                    String.valueOf(tempDouble2 + 0.4),
                    String.valueOf(tempDouble2 + 0.1),
                    String.valueOf(tempDouble2 + 0.2),
                    String.valueOf(tempDouble2 + 0.3)

            };

            String[] numbersx = {
                    String.valueOf(tempDouble2 + 0.4),
                    String.valueOf(tempDouble2 + 0.2),
                    String.valueOf(tempDouble2 + 0.1),
                    String.valueOf(tempDouble2 + 0.2),
                    String.valueOf(tempDouble2 + 0.3),
                    String.valueOf(tempDouble2 + 0.4),
                    String.valueOf(tempDouble2 + 0.5),
                    String.valueOf(tempDouble2 + 0.6),
                    String.valueOf(tempDouble2 + 0.3),
                    String.valueOf(tempDouble2 + 0.2)

            };

            String[] numbersy = {
                    String.valueOf(tempDouble2 + 0.2),
                    String.valueOf(tempDouble2 + 0.3),
                    String.valueOf(tempDouble2 + 0.4),
                    String.valueOf(tempDouble2 + 0.5),
                    String.valueOf(tempDouble2 + 0.6),
                    String.valueOf(tempDouble2 + 0.3),
                    String.valueOf(tempDouble2 + 0.2),
                    String.valueOf(tempDouble2 + 0.2),
                    String.valueOf(tempDouble2 + 0.3),
                    String.valueOf(tempDouble2 + 0.1)

            };

            // 获取 TextView 引用
            TextView textView1 = findViewById(R.id.x_1);
            TextView textView2 = findViewById(R.id.x_2);
            TextView textView3 = findViewById(R.id.x_3);
            TextView textView4 = findViewById(R.id.y_1);
            TextView textView5 = findViewById(R.id.y_2);
            TextView textView6 = findViewById(R.id.y_3);
            TextView textView7 = findViewById(R.id.c_1);
            TextView textView8 = findViewById(R.id.c_2);
            TextView textView9 = findViewById(R.id.c_3);
            TextView textView10 = findViewById(R.id.d_1);
            TextView textView11 = findViewById(R.id.d_2);
            TextView textView12 = findViewById(R.id.d_3);

            // 创建 Handler
            Handler handler = new Handler();

            // 创建 Runnable
            runnable = new Runnable() {
                int index1 = 0;
                int index2 = 0;
                int index3 = 0;
                int index4 = 0;
                int index5 = 0;
                int index6 = 0;
                int index7 = 0;
                int index8 = 0;
                int index9 = 0;
                int index10 = 0;
                int index11 = 0;
                int index12 = 0;

                @Override
                public void run() {
                    // 检查索引并重置
                    if (index1 >= numbers.length) {
                        index1 = 0;
                    }
                    if (index2 >= numbersx.length) {
                        index2 = 0;
                    }
                    if (index3 >= numbersy.length) {
                        index3 = 0;
                    }
                    if (index4 >= numbers1.length) {
                        index4 = 0;
                    }
                    if (index5 >= numbers4.length) {
                        index5 = 0;
                    }
                    if (index6 >= numbers5.length) {
                        index6 = 0;
                    }
                    if (index7 >= numbers2.length) {
                        index7 = 0;
                    }
                    if (index8 >= numbers2.length) {
                        index8 = 0;
                    }
                    if (index9 >= numbers2.length) {
                        index9 = 0;
                    }
                    if (index10 >= numbers3.length) {
                        index10 = 0;
                    }
                    if (index11 >= numbers3.length) {
                        index11 = 0;
                    }
                    if (index12 >= numbers3.length) {
                        index12 = 0;
                    }

                    // 更新 TextView
                    textView1.setText(numbers[index1]);
                    textView2.setText(numbersx[index2]);
                    textView3.setText(numbersy[index3]);
                    textView4.setText(numbers1[index4]);
                    textView5.setText(numbers4[index5]);
                    textView6.setText(numbers5[index6]);
                    textView7.setText(numbers2[index7]);
                    textView8.setText(numbers2[index8]);
                    textView9.setText(numbers2[index9]);
                    textView10.setText(numbers3[index10]);
                    textView11.setText(numbers3[index11]);
                    textView12.setText(numbers3[index12]);

                    // 增加索引
                    index1++;
                    index2++;
                    index3++;
                    index4++;
                    index5++;
                    index6++;
                    index7++;
                    index8++;
                    index9++;
                    index10++;
                    index11++;
                    index12++;
                    // 延迟3秒后再次运行
                    handler.postDelayed(this, 3000);
                }

            };
            // 移除之前的任务
            handler.removeCallbacks(runnable);

            // 立即执行 Runnable，然后每3秒执行一次
            handler.post(runnable);
        });

        setting = findViewById(R.id.parameter_setting);
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!frequency.getText().toString().trim().equals("")
                        && !speed.getText().toString().trim().equals("")
                        && !extremum.getText().toString().trim().equals("")
                        && !amplifier_u.getText().toString().trim().equals("")
                        && !amplifier_v.getText().toString().trim().equals("")
                        && !amplifier_w.getText().toString().trim().equals("")) {
                    num_u.setText(amplifier_u.getText().toString());
                    num_v.setText(amplifier_v.getText().toString());
                    num_w.setText(amplifier_w.getText().toString());

                    if (sw.getVisibility() == View.VISIBLE) {
                        sw.Set(
                                Float.parseFloat(extremum.getText().toString()),
                                Float.parseFloat(amplifier_u.getText().toString()),
                                Float.parseFloat(amplifier_v.getText().toString()),
                                Float.parseFloat(amplifier_w.getText().toString()),
                                Float.parseFloat(frequency.getText().toString()),
                                Float.parseFloat(speed.getText().toString()));

                    }

                    if (bw.getVisibility() == View.VISIBLE) {
                        bw.Set(
                                Float.parseFloat(extremum.getText().toString()),
                                Float.parseFloat(amplifier_u.getText().toString()),
                                Float.parseFloat(amplifier_v.getText().toString()),
                                Float.parseFloat(amplifier_w.getText().toString()),
                                Float.parseFloat(speed.getText().toString()));
                    }

                } else {
                    Toast.makeText(MainActivity.this, "信息输入错误，请确认！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // line_view = (RelativeLayout) findViewById(R.id.line_view);
        // line_view.setOnLongClickListener(new View.OnLongClickListener() {
        // @Override
        // public boolean onLongClick(View v) {
        // showAlertDialog();
        // return false;
        // }
        // });

        // WiFi功能相关
        {
            wifiListBeanList = new ArrayList<>();
            // 权限检测
            getPerMission();
            // 开启wifi
            MyWifiManager.openWifi(mWifiManager);
            // 扫描WiFi
            MyWifiManager.startScanWifi(mWifiManager);
            // 获取到wifi列表
            mScanResultList = MyWifiManager.getWifiList(mWifiManager);
            for (int i = 0; i < mScanResultList.size(); i++) {
                WifiListBean wifiListBean = new WifiListBean();
                wifiListBean.setName(mScanResultList.get(i).SSID);
                wifiListBean.setEncrypt(MyWifiManager.getEncrypt(mWifiManager, mScanResultList.get(i)));
                wifiListBeanList.add(wifiListBean);
            }

            if (wifiListBeanList.size() > 0 && !isWifiConnected()) {
                for (int i = 0; i < wifiListBeanList.size(); i++) {
                    // 确定
                    wifiName = wifiListBeanList.get(i).getName();
                    MyWifiManager.disconnectNetwork(mWifiManager);// 断开当前wifi
                    String type = MyWifiManager.getEncrypt(mWifiManager, mScanResultList.get(i));// 获取加密方式

                    Toast.makeText(this, "连接wifi:\n" + wifiName + "\n加密方式:\n" + type, Toast.LENGTH_SHORT).show();

                    if (type.equals("没密码")) {
                        MyWifiManager.connectWifi(mWifiManager, wifiName, null, type);// 连接wifi
                    } else {
                        MyWifiManager.connectWifi(mWifiManager, wifiName, "liu123456789", type);// 连接wifi
                    }
                }
            }
        }

        // 请求必要权限
        checkAndRequestPermissions();

        // 时钟一秒刷新一次
        handler.postDelayed(task, 1000);
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };

            boolean needRequest = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    needRequest = true;
                    break;
                }
            }

            if (needRequest) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            } else {
                initModbusManager();
            }
        } else {
            initModbusManager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initModbusManager();
            } else {
                Toast.makeText(this, "需要相关权限才能正常运行", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initModbusManager() {
        modbusDataManager = new ModbusDataManager();
        modbusDataManager.setOnDataUpdateListener(new ModbusDataManager.OnDataUpdateListener() {
            @Override
            public void onDataUpdate(final float[] data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUIWithModbusData(data);
                    }
                });
            }
        });
        modbusDataManager.startQuery();
    }

    private void initView() {
        if (sw.getVisibility() == View.VISIBLE) {
            title.setText("电源质量分析图(电流)");
            num_u.setText(Float.toString(sw.getAmplifier_u()));
            num_v.setText(Float.toString(sw.getAmplifier_v()));
            num_w.setText(Float.toString(sw.getAmplifier_w()));

            frequency.setText(Float.toString(sw.GetFrequency()));
            speed.setText(Float.toString(sw.GetSpeed()));
            extremum.setText(Float.toString(sw.GetExtremum()));
            amplifier_u.setText(Float.toString(sw.getAmplifier_u()));
            amplifier_v.setText(Float.toString(sw.getAmplifier_v()));
            amplifier_w.setText(Float.toString(sw.getAmplifier_w()));
        }
        if (bw.getVisibility() == View.VISIBLE) {
            title.setText("电源质量分析图(电压)");
            num_u.setText(Float.toString(bw.getAmplifier_u()));
            num_v.setText(Float.toString(bw.getAmplifier_v()));
            num_w.setText(Float.toString(bw.getAmplifier_w()));
            // frequency.setText("0.0");
            frequency.setText(Float.toString(bw.GetFrequency()));
            speed.setText(Float.toString(bw.GetSpeed()));
            extremum.setText(Float.toString(bw.GetExtremum()));
            amplifier_u.setText(Float.toString(bw.getAmplifier_u()));
            amplifier_v.setText(Float.toString(bw.getAmplifier_v()));
            amplifier_w.setText(Float.toString(bw.getAmplifier_w()));
        }
    }

    // 获取权限
    private void getPerMission() {
        mPermissionsChecker = new PermissionsChecker(MainActivity.this);
        if (mPermissionsChecker.lacksPermissions(permsLocation)) {
            ActivityCompat.requestPermissions(MainActivity.this, permsLocation, RESULT_CODE_LOCATION);
        }
    }

    // 监听wifi变化
    private void registerReceiverWifi() {
        wifiReceiver = new WifiBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);// 监听wifi是开关变化的状态
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);// 监听wifi连接状态
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);// 监听wifi列表变化（开启一个热点或者关闭一个热点）
        registerReceiver(wifiReceiver, filter);
    }

    // 判断WiFi是否连接
    public boolean isWifiConnected() {

        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
        if (mWifiManager.isWifiEnabled() && ipAddress != 0) {
            return true;
        }
        return false;
    }

    // 监听wifi状态广播接收器
    public class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                // wifi开关变化
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (state) {
                    case WifiManager.WIFI_STATE_DISABLED: {
                        // wifi关闭
                        Log.e("=====", "已经关闭");
                        Toast.makeText(context, "\n 打开变化：wifi已经关闭", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case WifiManager.WIFI_STATE_DISABLING: {
                        // wifi正在关闭
                        Log.e("=====", "正在关闭");
                        Toast.makeText(context, "\n 打开变化：wifi正在关闭", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLED: {
                        // wifi已经打开
                        Log.e("=====", "已经打开");
                        Toast.makeText(context, "\n 打开变化：wifi已经打开", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case WifiManager.WIFI_STATE_ENABLING: {
                        // wifi正在打开
                        Log.e("=====", "正在打开");
                        Toast.makeText(context, "\n 打开变化：wifi正在打开", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case WifiManager.WIFI_STATE_UNKNOWN: {
                        // 未知
                        Log.e("=====", "未知状态");
                        Toast.makeText(context, "\n 打开变化：wifi未知状态", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                // 监听wifi连接状态
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                Log.e("=====", "--NetworkInfo--" + info.toString());

                if (NetworkInfo.State.DISCONNECTED == info.getState()) {// wifi没连接上

                    Log.e("=====", "wifi没连接上");
                    Toast.makeText(context, "\n 连接状态：wifi没连接上", Toast.LENGTH_SHORT).show();

                } else if (NetworkInfo.State.CONNECTED == info.getState()) {// wifi连接上了

                    Log.e("=====", "wifi已连接");
                    Toast.makeText(context, "\n 连接状态：wifi已连接，wifi名称："
                            + MyWifiManager.getWiFiName(mWifiManager), Toast.LENGTH_SHORT).show();

                } else if (NetworkInfo.State.CONNECTING == info.getState()) {// 正在连接

                    Log.e("=====", "wifi正在连接");
                    Toast.makeText(context, "\n 连接状态：wifi正在连接", Toast.LENGTH_SHORT).show();

                }
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                // 监听wifi列表变化
                Log.e("=====", "wifi列表发生变化");
            }
        }
    }

    private void updateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");// HH:mm:ss
        // 获取当前时间
        Date date = new Date(System.currentTimeMillis());
        time.setText(simpleDateFormat.format(date));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");// HH:mm:ss

        if (interval == 4) {
            interval = 0;
            if (sw.times.size() >= 10) {
                sw.times.remove(0);
            }
            sw.times.add(timeFormat.format(date));

            if (bw.times.size() >= 10) {
                bw.times.remove(0);
            }
            bw.times.add(timeFormat.format(date));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // registerReceiverWifi();//监听wifi变化
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (modbusDataManager != null) {
            modbusDataManager.stopQuery();
        }
    }

    private void updateUIWithModbusData(float[] data) {
        if (data.length >= 5) {
            num_u.setText(String.format("%.2f", data[0]));
            num_v.setText(String.format("%.2f", data[1]));
            num_w.setText(String.format("%.2f", data[2]));
            frequency.setText(String.format("%.2f", data[3]));
            speed.setText(String.format("%.2f", data[4]));

            if (data.length >= 8) {
                extremum.setText(String.format("%.2f", data[5]));
                amplifier_u.setText(String.format("%.2f", data[6]));
                amplifier_v.setText(String.format("%.2f", data[7]));
                amplifier_w.setText(String.format("%.2f", data[8]));
            }
        }
    }
}
