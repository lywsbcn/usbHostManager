package com.jh.usbhostmanage;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jh.usbhostmanage.usb_device.UsbDeviceManage;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends Activity {

    private String TAG="usb_ports";

    public static LocalBroadcastManager localBroadcastManager;

    private LinearLayout list_layout;
    private LinearLayout usb_head;
    private TextView manufacturerName;
    private TextView name;
    private TextView productId;
    private TextView productName;
    private TextView protocol;
    private TextView serialNumber;
    private TextView subclass;
    private TextView vendorId;
    private TextView version;
    private ImageView cardImage;
    private Button scanDevice;

    private  UsbManager usbManager;

    private UsbDeviceManage usbDeviceManage;

    Map<String,UsbDevice> usb_list;
    private ArrayList<UsbDeviceModel> usbModel_list=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbDeviceManage=new UsbDeviceManage(this);

        initializationViews();
        getUsbDeviceList();

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerUsbBroad();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterUsbBroad();

    }

    private void initializationViews(){
        list_layout=(LinearLayout) findViewById(R.id.usb_list_layout);
        usb_head=(LinearLayout)findViewById(R.id.usb_head_layout);
        manufacturerName =(TextView) findViewById(R.id.manyfactyrerName);
        name=(TextView)findViewById(R.id.name);
        productId=(TextView)findViewById(R.id.product_id);
        productName=(TextView)findViewById(R.id.product_name);
        protocol=(TextView)findViewById(R.id.protocol);
        serialNumber=(TextView)findViewById(R.id.seruakNumber);
        subclass=(TextView)findViewById(R.id.subclass);
        vendorId=(TextView)findViewById(R.id.vendorid);
        version=(TextView)findViewById(R.id.version);
        scanDevice=(Button)findViewById(R.id.scanDevice);
        scanDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUsbDeviceList();
            }
        });

        cardImage=(ImageView)findViewById(R.id.card_image);


    }
    /* 获取usb设备列表*/
    private void getUsbDeviceList(){
        list_layout.removeAllViews();
        usbModel_list.clear();

        usb_list = usbDeviceManage.usb_map();

        int i=0;
        for (String s : usb_list.keySet()){

            UsbDeviceModel model = new UsbDeviceModel(this,usb_list.get(s));

            if (model.productId == 2922 && model.vendorId == 3118 || 1==1) {

                usbModel_list.add(model);

                UsbDeviceButton button = new UsbDeviceButton(this);
                button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                button.usbDeviceModel = model;
                button.setText(model.manufacturerName);

                list_layout.addView(button);
                button.setOnClickListener(new UsbDeviceButtonClickListener());
                if (i == 0) button.callOnClick();
                i++;
            }
        }

    }




    private class UsbDeviceButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            if (v instanceof UsbDeviceButton){
                UsbDeviceButton button=(UsbDeviceButton)v;
                showUsbDeviceDetail(button.usbDeviceModel);
            }
        }
    }

    private void showUsbDeviceDetail(UsbDeviceModel model){
        usb_head.removeAllViews();
        manufacturerName.setText(model.manufacturerName);
        name.setText        ("name: "+model.name);
        productId.setText   ("productId: "+model.productId);
        productName.setText ("productName: "+model.productName);
        protocol.setText    ("protocol: "+model.protocol);
        serialNumber.setText("serialNumber: "+model.serialNumber);
        subclass.setText    ("subclass: "+model.subClass);
        vendorId.setText    ("vendorId: "+model.vendorId);
        version.setText     ("version: "+model.version);

        if (!model.hasPermission()){
            UsbDeviceButton premissionBtn=new UsbDeviceButton(this);
            premissionBtn.setText("授权");
            premissionBtn.usbDeviceModel=model;
            premissionBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            usb_head.addView(premissionBtn);
            premissionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    usbDeviceManage.applyPermission(((UsbDeviceButton)v).usbDeviceModel.usbDevice);
                }
            });

        }
        UsbDeviceButton open =new UsbDeviceButton(this);
        open.setText("打开");
        open.usbDeviceModel=model;
        open.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        usb_head.addView(open);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usbDeviceManage.open(((UsbDeviceButton)v).usbDeviceModel.usbDevice);
                v.setEnabled(false);
            }
        });
        if (usbDeviceManage.isOpen(model.usbDevice)) open.setEnabled(false);


    }



    private void registerUsbBroad(){
        registerReceiver(usbDeviceManage.permissionReciver,createIntentFilter(UsbDeviceManage.ACTION_DEVICE_PERMISSION));

        registerReceiver(new UsbDataReceive(),createIntentFilter(UsbDeviceManage.ACTION_USB_DATA_SEND));
    }
    private void unregisterUsbBroad(){
        unregisterReceiver(usbDeviceManage.permissionReciver);
//        unregisterReceiver();
    }

    private class UsbDataReceive extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String s =intent.getStringExtra("usb_data");
            s= s.trim();
            try {
                int v=Integer.parseInt(s);

                int cardIndex=UsbCard.getCardIndex(v/10,UsbCard.scheme2);
                if (cardIndex !=-1)   showCard(cardIndex);
                else {
                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                }

            }catch (NumberFormatException e){
                Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
            }

//            int cardIndex=UsbCard.getCardIndex(s,UsbCard.scheme1);
//            if (cardIndex !=-1)   showCard(cardIndex);
//                else {
//                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
//                }

            Log.d(TAG, "onReceive: "+s);


        }
    }

    public IntentFilter createIntentFilter(String filterName){
        IntentFilter filter=new IntentFilter();
        filter.addAction(filterName);
        return filter;
    }

    private void showCard(int index){
        String imageName="card"+index;
        int imageId=getResources().getIdentifier(imageName,"drawable","com.jh.usbhostmanage");
        cardImage.setImageResource(imageId);
    }


}
