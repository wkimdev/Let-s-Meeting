package com.wkimdev.letsmeeting.model.auth;

import com.google.gson.annotations.SerializedName;

//닉네임 업데이트 요청 모델
public class RequestNameUpdateData {

    @SerializedName("email")
    String email;

    @SerializedName("name")
    String name;

    public RequestNameUpdateData(String email, String name) {
        this.email = email;
        this.name = name;
    }
}
