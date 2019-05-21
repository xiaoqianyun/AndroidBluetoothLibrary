package com.example.bluetoothlibrary.bluetooth3.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.bluetoothlibrary.utils.LogUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;


/**
 * 发起蓝牙连接的客户端
 */
public class ConnectThread extends Thread {
    private static final String TAG = "ConnectThread";
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothAdapter bluetoothAdapter,BluetoothDevice bluetoothDevice,String uuid)  {
       this.mBluetoothAdapter = bluetoothAdapter;
       this.mmDevice = bluetoothDevice;

       //使用一个临时变量，等会赋值给mmSocket
       //因为mmSocket是静态的
       BluetoothSocket tmp = null ;

       if(mmSocket != null){
           LogUtil.showLogE(TAG,"ConnectThread-->mmSocket != null先去释放");
           try {
               mmSocket.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
        LogUtil.showLogD(TAG,"ConnectThread-->mmSocket != null已释放");

       //1、获取BluetoothSocket
//        try {
//            tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
//
//        } catch (IOException e) {
//            LogUtil.showLogE(TAG,"ConnectThread-->获取BluetoothSocket异常!" + e.getMessage());
//        }


        try {
            tmp = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket",new Class[] {int.class}).invoke(mmDevice,3);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            LogUtil.showLogE(TAG,"ConnectThread-->获取BluetoothSocket异常1!" + e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            LogUtil.showLogE(TAG,"ConnectThread-->获取BluetoothSocket异常2!" + e.getMessage());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            LogUtil.showLogE(TAG,"ConnectThread-->获取BluetoothSocket异常3!" + e.getMessage());
        }


        mmSocket = tmp;
        if(mmSocket != null){
            LogUtil.showLogW(TAG,"ConnectThread-->已获取BluetoothSocket");
        }

    }

    @Override
    public void run(){

        //连接之前先取消发现设备，否则会大幅降低连接尝试的速度，并增加连接失败的可能性
        if(mBluetoothAdapter == null){
            LogUtil.showLogE(TAG,"ConnectThread:run-->mBluetoothAdapter == null");
            return;
        }
        //取消发现设备
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        if(mmSocket == null){
            LogUtil.showLogE(TAG,"ConnectThread:run-->mmSocket == null");
            return;
        }

        //2、通过socket去连接设备
        try {
            LogUtil.showLogD(TAG,"ConnectThread:run-->去连接...");
            if(onBluetoothConnectListener != null){
                onBluetoothConnectListener.onStartConn();  //开始去连接回调
            }
            mmSocket.connect();  //connect()为阻塞调用，连接失败或 connect() 方法超时（大约 12 秒之后），它将会引发异常

            if(onBluetoothConnectListener != null){
                onBluetoothConnectListener.onConnSuccess(mmSocket);  //连接成功回调
                LogUtil.showLogW(TAG,"ConnectThread:run-->连接成功");
            }

        } catch (IOException e) {
            LogUtil.showLogE(TAG,"ConnectThread:run-->连接异常！" + e.getMessage());

            if(onBluetoothConnectListener != null){
                onBluetoothConnectListener.onConnFailure("连接异常：" + e.getMessage());
            }

            //释放
            cancel();
        }


    }

    /**
     * 释放
     */
    public void cancel() {
        try {
            if (mmSocket != null && mmSocket.isConnected()) {
                LogUtil.showLogD(TAG,"ConnectThread:cancel-->mmSocket.isConnected() = " + mmSocket.isConnected());
                mmSocket.close();
                mmSocket = null;
                return;
            }

            if (mmSocket != null) {
                mmSocket.close();
                mmSocket = null;
            }

            LogUtil.showLogD(TAG,"ConnectThread:cancel-->关闭已连接的套接字释放资源");

        } catch (IOException e) {
            LogUtil.showLogE(TAG,"ConnectThread:cancel-->关闭已连接的套接字释放资源异常!" + e.getMessage());
        }
    }

    private OnBluetoothConnectListener onBluetoothConnectListener;

    public void setOnBluetoothConnectListener(OnBluetoothConnectListener onBluetoothConnectListener) {
        this.onBluetoothConnectListener = onBluetoothConnectListener;
    }

    //连接状态监听者
    public interface OnBluetoothConnectListener{
        void onStartConn();  //开始连接
        void onConnSuccess(BluetoothSocket bluetoothSocket);  //连接成功
        void onConnFailure(String errorMsg);  //连接失败
    }


}
