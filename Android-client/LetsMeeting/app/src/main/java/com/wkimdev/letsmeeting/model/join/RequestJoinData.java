package com.wkimdev.letsmeeting.model.join;

import com.google.gson.annotations.SerializedName;

//회원가입 요청 모델(Body안에 들어갈 필드값)
public class RequestJoinData {

    @SerializedName("email")
    String email;

    @SerializedName("password")
    String password;

    @SerializedName("name")
    String name;

    public RequestJoinData(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }
}
