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
            "/dev/ttyS1",
            "/dev/ttyUSB0",
            "/dev/ttyACM0"
    };
    private static final int BAUDRATE = 9600;
    private float[] modbusData;
    private OnDataUpdateListener dataUpdateListener;
    private boolean isEmulator;

    public ModbusDataManager() {
        serialPortUtil = SerialPortUtil.getInstance();
        handler = new Handler(Looper.getMainLooper());
        modbusData = new float[10];
        isEmulator = checkIsEmulator();
    }

    private boolean checkIsEmulator() {
        return android.os.Build.PRODUCT.contains("sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK");
    }

    public void startQuery() {
        if (isEmulator) {
            Log.d(TAG, "Running in emulator, using simulated data");
            startSimulatedQuery();
            return;
        }

        boolean portOpened = false;
        for (String port : SERIAL_PORTS) {
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

        if (!portOpened) {
            Log.d(TAG, "No physical serial port available, using simulated data");
            startSimulatedQuery();
            return;
        }

        handler.post(queryRunnable);
    }

    private void startSimulatedQuery() {
        handler.post(simulatedRunnable);
    }

    private Runnable simulatedRunnable = new Runnable() {
        private double angle = 0;

        @Override
        public void run() {
            // 生成模拟的正弦波数据
            for (int i = 0; i < modbusData.length; i++) {
                modbusData[i] = (float) (100 + 50 * Math.sin(angle + i * Math.PI / 5));
            }
            angle += 0.1;

            if (dataUpdateListener != null) {
                dataUpdateListener.onDataUpdate(modbusData);
            }
            Log.d(TAG, "Simulated data updated");

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
                    Log.d(TAG, "Real data updated successfully");
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
        handler.removeCallbacks(simulatedRunnable);
        if (!isEmulator) {
            serialPortUtil.closeSerialPort();
        }
        Log.d(TAG, "Query stopped");
    }

    private void parseModbusResponse(byte[] response) {
        if (response[0] != 0x01 || response[1] != 0x03) {
            Log.e(TAG, "Invalid response format");
            return;
        }

        int byteCount = response[2];
        if (byteCount != 10) {
            Log.e(TAG, "Invalid data length");
            return;
        }

        for (int i = 0; i < 5 && i < modbusData.length; i++) {
            int highByte = response[3 + i * 2] & 0xFF;
            int lowByte = response[4 + i * 2] & 0xFF;
            int value = (highByte << 8) | lowByte;
            modbusData[i] = value;
            Log.d(TAG, "Register " + i + " value: " + value);
        }
    }

    public interface OnDataUpdateListener {
        void onDataUpdate(float[] data);
    }

    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

    public float[] getModbusData() {
        return modbusData;
    }
}