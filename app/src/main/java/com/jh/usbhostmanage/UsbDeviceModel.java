package com.jh.usbhostmanage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by admin on 2017/6/2.
 */

public class UsbDeviceModel {

    private String TAG = "UsbModel";
    public final static String ACTION_DEVICE_PERMISSION="ACTION_DEVICE_PERMISSION";
    public final static String ACTION_USB_DATA_SEND="ACTION_USB_DEVICE_GET_DATA";

    public String manufacturerName;
    public String name;
    public int productId;
    public String productName;
    public int protocol;
    public String serialNumber;
    public int subClass;
    public int vendorId;
    public String version;

    private Activity mActivity;


    public UsbDevice usbDevice;




    private UsbDeviceConnection connection;
    private UsbEndpoint usbEndpointIn;
    private UsbManager usbManager;
    private Thread thread;

    public UsbDeviceModel(Activity aActivity, UsbDevice aUsbDevice){

        usbDevice=aUsbDevice;
        mActivity=aActivity;

        manufacturerName =aUsbDevice.getManufacturerName();
        manufacturerName = manufacturerName ==null ?"UNKNOWN_DEVICE" :manufacturerName;
        name =aUsbDevice.getDeviceName();
        name = name ==null ? "" :name;
        productId=aUsbDevice.getProductId();
        productName=aUsbDevice.getProductName();
        productName= productName ==null ?"" : productName;
        protocol=aUsbDevice.getDeviceProtocol();
        serialNumber=aUsbDevice.getSerialNumber();
        serialNumber = serialNumber == null ?"":serialNumber;
        subClass=aUsbDevice.getDeviceSubclass();
        vendorId=aUsbDevice.getVendorId();
        version=aUsbDevice.getVersion();
        version=version ==null ?"":version;

    }

    public boolean hasPermission(){
        usbManager =(UsbManager) mActivity.getSystemService(Context.USB_SERVICE);
        return usbManager.hasPermission(usbDevice);
    }
    public boolean isOpen(){
        return connection !=null;
    }


}
