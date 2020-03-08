package com.guhafun;

interface SerialPortListener {
    void onSerialPortConnected(String port, int baudRate);
    void onSerialPortFailedToConnect(String port, int baudRate, Exception e);
    void onSerialPortClosed(String port);
    void onSerialPortFailedToClose(String port, Exception e);
    void onSerialPortDataSent(int...message);
    void onSerialPortDataReceived(int[] data);
    void onInitialDataReceived(int[] data);
    void onException(Exception e);
}
