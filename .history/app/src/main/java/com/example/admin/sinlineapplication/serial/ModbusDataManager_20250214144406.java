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

    // Genymotion/VirtualBox �еĴ����豸·��
    private static final String[] SERIAL_PORTS = {
            "/dev/ttyS0", // ��׼����
            "/dev/ttyS1", // VirtualBox ӳ��Ĵ���ͨ���� ttyS1
            "/dev/ttyUSB0",
            "/dev/ttyACM0"
    };

    private static final int BAUDRATE = 9600;
    private float[] modbusData;
    private OnDataUpdateListener dataUpdateListener;

    public ModbusDataManager() {
        serialPortUtil = SerialPortUtil.getInstance();
        handler = new Handler(Looper.getMainLooper());
        modbusData = new float[10];
    }

    public void startQuery() {
        // ���Դ����п��ܵĴ���
        boolean portOpened = false;
        String openedPort = null;

        for (String port : SERIAL_PORTS) {
            File device = new File(port);
            if (device.exists()) {
                try {
                    // ��������Ȩ��
                    Process process = Runtime.getRuntime().exec("chmod 666 " + port);
                    process.waitFor();

                    boolean isOpen = serialPortUtil.openSerialPort(port, BAUDRATE);
                    if (isOpen) {
                        Log.d(TAG, "Successfully opened serial port: " + port);
                        portOpened = true;
                        openedPort = port;
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open port " + port + ": " + e.getMessage());
                }
            } else {
                Log.d(TAG, "Port does not exist: " + port);
            }
        }

        if (!portOpened) {
            Log.e(TAG, "No available serial ports found");
            return;
        }

        // ��ʼ��ѯ����
        handler.post(queryRunnable);
    }

    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // ���� Modbus ��ѯ����
                serialPortUtil.sendModbus03Command((byte) 1, 0, 5);

                // ��ȡ��Ӧ
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

    public void stopQuery() {
        handler.removeCallbacks(queryRunnable);
        serialPortUtil.closeSerialPort();
        Log.d(TAG, "Query stopped");
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