package com.jh.usbhostmanage;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.widget.Button;

/**
 * Created by admin on 2017/6/2.
 */

public class UsbDeviceButton extends Button {

    public UsbDeviceModel usbDeviceModel;

    public UsbDeviceButton(Context aContext){
        super(aContext);
    }
}
