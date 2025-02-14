package com.example.admin.sinlineapplication.serial;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;

public class ModbusDataManager {
    private static final String TAG = "ModbusDataManager";
    private SerialPortUtil serialPortUtil;
    private Handler handler;
    private static final int QUERY_INTERVAL = 1000;
    private static final String[] SERIAL_PORTS = {
            "/dev/ttyS0",
            "/dev/ttyUSB0",
            "/dev/ttyACM0",
            "/dev/ttyS1",
            "/dev/ttyS2",
            "/dev/ttyS3"
    };
    private static final int BAUDRATE = 9600;

    private float[] modbusData;
    private OnDataUpdateListener dataUpdateListener;
    private boolean isSimulator = false;

    public ModbusDataManager() {
        serialPortUtil = SerialPortUtil.getInstance();
        handler = new Handler(Looper.getMainLooper());
        modbusData = new float[10];

        // 检查是否在模拟器中运行
        isSimulator = isEmulator();
    }

    private boolean isEmulator() {
        return android.os.Build.PRODUCT.contains("sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK");
    }

    public void startQuery() {
        if (isSimulator) {
            // 在模拟器中，使用模拟数据
            Log.d(TAG, "Running in simulator, using mock data");
            startMockDataQuery();
            return;
        }

        // 尝试打开可用的串口
        boolean portOpened = false;
        for (String port : SERIAL_PORTS) {
            File device = new File(port);
            if (device.exists()) {
                try {
                    boolean isOpen = serialPortUtil.openSerialPort(port, BAUDRATE);
                    if (isOpen) {
                        Log.d(TAG, "Successfully opened serial port: " + port);
                        portOpened = true;
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open port " + port + ": " + e.getMessage());
                }
            }
        }

        if (!portOpened) {
            Log.e(TAG, "No available serial ports found");
            startMockDataQuery(); // 如果没有可用串口，使用模拟数据
            return;
        }

        handler.post(queryRunnable);
    }

    private void startMockDataQuery() {
        handler.post(mockDataRunnable);
    }

    private Runnable mockDataRunnable = new Runnable() {
        @Override
        public void run() {
            // 生成模拟数据
            for (int i = 0; i < modbusData.length; i++) {
                // 生成一些随机波动的数据
                modbusData[i] = (float) (100 + Math.random() * 20);
            }

            if (dataUpdateListener != null) {
                dataUpdateListener.onDataUpdate(modbusData);
            }
            Log.d(TAG, "Mock data updated");

            handler.postDelayed(this, QUERY_INTERVAL);
        }
    };

    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                serialPortUtil.sendModbus03Command((byte) 1, 0, 5);

                byte[] response = serialPortUtil.readResponse();
                if (response != null && response.length >= 13) {
                    parseModbusResponse(response);

                    if (dataUpdateListener != null) {
                        dataUpdateListener.onDataUpdate(modbusData);
                    }
                    Log.d(TAG, "Data updated successfully");
                } else {
                    Log.e(TAG, "Invalid response data");
                }
            } catch (Exception e) {
                Log.e(TAG, "Query error: " + e.getMessage());
            }

            handler.postDelayed(this, QUERY_INTERVAL);
        }
    };

    public void stopQuery() {
        handler.removeCallbacks(queryRunnable);
        handler.removeCallbacks(mockDataRunnable);
        if (!isSimulator) {
            serialPortUtil.closeSerialPort();
        }
        Log.d(TAG, "Query stopped");
    }

    // 解析Modbus响应数据
    private void parseModbusResponse(byte[] response) {
        // 检查响应的基本格式
        if (response[0] != 0x01 || response[1] != 0x03) {
            Log.e(TAG, "Invalid response format");
            return;
        }

        int byteCount = response[2]; // 数据字节数
        if (byteCount != 10) { // 5个寄存器 * 2字节/寄存器
            Log.e(TAG, "Invalid data length");
            return;
        }

        // 解析数据
        for (int i = 0; i < 5 && i < modbusData.length; i++) {
            int highByte = response[3 + i * 2] & 0xFF;
            int lowByte = response[4 + i * 2] & 0xFF;
            int value = (highByte << 8) | lowByte;
            modbusData[i] = value;
            Log.d(TAG, "Register " + i + " value: " + value);
        }
    }

    // 数据更新监听接口
    public interface OnDataUpdateListener {
        void onDataUpdate(float[] data);
    }

    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

    // 获取最新的数据
    public float[] getModbusData() {
        return modbusData;
    }
}