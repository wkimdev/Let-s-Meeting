package com.wkimdev.letsmeeting.setting;


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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.auth.RequestNameUpdateData;
import com.wkimdev.letsmeeting.service.MemberService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 이름 변경 화면
public class ChangeNameActivity extends AppCompatActivity {

    private String TAG = "ChangeNameActivity";

    private EditText txt_currentName; //현재계정이름
    private EditText editTxt_changeName; //변경할 계정이름
    private Button btn_saveName; //변경된 이름 저장 버튼
    private String email; //계정이메일
    private String changeName; //변경된 이름

    private ProgressBar progressBar; //로그아웃시 로딩바호출
    private Sprite fadingCircle;    //노출되는 로딩바 애니메이션 스타일

    //이름변경을 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_name);

        editTxt_changeName = findViewById(R.id.editTxt_changeName);
        btn_saveName = findViewById(R.id.btn_saveName);

        txt_currentName = findViewById(R.id.txt_currentName);

        progressBar = findViewById(R.id.spin_kit);; //이름변경시 로딩바 노출
        fadingCircle = new FadingCircle(); //노출되는 로딩바 애니메이션 스타일


        Intent intent = getIntent();
        if(!TextUtils.isEmpty(intent.getStringExtra("email"))){
            email = intent.getStringExtra("email");
            txt_currentName.setText(intent.getStringExtra("currentName"));
        }


        //변경할 이름을 클릭하는 순간 저장버튼 활성화
        //저장버튼 클릭 후, DB 업데이트, 로딩바 발생, 이전화면 이동, 변경된 이름 노출
        editTxt_changeName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {

                    //저장버튼 활성화
                    btn_saveName.setEnabled(true);
                    btn_saveName.setBackgroundColor(getResources().getColor(R.color.green_dark));
                } else {
                    //저장버튼 비활성화
                    btn_saveName.setEnabled(false);
                    btn_saveName.setBackgroundColor(getResources().getColor(R.color.gray));
                }

            }
        });

        //변경한 이름저장
        btn_saveName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                changeName = editTxt_changeName.getText().toString().trim();

                //레트로핏을 통해 변경된 이름 저장
                RequestNameUpdateData requestData = new RequestNameUpdateData(email, changeName);
                changeName(requestData, changeName);

            }
        });

    }

    //레트로핏을 통해 이름을 변경하는 메소드
    private void changeName(RequestNameUpdateData requestNameUpdateData, String changeName) {

        //enqueue에 파라미터로 넘긴 콜백에서는 통신이 성공/실패 했을때 수행할 동작을 재정의 한다
        service.updateName(requestNameUpdateData).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                ResponseCommonData result = response.body();


                //로딩바 띄운후, 이전화면으로 이동 (변경된 이름 인텐트로 전달)
                if (200 == result.getStatusCode()) {//응답성공

                    //로딩애니메이션 노출
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminateDrawable(fadingCircle); //진행바 모양을 그리는데 사용되는 드로어블 정의

                    // 1.5초간 멈추게 하고싶다면
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {

                            //변경된 값 SharedPreference에 업데이트
                            PreferenceManager.setString(getApplicationContext(), "name", changeName);


                            //1.5초 후 이전화면 이동
                            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("changeName", changeName);
                            startActivity(intent);
                            finish();

                        }
                    }, 1500);


                } else {//응답실패


                    AlertDialog.Builder ad = new AlertDialog.Builder(ChangeNameActivity.this);
                    ad.setMessage("오류가 발생했습니다. 다시 시도해주세요.");
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
                Log.e(TAG, "레트로핏 onFailure 응답 오류 발생: ");
                t.printStackTrace();
            }
        });
    }

}