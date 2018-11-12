package com.guhafun.ws2812bcontroller;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;

//Класс реализующий "просллушку" входящего потока на наличие сообщений и их считывание
public class InputThread extends Thread{
    private String TAG = "ConLog";
    private InputStream inputStream;
    private byte[] data;
    private byte[] initializeData = new byte[54];

    private final int INITIALIZE = 1;
    private final int ON_OFF = 2;
    private final byte PREV_MODE = 3;
    private final byte NEXT_MODE = 4;
    private final byte PAUSE_PLAY = 5;
    private final byte FAV_MODE = 6;
    private final byte ACT_DEACT_MODE = 7;
    private final byte AUTO_MODE = 8;
    private final byte SET_COLOR = 9;
    private final byte SET_BRIGHT = 10;
    private final byte SET_SPEED = 11;
    private final byte SAVE_SETTINGS = 12;



    //Флаг с актуальным состоянием потока
    private boolean isNeedToListenData = false;
    private Context mContext = null;

    protected InputThread(InputStream inStream, Context context){
        Log.d(TAG, "InputThread инициализирован");
        inputStream = inStream;
        mContext = context;
    }

    @Override
    public void run(){
        Log.d(TAG, "InputThread поток запущен");
        int count;

        byte[] initializeData;
       // byte[] procedureData;

        while(isNeedToListenData) {
            try {
                if (inputStream.available() > 0) {
//                    try {
//                        Thread.sleep(10);
//                    } catch (InterruptedException ie) {
//                        Log.e(TAG, "InputThread: Ошибка приостановки потока!", ie);
//                    }

                    count = inputStream.available();

                    Log.d(TAG, "InputThread: count: " + count);

                    if (count > 0 && count < 54) {

                        Log.d(TAG, "InputThread: Доступно байт: " + count);
                        //Присваиваем счетчику количество доступных байт, создаем эквивалентный счетчику массив, считываем данные и отправляем изменения в главный поток
                        count = inputStream.available();
                        data = new byte[count];
                        count = inputStream.read(data);
                        messageProcessing(data);

                        Log.d(TAG, "InputThread: Принято байт (<54): " + count + ", Содердимое: " + Arrays.toString(data));


                    }

                    if (count == 54) {
                        Log.d(TAG, "InputThread: Доступно байт: " + count);
                        //
                        data = new byte[count];
                        count = inputStream.read(data);
                        updateInitData(data);
                        messageProcessing(data);

                        //ControlActivity.isInitialDataRecieved = true;

                        Log.d(TAG, "InputThread: Принято байт (=54): " + count + ", Содердимое: " + Arrays.toString(data));
                    }

                    if (count > 54){

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

//    public boolean getEnabled(){
//        return isNeedToListenData;
//    }

    //Метод для отпраки данных в главный поток
    private void messageProcessing(byte [] inputData){
        //Здесь проверяется соответствие длины массива(кол-ва байт) которые может отправить МК при разных коммандах , в случае расхождений метод прерывается
        switch(inputData[0]){
            case INITIALIZE:
                if (inputData.length != 54) {
                    return;
                }
                break;

            case ON_OFF:
            case PREV_MODE:
            case NEXT_MODE:
            case PAUSE_PLAY:
            case FAV_MODE:
            case AUTO_MODE:
            case SET_BRIGHT:
            case SET_SPEED:
            case SAVE_SETTINGS:
                if (inputData.length != 2) {
                    return;
                }
                break;

            case ACT_DEACT_MODE:
                if (inputData.length != 3) {
                    return;
                }
                break;

            case SET_COLOR:
                if (inputData.length != 4) {
                    return;
                }
                break;


                //Если пришедшая команда неивестна(пришёл хлам) - прерываем метод
            default:
                return;
        }

        //Когда все проверки были пройдены, отправляем данные в ControlActivity
        Intent intent = new Intent(ControlActivity.DATA_MESSAGE);
        intent.putExtra("msg", inputData[0]);
        intent.putExtra("data", inputData);

        Log.d(TAG, "InputThread отправлена команда: " + inputData[0] + ", с содержимым: " + Arrays.toString(inputData));

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

    }

    private void updateInitData(byte[] data){
        initializeData = Arrays.copyOf(data, 54);
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