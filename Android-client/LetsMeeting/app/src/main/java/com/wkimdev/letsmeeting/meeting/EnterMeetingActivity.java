package com.wkimdev.letsmeeting.meeting;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.wkimdev.letsmeeting.MainActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.model.meeting.RequestStatusUpdate;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeetingInfo;
import com.wkimdev.letsmeeting.service.MeetingService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * 회의화면 전 참여준비 화면
 * - 로그인 여부에 따라 입력폼이 달라진다.
 * - 진입경로:
 *  1) 앱 메인화면 >미로그인 후 회의참여 버튼 클릭
 *  2) 앱 로그인 > 홈 화면 > 상단 회의참여 버튼 클릭
 */
public class EnterMeetingActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    private Button btn_cancle;
    private EditText editTxt_meetingId;
    private EditText editTxt_deviceName;
    private EditText editTxt_meetingPw;
    private Button btn_enter;
    private Button btn_enterWithAuth;
    private Switch switch_audio;
    private Switch switch_video;
    private String userName; //유저닉네임
    private String userEamil; //유저이메일
    private boolean isLogin; //로그인여부

    //회의검증을 위한 레트로핏 서비스 API 객체 생성
    private MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);

    private boolean isMeetingIdActive; //회의ID활성화 여부
    private boolean isUsernameActive; //유저명활성화 여부
    private boolean isPwdActive;      //비밀번호활성화 여부

    private InputMethodManager imm; //입력제어 매니저 선언


    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_meeting);

        btn_cancle = findViewById(R.id.btn_cancle);
        editTxt_meetingId = findViewById(R.id.editTxt_meetingId);
        editTxt_deviceName = findViewById(R.id.editTxt_deviceName);
        editTxt_meetingPw = findViewById(R.id.editTxt_meetingPw);
        btn_enter = findViewById(R.id.btn_enter);
        btn_enterWithAuth = findViewById(R.id.btn_enterWithAuth);
        switch_audio = findViewById(R.id.switch_audio);
        switch_video = findViewById(R.id.switch_video);

        userName = PreferenceManager.getString(getApplicationContext(), "name");
        userEamil = PreferenceManager.getString(getApplicationContext(), "email");

        //Log.e(TAG, "onCreate: name값 확인 " + userName);
        if (!CommonUtils.isStringEmpty(userName)) {
            isLogin = true;
            editTxt_deviceName.setText(userName);
        }

        // 입력받는 방법을 관리하는 Manager객체를  요청하여 InputMethodmanager에 반환한다.
        // 로그인 후 키보드를 내리기 위해 사용한다
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    }


    /**
     * 버튼 이벤트 등록
     */
    @Override
    protected void onResume() {
        super.onResume();


        //로그인을 했으면 1. 닉네임 노출 2. 회의비밀번호 입력폼 미노출
        if (isLogin) {
            editTxt_deviceName.setHint(PreferenceManager.getString(getApplicationContext(), "name"));
            editTxt_meetingPw.setVisibility(View.INVISIBLE);
            btn_enterWithAuth.setVisibility(View.VISIBLE);
            btn_enter.setVisibility(View.INVISIBLE);
        }


        //회의ID입력시 참가버튼 활성화 여부
        editTxt_meetingId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                //회의ID 길이가 넘어가야 참여버튼 활성화
                if (s.length() == 7) { //글도 못쓰도록 막아놓기
                    isMeetingIdActive = true;
                } else {//참가버튼 비활성화
                    isMeetingIdActive = false;
                }
                checkBtnActive();
            }
        });

        //닉네임 입력 확인 이벤트 처리
        editTxt_deviceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 1) {
                    isUsernameActive = true;
                } else {
                    isUsernameActive = false;
                }
                checkBtnActive();
            }
        });


        //회의암호 입력 확인 이벤트 처리
        editTxt_meetingPw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 1) {
                    isPwdActive = true;
                } else {
                    isPwdActive = false;
                }
                checkBtnActive();
            }
        });

        //미로그인시 회의참가
        btn_enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String meetingNum = editTxt_meetingId.getText().toString();
                String meetingPwd = editTxt_meetingPw.getText().toString();
                verifyMeeting(meetingNum, meetingPwd);
            }
        });

        //로그인시 회의참가
        btn_enterWithAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String meetingID = editTxt_meetingId.getText().toString();

                //해당 meetingId가 있는지 서버검증
                //로딩 후 값이 있으면 회의화면으로 이동
                //값이 없으면 예외팝업 발생
                verifyMeeting(meetingID, "");
            }
        });

        //닫기 버튼 클릭
        btn_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    //1.회의번호 및 회의비밀번호가 유효한지 확인
    //2.회의상태값을 확인 후 회의에 입장할 수 있는지 여부 확인
    //3.회의번호가 유효하면 회의참가 처리
    private void verifyMeeting(String meetingNum, String pwd){

        // 다중 쿼리들을 담아서 사용할 Map 인스턴스 생성
        Map<String, String> querys = new HashMap<>();
        querys.put("num", meetingNum);
        querys.put("pwd", pwd);

        service.verifyMeeting(querys).enqueue(new Callback<ResponseMeetingInfo>() {
            @Override
            public void onResponse(Call<ResponseMeetingInfo> call, Response<ResponseMeetingInfo> response) {

                ResponseMeetingInfo result = response.body();
                MeetingDataResult data = result.getData();

                if (303 == result.getStatusCode()) {//응답성공 했으나 일치하는 값이 없음

                    AlertDialog.Builder ad = new AlertDialog.Builder(EnterMeetingActivity.this);
                    String message;
                    if (!CommonUtils.isStringEmpty(pwd)) {
                        message = "회의ID 또는 회의비밀번호를\n다시 확인해주세요.";
                    } else {
                        message = "잘못된 회의ID입니다.\n확인하고 다시 시도해주세요.";
                    }

                    ad.setMessage(message);
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    ad.show();


                } else if (200 == result.getStatusCode()) {

                    //호스트가 아직 회의를 열지 않은 상태
                    if ("close".equals(data.getStatus())) {

                        //호스트인데 회의가 열려있지 않은 경우, 상태값 업데이트 및 회의실 입장
                        if (isLogin && userEamil.equals(data.getHostId())) {
                            //회의상태값 업데이트
                            updateMeetingStatus(userEamil, data.getMeetingId(), meetingNum, data.getParticipantsNum());


                        } else {
                            //호스트가 아니면 회의입장 제한 화면으로 이동.
                            startActivity(new Intent(EnterMeetingActivity.this, NotOpenActivity.class));
                            //입력폼 초기화
                            editTxt_meetingId.setText("");
                            editTxt_deviceName.setText("");
                            editTxt_meetingPw.setText("");
                        }

                    } else {

                        //키보드를 숨겨준다
                        // 첫번째 매개변수에 해당하는(여기서는 input) 곳에 키보드가 보이면 키보드를 숨긴다
                        // 두번째 매개변수에는 해당 Flag없이 0을 넘겨준다.
                        imm.hideSoftInputFromWindow(editTxt_meetingPw.getWindowToken(), 0);

                        Log.e(TAG, "회의검증 성공 >>>> 회의화면으로 진입하기 !!!!");
                        Intent intent = new Intent(EnterMeetingActivity.this, MeetingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);


                        //닉네임, 오디오설정, 비디오설정 전달
                        intent.putExtra("nickname", editTxt_deviceName.getText().toString());
                        intent.putExtra("muted", switch_audio.isChecked()); //음소거 여부(true: 음소거)
                        intent.putExtra("cameraOff", switch_video.isChecked()); //카메라 연결여부(true: 카메라끔)
                        intent.putExtra("meetingNum", meetingNum); //회의번호
                        intent.putExtra("maximum", data.getParticipantsNum());  //회의최대참여인원수

                        intent.putExtra("hostId", data.getHostId()); //호스트ID
                        intent.putExtra("userEmail", userEamil); //사용자ID(이메일)
                        intent.putExtra("meetingId", data.getMeetingId()); //회의Id

                        //회의정보 팝업에 노출한 필드값
                        intent.putExtra("meetingTitle", data.getTitle()); //회의타이틀
                        intent.putExtra("meetingPwd", data.getMeetingPwd()); //회의비밀번호
                        intent.putExtra("duration", data.getDuration()); //회의기간

                        startActivity(intent);
                        finish();
                    }

                } else {//실패
                    Log.e(TAG, "서버 응답 실패 :" + result.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseMeetingInfo> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ", t);
                t.printStackTrace();
            }
        });
    }

    // 회의상태값 업데이트
    private void updateMeetingStatus(String hostId, int meetingId,
                                        String meetingNum, int participantsNum) {
        RequestStatusUpdate requestStatusUpdate = new RequestStatusUpdate(hostId, meetingId);
        service.updateMeetingStatus(requestStatusUpdate).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                ResponseCommonData result = response.body();
                if (200 == result.getStatusCode()) { //응답성공

                    //회의상태값 open으로 변경후 회의실 입장
                    Intent intent = new Intent(EnterMeetingActivity.this, MeetingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                    //닉네임, 오디오설정, 비디오설정 전달
                    intent.putExtra("nickname", editTxt_deviceName.getText().toString());
                    intent.putExtra("muted", switch_audio.isChecked()); //음소거 여부(true: 음소거)
                    intent.putExtra("cameraOff", switch_video.isChecked()); //카메라 연결여부(true: 카메라끔)
                    intent.putExtra("meetingNum", meetingNum); //회의번호
                    intent.putExtra("maximum", participantsNum);  //회의최대참여인원수

                    intent.putExtra("hostId", hostId); //호스트ID
                    intent.putExtra("userEmail", userEamil); //사용자ID(이메일)
                    intent.putExtra("meetingId", meetingId); //회의Id

                    startActivity(intent);
                    finish();

                } else { //응답실패

                    Log.e(TAG, "updateMeetingStatus failed....! " + result.getStatusCode() + " : " + result.getMessage());
                    //오류팝업 발생시킨 후 화면 유지
                    AlertDialog.Builder ad = new AlertDialog.Builder(EnterMeetingActivity.this);

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


    // 참가버튼 활성화 여부 확인
    public void checkBtnActive() {

        if (isLogin) {//로그인했을 경우 활성화 조건체크
            if (isMeetingIdActive) {
                btn_enterWithAuth.setEnabled(true);
                btn_enterWithAuth.setBackgroundColor(getResources().getColor(R.color.green_dark));
            } else {
                btn_enterWithAuth.setEnabled(false);
                btn_enterWithAuth.setBackgroundColor(getResources().getColor(R.color.gray));
            }
        } else {//미로그인했을 경우 활성화 조건체크

            //비밀번호 입력의 모든 항목이 체크 되었을 경우 저장 버튼 활성화
            if (isMeetingIdActive && isUsernameActive && isPwdActive) {
                btn_enter.setEnabled(true);
                btn_enter.setBackgroundColor(getResources().getColor(R.color.green_dark));
            } else {
                btn_enter.setEnabled(false);
                btn_enter.setBackgroundColor(getResources().getColor(R.color.gray));
            }

        }
    }

    //MainActivity에 nohistory옵션을 줬기 때문에, 뒤로가기시 메인화면 이동 처리를 따로 함
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}