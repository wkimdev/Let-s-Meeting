package com.wkimdev.letsmeeting.setting;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.configure.ServiceConstants;
import com.wkimdev.letsmeeting.login.LoginActivity;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.auth.RequestPwdUpdateData;
import com.wkimdev.letsmeeting.model.login.LoginResponse;
import com.wkimdev.letsmeeting.service.MemberService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 비밀번호 변경 화면
public class ChangePwdActivity extends AppCompatActivity {

    private static final String TAG = "ChangePwdActivity";
    private EditText editTxt_currentPwd; //현재비밀번호
    private EditText editTxt_newPwd; //새비밀번호
    private EditText editTxt_checkPwd; //새비밀번호 확인
    private Button btn_savePwd; //비밀번호 저장버튼

    private String email; //계정이메일
    private String newPwdValue; //새비밀번호값
    private String checkPwdValue; //비밀번호확인 값

    private boolean isCurrentPwdActive; //현재비밀번호가 작성되었는지 여부
    private boolean isNewPwdActive; //새비밀번호가 작성되었는지 여부
    private boolean isCheckPwdActive; //새비밀번호 확인란이 작성되었는지 여부

    private ProgressBar progressBar; //비밀번호 변경 성공 후 로딩바호출
    private Sprite fadingCircle;    //노출되는 로딩바 애니메이션 스타일

    private InputMethodManager imm; //입력제어 매니저 선언

    //이름변경을 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pwd);

        editTxt_currentPwd = findViewById(R.id.editTxt_currentPwd);
        editTxt_newPwd = findViewById(R.id.editTxt_newPwd);
        editTxt_checkPwd = findViewById(R.id.editTxt_checkPwd);
        btn_savePwd = findViewById(R.id.btn_savePwd);

        progressBar = findViewById(R.id.spin_kit);; //로딩바 뷰 바인딩
        fadingCircle = new FadingCircle(); //노출되는 로딩바 애니메이션 스타일

        // 입력받는 방법을 관리하는 Manager객체를  요청하여 InputMethodmanager에 반환한다.
        // 로그인 후 키보드를 내리기 위해 사용한다
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        Intent intent = getIntent();
        if(!TextUtils.isEmpty(intent.getStringExtra("email"))){
            email = intent.getStringExtra("email");
        }

        //현재비밀번호 작성여부
        editTxt_currentPwd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 0) {
                    isCurrentPwdActive = true;
                } else {
                    isCurrentPwdActive = false;
                }
                checkBtnActive();
            }
        });

        //새비밀번호 작성여부
        editTxt_newPwd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 0) {
                    isNewPwdActive = true;
                } else {
                    isNewPwdActive = false;
                }
                checkBtnActive();

            }
        });


        //새비밀번호확인 작성여부
        editTxt_checkPwd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 0) {
                    isCheckPwdActive = true;
                } else {
                    isCheckPwdActive = false;
                }
                checkBtnActive();
            }
        });


        //비밀번호 변경 후, 로그아웃 해야 한다는 팝업을 띄운 뒤 로그아웃을 시킨다
        //1) 새 비밀번호와 비밀번호 확인이 일치하지 않습니다 -> 저장 버튼 클릭 후 팝업 발생
        //2) 이전 비밀번호가 잘못되엇습니다 -> 저장 버튼 클릭 후 체크 함
        btn_savePwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 입력받는 방법을 관리하는 Manager객체를 요청하여 InputMethodmanager에 반환한다.
                // 로그인 후 키보드를 내리기 위해 사용한다
                imm.hideSoftInputFromWindow(editTxt_checkPwd.getWindowToken(), 0);

                //1) 새비밀번호 유효성 체크 확인
                //2) 새비밀번호 일치 확인
                newPwdValue = editTxt_newPwd.getText().toString();
                checkPwdValue = editTxt_checkPwd.getText().toString();

                //새비밀번호와 새비밀번호 확인값이 같은지 체크함
                if(checkPwdValidation(newPwdValue, checkPwdValue)) {
                    //3) 이전 비밀번호 일치 여부 확인
                    getMemberInfo(email, editTxt_currentPwd.getText().toString());
                };
            }

        });
    }

    //새비밀번호와 새비밀번호 확인값이 같은지 체크
    private boolean checkPwdValidation(String newPwdValue, String checkPwdValue) {

        //8자 이상, 문자 숫자 1개 이상포함되었는지 확인
        if (!(newPwdValue.matches(ServiceConstants.PASSWORD_VALIDATION_VALUE) && newPwdValue.length() >= 8)) {
            editTxt_newPwd.requestFocus();
            showDialogPopup("비밀번호는 숫자 또는 문자 포함 및 8자 이상이어야 합니다!");
            return false;
        }

        //비밀번호 일치 여부 확인
        if (!newPwdValue.equals(checkPwdValue)) {
            editTxt_checkPwd.requestFocus();
            showDialogPopup("새비밀번호값이 일치하지 않습니다");
            return false;
        }
        return true;
    }


    // 요청값 (이메일)과 확인값(현재비밀번호)을 던져서, 값이 같은지 비교하기
    private void getMemberInfo(String email, String currPwd) {
        service.memberInfo(email).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                LoginResponse result = response.body();

                if (result.getData().getPassword().equals(CommonUtils.getHash(currPwd))) {
                    //로딩바 뜨고 팝업 발생
                    AlertDialog.Builder ad = new AlertDialog.Builder(ChangePwdActivity.this);
                    ad.setMessage("비밀번호를 변경하면, 다른 장치의 렛츠미팅에서도 로그아웃이 됩니다. \n 계속 하시겠습니까?");
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            //비밀번호 난수화
                            String passwordHash = CommonUtils.getHash(newPwdValue);
                            RequestPwdUpdateData requestPwdUpdateData = new RequestPwdUpdateData(email, passwordHash);
                            //비밀번호 업데이트 실행
                            updatePwd(requestPwdUpdateData);

                        }
                    });
                    ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    ad.show();
                } else {
                    showDialogPopup("이전 비밀번호가 잘못되습니다");
                    editTxt_currentPwd.requestFocus();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
            }
        });
    }


    //레트로핏으로 비밀번호 업데이트를 수행하는 메소드
    private void updatePwd(RequestPwdUpdateData requestPwdUpdateData) {

        service.updatePwd(requestPwdUpdateData).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                ResponseCommonData result = response.body();

                //비밀번호 변경이 성공했을 경우, 로딩바 발생 후 메인화면으로 이동
                if (result.getStatusCode() == 200) {

                    //로딩애니메이션 노출
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminateDrawable(fadingCircle); //진행바 모양을 그리는데 사용되는 드로어블 정의

                    // 1.5초간 멈추게 하고싶다면
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {

                            //로그인 정보 삭제
                            PreferenceManager.removeKey(getApplicationContext(), "name");
                            PreferenceManager.removeKey(getApplicationContext(), "email");
                            PreferenceManager.removeKey(getApplicationContext(), "profile");

                            //1.5초 후 로그아웃 시키고, 로그인 화면으로 이동
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();

                        }
                    }, 1500);

                } else {
                    // 실패했을 경우
                    AlertDialog.Builder ad = new AlertDialog.Builder(ChangePwdActivity.this);
                    ad.setMessage("오류 발생!");
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



    // "이전 비밀번호가 잘못되습니다" 다이알로그팝업을 노출시키는 팝업 공통 메소드
    public void showDialogPopup(String message) {

        AlertDialog.Builder ad = new AlertDialog.Builder(ChangePwdActivity.this);
        ad.setMessage(message);
        ad.setCancelable(false);
        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.show();

    }


    // 비밀번호 변경 버튼 활성화 여부 확인
    public void checkBtnActive() {

        //비밀번호 입력의 모든 항목이 체크 되었을 경우 저장 버튼 활성화
        if (isCurrentPwdActive && isNewPwdActive && isCheckPwdActive) {
            btn_savePwd.setEnabled(true);
            btn_savePwd.setBackgroundColor(getResources().getColor(R.color.green_dark));
        } else {
            btn_savePwd.setEnabled(false);
            btn_savePwd.setBackgroundColor(getResources().getColor(R.color.gray));
        }
    }
}