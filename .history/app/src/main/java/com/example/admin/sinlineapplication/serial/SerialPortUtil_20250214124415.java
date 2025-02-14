package com.example.admin.sinlineapplication.serial;

import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPortUtil {
    private static final String TAG = "SerialPortUtil";
    private SerialPort serialPort = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private boolean isStart = false;

    // 单例模式
    private static SerialPortUtil instance;

    static {
        System.loadLibrary("serial_port");
    }

    public static SerialPortUtil getInstance() {
        if (instance == null) {
            instance = new SerialPortUtil();
        }
        return instance;
    }

    // 打开串口
    public boolean openSerialPort(String devicePath, int baudrate) {
        try {
            serialPort = new SerialPort(new File(devicePath), baudrate, 0);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            isStart = true;
            return true;
        } catch (IOException e) {
            Log.e(TAG, "打开串口失败: " + e.getMessage());
            return false;
        }
    }

    // 关闭串口
    public void closeSerialPort() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (serialPort != null) {
                serialPort.close();
            }
            isStart = false;
        } catch (IOException e) {
            Log.e(TAG, "关闭串口失败: " + e.getMessage());
        }
    }

    // 发送Modbus-RTU 03功能码查询命令
    public void sendModbus03Command(byte slaveId, int startAddress, int registerCount) {
        if (!isStart) {
            return;
        }

        // 组装Modbus RTU命令
        byte[] command = new byte[8];
        command[0] = slaveId; // 从机地址
        command[1] = 0x03; // 功能码
        command[2] = (byte) (startAddress >> 8); // 起始地址高字节
        command[3] = (byte) (startAddress & 0xFF); // 起始地址低字节
        command[4] = (byte) (registerCount >> 8); // 寄存器数量高字节
        command[5] = (byte) (registerCount & 0xFF); // 寄存器数量低字节

        // 计算CRC
        int crc = calculateCRC(command, 6);
        command[6] = (byte) (crc & 0xFF); // CRC低字节
        command[7] = (byte) (crc >> 8); // CRC高字节

        try {
            outputStream.write(command);
        } catch (IOException e) {
            Log.e(TAG, "发送数据失败: " + e.getMessage());
        }
    }

    // 读取响应数据
    public byte[] readResponse() {
        if (!isStart) {
            return null;
        }

        try {
            byte[] buffer = new byte[1024];
            int size = inputStream.read(buffer);
            if (size > 0) {
                byte[] data = new byte[size];
                System.arraycopy(buffer, 0, data, 0, size);
                return data;
            }
        } catch (IOException e) {
            Log.e(TAG, "读取数据失败: " + e.getMessage());
        }
        return null;
    }

    // 计算CRC校验
    private int calculateCRC(byte[] data, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc = crc >> 1;
                }
            }
        }
        return crc;
    }
}

class SerialPort {
    private static final String TAG = "SerialPort";
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate, int flags) throws IOException {
        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    private native static FileDescriptor open(String path, int baudrate, int flags);

    public native void close();
}