package com.test.screenimageplay.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.test.screenimageplay.R;
import com.test.screenimageplay.core.BaseActivity;
import com.test.screenimageplay.decode.DecodeThread;
import com.test.screenimageplay.entity.Frame;
import com.test.screenimageplay.mediacodec.VIdeoMediaCodec;
import com.test.screenimageplay.server.NormalPlayQueue;
import com.test.screenimageplay.server.TcpServer;
import com.test.screenimageplay.server.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.interf.OnAcceptTcpStateChangeListener;
import com.test.screenimageplay.utils.ToastUtils;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    @BindView(R.id.iv_code)
    ImageView ivCode;
    @BindView(R.id.sf_view)
    SurfaceView sfView;
    @BindView(R.id.rl_code)
    RelativeLayout rlCode;

    private SurfaceHolder mSurfaceHolder;
    private FileOutputStream fos;
    private VIdeoMediaCodec videoMediaCodec;
    private DecodeThread mDecodeThread;
    private NormalPlayQueue mPlayqueue;
    private TcpServer mTcpServer;
    private String TAG = "wtt";
    private Context mContext;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    rlCode.setVisibility(View.GONE);
                    sfView.setVisibility(View.VISIBLE);
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
        initialFIle();
        startServer();
        //surface保证他们进行交互，当surface销毁之后，surfaceholder断开surface及其客户端的联系
        mSurfaceHolder = sfView.getHolder();
    }

    @Override
    protected void initData() {
        //获取本机ip
        if (TextUtils.isEmpty(getIPAddress(mContext))) {
            Log.e(TAG, "initData: xxx");
            ToastUtils.showShort(mContext, "请先设置网络");
            return;
        }
        Log.e(TAG, "initData: xxx" + getIPAddress(mContext));
        //以ip生成二维码
        Bitmap bitmap = CodeUtils.createImage(getIPAddress(mContext), 500, 500, null);
        if (bitmap == null) {
            return;
        }
        rlCode.setVisibility(View.VISIBLE);
        ivCode.setImageBitmap(bitmap);
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
                if (videoMediaCodec != null) videoMediaCodec.release();
            }
        });
    }

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
        mPlayqueue = new NormalPlayQueue();
        mTcpServer = new TcpServer();
        mTcpServer.setOnAccepttBuffListener(new MyAcceptBuffListener());
        mTcpServer.setOnTcpConnectListener(new MyAcceptTcpStateListener());
        mTcpServer.startServer();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initialMediaCodec(SurfaceHolder holder) {
        //初始化解码器
        videoMediaCodec = new VIdeoMediaCodec(holder);
        //开启解码线程
        mDecodeThread = new DecodeThread(videoMediaCodec.getCodec(), mPlayqueue);
        videoMediaCodec.start();
        mDecodeThread.start();
    }


    //接收到关于不同帧类型的回调
    class MyAcceptBuffListener implements OnAcceptBuffListener {

        @Override
        public void acceptBuff(Frame frame) {
            //存入缓存
            mPlayqueue.putByte(frame);
        }
    }

    //客户端Tcp连接状态的回调...
    class MyAcceptTcpStateListener implements OnAcceptTcpStateChangeListener {

        @Override
        public void acceptTcpConnect() {
            //接收到客户端的连接...
            Log.e(TAG, "接收到客户端的连接...");
            Message msg = new Message();
            msg.what = 1;
            mHandler.sendMessage(msg);
        }

        @Override
        public void acceptTcpDisConnect(Exception e) {
            //客户端的连接断开...
            Log.e(TAG, "客户端的连接断开..." + e.toString());
        }
    }

    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                //当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                return ipAddress;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
            return null;
        }
        return null;
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void finish() {
        super.finish();
        if (mPlayqueue != null) mPlayqueue.stop();
        if (videoMediaCodec != null) videoMediaCodec.release();
        if (mDecodeThread != null) mDecodeThread.shutdown();
        if (mTcpServer != null) mTcpServer.stopServer();
    }
}
