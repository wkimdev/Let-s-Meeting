package com.wkimdev.letsmeeting.join;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.ServiceConstants;
import com.wkimdev.letsmeeting.login.LoginActivity;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.join.RequestJoinData;
import com.wkimdev.letsmeeting.service.MemberService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 가입하기 화면 액티비티
public class JoinActivity extends AppCompatActivity {

    private static final String TAG = "JoinActivity";

    private ScrollView scrollView_join;
    private TextView txt_joinEmail; //가입이메일
    private EditText editTxt_name;   //가입이름
    private EditText editTxt_password; //비밀번호입력
    private EditText editTxt_check_password; //비밀번호 확인값 입력

    //비밀번호 유효성 체크 텍스트
    private TextView txt_check1;
    private TextView txt_check2;
    private String joinEmail; //가입이메일
    private TextView txt_password_match; //비밀번호 확인 일치 문구
    private TextView txt_password_not_match; //비밀번호 확인 미일치 문구
    private TextView txt_nameCheck; //이름입력 요청 문구

    private boolean confirmName; //이름작성 완료여부
    private boolean confirmPwd; //비밀번호 유효성 체크 완료여부
    private Button btn_join; //가입버튼

    //회원가입을 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);

    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     *  - 뷰 바인딩
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        scrollView_join = findViewById(R.id.scrollView_join);
        txt_joinEmail = findViewById(R.id.txt_joinEmail);
        editTxt_name = findViewById(R.id.editTxt_name);
        editTxt_password = findViewById(R.id.editTxt_password);
        editTxt_check_password = findViewById(R.id.editTxt_check_password);
        txt_check1 = findViewById(R.id.txt_check1);
        txt_check2 = findViewById(R.id.txt_check2);
        txt_password_match = findViewById(R.id.txt_password_match);
        txt_password_not_match = findViewById(R.id.txt_password_not_match);
        txt_nameCheck = findViewById(R.id.txt_nameCheck);
        btn_join = findViewById(R.id.btn_join);

        //인텐트로 부터 가입 이메일 확인
        Intent intent = getIntent();
        if (!TextUtils.isEmpty(intent.getStringExtra("joinEmail"))) {
            joinEmail = intent.getStringExtra("joinEmail");
            txt_joinEmail.setText("가입이메일 : " + joinEmail);
        }

    }



    @Override
    protected void onResume() {
        super.onResume();

        //닉네임 입력시, 닉네임 유효성 체크
        editTxt_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 1) {
                    confirmName = true;
                    checkAllInput();
                }
            }
        });

        //비밀번호입력시, 비밀번호 유효성 체크
        editTxt_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String inputPwd = editTxt_password.getText().toString().trim();
                txt_check1.setTextColor(getResources().getColor(R.color.red));
                if (inputPwd.length() >= 8) { //입력한 비밀번호가 8보다 클때
                    txt_check1.setTextColor(getResources().getColor(R.color.green));
                } else {
                    txt_check1.setTextColor(getResources().getColor(R.color.red));
                }

                //비밀번호가 정규식에 부합할때
                if (inputPwd.matches(ServiceConstants.PASSWORD_VALIDATION_VALUE) && s.length() > 0) {
                    txt_check2.setTextColor(getResources().getColor(R.color.green));
                } else {
                    txt_check2.setTextColor(getResources().getColor(R.color.red));
                }

                //비밀번호 확인에 값이 있으면, 그값과 재비교
                String inputCheckPwd = editTxt_check_password.getText().toString().trim();
                if (!TextUtils.isEmpty(inputCheckPwd)) {

                    if (!inputPwd.equals(inputCheckPwd)) { //비밀번호 확인값 불일치

                        txt_password_match.setVisibility(View.INVISIBLE);
                        txt_password_not_match.setVisibility(View.VISIBLE);
                        confirmPwd = true;
                        checkAllInput();

                    } else { //비밀번호 확인값 일차
                        txt_password_match.setVisibility(View.INVISIBLE);
                        txt_password_not_match.setVisibility(View.VISIBLE);
                        confirmPwd = false;
                        checkAllInput();
                    }
                } else {
                    txt_password_match.setVisibility(View.INVISIBLE);
                    txt_password_not_match.setVisibility(View.INVISIBLE);
                }

            }
        });

        //비밀번호 확인시, 위에 입력한 비밀번호와 일치여부 체크
        editTxt_check_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            //유효성 조건에 모두 부합하면 가입하기 버튼 활성화
            @Override
            public void afterTextChanged(Editable s) {
                String inputPwd = editTxt_check_password.getText().toString().trim();
                String firstInputPwd = editTxt_password.getText().toString().trim();
                if (s.length() > 0 && firstInputPwd.equals(inputPwd)) { //비밀번호가 일치함
                    txt_password_match.setVisibility(View.VISIBLE);
                    txt_password_not_match.setVisibility(View.INVISIBLE);
                    confirmPwd = true;
                    checkAllInput();

                } else if (s.length() > 0) { //비밀번호가 일치 하지 않음
                    txt_password_match.setVisibility(View.INVISIBLE);
                    txt_password_not_match.setVisibility(View.VISIBLE);
                    confirmPwd = false;
                    checkAllInput();

                } else {
                    txt_password_match.setVisibility(View.INVISIBLE);
                    txt_password_not_match.setVisibility(View.INVISIBLE);
                    confirmPwd = false;
                    checkAllInput();

                }

            }
        });

        //모든 유효성 체크가 완료된 이후 가입 버튼이 활성화
        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String passwordHash = CommonUtils.getHash(editTxt_password.getText().toString().trim());

                RequestJoinData joinData = new RequestJoinData(
                        joinEmail, passwordHash, editTxt_name.getText().toString().trim()
                );

                //레트로핏으로 회원가입을 수행
                startJoin(joinData);

            }
        });


        // 키보드가 스크롤이 되기 위한 처리
        editTxt_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int[] location = new int[2];
                //View의 x,y좌표값을 반환한다
                editTxt_password.getLocationOnScreen(location);
                Log.e(TAG, "onResume: 비밀번호 텍스트에디터 좌표값 확인 "+ location[0] + ", "+ location[1]);
                //해당 View의 x,y좌표값으로 스크롤 시킨다
                scrollView_join.scrollTo( location[0], location[1] - 200);
            }
        });


        //비밀번호 입력 후 가입하기버튼 위치로 스크롤이 이동되도록 처리해주는 코드
        editTxt_check_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] location = new int[2];
                editTxt_check_password.getLocationOnScreen(location);
                Log.e(TAG, "onResume: 비밀번호 확인 텍스트에디터 좌표값 확인 "+ location[0] + ", "+ location[1]);
                scrollView_join.scrollTo( location[0], location[1] - 200);
            }
        });


    }


    //레트로핏으로 회원가입을 수행하는 메소드
    private void startJoin(RequestJoinData joinData) {

        service.join(joinData).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {
                ResponseCommonData joinResponse = response.body();

                if (200 == joinResponse.getStatusCode()) {//성공

                    // 가입 처리 성공 이후, 로그인 화면으로 이동
                    Toast.makeText(getApplicationContext(), "가입이 완료 되었습니다!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    finish();

                } else {

                    Toast.makeText(getApplicationContext(), "가입이 실패했습니다!", Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();

            }
        });

    }


    //화면의 모든 입력값들이 작성 되어있는지 확안
    public void checkAllInput() {
        //네임값과 비밀번호가 전부 입력되었을때, 버튼 활성화
        //비밀번호까지 입력되었는데, 이름이 입력되지 않았으면 포커스 주고, 입력 유도
        String joinName = editTxt_name.getText().toString().trim();
        if ( confirmName && confirmPwd ) {
            txt_nameCheck.setVisibility(View.INVISIBLE);

            //가입버튼 활성화
            btn_join.setEnabled(true);
            btn_join.setBackgroundColor(getResources().getColor(R.color.green_dark));

        } else if (confirmPwd && joinName.isEmpty()) { //비밀번호만 작성하고 이름값이 없음
            editTxt_name.requestFocus();
            txt_nameCheck.setVisibility(View.VISIBLE);

            //가입버튼 바활성화
            btn_join.setEnabled(false);
            btn_join.setBackgroundColor(getResources().getColor(R.color.gray));

        } else {

            //가입버튼 바활성화
            btn_join.setEnabled(false);
            btn_join.setBackgroundColor(getResources().getColor(R.color.gray));

        }

    }
}