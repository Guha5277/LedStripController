package com.guhafun;

interface SerialPortListener {
    void onSerialPortConnected(String port, int baudRate);
    void onSerialPortFailedToConnect(String port, int baudRate, Exception e);
    void onFailedToSendData();
    void onSerialPortClosed(String port);
    void onSerialPortFailedToClose(String port, Exception e);
    void onSerialPortDataReceived(int[] data);
    void onInitialDataReceived(int[] data);
    void onException(Exception e);
}
