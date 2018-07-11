package com.test.screenimageplay.boastcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.test.screenimageplay.utils.NetWorkUtils;
import com.test.screenimageplay.utils.ToastUtils;
import com.wt.screenimage_lib.entity.InfoDate;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by wt on 2018/7/2.
 * 监听网络变化
 */
public class NetWorkStateReceiver extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("网络状态发生变化");
        InfoDate infoDate = new InfoDate();
        infoDate.setFromState(0);
        infoDate.setNetSpeed("");
        infoDate.setCurrentSize(0);
        //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            //获得ConnectivityManager对象
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService
                    (Context.CONNECTIVITY_SERVICE);
            //获取ConnectivityManager对象对应的NetworkInfo对象
            // 获取WIFI连接的信息
            NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiNetworkInfo.isConnected()) {
                infoDate.setCurrentIP(NetWorkUtils.getIp(context));
                EventBus.getDefault().post(infoDate);
//                EventBus.getDefault().post(NetWorkUtils.getIp(context));
            } else if (!wifiNetworkInfo.isConnected()) {
                infoDate.setCurrentIP("");
                EventBus.getDefault().post(infoDate);
//                EventBus.getDefault().post("");
                ToastUtils.showShort(context, "请先连接无线网！！");
            }
            return;
        }
        //获得ConnectivityManager对象
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.
                CONNECTIVITY_SERVICE);
        //获取所有网络连接的信息
        Network[] networks = connMgr.getAllNetworks();
        //通过循环将网络信息逐个取出来
        for (int i = 0; i < networks.length; i++) {
            //获取ConnectivityManager对象对应的NetworkInfo对象
            NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (networkInfo.isConnected()) {
                    infoDate.setCurrentIP(NetWorkUtils.getIp(context));
                    EventBus.getDefault().post(infoDate);
//                    EventBus.getDefault().post(NetWorkUtils.getIp(context));
                } else if (!networkInfo.isConnected()) {
//                    EventBus.getDefault().post("");
                    infoDate.setCurrentIP("");
                    EventBus.getDefault().post(infoDate);
                    ToastUtils.showShort(context, "请先连接无线网！！");
                }
                break;
            }
        }

    }
}
