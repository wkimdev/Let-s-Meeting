package com.wkimdev.letsmeeting.model.meeting;

import java.util.ArrayList;

// 회의응답 DTO 모델 클래스
public class ResponseMeetingInfo {

    //응답상태코드
    private int statusCode;

    //응답상태 메세지
    private String message;

    //응답 데이터 모델
    private MeetingDataResult data;

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public MeetingDataResult getData() {
        return data;
    }
}
