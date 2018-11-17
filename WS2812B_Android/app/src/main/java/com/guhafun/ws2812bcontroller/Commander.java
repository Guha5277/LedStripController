package com.guhafun.ws2812bcontroller;

import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

// Класс для отпраки сообщений Микроконтроллеру
public class Commander {
    private OutputStream mOutputStream;

    private String TAG = "ConLog";

    private final byte GET_INITIAL_DATA = 1;
    private final byte ON_OFF = 2;
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
    private final byte SET_MODE_TO = 13;
    private final byte SET_AUTO_SAVE = 14;
    private final byte AUTO_SAVE_DURATION = 15;
    private final byte AUTO_MODE_DURATION = 16;
    private final byte SET_RANDOM = 17;

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

    void getInitialData(){
        sendMessage(GET_INITIAL_DATA);
    }

    //Включение/выключение ленты
    void onOff(){
        sendMessage(ON_OFF);
    }

    //Назад
    void prevMode(){
        sendMessage(PREV_MODE);
    }

    //Вперед
    void nextMode(){
        sendMessage(NEXT_MODE);
    }

    //Пауза/воспроизведение
    void pausePlay(){
        sendMessage(PAUSE_PLAY);
    }

    //Установить эффект в качестве стартового
    void addToFav(byte modeIndex){
        sendMessage(FAV_MODE, modeIndex);
    }

    //Добавить/исключить режим из плейлиста
    void actDeactMode(byte mode, boolean state){
        //Формируем сообщение для отпраки 1 - включить в список, 0 - исключить из списка, в зависимоти от полученного результата
        byte result = (state) ? (byte) 1 : 0;

        sendMessage(ACT_DEACT_MODE, mode, result);
    }

    //Включить/выключить авторежим
    void setAutoMode(boolean state){
        //byte onOffValue;

        byte onOffValue = (state) ? (byte) 1 : 0;

        sendMessage(AUTO_MODE, onOffValue);
    }

    //Установить произвольный цвет
    void setColor(byte r, byte g,  byte b){
        sendMessage(SET_COLOR, r, g, b);
    }

    //Задать яркость
    public void setBright (byte bright) {
        sendMessage(SET_BRIGHT, bright);
    }

    //Установить скорость эффектов
    public void setSpeed (byte speed) {
        sendMessage(SET_SPEED, speed);
    }

    //Сохранить настройки
    void saveSettings() {
        sendMessage(SAVE_SETTINGS);
    }

    //Включить выбранный из списка эффект
    void setModeTo(byte mode) {
        sendMessage(SET_MODE_TO, (byte) (mode + 1));
    }

    //Включить/отключить автоматическое сохранение изменений
    void setAutoSave(boolean state) {
        byte result = (state) ? (byte) 1 : 0;
        sendMessage(SET_AUTO_SAVE, result);
    }

    //Установить периодичность автосохранения
    void setAutoSaveDuration(byte duration) {
        sendMessage(AUTO_SAVE_DURATION, duration);
    }

    //Установить время воспроизведения одного эффекта (авторежим)
    void setAutoModeDuration(byte duration) {
        sendMessage(AUTO_MODE_DURATION, duration);
    }

    //Включить/выключить случайное переключение режимов
    void setRandom(boolean state) {
        byte result = (state) ? (byte) 1 : 0;
        sendMessage(SET_RANDOM, result);
    }
}
