package com.wkimdev.letsmeeting;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.join.AgreementActivity;
import com.wkimdev.letsmeeting.login.LoginActivity;
import com.wkimdev.letsmeeting.meeting.EnterMeetingActivity;
import com.wkimdev.letsmeeting.util.CommonUtils;

// 앱 시작시 뜨는 화면
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button btnEnterMeeting; //회의참가버튼
    private Button btnJoin; // 가입버튼
    private Button btnLogin; // 로그인버튼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnJoin = findViewById(R.id.btn_join);
        btnLogin = findViewById(R.id.btn_login);
        btnEnterMeeting = findViewById(R.id.btn_enterMeeting);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: call...!");

        //로그인이 되어 있으면, 홈 화면으로 이동
        if (!CommonUtils.isStringEmpty(PreferenceManager.getString(getApplicationContext(), "email"))) {
            Log.e(TAG, "메인화면에서 로그인이 되어 있으면, 홈 화면으로 이동하도록 처리 됨!!!!");
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        // 회의참가버튼
        btnEnterMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EnterMeetingActivity.class));
            }
        });

        // 가입하기 화면 이동
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AgreementActivity.class));
            }
        });

        // 로그인 화면 이동
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });
    }

}