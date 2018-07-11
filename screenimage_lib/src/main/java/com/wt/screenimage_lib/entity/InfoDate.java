package com.wt.screenimage_lib.entity;

/**
 * Created by wt on 2018/7/11.
 */
public class InfoDate {
    /**
     * @param fromState 来自于哪里的回调 0:NetWorkStateReceiver 1:接收到网速回调
     *                  2:接收到客户端的连接 3：接收客户端的连接断开 4：接收到客户端的长宽
     * @param currentIP 当前ip
     * @param netSpeed 网速
     * @param currentSize 当前客户端数量
     * @param width 宽
     * @param height 高
     */
    private int fromState;
    private String currentIP;
    private String netSpeed;
    private int currentSize;
    private int width;
    private int height;

    public InfoDate() {
    }



    public int getFromState() {
        return fromState;
    }

    public void setFromState(int fromState) {
        this.fromState = fromState;
    }

    public String getCurrentIP() {
        return currentIP;
    }

    public void setCurrentIP(String currentIP) {
        this.currentIP = currentIP;
    }

    public String getNetSpeed() {
        return netSpeed;
    }

    public void setNetSpeed(String netSpeed) {
        this.netSpeed = netSpeed;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(int currentSize) {
        this.currentSize = currentSize;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
