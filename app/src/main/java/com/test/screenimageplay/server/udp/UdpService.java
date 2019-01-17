package com.test.screenimageplay.server.udp;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;

import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import com.wt.screenimage_lib.constant.Constants;

import com.test.screenimageplay.server.udp.interf.OnUdpConnectListener;
import com.wt.screenimage_lib.utils.AboutNetUtils;
import com.wt.screenimage_lib.utils.WeakHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * Created by wt on 2018/7/11.
 */
public class UdpService extends Service implements OnUdpConnectListener {
    private Handler mHandler;
    //服务端的ip
    private String ip = null;
    private static int BROADCAST_PORT = 15000;
    private static String BROADCAST_IP = "224.0.0.1";
    InetAddress inetAddress = null;
    //发送广播端的socket
    MulticastSocket multicastSocket = null;
    private Context context;
    private WeakHandler weakHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        context = this;
        mHandler = new Handler();
        weakHandler = new WeakHandler();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        initData();
        return super.onStartCommand(intent, flags, startId);
    }
    private void initData() {
        ip = null;
        try {
            while (ip == null) {
                ip = getAddressIP();
            }
            inetAddress = InetAddress.getByName(BROADCAST_IP);//多点广播地址组
            multicastSocket = new MulticastSocket(BROADCAST_PORT);//多点广播套接字
            multicastSocket.setTimeToLive(1);
            multicastSocket.joinGroup(inetAddress);
            Log.e("UdpService", "start multcast socket");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ip != null) {
            //开始广播
            new UDPBoardcastThread(context, ip, inetAddress, multicastSocket,
                    BROADCAST_PORT, weakHandler, this);
        }
    }

    @Override
    public void udpConnectSuccess() {

    }

    @Override
    public void udpDisConnec() {
        // TODO: 2018/7/11 连接失败
        Log.e("123", "udpDisConnec: 连接失败");
        initData();
    }
    /**
     * 1.获取本机正在使用网络IP地址（wifi、有线）
     */
    public String getAddressIP() {
        //检查网络是否连接
        while (!AboutNetUtils.isNetWorkConnected(context)) {
        }
        ip = AboutNetUtils.getLocalIpAddress();
        return ip;
    }

//    // TODO: 2018/7/12 获取本地所有ip地址
//    public String getLocalIpAddress() {
//        String address = null;
//        try {
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
//                NetworkInterface intf = en.nextElement();
//                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
//                    InetAddress inetAddress = enumIpAddr.nextElement();
//                    if (!inetAddress.isLoopbackAddress()) {
//                        address = inetAddress.getHostAddress().toString();
//                        //ipV6
//                        if (!address.contains("::")) {
//                            return address;
//                        }
//                    }
//                }
//            }
//        } catch (SocketException ex) {
//            Log.e("getIpAddress Exception", ex.toString());
//        }
//        return null;
//    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        mHandler.post(new ToastRunnable("UDP Service is unavailable."));
        super.onDestroy();
    }


    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            this.mText = text;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }

    }
}
