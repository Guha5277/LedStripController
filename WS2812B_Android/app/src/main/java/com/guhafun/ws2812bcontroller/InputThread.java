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

        while(isNeedToListenData){
            try{
                if(inputStream.available() > 0 && inputStream.available() < 10){
                    try {
                        Thread.sleep(10);
                    }catch (InterruptedException ie){
                        Log.e(TAG, "ControlActivity: Ошибка приостановки потока!", ie);
                    }

                    count = inputStream.available();
                    data = new byte[count];
                    count = inputStream.read(data);
                    ControlActivity.data = data;

                    Log.d(TAG, "ControlActivity: Принято байт: " + count + ", Содердимое: " + Arrays.toString(data));
                }

                if(inputStream.available() == 54 ){
                    try {
                        Thread.sleep(10);
                    }catch (InterruptedException ie){
                        Log.e(TAG, "ControlActivity: Ошибка приостановки потока!", ie);
                    }

                    count = inputStream.available();
                    data = new byte[count];
                    count = inputStream.read(data);
                    ControlActivity.data = data;
                    ControlActivity.isInitialDataRecieved = true;

                    Log.d(TAG, "ControlActivity: Принято байт: " + count + ", Содердимое: " + Arrays.toString(data));
                }
            }catch (IOException ie){
                setEnabled(false);
                Log.e(TAG, "ControlActivity: Ошибка при получении данных!", ie);
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

    public byte[] getData(){
        if(data != null){
            Log.d(TAG, "InputThread данные переданы в главный поток");
            return data;
        }
        else {
            Log.d(TAG, "InputThread данные переданы в главный поток");
            return null;
        }
    }

    public void clearData(){
        Log.d(TAG, "Данные были очищены");
        data = null;
    }
}