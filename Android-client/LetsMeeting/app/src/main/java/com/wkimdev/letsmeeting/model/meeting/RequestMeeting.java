package com.wkimdev.letsmeeting.model.meeting;

import com.google.gson.annotations.SerializedName;

//회의생성 요청 모델
public class RequestMeeting {

    @SerializedName("meetingId")
    int meetingId;

    @SerializedName("title")
    String title;

    @SerializedName("startDate")
    String startDate;

    @SerializedName("startTime")
    String startTime;

    @SerializedName("duration")
    String duration;

    @SerializedName("gmt")
    String gmt;

    @SerializedName("email")
    String email;

    @SerializedName("hostId")
    String hostId;

    @SerializedName("participantNum")
    int participantNum;

    public RequestMeeting(String title, String startDate, String startTime, String duration,
                          String gmt, String email, int meetingId, String hostId, int participantNum) {
        this.title = title;
        this.startDate = startDate;
        this.startTime = startTime;
        this.duration = duration;
        this.gmt = gmt;
        this.email = email;
        this.meetingId = meetingId;
        this.hostId = hostId;
        this.participantNum = participantNum;
    }
}
