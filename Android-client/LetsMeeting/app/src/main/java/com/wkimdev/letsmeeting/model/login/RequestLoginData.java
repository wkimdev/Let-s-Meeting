package com.wkimdev.letsmeeting.model.login;

import com.google.gson.annotations.SerializedName;

//로그인 요청 모델
public class RequestLoginData {

    @SerializedName("email")
    String email;

    @SerializedName("password")
    String password;

    public String getEmail() {
        return email;
    }

    public RequestLoginData(String email, String password) {
        this.email = email;
        this.password = password;
    }

}
