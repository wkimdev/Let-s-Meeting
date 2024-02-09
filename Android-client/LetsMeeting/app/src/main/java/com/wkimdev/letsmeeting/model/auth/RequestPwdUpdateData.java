package com.wkimdev.letsmeeting.model.auth;

import com.google.gson.annotations.SerializedName;

//비밀번호 업데이트 요청 모델
public class RequestPwdUpdateData {

    @SerializedName("email")
    String email;

    @SerializedName("password")
    String password;

    public RequestPwdUpdateData(String email, String password) {
        this.email = email;
        this.password = password;

    }
}
