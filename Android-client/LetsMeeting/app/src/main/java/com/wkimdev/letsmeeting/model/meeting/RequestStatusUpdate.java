package com.wkimdev.letsmeeting.model.meeting;

import com.google.gson.annotations.SerializedName;

//회의상태값 업데이트 요청 모델
public class RequestStatusUpdate {

    @SerializedName("hostId")
    String hostId;

    @SerializedName("meetingId")
    int meetingId;

    public RequestStatusUpdate(String hostId, int meetingId) {
        this.hostId = hostId;
        this.meetingId = meetingId;
    }
}
