package com.wkimdev.letsmeeting.model;

//채팅메세지 DTO 모델 클래스
public class ChatData {
    private String message;
    private String nickname;
    private String profile;
    private String sendTime;
    private String userId;

    public ChatData(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }
}

