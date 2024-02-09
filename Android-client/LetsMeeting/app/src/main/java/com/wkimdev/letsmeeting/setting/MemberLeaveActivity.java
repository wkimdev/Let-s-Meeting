package com.wkimdev.letsmeeting.setting;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import com.wkimdev.letsmeeting.MainActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.service.MemberService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//회원탈퇴 액티비티
public class MemberLeaveActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private RadioGroup rdGroup_reason; //탈퇴사유 라디오버튼 선택그룹
    private CheckBox chBtn_confirm; //회원탈퇴 동의 확인 체크박스
    private Button btn_leave; //회원탈퇴버튼
    private String email; //로그인계정

    private int reasonNumber = 0; //탈퇴사유번호

    //로그아웃 실행을 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);

    // 회원탈퇴버튼 클릭 후 진행중임을 나타내는 프로그래스바
    private ProgressDialog progressDoalog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_leave);

        rdGroup_reason = findViewById(R.id.rdGroup_reason);
        chBtn_confirm = findViewById(R.id.chBtn_confirm);
        btn_leave = findViewById(R.id.btn_leave);

        email = PreferenceManager.getString(this, "email");
    }

    @Override
    protected void onResume() {
        super.onResume();

        //사용자가 선택한 탈퇴사유 번호를 확인한다
        rdGroup_reason.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.radioBtn_reason1: //사용빈도
                        reasonNumber = 1;
                        break;
                    case R.id.radioBtn_reason2: //불편
                        reasonNumber = 2;
                        break;
                    case R.id.radioBtn_reason3: //개인정보 유출우려
                        reasonNumber = 3;
                        break;
                    case R.id.radioBtn_reason4: //기능빈약
                        reasonNumber = 4;
                        break;
                    case R.id.radioBtn_reason5: //기타
                        reasonNumber = 5;
                        break;
                }
            }
        });


        //유의사항 확인 버튼 클릭
        chBtn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reasonNumber == 0) {
                    //탈퇴사유를 클릭하지 않으면 팝업을 띄운다
                    AlertDialog.Builder ad = new AlertDialog.Builder(MemberLeaveActivity.this);

                    ad.setMessage("탈퇴사유를 선택 해주세요.");
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            chBtn_confirm.setChecked(false);
                        }
                    });
                    ad.show();
                } else {
                    //회원탈퇴 버튼 활성화
                    btn_leave.setEnabled(true);
                    btn_leave.setBackgroundColor(getResources().getColor(R.color.deepGreen_primary));
                }
            }
        });


        //회원탈퇴 버튼 클릭
        btn_leave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //1. 멤버정보 테이블에 탈퇴처리일, 탈퇴사유 저장
                //2. 해당 멤버가 작성한 회의예약내역들 삭제
                //3. 멤버테이블에서 회원정보 삭제하기

                //기본로딩바 노출
                progressDoalog = new ProgressDialog(MemberLeaveActivity.this);
                progressDoalog.setMessage("진행 중...");
                progressDoalog.show();

                //레트로핏을 통해 회원탈퇴 수행
                leaveMember(email, reasonNumber);
            }
        });
    }


    //뒤로가기시 셋팅화면으로 이동
    @Override
    public void onBackPressed() {
        startActivity(new Intent(MemberLeaveActivity.this, SettingActivity.class));
        finish();
    }


    //레트로핏으로 회원탈퇴를 처리
    private void leaveMember(String email, int reasonNumber) {
        service.memberLeave(email, reasonNumber).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                ResponseCommonData result = response.body();

                if (200 == result.getStatusCode()) {//응답성공

                    //로그인 정보 삭제
                    PreferenceManager.removeKey(getApplicationContext(), "name");
                    PreferenceManager.removeKey(getApplicationContext(), "email");
                    PreferenceManager.removeKey(getApplicationContext(), "profile");

                    //프로그레스바가 뜬 다음 화면 이동
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            progressDoalog.dismiss();

                            //회원탈퇴 실행 후 메인화면으로 이동
                            Intent intent = new Intent(MemberLeaveActivity.this, MainActivity.class);

                            /**
                             * 새로운 액티비티를 실행하고, 현재 액티비티를 stack에서 제거하기 위해 아래 옵션들을 줌
                             *
                             * FLAG_ACTIVITY_CLEAR_TOP:
                             *  - 호출하려는 액티비티가 이미 스택에 쌓여있을 때, 새로 인스턴스를 생성하지 않고 기존의 액티비티를 포그라운드로 가져옵니다.
                             *  - 그리고 액티비티스택의 최상단부터 포그라운드로 가져올 액티비티까지의 모든 액티비티를 삭제합니다.
                             * FLAG_ACTIVITY_CLEAR_TASK:
                             *  - 활동이 시작되기 전에 활동과 연관된 기존 작업이 지워지도록 하는 플래그.
                             *  - FLAG_ACTIVITY_NEW_TASK와 같이 사용되어야 함.
                             * FLAG_ACTIVITY_NEW_TASK:
                             *  - 기존에 해당 태스크가 없을 때 새로운 task 를 만들면서 launch 시킨다.
                             *  @참고 -> https://m.blog.naver.com/PostView.naver?isHttpsRedirect=true&blogId=estern&logNo=220012629594
                             **/
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }, 800);

                } else {
                    //로그인 실패시 팝업을 띄운다
                    AlertDialog.Builder ad = new AlertDialog.Builder(MemberLeaveActivity.this);

                    ad.setMessage("오류가 발생했습니다(발생오류 코드 404). \n잠시 후에 다시 시도해주세요.");
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
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });
    }


}