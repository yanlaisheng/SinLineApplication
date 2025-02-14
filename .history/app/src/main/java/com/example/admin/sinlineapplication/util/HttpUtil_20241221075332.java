package com.example.admin.sinlineapplication.util;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class HttpUtil {
    private static final String Accept = "application/json, text/plain, */*";
    private static String Authorization;

    Button GET;
//    TextView data, data1, data2, data3, data4;
    String time, value, value1, value2, value3;


    long time_str;

    public String getValue() {
        return value;
    }

    public String getValue1() {
        return value1;
    }

public Map<String, String> getWenDuAndShiDu(TextView data , TextView data1 ) {
    Map<String, String> resultMap = new HashMap<>();

    // 鉴权值获取
    String Authorization = "";
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Authorization = Token.token();
        }
        Log.d("Token", Authorization);
    } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
        e.printStackTrace();
    }


    // 异步执行第一个请求
    String finalAuthorization = Authorization;
    CompletableFuture<Void> futureTemp = null;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        futureTemp = CompletableFuture.runAsync(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://iot-api.heclouds.com/datapoint/history-datapoints?product_id=90VoX7q1Z9&device_name=test3&datastream_id=temp")
                    .addHeader("Authorization", finalAuthorization)
                    .addHeader("Accept", Accept)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    String responseBody = response.body().string();
                    parseJSONWithGSON(responseBody);
                    JsonRootBean app = new Gson().fromJson(responseBody, JsonRootBean.class);
                    List<Datastreams> streams = app.getData().getDatastreams();
                    List<Datapoints> points = streams.get(0).getDatapoints();
                    String value = points.get(0).getValue();
                    String time = points.get(0).getAt();
                    resultMap.put("tempValue", value);
                    resultMap.put("tempTime", time);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    // 异步执行第二个请求
    String finalAuthorization1 = Authorization;
    CompletableFuture<Void> futureHumidity = null;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        futureHumidity = CompletableFuture.runAsync(() -> {
            OkHttpClient client1 = new OkHttpClient();
            Request request1 = new Request.Builder()
                    .url("https://iot-api.heclouds.com/datapoint/history-datapoints?product_id=90VoX7q1Z9&device_name=test3&datastream_id=xinlv")
                    .addHeader("Authorization", finalAuthorization1)
                    .addHeader("Accept", Accept)
                    .build();
            try (Response response1 = client1.newCall(request1).execute()) {
                String responseData1 = response1.body().string();
                JsonRootBean app = new Gson().fromJson(responseData1, JsonRootBean.class);
                List<Datastreams> streams = app.getData().getDatastreams();
                List<Datapoints> points = streams.get(0).getDatapoints();
                String value1 = points.get(0).getValue();
                String time = points.get(0).getAt();
                resultMap.put("humidityValue", value1);
                resultMap.put("humidityTime", time);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    // 等待两个请求都完成
    CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futureTemp, futureHumidity);
    try {
        combinedFuture.get(); // 阻塞直到两个异步任务都完成
    } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
    }
    // 在UI线程更新UI
    new Handler(Looper.getMainLooper()).post(() -> {
        data.setText("101");
//        data4.setText(String.format("时间:%s", resultMap.get("tempTime").substring(10, 19)));
        data1.setText("5375");
    });

    return resultMap;
}

    private void parseJSONWithGSON(String jsonData) {
        JsonRootBean app = new Gson().fromJson(jsonData, JsonRootBean.class);
        List<Datastreams> streams = app.getData().getDatastreams();
        List<Datapoints> points = streams.get(0).getDatapoints();
        int count = app.getData().getCount();//获取数据的数量
        for (int i = 0; i < points.size(); i++) {
            String time = points.get(i).getAt();
            String value = points.get(i).getValue();
            Log.w("www", "time=" + time);
            Log.w("www", "value=" + value);
        }

    }

}
