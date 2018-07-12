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
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.wt.screenimage_lib.ScreenImageApi;
import com.wt.screenimage_lib.ScreenImageController;
import com.wt.screenimage_lib.entity.InfoDate;
import com.wt.screenimage_lib.entity.ReceiveData;
import com.wt.screenimage_lib.server.tcp.EncodeV1;
import com.wt.screenimage_lib.server.tcp.interf.OnServerStateChangeListener;
import com.test.screenimageplay.server.udp.interf.OnUdpConnectListener;
import com.wt.screenimage_lib.utils.AboutNetUtils;
import com.wt.screenimage_lib.utils.WeakHandler;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wt on 2018/7/11.
 */
public class UdpService extends Service implements OnUdpConnectListener{
    private Handler mHandler;
//    private ServerSocket server;
    //服务端的ip
    private String ip = null;
    private static int BROADCAST_PORT = 15000;
    private static String BROADCAST_IP = "224.0.0.1";
    InetAddress inetAddress = null;
    //发送广播端的socket
    MulticastSocket multicastSocket = null;
    private ExecutorService mExecutorService = null;//thread pool
    private List<Socket> mList = new ArrayList<Socket>();
    private boolean isConnected = false;
    private ConnectivityManager connectivity;
    private NetworkInfo netWorkinfo;
    private Context context;
    private WeakHandler weakHandler;
    private boolean isStart=false;

    private MyOnServerStateChangeListener mListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //1
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        context=this;
        mHandler = new Handler();
        weakHandler = new WeakHandler();
        addStreamListener();
//        try {
//            server = new ServerSocket(PORT);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    private void addStreamListener() {
        mListener = new MyOnServerStateChangeListener();
        ScreenImageController.getInstance().addOnAcceptTcpStateChangeListener(mListener);
    }

    //2
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
            Log.e("UdpService","start multcast socket");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ip != null) {
            //开始广播
            new UDPBoardcastThread(context,ip, inetAddress,multicastSocket, BROADCAST_PORT,weakHandler,this);
            new Thread() {
                @Override
                public void run() {
//                    try {
                        //建立一个线程池，每次收到一个客户端，新开一个线程
                        mExecutorService = Executors.newCachedThreadPool();
//                        Socket client = null;
//                        mList.clear();
//                        while (isConnected) {
//                            client = server.accept();
//                            //把客户端放入客户端集合中
//                            if (!connectOrNot(client)) {
//                                mList.add(client);
//                                Log.i("UDPService", "当前连接数：" + mList.size());
//                            }
//                            if (!isStart) {
//                                isStart=true;
//                                startServer();
//                            }
////                            mExecutorService.execute(new Service(client));
//                        }

//                        //释放客户端
//                        for (int i = 0; i < mList.size(); i++)
//                            mList.get(i).close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
            }.start();
        }
    }

    @Override
    public void udpConnectSuccess() {

    }

    @Override
    public void udpDisConnec() {
        // TODO: 2018/7/11 连接失败
        isConnected=false;
        Log.e("123", "udpDisConnec: 连接失败");
        initData();

    }


    //客户端线程，组成线程池
//    class Service implements Runnable {
//        private Socket socket;
//        private BufferedReader in = null;
//        private String msg = "";
//
//        public Service(Socket socket) {
//            this.socket = socket;
//        }
//
//        @Override
//        public void run() {
//            try {
//                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                //等待接收客户端发送的数据
//                while (isConnected) {
//
//                    if ((msg = in.readLine()) != null && isConnected) {
//
//                        // 创建一个Instrumentation对象，调用inst对象的按键模拟方法
//                        Instrumentation inst = new Instrumentation();
//                        try {
//                            int codeKey = Integer.parseInt(msg);
//                            //codeKey对应键值参照KeyCodeTable.txt文件，在客户端中实现
//                            inst.sendKeyDownUpSync(codeKey);
//
//                            //发送回执
//                            this.sendmsg(socket);
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
//
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        private void sendmsg(Socket socket2) {
//            // TODO Auto-generated method stub
//            PrintWriter pout = null;
//
//            try {
//                pout = new PrintWriter(new BufferedWriter(
//                        new OutputStreamWriter(socket2.getOutputStream())), true);
//                pout.println("I am ok");
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//        }
//
//    }

    /**
     * 1.获取本机正在使用网络IP地址（wifi、有线）
     */
    public String getAddressIP() {
        //检查网络是否连接
        while (!AboutNetUtils.isNetWorkConnected(context)) {
            isConnected = true;
        }
        ip = getLocalIpAddress();
        return ip;
    }

    public String getLocalIpAddress() {
        String address = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        address = inetAddress.getHostAddress().toString();
                        //ipV6
                        if (!address.contains("::")) {
                            return address;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("getIpAddress Exception", ex.toString());
        }
        return null;
    }

//    private boolean isNetWorkConnected() {
//        // TODO Auto-generated method stub
//        try{
//            connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//            if(connectivity != null){
//                netWorkinfo = connectivity.getActiveNetworkInfo();
//                if(netWorkinfo != null && netWorkinfo.isAvailable()){
//                    if(netWorkinfo.getState() == NetworkInfo.State.CONNECTED){
//                        isConnected = true;
//                        return true;
//                    }
//                }
//            }
//        }catch(Exception e){
//            Log.e("UdpService : ",e.toString());
//            return false;
//        }
//        return false;
//    }

    //若已添加，则返回true
    private boolean connectOrNot(Socket socket) {
        int num = mList.size();
        for (int index = 0; index < num; index++) {
            Socket mSocket = mList.get(index);
            if (mSocket.getInetAddress().getHostAddress().
                    equals(socket.getInetAddress().getHostAddress())) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler01 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                //连接失败
                case 0x0001:
                    initData();
                    break;
            }
        }

    };

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

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        mHandler.post(new ToastRunnable("UDP Service is unavailable."));
        super.onDestroy();
    }




    class MyOnServerStateChangeListener extends OnServerStateChangeListener {
        InfoDate infoDate=new InfoDate();
        @Override
        public void acceptH264TcpNetSpeed(final String netSpeed) {
            super.acceptH264TcpNetSpeed(netSpeed);
            weakHandler.post(new Runnable() {
                @Override
                public void run() {
                    infoDate.setFromState(1);
                    infoDate.setNetSpeed(netSpeed);
                    EventBus.getDefault().post(infoDate);
                }
            });
        }

        @Override
        public void acceptH264TcpConnect(int currentSize) {
            //接收到客户端的连接...
            weakHandler.post(new Runnable() {
                @Override
                public void run() {
                    infoDate.setFromState(2);
                    EventBus.getDefault().post(infoDate);
                }
            });
//            Log.e(TAG, " acceptH264TcpConnect 接收到客户端的连接...");
//            Message msg = new Message();
//            msg.what = 1;
//            mHandler.sendMessage(msg);
        }

        @Override
        public void acceptH264TcpDisConnect(Exception e, int currentSize) {
            //客户端的连接断开...
            infoDate.setFromState(3);
            infoDate.setCurrentSize(currentSize);
            EventBus.getDefault().post(infoDate);
//            Log.e(TAG, " acceptH264TcpDisConnect 客户端的连接断开..." + e.toString());
//            if (currentSize < 1) {
//                Message msg = new Message();
//                msg.what = 2;
//                mHandler.sendMessage(msg);
//            }
//            runOnUiThread(()->{
//                tvNetSpeed.setText("");
//            });
        }

        @Override
        public EncodeV1 acceptLogicTcpMsg(ReceiveData data) {
            //处理收到的消息逻辑,在子线程执行,返回的EnvodeV1的内容会在本次Tcp连接中返回
            if (data.getHeader().getMainCmd() == ScreenImageApi.LOGIC_REQUEST.MAIN_CMD &&
                    data.getHeader().getSubCmd() == ScreenImageApi.LOGIC_REQUEST.GET_START_INFO) {
                //收到初始化信息
                Log.e("123", "收到初始化屏幕消息,初始化SurfaceView宽度为" + data.getSendBody());
                String[] split = data.getSendBody().split(",");
                int width = Integer.parseInt(split[0]);
                int height = Integer.parseInt(split[1]);
                infoDate.setFromState(4);
                infoDate.setWidth(width);
                infoDate.setHeight(height);
                EventBus.getDefault().post(infoDate);
//                changeSurfaceState(width, height);
                EncodeV1 encodeV1 = new EncodeV1(ScreenImageApi.LOGIC_REPONSE.MAIN_CMD, ScreenImageApi.LOGIC_REPONSE.GET_START_INFO,
                        "480,800", new byte[0]);
                return encodeV1;
            }
            return null;
        }
    }

    public void finish(){
        ScreenImageController.getInstance().removeOnAcceptTcpStateChangeListener(mListener);
    }

}
