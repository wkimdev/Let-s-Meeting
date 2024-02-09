package com.wkimdev.letsmeeting;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.util.CommonUtils;

//홈 화면 액티비티
public class HomeActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    //하단메뉴바 선언
    private BottomNavigationView bottomNavi;
    //프래그먼트 사용 선언
    private FragmentManager fm;
    private FragmentTransaction ft;
    private MeetingFrag meetingFrag;
    private ReservationFrag reservationFrag;
    private SettingFrag settingFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.e(TAG, "onCreate: !!!!");

        //뷰바인딩
        bottomNavi = findViewById(R.id.bottomNavi);

        //프래그먼트 객체 생성
        meetingFrag = new MeetingFrag();
        reservationFrag = new ReservationFrag();
        settingFrag = new SettingFrag();

        //Intent로 프래그먼트 이동 지정
        Intent intent = getIntent();
        //회의예약 추가 후 화면 이동
        if (intent.getBooleanExtra("fromAddReservation", false)) {
            //예약화면 프래그먼트 화면 지정
            setFrag(1);
            bottomNavi.setSelectedItemId(R.id.action_reservation);
        } else {
            //첫 프래그먼트 화면 지정
            setFrag(0);
        }

        //하단 메뉴바 리스너 등록
        SettingListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e(TAG, "onResume: !!!!");

        // 로그인 상태가 아니면 앱 초기 화면을 띄운다
        if (CommonUtils.isStringEmpty(PreferenceManager.getString(getApplicationContext(), "email"))) {
            Log.e(TAG, "로그인 상태가 아니면 앱 초기 화면을 띄우는 구문 호출...!!!");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    // 프래그먼트 교체가 일어나는 실행문
    // 프래그먼트 트랜잭션을 수행하는 코드
    private void setFrag(int n) {

        //menuItem.getItemId(R.id.action_reservation);

        //액티비티와 관련된 프래그먼트와 상호작용하기 위해 fragmentManager 반환
        fm = getSupportFragmentManager();
        //fragmentManager와 연결된 일련의 처리과정을 시작
        ft = fm.beginTransaction();

        switch (n) {
            case 0:
                ft.replace(R.id.main_frame, meetingFrag);
                ft.commit(); //commit을 호출해야 내용이 적용된다
                break;

            case 1:
                ft.replace(R.id.main_frame, reservationFrag);
                //item.setChecked(true);
                ft.commit(); //commit을 호출해야 내용이 적용된다
                break;

            case 2:
                ft.replace(R.id.main_frame, settingFrag);
                ft.commit(); //commit을 호출해야 내용이 적용된다
                break;
        }
    }

    //하단메뉴바 클릭 리스너 등록
    private void SettingListener() {
        //선택 리스너 등록
        bottomNavi.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()){
                    case R.id.action_meeting: {
                        item.setChecked(true);
                        setFrag(0);
                        break;
                    }
                    case R.id.action_reservation: {
                        item.setChecked(true);
                        setFrag(1);
                        break;
                    }
                    case R.id.action_setting: {
                        item.setChecked(true);
                        setFrag(2);
                        break;
                    }
                }
                return false;
            }
        });
    }

}