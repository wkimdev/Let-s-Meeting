package com.wkimdev.letsmeeting.reservation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.wkimdev.letsmeeting.HomeActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.setting.SettingActivity;

//회의 예약 화면
public class ReservationActivity extends AppCompatActivity {

    //하단메뉴바 선언
    private BottomNavigationView bottomNavi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        //뷰바인딩
        bottomNavi = findViewById(R.id.bottomNavi);
        SettingListener(); //리스너 등록

        //맨 처음 시작할 탭 설정
        bottomNavi.setSelectedItemId(R.id.action_reservation);
    }

    //하단메뉴바 클릭 리스너 등록
    private void SettingListener() {
        //선택 리스너 등록
        bottomNavi.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()){
                    case R.id.action_meeting: {
                        startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                        return true;
                    }
                    case R.id.action_reservation: {
                        //화면유지
                        return true;
                    }
                    case R.id.action_setting: {
                        startActivity(new Intent(getApplicationContext(), SettingActivity.class));
                        return true;
                    }
                }
                return false;
            }
        });
    }


}
