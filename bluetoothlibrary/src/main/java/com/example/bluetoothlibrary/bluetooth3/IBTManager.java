package com.example.bluetoothlibrary.bluetooth3;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.example.bluetoothlibrary.listener.OnBTConReceiverListener;
import com.example.bluetoothlibrary.listener.OnBTConnectListener;
import com.example.bluetoothlibrary.listener.OnBindStateChangeListener;
import com.example.bluetoothlibrary.listener.OnBluetoothStateChangeListener;
import com.example.bluetoothlibrary.listener.OnDeviceSearchListener;

import java.util.List;

public interface IBTManager {

    //初始化蓝牙
    void initBluetooth(Context context);

    //打开蓝牙
    void openBluetooth(Context context,boolean isFast);

    //注销蓝牙广播
    void unRegisterBluetoothReceiver(Context context);

    void startDiscoveryDevice(OnDeviceSearchListener onDeviceSearchListener);

    void startDiscoveryDevice(OnDeviceSearchListener onDeviceSearchListener,long scanTime);

    void removeStopScanRunnable();

    void stopDiscoveryDevice();

    void startConnectDevice(BluetoothDevice bluetoothDevice, String uuid, long conOutTime, final OnBTConnectListener onBTConnectListener, OnBTConReceiverListener onBTConReceiverListener);

    void clearConnectedThread();

    boolean sendData(String data,boolean isHex);

    boolean sendData(byte[] data);

    List<BluetoothDevice> getBoundDeviceList();

    boolean boundDevice(BluetoothDevice bluetoothDevice);

    boolean disBoundDevice(BluetoothDevice bluetoothDevice);

    boolean getBluetoothState();

    void setOnBluetoothStateChangeListener(OnBluetoothStateChangeListener onBluetoothStateChangeListener);

    void setOnBindStateChangeListener( OnBindStateChangeListener onBindStateChangeListener);

    BluetoothDevice getDeviceByAddress(String macAddress);
}
