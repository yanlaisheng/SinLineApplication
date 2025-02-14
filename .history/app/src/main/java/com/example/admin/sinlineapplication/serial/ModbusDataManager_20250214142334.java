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

    // 存储查询到的数据
    private float[] modbusData;
    private OnDataUpdateListener dataUpdateListener;

    public ModbusDataManager() {
        serialPortUtil = SerialPortUtil.getInstance();
        handler = new Handler(Looper.getMainLooper());
        modbusData = new float[10]; // 存储10个数据点
    }

    // 开始定时查询
    public void startQuery() {
        // 检查串口文件是否存在
        File device = new File(SERIAL_PORT);
        if (!device.exists()) {
            Log.e(TAG, "Serial port does not exist: " + SERIAL_PORT);
            return;
        }

        // 检查串口权限
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

        // 打开串口
        boolean isOpen = serialPortUtil.openSerialPort(SERIAL_PORT, BAUDRATE);
        if (!isOpen) {
            Log.e(TAG, "Failed to open serial port");
            return;
        }
        Log.d(TAG, "Serial port opened successfully");

        // 开始定时查询
        handler.post(queryRunnable);
    }

    // 停止查询
    public void stopQuery() {
        handler.removeCallbacks(queryRunnable);
        serialPortUtil.closeSerialPort();
        Log.d(TAG, "Serial port closed");
    }

    // 定时查询任务
    private Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // Modbus RTU 查询命令：从机地址01，功能码03，起始地址0000，寄存器数量0005
                byte[] command = new byte[] {
                        0x01, // 从机地址
                        0x03, // 功能码
                        0x00, // 起始地址高字节
                        0x00, // 起始地址低字节
                        0x00, // 寄存器数量高字节
                        0x05, // 寄存器数量低字节
                        (byte) 0x85, // CRC低字节
                        (byte) 0xC9 // CRC高字节
                };

                // 发送查询命令
                serialPortUtil.sendModbus03Command((byte) 1, 0, 5);

                // 读取响应
                byte[] response = serialPortUtil.readResponse();
                if (response != null && response.length >= 13) { // Modbus RTU响应长度：1+1+1+10=13字节
                    // 解析响应数据
                    parseModbusResponse(response);

                    // 通知UI更新
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

            // 延时1秒后再次查询
            handler.postDelayed(this, QUERY_INTERVAL);
        }
    };

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