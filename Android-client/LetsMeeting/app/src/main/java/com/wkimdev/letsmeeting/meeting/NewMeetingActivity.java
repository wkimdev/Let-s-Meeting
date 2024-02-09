package com.wkimdev.letsmeeting.meeting;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.model.meeting.RequestMeeting;
import com.wkimdev.letsmeeting.model.meeting.RequestStatusUpdate;
import com.wkimdev.letsmeeting.model.meeting.ResponseAddMeeting;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeetingInfo;
import com.wkimdev.letsmeeting.service.MeetingService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * 새회의생성 액티비티
 */
public class NewMeetingActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private EditText editTxt_title;
    private TextView txt_startDate;
    private TextView txt_duration;
    private ImageButton imgBtn_duration;
    private Spinner spinner_participantNum;
    private Button btn_cancle;
    private Button btn_startMeeting;

    private TextView txt_checkTitleLength;
    private TextView txt_countTextLength;
    private TextInputLayout edit_meetingTitle;

    private Switch switch_audio;
    private Switch switch_video;

    private String duration;
    private TimePickerDialog.OnTimeSetListener durationCallbackMethod; //회의기간선택 타임피커 콜백처리 변수 선언

    //회의생성을 위한 레트로핏 서비스 API 객체 생성
    private MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);
    private String startDate;

    private String email;
    private String nickname;
    private String title;

    // 진행바
    private ProgressDialog progressDoalog;

    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_meeting);

        editTxt_title = findViewById(R.id.editTxt_title);
        txt_startDate = findViewById(R.id.txt_startDate);
        txt_duration = findViewById(R.id.txt_duration);
        imgBtn_duration = findViewById(R.id.imgBtn_duration);
        edit_meetingTitle = findViewById(R.id.edit_meetingTitle);

        spinner_participantNum = findViewById(R.id.spinner_participantNum);
        btn_cancle = findViewById(R.id.btn_cancle);
        btn_startMeeting = findViewById(R.id.btn_startMeeting);

        switch_audio = findViewById(R.id.switch_audio);
        switch_video = findViewById(R.id.switch_video);

        email = PreferenceManager.getString(NewMeetingActivity.this, "email");
        nickname = PreferenceManager.getString(this, "name");
        editTxt_title.setHint(nickname + "님의 렛츠미팅 회의");

        //회의타이틀 글자 수 제한
        edit_meetingTitle.setCounterMaxLength(100);


        //Calendar를 이용하여 오늘날짜 기준으로 년, 월, 일 정보를 가져온다
        final Calendar cal = Calendar.getInstance();
        int currYear = cal.get(Calendar.YEAR);
        int currMonth = cal.get(Calendar.MONTH);
        int currDay = cal.get(Calendar.DATE);

        //최초화면진입시 회의날짜는 오늘로 셋팅
        setMeetingDate(currYear, currMonth+1, currDay);

        //최초화면진입시 회의기간은 설정
        setDuration(0, 30);


        //time picker dialog 리스너 등록
        this.durationInitializeListener();

    }


    @Override
    protected void onResume() {
        super.onResume();

        //기본로딩바 노출
        progressDoalog = new ProgressDialog(this);
        progressDoalog.setMessage("새 회의 생성 중....");

        //회의 참여 인원수 제한 spinner설정
        ArrayAdapter participantNumAdapter = ArrayAdapter.createFromResource(this,
                R.array.participant_num, R.layout.spinner_layout);
        participantNumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_participantNum.setAdapter(participantNumAdapter);


        //회의기간 설정
        imgBtn_duration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //스피너모드 타임피커
                TimePickerDialog dialog = new TimePickerDialog(
                        NewMeetingActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        durationCallbackMethod, 0, 30, true);

                dialog.setTitle("회의기간선택");
                //기존테마의 배경을 없앤다
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.show();
            }
        });


        //회의 타이틀 입력시 입력포커스 이벤트를 받아 하단에 알림 문구를 띄워준다.
        editTxt_title.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) { //text가 입력될때 하단에 문구 노출
                    edit_meetingTitle.setHelperText("회의명은 100자 이내로 작성해주세요.");
                    edit_meetingTitle.setHelperTextColor(ColorStateList.valueOf(getResources().getColor(R.color.red)));
                } else {
                    edit_meetingTitle.setHelperText("");
                }
            }
        });


        //취소버튼 클릭 후 이전화면으로 이동
        btn_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        //새 회의 생성 후 회의참여화면 진입
        btn_startMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //로딩바 노출
                progressDoalog.show();

                //현재시간 정보
                Date now = new Date();
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                String startTime = dateFormat.format(now);

                //회의참여자 수
                Log.e(TAG, "onClick: 선택한 회의참여 인원수 " + spinner_participantNum.getSelectedItem());
                int participantNum = Integer.parseInt(
                        spinner_participantNum.getSelectedItem().toString().replace("명", ""));

                if (TextUtils.isEmpty(editTxt_title.getText().toString())) {
                    title = nickname + "님의 렛츠미팅 회의";
                } else {
                    title = editTxt_title.getText().toString();
                }

                RequestMeeting requestMeeting = new RequestMeeting(title, startDate, startTime
                                 , txt_duration.getText().toString()
                                 , "KO", email, 0, email, participantNum);
                //회의생성요청
                addMeeting(requestMeeting);

            }
        });
    }

    //회의 생성 처리
    private void addMeeting(RequestMeeting reqMeeting) {
        service.addMeeting(reqMeeting).enqueue(new Callback<ResponseAddMeeting>() {
            @Override
            public void onResponse(Call<ResponseAddMeeting> call, Response<ResponseAddMeeting> response) {
                ResponseAddMeeting joinResponse = response.body();

                if (200 == joinResponse.getStatusCode()) {//성공

                    //회의생성 후 회의상태값 업데이트
                    updateMeetingStatus(email, joinResponse.getMeetingId());

                } else {
                    Toast.makeText(getApplicationContext(), "회의생성이 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseAddMeeting> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });
    }

    //회의날짜 설정: 표기형식: yyyy.MM.dd (ex: 2022.01.01)
    public void setMeetingDate(int year, int month, int day) {
        String startDateValue = year + "." + CommonUtils.convertDateValue(month) + "." + CommonUtils.convertDateValue(day);
        //앱화면에 표기형식: yyyy.MM.dd (ex: 2022.01.01)
        txt_startDate.setText(startDateValue);
        //서버에 요청할 때 포맷(yyyy-mm-dd)
        startDate = CommonUtils.formatDate2(startDateValue);
    }

    //회의상태값 업데이트(close => open)
    private void updateMeetingStatus(String hostId, int meetingId) {
        RequestStatusUpdate requestModel = new RequestStatusUpdate(hostId, meetingId);
        service.updateMeetingStatus(requestModel).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {
                ResponseCommonData result = response.body();
                if (200 == result.getStatusCode()) {

                    // 회의생성 후 로딩바 뜨고 회의상태값 업데이트, 회의정보 조회
                    getMeetingInfo(meetingId);

                }
            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {

            }
        });
    }


    //회의ID로 회의정보 조회
    private void getMeetingInfo(int meetingId) {

        service.getMeetingInfo(meetingId).enqueue(new Callback<ResponseMeetingInfo>() {
            @Override
            public void onResponse(Call<ResponseMeetingInfo> call, Response<ResponseMeetingInfo> response) {

                ResponseMeetingInfo responseMeeting = response.body();

                if (200 == responseMeeting.getStatusCode()) {//성공

                    //회의정보 조회 완료 후 로딩바 사라지도록 처리
                    progressDoalog.dismiss();

                    MeetingDataResult resultData = responseMeeting.getData();

                    Intent intent = new Intent(NewMeetingActivity.this, MeetingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);


                    //닉네임, 오디오설정, 비디오설정 전달
                    intent.putExtra("nickname", nickname);
                    intent.putExtra("muted", switch_audio.isChecked()); //음소거 여부(true: 음소거)
                    intent.putExtra("cameraOff", switch_video.isChecked()); //카메라 연결여부(true: 카메라끔)
                    intent.putExtra("meetingNum", resultData.getMeetingNum()); //회의번호
                    intent.putExtra("maximum", resultData.getParticipantsNum());  //회의최대참여인원수

                    intent.putExtra("hostId", email); //호스트ID
                    intent.putExtra("userEmail", email); //사용자ID(이메일)
                    intent.putExtra("meetingId", meetingId); //회의Id

                    //회의정보 팝업에 노출한 필드값
                    intent.putExtra("meetingTitle", resultData.getTitle()); //회의타이틀
                    intent.putExtra("meetingPwd", resultData.getMeetingPwd()); //회의비밀번호
                    intent.putExtra("duration", resultData.getDuration()); //회의기간

                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(getApplicationContext(), "회의조회가 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseMeetingInfo> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });

    }


    //timePickerDialog에서 선택한 회의기간 정보를 듣는 이벤트
    private void durationInitializeListener() {

        durationCallbackMethod = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                setDuration(hourOfDay, minute);
            }
        };
    }

    //회의기간 설정 - 디폴트 30분
    public void setDuration(int hour, int minute) {
        if (hour == 0) {
            duration = minute + "분";
        } else {
            duration = hour + "시간 " + minute + "분";
        }
        txt_duration.setText(duration);
    }


}