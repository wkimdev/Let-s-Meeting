package com.wkimdev.letsmeeting.meeting;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.RequestStatusUpdate;
import com.wkimdev.letsmeeting.service.MeetingService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 회의시작 전 준비 화면(오디오, 비디오꺼짐 설정)
 * 진입경로:  회의세부정보 화면 > 하단 '시작'버튼 클릭 후 진입
 */
public class ReadyMeetingActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private Button btn_cancle;
    private SwitchCompat switch_audio;
    private SwitchCompat switch_video;
    private Button btn_start;
    private String nickname;
    private String meetingNum;
    private int maximum;
    private String hostId;
    private String userEmail;
    private int meetingId;

    private String meetingTitle;
    private String meetingPwd;
    private String duration;

    //회의 상태 업데이트를 위한 레트로핏 서비스 API 객체 생성
    private MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_meeting);

        btn_cancle = findViewById(R.id.btn_cancle);
        switch_audio = findViewById(R.id.switch_audio);
        switch_video = findViewById(R.id.switch_video);
        btn_start = findViewById(R.id.btn_start);


        //회의상세 화면으로부터 회의정보를 전달받는다.
        Intent intent = getIntent();
        nickname = intent.getStringExtra("nickname"); //회의참여 닉네임
        meetingNum = intent.getStringExtra("meetingNum"); //회의번호
        maximum = intent.getIntExtra("maximum", 0); //회의최대참여인원수
        hostId = intent.getStringExtra("hostId"); //호스트ID
        meetingId = intent.getIntExtra("meetingId", 0); //회의Id

        //회의정보 팝업에 노출한 필드값
        meetingTitle = intent.getStringExtra("meetingTitle"); //회의타이틀
        meetingPwd = intent.getStringExtra("meetingPwd"); //회의비밀번호
        Log.e(TAG, "2. 회의비밀번호 확인 : " + meetingPwd);
        duration = intent.getStringExtra("duration"); //회의기간

    }

    @Override
    protected void onResume() {
        super.onResume();

        //취소버튼 클릭 이벤트 처리
        btn_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        //회의 시작 버튼 클릭
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //회의상태값 업데이트 후 회의참여 화면 진입하기
                updateMeetingStatus(hostId, meetingId);
            }
        });
    }

    // 회의상태값 업데이트
    private void updateMeetingStatus(String hostId, int meetingId) {
        RequestStatusUpdate requestStatusUpdate = new RequestStatusUpdate(hostId, meetingId);
        service.updateMeetingStatus(requestStatusUpdate).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                ResponseCommonData result = response.body();
                if (200 == result.getStatusCode()) { //응답성공

                    //회의상태값 open으로 변경 후 회의실 입장
                    Intent intent = new Intent(ReadyMeetingActivity.this, MeetingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    //닉네임, 오디오설정, 비디오설정 전달
                    intent.putExtra("nickname", nickname);
                    intent.putExtra("muted", switch_audio.isChecked()); //음소거 여부(true: 음소거)
                    intent.putExtra("cameraOff", switch_video.isChecked()); //카메라 연결여부(true: 카메라끔)
                    intent.putExtra("meetingNum", meetingNum); //회의번호
                    intent.putExtra("maximum", maximum); //회의최대참여인원수

                    intent.putExtra("hostId", hostId); //호스트ID
                    intent.putExtra("userEmail", hostId); //사용자ID(이메일)
                    intent.putExtra("meetingId", meetingId); //회의Id

                    //회의정보 팝업에 노출한 필드값
                    intent.putExtra("meetingTitle", meetingTitle); //회의타이틀
                    intent.putExtra("meetingPwd", meetingPwd); //회의비밀번호
                    intent.putExtra("duration", duration); //회의기간

                    Log.e(TAG, "3. 회의비밀번호 확인 : " + meetingPwd);

                    startActivity(intent);
                    finish();


                } else { //응답실패

                    Log.e(TAG, "updateMeetingStatus failed....! " + result.getStatusCode() + " : " + result.getMessage());
                    //오류팝업 발생시킨 후 화면 유지
                    AlertDialog.Builder ad = new AlertDialog.Builder(ReadyMeetingActivity.this);

                    ad.setMessage("오류가 발생했습니다.");
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    ad.show();
                }

            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ", t);
                t.printStackTrace();
            }
        });
    }

}