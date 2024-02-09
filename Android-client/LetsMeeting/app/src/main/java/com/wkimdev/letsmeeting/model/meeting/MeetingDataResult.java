package com.wkimdev.letsmeeting.model.meeting;

import com.google.gson.annotations.SerializedName;

// 회의응답 Data DTO 모델 클래스
public class MeetingDataResult {

    //GSON에 포함된 어노테이션
    //@SerializedName("속성명")으로
    //문자열로 넘겨주는 속성명은 해당 객체가 JSON으로 바뀔때 사용하는 값으로, 변수이름과 속성명을 똑같이 준다면 생략가능하다
    @SerializedName("meeting_id")
    private int meetingId;

    private String title;

    private String duration;

    //회의번호
    @SerializedName("meeting_num")
    private String meetingNum;

    //회의비밀번호
    @SerializedName("meeting_pwd")
    private String meetingPwd;

    //표준시간
    private String gmt;

    //회의시작시간
    @SerializedName("meeting_sdate")
    private String startDate;

    private String email;

    //회의주체자
    @SerializedName("host_id")
    private String hostId;

    //회의참여자 수
    @SerializedName("participants_num")
    private int participantsNum;

    //회의가 열려있는지 확인하는 상태값(open/close)
    @SerializedName("status")
    private String status;


    public int getMeetingId() {
        return meetingId;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }

    public String getMeetingNum() {
        return meetingNum;
    }

    public String getMeetingPwd() {
        return meetingPwd;
    }

    public String getGmt() {
        return gmt;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEmail() {
        return email;
    }

    public String getHostId() {
        return hostId;
    }

    public int getParticipantsNum() {
        return participantsNum;
    }

    public String getStatus() {
        return status;
    }

}
