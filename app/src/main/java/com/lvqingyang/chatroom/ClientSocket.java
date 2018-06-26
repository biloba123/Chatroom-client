package com.lvqingyang.chatroom;

import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;
import com.lvqingyang.chatroom.bean.User;
import com.lvqingyang.chatroom.i.StateListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lv Qingyang
 * @date 2018/6/23
 * @email biloba12345@gamil.com
 * @github https://github.com/biloba123
 * @blog https://biloba123.github.io/
 * @see
 * @since
 */
public class ClientSocket {
    private static final int STATE_CON = 92;
    private static final int STATE_CON_FAIL = 98;
    private static final int STATE_MSG = 266;
    private static final int STATE_CLOSE = 337;
    private String mHost;
    private int mPort;
    private Socket mClient;
    private ExecutorService mExecutor;
    private MessageHandler mHandler;
    private BufferedReader mReader;
    private PrintWriter mWriter;
    private static final String TAG = "ClientSocket";
    private static final String FLAG_EXIT = "$exit$";
    private static final String FLAG_END = "$end$";
    private String mMessage;
    private StringBuilder mSb;
    private User mUser;
    private Gson mGson=new Gson();


    public ClientSocket(String host, int port, User user) {
        mHost = host;
        mPort = port;
        mUser=user;
    }

    /**
     * 状态回调
     * @param listener
     */
    public void setMessageListener(StateListener listener) {
        if (listener != null) {
            mHandler = new MessageHandler();
            mHandler.setListener(listener);
        } else {
            mHandler = null;
        }
    }

    /**
     * 连接server，并开始接收消息
     */
    public void connect() {
        mExecutor= Executors.newCachedThreadPool();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mClient = new Socket(mHost, mPort);
                    mReader = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
                    mWriter = new PrintWriter(new OutputStreamWriter(mClient.getOutputStream()), true);
                    //向server发送用户信息
                    mWriter.println(mGson.toJson(mUser));

                    notifyListener(STATE_CON, null);
                    receiveMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    destory(STATE_CON_FAIL);
                }
            }
        });
    }

    /**
     * 通知监听者
     * @param state 状态
     * @param s 数据
     */
    private void notifyListener(int state, String s) {
        if (mHandler != null) {
            Message message = Message.obtain();
            message.what = state;
            message.obj = s;
            mHandler.sendMessage(message);
        }
    }

    private void receiveMessage() throws IOException {
        while (true) {
            if (!mClient.isClosed()) {
                if (mClient.isConnected()) {
                    if (!mClient.isInputShutdown()) {
                        if ((mMessage = mReader.readLine()) != null) {
                            if (mSb == null) {
                                mSb=new StringBuilder();
                            }
                            if (mMessage.endsWith(FLAG_END)) {
                                mSb.append(mMessage, 0, mMessage.length()-5);
                                mMessage=mSb.toString();
                                mSb=null;

                                notifyListener(STATE_MSG, mMessage);
                            }else {
                                mSb.append(mMessage+"\n");
                            }
                        }
                    }
                }
            }
        }
    }

    public void sendMessage(final String msg) {
        if (mClient != null && mClient.isConnected()) {
            if (!mClient.isOutputShutdown()) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mWriter.println(new com.lvqingyang.chatroom.bean.Message(
                                com.lvqingyang.chatroom.bean.Message.TYPE_MSG,
                                mUser,
                                msg
                        )+FLAG_END);

                        //若退出，则发送完消息后destory
                        if (FLAG_EXIT.endsWith(msg)) {
                            destory(STATE_CLOSE);
                        }
                    }
                });
            }
        }
    }

    public void disconnect(){
        sendMessage(FLAG_EXIT);
    }

    private void destory(int state){
        try {
            if (mClient != null) {
                mClient.close();
            }
            if (mReader != null) {
                mReader.close();
            }
            if (mWriter != null) {
                mWriter.close();
            }
            mExecutor.shutdownNow();
            mClient=null;
            notifyListener(state, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MessageHandler extends Handler {
        private StateListener mListener;

        public void setListener(StateListener listener) {
            mListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mListener != null) {
                switch (msg.what) {
                    case STATE_CON:
                        mListener.connect();
                        break;
                    case STATE_CON_FAIL:
                        mListener.connectFail();
                        break;
                    case STATE_CLOSE:
                        mListener.close();
                        break;
                    case STATE_MSG:
                        mListener.receiveMessage(com.lvqingyang.chatroom.bean.Message.fromJson(msg.obj.toString()));
                        break;
                        default:
                }
            }
        }
    }
}
