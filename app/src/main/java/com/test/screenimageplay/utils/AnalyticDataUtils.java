package com.test.screenimageplay.utils;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wt on 2018/6/12.
 * 根据协议解析数据
 */
public class AnalyticDataUtils {

    // TODO: 2018/6/11 wt处理协议相应指令
    public static void AnalyticData(BufferedInputStream bis, byte[] bytes,
                                   onAnalyticDataListener listener) throws IOException {

        //实现数组之间的复制
        //bytes：源数组
        //srcPos：源数组要复制的起始位置
        //dest：目的数组
        //destPos：目的数组放置的起始位置
        //length：复制的长度
        byte[] buff = new byte[4];
        System.arraycopy(bytes, 1, buff, 0, 4);
        final short mainCmd = ByteUtil.bytesToShort(buff);       //主指令  1`5
        buff=new byte[4];
        System.arraycopy(bytes, 5, buff, 0, 4);
        final short subCmd = ByteUtil.bytesToShort(buff);    //子指令  5`9
        buff = new byte[4];
        System.arraycopy(bytes, 9, buff, 0, 4);
        int stringBodySize = ByteUtil.bytesToInt(buff);//文本数据 9 ~ 13;
        buff = new byte[4];
        System.arraycopy(bytes, 13, buff, 0, 4);
        int byteBodySize = ByteUtil.bytesToInt(buff);//byte数据 13^17

        buff = new byte[2 * 1024];
        int len = 0;
        int totalLen = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = bis.read(buff)) != -1) {
            if (len != -1) totalLen += len;
            baos.write(buff, 0, len);
            //解析出文本内容
            if (totalLen >= stringBodySize) {
                break;
            }
        }
        final String body = baos.toString();
        baos.close();
        if (!TextUtils.isEmpty(body)||byteBodySize!=0){
          listener.onSuccess(mainCmd,subCmd,body,byteBodySize);
        }
    }

    /**
     * 保证从流里读到指定长度数据
     *
     * @param is
     * @param readSize
     * @return
     * @throws Exception
     */
    public static byte[] readByte(InputStream is, int readSize) throws IOException {
        byte[] buff = new byte[readSize];
        int len = 0;
        int eachLen = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (len < readSize) {
            eachLen = is.read(buff);
            if (eachLen != -1) {
                len += eachLen;
                baos.write(buff, 0, eachLen);
            } else {
                baos.close();
                throw new IOException("wtt" + "   :tcp have diaconnect...");
            }
            if (len < readSize) {
                buff = new byte[readSize - len];
            }
        }
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }
    /**
     * 回调
     */
    private static onAnalyticDataListener mListener;

    public interface onAnalyticDataListener {
        void onSuccess(int mainCmd, int subCmd, String sendBody,int sendBuffer) throws IOException;

    }

    public void setOnItemClickListenner(onAnalyticDataListener listener) {
        this.mListener = listener;
    }
}
