package com.guhafun.ws2812bcontroller;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

// Класс для отпраки сообщений Микроконтроллеру
public class Commander {
    private OutputStream mOutputStream;

    private String TAG = "ConLog";

    private final Byte ON_OFF = 2;
    private final Byte PREV_MODE = 3;
    private final Byte NEXT_MODE = 4;
    private final Byte PAUSE_PLAY = 5;
    private final Byte FAV_MODE = 6;
    private final Byte ACT_DEACT_MODE = 7;
    private final Byte AUTO_MODE = 8;
    private final Byte SET_COLOR = 9;
    private final Byte SET_BRIGHT = 10;
    private final Byte SET_SPEED = 11;
    private final Byte SAVE_SETTINGS = 12;


    Commander(OutputStream outputStream){
        this.mOutputStream = outputStream;
    }

    private void sendMessage(byte ... data){
        try{
            Log.d(TAG, "Output: Попытка отправки данных: " + Arrays.toString(data) + " ...");
            mOutputStream.write(data);
        }catch (IOException ie){
            Log.e(TAG, "Output: Ошибка отправки данных!", ie);
        }
        Log.d(TAG, "Output: Данные отправлены");
    }

    public void onOff(){
        sendMessage(ON_OFF);
    }

    public void prevMode(){
        sendMessage(PREV_MODE);
    }

    public void nextMode(){
        sendMessage(NEXT_MODE);
    }

    public void pausePlay(){
        sendMessage(PAUSE_PLAY);
    }

    public void addToFav(byte modeIndex){
        sendMessage(FAV_MODE, modeIndex);
    }

    public void actDeactMode(byte mode, byte state){
        sendMessage(ACT_DEACT_MODE, mode, state);
    }

    public void setAutoMode(boolean state){
        byte onOffValue;

        onOffValue = (state) ? (byte) 1 : 0;

        sendMessage(AUTO_MODE, onOffValue);
    }

    public void setColor(byte r, byte g,  byte b){
        sendMessage(SET_COLOR, r, g, b);
    }

    public void setBright (byte bright) {
        sendMessage(SET_BRIGHT, bright);
    }

    public void setSpeed (byte speed) {
        sendMessage(SET_SPEED, speed);
    }

    public void saveSettings() {
        sendMessage(SAVE_SETTINGS);
    }


}
