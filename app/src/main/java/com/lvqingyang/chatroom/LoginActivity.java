package com.lvqingyang.chatroom;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.Gson;
import com.lvqingyang.chatroom.bean.User;
import com.lvqingyang.chatroom.net.API;
import com.lvqingyang.chatroom.net.MyOkHttp;
import com.lvqingyang.chatroom.tool.MyPrefence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import shem.com.materiallogin.DefaultLoginView;
import shem.com.materiallogin.DefaultRegisterView;
import shem.com.materiallogin.MaterialLoginView;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private shem.com.materiallogin.MaterialLoginView login;
    private ExecutorService mExecutor;
    private MyOkHttp mOkHttp;
    private Gson mGson;
    private MyPrefence mPrefence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.login = (MaterialLoginView) findViewById(R.id.login);

        mPrefence=MyPrefence.getInstance(this);
        if (mPrefence.isLogined()) {
            startActivity(MainActivity.newIntent(this));
            finish();
        }
        mExecutor = Executors.newFixedThreadPool(2);
        mOkHttp = MyOkHttp.getInstance();
        mGson = new Gson();

        ((DefaultLoginView) login.getLoginView()).setListener(new DefaultLoginView.DefaultLoginViewListener() {
            @Override
            public void onLogin(TextInputLayout loginUser, TextInputLayout loginPass) {
                //Handle login
                String username = loginUser.getEditText().getText().toString(),
                        password = loginPass.getEditText().getText().toString();
                if (checkEmpty(loginUser, username) && checkEmpty(loginPass, password)) {
                    loginRequest(username, password);
                }
            }
        });

        ((DefaultRegisterView) login.getRegisterView()).setListener(new DefaultRegisterView.DefaultRegisterViewListener() {
            @Override
            public void onRegister(TextInputLayout registerUser, TextInputLayout registerPass, TextInputLayout registerPassRep) {
                //Handle register
                String username = registerUser.getEditText().getText().toString(),
                        pwd = registerPass.getEditText().getText().toString(),
                        rePwd = registerPassRep.getEditText().getText().toString();
                if (checkEmpty(registerUser, username) && checkEmpty(registerPass, pwd) && checkEmpty(registerPassRep, rePwd)) {
                    if (TextUtils.equals(pwd, rePwd)) {
                        registerRequest(username, pwd);
                    } else {
                        registerPassRep.setError("Password is not same");
                    }
                }
            }
        });
    }

    private boolean checkEmpty(TextInputLayout textInputLayout, String text) {
        if (TextUtils.isEmpty(text)) {
            textInputLayout.setError("Empty");
            return false;
        }

        textInputLayout.setError(null);
        return true;
    }

    private void loginRequest(final String username, final String password) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final User user = new User(username, password);
                try {
                    final String response = mOkHttp.postJson(API.HOST + API.URL_LOGIN, mGson.toJson(user));
                    if (TextUtils.isEmpty(response)) {
                        showToast("Username or password is error");
                        return;
                    }
                    final User user1 = mGson.fromJson(response, User.class);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loginSuccess(user1, "Login success");
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("error");
                }
            }
        });
    }

    private void registerRequest(final String username, final String pwd) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final User user = new User(username, pwd);
                try {
                    final String response = mOkHttp.postJson(API.HOST + API.URL_REGISTER, mGson.toJson(user));
                    final int id = Integer.parseInt(response);
                    switch (id) {
                        case -1:
                            showToast("Register fail, retry later.");
                            break;
                        case 0:
                            showToast("The username has be used.");
                            break;
                        default:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    user.setId(id);
                                    loginSuccess(user, "Register success");
                                }
                            });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("error");
                }
            }
        });
    }

    private void loginSuccess(User user, String hint) {
        Toast.makeText(LoginActivity.this, hint, Toast.LENGTH_SHORT).show();
        mPrefence.saveUser(user);
        startActivity(MainActivity.newIntent(this));
        finish();
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }
}
