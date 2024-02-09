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
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.model.meeting.RequestMeeting;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeetingInfo;
import com.wkimdev.letsmeeting.service.MeetingService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//회의예약화면 편집 화면
public class EditReservationActivity extends AppCompatActivity {


    private String TAG = this.getClass().getSimpleName();

    private EditText editTxt_title;
    private TextView txt_startDate;
    private TextView txt_startTime;
    private TextView txt_duration;
    //private TextView txt_gmt;
    private TextView txt_meetingNum;
    private TextView txt_meetingPwd;
    private Button btn_saveMeeting;

    private ImageButton imgBtn_startDate;
    private ImageButton imgBtn_startTime;
    private ImageButton imgBtn_duration;
    private ImageButton imgBtn_standTime;

    private int meetingId; //회의ID
    private String startDate;
    private String startTime;
    private String duration; //회의진행시간

    private String hintTitle; //회의타이틀 디폴트
    private String title; //회의타이틀

    private Spinner spinner_participantNum; //회의참여자수 제한
    private TextView txt_hostIdValue; //회의호스트ID
    private String hostId; //호스트아이디

    //오늘날짜 변수 선언
    private int currYear, currMonth, currDay;
    //현재시간 변수선언
    private int currHour, currMinute;
    //회의날짜가 오늘인지 판단
    private boolean isMeetingDateToday;
    private boolean isChangeDate; //회의날짜변경 여부 -> 날짜포맷 변경 및, 알람등록시간 변경을 위해 사용
    private boolean isChangeTime; //회의시간변경 여부 -> 시간포맷 변경 및, 알람등록시간 변경을 위해 사용

    private ProgressBar progressBar; //로그아웃시 로딩바호출
    private Sprite fadingCircle; //로딩바 애니메이션타입 선언

    private DatePickerDialog.OnDateSetListener callbackMethod; //회의날짜 데이트피커 콜백처리 변수 선언
    private TimePickerDialog.OnTimeSetListener timeCallbackMethod; //회의시간선택 타임피커 콜백처리 변수 선언
    private TimePickerDialog.OnTimeSetListener durationCallbackMethod; //회의기간선택 타임피커 콜백처리 변수 선언

    //회의상세정보 호출을 위한 레트로핏 인스턴스 생성
    MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);

    //시스템에서 알람 서비스를 제공하도록 도와주는 클래스
    //특정 시점에 알람이 울리도록 도와준다
    private AlarmManager alarmManager;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reservation);

        editTxt_title = findViewById(R.id.editTxt_title);
        txt_startDate = findViewById(R.id.txt_startDate);
        txt_startTime = findViewById(R.id.txt_startTime);
        txt_duration = findViewById(R.id.txt_duration);
        //txt_gmt = findViewById(R.id.txt_gmt);
        txt_meetingNum = findViewById(R.id.txt_meetingNum);
        txt_meetingPwd = findViewById(R.id.txt_meetingPwd);
        btn_saveMeeting = findViewById(R.id.btn_saveMeeting);

        imgBtn_startDate = findViewById(R.id.imgBtn_startDate);
        imgBtn_startTime = findViewById(R.id.imgBtn_startTime);
        imgBtn_duration = findViewById(R.id.imgBtn_duration);
        //imgBtn_standTime = findViewById(R.id.imgBtn_standTime);
        spinner_participantNum = findViewById(R.id.spinner_participantNum);
        txt_hostIdValue = findViewById(R.id.txt_hostIdValue);

        progressBar = findViewById(R.id.spin_kit);; //로그아웃시 로딩바 노출
        fadingCircle = new FadingCircle(); //노출되는 로딩바 애니메이션 스타일

        //푸시알림을 보내기 위해, 시스템에서 알림 서비스를 생성해주는 코드
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);


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


        Intent intent = getIntent();
        meetingId = intent.getIntExtra("meetingId", 0);
        if (meetingId > 0) {
            getMeetingInfo(meetingId);
        }
        startDate = intent.getStringExtra("startDate");
        startTime = intent.getStringExtra("startTime");
        Log.e(TAG, "onCreate: startTime 값 확인..!" + startTime);

        //date, tiem picker dialog 리스너 등록
        this.InitializeListener();
        this.timeInitializeListener();
        this.durationInitializeListener();

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onResume() {
        super.onResume();

        //회의 참여 인원수 제한 spinner설정
        ArrayAdapter participantNumAdapter = ArrayAdapter.createFromResource(this,
                R.array.participant_num, R.layout.spinner_layout);
        participantNumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_participantNum.setAdapter(participantNumAdapter);


        //회의날짜가 현재날짜라면 오늘회의라는 구분값을 true로 준다
        LocalDate today = LocalDate.now();
        String todayValue = today.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        if (startDate.equals(todayValue)) {
            isMeetingDateToday = true;
        }

        //회의날짜변경 처리
        imgBtn_startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Log.e(TAG, "onClick: startDate값 확인 " + startDate );
                String[] dateValue = txt_startDate.getText().toString().split("[.]");

                //날짜선택 다이알로그 창을 띄운다
                //올해까지만 선택가능 하고, 이전 날짜는 선택 못한다
                DatePickerDialog dialog = new DatePickerDialog(EditReservationActivity.this,
                        callbackMethod, Integer.valueOf(dateValue[0]),
                        Integer.valueOf(dateValue[1])-1, Integer.valueOf(dateValue[2]));

                Calendar maxDate = Calendar.getInstance(); //캘린더에서 선택할 수 있는 최소날짜
                Calendar minDate = Calendar.getInstance(); //캘린더에서 선택할 수 있는 최대날짜

                //선택가능 시작일 - 오늘
                minDate.set(currYear, currMonth, currDay);
                //밀리세컨드로 날짜변환 후 Calendar's 타임으로 변환
                dialog.getDatePicker().setMinDate(minDate.getTime().getTime());

                //선택가능 종료일 - 올해말
                maxDate.set(currYear, 11,31);
                dialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

                dialog.show();

            }
        });

        //회의시작날짜 변경 처리
        txt_startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] dateValue = txt_startDate.getText().toString().split("[.]");

                //날짜선택 다이알로그 창을 띄운다
                //올해까지만 선택가능 하고, 이전 날짜는 선택 못한다
                DatePickerDialog dialog = new DatePickerDialog(EditReservationActivity.this,
                        callbackMethod, Integer.valueOf(dateValue[0]),
                        Integer.valueOf(dateValue[1])-1, Integer.valueOf(dateValue[2]));

                Calendar maxDate = Calendar.getInstance(); //캘린더에서 선택할 수 있는 최소날짜
                Calendar minDate = Calendar.getInstance(); //캘린더에서 선택할 수 있는 최대날짜

                //선택가능 시작일 - 오늘
                minDate.set(currYear, currMonth, currDay);
                //밀리세컨드로 날짜변환 후 Calendar's 타임으로 변환
                dialog.getDatePicker().setMinDate(minDate.getTime().getTime());

                //선택가능 종료일 - 올해말
                maxDate.set(currYear, 11,31);
                dialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

                dialog.show();

            }
        });


        //회의시간 변경 처리
        imgBtn_startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //스피너모드 타임피커 - 현재시간 이전은 선택할 수 없다
                TimePickerDialog dialog = new TimePickerDialog(
                        EditReservationActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        timeCallbackMethod, currHour, currMinute, false);

                dialog.setTitle("회의시간선택");
                //기존테마의 배경을 없앤다
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.show();

            }
        });

        //회의기간 변경 처리
        imgBtn_duration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //스피너모드 타임피커 - 현재시간 이전은 선택할 수 없다
                TimePickerDialog dialog = new TimePickerDialog(
                        EditReservationActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        durationCallbackMethod, 0, 30, true);

                dialog.setTitle("회의기간선택");
                //기존테마의 배경을 없앤다
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.show();

            }
        });


        //표준시간 변경
        /*imgBtn_standTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });*/


        //회의저장 버튼 클릭 처리
        btn_saveMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //회의명이 없으면 디폴트값을 넣는다
                if (TextUtils.isEmpty(editTxt_title.getText().toString())) {
                    title = hintTitle;
                } else {
                    title = editTxt_title.getText().toString();
                }

                //기간값이 없을 경우
                if(TextUtils.isEmpty(duration)){
                    duration = txt_duration.getText().toString();
                }

                //회의날짜 변경사항이 없을 경우, startDate 포맷을 변경해야함 -> yyyy-mm-dd
                if(!isChangeDate) {
                    startDate = CommonUtils.formatDate2(startDate);
                }

                //시간 변경사항이 없을 경우, startTime 포맷을 변경해야함 -> hh:mm:ss
                if(!isChangeTime) {
                    startTime = CommonUtils.convertTimeFormat(startTime);
                }

                //현재시간 이후로 회의시간을 설정할 수 있도록 체크한다.
                if (!isValidMeetingDate(startDate, startTime)) {
                    //startTime값 원래 데이터 타입으로 초기화
                    startTime = txt_startTime.getText().toString();
                    isChangeTime = false;
                    Toast.makeText(EditReservationActivity.this, "회의시간은, 현재시간 이후로 선택해주세요!", Toast.LENGTH_SHORT).show();
                    return;
                }

                //회의시간이 변경되었을 경우, 알람시간을 변경한다
                if (isChangeTime) {
                    //startTime should hh:mm:ss type
                    setNotice(startDate, startTime, title, meetingId);
                }

                Log.e(TAG, "onClick: 선택한 회의참여 인원수 " + spinner_participantNum.getSelectedItem());
                int participantNum = Integer.parseInt(spinner_participantNum.getSelectedItem().toString().replace("명", ""));

                //레트로핏으로 회의내용을 업데이트 요청
                RequestMeeting requestMeeting = new RequestMeeting(title, startDate,
                        startTime, // hh:mm:ss 포맷으로 저장
                        duration, "KO", "", meetingId, hostId, participantNum);
                updateMeetingInfo(requestMeeting);
            }
        });

    }


    //회의시간이 되면 사용자가에게 푸시알림을 보내주기 위해, 알림을 등록해주는 코드
    //회의시간을 yyyy-mm-dd hh:mm:ss 포맷으로 전달한다
    public void setNotice(String startDate, String startTime, String title, int meetingId) {
        String meetingTime = startDate + " " + startTime;
        //NotificationReceiver에 알림내용을 전달
        //알림메세지 내용 => "리즈의 회의", 오후 __시에 시작됨
        String timeValue = CommonUtils.formatTimeToAMPM(startTime);
        Log.e(TAG, "알림메세지에 등록되는 시간 값 확인 : " + timeValue);

        //알람을 수신할 수 있도록 하는 리시버로 인텐트 요청
        Intent receiverIntent = new Intent(this, NotificationReceiver.class);
        receiverIntent.putExtra("content", title + ", " + timeValue + " 에 시작됨" );

        /*PendingIntent - Notification으로 작업을 수행할 때 인텐트가 실행되도록 합니다.
        Notification은 안드로이드 시스템의 NotificationManager가 Intent를 실행합니다.
        즉 다른 프로세스에서 수행하기 때문에 Notification으로 Intent수행시 PendingIntent의 사용이 필수 입니다.*/


        //- 브로드캐스트로 실행될 pendingIntent선언
        // Intent가 새로 생성될때마다(알람을 등록할 때마다) intent값을 업데이트 시키기 위해, FLAG_UPDATE_CURRENT 플래그를 준다
        //- 이전 알람을 취소시키지 않으려면 requestCode를 다르게 줘야 한다.
        //그러기위해, meetingID를 requestCode로 등록해서 요청한다.

        //이전에 등록한 인텐트가 있는지 확인한다
        //FLAG_NO_CREATE => 설명된 PendingIntent가 존재하지 않는 경우, null을 반환합니다(새 항목을 만드는 대신).
        boolean alarmUp = (PendingIntent.getBroadcast(this, meetingId,
                new Intent(this, NotificationReceiver.class),
                PendingIntent.FLAG_NO_CREATE) != null);

        if (!alarmUp) {
            Log.e(TAG, "이전 알람이 등록 되어 있지 않아, 새로 등록해야 합니다. ");

        } else {

            //이전 알람이 있을 경우, 이전알람을 취소하고 새로 등록한다
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, meetingId,
                    receiverIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT
            );

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
    }


    //레트로핏으로 회의내용을 업데이트 요청
    private void updateMeetingInfo(RequestMeeting requestMeeting) {

        service.updateMeeting(requestMeeting).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                ResponseCommonData joinResponse = response.body();

                if (200 == joinResponse.getStatusCode()) {//성공

                    //로딩바 발생
                    //로딩애니메이션 노출
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminateDrawable(fadingCircle); //진행바 모양을 그리는데 사용되는 드로어블 정의


                    // 0.8초간 로딩바를 띄운 뒤 회의상세화면으로 이동 시킴
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            //1초 후 앱 처음화면으로 이동
                            Intent intent = new Intent(getApplicationContext(), MeetingDetailActivity.class);
                            intent.putExtra("meetingId", meetingId);
                            intent.putExtra("startDate", startDate);
                            intent.putExtra("startTime", CommonUtils.formatTimeVer2(startTime)); //오후 hh:mm 포맷으로 전달
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                    }, 800);
                } else {
                    Toast.makeText(getApplicationContext(), "회의수정이 실패했습니다!", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });
    }


    //회의상세정보를 호출해 화면에 응답값을 바인딩 한다
    public void getMeetingInfo(int meetingId) {
        service.getMeetingInfo(meetingId).enqueue(new Callback<ResponseMeetingInfo>() {
            @Override
            public void onResponse(Call<ResponseMeetingInfo> call, Response<ResponseMeetingInfo> response) {

                ResponseMeetingInfo responseMeeting = response.body();
                if (200 == responseMeeting.getStatusCode()) {//성공
                    setMeetingData(responseMeeting.getData());
                } else {
                    Toast.makeText(getApplicationContext(), "회의생성이 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseMeetingInfo> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });

    }


    //회의편집화면 뷰들에 데이터 바인딩
    public void setMeetingData(MeetingDataResult meetingData) {
        editTxt_title.setText(meetingData.getTitle());
        txt_startDate.setText(startDate);
        txt_startTime.setText(startTime);
        txt_duration.setText(meetingData.getDuration());
        txt_meetingNum.setText(meetingData.getMeetingNum());
        txt_meetingPwd.setText(meetingData.getMeetingPwd());
        //회의 호스트 ID
        hostId = meetingData.getHostId();
        txt_hostIdValue.setText(meetingData.getHostId());
        //회의 최대참여자수
        spinner_participantNum.setSelection(meetingData.getParticipantsNum()-2);
    }


    //datePickerDialog에서 선택한 날짜 정보를 듣는 이벤트
    public void InitializeListener()
    {
        callbackMethod = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                monthOfYear++;
                isChangeDate = true;
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

                isChangeTime = true;
                Log.e(TAG, "오늘 날짜여부 ??  " + isMeetingDateToday);

                //회의날짜와 현재시간을 같이 비교하여, 현재시간 이후로 회의시간을 설정할 수 있게 만든다
                if (hourOfDay <= currHour && minute <= currMinute && isMeetingDateToday) {
                    Log.e(TAG, "현재시간 이후로 선택해주세요!!");
                    isChangeTime = false;
                    Toast.makeText(EditReservationActivity.this, "현재시간 이후로 선택해주세요!", Toast.LENGTH_SHORT).show();
                } else {
                    //@TODO: 진행 중
                    setMeetingTime(hourOfDay, minute);
                }
            }
        };
    }

    //회의날짜가 오늘날짜 이전이면, 저장할 수 없도록 예외처리하기 위한 구분값
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean isValidMeetingDate(String startDate, String startTime) {

        //저장시점의 회의날짜와 회의시간을 받는다.
        String meetingDate = startDate + " " + startTime;

        //현재 날짜 구하기
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String nowString = now.format(dateTimeFormatter);

        int resultValue = CommonUtils.compareDate(meetingDate, nowString);
        if (resultValue <= 0) {//회의날짜가 오늘날짜보다 이전
            return false;
        } else { //회의날짜가 오늘날짜보다 이후
            return true;
        }
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
        String startDateValue = year + "." + CommonUtils.convertDateValue(month) + "." + CommonUtils.convertDateValue(day);

        //현재 month보다 한달전으로 들어오는 이슈로 currMonth+1로 처리함.
        if (year == currYear && month == (currMonth+1) && day == currDay) {
            isMeetingDateToday = true;
        } else {
            isMeetingDateToday = false;
        }
        txt_startDate.setText(startDateValue);

        //서버에 요청할 값 셋팅. yyyy-mm-dd 포맷으로 요청
        startDate = year + "-" + CommonUtils.convertDateValue(month) + "-" + CommonUtils.convertDateValue(day);
    }


    /**
     * 회의시간 설정
     *  1) 편집화면 시작시간 인풋박스에 오후/오전 HH:mm 포맷으로 데이터 노출 설정
     *  2) 서버에 회의시간 저장시 HH:mm:ss 포맷으로 데이터 저장되도록 설정
     * @param hour HH(0~23)
     * @param minute mm(0~59)
     */
    public void setMeetingTime(int hour, int minute) {
        String startTimeValue = hour + ":" + minute;

        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        DateFormat dateFormat2 = new SimpleDateFormat("a hh:mm");
        DateFormat dateFormat3 = new SimpleDateFormat("HH:mm:ss");
        Date datetime = null;

        try {
            //문자열을 파싱해서 Date객체를 만들어준다
            datetime = dateFormat.parse(startTimeValue);
        } catch (ParseException e) {
            //패턴과다른 문자열이 입력되면 parse exception이 발생된다
            e.printStackTrace();
        }

        //편집화면 시작시간 인풋박스에 오후/오전 HH:mm 포맷으로 데이터 노출 설정
        txt_startTime.setText(dateFormat2.format(datetime));

        //서버에 회의시간 저장시 HH:mm:ss 포맷으로 데이터 저장되도록 설정
        startTime = dateFormat3.format(datetime);
    }

    //회의기간 설정
    public void setDuration(int hour, int minute) {

        if (hour == 0) {
            duration = minute + "분";
        } else {
            duration = hour + "시간 " + minute + "분";
        }
        txt_duration.setText(duration);
    }

}