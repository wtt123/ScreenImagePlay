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
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.test.screenimageplay.R;
import com.test.screenimageplay.constant.ScreenImageApi;
import com.test.screenimageplay.core.BaseActivity;
import com.test.screenimageplay.decode.DecodeThread;
import com.test.screenimageplay.entity.Frame;
import com.test.screenimageplay.mediacodec.VIdeoMediaCodec;
import com.test.screenimageplay.server.tcp.AcceptMsgThread;
import com.test.screenimageplay.server.NetWorkStateReceiver;
import com.test.screenimageplay.server.tcp.NormalPlayQueue;
import com.test.screenimageplay.server.tcp.TcpServer;
import com.test.screenimageplay.server.tcp.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.tcp.interf.OnAcceptTcpStateChangeListener;
import com.test.screenimageplay.utils.AboutIpUtils;
import com.test.screenimageplay.utils.NetWorkUtils;
import com.test.screenimageplay.utils.ToastUtils;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;

public class MainActivity extends BaseActivity implements OnAcceptTcpStateChangeListener, OnAcceptBuffListener {

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

    private SurfaceHolder mSurfaceHolder;
    private FileOutputStream fos;
    private VIdeoMediaCodec videoMediaCodec;
    private DecodeThread mDecodeThread;
    private NormalPlayQueue mPlayqueue;
    private TcpServer mTcpServer;
    private String TAG = "wt";
    private Context mContext;
    private NetWorkStateReceiver netWorkStateReceiver;
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
    private String currentIP;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mContext = this;
        EventBus.getDefault().register(this);
        initialFIle();
        startServer();
        //surface保证他们进行交互，当surface销毁之后，surfaceholder断开surface及其客户端的联系
        mPlayqueue = new NormalPlayQueue();
        mSurfaceHolder = sfView.getHolder();
        //监听surface的生命周期
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Surface创建时激发，一般在这里调用画面的线程
                initialMediaCodec(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //Surface的大小发生改变时调用。
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //销毁时激发，一般在这里将画面的线程停止、释放。
                releaseMediaCodec();
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
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
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

    private void startServer() {
        //开启服务
        mTcpServer = new TcpServer();
        //发送初始化成功指令
        mTcpServer.setBackpassBody(ScreenImageApi.SERVER.MAIN_CMD,
                ScreenImageApi.SERVER.INITIAL_SUCCESS, "初始化成功", new byte[0]);
        mTcpServer.setOnAccepttBuffListener(this);
        mTcpServer.setOnTcpConnectListener(this);
        mTcpServer.startServer();
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initialMediaCodec(SurfaceHolder holder) {
        //初始化解码器
        Log.e(TAG, "initial play queue");
        videoMediaCodec = new VIdeoMediaCodec(holder);
        //开启解码线程
        mDecodeThread = new DecodeThread(videoMediaCodec.getCodec(), mPlayqueue);
        videoMediaCodec.start();
        mDecodeThread.start();
    }

    private void updateUI(String currentIP) {
        Log.e(TAG, "initData: xxx" +currentIP);
        //以ip生成二维码
        Bitmap bitmap = CodeUtils.createImage(currentIP, 500, 500,
                null);
        llCode.setVisibility(View.VISIBLE);
        ivCode.setImageBitmap(bitmap);
        if (!TextUtils.isEmpty(AboutIpUtils.getDeviceModel())) {
            tvDeviceName.setText(AboutIpUtils.getDeviceModel());
        }else {
            tvDeviceName.setText("null");
        }
        if (!TextUtils.isEmpty(AboutIpUtils.getWinfeName(mContext))) {
            tvWifeName.setText("Wife：" + AboutIpUtils.getWinfeName(mContext));
        }else {
            tvWifeName.setText("Wife：null");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    // TODO: 2018/6/27 释放资源
    public void releaseMediaCodec() {
        mPlayqueue.stop();
        mDecodeThread.shutdown();

    }

    //Tcp连接状态的回调...
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void acceptTcpConnect(AcceptMsgThread acceptMsgThread) {
        //接收到客户端的连接...
        Log.e(TAG, "接收到客户端的连接...");
        mTcpServer.setacceptTcpConnect(acceptMsgThread);
        Message msg = new Message();
        msg.what = 1;
        mHandler.sendMessage(msg);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void acceptTcpDisConnect(Exception e, AcceptMsgThread acceptMsgThread) {
        //客户端的连接断开...
        Log.e(TAG, "客户端的连接断开..." + e.toString());
        mTcpServer.setacceptTcpDisConnect(mContext,acceptMsgThread);
        if (mTcpServer.currentSize() < 1) {
            Message msg = new Message();
            msg.what = 2;
            mHandler.sendMessage(msg);
        }
    }

    //接收到关于不同帧类型的回调
    @Override
    public void acceptBuff(Frame frame) {
        //存入缓存
        mPlayqueue.putByte(frame);
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
        Log.e("wtt", "onMessageEvent: "+state );
      if (!state.equals(currentIP)){
          updateUI(state);
      }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(netWorkStateReceiver);
        mHandler.removeCallbacksAndMessages(null);
        EventBus.getDefault().unregister(this);
        super.onDestroy();

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void finish() {
        if (mPlayqueue != null) mPlayqueue.stop();
        if (videoMediaCodec != null) videoMediaCodec.release();
        if (mDecodeThread != null) mDecodeThread.shutdown();
        if (mTcpServer != null) mTcpServer.stopServer();
        super.finish();
    }
}
