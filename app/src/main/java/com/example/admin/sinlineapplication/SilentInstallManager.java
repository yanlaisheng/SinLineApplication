package com.example.admin.sinlineapplication;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SilentInstallManager {

    private static final String TAG = "SilentInstallManager";
    private static SilentInstallManager silentInstallManager;
    private static final String BROADCAST_ACTION = "com.android.packageinstaller.ACTION_INSTALL_COMMIT";
    private static final String INSTALL_CMD = "install";
    private Context mContext;


    private SilentInstallManager(Context mContext) {
        this.mContext = mContext;
    }

    public static SilentInstallManager getInstance(Context mContext) {
        if (null == silentInstallManager) {
            silentInstallManager = new SilentInstallManager(mContext);
        }
        return silentInstallManager;
    }

    public static boolean startInstall(String apkPath) {
        String[] args = {"pm", INSTALL_CMD, "-r", apkPath};
        String result = apkProcess(args);
        Log.e(TAG, "install log:" + result);
        if (result != null
                && (result.endsWith("Success") || result.endsWith("Success\n"))) {
            return true;
        }
        return false;
    }

    public static String apkProcess(String[] args) {
        String result = null;
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;
            process = processBuilder.start();
            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }
            baos.write('\n');
            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }
            byte[] data = baos.toByteArray();
            result = new String(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (errIs != null) {
                    errIs.close();
                }
                if (inIs != null ) {
                    inIs.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }



}
//    adb shell am broadcast -a com.zqc.package.silentinstaller --es file_path "/storage/sdcard0/DCIM/Video/Demo.apk"

