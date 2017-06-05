# usbHostManager
ANDROID USB转串口编程

我们开发使用的是usb主机模式,即:安卓平板作为主机,usb外设作为从机进行数据通信。整个开发过程有以下几点：

1：发现设备

/* 发现usb设备*/
private void findUsbList(){
    usbManager =(UsbManager) getSystemService(USB_SERVICE);
    usbList =usbManager.getDeviceList();
    String SN="14289B3276";
    for (String s : usbList.keySet()){
        UsbDevice tmp=usbList.get(s);
        String tmpSN = tmp.getSerialNumber();
        if (tmpSN !=null && tmpSN.equals(SN)){
      	usbDevice = tmp;
        }
    }
}

通过UsbManager这个系统提供的类,我们可以枚举出当前连接的所有usb设备,我们主要需要的是UsbDevice对象,是的,这个类就代表了android所有连接的usb设备。

2：打开设备
接下来，我们需要打开刚刚搜索到的usb设备，我们可以将平板与usb外设之间的连接想象成一个通道，只有把通道的门打开后，两边才能进行通信。
一般来说，在没有定制的android设备上首次访问usb设备的时候，默认我们是没有访问权限的，因此我们首先要判断对当前要打开的usbDevice是否有访问权限：

/* 检查权限,和申请授权*/

private void checkPermission(){
    if(!usbManager.hasPermission(usbDevice)){
        Log.d(TAG, "hasPermission: false");
        usbPermissionReceiver=new UsbPermissionReceiver();
        
        Intent intent= new Intent(ACTION_DEVICE_PERMISSION);
        PendingIntent mPermissionIntent =PendingIntent.getBroadcast(this,0,intent,0);
        IntentFilter fileter = new IntentFilter(ACTION_DEVICE_PERMISSION);
        registerReceiver(usbPermissionReceiver,fileter);
        usbManager.requestPermission(usbDevice,mPermissionIntent);
    }else {
        Log.d(TAG, "hasPermission: true");

        getUsbPoints();
    }
}

这里我们声明一个广播UsbPermissionReceiver,当接收到授权成功的广播后做一些其他的处理:

private class UsbPermissionReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        String action=intent.getAction();
        if(ACTION_DEVICE_PERMISSION.equals(action)){
            synchronized (this){
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getDeviceName().equals(usbDevice.getDeviceName())){
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)){
                        Log.d(TAG, "授权成功!!!!");
                        getUsbPoints();
                    }else{
                        Log.d(TAG, "授权失败!!!!!");
                    }
                }
            }
        }
    }
}

接下来，我们要找到具有数据传输功能的接口UsbInterface，从它里边找到数据输入和输出端口UsbEndpoint，一般情况下，一个UsbDevice有多个UsbInterface，我们需要的一般是第一个，但是这里我们要的是第二个，所以：
usbInterface =usbDevice.getInterface(1);

同样的，一个UsbInterface有多个UsbEndpoint，有控制端口和数据端口等等，因此我们需要根据类型和数据流向来找到我们需要的数据输入和输出两个端口：

private void getUsbPoints(){
    usbInterface =usbDevice.getInterface(1);
    for (int i=0;i < usbInterface.getEndpointCount();i++){
        UsbEndpoint p=usbInterface.getEndpoint(i);
        if (p.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK){ 
            if(p.getDirection() == UsbConstants.USB_DIR_IN){
                usbEndpointIn =p;
            }else if (p.getDirection() == UsbConstants.USB_DIR_OUT){
                usbEndpointOut =p;
            }
        }
    }
    connection = usbManager .openDevice(usbDevice);
    connection.controlTransfer(0x21, 0x09, 0x200, 0,null, 0, 0);
    threadGetData();
}

最后,才是真正的打开usb设备,我们需要和外设建立一个UsbDeviceConnection，它的获取也很简单，就一句代码：

connection = usbManager .openDevice(usbDevice);	

到这里，理论上平板和usb外设之间的连接已经建立了，也可以首发数据了，但是，我们大部分情况下还需要对usb串口进行一些配置，比如波特率，停止位，数据控制等，不然两边配置不同，收到的数据会乱码。具体怎么配置，就要看你连接的usb外设是什么串口的芯片了。不在这里阐述了。

3.数据传输
到这里，我们已经可以于usb外设进行数据传输了，首先来看怎么向usb设备发送数据。

1）	向usb外设发送数据
在第二步中，我们已经获取了数据的输入输出端口usbEndPointOut，我们向外设发送数据就是通过这个端口来实现的。来看怎么用：
int ret = connection.bulkTransfer(usbEndpointOut, data, data.length, 1000);
bulkTransfer这个函数用于在给定的端口进行数据传输，第一个参数就是此次传输的端口，这里我们用的是输出端口，第二个参数就是要发送的数据，类型为字节数组，第三个参数代表要发送的数据长度，最后一个参数是超时，返回值代表发送成功的字节数，如果返回-1，那就是发送失败了。

2）	接收usb外设发送来的数据
同理，我们已经找到了数据输入端口 UsbEndpointIn，因为数据的输入是不定时的，因此我们可以另开一个线程，来专门接收数据：

private void threadGetData(){
    thread =new Thread(){
        @Override
        public void run() {
            super.run();           
            getUsbData();
            }
    };
    thread.start();
}

接收数据的代码如下：

private void getUsbData(){
        int inMax = usbEndpointIn.getMaxPacketSize();
        ByteBuffer byteBuffer = ByteBuffer.allocate(inMax);
        UsbRequest usbRequest = new UsbRequest();
        usbRequest.initialize(connection, usbEndpointIn);
        usbRequest.queue(byteBuffer, inMax);
        if (connection.requestWait() == usbRequest) {
            byte[] retData = byteBuffer.array();            
            String s="";
            for (Byte b1 : retData) { if (b1 !=0)   s+=(char)(int)b1;}
            Log.d(TAG, s);  //输出
            usbRequest.close();
            getUsbData();
        }
    }
}

