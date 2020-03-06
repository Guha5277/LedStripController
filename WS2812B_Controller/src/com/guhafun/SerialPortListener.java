package com.guhafun;

interface SerialPortListener {
    void onSerialPortConnected(String port, int baudRate);
    void onSerialPortFailedToConnect(String port, int baudRate);
    void onSerialPortClosed(String port);
    void onSerialPortDataSent(int...message);
    void onSerialPortDataReceived(int[] data);
    void onSerialPortConfigReceived(int[] data);
    void onException(Exception e);
}
