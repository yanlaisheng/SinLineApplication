package com.example.admin.sinlineapplication.serial;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;

public class ModbusDataManager {
    private static final String TAG = "ModbusDataManager";
    private SerialPortUtil serialPortUtil;
    private Handler handler;
    private static final int QUERY_INTERVAL = 1000; // 1s interval
    private static final String SERIAL_PORT = "/dev/ttyS0"; // serial port path
    private static final int BAUDRATE = 9600; // baudrate

    // �洢��ѯ��������
    private float[] modbusData;
    private OnDataUpdateListener dataUpdateListener;

    public ModbusDataManager() {
        serialPortUtil = SerialPortUtil.getInstance();
        handler = new Handler(Looper.getMainLooper());
        modbusData = new float[10]; // �洢10�����ݵ�
    }

    // ��ʼ��ʱ��ѯ
    public void startQuery() {
        // ��鴮���ļ��Ƿ����
        File device = new File(SERIAL_PORT);
        if (!device.exists()) {
            Log.e(TAG, "Serial port does not exist: " + SERIAL_PORT);
            return;
        }

        // ��鴮��Ȩ��
        if (!device.canRead() || !device.canWrite()) {
            try {
                Process su = Runtime.getRuntime().exec("su");
                String cmd = "chmod 666 " + SERIAL_PORT + "\nexit\n";
                su.getOutputStream().write(cmd.getBytes());
                su.getOutputStream().flush();
                su.waitFor();
            } catch (Exception e) {
                Log.e(TAG, "Failed to set serial port permission: " + e.getMessage());
                return;
            }
        }

        // �򿪴���
        boolean isOpen = serialPortUtil.openSerialPort(SERIAL_PORT, BAUDRATE);
        if (!isOpen) {
            Log.e(TAG, "Failed to open serial port");
            return;
        }
        Log.d(TAG, "Serial port opened successfully");

        // ��ʼ��ʱ��ѯ
        handler.post(queryRunnable);
    }

    // ֹͣ��ѯ
    public void stopQuery() {
        handler.removeCallbacks(queryRunnable);
        serialPortUtil.closeSerialPort();
        Log.d(TAG, "Serial port closed");
    }

    // ��ʱ��ѯ����
    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // Modbus RTU ��ѯ����ӻ���ַ01��������03����ʼ��ַ0000���Ĵ�������0005
                byte[] command = new byte[] {
                        0x01, // �ӻ���ַ
                        0x03, // ������
                        0x00, // ��ʼ��ַ���ֽ�
                        0x00, // ��ʼ��ַ���ֽ�
                        0x00, // �Ĵ����������ֽ�
                        0x05, // �Ĵ����������ֽ�
                        (byte) 0x85, // CRC���ֽ�
                        (byte) 0xC9 // CRC���ֽ�
                };

                // ���Ͳ�ѯ����
                serialPortUtil.sendModbus03Command((byte) 1, 0, 5);

                // ��ȡ��Ӧ
                byte[] response = serialPortUtil.readResponse();
                if (response != null && response.length >= 13) { // Modbus RTU��Ӧ���ȣ�1+1+1+10=13�ֽ�
                    // ������Ӧ����
                    parseModbusResponse(response);

                    // ֪ͨUI����
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

            // ��ʱ1����ٴβ�ѯ
            handler.postDelayed(this, QUERY_INTERVAL);
        }
    };

    // ����Modbus��Ӧ����
    private void parseModbusResponse(byte[] response) {
        // �����Ӧ�Ļ�����ʽ
        if (response[0] != 0x01 || response[1] != 0x03) {
            Log.e(TAG, "Invalid response format");
            return;
        }

        int byteCount = response[2]; // �����ֽ���
        if (byteCount != 10) { // 5���Ĵ��� * 2�ֽ�/�Ĵ���
            Log.e(TAG, "Invalid data length");
            return;
        }

        // ��������
        for (int i = 0; i < 5 && i < modbusData.length; i++) {
            int highByte = response[3 + i * 2] & 0xFF;
            int lowByte = response[4 + i * 2] & 0xFF;
            int value = (highByte << 8) | lowByte;
            modbusData[i] = value;
            Log.d(TAG, "Register " + i + " value: " + value);
        }
    }

    // ���ݸ��¼����ӿ�
    public interface OnDataUpdateListener {
        void onDataUpdate(float[] data);
    }

    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

    // ��ȡ���µ�����
    public float[] getModbusData() {
        return modbusData;
    }
}