package com.wkimdev.letsmeeting.service;

import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.auth.RequestNameUpdateData;
import com.wkimdev.letsmeeting.model.auth.RequestPwdUpdateData;
import com.wkimdev.letsmeeting.model.join.RequestJoinData;
import com.wkimdev.letsmeeting.model.login.LoginResponse;
import com.wkimdev.letsmeeting.model.login.RequestLoginData;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

// 멤버 정보를 처리하는 레트로핏 서비스
public interface MemberService {

    /*
    * 로그인 API
    * @GET("posts/{post}") - 요청메소드 GET, baseUrl에 연결될 EndPoint 'posts/{post}'
    * Call은 응답이 왔을때 Callback으로 불려질 타입
    */
    @POST("/api/auth/member")
    Call<LoginResponse> login(@Body RequestLoginData requestLoginData);

    // 회원가입 API
    @POST("/api/member/new")
    Call<ResponseCommonData> join(@Body RequestJoinData requestJoinData);

    // 비밀번호 재발급 API
    @POST("/api/member/info/pwd")
    Call<ResponseCommonData> updatePwd(@Body RequestPwdUpdateData requestPwdUpdateData);

    // 회원이름 변경 API
    @POST("/api/member/info/name")
    Call<ResponseCommonData> updateName(@Body RequestNameUpdateData requestPwdUpdateData);

    // 회원정보를 가져오는 API
    @GET("/api/member/info/{id}")
    Call<LoginResponse> memberInfo(@Path("id") String email);

    // 프로필사진을 업로드 하는 API
    // 단일파일을 전송할때는 아래와 같이 사용한다.
    // 안드로이드에서 retrofit으로 이미지나 영상등을 보낼 때에는 Mutipart를 사용
    @Multipart
    @POST("/api/member/profile")
    Call<ResponseCommonData> uploadProfileImg(@Part MultipartBody.Part file, @PartMap Map<String, RequestBody> map);


    // 프로필이미지를 가져오는 API
    @Streaming //용량이 적을 경우 @Streaming은 생략이 가능하다.
    @GET("/api/member/profile/{filename}")
    Call<ResponseBody> downloadImage(@Path("filename") String fileName);


    //회원탈퇴 API
    @DELETE("/api/member/info/{id}/{reasonnum}")
    Call<ResponseCommonData> memberLeave(@Path("id") String email, @Path("reasonnum") int reasonnum);

}
