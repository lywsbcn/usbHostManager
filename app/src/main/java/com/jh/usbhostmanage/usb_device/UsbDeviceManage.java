package com.jh.usbhostmanage.usb_device;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import android.widget.Toast;

import com.jh.usbhostmanage.MainActivity;
import com.jh.usbhostmanage.UsbDeviceModel;

import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by admin on 2017/6/3.
 */

public class UsbDeviceManage {
    public final static String ACTION_DEVICE_PERMISSION="ACTION_DEVICE_PERMISSION";
    public final static String ACTION_USB_DATA_SEND="ACTION_USB_DATA_SEND";

    private String TAG ="UsbDeviceManage";
    private Activity mActivity;
    private UsbManager usbManager;
    private ArrayList<UsbDeviceConnection> connection_list=new ArrayList<>();

    public UsbPermissionReciver permissionReciver=new UsbPermissionReciver();
    public usbDataReciver usbDataReciver =new usbDataReciver();

    /* 初始化usb管理类*/
    public UsbDeviceManage (Activity aActivity){
        mActivity=aActivity;
        usbManager=(UsbManager)mActivity.getSystemService(Context.USB_SERVICE);
    }
    /* 获取所有usb列表(map)*/
    public Map<String,UsbDevice>usb_map(){
        return usbManager.getDeviceList();
    }

    /* 获取所有usb列表(array)*/
    public ArrayList<UsbDevice> usb_list(){
        ArrayList <UsbDevice> tArray=new ArrayList<>();

        for (String s : usb_map().keySet()){
            tArray.add(usb_map().get(s));
        }
        return tArray;
    }
    /* 根据 PID\ VID 获取usb列表*/
    public ArrayList<UsbDevice> usb_list(int productId,int vendorId){
        ArrayList <UsbDevice> tArray=new ArrayList<>();
        for (String s :usb_map().keySet()){
            UsbDevice u=usb_map().get(s);
           if (getProductId(u) == productId && getVendorId(u)==vendorId){
               tArray.add(u);
           }
        }
        return tArray;
    }
    /* 根据 设备名 deviceName (name) 获取设备*/

    public UsbDevice deviceByName(String name){
        return usb_map().get(name);
    }
    /* 根据 设备sno 获取设备*/
    public UsbDevice deviceBySno(String sno){
        ArrayList<UsbDevice> tArray=usb_list();
        for (UsbDevice u : tArray){
            String s=getSerialNumber(u);
            if (s != null && s.equals(sno)){
                return u;
            }
        }
        return null;
    }

   /* 获取PID*/
    public int getProductId(UsbDevice aUsbDevice){
        return aUsbDevice.getProductId();
    }
    /* 获取VID*/
    public int getVendorId(UsbDevice aUsbDevice){
        return aUsbDevice.getVendorId();
    }
    /* 获取SNO*/
    public String getSerialNumber(UsbDevice aUsbDevice){
        return aUsbDevice.getSerialNumber();
    }

    /* 申请授权*/
    public void applyPermission(UsbDevice aUsbDevice){
        Intent intent=new Intent(UsbDeviceModel.ACTION_DEVICE_PERMISSION);
        PendingIntent pend=PendingIntent.getBroadcast(mActivity,0,intent,0);
        usbManager.requestPermission(aUsbDevice,pend);
    }
    /* 设置权限通知接收*/
    public class UsbPermissionReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
            String action=intent .getAction();

            if (UsbDeviceModel.ACTION_DEVICE_PERMISSION.equals(action)){
                synchronized (this){
//                     UsbDevice device =intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)){
                        Log.d(TAG, "授权成功!!!!");
                        Toast.makeText(mActivity, "授权成功", Toast.LENGTH_SHORT).show();
//                        checkPermission();
                    }else {
                        Log.d(TAG, "授权失败!!!!!");
                        Toast.makeText(mActivity, "授权失败",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    /* 打开并连接设备*/
    public void open(UsbDevice aUsbDevice){
        Log.d(TAG, "UsbInterface_count: " + aUsbDevice.getInterfaceCount());

        UsbInterface usbInterface =aUsbDevice.getInterface(1);
        UsbEndpoint usbEndpointIn=null;     //设备输出(读取设备数据)
        UsbEndpoint usbEndpointOut=null;    //设备输入(写入设备)

        Log.d(TAG, "UsbPoint_count "+usbInterface.getEndpointCount());

        for (int i=0;i < usbInterface.getEndpointCount();i++){
            UsbEndpoint p=usbInterface.getEndpoint(i);

            if (p.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK){
                Log.d(TAG, "USB_ENDPOINT_XFER_BULK: ");
                if(p.getDirection() == UsbConstants.USB_DIR_IN){
                    Log.d(TAG, "USB_DIR_IN: ");
                    usbEndpointIn =p;
                }else if (p.getDirection() == UsbConstants.USB_DIR_OUT){
                    Log.d(TAG, "USB_DIR_OUT: ");
                    usbEndpointOut=p;
                }
            }
        }


        UsbDeviceConnection connection = usbManager .openDevice(aUsbDevice);

        connection_list.add(connection);

        connection.controlTransfer(0x21, 0x09, 0x200, 0,null, 0, 0);

        threadGetData(connection,usbEndpointIn);
    }

    private void threadGetData(final UsbDeviceConnection connection, final UsbEndpoint in){
        Thread thread =new Thread(){
            @Override
            public void run() {
                super.run();

                Log.d(TAG, "new thread: ");
                getUsbData(connection,in);

                Log.d(TAG, "Thread finish");
            }
        };
        thread.start();
    }
    private void getUsbData(UsbDeviceConnection connection, UsbEndpoint in){
        int inMax= in.getMaxPacketSize();

        ByteBuffer byteBuffer = ByteBuffer.allocate(inMax);
        UsbRequest usbRequest = new UsbRequest();

        usbRequest.initialize(connection, in);

        usbRequest.queue(byteBuffer, inMax);

        if (connection.requestWait() == usbRequest) {
            byte[] retData = byteBuffer.array();

            String s="";
            for (Byte b1 : retData) {
                if (b1 !=0)  {
                    char c=(char)(int)b1;
                    s+=String.valueOf(c);
                }
            }
            Log.d(TAG, "getUsbData: "+s);
            usbDataSend(s);

            usbRequest.close();

            getUsbData(connection,in);
        }
    }

    public void usbDataSend(String s){
        Intent intent=new Intent(ACTION_USB_DATA_SEND);
        intent.putExtra("usb_data",s);
        mActivity.sendBroadcast(intent);
    }
    public class usbDataReciver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String s=intent.getStringExtra("usb_data");
            Log.d(TAG, "onReceive: "+s);
        }
    }

    public boolean isOpen(UsbDevice aUsbDevice){
        for (UsbDeviceConnection c : connection_list){
            String sn=getSerialNumber(aUsbDevice);
            if(sn!=null && sn.equals(c.getSerial())){
                return true;
            }
        }
        return false;
    }

}
