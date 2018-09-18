package com.guhafun;

import javax.swing.*;

import jssc.*;

public class Main {

    private static SerialPort serialPort;       //Объект через который будут происходить все манипуляции с COM-портом
    public static boolean isConnected = false;  //Флаг подключения
    public static boolean isRecponceRecived = false; //Флаг получения ответа

    public static String com = "";              //Имя нашего ком-порта
    public static int baudRate = 0;             //Скорость подключения
    private static int data[] = new int[54];    //Массив получаемых и отправляемых данных


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame());
    }
/***************Открытие порта ******************/
    public static void openPort(String comPort, int bRate) {
        com = comPort;
        baudRate = bRate;
        serialPort = new SerialPort(com);  //Инициализируем COM-порт

        try {
            //Открываем порт с заданными параметрами, устанавливаем слушателя
            serialPort.openPort();
            serialPort.setParams(baudRate, 8, 1, 0);
            serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
           // serialPort.writeByte((byte)1);

        } catch (SerialPortException se) {
            se.printStackTrace();
        }

    }

    /***************Попытка подключения******************/
    public static void tryToConnect() {
        try {
            serialPort.writeByte((byte) 1);
        } catch (SerialPortException se) {
            se.printStackTrace();
        }
    }
    /***************Отключение******************/
    public static void disconnect() {
        try {
            serialPort.closePort();
            isConnected = false;
        } catch (SerialPortException se) {
            se.printStackTrace();
        }
    }

    /***************Отправка массива данных******************/
    public static void sendData(int...data) {
            try {
                serialPort.writeIntArray(data);
            } catch (SerialPortException se) {
                se.printStackTrace();
            }
        }

    public static int[] returnRecivedData(){
        return data;
    }
    /***************Очистка массива данных******************/
    public static void clearData(){
        for(int i = 0; i < data.length; i++){
            data[i] = 0;
        }
    }


    /***************Слушатель для события принятия данных ******************/
    private static class PortReader implements SerialPortEventListener {
        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() == 54) {           //Если пришли настройки (54 байт в буфере)
                System.out.println("Setting has come!");

                try {
                    data = serialPort.readIntArray();
                        isConnected = true;

                } catch (SerialPortException se) {
                    se.printStackTrace();
                }
            }

            if(serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() >= 2 && serialPortEvent.getEventValue() <= 5){
                System.out.println("Responce has been comed!");
                try {
                    data = serialPort.readIntArray();
                    isRecponceRecived = true;

                }
                catch (SerialPortException se){
                    se.printStackTrace();
                }

            }


        }
    }
}