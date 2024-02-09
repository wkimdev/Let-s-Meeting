package com.wkimdev.letsmeeting.model.meeting;

import com.wkimdev.letsmeeting.model.login.LoginDataResult;

import java.util.ArrayList;

// 회의응답 DTO 모델 클래스
public class ResponseMeeting {

    //응답상태코드
    private int statusCode;

    //응답상태 메세지
    private String message;

    //전체데이터 갯수
    private int totalCnt;

    public int getTotalCnt() {
        return totalCnt;
    }

    //응답 데이터 모델
    private ArrayList<MeetingDataResult> data;

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<MeetingDataResult> getData() {
        return data;
    }
}
