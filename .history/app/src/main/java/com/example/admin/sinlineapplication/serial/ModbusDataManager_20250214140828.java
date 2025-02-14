package com.example.admin.sinlineapplication.serial;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ModbusDataManager {
    private static final String TAG = "ModbusDataManager";
    private SerialPortUtil serialPortUtil;
    private Handler handler;
    private static final int QUERY_INTERVAL = 1000; // 1秒查询间隔
    private static final String SERIAL_PORT = "/dev/ttyS3"; // 串口设备路径
    private static final int BAUDRATE = 9600; // 波特率

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
        // 打开串口
        boolean isOpen = serialPortUtil.openSerialPort(SERIAL_PORT, BAUDRATE);
        if (!isOpen) {
            Log.e(TAG, "串口打开失败");
            return;
        }
        Log.d(TAG, "串口打开成功");

        // 开始定时查询
        handler.post(queryRunnable);
    }

    // 停止查询
    public void stopQuery() {
        handler.removeCallbacks(queryRunnable);
        serialPortUtil.closeSerialPort();
        Log.d(TAG, "串口关闭");
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
                    Log.d(TAG, "数据更新成功");
                } else {
                    Log.e(TAG, "响应数据无效");
                }
            } catch (Exception e) {
                Log.e(TAG, "查询出错: " + e.getMessage());
            }

            // 延时1秒后再次查询
            handler.postDelayed(this, QUERY_INTERVAL);
        }
    };

    // 解析Modbus响应数据
    private void parseModbusResponse(byte[] response) {
        // 检查响应的基本格式
        if (response[0] != 0x01 || response[1] != 0x03) {
            Log.e(TAG, "响应格式错误");
            return;
        }

        int byteCount = response[2]; // 数据字节数
        if (byteCount != 10) { // 5个寄存器 * 2字节/寄存器
            Log.e(TAG, "数据长度错误");
            return;
        }

        // 解析数据
        for (int i = 0; i < 5 && i < modbusData.length; i++) {
            int highByte = response[3 + i * 2] & 0xFF;
            int lowByte = response[4 + i * 2] & 0xFF;
            int value = (highByte << 8) | lowByte;
            modbusData[i] = value;
            Log.d(TAG, "寄存器" + i + "值: " + value);
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