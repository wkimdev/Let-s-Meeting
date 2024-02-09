package com.wkimdev.letsmeeting.login;

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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.wkimdev.letsmeeting.HomeActivity;
import com.wkimdev.letsmeeting.MainActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.configure.ServiceConstants;
import com.wkimdev.letsmeeting.model.login.LoginDataResult;
import com.wkimdev.letsmeeting.model.login.LoginResponse;
import com.wkimdev.letsmeeting.model.login.RequestLoginData;
import com.wkimdev.letsmeeting.service.MemberService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 로그인 화면
public class LoginActivity extends AppCompatActivity {

    private String TAG = "LoginActivity";

    private EditText editTxt_email; //입력한 이메일
    private EditText editTxt_password; //입력한 비밀번호
    private Button btn_login; //로그인버튼
    private TextView txt_findPwd; //비밀번호찾기
    private boolean confirmEmail; //이메일 유효성 확인
    private ProgressBar progressBar; //로그아웃시 로딩바호출을 위한 선언
    private Sprite fadingCircle; //로딩애니메이션 선언

    private InputMethodManager imm; //입력제어 매니저 선언

    //로그인 액션을 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);

    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTxt_email = findViewById(R.id.editTxt_email);
        editTxt_password = findViewById(R.id.editTxt_password);
        btn_login = findViewById(R.id.btn_login);
        txt_findPwd = findViewById(R.id.txt_findPwd);

        progressBar = findViewById(R.id.spin_kit);; //로그아웃시 로딩바 노출
        fadingCircle = new FadingCircle(); //노출되는 로딩바 애니메이션 스타일

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

        //이메일을 편집할 때마다 이벤트를 듣는다
        editTxt_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                //이메일입력시 유효성 체크
                String emailValue = editTxt_email.getText().toString().trim();
                if (emailValue.matches(ServiceConstants.EMAIL_VALIDATION_VALUE) && s.length() > 0) {
                    confirmEmail = true;
                } else {
                    confirmEmail = false;
                }
                checkAllInput();

            }
        });


        //이메일이 입력되어 있고, 유효성 체크 완료
        //이메일 입력 후 비밀번호 작성하는 순간 로그인 버튼 활성화
        editTxt_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                checkAllInput();
            }
        });


        //로그인 실행
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String hashPwd = CommonUtils.getHash(editTxt_password.getText().toString().trim());
                RequestLoginData requestLogin = new RequestLoginData(editTxt_email.getText().toString().trim(), hashPwd);

                //레트로핏을 통해 로그인 액션 수행
                startLogin(requestLogin);

                //키보드를 숨겨준다
                // 첫번째 매개변수에 해당하는(여기서는 input) 곳에 키보드가 보이면 키보드를 숨긴다
                // 두번째 매개변수에는 해당 Flag없이 0을 넘겨준다.
                imm.hideSoftInputFromWindow(editTxt_password.getWindowToken(), 0);

            }
        });

        //비밀번호 찾기 화면
        txt_findPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), FindPwdActivity.class));
            }
        });

    }

    //레트로핏으로 로그인액션 수행
    private void startLogin(RequestLoginData request) {

        //enqueue에 파라미터로 넘긴 콜백에서는 통신이 성공/실패 했을때 수행할 동작을 재정의 한다
        service.login(request).enqueue(new Callback<LoginResponse>() {

            //성공했을 경우
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                LoginResponse result = response.body();
                LoginDataResult dataResult = result.getData();


                if (200 == result.getStatusCode()) {//응답성공

                    //닉네임 SharedPreference 에 저장
                    String memberName = dataResult.getMemName();
                    PreferenceManager.setString(getApplicationContext(), "name", memberName);

                    //ID(이메일형식) SharedPreference 에 저장
                    String email = dataResult.getEmail();
                    PreferenceManager.setString(getApplicationContext(), "email", email);

                    //프로필이미지 SharedPreference 에 저장
                    String profile = dataResult.getProfile();
                    PreferenceManager.setString(getApplicationContext(), "profile", profile);


                    //로딩애니메이션 노출
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminateDrawable(fadingCircle); //진행바 모양을 그리는데 사용되는 드로어블 정의

                    // 2초간 멈추게 하고싶다면
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {

                            //로그인 성공시 홈화면으로 이동
                            //Toast.makeText(LoginActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();

                        }
                    }, 2000);


                } else if (201 == result.getStatusCode()) {//로그인실패

                    //로그인 실패시 팝업을 띄운다
                    AlertDialog.Builder ad = new AlertDialog.Builder(LoginActivity.this);

                    ad.setMessage("잘못된 이메일 또는 비밀번호 입니다. \n입력한 정보를 다시 확인해주세요.");
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

            //실패했을 경우
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });
    }


    //모든 인풋값들의 유효성 확인
    public void checkAllInput() {

        String pwdValue = editTxt_password.getText().toString().trim();

        if (confirmEmail && !TextUtils.isEmpty(pwdValue)) {

            //가입버튼 활성화
            btn_login.setEnabled(true);
            btn_login.setBackgroundColor(getResources().getColor(R.color.green_dark));


        } else {

            //가입버튼 바활성화
            btn_login.setEnabled(false);
            btn_login.setBackgroundColor(getResources().getColor(R.color.gray));

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