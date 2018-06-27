package com.lvqingyang.chatroom.bean;

import com.google.gson.Gson;

/**
 * @author Lv Qingyang
 * @date 2018/6/25
 * @email biloba12345@gamil.com
 * @github https://github.com/biloba123
 * @blog https://biloba123.github.io/
 * @see
 * @since
 */
public class MyMessage {
    public static final int TYPE_MSG = 703;
    public static final int TYPE_MSG_RECEIVE = 23;
    public static final int TYPE_MSG_SEND = 54;
    public static final int TYPE_ARRVIDE = 808;
    public static final int TYPE_EXIT = 275;
    private int type;
    private int userId;
    private String username;
    private String content;
    private static final Gson GSON=new Gson();


    public MyMessage() {
    }

    public MyMessage(int type, User user, String content) {
        this(type, user.getId(), user.getUsername(), content);
    }

    public MyMessage(int type, int userId, String username, String content) {
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.content = content;
    }

    public static MyMessage fromJson(String json){
        return GSON.fromJson(json, MyMessage.class);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}
