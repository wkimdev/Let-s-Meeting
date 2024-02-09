package com.wkimdev.letsmeeting.join;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.wkimdev.letsmeeting.MainActivity;
import com.wkimdev.letsmeeting.R;

//가입전 개인정보 동의여부 화면
public class AgreementActivity extends AppCompatActivity {

    private static final String TAG = "AgreementActivity";

    // 다음진행 버튼
    public Button nextBtn;
    public Button agree1Btn; //이용약관 보기 버튼
    public Button agree2Btn; //개인정보처리방침 약관보기 버튼

    // 체크박스 체크여부
    public int TERMS_AGREE_1 = 0; // No Check = 0, Check = 1
    public int TERMS_AGREE_2 = 0; // No Check = 0, Check = 1
    public int TERMS_AGREE_3 = 0; // No Check = 0, Check = 1

    // 체크박스
    AppCompatCheckBox check1; // 첫번쨰 동의
    AppCompatCheckBox check2; // 두번쨰 동의
    AppCompatCheckBox check3; // 전체 동의

    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     *  - 뷰 바인딩
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);

        check1 = findViewById(R.id.join_layout_5_check_1);
        check2 = findViewById(R.id.join_layout_5_check_2);
        check3 = findViewById(R.id.join_layout_5_check_3);
        nextBtn = findViewById(R.id.btn_next);
        agree1Btn = findViewById(R.id.join_layout_5_btn_1);
        agree2Btn = findViewById(R.id.join_layout_5_btn_2);
    }


    /**
     * 버튼 이벤트 등록
     */
    @Override
    protected void onResume() {
        super.onResume();

        //이용약관보기
        agree1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AgreementActivity.this, ServiceAgreementActivity.class));
            }
        });

        //개인정보수집 약관보기
        agree2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AgreementActivity.this, PrivacyAgreementActivity.class));
            }
        });

        // 1항동의
        check1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    TERMS_AGREE_1 = 1;
                } else {
                    TERMS_AGREE_1 = 0; //미동의
                    check3.setChecked(false); //모두동의 체크 해제
                }
                checkAgreeStatus();
            }
        });

        // 2항동의
        check2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
              @Override
              public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                  if (isChecked) {
                      TERMS_AGREE_2 = 1;
                  } else {
                      TERMS_AGREE_2 = 0;
                      check3.setChecked(false); //모두동의 체크 해제
                  }
                  checkAgreeStatus();
              }
          }
        );

        // 전체동의
    check3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                  @Override
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                      if (isChecked) {
                          check1.setChecked(true);
                          check2.setChecked(true);
                          TERMS_AGREE_3 = 1;
                      } else {
                          check1.setChecked(false);
                          check2.setChecked(false);
                          TERMS_AGREE_3 = 0;
                      }
                      checkAgreeStatus();
                  }
              }
        );

        // 가입하기 버튼 클릭 (전체동의가 되어있어야 버튼이 활성화 된다.)
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AgreementActivity.this, ConfirmMailAuth.class);
                startActivity(intent);
                finish();
            }
        });
    }

    //약관동의 체크상태 확인
    private void checkAgreeStatus() {
        if (TERMS_AGREE_1 == 1 && TERMS_AGREE_2 == 1) {
            nextBtn.setEnabled(true);
            nextBtn.setBackgroundColor(getResources().getColor(R.color.deepGreen_primary));
        } else {
            nextBtn.setEnabled(false);
            nextBtn.setBackgroundColor(getResources().getColor(R.color.gray));
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