package com.test.screenimageplay.server;

import android.os.SystemClock;
import android.util.Log;

import com.test.screenimageplay.constant.ScreenImageApi;
import com.test.screenimageplay.entity.Frame;
import com.test.screenimageplay.server.interf.OnAcceptBuffListener;
import com.test.screenimageplay.server.interf.OnAcceptTcpStateChangeListener;
import com.test.screenimageplay.utils.AnalyticDataUtils;
import com.test.screenimageplay.utils.DecodeUtils;
import com.test.screenimageplay.utils.ToastUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by wt on 2018/6/7.
 * 接收消息,执行操作线程
 */
public class AcceptMsgThread extends Thread {
    private InputStream InputStream;
    private OutputStream outputStream;
    private EncodeV1 mEncodeV1;
    private boolean startFlag = true;
    private OnAcceptBuffListener listener;
    private OnAcceptTcpStateChangeListener mStateChangeListener;
    private DecodeUtils mDecoderUtils;
    private String TAG = "AcceptMsgThread";

    public AcceptMsgThread(InputStream is, OutputStream outputStream, EncodeV1 encodeV1, OnAcceptBuffListener
            listener, OnAcceptTcpStateChangeListener disconnectListenerlistener) {
        this.InputStream = is;
        this.outputStream = outputStream;
        this.mEncodeV1 = encodeV1;
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
        //告诉客户端我已经初始化成功
        byte[] content = mEncodeV1.buildSendContent();
        try {
            outputStream.write(content);
            if (mStateChangeListener != null) mStateChangeListener.acceptTcpConnect();
            while (startFlag) {
                //开始接收客户端发过来的数据
//                byte[] length = readByte(InputStream, 4);
                byte[] length = AnalyticDataUtils.readByte(InputStream, 18);
                //数据如果为空，则休眠，防止cpu为空
                if (length.length == 0) {
                    SystemClock.sleep(1);
                    continue;
                }
                byte netVersion = length[0];
                Log.e("wtt", "run: "+netVersion );
                if (netVersion == ScreenImageApi.encodeVersion1) {
                    //解析拆分数据
                    BufferedInputStream inputStream = new BufferedInputStream(InputStream);
                     AnalyticDataUtils.AnalyticData(inputStream, length,
                            new AnalyticDataUtils.onAnalyticDataListener() {
                                @Override
                                public void onSuccess(int mainCmd, int subCmd, String sendBody,
                                                      int sendBuffer) throws IOException {
                                    Log.e("wtt", "onSuccess: "+sendBuffer );
                                    byte[] buff =AnalyticDataUtils.readByte(InputStream, sendBuffer);
                                    //区分帧信息
                                    mDecoderUtils.isCategory(buff);
                                }
                            });
                } else {
                    Log.e("wtt", "run: 收到消息无法解析");
                }
//                int buffLength = bytesToInt(length);
//                byte[] buff = readByte(InputStream, buffLength);
//                //区分帧信息
//                mDecoderUtils.isCategory(buff);
            }
        } catch (Exception e) {
            if (mStateChangeListener != null) {
                mStateChangeListener.acceptTcpDisConnect(e);
            }
        } finally {
            startFlag = false;
        }
    }




//    /**
//     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
//     *
//     * @param src byte数组
//     * @return int数值
//     */
//    public static int bytesToInt(byte[] src) {
//        int value;
//        value = (int) ((src[0] & 0xFF)
//                | ((src[1] & 0xFF) << 8)
//                | ((src[2] & 0xFF) << 16)
//                | ((src[3] & 0xFF) << 24));
//        return value;
//    }

    public void shutdown() {
        startFlag = false;
        //中断非阻塞状态线程
        this.interrupt();
    }
}
