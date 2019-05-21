package com.example.bluetoothlibrary.bluetooth3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.widget.Toast;

import com.example.bluetoothlibrary.utils.LogUtil;

/**
 * 检测手机系统是否支持3.0
 */
public class SystemBtCheck {
    private static final String TAG = "SystemBtCheck";
    //3.0蓝牙
    public BluetoothAdapter bluetooth3Adapter;

    /**
     * 静态内部类：饿汉式-单例
     */
    private static class SystemBtCheckHolder{
        private static SystemBtCheck systemBtCheck = new SystemBtCheck();
    }

    /**
     * 单例模式获取
     */
    public static SystemBtCheck getInstance(){
        return SystemBtCheckHolder.systemBtCheck;
    }

    /**
     * 检擦是否支持蓝牙
     * @param context 上下文
     */
    public void initBle(Context context){
        if(!checkBt3()){
            LogUtil.showLogE(TAG,"该设备不支持蓝牙");
            Toast.makeText(context, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    /**
     * 打开蓝牙
     * @param isFast  true 直接打开蓝牙  false 提示用户打开
     */
    public void openBluetooth(Context context,boolean isFast){
        if(!isEnable()){
            if(isFast){
                LogUtil.showLogD(TAG,"直接打开手机蓝牙");
                bluetooth3Adapter.enable();  //BLUETOOTH_ADMIN权限
            }else{
                LogUtil.showLogD(TAG,"提示用户去打开手机蓝牙");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableBtIntent);
            }
        }else{
            LogUtil.showLogD(TAG,"手机蓝牙状态已开");
        }
    }

    /**
     * 直接关闭蓝牙
     */
    public void closeBlutooth(){
        if(bluetooth3Adapter == null)
            return;

        bluetooth3Adapter.disable();
    }

    /**
     * 检测手机系统是否支持3.0蓝牙
     * @return true 支持3.0  false 不支持3.0
     */
    private boolean checkBt3(){
        bluetooth3Adapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetooth3Adapter == null){
            return false;
        }else{
            LogUtil.showLogD(TAG,"该设备支持蓝牙3.0");
            return true;
        }
    }

    /**
     * 蓝牙是否开启
     */
    public boolean isEnable(){
        if(bluetooth3Adapter == null){
            LogUtil.showLogE(TAG,"isEnable-->bluetooth3Adapter == null");
            return false;
        }
        return bluetooth3Adapter.isEnabled();
    }

    /**
     * 本地蓝牙是否处于正在扫描状态
     * @return true false
     */
    public boolean isDiscovery(){
        if(bluetooth3Adapter ==null){
            LogUtil.showLogE(TAG,"isDiscovery-->bluetooth3Adapter == null");
            return false;
        }
        return bluetooth3Adapter.isDiscovering();
    }

    /**
     * 取消发现
     */
    public void cancelDiscovery(){
        if(bluetooth3Adapter ==null){
            LogUtil.showLogE(TAG,"cancelDiscovery-->bluetooth3Adapter == null");
            return;
        }
        if(isDiscovery()){
            bluetooth3Adapter.cancelDiscovery();
        }
    }

}
