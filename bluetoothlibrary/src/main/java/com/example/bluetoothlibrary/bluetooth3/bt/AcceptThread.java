package com.example.bluetoothlibrary.bluetooth3.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.bluetoothlibrary.utils.LogUtil;

import java.io.IOException;
import java.util.UUID;

/**
 * 用于接受传入连接的服务器
 * 设置服务器套接字并接受连接的过程
 * 1、
 * 2、
 * 3、
 */
public class AcceptThread extends Thread {
    private static final String TAG = "BTManager";
    private final BluetoothServerSocket mmServerSocket;
    private final BluetoothAdapter mBluetoothAdapter;

    public AcceptThread(BluetoothAdapter bluetoothAdapter,String uuid){
        this.mBluetoothAdapter = bluetoothAdapter;

        // 使用一个临时变量，等会赋值给mmServerSocket
        // 因为mmServerSocket是静态的
        BluetoothServerSocket tmp = null;
        try {
            //1、获取BluetoothServerSocket
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("blue", UUID.fromString(uuid));

        } catch (IOException e) {
            LogUtil.showLogE(TAG,"AcceptThread-->获取BluetoothServerSocket异常!" + e.getMessage());
        }

        mmServerSocket = tmp;
        if(mmServerSocket != null){
            LogUtil.showLogW(TAG,"AcceptThread-->已获取BluetoothServerSocket");
        }

    }

    @Override
    public void run() {
        BluetoothSocket socket = null;
        //持续监听连接状态，直到出现异常或者建立连接
        while(true){
            try {

                if(mmServerSocket == null){
                    LogUtil.showLogE(TAG,"AcceptThread:run-->mmServerSocket == null");
                    break;
                }
                //2、调用 accept() 开始侦听连接请求
                socket = mmServerSocket.accept();

            } catch (IOException e) {
                LogUtil.showLogE(TAG,"AcceptThread:run-->服务器接受连接请求异常!" + e.getMessage());
                break;
            }

            //连接请求已被接受
            if(socket != null){
                LogUtil.showLogW(TAG,"AcceptThread:run-->服务器成功接受连接请求");
                //释放资源
                cancel();
            }
        }
    }

    /**
     * 释放服务器套接字及其所有资源
     */
    private void cancel() {
        try {

            if(mmServerSocket == null){
                LogUtil.showLogE(TAG,"AcceptThread:cancel-->mmServerSocket == null");
                return;
            }

            //3、释放
            mmServerSocket.close();
            this.interrupt();  //改变线程的中断状态
            LogUtil.showLogW(TAG,"AcceptThread:cancel-->释放服务器套接字及其所有资源");

        } catch (IOException e) {
            LogUtil.showLogE(TAG,"AcceptThread:cancel-->释放服务器套接字及其所有资源异常！" + e.getMessage());
        }
    }

}
