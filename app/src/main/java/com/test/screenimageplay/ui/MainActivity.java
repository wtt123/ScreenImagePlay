package com.test.screenimageplay.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.test.screenimageplay.R;
import com.test.screenimageplay.core.BaseActivity;

import com.test.screenimageplay.boastcast.NetWorkStateReceiver;
import com.test.screenimageplay.utils.AboutIpUtils;
import com.test.screenimageplay.utils.NetWorkUtils;
import com.test.screenimageplay.utils.SupportMultipleScreensUtil;
import com.test.screenimageplay.utils.ToastUtils;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.wt.screenimage_lib.ScreenImageApi;
import com.wt.screenimage_lib.ScreenImageController;
import com.wt.screenimage_lib.control.VideoPlayController;
import com.wt.screenimage_lib.entity.ReceiveData;
import com.wt.screenimage_lib.server.tcp.EncodeV1;
import com.wt.screenimage_lib.server.tcp.interf.OnServerStateChangeListener;
import com.wt.screenimage_lib.utils.DensityUtil;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    @BindView(R.id.iv_code)
    ImageView ivCode;
    @BindView(R.id.sf_view)
    SurfaceView sfView;
    @BindView(R.id.ll_code)
    LinearLayout llCode;
    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.tv_wife_name)
    TextView tvWifeName;

    private VideoPlayController mController;
    private SurfaceHolder mSurfaceHolder;
    private FileOutputStream fos;

    private String TAG = "wtt";
    private Context mContext;
    private NetWorkStateReceiver netWorkStateReceiver;
    private String currentIP;
    private MyOnServerStateChangeListener mListener;
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    llCode.setVisibility(View.GONE);
                    sfView.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    llCode.setVisibility(View.VISIBLE);
                    sfView.setVisibility(View.GONE);
                    break;
            }
        }
    };


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mContext = this;
        EventBus.getDefault().register(this);
        startServer();
        initialFIle();
        //surface保证他们进行交互，当surface销毁之后，surfaceholder断开surface及其客户端的联系
        mSurfaceHolder = sfView.getHolder();
        mController = new VideoPlayController();
        //监听surface的生命周期
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Surface创建时激发，一般在这里调用画面的线程
                mController.surfaceCreate(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //Surface的大小发生改变时调用。
                Log.e(TAG, "surface change width = " + width + " height = " + height);
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //销毁时激发，一般在这里将画面的线程停止、释放。
                mController.surfaceDestrory();
            }
        });
    }

    @Override
    protected void initData() {
        RxPermissions rxPermissions = new RxPermissions(this);
        String[] permissions = {
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK};
        rxPermissions
                .requestEach(permissions)
                .subscribe(permission -> { // will emit 2 Permission objects
                    if (permission.granted) {
                        Log.e("wtt", "accept: 同意");
                    } else if (permission.shouldShowRequestPermissionRationale) {
                        ToastUtils.showShort(mContext, "拒绝权限，等待下次询问哦");

                    } else {
                        startAppSettings();
                        ToastUtils.showShort(mContext, "拒绝权限，不再弹出询问框，请前往APP应用设置中打开此权限");
                    }
                });
        acquireWakeLock();
        if (!NetWorkUtils.isWifiActive(mContext)) {
            ToastUtils.showShort(mContext, "请先连接无线网！！");
            return;
        }
        if (TextUtils.isEmpty(NetWorkUtils.getIp(mContext))) {
            ToastUtils.showShort(mContext, "请先设置网络");
            return;
        }
        currentIP = NetWorkUtils.getIp(mContext);
        updateUI(currentIP);
    }


    private void startServer() {
        ScreenImageController.getInstance()
                .init(getApplication()).startServer();
        mListener = new MyOnServerStateChangeListener();
        ScreenImageController.getInstance().addOnAcceptTcpStateChangeListener(mListener);
    }


    // TODO: 2018/6/12 wt用于本地测试
    private void initialFIle() {
        //本地生成一个音频文件
        File file = new File(Environment.getExternalStorageDirectory(), "test.aac");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)


    private void updateUI(String currentIP) {
        Log.e(TAG, "initData: xxx" + currentIP);
        //以ip生成二维码
        Bitmap bitmap = CodeUtils.createImage(currentIP, 500, 500,
                null);
        llCode.setVisibility(View.VISIBLE);
        ivCode.setImageBitmap(bitmap);
        if (!TextUtils.isEmpty(AboutIpUtils.getDeviceModel())) {
            tvDeviceName.setText(AboutIpUtils.getDeviceModel());
        } else {
            tvDeviceName.setText("null");
        }
        if (!TextUtils.isEmpty(AboutIpUtils.getWinfeName(mContext))) {
            tvWifeName.setText("Wife：" + AboutIpUtils.getWinfeName(mContext));
        } else {
            tvWifeName.setText("Wife：null");
        }
    }

    class MyOnServerStateChangeListener extends OnServerStateChangeListener {

        @Override
        public void acceptH264TcpConnect(int currentSize) {
            //接收到客户端的连接...
            Log.e(TAG, " acceptH264TcpConnect 接收到客户端的连接...");
            Message msg = new Message();
            msg.what = 1;
            mHandler.sendMessage(msg);
        }

        @Override
        public void acceptH264TcpDisConnect(Exception e, int currentSize) {
            //客户端的连接断开...
            Log.e(TAG, " acceptH264TcpDisConnect 客户端的连接断开..." + e.toString());
            if (currentSize < 1) {
                Message msg = new Message();
                msg.what = 2;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public EncodeV1 acceptLogicTcpMsg(ReceiveData data) {   //处理收到的消息逻辑,在子线程执行,返回的EnvodeV1的内容会在本次Tcp连接中返回
            if (data.getHeader().getMainCmd() == ScreenImageApi.LOGIC_REQUEST.MAIN_CMD &&
                    data.getHeader().getSubCmd() == ScreenImageApi.LOGIC_REQUEST.GET_START_INFO) {
                //收到初始化信息
                Log.e(TAG, "收到初始化屏幕消息,初始化SurfaceView宽度为" + data.getSendBody());
                String[] split = data.getSendBody().split(",");
                int width = Integer.parseInt(split[0]);
                int height = Integer.parseInt(split[1]);
                changeSurfaceState(width, height);
                EncodeV1 encodeV1 = new EncodeV1(ScreenImageApi.LOGIC_REPONSE.MAIN_CMD, ScreenImageApi.LOGIC_REPONSE.GET_START_INFO,
                        "480,800", new byte[0]);
                return encodeV1;
            }
            return null;
        }
    }

    // TODO: 2018/7/4 改变sf的大小
    private void changeSurfaceState(int width, int height) {
        runOnUiThread(() -> {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) sfView.getLayoutParams();
            int w = DensityUtil.dip2px(mContext, width);
            int h = DensityUtil.dip2px(mContext, height);
            layoutParams.width = w;
            layoutParams.height = h;
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            sfView.setLayoutParams(layoutParams);
            SupportMultipleScreensUtil.scale(sfView);
        });
    }


    // TODO: 2018/6/25 去权限设置页
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
        startActivityForResult(intent, 100);
    }


    @Override
    protected void onResume() {
        if (netWorkStateReceiver == null) {
            netWorkStateReceiver = new NetWorkStateReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkStateReceiver, filter);
        super.onResume();
    }


    // TODO: 2018/7/2 ip切换时更新当前ui
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String state) {
        Log.e("wtt", "onMessageEvent: " + state);
        updateUI(state);

    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {

            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,

                    this.getClass().getCanonicalName());

            mWakeLock.acquire();


        }


    }


    private void releaseWakeLock() {
        if (mWakeLock != null) {

            mWakeLock.release();

            mWakeLock = null;

        }

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(netWorkStateReceiver);
        mHandler.removeCallbacksAndMessages(null);
        EventBus.getDefault().unregister(this);
        ScreenImageController.getInstance().stopServer();
        releaseWakeLock();
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        ScreenImageController.getInstance().removeOnAcceptTcpStateChangeListener(mListener);
        if (mController != null) mController.stop();
    }
}
