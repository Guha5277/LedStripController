package com.guhafun;

import jssc.*;

public class SerialPortController implements SerialPortEventListener{
    private SerialPort serialPort;
    private final int dataBits = 8;
    private final int stopBits = 1;
    private final int parity = 0;

    SerialPortListener listener;

    SerialPortController(SerialPortListener listener){
        this.listener = listener;
    }

    public String[] getSerialPortList(){
        return SerialPortList.getPortNames();
    }

    public void connect(String port, int baudRate){
        serialPort = new SerialPort(port);
        try {
            serialPort.setParams(baudRate, dataBits, stopBits, parity);
            serialPort.addEventListener(this);
            listener.onSerialPortConnected(port, baudRate);
        } catch (SerialPortException e) {
            listener.onSerialPortFailedToConnect(port, baudRate);
            listener.onException(e);
        }
    }

    public void sendMessage(int...message){
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
        try {
            String port = serialPort.getPortName();
            serialPort.closePort();
            listener.onSerialPortClosed(port);
        } catch (SerialPortException e) {
            listener.onException(e);
        }
    }


    //Serial port events
    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        if (serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() == 54) { //Если пришли настройки (54 байт в буфере)
//            System.out.println("Setting has come!");
            try {
                listener.onSerialPortConfigReceived(serialPort.readIntArray());
            } catch (SerialPortException se) {
                listener.onException(se);
            }
        }

        if(serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() >= 2 && serialPortEvent.getEventValue() <= 5){
//            System.out.println("Responce has been comed!");
            try {
                listener.onSerialPortDataReceived(serialPort.readIntArray());
//                isRecponceRecived = true;

            }
            catch (SerialPortException se){
                listener.onException(se);
            }

        }
    }
}
