package com.example.bluetoothlibrary.bluetooth3;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.example.bluetoothlibrary.bluetooth3.bt.AcceptThread;
import com.example.bluetoothlibrary.bluetooth3.bt.ConnectThread;
import com.example.bluetoothlibrary.bluetooth3.bt.ConnectedThread;
import com.example.bluetoothlibrary.broadcastreceiver.BluetoothBroadcastReceiver;
import com.example.bluetoothlibrary.constant.ConsConfig;
import com.example.bluetoothlibrary.listener.OnBTConReceiverListener;
import com.example.bluetoothlibrary.listener.OnBTConnectListener;
import com.example.bluetoothlibrary.listener.OnBindStateChangeListener;
import com.example.bluetoothlibrary.listener.OnBluetoothStateChangeListener;
import com.example.bluetoothlibrary.listener.OnDeviceSearchListener;
import com.example.bluetoothlibrary.permission.PermissionListener;
import com.example.bluetoothlibrary.permission.PermissionRequest;
import com.example.bluetoothlibrary.utils.ClsUtils;
import com.example.bluetoothlibrary.utils.FormatConversion;
import com.example.bluetoothlibrary.utils.LogUtil;
import com.example.bluetoothlibrary.utils.TypeConversion;

import java.util.ArrayList;
import java.util.List;

public class BTManager implements IBTManager{

    private static final String TAG = "BTManager";
    private Context mContext;
    private SystemBtCheck systemBtCheck;
    private BluetoothAdapter bluetooth3Adapter;
    //蓝牙广播接收者
    private BluetoothBroadcastReceiver bluetoothBroadcastReceiver;
    //当前连接的设备
    private BluetoothDevice curConnDevice;
    //发起连接的线程
    private ConnectThread connectThread;
    //接收连接的线程
    private AcceptThread acceptThread;
    //管理连接的线程
    private ConnectedThread connectedThread;
    //3.0蓝牙连接监听者
    private OnBTConnectListener onBTConnectListener;

    //当前连接状态
    private boolean curConnState = false;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    /**
     * 静态内部类：饿汉式-单例
     */
    private static class BTManagerHolder{
        private static BTManager btManager = new BTManager();
    }

    /**
     * 单例模式获取
     */
    public static BTManager getInstance(){
        return BTManagerHolder.btManager;
    }


    //初始化蓝牙状态监听者
    @Override
    public void setOnBluetoothStateChangeListener(OnBluetoothStateChangeListener onBluetoothStateChangeListener){
       if(bluetoothBroadcastReceiver == null){
           LogUtil.showLogE(TAG,"setOnBluetoothStateChangeListener-->bluetoothBroadcastReceiver == null");
           return;
       }
        bluetoothBroadcastReceiver.setOnBluetoothStateChangeListener(onBluetoothStateChangeListener);
//       try {
//       }catch (Exception e){
//           throw new NullPointerException("com.example.bluetoothlibrary.bluetooth3.setOnBluetoothStateChangeListener()-->bluetoothBroadcastReceiver == null");
//       }
    }
    //初始化配对状态监听者
    @Override
    public void setOnBindStateChangeListener( OnBindStateChangeListener onBindStateChangeListener){
        if(bluetoothBroadcastReceiver == null){
            LogUtil.showLogE(TAG,"setOnBindStateChangeListener-->bluetoothBroadcastReceiver == null");
            return;
        }
        bluetoothBroadcastReceiver.setOnBindStateChangeListener(onBindStateChangeListener);
    }


    /**
     * 初始化蓝牙
     * 1、检测蓝牙是否支持
     * 2、注册蓝牙广播
     * 3、动态申请权限
     * @param context  上下文
     */
    @Override
    public void initBluetooth(Context context){
        mContext = context;
        systemBtCheck = SystemBtCheck.getInstance();
        //1、检测蓝牙是否支持
        systemBtCheck.initBle(mContext);
        bluetooth3Adapter = systemBtCheck.bluetooth3Adapter;

        if(bluetoothBroadcastReceiver == null){
            //2、注册蓝牙广播
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //蓝牙状态
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //绑定状态
            filter.addAction(BluetoothDevice.ACTION_FOUND); //扫描到设备
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始扫描
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//停止扫描
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);  //本地蓝牙扫描模式已更改
            filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST); //请求配对
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);  //与远程蓝牙建立ACL连接
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); //与远程蓝牙ACL断开连接
            filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED); //首次检索设备的友好名称
            bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver();
            mContext.registerReceiver(bluetoothBroadcastReceiver,filter);
        }

        //打开蓝牙在使用的时候开启
//        systemBtCheck.openBluetooth(mContext,false);

        //3、Android 6.0以上动态申请权限
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.requestRuntimePermission(mContext, ConsConfig.requestPermissions, new PermissionListener() {
                @Override
                public void onGranted() {
                    LogUtil.showLogD(TAG,"所有权限已被授予");
                }

                //用户勾选“不再提醒”拒绝权限后，关闭程序再打开程序只进入该方法！
                @Override
                public void onDenied(List<String> deniedPermissions) {
                    for (String deniedPermission : deniedPermissions) {
                        LogUtil.showLogE(TAG,"被拒绝权限：" + deniedPermission);
                    }
                }
            });
        }

    }

    @Override
    public void openBluetooth(Context context, boolean isFast) {
        //打开蓝牙在使用的时候开启
        if(systemBtCheck == null){
            LogUtil.showLogE(TAG,"openBluetooth-->systemBtCheck == null");
            return;
        }
        systemBtCheck.openBluetooth(context,isFast);
    }

    /**
     * 注销蓝牙广播
     */
    @Override
    public void unRegisterBluetoothReceiver(Context context){
        if(bluetoothBroadcastReceiver != null){
            context.unregisterReceiver(bluetoothBroadcastReceiver);
            bluetoothBroadcastReceiver = null;
        }
    }

    //////////////////////////////////  扫描设备  /////////////////////////////////////////////////

    private OnDeviceSearchListener onDeviceSearchListener;
    /**
     * 扫描设备
     * @param onDeviceSearchListener 设备扫描监听
     */
    @Override
    public void startDiscoveryDevice(OnDeviceSearchListener onDeviceSearchListener){
        if(bluetoothBroadcastReceiver == null){
            LogUtil.showLogE(TAG,"startDiscoveryDevice-->bluetoothBroadcastReceiver == null");
            return;
        }
        this.onDeviceSearchListener = onDeviceSearchListener;
        bluetoothBroadcastReceiver.setOnDeviceSearchListener(onDeviceSearchListener);

        if(bluetooth3Adapter == null){
            LogUtil.showLogE(TAG,"startDiscoveryDevice-->bluetooth3Adapter == null");
            return;
        }

        if(bluetooth3Adapter.isDiscovering()){
            LogUtil.showLogE(TAG,"startDiscoveryDevice-->已处于搜索状态");
            return;
        }

        LogUtil.showLogD(TAG,"开始扫描设备");
        bluetooth3Adapter.startDiscovery();
    }

    /**
     * 定时 扫描设备
     * @param onDeviceSearchListener  设备扫描监听
     * @param scanTime  扫描时间
     */
    @Override
    public void startDiscoveryDevice(OnDeviceSearchListener onDeviceSearchListener,long scanTime){
        if(bluetoothBroadcastReceiver == null){
            LogUtil.showLogE(TAG,"startDiscoveryDevice-->bluetoothBroadcastReceiver == null");
            return;
        }
        this.onDeviceSearchListener = onDeviceSearchListener;
        bluetoothBroadcastReceiver.setOnDeviceSearchListener(onDeviceSearchListener);

        if(bluetooth3Adapter == null){
            LogUtil.showLogE(TAG,"startDiscoveryDevice-->bluetooth3Adapter == null");
            return;
        }

        if(bluetooth3Adapter.isDiscovering()){
            LogUtil.showLogE(TAG,"startDiscoveryDevice-->正在扫描中...");
            return;
        }

        LogUtil.showLogD(TAG,"开始扫描设备");
        bluetooth3Adapter.startDiscovery();

        //定时
        mHandler.postDelayed(stopScanRunnable,scanTime);
    }

    private Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            if(onDeviceSearchListener != null){
                onDeviceSearchListener.onDiscoveryOutTime();  //扫描超时回调
            }
            stopDiscoveryDevice();
        }
    };


    @Override
    public void removeStopScanRunnable(){
        if(mHandler != null){
            mHandler.removeCallbacks(stopScanRunnable);
//            LogUtil.showLogD(TAG,"移除扫描超时");
        }
    }

    ////////////////////////////////////// 停止扫描  //////////////////////////////////////////////
    /**
     * 停止扫描
     */
    @Override
    public void stopDiscoveryDevice() {
        if(bluetooth3Adapter == null){
            LogUtil.showLogE(TAG,"stopDiscoveryDevice-->bluetooth3Adapter == null");
            return;
        }

        if(!bluetooth3Adapter.isDiscovering()){
            LogUtil.showLogD(TAG,"startDiscoveryDevice-->已停止扫描");
            return;
        }

        LogUtil.showLogD(TAG,"停止扫描设备");
        bluetooth3Adapter.cancelDiscovery();

    }

    //////////////////////////////////  获取已绑定的设备  /////////////////////////////////////////
    public List<BluetoothDevice> getBoundDeviceList(){
        if(bluetooth3Adapter == null){
            LogUtil.showLogE(TAG,"getBoundDeviceList-->bluetooth3Adapter == null");
            return null;
        }

        return new ArrayList<>(bluetooth3Adapter.getBondedDevices());
    }

    //////////////////////////////////  绑定/解绑设备  ////////////////////////////////////////////
    /**
     * 执行绑定 反射
     * @param bluetoothDevice 蓝牙设备
     * @return true 执行绑定 false 未执行绑定
     */
    @Override
    public boolean boundDevice(BluetoothDevice bluetoothDevice){
        if(bluetoothDevice == null){
            LogUtil.showLogE(TAG,"boundDevice-->bluetoothDevice == null");
            return false;
        }

        try {
            return ClsUtils.createBond(BluetoothDevice.class,bluetoothDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 执行解绑  反射
     * @param bluetoothDevice 蓝牙设备
     * @return  true 执行解绑  false未执行解绑
     */
    @Override
    public boolean disBoundDevice(BluetoothDevice bluetoothDevice){
        if(bluetoothDevice == null){
            LogUtil.showLogE(TAG,"disBoundDevice-->bluetoothDevice == null");
            return false;
        }

        try {
            return ClsUtils.removeBond(BluetoothDevice.class,bluetoothDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean getBluetoothState() {
        if(bluetooth3Adapter == null){
            LogUtil.showLogE(TAG,"getBluetoothState-->bluetooth3Adapter == null");
            return false;
        }
        return bluetooth3Adapter.isEnabled();
    }

    ///////////////////////////////////// 连接设备 ////////////////////////////////////////////////

    /**
     * 开始连接设备
     * @param bluetoothDevice   蓝牙设备
     * @param uuid               发起连接的UUID
     * @param conOutTime        连接超时时间
     */
    @Override
    public void startConnectDevice(final BluetoothDevice bluetoothDevice, String uuid, long conOutTime, final OnBTConnectListener onBTConnectListener, OnBTConReceiverListener onBTConReceiverListener){
        if(bluetoothDevice == null){
            LogUtil.showLogE(TAG,"startConnectDevice-->bluetoothDevice == null");
            return;
        }
        if(bluetooth3Adapter == null){
            LogUtil.showLogE(TAG,"startConnectDevice-->bluetooth3Adapter == null");
            return;
        }
        bluetoothBroadcastReceiver.setOnBTConReceiverListener(onBTConReceiverListener);
        //标记当前连接状态为 false
        curConnState = false;
        this.curConnDevice = bluetoothDevice;
        this.onBTConnectListener = onBTConnectListener;
        connectThread = new ConnectThread(bluetooth3Adapter,curConnDevice,uuid);
        connectThread.setOnBluetoothConnectListener(new ConnectThread.OnBluetoothConnectListener() {
            @Override
            public void onStartConn() {
                LogUtil.showLogD(TAG,"startConnectDevice-->开始连接..." + bluetoothDevice.getName() + "-->" + bluetoothDevice.getAddress());
                if(onBTConnectListener != null){
                    onBTConnectListener.onStartConnect();  //开始连接回调
                }
            }


            @Override
            public void onConnSuccess(BluetoothSocket bluetoothSocket) {
                //移除连接超时
                mHandler.removeCallbacks(connectOuttimeRunnable);
                LogUtil.showLogD(TAG,"startConnectDevice-->移除连接超时");
                LogUtil.showLogW(TAG,"startConnectDevice-->连接成功");
                if(onBTConnectListener != null){
                    onBTConnectListener.onConnectSuccess();  //连接成功回调
                }
                //标记当前连接状态为true
                curConnState = true;

                //管理连接，收发数据
                managerConnectSendReceiveData(bluetoothSocket);
            }

            @Override
            public void onConnFailure(String errorMsg) {
                LogUtil.showLogE(TAG,"startConnectDevice-->" + errorMsg);
                if(onBTConnectListener != null){
                    onBTConnectListener.onConnectFailure();  //连接失败回调
                }

                //标记当前连接状态为false
                curConnState = false;

                //断开管理连接
                clearConnectedThread();
            }
        });

        connectThread.start();
        //设置连接超时时间
        mHandler.postDelayed(connectOuttimeRunnable,conOutTime);

    }

    //连接超时
    private Runnable connectOuttimeRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtil.showLogE(TAG,"startConnectDevice-->连接超时" );
            if(onBTConnectListener != null){
                onBTConnectListener.onConnectOutTime();  //连接超时回调
            }
            //标记当前连接状态为false
            curConnState = false;

            //断开管理连接
            clearConnectedThread();
        }
    };

    ///////////////////////////////////////  断开当前连接  ////////////////////////////////////////

    /**
     * 断开已有的连接
     */
    @Override
    public void clearConnectedThread(){
        LogUtil.showLogD(TAG,"clearConnectedThread-->即将断开");

        //connectedThread断开已有连接
        if(connectedThread == null){
            LogUtil.showLogE(TAG,"clearConnectedThread-->connectedThread == null");
            return;
        }
        connectedThread.terminalClose(connectThread);

        //等待线程运行完后再断开
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connectedThread.cancel();  //释放连接

                connectedThread = null;
            }
        },10);

        LogUtil.showLogW(TAG,"clearConnectedThread-->成功断开连接");
        if(onBTConnectListener != null){
            onBTConnectListener.onDisConnectSuccess();  // 成功断开连接回调
        }

    }


    ///////////////////////////////////  管理已有连接、收发数据  //////////////////////////////////
    public void managerConnectSendReceiveData(BluetoothSocket bluetoothSocket){
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
        connectedThread.setOnSendReceiveDataListener(new ConnectedThread.OnSendReceiveDataListener() {
            @Override
            public void onSendDataSuccess(byte[] data) {
                LogUtil.showLogW(TAG,"发送数据成功,长度" + data.length + "->" + TypeConversion.bytes2HexString(data,data.length));
                if(onBTConnectListener != null){
                    onBTConnectListener.onSendSuccess(data); // 发送数据成功回调
                }
            }

            @Override
            public void onSendDataError(byte[] data,String errorMsg) {
                LogUtil.showLogE(TAG,"发送数据出错,长度" + data.length + "->" + TypeConversion.bytes2HexString(data,data.length));
                if(onBTConnectListener != null){
                    onBTConnectListener.onSendError(data,errorMsg); // 发送数据出错回调
                }
            }

            @Override
            public void onReceiveDataSuccess(byte[] buffer) {
                LogUtil.showLogW(TAG,"成功接收数据,长度" + buffer.length + "->" + TypeConversion.bytes2HexString(buffer,buffer.length));
                if(onBTConnectListener != null){
                    onBTConnectListener.onReceiveSuccess(buffer); // 成功接收数据回调
                }
            }

            @Override
            public void onReceiveDataError(String errorMsg) {
                LogUtil.showLogE(TAG,"接收数据出错：" + errorMsg);
                if(onBTConnectListener != null){
                    onBTConnectListener.onReceiveError(errorMsg); // 接收数据出错回调
                }
            }
        });
    }

    /**
     * 发送数据
     * @param data      要发送的数据 字符串
     * @param isHex     是否是16进制字符串
     * @return   true 发送成功  false 发送失败
     */
    @Override
    public boolean sendData(String data,boolean isHex){
        if(connectedThread == null){
            LogUtil.showLogE(TAG,"sendData:string -->connectedThread == null");
            return false;
        }
        if(data == null || data.length() == 0){
            LogUtil.showLogE(TAG,"sendData:string-->要发送的数据为空");
            return false;
        }

        if(isHex){  //是16进制字符串
            data.replace(" ","");  //取消空格
            //检查16进制数据是否合法
            if(data.length() % 2 != 0){
                //不合法，最后一位自动填充0
                String lasts = "0" + data.charAt(data.length() - 1);
                data = data.substring(0,data.length() - 2) + lasts;
            }
            LogUtil.showLogD(TAG,"sendData:string -->准备写入：" + FormatConversion.addStringSpace(data));  //加空格显示
            return connectedThread.write(TypeConversion.hexString2Bytes(data));
        }

        //普通字符串
        LogUtil.showLogD(TAG,"sendData:string -->准备写入：" + data);
        return connectedThread.write(data.getBytes());
    }

    /**
     * 发送数据
     * @param data   要发送的数据 byte[]
     * @return  true 发送成功  false 发送失败
     */
    @Override
    public boolean sendData(byte[] data){
        if(connectedThread == null){
            LogUtil.showLogE(TAG,"sendData:byte[]-->connectedThread == null");
            return false;
        }

        if(data == null || data.length == 0){
            LogUtil.showLogE(TAG,"sendData:byte[]-->要发送的数据为空");
            return false;
        }

        LogUtil.showLogD(TAG,"sendData:byte[] -->准备写入：" + TypeConversion.bytes2HexString(data,data.length)); //有空格16进制字符串
        return connectedThread.write(data);
    }


    ///////////////////////////////////////  其他方法  ////////////////////////////////////////////

    /**
     * 依据mac地址获取蓝牙设备
     * @param macAddress
     * @return
     */
    @Override
    public BluetoothDevice getDeviceByAddress(String macAddress){
        if(macAddress == null || macAddress.equals("")){
            LogUtil.showLogE(TAG,"getDeviceByAddress-->macAddress == null");
            return null;
        }

        if(bluetooth3Adapter == null){
            bluetooth3Adapter = SystemBtCheck.getInstance().bluetooth3Adapter;
        }

        return bluetooth3Adapter.getRemoteDevice(macAddress);
    }








}
