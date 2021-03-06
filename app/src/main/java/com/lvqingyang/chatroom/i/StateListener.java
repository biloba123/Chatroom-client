package com.lvqingyang.chatroom.i;

import com.lvqingyang.chatroom.bean.MyMessage;

/**
 * @author Lv Qingyang
 * @date 2018/6/25
 * @email biloba12345@gamil.com
 * @github https://github.com/biloba123
 * @blog https://biloba123.github.io/
 * @see
 * @since
 */
public interface StateListener {
    void connect();

    void connectFail();

    void receiveMessage(MyMessage msg);

    void onlineChange();

    void close();
}
