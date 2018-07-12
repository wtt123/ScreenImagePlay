package com.test.screenimageplay.server.udp;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import com.test.screenimageplay.server.udp.interf.OnUdpConnectListener;
import com.wt.screenimage_lib.utils.AboutNetUtils;
import com.wt.screenimage_lib.utils.WeakHandler;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by wt on 2018/7/11.
 * udp广播，建立连接使用
 */
public class UDPBoardcastThread extends Thread {
    private String ip;
    private InetAddress inetAddress;
    private int broadcastPort;
    private Context context;
    //发送广播端的socket
    private WeakHandler weakHandler;
    private MulticastSocket multicastSocket;
    private OnUdpConnectListener listener;

    public UDPBoardcastThread(Context context, String ip, InetAddress inetAddress,
                              MulticastSocket multicastSocket, int broadcastPort, WeakHandler weakHandler, OnUdpConnectListener listener) {
        Log.e("123", "UDPBoardcastThread: zzz");
        this.context = context;
        this.ip = ip;
        this.inetAddress = inetAddress;
        this.broadcastPort = broadcastPort;
        this.weakHandler = weakHandler;
        this.multicastSocket = multicastSocket;
        this.listener = listener;
        this.start();
    }

    @Override
    public void run() {
        DatagramPacket dataPacket = null;
        //将本机的IP地址放到数据包里
        byte[] data = ip.getBytes();
        dataPacket = new DatagramPacket(data, data.length, inetAddress, broadcastPort);
        //判断是否中断连接了
        while (AboutNetUtils.isNetWorkConnected(context)) {
            try {
                multicastSocket.send(dataPacket);
                Thread.sleep(5000);
//                Log.e("123:","再次发送ip地址广播");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        weakHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.udpDisConnec();
            }
        });
    }
}
