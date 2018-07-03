package com.wt.screenimage_lib.entity;

/**
 * Created by xu.wang
 * Date on  2018/7/3 18:56:00.
 *
 * @Desc
 */

public class TcpEvent {
    public static final int TCP_CONNECT = 111;
    public static final int TCP_DISCONNECT = 112;

    private int tcpMsg;
    private int size;
    private Exception e;

    public int getTcpMsg() {
        return tcpMsg;
    }

    public void setTcpMsg(int tcpMsg) {
        this.tcpMsg = tcpMsg;
    }

    public Exception getE() {
        return e;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setE(Exception e) {
        this.e = e;
    }
}
