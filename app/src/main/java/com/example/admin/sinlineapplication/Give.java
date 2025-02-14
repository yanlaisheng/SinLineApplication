package com.example.admin.sinlineapplication;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class Give extends  Activity {

    private TextView textView1;
    private TextView textView2;

    private final String[] numbers = {
            "8001.3", "8012.7", "8023.4", "8034.6", "8045.8", "8056.9", "8067.2", "8078.5", "8089.1", "8090.3",
            "8002.4", "8013.8", "8024.5", "8035.7", "8046.9", "8057.2", "8068.4", "8079.6", "8091.2", "8003.5",
            "8014.9", "8025.6", "8036.8", "8047.1", "8058.3", "8069.5", "8080.7", "8092.3", "8004.6", "8015.7"
    };

    private int index1 = 0;
    private int index2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        final Handler handler1 = new Handler();
        final Handler handler2 = new Handler();

        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                textView1.setText(numbers[index1]);
                index1++;
                if (index1 >= numbers.length) {
                    index1 = 0;
                }
                handler1.postDelayed(this, 3000); // 每 3 秒更新一次
            }
        }, 3000); // 初始延迟 3 秒

        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 在每个数字后面加上 1
                String numberWithOne = numbers[index2] + "1";
                textView2.setText(numberWithOne);
                index2++;
                if (index2 >= numbers.length) {
                    index2 = 0;
                }
                handler2.postDelayed(this, 3000); // 每 3 秒更新一次
            }
        }, 3000); // 初始延迟 3 秒
    }
}