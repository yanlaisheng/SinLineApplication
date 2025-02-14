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

    // ����ģʽ
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

    // �򿪴���
    public boolean openSerialPort(String devicePath, int baudrate) {
        try {
            serialPort = new SerialPort(new File(devicePath), baudrate, 0);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            isStart = true;
            return true;
        } catch (IOException e) {
            Log.e(TAG, "�򿪴���ʧ��: " + e.getMessage());
            return false;
        }
    }

    // �رմ���
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
            Log.e(TAG, "�رմ���ʧ��: " + e.getMessage());
        }
    }

    // ����Modbus-RTU 03�������ѯ����
    public void sendModbus03Command(byte slaveId, int startAddress, int registerCount) {
        if (!isStart) {
            return;
        }

        // ��װModbus RTU����
        byte[] command = new byte[8];
        command[0] = slaveId; // �ӻ���ַ
        command[1] = 0x03; // ������
        command[2] = (byte) (startAddress >> 8); // ��ʼ��ַ���ֽ�
        command[3] = (byte) (startAddress & 0xFF); // ��ʼ��ַ���ֽ�
        command[4] = (byte) (registerCount >> 8); // �Ĵ����������ֽ�
        command[5] = (byte) (registerCount & 0xFF); // �Ĵ����������ֽ�

        // ����CRC
        int crc = calculateCRC(command, 6);
        command[6] = (byte) (crc & 0xFF); // CRC���ֽ�
        command[7] = (byte) (crc >> 8); // CRC���ֽ�

        try {
            outputStream.write(command);
        } catch (IOException e) {
            Log.e(TAG, "��������ʧ��: " + e.getMessage());
        }
    }

    // ��ȡ��Ӧ����
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
            Log.e(TAG, "��ȡ����ʧ��: " + e.getMessage());
        }
        return null;
    }

    // ����CRCУ��
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