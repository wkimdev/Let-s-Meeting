package com.wkimdev.letsmeeting.service;

import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.RequestMeeting;
import com.wkimdev.letsmeeting.model.meeting.RequestStatusUpdate;
import com.wkimdev.letsmeeting.model.meeting.ResponseAddMeeting;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeeting;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeetingInfo;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.FieldMap;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

// 회의 정보를 처리하는 레트로핏 서비스
public interface MeetingService {

    //새회의를 생성 API
    @POST("/api/meeting/new")
    Call<ResponseAddMeeting> addMeeting(@Body RequestMeeting requestMeeting);

    //회의리스트 조회 API
    @GET("/api/meeting/list/{id}")
    Call<ResponseMeeting> getMeetingList(@Path("id") String email);

    //회의상세 조회 API
    @GET("/api/meeting/info/{id}")
    Call<ResponseMeetingInfo> getMeetingInfo(@Path("id") int meetingId);

    //회의 수정 API
    @POST("/api/meeting/info")
    Call<ResponseCommonData> updateMeeting(@Body RequestMeeting requestMeeting);

    //회의 삭제 API
    @DELETE("/api/meeting/info/{id}")
    Call<ResponseCommonData> deleteMeeting(@Path("id") int meetingId);

    //오늘날짜 회의 요청 API
    @GET("/api/meeting/today/{date}/{email}/{limit}/{page}")
    Call<ResponseMeeting> getTodayMeetingList(@Path("date") String date, @Path("email") String email, @Path("limit") int limit, @Path("page") int page);

    //회의리스트 페이징 API
    @GET("/api/meeting/list/{email}/{limit}/{page}")
    Call<ResponseMeeting> getMeetingListWithPaging(@Path("email") String email, @Path("limit") int limit, @Path("page") int page);

    //오늘날짜 이전 회의는 삭제
    @DELETE("/api/meeting/yesterday/{date}")
    Call<ResponseCommonData> deletePreviousMeeting(@Path("date") String date);


    //회의번호, 회의비밀번호 검증
    @GET("/api/meeting/verification")
    //Call<ResponseMeetingInfo> verifyMeeting(@Path("num") St
    // ring meetingNum, @Path("pwd") String meetingPwd);
    Call<ResponseMeetingInfo> verifyMeeting(@QueryMap Map<String,String> querys);


    //회의 상태값 업데이트 API
    @POST("/api/meeting/status")
    Call<ResponseCommonData> updateMeetingStatus(@Body RequestStatusUpdate requestStatusUpdate);



}
