package com.guhafun.ws2812bcontroller;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

//Класс реализующий "просллушку" входящего потока на наличие сообщений и их считывание
public class InputThread extends Thread{
    private String TAG = "ConLog";
    private InputStream inputStream;
    private Context mContext;

    private final byte INITIAL_DATA_LENGHT = 58;

    InputThread(InputStream inStream, Context context){
        Log.d(TAG, "InputThread инициализирован");
        inputStream = inStream;
        mContext = context;
    }

    @Override
    public void run(){
        Log.d(TAG, "InputThread поток запущен");

        int count;
        int readCount;
        byte[] data;

        while(true) {
            try {
                //Если в буффере есть доступные данные для считывания
                if (inputStream.available() > 0) {
                    //Сохраняем количество байт для считывания в счетчик
                    count = inputStream.available();

                    Log.d(TAG, "InputThread: count: " + count);

                    //Проверяем их количество
                    if (count > 0 && count < 54) {

                        Log.d(TAG, "InputThread: Доступно байт: " + count);
                        //Присваиваем счетчику количество доступных байт, создаем эквивалентный счетчику массив, считываем данные и отправляем изменения в главный поток
                        count = inputStream.available();

                        //Считываем и отправляем полученные данные
                        data = new byte[count];
                        readCount = inputStream.read(data);
                        messageProcessing(data);

                        Log.d(TAG, "InputThread: Принято байт (<54): " + readCount + ", Содердимое: " + Arrays.toString(data));
                    }

                    //Если пришло 54 байта - это данные инициализации, которые приходят при подключении
                    if (count == INITIAL_DATA_LENGHT) {
                        Log.d(TAG, "InputThread: Доступно байт: " + count);

                        //Считываем и отправляем полученные данные
                        data = new byte[count];
                        readCount = inputStream.read(data);
                        messageProcessing(data);

                        Log.d(TAG, "InputThread: Принято байт (=54): " + readCount + ", Содердимое: " + Arrays.toString(data));
                    }

                    //Если пришла какая-то неведомая фигня, то просто очищаем буфер
                    if (count > INITIAL_DATA_LENGHT){

                        byte[] temp;
                        temp = new byte[count];
                        readCount = inputStream.read(temp);
                        Log.d(TAG, "InputThread: Уничтожено байт: " + readCount + ", Содердимое: " + Arrays.toString(temp));
                    }
                }

                }catch(IOException ie){
                Log.e(TAG, "InputThread: Ошибка при получении данных!", ie);
            }
        }
    }

    //Метод для отпраки данных в главный поток
    private void messageProcessing(byte [] inputData){
        final int INITIALIZE = 1;
        final int ON_OFF = 2;
        final byte PREV_MODE = 3;
        final byte NEXT_MODE = 4;
        final byte PAUSE_PLAY = 5;
        final byte FAV_MODE = 6;
        final byte ACT_DEACT_MODE = 7;
        final byte AUTO_MODE = 8;
        final byte SET_COLOR = 9;
       // final byte SET_BRIGHT = 10;
        final byte SET_SPEED = 11;
        final byte SAVE_SETTINGS = 12;
        final byte SET_MODE_TO = 13;

        //Здесь проверяется соответствие длины принятого массива(кол-ва байт) которые может отправить МК при разных коммандах, в случае расхождений метод прерывается...
        switch(inputData[0]){
            case INITIALIZE:
                if (inputData.length != INITIAL_DATA_LENGHT) {
                    return;
                }
                break;

            case ON_OFF:
            case PAUSE_PLAY:
            case FAV_MODE:
            case AUTO_MODE:
            //case SET_BRIGHT:
            case SET_SPEED:
            case SAVE_SETTINGS:
                if (inputData.length != 2) {
                    return;
                }
                break;

            case PREV_MODE:
            case NEXT_MODE:
            case ACT_DEACT_MODE:
            case SET_MODE_TO:
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

        Log.d(TAG, "InputThread отправлено в главный поток: " + inputData[0] + ", с содержимым: " + Arrays.toString(inputData));

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}