package com.lvqingyang.chatroom;

import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lvqingyang.chatroom.bean.MyMessage;
import com.lvqingyang.chatroom.bean.User;
import com.lvqingyang.chatroom.i.StateListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
    private static final int STATE_ONLINE_CHANGE = 346;
    private static final String TAG = "ClientSocket";
    private static final String FLAG_EXIT = "$exit$";
    private static final String FLAG_END = "$end$";
    private String mHost;
    private int mPort;
    private Socket mClient;
    private ExecutorService mExecutor;
    private MessageHandler mHandler;
    private BufferedReader mReader;
    private PrintWriter mWriter;
    private String mMessage;
    private MyMessage mMsg;
    private StringBuilder mSb;
    private User mUser;
    private Gson mGson = new Gson();
    private boolean mIsExit = false;
    private List<User> mUserList=new ArrayList<>();


    public ClientSocket(String host, int port, User user) {
        mHost = host;
        mPort = port;
        mUser = user;
    }

    /**
     * 状态回调
     *
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
        mExecutor = Executors.newCachedThreadPool();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mClient = new Socket(mHost, mPort);
                    mReader = new BufferedReader(new InputStreamReader(mClient.getInputStream(), "UTF-8"));
                    mWriter = new PrintWriter(new OutputStreamWriter(mClient.getOutputStream(), "UTF-8"), true);
                    //向server发送用户信息
                    mWriter.println(mGson.toJson(mUser));

                    //接收在线用户
                    if ((mMessage = mReader.readLine()) != null) {
                        mUserList.addAll((Collection<? extends User>) mGson.fromJson(mMessage, new TypeToken<List<User>>() {}.getType()));
                    }

                    notifyListener(STATE_CON, null);
                    receiveMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    destory(STATE_CLOSE);
                }
            }
        });
    }

    /**
     * 通知监听者
     *
     * @param state 状态
     * @param msg   数据
     */
    private void notifyListener(int state, MyMessage msg) {
        if (mHandler != null) {
            Message message = Message.obtain();
            message.what = state;
            message.obj = msg;
            mHandler.sendMessage(message);
        }
    }

    private void receiveMessage() throws IOException {
        while (!mIsExit) {
            if (!mClient.isClosed()) {
                if (mClient.isConnected()) {
                    if (!mClient.isInputShutdown()) {
                        if ((mMessage = mReader.readLine()) != null) {
                            if (mSb == null) {
                                mSb = new StringBuilder();
                            }
                            if (mMessage.endsWith(FLAG_END)) {
                                mSb.append(mMessage, 0, mMessage.length() - 5);
                                mMessage = mSb.toString();
                                mSb = null;

                                mMsg = MyMessage.fromJson(mMessage);
                                switch (mMsg.getType()) {
                                    case MyMessage.TYPE_EXIT:
                                        userOutline(mMsg.getUserId());
                                        notifyListener(STATE_ONLINE_CHANGE, null);
                                        break;
                                    case MyMessage.TYPE_ARRVIDE:
                                        if (mMsg.getUserId()!=mUser.getId()) {
                                            mUserList.add(new User(mMsg.getUserId(), mMsg.getUsername()));
                                        }
                                        notifyListener(STATE_ONLINE_CHANGE, null);
                                        break;
                                    default:
                                }
                                notifyListener(STATE_MSG, mMsg);
                            } else {
                                mSb.append(mMessage + "\n");
                            }
                        }
                    }
                }
            }
        }

    }

    private void destory(int state) {
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
            mClient = null;
            notifyListener(state, null);
            mIsExit = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void userOutline(int userId) {
        for (Iterator<User> i = mUserList.iterator(); i.hasNext(); ) {
            if (i.next().getId() == userId) {
                i.remove();
            }
        }
    }

    public void disconnect() {
        sendMessage(FLAG_EXIT);
        mIsExit = true;
    }

    public void sendMessage(final String msg) {
        if (mClient != null && mClient.isConnected()) {
            if (!mClient.isOutputShutdown()) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (FLAG_EXIT.equals(msg)) {
                            mWriter.println(msg);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                destory(STATE_CLOSE);
                            }
                        } else {
                            mWriter.println(new MyMessage(
                                    MyMessage.TYPE_MSG,
                                    mUser,
                                    msg
                            ) + FLAG_END);
                        }
                    }
                });
            }
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
                        mListener.receiveMessage((MyMessage) msg.obj);
                        break;
                    case STATE_ONLINE_CHANGE:
                        mListener.onlineChange();
                        break;
                    default:
                }
            }
        }
    }

    public List<User> getUserList() {
        if (mUserList == null) {
            return new ArrayList<>();
        }
        return mUserList;
    }
}
