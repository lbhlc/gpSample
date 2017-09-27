package com.gprinter.sample.printZXT;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
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
import com.gprinter.sample.MainActivity;

import com.sample.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Wrapper;
import java.util.ArrayList;

public class PrintActivity extends Activity implements View.OnClickListener{

    /**
     * 保存
     */
    private TextView mSave;
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
    String cacheDirPath=null;
    private static final int SAVESUCCEED=200;
    private static final int SAVEFAILED=201;
    private static final int SAVEZBZ=203;
    /**
     * 折线图的数据
     */
    private LineData mLineData = new LineData();
    private ArrayList<ILineData> mDataList = new ArrayList<>();
    private ArrayList<PointF> mPointArrayList = new ArrayList<>();
    protected float[][] points = new float[][]{{1,10}, {2,47}, {3,11}, {4,38}, {5,9},{6,52}, {7,14}, {8,37}, {9,29}, {10,31}};
    protected float[][] points2 = new float[][]{{1,52}, {2,13}, {3,51}, {4,20}, {5,19},{6,20}, {7,54}, {8,7}, {9,19}, {10,41}};

    /**
     *
     * 计数
     */
    int i=0;
    ArrayList<String>list=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        initView();
        initData();
        LineChart lineChart = (LineChart)findViewById(R.id.lineChart);
        lineChart.setDataList(mDataList);
        cacheDirPath=getExternalCacheDir().getAbsolutePath();
        Log.e("LBH",cacheDirPath);
    }
    private void initView() {

        mSave = (TextView) findViewById(R.id.tv_save);
        mSaveArea = (LinearLayout) findViewById(R.id.ll_save_area);
        mPicGet = (ImageView) findViewById(R.id.iv_pic_get);
        mFL = (FrameLayout) findViewById(R.id.fl_pic);
        mTotal = (FrameLayout) findViewById(R.id.fl_total);

        mSave.setOnClickListener(this);

        successHandler = new SuccessHandler();
        initHandler = new InitHandler();
    }

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
    @Override
    public void onClick(View v) {
        mSave.setText("存储中……");
        mSave.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                screenshot();
            }
        }).start();
    }
    private void screenshot() {

        // 获取屏幕
        View dView = getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        Bitmap bmp = dView.getDrawingCache();
        if (bmp != null) {
            try {
                //二次截图
                saveBitmap = Bitmap.createBitmap(mSaveArea.getWidth(), mSaveArea.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(saveBitmap);
                Paint paint = new Paint();
                canvas.drawBitmap(bmp, new Rect(mCutLeft, mCutTop, mCutLeft + mSaveArea.getWidth(), mCutTop + mSaveArea.getHeight()),
                        new Rect(0, 0, mSaveArea.getWidth(), mSaveArea.getHeight()), paint);

                File imageDir=new File(Constant.IMAGE_DIR);
                //File imageDir = new File(cacheDirPath);
                if (!imageDir.exists()) {
                    imageDir.mkdir();
                }
                String imageName = Constant.SCREEN_SHOT;
                File file = new File(imageDir, imageName);
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FileOutputStream os = new FileOutputStream(file);
                saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();

                //将截图保存至相册并广播通知系统刷新
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), imageName, null);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
                Log.e("LBH",Uri.fromFile(file).getPath());
                sendBroadcast(intent);
                intent=new Intent(PrintActivity.this,MainActivity.class);
                /**
                 * 目前可以打印3个图片一次
                 */
                String path=Uri.fromFile(file).getPath();
                if (path!=null) {
                    if (i < 3){
                        list.add(path);
                        Message message = new Message();
                        message.what = SAVEZBZ;
                        successHandler.sendMessage(message);
                        i++;
                    }else if (i==3) {
                        intent.putStringArrayListExtra("imagepath",list);
                        Message message = new Message();
                        message.what = SAVESUCCEED;
                        successHandler.sendMessage(message);
                        startActivity(intent);
                    }else
                    {
                        return;
                    }
                }
                else
                {
                    Message message=new Message();
                    message.what=SAVEFAILED;
                    successHandler.sendMessage(message);
                }
//
//                successHandler.sendMessage(Message.obtain());

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            initHandler.sendMessage(Message.obtain());
        }

    }
    /**
     * 成功动画handler
     */
    private class SuccessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//            showSuccess();
            switch (msg.what)
            {
                case SAVESUCCEED:
                    showSuccessNew();
                    break;
                case  SAVEFAILED:
                    Toast.makeText(PrintActivity.this,"图片不存在",Toast.LENGTH_LONG).show();
                    return;
                case SAVEZBZ:
                    showSuccessNew();
                    break;

            }


        }
    }
    /**
     * 恢复初始化handler
     */
    private class InitHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            ToastUtils.show(PrintActivity.this, "存储失败");
            mSave.setEnabled(true);
            mSave.setText("存储到相册");
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
                        mSave.setText("存储到相册");
                    }
                }, 1500);
            }
        });
        paramsAnimator.start();
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
    private void initData() {
        for (int i = 0; i < 8; i++) {
            mPointArrayList.add(new PointF(points[i][0], points[i][1]));
        }
        mLineData.setValue(mPointArrayList);
        mLineData.setColor(Color.MAGENTA);
        mLineData.setPaintWidth(pxTodp(3));
        mLineData.setTextSize(pxTodp(10));
        mDataList.add(mLineData);
    }
    protected float pxTodp(float value){
        DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float valueDP= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,value,metrics);
        return valueDP;
    }
}
