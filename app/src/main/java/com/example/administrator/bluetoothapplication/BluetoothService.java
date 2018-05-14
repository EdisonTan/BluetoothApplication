package com.example.administrator.bluetoothapplication;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.content.ContentValues.TAG;
//import static android.telecom.Call.STATE_CONNECTING;

/**
 * Created by Administrator on 2017/2/26 0026.
 */

public class BluetoothService extends Service implements Thread.UncaughtExceptionHandler {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    public void startServiceDiscovery(){
        mBluetoothGatt.discoverServices();

    }
    public void showState(){
        String str = String.valueOf(mBluetoothGatt.getDevice().getBondState());
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        str = String.valueOf(mBluetoothGatt.getServices().size());
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
        str = String.valueOf(service == null);
        Toast.makeText(this, str,Toast.LENGTH_SHORT).show();
        this.enable_JDY_ble(true);

    }

    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DIGIT_SEND_COMPLETE =
            "com.example.administrator.bluetoothapplication";
    public final static String ACTION_DIGIT_TEST =
            "com.example.administrator.bluetoothapplication.ACTION_DIGIT_TEST";



    private IBinder mBinder = new LocalBinder();

    public static String Service_uuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_FUNCTION = "0000ffe1-0000-1000-8000-00805f9b34fb";

    //必须实现接口uncaughtException
    @Override
    public void uncaughtException(Thread arg0, Throwable arg1) {
        //在此处理异常， arg1即为捕获到的异常
        Log.i("AAA", "uncaughtException   " + arg1);
        arg1.printStackTrace();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        System.out.println("onBind Service");

        Thread.setDefaultUncaughtExceptionHandler(BluetoothService.this);


        Toast.makeText(this, "service已经开启", Toast.LENGTH_SHORT).show();

        //simulateSignal();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.i(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mBluetoothGatt.discoverServices();


        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void enable_JDY_ble(boolean p) {
        try {
            if (p) {
                BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));

                Log.i(TAG, String.valueOf(service.getUuid().toString()));

                if (service == null)
                {

                    Toast.makeText(this, "service is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                BluetoothGattCharacteristic ale = service.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));

                Log.i("AAA",String.valueOf(service.getCharacteristics().size()));

                boolean set = mBluetoothGatt.setCharacteristicNotification(ale, true);
                Log.d(TAG, " setnotification = " + set);
                BluetoothGattDescriptor dsc = ale.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                byte[] bytes = {0x01, 0x00};
                dsc.setValue(bytes);
                boolean success = mBluetoothGatt.writeDescriptor(dsc);
                Log.d(TAG, "writing enabledescriptor:" + success);
            } else {
                BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"));
                BluetoothGattCharacteristic ale = service.getCharacteristic(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
                boolean set = mBluetoothGatt.setCharacteristicNotification(ale, false);
                Log.d(TAG, " setnotification = " + set);
                BluetoothGattDescriptor dsc = ale.getDescriptor(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
                byte[] bytes = {0x00, 0x00};
                dsc.setValue(bytes);
                boolean success = mBluetoothGatt.writeDescriptor(dsc);
                Log.d(TAG, "writing enabledescriptor:" + success);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.d(TAG, "CharacteristicRead");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            ///////////////////////////////////////////////////
            //Log.d(TAG, "CharacteristicChanged");


        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "some service is discovered " + status);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X", byteChar));
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        }
        sendBroadcast(intent);
    }
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    byte[] WriteBytes = new byte[20];
    public void txxx(String g){
        g=""+g;
        WriteBytes= hex2byte(g.getBytes());

        BluetoothGattCharacteristic gg;

        gg=mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
        //byte t[]={51,1,2};
        gg.setValue(WriteBytes);
        mBluetoothGatt.writeCharacteristic(gg);

        Intent intent = new Intent(ACTION_DIGIT_SEND_COMPLETE);
        sendBroadcast(intent);

        //mBluetoothGatt.setCharacteristicNotification(gg, true);

        //gg=mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
        //mBluetoothGatt.setCharacteristicNotification(gg, true);
    }

    public  byte[] hex2byte(byte[] b) {
        if ((b.length % 2) != 0) {
            throw new IllegalArgumentException("长度不为2的倍数");
        }
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        b = null;
        return b2;
    }


    private void simulateSignal(){
        final int maxSize = 0x7fffff;

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        double part = 0.2;
                        double step = 0.03;
                          while(true){
                              final Intent testSignalIntent = new Intent(BluetoothService.ACTION_DIGIT_TEST);
                              part += step;
                              if (part > 0.4) {
                                  part = 0.1;
                              }
                              String valueStr = String.valueOf(maxSize * (part));
                              String sendValue = valueStr + valueStr + valueStr;
                              testSignalIntent.putExtra(BluetoothService.EXTRA_DATA,sendValue);
                              sendBroadcast(testSignalIntent);
                              Log.d("test",sendValue);
                              try{
                                  Thread.sleep(10);
                              } catch (InterruptedException e){
                                  e.printStackTrace();
                              }
                          }
                    }
                }
        ).start();
    }


}
