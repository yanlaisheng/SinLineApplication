package com.example.admin.sinlineapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

//接收广播
public class SilentInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "SilentInstallReceiver";
    private Context mContext;
    private static final String ACTION_SILENCE_INSTALL = "com.zqc.package.silentinstaller";
    private static final String KEY_FILE_PATH = "file_path";
    private static final String KEY_PACKAGE_NAME = "package_name";
    private static final int MSG_SILENT_INSTALL = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_SILENT_INSTALL:
                    Intent intent = (Intent) msg.obj;
                    String filePath = intent.getStringExtra(KEY_FILE_PATH);
                    String packageName = intent.getStringExtra(KEY_PACKAGE_NAME);
                    Log.i(TAG, "---MSG_SILENT_INSTALL filePath=" + filePath + ",packageName=" + packageName);
                    SilentInstallManager.getInstance(mContext).startInstall(filePath);//安装应用
                    break;

                default:
                    break;
            }
        }

        ;
    };




    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "onReceive() action=" + action);
        if (ACTION_SILENCE_INSTALL.equals(action)) {
            mContext = context;
            Message msg = Message.obtain(mHandler);
            msg.what = MSG_SILENT_INSTALL;
            msg.obj = intent;
            mHandler.sendMessage(msg);
        }

    }
}