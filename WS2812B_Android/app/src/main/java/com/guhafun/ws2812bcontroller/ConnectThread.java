package com.example.ws2812bcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final String TAG = "ConLog";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Handler mHandler;
    //Context mContext;

    public ConnectThread(BluetoothDevice device, Handler handler) {

        mHandler = handler;
        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "ConnectThread: Ошибка получения Socket'a", e);
        }
        mmSocket = tmp;
        Log.d(TAG, "ConnectThread: Socket получен " + mmSocket.toString());
    }

    @Override
    public void run() {
        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            mHandler.sendEmptyMessage(0);
            Log.d(TAG, "ConnectThread: Не удалось установить соединение (what 0)");

            try {
                //В случае ошибки отправляем сообщение в основной поток Handler'y
                mmSocket.close();
            } catch (IOException closeException) {

                Log.e(TAG, "ConnectThread: Ошибка закрытия Socket'a (what 0)", closeException);
            }
        }

        if(mmSocket.isConnected()) {
            //В случае успешного соединения,  задаем сокет для ControlActivity и отправляем соответствующее сообщение Handler'y
            ControlActivity.setMmSocket(mmSocket);
            mHandler.sendEmptyMessage(1);
            Log.d(TAG, "ConnectThread: Соедниение установлено! (what 1)");
        }
    }
}

