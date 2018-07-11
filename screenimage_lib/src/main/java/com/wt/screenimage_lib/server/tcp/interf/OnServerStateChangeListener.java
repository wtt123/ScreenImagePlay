package com.wt.screenimage_lib.server.tcp.interf;

import com.wt.screenimage_lib.entity.ReceiveData;
import com.wt.screenimage_lib.server.tcp.AcceptMsgThread;
import com.wt.screenimage_lib.server.tcp.EncodeV1;

import java.io.OutputStream;

/**
 * Created by wt
 * 监听连接回调
 */

public abstract class OnServerStateChangeListener {
    //接收到客户端的Tcp连接
    public abstract void acceptH264TcpConnect(int currentSize);

    /**
     * by wt
     * 接收到客户端的Tcp断开连接
     *
     * @param e           异常提示
     * @param currentSize 当前投屏线程
     */
    public abstract void acceptH264TcpDisConnect(Exception e, int currentSize);

    //接到逻辑消息
    public abstract EncodeV1 acceptLogicTcpMsg(ReceiveData data);

    //读数据的时间
    public void acceptH264TcpNetSpeed(String netSpeed) {

    }

}
