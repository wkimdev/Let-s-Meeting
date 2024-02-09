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
import android.widget.TextView;
import android.widget.Toast;

import com.wkimdev.letsmeeting.HomeActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.model.meeting.RequestStatusUpdate;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeetingInfo;
import com.wkimdev.letsmeeting.service.MeetingService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//회의 푸시알림 클릭 후 진입하는 회의준비 화면
public class PushMeetingActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private Button btn_ignore; //무시하기
    private Button btn_enterMeeting; //회의참여버튼

    //회의내용
    private TextView txt_meetingTitle;
    private TextView txt_startTime; //회의시간표기 => 4.12 PM 2:00 (1시간 30분)
    private TextView txt_hostId;
    private SwitchCompat switch_audio;
    private SwitchCompat switch_video;
    private int meetingId; //회의 ID
    private String hostId;
    private String nickname;

    //회의상세정보 호출을 위한 레트로핏 인스턴스 생성
    MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);
    private String meetingNum;
    private int maximum;
    private String meetingTitle;
    private String meetingPwd;
    private String duration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_ready);

        btn_enterMeeting = findViewById(R.id.btn_enterMeeting);
        btn_ignore = findViewById(R.id.btn_ignore);
        txt_meetingTitle = findViewById(R.id.txt_meetingTitle);
        txt_startTime = findViewById(R.id.txt_startTime);
        txt_hostId = findViewById(R.id.txt_hostId);
        switch_audio = findViewById(R.id.switch_audio);
        switch_video = findViewById(R.id.switch_video);

        hostId = PreferenceManager.getString(this, "email");
        nickname = PreferenceManager.getString(this, "name");

        meetingId = getIntent().getIntExtra("meetingId", 0);
        if (meetingId > 0) {
            getMeetingInfo(meetingId);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        //무시하기 후, 회의예약 화면으로 이동
        btn_ignore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                intent.putExtra("fromAddReservation", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });


        //회의참여화면으로 이동
        btn_enterMeeting.setOnClickListener(new View.OnClickListener() {
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
                    Intent intent = new Intent(PushMeetingActivity.this, MeetingActivity.class);

                    //푸시화면으로 부터 들어온 회의화면에서 뒤로가기시 홈으로 이동하기 위한 구분값
                    intent.putExtra("fromPushScreen", true);

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

                    startActivity(intent);

                } else { //응답실패

                    Log.e(TAG, "updateMeetingStatus failed....! " + result.getStatusCode() + " : " + result.getMessage());
                    //오류팝업 발생시킨 후 화면 유지
                    AlertDialog.Builder ad = new AlertDialog.Builder(PushMeetingActivity.this);

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


    //회의상세정보를 호출한 다음 화면에 응답값을 바인딩 한다
    public void getMeetingInfo(int meetingId) {
        service.getMeetingInfo(meetingId).enqueue(new Callback<ResponseMeetingInfo>() {
            @Override
            public void onResponse(Call<ResponseMeetingInfo> call, Response<ResponseMeetingInfo> response) {

                ResponseMeetingInfo responseMeeting = response.body();

                if (200 == responseMeeting.getStatusCode()) {//성공
                    setMeetingData(responseMeeting.getData());
                } else {
                    Toast.makeText(getApplicationContext(), "회의생성이 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseMeetingInfo> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ", t);
            }
        });
    }

    //1) 회의정보 뷰들에 응답받은 회의 데이터 바인딩
    //2) 전역변수들에 회의정보 셋팅
    public void setMeetingData(MeetingDataResult meetingData) {

        meetingNum = meetingData.getMeetingNum();
        maximum = meetingData.getParticipantsNum();
        meetingTitle = meetingData.getTitle();
        meetingPwd = meetingData.getMeetingPwd();
        duration = meetingData.getDuration();

        txt_meetingTitle.setText(meetingData.getTitle());
        //회의시간표기 => 4.12 PM 2:00 (1시간 30분)
        String meetingTime = CommonUtils.convertMeetingDateFormat(meetingData.getStartDate())
                + " (" + duration + ")";
        txt_startTime.setText(meetingTime);
        txt_hostId.setText(meetingData.getHostId()); //회의 호스트 ID
    }
}