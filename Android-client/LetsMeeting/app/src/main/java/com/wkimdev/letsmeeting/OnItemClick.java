package com.wkimdev.letsmeeting;

/**
 * 클릭리스너 인터페이스
 */
public interface OnItemClick {

    //회의상세 화면으로 이동시키는 클릭 이벤트
    void onClickMeetingDetail(int meetingId, String startDate, String startTime);

    //회의화면으로 이동시키는 클릭 이벤트
    void onClickMeetingStart(int meetingId, String meetingNum, int participantsNum,
                             String meetingTitle, String meetingPwd, String duration, String status);

    //채팅화면 클릭시, 키보드가 내려가도록 이벤트처리
    void onClickChatView();

}
