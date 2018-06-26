package com.lvqingyang.chatroom;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.lvqingyang.chatroom.bean.Message;
import com.lvqingyang.chatroom.bean.User;
import com.lvqingyang.chatroom.i.StateListener;
import com.lvqingyang.chatroom.tool.MyPrefence;
import com.lvqingyang.chatroom.tool.SolidRVBaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ClientSocket mClientSocket;
    private static final String HOST = "192.168.1.105";//服务器地址
    private static final int PORT = 6666;//连接端口号
    private static final String TAG = "MainActivity";
    private android.support.v7.widget.RecyclerView rvmsg;
    private android.widget.EditText etmsg;
    private android.support.design.widget.FloatingActionButton fabsend;
    private MyPrefence mPrefence;
    private User mUser;
    private List<Message> mMessages=new ArrayList<>();
    private SolidRVBaseAdapter<Message> mAdapter;

    public static Intent newIntent(Context context) {
        Intent starter = new Intent(context, MainActivity.class);
//        starter.putExtra();
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initeView();

        mPrefence=MyPrefence.getInstance(this);
        mUser=mPrefence.getUser(User.class);
        mClientSocket=new ClientSocket(HOST, PORT, mUser);
        mClientSocket.connect();

        mClientSocket.setMessageListener(new StateListener() {
            @Override
            public void connect() {
                if (BuildConfig.DEBUG) Log.d(TAG, "connect: ");
            }

            @Override
            public void connectFail() {
                showErrorDialog();
            }

            @Override
            public void receiveMessage(Message msg) {
                if (BuildConfig.DEBUG) Log.d(TAG, "receiveMessage: "+msg);
                mAdapter.addItem(msg);
            }

            @Override
            public void close() {
                if (BuildConfig.DEBUG) Log.d(TAG, "close: ");
            }
        });
    }

    private void initeView() {
        this.fabsend = (FloatingActionButton) findViewById(R.id.fab_send);
        this.etmsg = (EditText) findViewById(R.id.et_msg);
        this.rvmsg = (RecyclerView) findViewById(R.id.rv_msg);

        mAdapter=new SolidRVBaseAdapter<Message>(this, mMessages) {
            @Override
            protected void onBindDataToView(SolidCommonViewHolder holder, Message bean) {
                switch (bean.getType()) {
                    case Message.TYPE_ARRVIDE:
                        holder.setText(R.id.tv_hint, bean.getUsername()+"进入聊天室");
                        break;
                    case Message.TYPE_EXIT:
                        holder.setText(R.id.tv_hint, bean.getUsername()+"离开聊天室");
                        break;
                    case Message.TYPE_MSG_RECEIVE:
                    case Message.TYPE_MSG_SEND:
                        holder.setImage(R.id.iv_head, Data.IDS_HEAD[bean.getUserId()%Data.COUNT_HEAD]);
                        holder.setText(R.id.tv_name, bean.getUsername());
                        holder.setText(R.id.tv_msg, bean.getContent());
                        break;
                    default:
                }
            }

            @Override
            public int getItemLayoutID(int viewType) {
                switch (viewType) {
                    case Message.TYPE_ARRVIDE:
                    case Message.TYPE_EXIT:
                        return R.layout.item_msg_hint;
                    case Message.TYPE_MSG_RECEIVE:
                        return R.layout.item_msg_left;
                    case Message.TYPE_MSG_SEND:
                        return R.layout.item_msg_right;
                    default:
                        return 0;
                }
            }

            @Override
            public int getItemViewType(int position) {
                if (mBeans.size()>position) {
                    Message message=mBeans.get(position);
                    if (message.getType()== Message.TYPE_MSG) {
                        if (message.getUserId()==mUser.getId()) {
                            message.setType(Message.TYPE_MSG_SEND);
                        }else {
                            message.setType(Message.TYPE_MSG_RECEIVE);
                        }
                    }
                    return message.getType();
                }
                return super.getItemViewType(position);
            }
        };
        rvmsg.setLayoutManager(new LinearLayoutManager(this));
        rvmsg.setAdapter(mAdapter);

        fabsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg=etmsg.getText().toString();
                if (!msg.isEmpty()) {
                    etmsg.setText("");
                    mClientSocket.sendMessage(msg);
                }
            }
        });
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.con_fail)
                .setMessage(R.string.con_fail_msg)
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mClientSocket.connect();
                    }
                })
                .setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }
}
