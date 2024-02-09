package com.wkimdev.letsmeeting.reservation;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.wkimdev.letsmeeting.HomeActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.meeting.RequestMeeting;
import com.wkimdev.letsmeeting.model.meeting.ResponseAddMeeting;
import com.wkimdev.letsmeeting.service.MeetingService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//회의예약 화면
public class AddReservationActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private Button btn_saveName; //저장버튼
    private EditText editTxt_title; //회의타이틀

    private TextView txt_startDate; //회의시작날짜 노출

    private ImageButton imgBtn_startDate; //회의시작날자 선택 버튼
    private ImageButton imgBtn_startTime; //회의시작시간 선택 버튼
    private ImageButton imgBtn_duration; //회의기간 선택 버튼
    private ImageButton imgBtn_standTime; //회의 표준시간대 선택 버튼

    private TextView txt_startTime; //회의시작 시간
    private TextView txt_duration; //회의시작 기간
    private TextView txt_standTime; //표준시간대


    private DatePickerDialog.OnDateSetListener callbackMethod; //회의날짜 데이트피커 콜백처리 변수 선언
    private TimePickerDialog.OnTimeSetListener timeCallbackMethod; //회의시간선택 타임피커 콜백처리 변수 선언
    private TimePickerDialog.OnTimeSetListener durationCallbackMethod; //회의기간선택 타임피커 콜백처리 변수 선언

    //오늘날짜 변수 선언
    int currYear, currMonth, currDay;
    //현재시간 변수선언
    int currHour, currMinute;
    //회의날짜가 오늘인지 판단
    boolean isMeetingDateToday = true;

    private String name; //사용자이름
    private String hintTitle; //회의타이틀 디폴트
    private String startDate; //회의시작일
    private String startTime; //회의시작시간
    private String duration; //회의진행시간
    private String gmt; //표준시간
    private String title; //회의타이틀

    //시스템에서 알람 서비스를 제공하도록 도와주는 클래스
    //특정 시점에 알람이 울리도록 도와준다
    private AlarmManager alarmManager;

    private Spinner spinner_participantNum; //회의참여인원수 제한

    //회의생성을 위한 레트로핏 서비스 API 객체 생성
    private MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reservation);

        btn_saveName = findViewById(R.id.btn_saveName);
        editTxt_title = findViewById(R.id.editTxt_title);
        imgBtn_startDate = findViewById(R.id.imgBtn_startDate);
        imgBtn_startTime = findViewById(R.id.imgBtn_startTime);
        imgBtn_duration = findViewById(R.id.imgBtn_duration);
        //imgBtn_standTime = findViewById(R.id.imgBtn_standTime);
        txt_startDate = findViewById(R.id.txt_startDate);
        txt_startTime =findViewById(R.id.txt_startTime);
        txt_duration = findViewById(R.id.txt_duration);
        //txt_standTime = findViewById(R.id.txt_standTime);
        spinner_participantNum = findViewById(R.id.spinner_participantNum);


        //Calendar를 이용하여 오늘날짜 기준으로 년, 월, 일 정보를 가져온다
        final Calendar cal = Calendar.getInstance();
        currYear = cal.get(Calendar.YEAR);
        currMonth = cal.get(Calendar.MONTH);
        currDay = cal.get(Calendar.DATE);

        //현재 시간
        LocalTime now = LocalTime.now();
        // 시, 분 구하기
        currHour = now.getHour();
        currMinute = now.getMinute();

        //최초화면진입시 회의날짜는 오늘로 셋팅
        setMeetingDate(currYear, currMonth+1, currDay);

        //최초화면진입시 회의시간은 현재시간 기준으로 셋팅
        setMeetingTime(currHour, currMinute);

        //최초화면진입시 회의기간은 설정
        setDuration(0, 30);

        //푸시알림을 보내기 위해, 시스템에서 알림 서비스를 생성해주는 코드
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        //date, tiem picker dialog 리스너 등록
        this.InitializeListener();
        this.timeInitializeListener();
        this.durationInitializeListener();

    }


    @Override
    protected void onResume() {
        super.onResume();

        name = PreferenceManager.getString(getApplicationContext(), "name");
        hintTitle = name + "님의 렛츠미팅 회의";
        editTxt_title.setHint(hintTitle);

        //회의시작날짜 선택
        imgBtn_startDate.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {

                //날짜선택 다이알로그 창을 띄운다
                //올해까지만 선택가능 하고, 이전 날짜는 선택 못한다
                DatePickerDialog dialog = new DatePickerDialog(AddReservationActivity.this,
                        callbackMethod, currYear, currMonth, currDay);

                Calendar minDate = Calendar.getInstance(); //캘린더에서 선택할 수 있는 최소날짜
                //Calendar maxDate = Calendar.getInstance(); //캘린더에서 선택할 수 있는 최대날짜

                //선택가능 시작일 - 오늘
                minDate.set(currYear, currMonth, currDay);
                //밀리세컨드로 날짜변환 후 Calendar's 타임으로 변환
                dialog.getDatePicker().setMinDate(minDate.getTime().getTime());
                dialog.show();
            }
        });

        //회의시작시간 선택
        imgBtn_startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //스피너모드 타임피커 - 현재시간 이전은 선택할 수 없다
                TimePickerDialog dialog = new TimePickerDialog(
                        AddReservationActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        timeCallbackMethod, currHour, currMinute, false);

                dialog.setTitle("회의시간선택");
                //기존테마의 배경을 없앤다
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.show();
            }
        });

        //회의기간 선택
        imgBtn_duration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //스피너모드 타임피커 - 현재시간 이전은 선택할 수 없다
                TimePickerDialog dialog = new TimePickerDialog(
                        AddReservationActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        durationCallbackMethod, 0, 30, true);

                dialog.setTitle("회의기간선택");
                //기존테마의 배경을 없앤다
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.show();

            }
        });


        //표준시간대 선택
//        imgBtn_standTime.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                //표준시간대 화면 이동
//                startActivity(new Intent(AddReservationActivity.this, ChangeGMTActivity.class));
//
//            }
//        });


        //회의 참여 인원수 제한 spinner설정
        ArrayAdapter participantNumAdapter = ArrayAdapter.createFromResource(this,
                R.array.participant_num, R.layout.spinner_layout);
        participantNumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_participantNum.setAdapter(participantNumAdapter);


        //회의저장 이벤트 발생 후 레트로핏으로 서버에 회의저장 요청
        btn_saveName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //회의명이 없으면 디폴트값을 넣는다
                if (TextUtils.isEmpty(editTxt_title.getText().toString())) {
                    title = hintTitle;
                } else {
                    title = editTxt_title.getText().toString();
                }

                Log.e(TAG, "onClick: 선택한 회의참여 인원수 " + spinner_participantNum.getSelectedItem());
                int participantNum = Integer.parseInt(spinner_participantNum.getSelectedItem().toString().replace("명", ""));

                String email = PreferenceManager.getString(getApplicationContext(), "email");
                RequestMeeting requestMeeting = new RequestMeeting(title, startDate, startTime,
                        duration, "KO", email, 0, email, participantNum);
                addMeeting(requestMeeting);

            }
        });
    }

    //회의시간이 되면 사용자가에게 푸시알림을 보내주기 위해, 알림을 등록해주는 코드
    //회의시간을 yyyy-mm-dd hh:mm:ss 포맷으로 전달한다
    private void setNotice(String startDate, String startTime, String title, int meetingId) {
        Log.e(TAG, "setNotice 등록!!!");
        String meetingTime = startDate + " " + startTime;

        //NotificationReceiver에 알림내용을 전달
        //알림메세지 내용 => "리즈의 회의", 오후 __시에 시작됨
        String timeValue = CommonUtils.formatTimeToAMPM(startTime);
        Log.e(TAG, "알림메세지에 등록되는 시간 값 확인 : " + timeValue);

        //알람을 수신할 수 있도록 하는 리시버로 인텐트 요청
        Intent receiverIntent = new Intent(this, NotificationReceiver.class);
        receiverIntent.putExtra("content", title + ", " + timeValue + " 에 시작됨" );
        receiverIntent.putExtra("meetingId", meetingId);

        /**
         * PendingIntent란?
         * - Notification으로 작업을 수행할 때 인텐트가 실행되도록 합니다.
         * Notification은 안드로이드 시스템의 NotificationManager가 Intent를 실행합니다.
         * 즉 다른 프로세스에서 수행하기 때문에 Notification으로 Intent수행시 PendingIntent의 사용이 필수 입니다.
         */

        /**
         * 브로드캐스트로 실행될 pendingIntent선언 한다.
         * Intent가 새로 생성될때마다(알람을 등록할 때마다) intent값을 업데이트 시키기 위해, FLAG_UPDATE_CURRENT 플래그를 준다
         * 이전 알람을 취소시키지 않으려면 requestCode를 다르게 줘야 하기 때문에 meetingID를 requestCode로 등록해서 요청한다.
         * */
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, meetingId,
                receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        //날짜 포맷을 바꿔주는 소스코드
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datetime = null;
        try {
            datetime = dateFormat.parse(meetingTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(datetime);

        //알람시간 설정
        //param 1)알람의 타입
        //param 2)알람이 울려야 하는 시간(밀리초)을 나타낸다.
        //param 3)알람이 울릴 때 수행할 작업을 나타냄
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }


    //레트로핏으로 서버에 회의생성 요청
    public void addMeeting(RequestMeeting requestMeeting) {
        service.addMeeting(requestMeeting).enqueue(new Callback<ResponseAddMeeting>() {
            @Override
            public void onResponse(Call<ResponseAddMeeting> call, Response<ResponseAddMeeting> response) {

                ResponseAddMeeting joinResponse = response.body();

                if (200 == joinResponse.getStatusCode()) {//성공

                    //회의시간 알림 등록
                    //meetingId를 리턴해, noti 등록
                    setNotice(startDate ,startTime, title, joinResponse.getMeetingId());

                    // 회의생성 후 로딩바가 뜬 다음, 예약화면으로 이동
                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("fromAddReservation", true);
                    startActivity(intent);
                    finish();

                    //Toast.makeText(getApplicationContext(), "회의생성이 성공했습니다!", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(), "회의생성이 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseAddMeeting> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });
    }


    //datePickerDialog에서 선택한 날짜 정보를 듣는 이벤트
    public void InitializeListener()
    {
        callbackMethod = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                monthOfYear++; //달력날짜가 한달전으로 노출되어 변경

                Log.e(TAG, "onDateSet: "+ year + "년" + monthOfYear + "월" + dayOfMonth + "일" );
                //선택한 날짜를 포맷에 맞게 표기해주는 메소드
                setMeetingDate(year, monthOfYear, dayOfMonth);
            }
        };
    }


    //timePickerDialog에서 선택한 회의시간 정보를 듣는 이벤트
    public void timeInitializeListener() {
        timeCallbackMethod = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Log.e(TAG, "onTimeSet: 시간 " + hourOfDay + ", 분 " + minute );

                //회의날짜와 현재시간을 같이 비교하여, 현재시간 이후로 회의시간을 설정할 수 있게 만든다
                if (hourOfDay <= currHour && minute <= currMinute && isMeetingDateToday) {
                    Log.e(TAG, "현재시간 이후로 선택해주세요!!");
                    Toast.makeText(AddReservationActivity.this, "현재시간 이후로 선택해주세요!", Toast.LENGTH_SHORT).show();
                } else {
                    setMeetingTime(hourOfDay, minute);
                }
            }
        };
    }

    //timePickerDialog에서 선택한 회의기간 정보를 듣는 이벤트
    private void durationInitializeListener() {

        durationCallbackMethod = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Log.e(TAG, "회의기간 onTimeSet: 시간 " + hourOfDay + ", 분 " + minute );
                setDuration(hourOfDay, minute);
            }
        };
    }


    //회의날짜 설정
    public void setMeetingDate(int year, int month, int day) {
        //표기형식: yyyy.MM.dd (ex: 2022.01.01)
        String startDateValue = year + "." + CommonUtils.convertDateValue(month) + "." + CommonUtils.convertDateValue(day);

        //현재 month보다 한달전으로 들어오는 이슈로 currMonth+1로 처리함.
        if (year == currYear && month == (currMonth+1) && day == currDay) {
            //startDateValue = year + "." + month + "." + day + " (오늘)";
            isMeetingDateToday = true;
        } else {
            //startDateValue = year + "." + month + "." + day;
            isMeetingDateToday = false;
        }
        //앱화면에 표기형식: yyyy.MM.dd (ex: 2022.01.01)
        txt_startDate.setText(startDateValue);

        //서버에 요청할 때 포맷(yyyy-mm-dd)
        startDate = year + "-" + CommonUtils.convertDateValue(month) + "-" + CommonUtils.convertDateValue(day);
    }


    //회의시간 설정
    //@TODO: 다른 방식으로 사용되도록 수정되어야 함
    public void setMeetingTime(int hour, int minute) {

        String AM_PM;
        String minuteValue = "";
        String hourStringValue = "";
        int hourValue = 0;

        //10자리 이내의 경우 앞에 0을 붙이는 처리
        hourStringValue = CommonUtils.convertDateValue(hour);
        minuteValue = CommonUtils.convertDateValue(minute);

        if(hour < 12) {
            AM_PM = "오전 ";
            //오전 12로 변경
            if (hour == 0) {
                hourStringValue = "12";
            }
            txt_startTime.setText(AM_PM + hourStringValue + ":" + minuteValue);

        } else {
            AM_PM = "오후 ";
            hourValue = hour;
            hourValue -= 12;
            txt_startTime.setText(AM_PM + (hourValue == 0 ? 12 : hourValue) + ":" + minuteValue);
        }
        //서버에 저장할땐 hh:mm:00 포맷으로 저장
        // (오전)0시 -> 12 시
        // (오전)1시 -> 01 시
        String hourStringValue2 = "";
        if (hour == 0) {
            hourStringValue2 = "00";
        } else {
            hourStringValue2 = CommonUtils.convertDateValue(hour);
        }
        startTime = hourStringValue2  + ":" + minuteValue + ":" + "00";
    }

    //회의기간 설정 - 디폴트 30분
    public void setDuration(int hour, int minute) {

        if (hour == 0) {
            duration = minute + "분";
        } else {
            duration = hour + "시간 " + minute + "분";
        }
        txt_duration.setText(duration);
    }


}
