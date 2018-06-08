package com.test.screenimageplay.server;

import android.os.SystemClock;
import android.util.Log;

import com.test.screenimageplay.entity.Frame;
import com.test.screenimageplay.server.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.interf.OnAcceptTcpStateChangeListener;
import com.test.screenimageplay.utils.DecodeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by wt on 2018/6/7.
 * 接收消息,执行操作线程
 */
public class AcceptMsgThread extends Thread {
    private java.io.InputStream InputStream;
    private OutputStream outputStream;
    private boolean startFlag = true;
    private OnAcceptBuffListener listener;
    private OnAcceptTcpStateChangeListener mStateChangeListener;
    private DecodeUtils mDecoderUtils;
    private String TAG = "AcceptMsgThread";

    public AcceptMsgThread(InputStream is, OutputStream outputStream, OnAcceptBuffListener
            listener, OnAcceptTcpStateChangeListener disconnectListenerlistener) {
        this.InputStream = is;
        this.outputStream = outputStream;
        this.listener = listener;
        this.mStateChangeListener = disconnectListenerlistener;
        mDecoderUtils = new DecodeUtils();
        mDecoderUtils.setOnVideoListener(new DecodeUtils.OnVideoListener() {
            @Override
            public void onSpsPps(byte[] sps, byte[] pps) {
                Frame spsPpsFrame = new Frame();
                spsPpsFrame.setType(Frame.SPSPPS);
                spsPpsFrame.setSps(sps);
                spsPpsFrame.setPps(pps);
                AcceptMsgThread.this.listener.acceptBuff(spsPpsFrame);
            }

            @Override
            public void onVideo(byte[] video, int type) {
                Frame frame = new Frame();
                switch (type) {
                    case Frame.KEY_FRAME:
                        frame.setType(Frame.KEY_FRAME);
                        frame.setBytes(video);
                        AcceptMsgThread.this.listener.acceptBuff(frame);
                        break;
                    case Frame.NORMAL_FRAME:
                        frame.setType(Frame.NORMAL_FRAME);
                        frame.setBytes(video);
                        AcceptMsgThread.this.listener.acceptBuff(frame);
                        break;
                    case Frame.AUDIO_FRAME:
                        frame.setType(Frame.AUDIO_FRAME);
                        frame.setBytes(video);
                        AcceptMsgThread.this.listener.acceptBuff(frame);
//                        Log.e("AcceptH264MsgThread", "audio frame ...");
                        break;
                    default:
                        Log.e("AcceptH264MsgThread", "other video...");
                        break;
                }

            }
        });
    }

    @Override
    public void run() {
        super.run();
        byte[] ok = "OK".getBytes();
        try {
            //向客户端回传我已经初始化好的成功标识
            outputStream.write(ok);
            if (mStateChangeListener != null) mStateChangeListener.acceptTcpConnect();
            while (startFlag) {
                //开始接收客户端发过来的数据
                byte[] length = readByte(InputStream, 4);
                //数据如果为空，则休眠，防止cpu为空
                if (length.length == 0) {
                    SystemClock.sleep(1);
                    continue;
                }
                int buffLength = bytesToInt(length);
                byte[] buff = readByte(InputStream, buffLength);
                //区分帧信息
                mDecoderUtils.isCategory(buff);
            }
        } catch (Exception e) {
            if (mStateChangeListener != null) {
                mStateChangeListener.acceptTcpDisConnect(e);
            }
        } finally {
            startFlag = false;
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
    private byte[] readByte(InputStream is, int readSize) throws IOException {
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
                throw new IOException(TAG + "   :tcp have diaconnect...");
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
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src byte数组
     * @return int数值
     */
    public static int bytesToInt(byte[] src) {
        int value;
        value = (int) ((src[0] & 0xFF)
                | ((src[1] & 0xFF) << 8)
                | ((src[2] & 0xFF) << 16)
                | ((src[3] & 0xFF) << 24));
        return value;
    }

    public void shutdown() {
        startFlag = false;
        //中断非阻塞状态线程
        this.interrupt();
    }
}
