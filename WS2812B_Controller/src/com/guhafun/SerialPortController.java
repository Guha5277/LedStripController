package com.guhafun;

import jssc.*;

import java.util.ArrayList;

public class SerialPortController implements SerialPortEventListener{
    private SerialPort serialPort;
    private final Integer[] baudRate = {600, 1200, 2400, 4800, 9600, 19200, 28800, 38400, 57600, 115200};
    private final int dataBits = 8;
    private final int stopBits = 1;
    private final int parity = 0;
    private final DataKeeper dataKeeper;

    SerialPortListener listener;

    SerialPortController(SerialPortListener listener){
        this.listener = listener;
        dataKeeper = new DataKeeper();
    }

    public String[] getSerialPortList(){
        return SerialPortList.getPortNames();
    }

    public void connect(String port, int baudRate){
        serialPort = new SerialPort(port);
        try {
            serialPort.openPort();
            serialPort.setParams(baudRate, dataBits, stopBits, parity);
            serialPort.addEventListener(this);
            listener.onSerialPortConnected(port, baudRate);
        } catch (SerialPortException e) {
            listener.onSerialPortFailedToConnect(port, baudRate, e);
        }
    }

    public synchronized void sendMessage(int...message){
        try {
            serialPort.writeIntArray(message);
            listener.onSerialPortDataSent(message);
        } catch (SerialPortException e) {
            listener.onException(e);
        }
    }

    public boolean isConnected(){
        if (serialPort == null) return false;
        return serialPort.isOpened();
    }

    public void disconnect(){
        String port = serialPort.getPortName();
        try {
            serialPort.closePort();
            listener.onSerialPortClosed(port);
        } catch (SerialPortException e) {
            listener.onSerialPortFailedToClose(port, e);
            listener.onSerialPortClosed(port);
        }
    }

    public Integer[] getBaudRateList(){
        return baudRate;
    }

    //Serial port events
    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        if(serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0){
            try {
                dataKeeper.addData(serialPort.readIntArray());
            } catch (SerialPortException e) {
                listener.onException(e);
            }
        }
    }

    //Сборщик данных в пакеты для обработки и очистки буфера данных в случае ошибок связи
    class DataKeeper{
        ArrayList<Integer> list;
        int counter;

        DataKeeper(){
            list = new ArrayList<>(58);
            counter = 0;
        }

        synchronized void addData(int[] data){
            for (int i = 0; i < data.length; i++) {
                list.add(data[i]);
            }

            int size = list.size();
            if (size == 58) {
                int[] result = list.stream().mapToInt(Integer::intValue).toArray();
                listener.onInitialDataReceived(result);
                list.clear();
                counter = 0;
                return;
            }
            else if (size >= 2 && size <=5) {
                int[] result = list.stream().mapToInt(Integer::intValue).toArray();
                listener.onSerialPortDataReceived(result);
                list.clear();
                counter = 0;
                return;
            }
            if (counter >= 2){
                counter = 0;
                list.clear();
                return;
            }
            counter++;
        }
    }
}
