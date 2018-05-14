package com.example.administrator.bluetoothapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    public static String EXTRAS_DEVICE_NAME = "MainActivity.EXTRAS_DEVICE_NAME";
    public static String EXTRAS_DEVICE_ADDRESS = "MainActivity.EXTRAS_DEVICE_ADDRESS";

    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothData bluetoothData;
    private BluetoothAdapter myBluetoothAdapter = null;
    private BluetoothSocket socket;
    private BluetoothDevice myBluetoothDevice;
    private BluetoothService mBluetoothLeService;
    private String mDeviceAddress1 = "A8:1B:6A:85:A8:02";
    private String mDeviceAddress0 = "98:5D:AD:25:4A:B2";
    private String mDeviceAddress = "C8:FD:19:40:84:5A";
    private String mDeviceAddress2 = "01:02:03:04:0B:83";
    private TextView stateView;
    private TextView leadIView;
    private TextView leadIIView;
    private TextView leadIIIView;
    private TextView leadIValueView;
    private TextView leadIIValueView;
    private EditText inputView;


    int HEIGHT ;
    int WIDTH ;
    int count = 0;
    int pathCount = 0;
    private int coordinateX = 0;
    private int coordinateY = 120;
    private int preX = 0;
    private int preY = 100;
    private SurfaceView leadIWaveView;
    private SurfaceView leadIIWaveView;
    private SurfaceView leadIIIWaveView;
    private SurfaceView leadAVRWaveView;
    private SurfaceView leadAVLWaveView;
    private SurfaceView leadAVFWaveView;
    private SurfaceHolder leadIHolder;
    private SurfaceHolder leadIIHolder;
    private SurfaceHolder leadIIIHolder;
    private SurfaceHolder leadAVRHolder;
    private SurfaceHolder leadAVLHolder;
    private SurfaceHolder leadAVFHolder;
    private Paint paint = new Paint();
    private ArrayList<Integer> leadIBuf;
    private ArrayList<Integer> leadITempBuffer;
    private ArrayList<Integer> leadIIBuf;
    private ArrayList<Integer> leadIITempBuffer;
    private ArrayList<Integer> leadIIIBuf;
    private ArrayList<Integer> leadIIITempBuffer;
    private int oldX = 0;
    private int oldY = 100;
    private int oldYleadII = 100;
    private int oldYleadIII = 100;
    private int oldYleadAVR = 100;
    private int oldYleadAVL = 100;
    private int oldYleadAVF = 100;
    private int scale = 5;
    private double maxValue = 1;
    private int amplifier = 300;
    private int viewGap = 100;
    private SimpleDateFormat simpleDateFormat;
    private String filePath;
    private FileOutputStream globleFout;


    private boolean SurfaceFlag = false;


    private boolean connectState = false;
    private int adsState;
    private int leadIValue;
    private int leadIIValue;
    private int leadIIIValue;   //leadIII = leadII - leadI
    private int leadAVR;   // avr = -0.5*(I+II)
    private int leadAVL; //avl = I - 0.5 * II
    private int leadAVF; //avf = -0.5I + II
    private int signMask = 0x8000;
    private int valueMask = 0x7FFFFF;

    private int offset = 625;

    //data pre-processing

    private int window_size = 20;
    private ArrayList<Double> filter_list_leadI = new ArrayList<>();
    private ArrayList<Double> filter_list_leadII = new ArrayList<>();
    private int threshold_filter = 500;

    private LinkedList<Double> value_leadI = new LinkedList<>();
    private LinkedList<Double> value_leadII = new LinkedList<>();
    private int divider = 0x7fffff;
    private Thread waveThread;

    private ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            mBluetoothLeService = ((BluetoothService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            Intent intent = getIntent();

//            mBluetoothLeService.connect(intent.getStringExtra(EXTRAS_DEVICE_ADDRESS));
            mBluetoothLeService.connect(mDeviceAddress2);

            Toast.makeText(MainActivity.this,"绑定成功", Toast.LENGTH_SHORT).show();
            Log.i("BLE","绑定成功");
            connectState = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bluetoothData = null;
            connectState = false;
            Toast.makeText(MainActivity.this,"绑定失败", Toast.LENGTH_LONG).show();
            Log.i("BLE","绑定失败");
        }
    };

    @Override
    public void finish(){
        super.finish();
        try{
            globleFout.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //在这里创建一个数据文件，用来存储画图数据。
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH_mm_ss");
        Date date = new Date(System.currentTimeMillis());

        filePath = Environment.getExternalStorageDirectory().getPath() +
                File.separator +
                "footBandData" +
                File.separator +
                simpleDateFormat.format(date) + ".txt";
        Log.d("filePath",filePath);



        File file = new File(filePath);
        File dir = new File(file.getParent());

        Comparator<Integer> big_root = new Comparator<Integer>(){
            @Override
            public int compare(Integer left,Integer right){
                return right.compareTo(left);
            }
        };
        Comparator<Integer> small_root = new Comparator<Integer>(){
            @Override
            public int compare(Integer left,Integer right){
                return left.compareTo(right);
            }
        };

        if (!dir.exists()){
            try {
                //检测是否有写的权限
                int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                        "android.permission.WRITE_EXTERNAL_STORAGE");
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // 没有写的权限，去申请写的权限，会弹出对话框
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{
                                    "android.permission.WRITE_EXTERNAL_STORAGE",
                                    "android.permission.READ_EXTERNAL_STORAGE"
                            },
                            1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("file","make dir" + dir.getPath());
            if (!dir.mkdir()){
                Log.d("file","make dir failed");
            }
        }

        try{
            if (file.createNewFile()){
                globleFout = new FileOutputStream(file);
//                byte [] bytes = "suck a dick".getBytes();
//                fout.write(bytes);
            } else {
                Log.d("create file","create file failed");
            }

        } catch (Exception e){
            e.printStackTrace();
        }



        myBluetoothAdapter =BluetoothAdapter.getDefaultAdapter();
        OpenBluetooth(myBluetoothAdapter);



        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        stateView = (TextView)findViewById(R.id.stateView);
        leadIView = (TextView)findViewById(R.id.leadOneView);
        leadIIView = (TextView)findViewById(R.id.leadTwoView);
        leadIValueView = (TextView)findViewById(R.id.leadIValueView);
        leadIIValueView = (TextView)
                findViewById(R.id.leadIIValueView);
        inputView = (EditText)findViewById(R.id.inputEditText);

        leadIWaveView = (SurfaceView) findViewById(R.id.leadIWaveView);
        leadIIWaveView = (SurfaceView) findViewById(R.id.leadIIWaveView);
        leadIIIWaveView = (SurfaceView) findViewById(R.id.leadIIIWaveView);
        leadAVRWaveView = (SurfaceView) findViewById(R.id.leadAVRWaveView);
        leadAVLWaveView = (SurfaceView) findViewById(R.id.leadAVLWaveView);
        leadAVFWaveView = (SurfaceView) findViewById(R.id.leadAVFWaveView);

        leadIHolder = leadIWaveView.getHolder();
        leadIIHolder = leadIIWaveView.getHolder();
        leadIIIHolder = leadIIIWaveView.getHolder();
        leadAVRHolder = leadAVRWaveView.getHolder();
        leadAVLHolder = leadAVLWaveView.getHolder();
        leadAVFHolder = leadAVFWaveView.getHolder();
 //       coordinateY = leadIWaveView.getHeight() / 2;

        leadITempBuffer = new ArrayList<>();
        leadIITempBuffer = new ArrayList<>();
        leadIIITempBuffer = new ArrayList<>();

        findViewById(R.id.scalePlus).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        threshold_filter += 30;
                        if (threshold_filter > 1000)
                            threshold_filter = 1000;
                        Toast.makeText(MainActivity.this,"放大倍数："+String.valueOf(threshold_filter),Toast.LENGTH_SHORT).show();
                    }
                }
        );
        findViewById(R.id.scaleMinus).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        threshold_filter -= 30;
                        if (threshold_filter < 1)
                            threshold_filter = 1;
                        Toast.makeText(MainActivity.this,"放大倍数："+String.valueOf(threshold_filter),Toast.LENGTH_SHORT).show();
                    }
                }
        );
        findViewById(R.id.resetEmp).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        threshold_filter = 10;
                        Toast.makeText(MainActivity.this,"放大倍数："+String.valueOf(threshold_filter),Toast.LENGTH_SHORT).show();
                    }
                }
        );

        leadIHolder.addCallback(
                new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        Canvas c = holder.lockCanvas();
                        Paint p  = new Paint();
                        c.drawColor(Color.WHITE);
                        holder.unlockCanvasAndPost(c);
                        Log.d(TAG, "surfaceCreated");
                        Toast.makeText(MainActivity.this,"surfaceCreated", Toast.LENGTH_LONG).show();
                        SurfaceFlag = true;
                        paint.setColor(Color.WHITE);
                        ////////////////////////////////////////////更新示波器的线程

                        if (waveThread == null){
                            waveThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(true){
                                        synchronized (leadITempBuffer){
                                            synchronized (leadIITempBuffer){
                                                if(leadITempBuffer.isEmpty())
                                                    continue;
                                                leadIBuf = new ArrayList<>(leadITempBuffer);
                                                leadITempBuffer.clear();

                                                if(leadIITempBuffer.isEmpty())
                                                    continue;
                                                leadIIBuf = new ArrayList<>(leadIITempBuffer);
                                                leadIITempBuffer.clear();
                                            }
                                        }

                                        //Log.d(TAG, String.valueOf(leadIBuf.size()));
                                        Canvas canvas = leadIHolder.lockCanvas(
                                                new Rect(
                                                        coordinateX,
                                                        0,
                                                        coordinateX + leadIBuf.size() * scale,
                                                        leadIWaveView.getHeight()
                                                ));
                                        Canvas canvas2 = leadIIHolder.lockCanvas(
                                                new Rect(
                                                        coordinateX,
                                                        0,
                                                        coordinateX + leadIIBuf.size() * scale,
                                                        leadIIWaveView.getHeight()
                                                ));
                                        Canvas canvas3 = leadIIIHolder.lockCanvas(
                                                new Rect(
                                                        coordinateX,
                                                        0,
                                                        coordinateX + leadIIBuf.size() * scale,
                                                        leadIIIWaveView.getHeight()
                                                ));
                                        Canvas canvas4 = leadAVRHolder.lockCanvas(
                                                new Rect(
                                                        coordinateX,
                                                        0,
                                                        coordinateX + leadIIBuf.size() * scale,
                                                        leadAVRWaveView.getHeight()
                                                ));
                                        Canvas canvas5 = leadAVLHolder.lockCanvas(
                                                new Rect(
                                                        coordinateX,
                                                        0,
                                                        coordinateX + leadIIBuf.size() * scale,
                                                        leadAVLWaveView.getHeight()
                                                ));
                                        Canvas canvas6 = leadAVFHolder.lockCanvas(
                                                new Rect(
                                                        coordinateX,
                                                        0,
                                                        coordinateX + leadIIBuf.size() * scale,
                                                        leadAVFWaveView.getHeight()
                                                ));

                                        canvas.scale(1,(float)0.2);


                                        //相当于清空画布，画上黑色。
                                        canvas.drawColor(Color.BLACK);
                                        canvas2.drawColor(Color.BLACK);
                                        canvas3.drawColor(Color.BLACK);
                                        canvas4.drawColor(Color.BLACK);
                                        canvas5.drawColor(Color.BLACK);
                                        canvas6.drawColor(Color.BLACK);



                                        for (int i = 0;i < leadIBuf.size();i++){
                                            //Log.d("leadIvalue",String.valueOf(leadIBuf.get(i) * parameter()) +"  "+ String.valueOf(leadIBuf.get(i)));

                                            paint.setColor(Color.WHITE);
                                            //要更改的数据在这里
                                            canvas.drawLine(
                                                    oldX,
                                                    oldY,
                                                    coordinateX + (i + 1) * scale,
                                                    offset-(int)(leadIBuf.get(i)  * parameter()) ,
                                                    paint
                                            );
                                            paint.setColor(Color.RED);
                                            canvas.drawLine(
                                                    oldX,
                                                    coordinateY,
                                                    coordinateX + (i + 1) * scale,
                                                    coordinateY,
                                                    paint
                                            );
                                            paint.setColor(Color.WHITE);
                                            canvas2.drawLine(
                                                    oldX,
                                                    oldYleadII,
                                                    coordinateX + (i + 1) * scale,
                                                    offset-(int)(leadIIBuf.get(i) * parameter()) ,
                                                    paint
                                            );
                                            paint.setColor(Color.RED);
                                            canvas2.drawLine(
                                                    oldX,
                                                    coordinateY,
                                                    coordinateX + (i + 1) * scale,
                                                    coordinateY,
                                                    paint
                                            );

                                            paint.setColor(Color.WHITE);
                                            canvas3.drawLine(
                                                    oldX,
                                                    oldYleadIII,
                                                    coordinateX + (i + 1) * scale,
                                                    125-(int)((leadIIBuf.get(i)-leadIBuf.get(i)) * parameter()) ,
                                                    paint
                                            );
                                            paint.setColor(Color.RED);
                                            canvas3.drawLine(
                                                    oldX,
                                                    coordinateY,
                                                    coordinateX + (i + 1) * scale,
                                                    coordinateY,
                                                    paint
                                            );

                                            paint.setColor(Color.WHITE);
                                            canvas4.drawLine(
                                                    oldX,
                                                    oldYleadAVR,
                                                    coordinateX + (i + 1) * scale,
                                                    0-(int)((-0.5*(leadIIBuf.get(i)+leadIBuf.get(i))) * parameter()) ,
                                                    paint
                                            );
                                            paint.setColor(Color.RED);
                                            canvas4.drawLine(
                                                    oldX,
                                                    coordinateY,
                                                    coordinateX + (i + 1) * scale,
                                                    coordinateY,
                                                    paint
                                            );

                                            paint.setColor(Color.WHITE);
                                            canvas5.drawLine(
                                                    oldX,
                                                    oldYleadAVL,
                                                    coordinateX + (i + 1) * scale,
                                                    180-(int)((leadIBuf.get(i) - 0.5 * leadIIBuf.get(i)) * parameter()) ,
                                                    paint
                                            );
                                            paint.setColor(Color.RED);
                                            canvas5.drawLine(
                                                    oldX,
                                                    coordinateY,
                                                    coordinateX + (i + 1) * scale,
                                                    coordinateY,
                                                    paint
                                            );

                                            paint.setColor(Color.WHITE);
                                            canvas6.drawLine(
                                                    oldX,
                                                    oldYleadAVF,
                                                    coordinateX + (i + 1) * scale,
                                                    180-(int)((-0.5*leadIBuf.get(i) + leadIIBuf.get(i)) * parameter()) ,
                                                    paint
                                            );
                                            paint.setColor(Color.RED);
                                            canvas6.drawLine(
                                                    oldX,
                                                    coordinateY,
                                                    coordinateX + (i + 1) * scale,
                                                    coordinateY,
                                                    paint
                                            );

//                                        Log.d(
//                                                TAG,
//                                                String.valueOf(oldX) + " " +
//                                                        String.valueOf(oldY) + " " +
//                                                        String.valueOf(coordinateX + i * scale) + " " +
//                                                        String.valueOf(leadIBuf.get(i) * maxValue / 0x7FFFFF + coordinateY)
//                                        );
                                            oldX = coordinateX + (i + 1) * scale;
                                            oldY = offset-(int)(leadIBuf.get(i) * parameter());
                                            oldYleadII = offset-(int)(leadIIBuf.get(i) * parameter());
                                            oldYleadIII = 125-(int)((leadIIBuf.get(i)-leadIBuf.get(i)) * parameter());
                                            oldYleadAVR = 0-(int)(-0.5*(leadIIBuf.get(i)+leadIBuf.get(i)) * parameter());
                                            oldYleadAVL = 180-(int)((leadIBuf.get(i) - 0.5 * leadIIBuf.get(i)) * parameter());
                                            oldYleadAVF = 180-(int)((-0.5*leadIBuf.get(i) + leadIIBuf.get(i)) * parameter());
//                                        Log.d(TAG,String.valueOf(oldY) + " "
//                                                + String.valueOf(oldYleadII) + ' '
//                                                + String.valueOf(oldYleadIII) + ' '
//                                                + String.valueOf(oldYleadAVR) + ' '
//                                                + String.valueOf(oldYleadAVL) + ' '
//                                                + String.valueOf(oldYleadAVF));

                                        }
                                        leadIHolder.unlockCanvasAndPost(canvas);
                                        leadIIHolder.unlockCanvasAndPost(canvas2);
                                        leadIIIHolder.unlockCanvasAndPost(canvas3);
                                        leadAVRHolder.unlockCanvasAndPost(canvas4);
                                        leadAVLHolder.unlockCanvasAndPost(canvas5);
                                        leadAVFHolder.unlockCanvasAndPost(canvas6);
                                        coordinateX += leadIBuf.size() * scale;

                                        if (coordinateX > leadIIWaveView.getWidth()){
                                            coordinateX = 0;
                                            oldX = 0;
                                            oldY = coordinateY;
                                        }
                                        try{
                                            Thread.sleep(50);
                                        }catch (Exception e){
                                            Log.d(
                                                    TAG,
                                                    "sleep error happened"
                                            );
                                        }

                                    }

                                }
                            });
                            waveThread.start();
                        } else {
                            waveThread.start();
                        }


                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                        Log.d(TAG, "surfaceChanged");
                        Toast.makeText(MainActivity.this,"surfaceChanged", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        Toast.makeText(MainActivity.this,"surfaceDestroyed", Toast.LENGTH_LONG).show();
                        SurfaceFlag = false;
                    }
                }
        );
        leadIIHolder.addCallback(
                new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {

                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {

                    }
                }
        );

        leadIIIHolder.addCallback(
                new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {

                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {

                    }
                }
        );


        HEIGHT = leadIWaveView.getHeight();
        WIDTH = leadIWaveView.getWidth();
        findViewById(R.id.sendData).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (inputView.getText().toString().equals("input")){
                            Toast.makeText(MainActivity.this,"并没有输入什么东西", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mBluetoothLeService.txxx(inputView.getText().toString());


                    }
                }
        );




//注册一个广播接收者來获取到扫描的蓝牙设备

        registerReceiver(mReceiver, makeGattUpdateIntentFilter());
    }

    public interface BluetoothData {

        /*发送指令函数，参数为指令信息，发送成功返回true,否则返回false*/
        public boolean SendMessage(String str);

        /*从蓝牙端获取信息函数,返回的信息为获取到的字符窜*/
        public String getMessage();
    }

    private void OpenBluetooth(BluetoothAdapter myBluetoothAdapter)
    {
        //本地蓝牙启动
        if(myBluetoothAdapter == null)
        {
            Toast.makeText(this,"蓝牙不可用", Toast.LENGTH_LONG).show();
            //finish();
            return;
        }
        if(!myBluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //判断是不是启动蓝牙的结果
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                //成功
                Toast.makeText(this, "蓝牙开启成功...", Toast.LENGTH_SHORT).show();

            } else {
                //失败
                Toast.makeText(this, "蓝牙开启失败...", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            //Log.d(TAG,action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(MainActivity.this,device.getName() + device.getAddress(), Toast.LENGTH_LONG).show();
            }
            else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)){
                String str = intent.getStringExtra(BluetoothService.EXTRA_DATA);

                //Log.d("broadcastReceiver",str);
                try{
//                    Log.d(TAG,String.valueOf(str.length()));
//                    Log.d(TAG,str);

                    leadIValue = Integer.parseInt(str.substring(6,12),16);
                    leadIIValue = Integer.parseInt(str.substring(12,18),16);
//                    leadIIValue = Integer.parseInt(str.substring(6,12),16);
//                    leadIValue = Integer.parseInt(str.substring(0,6),16);

                } catch (Exception e){
                    e.printStackTrace();

                    leadIValue = 0;
                    leadIIValue = 0;
                }


                if (leadIValue > 0x7fffff){
                    leadIValue = -(0xffffff - leadIValue + 1);
                }
                if (leadIIValue > 0x7fffff){
                    leadIIValue = -(0xffffff - leadIIValue + 1);
                }
//                Log.d("leadvalues before",String.valueOf(leadIValue) + "   " + String.valueOf(leadIIValue));
//                if ((leadIValue & 0x800000) > 0){
////                    leadIValue = leadIValue - 0xffffff - 1;
//                    leadIValue = -(leadIValue ^ 0xffffff) - 1;
//                    Log.d("fuck",String.valueOf(leadIValue));
//                }
//                if ((leadIIValue & 0x800000) > 0){
//                    leadIIValue = -(leadIIValue ^ 0xffffff) - 1;
//                    Log.d("fuck again",String.valueOf(leadIIValue));
//                }

//                leadIValue = sharpWaveFilter(filter_list_leadI,leadIValue);
//                leadIIValue = sharpWaveFilter(filter_list_leadII,leadIIValue);

//                leadIValue = midValueFilter(value_leadI,leadIValue);
//                leadIIValue = midValueFilter(value_leadII,leadIIValue);



                Double a = midValueFilter(value_leadI,sharpWaveFilter(filter_list_leadI,leadIValue));
                Double b = midValueFilter(value_leadII,sharpWaveFilter(filter_list_leadII,leadIIValue));
                leadIValue = a.intValue();
                leadIIValue = b.intValue();

                byte[] strbyte1 = String.valueOf(a).getBytes();
                byte[] strbyte2 = String.valueOf(b).getBytes();
                try{
                    globleFout.write(strbyte1);
                    globleFout.write(" ".getBytes());
                    globleFout.write(strbyte2);
                    globleFout.write("\n".getBytes());
                } catch (Exception e){
                    e.printStackTrace();
                }



                Log.d("leadvalues after",String.valueOf(leadIValue) + "   " + String.valueOf(leadIIValue));


                synchronized (leadITempBuffer){
                    synchronized (leadIITempBuffer){
                        leadITempBuffer.add(leadIValue); //添加到buffer里面
                        leadIITempBuffer.add(leadIIValue); //添加到buffer里面.
                    }
                }


            }
            //signal for test;
            else if (action.equals(BluetoothService.ACTION_DIGIT_TEST)){
                String str = intent.getStringExtra(BluetoothService.EXTRA_DATA);
                Log.d("test","signal");
                //Log.d("broadcastReceiver",str);
                try{
//                    Log.d(TAG,String.valueOf(str.length()));
//                    Log.d(TAG,str);

                    leadIValue = Integer.parseInt(str.substring(6,12),16);
                    leadIIValue = Integer.parseInt(str.substring(12,18),16);
//                    leadIIValue = Integer.parseInt(str.substring(6,12),16);
//                    leadIValue = Integer.parseInt(str.substring(0,6),16);

                } catch (Exception e){
                    e.printStackTrace();

                    leadIValue = 0;
                    leadIIValue = 0;
                }


                if (leadIValue > 0x7fffff){
                    leadIValue = -(0xffffff - leadIValue + 1);
                }
                if (leadIIValue > 0x7fffff){
                    leadIIValue = -(0xffffff - leadIIValue + 1);
                }



                //leadIValue = midValueFilter(value_leadI,sharpWaveFilter(filter_list_leadI,leadIValue));
                //leadIIValue = midValueFilter(value_leadII,sharpWaveFilter(filter_list_leadII,leadIIValue));

                byte[] strbyte1 = String.valueOf(leadIValue).getBytes();
                byte[] strbyte2 = String.valueOf(leadIIValue).getBytes();
                try{
                    globleFout.write(strbyte1);
                    globleFout.write(" ".getBytes());
                    globleFout.write(strbyte2);
                    globleFout.write("\n".getBytes());
                } catch (Exception e){
                    e.printStackTrace();
                }
//                Log.d("leadvalues after",String.valueOf(leadIValue) + "   " + String.valueOf(leadIIValue));


                synchronized (leadITempBuffer){
                    synchronized (leadIITempBuffer){
                        leadITempBuffer.add(leadIValue); //添加到buffer里面
                        leadIITempBuffer.add(leadIIValue); //添加到buffer里面.
                    }
                }
            }

            else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                Log.i("receiver",action);
                mBluetoothLeService.enable_JDY_ble(true);
            }
            else if (BluetoothService.ACTION_DIGIT_SEND_COMPLETE.equals(action)){
                Toast.makeText(MainActivity.this,"Send complete", Toast.LENGTH_LONG).show();
            }
            //else if ()
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothService.ACTION_DIGIT_SEND_COMPLETE);
        intentFilter.addAction(BluetoothService.ACTION_DIGIT_TEST);
        return intentFilter;
    }

    private void drawBack(final SurfaceHolder holder){

        new Thread(new Runnable() {
            final SurfaceHolder hd = holder;
            @Override
            public void run() {
                Canvas canvas = hd.lockCanvas();
                canvas.drawColor(Color.YELLOW);
                hd.unlockCanvasAndPost(canvas);
            }
        }).start();
    }

    private double parameter(){
        return 1;
//        return maxValue * 125 * amplifier / divider;
    }

    private int strHex2Int(String hex){
        int result = 0;
        //我们从末尾开始算起吧。



        return result;
    }

    /*
    private Integer midValueFilter(PriorityQueue<Integer> min_que, PriorityQueue<Integer> max_que, Integer new_value, LinkedList<Integer> value_list){
        Integer result = 0;

        if (value_list.size() < window_size - 1){
            value_list.add(new_value);
        }
        return result;
    }
    */

    private Double midValueFilter(LinkedList<Double> value_list,Double new_value){
        if (value_list.size() < window_size - 1){
            value_list.add(new_value);
            return new Double(0);
        }
        value_list.add(new_value);
        LinkedList<Double> temp_list = new LinkedList<>(value_list);
        Collections.sort(temp_list);
        value_list.remove(0);
        return value_list.get(window_size / 2) - temp_list.get(window_size / 2);
    }

    private Double sharpWaveFilter(ArrayList<Double> arr_list,Integer new_value){

        Double value = new_value * 2.4 / divider;



        //new_value = (new_value * 5000 * amplifier) / divider;




//        Log.d(TAG,String.valueOf(new_value));
        if (arr_list.size() < 2){
            arr_list.add(value);
            return new Double(0);
        }
        Double left_delta = Math.abs(arr_list.get(1) - arr_list.get(0));
        Double right_delta = Math.abs(new_value  - arr_list.get(1));


        //threshold_filter

        if (left_delta > 0.005 && right_delta > 0.005){
            Double mid_value = (arr_list.get(0) + value) / 2;
            arr_list.set(0,mid_value);
            arr_list.set(1,value);
            Log.d(TAG + " value",String.valueOf(arr_list.get(0)));
            return mid_value;
        }
        arr_list.set(0,arr_list.get(1));
        arr_list.set(1,value);

        Log.d(TAG + " value",String.valueOf(arr_list.get(0)));
        return arr_list.get(0);
    }
}

