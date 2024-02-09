package com.wkimdev.letsmeeting.util;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.reservation.ReservationActivity;
import com.wkimdev.letsmeeting.setting.SettingActivity;

//공통으로 하단메뉴의 클릭 이벤트를 처리해주는 메소드를 정의한 클래스
public class CommonBNBEvent extends AppCompatActivity  {

    //하단메뉴바 선언
    public BottomNavigationView bottomNavi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_home);

        //뷰바인딩
        bottomNavi = findViewById(R.id.bottomNavi);

    }

    //하단메뉴바 클릭 리스너 등록
    public void settingListener() {
        //선택 리스너 등록
        bottomNavi.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //int id = item.getItemId();
                //Log.e(TAG, "onNavigationItemSelected: "+ id);
                switch(item.getItemId()){
                    case R.id.action_meeting: {
                        //화면유지
                        return true;
                    }
                    case R.id.action_reservation: {
                        startActivity(new Intent(getApplicationContext(), ReservationActivity.class));
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
