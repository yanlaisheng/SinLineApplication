package com.example.admin.sinlineapplication.serial;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ModbusDataManager {
    private static final String TAG = "ModbusDataManager";
    private SerialPortUtil serialPortUtil;
    private Handler handler;
    private static final int QUERY_INTERVAL = 1000; // 1���ѯ���

    // �洢��ѯ��������
    private float[] modbusData;
    private OnDataUpdateListener dataUpdateListener;

    public ModbusDataManager() {
        serialPortUtil = SerialPortUtil.getInstance();
        handler = new Handler(Looper.getMainLooper());
        modbusData = new float[10]; // ������Ҫ�洢10������
    }

    // ��ʼ��ʱ��ѯ
    public void startQuery() {
        // �򿪴���
        boolean isOpen = serialPortUtil.openSerialPort("/dev/ttyS3", 9600);
        if (!isOpen) {
            Log.e(TAG, "���ڴ�ʧ��");
            return;
        }

        // ��ʼ��ʱ��ѯ
        handler.post(queryRunnable);
    }

    // ֹͣ��ѯ
    public void stopQuery() {
        handler.removeCallbacks(queryRunnable);
        serialPortUtil.closeSerialPort();
    }

    // ��ʱ��ѯ����
    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            // ����Modbus��ѯ����
            serialPortUtil.sendModbus03Command((byte) 1, 0, 5); // �ӻ���ַ1����ʼ��ַ0����ȡ5���Ĵ���

            // ��ȡ��Ӧ
            byte[] response = serialPortUtil.readResponse();
            if (response != null && response.length >= 5) {
                // ������Ӧ����
                parseModbusResponse(response);

                // ֪ͨUI����
                if (dataUpdateListener != null) {
                    dataUpdateListener.onDataUpdate(modbusData);
                }
            }

            // ��ʱ1����ٴβ�ѯ
            handler.postDelayed(this, QUERY_INTERVAL);
        }
    };

    // ����Modbus��Ӧ����
    private void parseModbusResponse(byte[] response) {
        // �����Ӧ�Ļ�����ʽ
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

    // ���ݸ��¼����ӿ�
    public interface OnDataUpdateListener {
        void onDataUpdate(float[] data);
    }

    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }
}