package com.wkimdev.letsmeeting.model.login;

import com.google.gson.annotations.SerializedName;

// 로그인 응답 Data DTO 모델 클래스
public class LoginDataResult {

    //GSON에 포함된 어노테이션
    //@SerializedName("속성명")으로
    //문자열로 넘겨주는 속성명은 해당 객체가 JSON으로 바뀔때 사용하는 값으로, 변수이름과 속성명을 똑같이 준다면 생략가능하다
    @SerializedName("mem_id")
    private int memId;

    private String email;

    @SerializedName("mem_name")
    private String memName;

    //프로필사진 path
    private String profile;

    //비밀번호
    private String password;

    public int getMemId() {
        return memId;
    }

    public String getEmail() {
        return email;
    }

    public String getMemName() {
        return memName;
    }

    public String getProfile() {
        return profile;
    }

    public String getPassword() {
        return password;
    }
}
