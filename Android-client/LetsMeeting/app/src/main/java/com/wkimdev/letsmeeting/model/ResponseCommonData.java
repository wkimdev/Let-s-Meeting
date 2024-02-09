package com.wkimdev.letsmeeting.model;

// 공통 응답 DTO 모델 클래스
public class ResponseCommonData {

    //응답상태코드
    private int statusCode;

    //응답상태 메세지
    private String message;

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

}
