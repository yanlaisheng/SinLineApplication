package com.example.admin.sinlineapplication.serial;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ModbusDataManager {
    private static final String TAG = "ModbusDataManager";
    private SerialPortUtil serialPortUtil;
    private Handler handler;
    private static final int QUERY_INTERVAL = 1000; // 1秒查询间隔

    // 存储查询到的数据
    private float[] modbusData;
    private OnDataUpdateListener dataUpdateListener;

    public ModbusDataManager() {
        serialPortUtil = SerialPortUtil.getInstance();
        handler = new Handler(Looper.getMainLooper());
        modbusData = new float[10]; // 假设需要存储10个数据
    }

    // 开始定时查询
    public void startQuery() {
        // 打开串口
        boolean isOpen = serialPortUtil.openSerialPort("/dev/ttyS3", 9600);
        if (!isOpen) {
            Log.e(TAG, "串口打开失败");
            return;
        }

        // 开始定时查询
        handler.post(queryRunnable);
    }

    // 停止查询
    public void stopQuery() {
        handler.removeCallbacks(queryRunnable);
        serialPortUtil.closeSerialPort();
    }

    // 定时查询任务
    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            // 发送Modbus查询命令
            serialPortUtil.sendModbus03Command((byte) 1, 0, 5); // 从机地址1，起始地址0，读取5个寄存器

            // 读取响应
            byte[] response = serialPortUtil.readResponse();
            if (response != null && response.length >= 5) {
                // 解析响应数据
                parseModbusResponse(response);

                // 通知UI更新
                if (dataUpdateListener != null) {
                    dataUpdateListener.onDataUpdate(modbusData);
                }
            }

            // 延时1秒后再次查询
            handler.postDelayed(this, QUERY_INTERVAL);
        }
    };

    // 解析Modbus响应数据
    private void parseModbusResponse(byte[] response) {
        // 检查响应的基本格式
        if (response[0] != 1 || response[1] != 0x03) {
            return;
        }

        int byteCount = response[2];
        for (int i = 0; i < byteCount / 2 && i < modbusData.length; i++) {
            int highByte = response[3 + i * 2] & 0xFF;
            int lowByte = response[4 + i * 2] & 0xFF;
            int value = (highByte << 8) | lowByte;
            modbusData[i] = value;
        }
    }

    // 数据更新监听接口
    public interface OnDataUpdateListener {
        void onDataUpdate(float[] data);
    }

    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }
}