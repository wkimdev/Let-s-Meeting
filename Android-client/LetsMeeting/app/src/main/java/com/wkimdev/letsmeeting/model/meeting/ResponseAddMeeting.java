package com.wkimdev.letsmeeting.model.meeting;

//회의생성 후 DTO 응답 모델 클래스
public class ResponseAddMeeting {

    //응답상태코드
    private int statusCode;

    //응답상태 메세지
    private String message;

    //생성된 meetingId
    private int meetingId;

    public int getMeetingId() {
        return meetingId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }


}
