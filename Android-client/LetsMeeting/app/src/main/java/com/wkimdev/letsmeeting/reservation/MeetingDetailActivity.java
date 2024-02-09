package com.wkimdev.letsmeeting.reservation;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.wkimdev.letsmeeting.HomeActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.meeting.ReadyMeetingActivity;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeetingInfo;
import com.wkimdev.letsmeeting.service.MeetingService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//회의상세화면
public class MeetingDetailActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private TextView txt_meetingTitle;
    private TextView txt_meetingDate;
    private TextView txt_meetingDuration;
    private TextView txt_meetingNum;
    private TextView txt_meetingPwd;
    private Button btn_edit;

    private TextView txt_hostId; //회의 호스트ID
    private TextView txt_participantNum; //회의참여인원수 제한 값

    private Button btn_startMeeting;
    private Button btn_delete;

    private int meetingId; //회의ID
    private String startDate;
    private String startTime;

    private ProgressBar progressBar; //로그아웃시 로딩바호출
    private Sprite fadingCircle; //로딩바 애니메이션타입 선언

    //회의상세정보 호출을 위한 레트로핏 인스턴스 생성
    MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_detail);

        Log.e(TAG, "onCreate Called..!");

        txt_meetingTitle = findViewById(R.id.txt_meetingTitle);
        txt_meetingDate = findViewById(R.id.txt_meetingDate);
        txt_meetingDuration = findViewById(R.id.txt_meetingDuration);
        txt_meetingNum = findViewById(R.id.txt_meetingNum);
        txt_meetingPwd = findViewById(R.id.txt_meetingPwd);
        btn_startMeeting = findViewById(R.id.btn_startMeeting);
        btn_delete = findViewById(R.id.btn_delete);
        btn_edit = findViewById(R.id.btn_edit);

        txt_hostId = findViewById(R.id.txt_hostId);
        txt_participantNum = findViewById(R.id.txt_participantNum);

        progressBar = findViewById(R.id.spin_kit);; //삭제시 로딩바 노출
        fadingCircle = new FadingCircle(); //노출되는 로딩바 애니메이션 스타일

        Intent intent = getIntent();
        meetingId = intent.getIntExtra("meetingId", 0);
        if (meetingId > 0) {
            getMeetingInfo(meetingId);
        }

        startDate = intent.getStringExtra("startDate");
        // 오후/오전 hh:mm 포맷으로 뷰에 노출되고,
        // 서버에 저장될때는 hh:mm:ss 형식으로 전달되야 한다
        startTime = intent.getStringExtra("startTime");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e(TAG, "onResume Called..!");


        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //회의수정화면으로 이동시킨다.
                Intent intent = new Intent(MeetingDetailActivity.this, EditReservationActivity.class);
                intent.putExtra("meetingId", meetingId);
                intent.putExtra("startDate", startDate);
                intent.putExtra("startTime", startTime);
                startActivity(intent);
            }
        });


        //삭제 버튼
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //삭제 팝업
                AlertDialog.Builder ad = new AlertDialog.Builder(MeetingDetailActivity.this);
                ad.setMessage("회의를 삭제하면 복구할 수 없습니다. \n 삭제하시겠습니까?");
                ad.setCancelable(false);
                ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        //레트로핏으로 서버 삭제 실행
                        //로딩바 발생 후 회의예약 화면으로 이동
                        deleteMeeting(meetingId);


                    }
                });
                ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                ad.show();
            }
        });

        //회의시작버튼
        btn_startMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MeetingDetailActivity.this, ReadyMeetingActivity.class);

                //닉네임, 오디오설정, 비디오설정 전달
                intent.putExtra("nickname", PreferenceManager.getString(MeetingDetailActivity.this, "name"));
                intent.putExtra("meetingNum", txt_meetingNum.getText().toString()); //회의번호
                int maximum = Integer.parseInt(txt_participantNum.getText().toString().replace("명", ""));
                intent.putExtra("maximum", maximum); //회의최대참여인원수

                intent.putExtra("hostId", PreferenceManager.getString(MeetingDetailActivity.this, "email")); //호스트ID
                intent.putExtra("meetingId", meetingId); //회의Id

                Log.e(TAG, "1. 회의비밀번호 확인 : " + txt_meetingPwd.getText().toString());
                //회의정보 팝업에 노출한 필드값
                intent.putExtra("meetingTitle", txt_meetingTitle.getText()); //회의타이틀
                intent.putExtra("meetingPwd", txt_meetingPwd.getText().toString()); //회의비밀번호
                intent.putExtra("duration", txt_meetingDuration.getText()); //회의기간

                startActivity(intent);
                finish();
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.e(TAG, "onPause called...!");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e(TAG, "onStop Called...! ");
    }

    //레트로핏으로 서버 삭제 실행
    private void deleteMeeting(int meetingId) {

        service.deleteMeeting(meetingId).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {
                ResponseCommonData commonData = response.body();

                //로딩바 발생 후 회의예약 화면으로 이동
                if (200 == commonData.getStatusCode()) {//성공


                    //로딩애니메이션 노출
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminateDrawable(fadingCircle); //진행바 모양을 그리는데 사용되는 드로어블 정의

                    // 1초간 멈추게 하고싶다면
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            //1초 후 앱 처음화면으로 이동
                            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                            intent.putExtra("fromAddReservation", true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();

                        }
                    }, 800);



                } else {
                    Toast.makeText(getApplicationContext(), "회의삭제에 실패했습니다!", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
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

    //회의정보 뷰들에 응답받은 회의 데이터 바인딩
    public void setMeetingData(MeetingDataResult meetingData) {
        txt_meetingTitle.setText(meetingData.getTitle());
        txt_meetingDate.setText(startDate + " " + startTime);
        txt_meetingDuration.setText(meetingData.getDuration());
        txt_meetingNum.setText(meetingData.getMeetingNum());
        txt_meetingPwd.setText(meetingData.getMeetingPwd());
        txt_hostId.setText(meetingData.getHostId()); //회의 호스트 ID
        txt_participantNum.setText(meetingData.getParticipantsNum() + "명"); //회의참여자수
    }

}