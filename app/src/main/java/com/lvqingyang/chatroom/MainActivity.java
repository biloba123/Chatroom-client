package com.lvqingyang.chatroom;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.lvqingyang.chatroom.bean.MyMessage;
import com.lvqingyang.chatroom.bean.User;
import com.lvqingyang.chatroom.i.StateListener;
import com.lvqingyang.chatroom.tool.MyPrefence;
import com.lvqingyang.chatroom.tool.SolidRVBaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String HOST = "47.106.169.85";//服务器地址
    private static final int PORT = 6666;//连接端口号
    private static final String TAG = "MainActivity";
    private ClientSocket mClientSocket;
    private android.support.v7.widget.RecyclerView rvmsg;
    private android.widget.EditText etmsg;
    private android.support.design.widget.FloatingActionButton fabsend;
    private MyPrefence mPrefence;
    private User mUser;
    private List<MyMessage> mMessages = new ArrayList<>();
    private SolidRVBaseAdapter<MyMessage> mAdapter;
    private RecyclerView rvonline;
    private SolidRVBaseAdapter<User> mOnlineAdapter;
    private android.support.v4.widget.DrawerLayout dl;

    public static Intent newIntent(Context context) {
        Intent starter = new Intent(context, MainActivity.class);
//        starter.putExtra();
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefence = MyPrefence.getInstance(this);
        mUser = mPrefence.getUser(User.class);
        mClientSocket = new ClientSocket(HOST, PORT, new User(mUser.getId(), mUser.getUsername()));
        mClientSocket.connect();

        initeView();

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
            public void receiveMessage(MyMessage msg) {
                if (BuildConfig.DEBUG) Log.d(TAG, "receiveMessage: " + msg);
                mAdapter.addItem(msg);
            }

            @Override
            public void onlineChange() {
                if (BuildConfig.DEBUG) Log.d(TAG, "onlineChange: " + mClientSocket.getUserList());
                mOnlineAdapter.notifyDataSetChanged();
            }

            @Override
            public void close() {
                if (BuildConfig.DEBUG) Log.d(TAG, "close: ");
                finish();
            }
        });
    }

    private void initeView() {
        this.fabsend = (FloatingActionButton) findViewById(R.id.fab_send);
        this.etmsg = (EditText) findViewById(R.id.et_msg);
        this.rvmsg = (RecyclerView) findViewById(R.id.rv_msg);
        this.dl = (DrawerLayout) findViewById(R.id.dl);
        this.rvonline = (RecyclerView) findViewById(R.id.rv_online);

        mAdapter = new SolidRVBaseAdapter<MyMessage>(this, mMessages) {
            @Override
            protected void onBindDataToView(SolidCommonViewHolder holder, MyMessage bean) {
                switch (bean.getType()) {
                    case MyMessage.TYPE_ARRVIDE:
                        holder.setText(R.id.tv_hint, bean.getUsername() + "进入聊天室");
                        break;
                    case MyMessage.TYPE_EXIT:
                        holder.setText(R.id.tv_hint, bean.getUsername() + "离开聊天室");
                        break;
                    case MyMessage.TYPE_MSG_RECEIVE:
                    case MyMessage.TYPE_MSG_SEND:
                        holder.setImage(R.id.iv_head, Data.IDS_HEAD[bean.getUserId() % Data.COUNT_HEAD]);
                        holder.setText(R.id.tv_name, bean.getUsername());
                        holder.setText(R.id.tv_msg, bean.getContent());
                        break;
                    default:
                }
            }

            @Override
            public int getItemLayoutID(int viewType) {
                switch (viewType) {
                    case MyMessage.TYPE_ARRVIDE:
                    case MyMessage.TYPE_EXIT:
                        return R.layout.item_msg_hint;
                    case MyMessage.TYPE_MSG_RECEIVE:
                        return R.layout.item_msg_left;
                    case MyMessage.TYPE_MSG_SEND:
                        return R.layout.item_msg_right;
                    default:
                        return 0;
                }
            }

            @Override
            public int getItemViewType(int position) {
                if (mBeans.size() > position) {
                    MyMessage myMessage = mBeans.get(position);
                    if (myMessage.getType() == MyMessage.TYPE_MSG) {
                        if (myMessage.getUserId() == mUser.getId()) {
                            myMessage.setType(MyMessage.TYPE_MSG_SEND);
                        } else {
                            myMessage.setType(MyMessage.TYPE_MSG_RECEIVE);
                        }
                    }
                    return myMessage.getType();
                }
                return super.getItemViewType(position);
            }
        };
        rvmsg.setLayoutManager(new LinearLayoutManager(this));
        rvmsg.setAdapter(mAdapter);

        mOnlineAdapter=new SolidRVBaseAdapter<User>(this, mClientSocket.getUserList()) {
            @Override
            protected void onBindDataToView(SolidCommonViewHolder holder, User bean) {
                holder.setImage(R.id.iv_head, Data.IDS_HEAD[bean.getId() % Data.COUNT_HEAD]);
                holder.setText(R.id.tv_name, bean.getUsername());
            }

            @Override
            public int getItemLayoutID(int viewType) {
                return R.layout.item_online;
            }
        };
        rvonline.setLayoutManager(new LinearLayoutManager(this));
        rvonline.setAdapter(mOnlineAdapter);

        fabsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = etmsg.getText().toString();
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
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_exit:
                exitApp();
                return true;
            case R.id.item_online:
                if (dl.isDrawerOpen(Gravity.END)) {
                    dl.closeDrawer(Gravity.END);
                } else {
                    dl.openDrawer(Gravity.END);
                }
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    private void exitApp() {
        mClientSocket.disconnect();
    }

    @Override
    public void onBackPressed() {
        exitApp();
    }
}
