package com.wkimdev.letsmeeting.meeting;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.login.LoginActivity;

//호스트에 의해 회의가 아직 열리지 않았을 경우 노출하는 액티비티
public class NotOpenActivity extends AppCompatActivity {

    private Button btn_moveToBefore;
    private Button btn_moveToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_not_open);

        btn_moveToBefore = findViewById(R.id.btn_moveToBefore);
        btn_moveToLogin = findViewById(R.id.btn_moveToLogin);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //이전화면으로 이동
        btn_moveToBefore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });


        //호스트의 경우 로그인화면으로 이동
        btn_moveToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotOpenActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });


    }
}