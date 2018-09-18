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

    //Эти переменные соответствуют переменным в коде Арудино - получается, что здесь их локальные копии
    public static int autoMode = 0;
    public static int maxBright = 0;
    public static int ledMode = 0;
    public static int[] ledModes = new int[49];
    public static int thisdelay = 0;


    public static void main(String[] args) {
      //  SerialPort serialPort = new SerialPort("arduinoPort"); //

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame();
            }
        });
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
            System.out.println(se);
        }

    }

    /***************Попытка подключения******************/
    public static void tryToConnect() {
        try {
            serialPort.writeByte((byte) 1);
        } catch (SerialPortException se) {
            System.out.println(se);
        }
    }
    /***************Отключение******************/
    public static void disconnect() {
        try {
            serialPort.closePort();
            isConnected = false;
        } catch (SerialPortException se) {
            System.out.println(se);
        }
    }

    /***************Отправка массива данных******************/
    public static void sendData(int...data) {
            try {
                serialPort.writeIntArray(data);
            } catch (SerialPortException se) {
                System.out.println(se);
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

                        int indx1 = 0;
                        ledMode = data[1];
                        maxBright = data[2];
                        autoMode = data[3];
                        thisdelay = data[4];
                        for (int indx2 = 5; indx2 < 54; indx2++) {
                            ledModes[indx1] = data[indx2];
                            indx1++;
                        }
                        isConnected = true;

                } catch (SerialPortException se) {
                    System.out.println(se);
                }
            }

            if(serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() >= 2 && serialPortEvent.getEventValue() <= 5){
                System.out.println("Responce has been comed!");
                try {
                    data = serialPort.readIntArray();
                   // ledMode = data[1];
                   // thisdelay = data[2];
                    isRecponceRecived = true;

                }
                catch (SerialPortException se){
                    System.out.println(se);
                }

            }


        }
    }
}