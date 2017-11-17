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
import android.content.SharedPreferences;
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

import com.example.smallchart.interfaces.iData.ILineData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
import java.util.List;
import java.util.Vector;

import static android.R.attr.path;

public class PrintActivity extends Activity implements View.OnClickListener {
    /**
     * 保存
     */
    private TextView mSave;
    /**
     * 折线图大哥大
     */
    private LineChart mLineChartPerssure01;
    private LineChart mLineChartPerssure02;
    private LineChart mLineChartPerssure03;
    /**
     * 大哥大数据
     */
    private  List<Float> listIntX = new ArrayList<>();
    private List<Float> listIntY = new ArrayList<>();
    private  List<Float> listIntZ = new ArrayList<>();
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
    private int lCutTop;
    /**
     * 左侧裁剪坐标
     */
    private int mCutLeft;
    private int lCutLeft;
    /**
     * 截图成功后显示的控件
     */
    private ImageView mPicGet;
    private FrameLayout mFL;
    /**
     * 绘图区高度
     */
    private int mPicGetHeight;
    private int leanPicGetHeight;
    /**
     * 绘图区宽度
     */
    private int mPicGetWidth;
    private int leanPicGetWidth;
    /**
     * 最后的截图
     */
    private Bitmap saveBitmap;
    private FrameLayout mTotal;
    /**
     * 待裁剪区域的绝对坐标
     */
    private int[] mSavePositions = new int[2];
    private int[] leanSavePositions = new int[2];
    /**
     * 成功动画handler
     */
    private SuccessHandler successHandler;
    /**
     * 恢复初始化handler
     */
    private InitHandler initHandler;
    String leanpath = null;
    String presspath=null;
    /**
     * 折线图的数据
     */
    private LineData mLineData = new LineData();
    private ArrayList<ILineData> mDataList = new ArrayList<>();
    private ArrayList<PointF> mPointArrayList = new ArrayList<>();
    protected float[][] points = new float[][]{{1, 0.5f}, {2, 0.70f}, {3, 0.6f}, {4, 0.9f}, {5, 0.3f}, {6, 0.2f}, {7, 0.9f}, {8, 0.6f}, {9, 0.3f}, {10, 0.7f},
            {11, 0.6f}, {12, 0.4f}, {13, 0.1f}, {14, 0.3f}, {15, 0.8f}, {16, 0.5f}, {17, 0.3f}, {18, 0.7f}, {19, 0.9f}, {20, 0.4f}};
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
    private final String PATHNAME="com.gprinter.sample.printeractivity.pathname";
    private SharedPreferences sharedPreferences=null;
    private SharedPreferences.Editor editor;
    private boolean flag=false;
    private LinearLayout press;
    private LinearLayout leanangle;
    private Bitmap leanbitmap;
    private Bitmap bmp;
    private Bitmap savebmp;
    private Bitmap saveLean;
    /**
     * 计数
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        initView();
        initData();

       // lineChartpy.setDataList(mDataList);
        connection();
        drawingLine();
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
        sharedPreferences=getSharedPreferences(PATHNAME,Context.MODE_PRIVATE);
        editor=sharedPreferences.edit();
//        cacheDirPath = getExternalCacheDir().getAbsolutePath();
//        Log.e("LBH", cacheDirPath);测试缓存文件夹
        Log.e("LBH","走你妹啊");

    }

    private void initView() {
        mSave = (TextView) findViewById(R.id.tv_save);
        mOpen = (TextView) findViewById(R.id.to_connect);
        mSaveArea = (LinearLayout) findViewById(R.id.ll_save_area);
        mLineChartPerssure01 = (LineChart) findViewById(R.id.lineChart_perssure01);
        mLineChartPerssure02 = (LineChart) findViewById(R.id.lineChart_perssure02);
        mLineChartPerssure03 = (LineChart) findViewById(R.id.lineChart_perssure03);
        mTotal = (FrameLayout) findViewById(R.id.fl_total);
        press= (LinearLayout) findViewById(R.id.press);
        leanangle= (LinearLayout) findViewById(R.id.leanangle);
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
            press.getLocationOnScreen(mSavePositions);
            mCutLeft = mSavePositions[0];
            mCutTop = mSavePositions[1];
            mPicGetHeight = mTotal.getHeight();
            mPicGetWidth = mTotal.getWidth();
            leanangle.getLocationInWindow(leanSavePositions);
            lCutLeft=leanSavePositions[0];
            lCutTop=leanSavePositions[1];
            leanPicGetHeight=mTotal.getHeight();
            leanPicGetWidth=mTotal.getWidth();

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
//        Log.e("LBH","onResum外:path="+path);
//        Log.e("LBH","onResum外:flag="+flag);
//        if (path==null&&flag)
//        {
//            path=sharedPreferences.getString("path",null);
//            Log.e("LBH","onResum内:path="+path);
//        }

    }
    @Override
    protected void onPause() {
        super.onPause();
//        Log.e("LBH","onpause:path="+path);
//        flag=true;
//        if (flag) {
//            editor.putString("path", path);
//            editor.commit();
//            path=null;
//        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e("LBH","onRestart");
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
            }
        }
        switch (v.getId()) {
            case R.id.tv_save:
                mSave.setText("开始打印");
                mSave.setEnabled(true);
                MyExcutorManager.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {

                            Log.e("LBH","screenshot="+path);
                            Log.e("LBH", "screenshot");
                            screenshot();
                            screenShotLeanAngle();
                            Log.e("LBH", "goToPrint");
                            Looper.prepare();//如果不写就会报java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
                            goToPrint();
                            Looper.loop();//如果不写就会报java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()

                    }
                });
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
    private void screenShotLeanAngle()
    {
        View dView = getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        leanbitmap=dView.getDrawingCache();
        if (leanbitmap!=null)
        {

            try {
                //二次截图
                saveLean=Bitmap.createBitmap(leanangle.getWidth(), leanangle.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(saveLean);
                Paint paint = new Paint();
                canvas.drawBitmap(leanbitmap, new Rect(lCutLeft, lCutTop, lCutLeft + leanangle.getWidth(), lCutTop + leanangle.getHeight()),
                        new Rect(0, 0, leanangle.getWidth(), leanangle.getHeight()), paint);//画的时候是有动画的效果

                File imageDir = new File(Constant.IMAGE_DIR);
                //File imageDir = new File(cacheDirPath);
                if (!imageDir.exists()) {
                    try {
                        imageDir.mkdir();
                    }catch (Exception e)
                    {
                        throw new IOException("unable to create file");
                    }
                }
                Log.e("LBH", imageDir.exists() + "");
                String imageName = Constant.SCREEN_lean;
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
                saveLean.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                Log.e("LBH", "261");
                //将截图保存至相册并广播通知系统刷新
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), imageName, null);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
                Log.e("LBH", Uri.fromFile(file).getPath());
                sendBroadcast(intent);
                // intent = new Intent(PrintActivity.this, MainActivity.class);
                leanpath = Uri.fromFile(file).getPath();
                successHandler.sendMessage(Message.obtain());

            } catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        } else {
            initHandler.sendMessage(Message.obtain());
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
         bmp = dView.getDrawingCache();
        Log.e("LBH", "231");
        if (bmp != null) {
            try {
                //二次截图
                savebmp = Bitmap.createBitmap(press.getWidth(), press.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(savebmp);
                Paint paint = new Paint();
                canvas.drawBitmap(bmp, new Rect(mCutLeft, mCutTop, mCutLeft + press.getWidth(), mCutTop + press.getHeight()),
                        new Rect(0, 0, press.getWidth(), press.getHeight()), paint);//画的时候是有动画的效果
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
                savebmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                Log.e("LBH", "261");
                //将截图保存至相册并广播通知系统刷新
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), imageName, null);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
                Log.e("LBH", Uri.fromFile(file).getPath());
                sendBroadcast(intent);
               // intent = new Intent(PrintActivity.this, MainActivity.class);
                presspath = Uri.fromFile(file).getPath();
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
    private void goToPrint() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
        esc.addText("Sample\n"); // 打印文字
        Log.e("LBH","size="+mDataList.size());
        for (int i=0;i<mDataList.size();i++)
        {
            Log.e("LBH",i+"="+mDataList.get(i));
//            esc.addText(mDataList.get(i)+"\n");
        }
        esc.addPrintAndLineFeed();

		/* 打印文字 */
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐

        Bitmap bitmap = BitmapFactory.decodeFile(presspath);
        Log.e("LBH","savebmp="+presspath);
        esc.addText("第一张\n");
        if (bitmap!=null)
        {
            esc.addRastBitImage(bitmap, 384, 0);
        }
        Log.e("LBH","saveLean="+leanpath);
        esc.addText("第二张\n");
        Bitmap bitmaplean=BitmapFactory.decodeFile(leanpath);
        if (bitmaplean!=null)
        {
            esc.addRastBitImage(bitmaplean, 384, 0);
        }
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText("Completed!\r\n"); // 打印结束
        printHandler.sendEmptyMessage(0);
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
            //showSuccessNew();
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
        paramsAnimator.setDuration(300);
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
                        goToPrint();
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
        editor.putString("path",null);
        editor.commit();
        editor=null;
        super.onDestroy();
    }
    private void drawingLine() {
        // 清空图形表
        mLineChartPerssure01.clear();
        mLineChartPerssure02.clear();
        mLineChartPerssure03.clear();
        if (listIntX.size() != 0) {
            // 设置线条数据
            generateDataLine(listIntX, null, null);
            showLine(mLineChartPerssure01, listIntX, null, null);
        }

        if (listIntY.size() != 0) {
            // 设置线条数据
            generateDataLine(null, listIntY, null);
            showLine(mLineChartPerssure02, null, listIntY, null);
        }

        if (listIntZ.size() != 0) {
            // 设置线条数据
            generateDataLine(null, null, listIntZ);
            showLine(mLineChartPerssure03, null, null, listIntZ);
        }
    }
    private void initData()
    {
        for (int i=0;i<60;i++)
        {
            listIntX.add(0.5f);
            listIntY.add(0.8f);
            listIntZ.add(0.3f);
        }
    }
    /**
     * 生成
     *
     * @param listX
     * @param listY
     * @return
     */
    private LineData generateDataLine(List<Float> listX, List<Float> listY, List<Float> listZ) {
        // X 线
        ArrayList<Entry> XValue = new ArrayList<>();

        if (listX == null) {
            listX = new ArrayList<>();
            listX.add(new Float(0.0));
        }
        for (int i = 0; i < listX.size(); i++) {
            XValue.add(new Entry(i, listX.get(i)));
        }
        LineDataSet xLine = new LineDataSet(XValue, "X");
        xLine.setAxisDependency(YAxis.AxisDependency.LEFT);
        xLine.setDrawValues(false);
        xLine.setLineWidth(4f);    //设置线的宽度
        xLine.setCircleSize(1f);   //设置小圆的大小
        xLine.setColor(Color.parseColor("#D0AA6C"));
        // Y 线
        ArrayList<Entry> YValue = new ArrayList<>();
        if (listY == null) {
            listY = new ArrayList<>();
            listY.add(new Float(0.0));
        }
        for (int i = 0; i < listY.size(); i++) {
            YValue.add(new Entry(i, listY.get(i)));
        }
        LineDataSet yLine = new LineDataSet(YValue, "Y");
        yLine.setAxisDependency(YAxis.AxisDependency.LEFT);
        yLine.setDrawValues(false);
        yLine.setLineWidth(4f);    //设置线的宽度
        yLine.setCircleSize(1f);   //设置小圆的大小
        yLine.setColor(Color.parseColor("#BF6A31"));
        // Z 线
        ArrayList<Entry> ZValue = new ArrayList<>();
        if (listZ == null) {
            listZ = new ArrayList<>();
            listZ.add(new Float(0.0));
        }
        for (int i = 0; i < listZ.size(); i++) {
            ZValue.add(new Entry(i, listZ.get(i)));
        }
        LineDataSet zLine = new LineDataSet(ZValue, "Z");
        zLine.setAxisDependency(YAxis.AxisDependency.LEFT);
        zLine.setDrawValues(false);//隐藏掉折线图上的值
        zLine.setLineWidth(4f);    //设置线的宽度
        zLine.setCircleSize(1f);   //设置小圆的大小
        zLine.setColor(Color.parseColor("#BF6A31"));
        LineData lineData = new LineData(xLine, yLine, zLine);
        return lineData;
    }


    /**
     * 显示线条
     *
     * @param listX
     * @param listY
     * @param listZ
     */
    private void showLine(LineChart lineChart, List<Float> listX, List<Float> listY, List<Float> listZ) {
        // 设置字体
        // Typeface fromAsset = Typeface.createFromAsset(mContext.getAssets(), "OpenSans-Regular.ttf");
        lineChart.setAlpha(0.8f);//设置透明度
        lineChart.setDrawBorders(false); //是否在折线图上添加边框

        lineChart.setBorderColor(Color.rgb(213, 216, 214));//边框颜色
        lineChart.setDrawGridBackground(false); // 是否显示表格颜色
        lineChart.setGridBackgroundColor(0); // 表格的的颜色
        lineChart.setHighlightPerDragEnabled(false);
        lineChart.setNoDataText("正在加载...");
        //设置点击chart图对应的数据弹出标注
        Legend l = lineChart.getLegend();
        l.setEnabled(false);//设置图表最下方的标注是否显示// 标签.例如: 移动  联通  电信
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //  xAxis.setTypeface(fromAsset);
        xAxis.setDrawLabels(false);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawLabels(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
//        xAxis.setValueFormatter(new IAxisValueFormatter() {
//            @Override
//            public String getFormattedValue(float value, AxisBase axis) {
//
//                return xList[(int) value % xList.length];
//            }
//        });

        YAxis leftAxis = lineChart.getAxisLeft();
//        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawLabels(true);//设置y轴的刻度值
        leftAxis.setTextColor(Color.RED);
        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);
//        YAxis rightAxis = mScakeChart.getAxisRight();
//        rightAxis.setEnabled(false);

        // 隐藏右边 的坐标轴
        lineChart.getAxisRight().setEnabled(false);
//        mLineChart.getAxisLeft().setEnabled(false);
//        mLineChart.getAxisLeft().setValueFormatter(new IAxisValueFormatter() {
//            @Override
//            public String getFormattedValue(float value, AxisBase axis) {
//                return "" + (int) value;//这样设置后，显示整数
        // 把这句代码注释掉，Y轴就可以显示浮点值了
//            }
//        });

        xAxis.setAvoidFirstLastClipping(true);

        xAxis.setTextSize(9);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#898989"));
        Description description = new Description();
        description.setText(" ");
        description.setTextSize(14);
        description.setTextColor(Color.parseColor("#777F82"));
        lineChart.setDescription(description);
        lineChart.setNoDataText("暂无数据");
        lineChart.setTouchEnabled(true); // 设置是否可以触摸
        lineChart.setDragEnabled(true);// 是否可以拖拽
        lineChart.setScaleEnabled(true);// 是否可以缩放
        lineChart.setPinchZoom(true);//y轴的值是否跟随图表变换缩放

        //将数据插入
        lineChart.setData(generateDataLine(listX, listY, listZ));
        lineChart.animateX(2000);
        lineChart.invalidate();
    }
}
