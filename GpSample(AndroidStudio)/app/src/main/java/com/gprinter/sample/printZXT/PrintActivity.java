package com.gprinter.sample.printZXT;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smallchart.chart.LineChart;
import com.example.smallchart.data.LineData;
import com.example.smallchart.interfaces.iData.ILineData;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.GpDevice;
import com.gprinter.sample.PrinterConnectDialog;
import com.gprinter.service.GpPrintService;
import com.sample.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

public class PrintActivity extends Activity implements View.OnClickListener {
    /**
     * 保存
     */
    private TextView mSave;
    /**
     * 去连接打印机
     */
    private TextView mOpen;
    /**
     * 裁剪区域
     */
    private LinearLayout mSaveArea;
    /**
     * 顶部裁剪坐标
     */
    private int mCutTop;
    /**
     * 左侧裁剪坐标
     */
    private int mCutLeft;
    /**
     * 截图成功后显示的控件
     */
    private ImageView mPicGet;
    private FrameLayout mFL;
    /**
     * 绘图区高度
     */
    private int mPicGetHeight;
    /**
     * 绘图区宽度
     */
    private int mPicGetWidth;
    /**
     * 最后的截图
     */
    private Bitmap saveBitmap;
    private FrameLayout mTotal;
    /**
     * 待裁剪区域的绝对坐标
     */
    private int[] mSavePositions = new int[2];
    /**
     * 成功动画handler
     */
    private SuccessHandler successHandler;
    /**
     * 恢复初始化handler
     */
    private InitHandler initHandler;
    String path = null;
    /**
     * 折线图的数据
     */
    private LineData mLineData = new LineData();
    private ArrayList<ILineData> mDataList = new ArrayList<>();
    private ArrayList<PointF> mPointArrayList = new ArrayList<>();
    protected float[][] points = new float[][]{{1, 0.50f}, {2, 0.70f}, {3, 0.60f}, {4, 0.90f}, {5, 0.30f}, {6, 0.20f}, {7, 0.90f}, {8, 0.60f}, {9, 0.30f}, {10, 0.70f},
            {11, 0.60f}, {12, 0.40f}, {13, 0.10f}, {14, 0.30f}, {15, 0.80f}, {16, 0.50f}, {17, 0.30f}, {18, 0.70f}, {19, 0.90f}, {20, 0.40f}};
    /**
     * 后加的全局变量
     */
    private GpService mGpService = null;
    private PrinterServiceConnection conn = null;
    private int mPrinterIndex = 0;
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private static final int REQUEST_PRINT_RECEIPT = 0xfc;
    public static final String CONNECT_STATUS = "connect.status";
    private PrinterHandler printHandler;
    BluetoothAdapter bluetoothAdapter;
    private final String TAG=getClass().getSimpleName();
    /**
     * 计数
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        initView();
        initData();
        LineChart lineChartx = (LineChart) findViewById(R.id.lineChartx);
        lineChartx.setDataList(mDataList);
        LineChart lineCharty = (LineChart) findViewById(R.id.lineCharty);
        lineCharty.setDataList(mDataList);
        LineChart lineChartz = (LineChart) findViewById(R.id.lineChartz);
        lineChartz.setDataList(mDataList);

//        initData2();
//        lineChart.setDataList(mDataList);
        connection();
        /**
         * 注册广播
         */
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS));
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_RECEIPT_RESPONSE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_LABEL_RESPONSE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));
        printHandler = new PrinterHandler();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkPersiossion();

//        cacheDirPath = getExternalCacheDir().getAbsolutePath();
//        Log.e("LBH", cacheDirPath);测试缓存文件夹
    }

    private void initView() {

        mSave = (TextView) findViewById(R.id.tv_save);
        mOpen = (TextView) findViewById(R.id.to_connect);
        mSaveArea = (LinearLayout) findViewById(R.id.ll_save_area);
        mPicGet = (ImageView) findViewById(R.id.iv_pic_get);
        mFL = (FrameLayout) findViewById(R.id.fl_pic);
        mTotal = (FrameLayout) findViewById(R.id.fl_total);
        mSave.setOnClickListener(this);
        mOpen.setOnClickListener(this);
        successHandler = new SuccessHandler();
        initHandler = new InitHandler();
    }

    /**
     * 获取截图的位置
     *
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            mSaveArea.getLocationOnScreen(mSavePositions);
            mCutLeft = mSavePositions[0];
            mCutTop = mSavePositions[1];
            mPicGetHeight = mTotal.getHeight();
            mPicGetWidth = mTotal.getWidth();
        }
    }

    /**
     * android 6.0动态权限获取模块
     */
    private void checkPersiossion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LBH", "大哥没有权限");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    /**
     * 点击按钮
     *
     * @param v
     */

    @Override
    public void onClick(View v) {
        if (bluetoothAdapter == null) {
            return;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            } else {
                // Toast.makeText(this,"蓝牙已经打开",Toast.LENGTH_LONG).show();
            }
        }
        switch (v.getId()) {
            case R.id.tv_save:
                mSave.setText("开始打印");
                mSave.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (path == null) {

                            Log.e("LBH", "screenshot");
                            screenshot();

                        } else {
                            Log.e("LBH", "goToPrint");
                            Looper.prepare();//如果不写就会报java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
                            goToPrint(path);
                            Looper.loop();//如果不写就会报java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
                        }

                    }
                }).start();
                break;
            case R.id.to_connect:
                Intent intentConnect = new Intent(PrintActivity.this, PrinterConnectDialog.class);
                boolean[] state = getConnectState();
                intentConnect.putExtra(CONNECT_STATUS, state);
                startActivity(intentConnect);
                break;
            default:
                Log.e(TAG,"DEFAULT");
                break;
        }
    }

    /**
     * 截图模块
     */
    private void screenshot() {
        Log.e("LBH", "225");
        // 获取屏幕
        View dView = getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        Bitmap bmp = dView.getDrawingCache();
        Log.e("LBH", "231");
        if (bmp != null) {
            try {
                //二次截图
                saveBitmap = Bitmap.createBitmap(mSaveArea.getWidth(), mSaveArea.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(saveBitmap);
                Paint paint = new Paint();
                canvas.drawBitmap(bmp, new Rect(mCutLeft, mCutTop, mCutLeft + mSaveArea.getWidth(), mCutTop + mSaveArea.getHeight()),
                        new Rect(0, 0, mSaveArea.getWidth(), mSaveArea.getHeight()), paint);

                File imageDir = new File(Constant.IMAGE_DIR);
                //File imageDir = new File(cacheDirPath);
                Log.e("LBH", "243");
                if (!imageDir.exists()) {
                    try {
                        imageDir.mkdir();
                    }catch (Exception e)
                    {
                        throw new IOException("unable to create file");
                    }

                    Log.e("LBH", "246");
                }
                Log.e("LBH", imageDir.exists() + "");
                String imageName = Constant.SCREEN_SHOT;
                File file = new File(imageDir, imageName);
                Log.e("LBh", "path=" + file.getPath());
                try {
                    if (file.exists()) {
                        try {
                            file.delete();
                        }catch (Exception e)
                        {
                            throw new IOException("this file is not exists");
                        }
                        Log.e("LBH", "252");
                    }
                    try{
                        file.createNewFile();
                    }catch (Exception e)
                    {
                        Log.e(TAG,"不能创建文件夹");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("LBH", e.getMessage());
                    Log.e("LBH", "258");
                }
                FileOutputStream os = new FileOutputStream(file);
                saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                Log.e("LBH", "261");
                //将截图保存至相册并广播通知系统刷新
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), imageName, null);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
                Log.e("LBH", Uri.fromFile(file).getPath());
                sendBroadcast(intent);
               // intent = new Intent(PrintActivity.this, MainActivity.class);
                path = Uri.fromFile(file).getPath();
//                /**
//                 * 目前可以打印3个图片一次
//                 */
//                String path=Uri.fromFile(file).getPath();
//                if (path!=null) {
//                    if (i < 3){
//                        list.add(path);
//                        Message message = new Message();
//                        message.what = SAVEZBZ;
//                        successHandler.sendMessage(message);
//                        i++;
//                    }else if (i==3) {
//                        intent.putStringArrayListExtra("imagepath",list);
//                        Message message = new Message();
//                        message.what = SAVESUCCEED;
//                        successHandler.sendMessage(message);
//                        startActivity(intent);
//                    }else
//                    {
//                        return;
//                    }
//                }
//                else
//                {
//                    Message message=new Message();
//                    message.what=SAVEFAILED;
//                    successHandler.sendMessage(message);
//                }
//                intent.putExtra("imagepath", path);
//                startActivity(intent);

                successHandler.sendMessage(Message.obtain());

            } catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        } else {
            initHandler.sendMessage(Message.obtain());
        }

    }

    /**
     * 打印模块
     *
     * @param path 打印图片的路径
     */
    private void goToPrint(String path) {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
        esc.addText("Sample\n"); // 打印文字
        esc.addPrintAndLineFeed();

		/* 打印文字 */
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
        Log.e("LBH", "打印的path=" + path);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        esc.addRastBitImage(bitmap, 384, 0);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText("Completed!\r\n"); // 打印结束
        printHandler.sendEmptyMessage(0);
        Log.e("LBH", "打印完毕");
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        esc.addPrintAndFeedLines((byte) 8);

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;
        try {
            rs = mGpService.sendEscCommand(mPrinterIndex, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();

            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private class PrinterHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.e("LBH", "打印完毕////");
                    mSave.setEnabled(true);
                    break;
                default:
                    Log.e(TAG,"printerhandler");
                    break;
            }
        }
    }

    /**
     * 成功动画handler
     */
    private class SuccessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            showSuccessNew();
//            switch (msg.what)
//            {
//                case SAVESUCCEED:
//                    showSuccessNew();
//                    break;
//                case  SAVEFAILED:
//                    Toast.makeText(PrintActivity.this,"图片不存在",Toast.LENGTH_LONG).show();
//                    return;
//                case SAVEZBZ:
//                    showSuccessNew();
//                    break;
//            }
        }
    }

    /**
     * 恢复初始化handler
     */
    private class InitHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            ToastUtils.show(PrintActivity.this, "存储失败");
            mSave.setEnabled(true);
            mSave.setText("开始打印");
        }
    }

    /**
     * 截图成功后显示动画
     */
    private void showSuccessNew() {
        ToastUtils.show(PrintActivity.this, "保存成功");
        mPicGet.setImageBitmap(saveBitmap);

        ObjectAnimator paramsAnimator = ObjectAnimator.ofFloat(new Wrapper(mPicGet), "params", 1f, 0.7f);
        paramsAnimator.setDuration(800);
        paramsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFL.setVisibility(View.VISIBLE);
                mPicGet.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPicGet.setVisibility(View.GONE);
                        mFL.setVisibility(View.GONE);
                        mSave.setEnabled(true);
                        mSave.setText("开始打印");
                    }
                }, 500);
            }
        });
        paramsAnimator.start();
        try {
            int type = mGpService.getPrinterCommandType(mPrinterIndex);
            if (type == GpCom.ESC_COMMAND) {
                mGpService.queryPrinterStatus(mPrinterIndex, 1000, REQUEST_PRINT_RECEIPT);
            } else {
                Toast.makeText(this, "Printer is not receipt mode", Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 包装类
     */
    class Wrapper {
        private View mTarget;

        private Wrapper(View mTarget) {
            this.mTarget = mTarget;
        }

        private float getParams() {
            ViewGroup.LayoutParams lp = mTarget.getLayoutParams();
            return lp.height / mPicGetHeight;
        }

        private void setParams(float params) {
            ViewGroup.LayoutParams lp = mTarget.getLayoutParams();
            lp.height = (int) (mPicGetHeight * params);
            lp.width = (int) (mPicGetWidth * params);
            mTarget.requestLayout();
        }
    }

    /**
     * 获取数据
     */
    private void initData() {
        for (int i = 0; i < points.length - 1; i++) {
            mPointArrayList.add(new PointF(points[i][0], points[i][1]));
        }
        mLineData.setValue(mPointArrayList);
        mLineData.setColor(Color.MAGENTA);
        mLineData.setPaintWidth(pxTodp(1f));
        mLineData.setTextSize(pxTodp(0.5f));
        mDataList.add(mLineData);

    }

    protected float pxTodp(float value) {
        DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float valueDP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
        return valueDP;
    }

    /**
     * 广播接收器，接收打印机的广播
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("TAG", action);
            // GpCom.ACTION_DEVICE_REAL_STATUS 为广播的IntentFilter
            if (action.equals(GpCom.ACTION_DEVICE_REAL_STATUS)) {

                // 业务逻辑的请求码，对应哪里查询做什么操作
                int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
                // 判断请求码，是则进行业务操作
                if (requestCode == MAIN_QUERY_PRINTER_STATUS) {

                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    String str;
                    if (status == GpCom.STATE_NO_ERR) {
                        str = "打印机正常";
                    } else {
                        str = "打印机 ";
                        if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {
                            str += "脱机";
                        }
                        if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {
                            str += "缺纸";
                        }
                        if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {
                            str += "打印机开盖";
                        }
                        if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {
                            str += "打印机出错";
                        }
                        if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {
                            str += "查询超时";
                        }
                    }
                    Toast.makeText(getApplicationContext(), "打印机：" + mPrinterIndex + " 状态：" + str, Toast.LENGTH_SHORT)
                            .show();
                } else if (requestCode == REQUEST_PRINT_RECEIPT) {
                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    if (status == GpCom.STATE_NO_ERR) {
                        goToPrint(path);
                    } else {
                        final AlertDialog.Builder warnDialog =
                                new AlertDialog.Builder(PrintActivity.this);
                        warnDialog.setIcon(R.drawable.ic_printer);
                        warnDialog.setTitle(R.string.friendlyreminder);
                        warnDialog.setMessage(R.string.toOpenbluetoothOrNot);
                        warnDialog.setPositiveButton(R.string.toOpen, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intentConnect = new Intent(PrintActivity.this, PrinterConnectDialog.class);
                                boolean[] state = getConnectState();
                                intentConnect.putExtra(CONNECT_STATUS, state);
                                startActivity(intentConnect);
                            }
                        });
                        warnDialog.setNegativeButton(R.string.waitAmoment, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        warnDialog.create().show();

                    }
                }
            }
        }
    };

    /**
     * 建立远程连接
     */
    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("ServiceConnection", "onServiceDisconnected() called");
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = GpService.Stub.asInterface(service);
        }
    }

    /**
     * 将service和activity进行绑定
     */
    private void connection() {
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService
    }

    /**
     * 获取连接打印机的状态
     *
     * @return
     */
    public boolean[] getConnectState() {
        boolean[] state = new boolean[GpPrintService.MAX_PRINTER_CNT];
        for (int i = 0; i < GpPrintService.MAX_PRINTER_CNT; i++) {
            state[i] = false;
        }
        for (int i = 0; i < GpPrintService.MAX_PRINTER_CNT; i++) {
            try {
                if (mGpService.getPrinterConnectStatus(i) == GpDevice.STATE_CONNECTED) {
                    state[i] = true;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return state;
    }

    @Override
    protected void onDestroy() {
        Log.e("LBH","onDestory");
        successHandler = null;
        printHandler = null;
        super.onDestroy();
    }
}
