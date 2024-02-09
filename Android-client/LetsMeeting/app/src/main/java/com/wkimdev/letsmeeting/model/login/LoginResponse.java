package com.wkimdev.letsmeeting.model.login;

// 로그인 응답 DTO 모델 클래스
public class LoginResponse {

    //응답상태코드
    private int statusCode;

    //응답상태 메세지
    private String message;

    //응답 데이터 모델
    private LoginDataResult data;

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public LoginDataResult getData() {
        return data;
    }
}
