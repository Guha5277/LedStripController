package com.guhafun.ws2812bcontroller;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

//Класс реализующий "просллушку" входящего потока на наличие сообщений и их считывание
public class InputThread extends Thread{
    private String TAG = "ConLog";
    private InputStream inputStream;
    private byte[] data;
    private byte[] initializeData;

    //Флаг с актуальным состоянием потока
    private boolean isNeedToListenData = false;

    protected InputThread(InputStream inStream){
        Log.d(TAG, "InputThread инициализирован");
        inputStream = inStream;
    }

    @Override
    public void run(){
        Log.d(TAG, "InputThread поток запущен");
        int count;

        while(isNeedToListenData) {
            try {
                if (inputStream.available() > 0) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "InputThread: Ошибка приостановки потока!", ie);
                    }

                    count = inputStream.available();

                    Log.d(TAG, "InputThread: count: " + count);

                    if (count > 0 && count <= 4) {

                        Log.d(TAG, "InputThread: Доступно байт: " + count);
                        count = inputStream.available();
                        data = new byte[count];
                        count = inputStream.read(data);

                        Log.d(TAG, "InputThread: Принято байт: " + count + ", Содердимое: " + Arrays.toString(data));

                    }

                    if (count == 54) {
                        Log.d(TAG, "InputThread: Доступно байт: " + count);
                        data = new byte[count];
                        count = inputStream.read(data);
                        initializeData = data;
                        ControlActivity.isInitialDataRecieved = true;

                        Log.d(TAG, "InputThread: Принято байт: " + count + ", Содердимое: " + Arrays.toString(data));
                    }

                    else {
                        byte[] temp;
                        temp = new byte[count];
                        inputStream.read(temp);
                    }
                }

                }catch(IOException ie){
                setEnabled(false);
                Log.e(TAG, "InputThread: Ошибка при получении данных!", ie);
            }
        }
        Log.d(TAG, "InputThread поток завершен");
    }

    public void setEnabled(boolean status){
        isNeedToListenData = status;
    }

    public boolean getEnabled(){
        return isNeedToListenData;
    }

    public byte[] getInitializeData(){
        if(initializeData != null){
            Log.d(TAG, "InputThread данные переданы в главный поток");
            return initializeData;
        }
        else {
            Log.d(TAG, "InputThread ошибка передачи данных другому потоку!");
            return null;
        }
    }

    public void clearData(){
        Log.d(TAG, "InputThread Данные были очищены");
        data = null;
    }
}