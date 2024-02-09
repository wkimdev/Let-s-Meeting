package com.wkimdev.letsmeeting.meeting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.wkimdev.letsmeeting.HomeActivity;
import com.wkimdev.letsmeeting.OnItemClick;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.WebAppInterface;
import com.wkimdev.letsmeeting.configure.ApplicationConstants;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.model.ChatData;
import com.wkimdev.letsmeeting.util.CommonUtils;

import java.util.ArrayList;
import java.util.List;


//회의화면
//웹뷰로 회의화면을 띄운다.
public class MeetingActivity extends AppCompatActivity implements OnItemClick  {

    private String TAG = this.getClass().getSimpleName();

    private String nickname;
    private boolean muted; //음소거 여부
    private boolean cameraOff; //비디오꺼짐 여부
    private String meetingNum; //회의번호
    private int meetingId; //회의ID
    private int maximum; //회의최대참여인원수

    private ConstraintLayout meeting_container;
    private WebView webView;

    private ImageView imgBtn_changeCamera; //카메라전환버튼
    private TextView meeting_title; //상단 로고 텍스트
    private Button btn_leave;


    //회의화면 하단바(음소거설정, 비디오설정, 채팅화면 이동)
    private ImageView img_micOn;
    private ImageView img_micOff;
    private TextView txt_micState;
    private ImageView img_videoOn;
    private ImageView img_videoOff;
    private TextView txt_videoState;
    private ImageView img_chat;
    private TextView txt_chat;

    private String hostId;  //호스트ID(이메일)
    private String userEmail; //유저ID
    private boolean isHost = false; //회의참가자가 호스트인지 여부

    private String meetingPwd;
    private String duration;
    private String meetingTitle;
    private ClipboardManager mClipboardManager;

    //카메라에 권한을 요청하는 코드값
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;

    // 진행바
    private ProgressDialog progressDoalog;

    // 채팅관련 변수선언
    private LinearLayout layout_container; //채팅화면 레이아웃
    private EditText editTxt_chat;
    private Button btn_send;
    private RecyclerView rc_chat;
    private RecyclerView.LayoutManager layoutManager;
    private ChatAdapter chatAdapter;
    private boolean isChatScreen; //채팅화면 레이아웃 여부
    private ArrayList<ChatData> chatList;
    private MainHandler mainHandler;
    private int index = 0; //채팅리스트 인덱스
    private TextView txt_cntChat; //읽지않는 채팅갯수 카운트
    private int chatCnt; //읽지않는 채팅갯수

    //채팅노티피케이션 레이아웃
    private LinearLayout layout_chatNotification;
    private ImageView iv_profile_noti;
    private TextView txt_nickname_txt;
    private TextView txt_message_noti;
    private CardView card_chatNotification;
    private CoordinatorLayout layout_coordinator;
    private InputMethodManager imm; //입력제어 매니저 선언.  키보드를 내리기 위해 사용한다
    private boolean fromPushScreen; //푸시화면으로 부터 진입했을 경우, 뒤로가기시 홈으로 이동하기 위한 구분값


    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting);

        meeting_container = findViewById(R.id.meeting_container);
        webView = findViewById(R.id.webView);
        imgBtn_changeCamera = findViewById(R.id.imgBtn_changeCamera);
        meeting_title = findViewById(R.id.meeting_title);

        btn_leave = findViewById(R.id.btn_leave);

        img_micOn = findViewById(R.id.img_micOn);
        img_micOff = findViewById(R.id.img_micOff);
        txt_micState = findViewById(R.id.txt_micState);
        img_videoOn = findViewById(R.id.img_videoOn);
        img_videoOff = findViewById(R.id.img_videoOff);
        txt_videoState = findViewById(R.id.txt_videoState);
        img_chat = findViewById(R.id.img_chat);
        txt_chat = findViewById(R.id.txt_chat);
        layout_container = findViewById(R.id.layout_container);

        //채팅
        editTxt_chat = findViewById(R.id.editTxt_chat);
        btn_send = findViewById(R.id.btn_send);
        rc_chat = findViewById(R.id.rc_chat);
        txt_cntChat = findViewById(R.id.txt_cntChat);

        //채팅알림 레이아웃
        layout_chatNotification = findViewById(R.id.layout_chatNotification);
        iv_profile_noti = findViewById(R.id.iv_profile_noti);
        txt_nickname_txt = findViewById(R.id.txt_nickname_noti);
        txt_message_noti = findViewById(R.id.txt_message_noti);
        card_chatNotification = findViewById(R.id.card_chatNotification);
        layout_coordinator = findViewById(R.id.layout_coordinator);

        // 입력받는 방법을 관리하는 Manager객체를  요청하여 InputMethodmanager에 반환한다.
        // 채팅을 보낸 후 키보드를 내리기 위해 사용한다
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        chatList = new ArrayList<>();

        //Request Permissions
        this.requestPermissionForCameraAndMicrophone();

        //웹뷰에 세부적인 내용을 셋팅해준다.
        this.setUpWebViewDefaults(webView);

        //채팅메세지가 수신 되었을 때 리사이클러뷰를 업데이트하기 위한 핸들러이벤트 등록
        mainHandler = new MainHandler();

        //webview와 javascript interface를 주고 받기 위한 클래스 결합
        //name 인자값에 지정한 값이, WebView에서 실행되는 자바스크립트를 위한 인터페이스명이 된다 (인터페이스명: 'Android')
        webView.addJavascriptInterface(new WebAppInterface(this, mainHandler), "Android");

        //loadUrl은 주어진 URL을 로드 시키는 메소드
        //assets에 있는 mobile.js을 로딩합니다. 안드로이드 -> webview로 event전달을 위해 필요.
        webView.loadUrl("file:///android_asset/index_mobile.html");


        //닉네임, 오디오설정 여부, 비디오켜짐 여부를 받는다.
        Intent intent = getIntent();
        nickname = intent.getStringExtra("nickname");
        muted = intent.getBooleanExtra("muted", false);
        cameraOff = intent.getBooleanExtra("cameraOff", false);
        meetingNum = intent.getStringExtra("meetingNum");
        maximum = intent.getIntExtra("maximum", 2);

        hostId = intent.getStringExtra("hostId");
        userEmail = intent.getStringExtra("userEmail");
        meetingId = intent.getIntExtra("meetingId", 0);

        //회의정보 팝업을 띄우기 위해 사용
        meetingTitle = intent.getStringExtra("meetingTitle");
        meetingPwd = intent.getStringExtra("meetingPwd");
        duration = intent.getStringExtra("duration");

        fromPushScreen = intent.getBooleanExtra("fromPushScreen", false);

        //웹뷰에 표시할 웹사이트 주소, WebRTC를 통해 영상을 노출하는 페이지
        String webviewUrl = ApplicationConstants.WEBRTC_SERVER_URL + "/mobile?num=" + meetingNum
                    + "&muted=" + muted + "&cameraOff=" + cameraOff + "&nickname=" + nickname
                    + "&maximum=" + maximum + "&userEmail=" + userEmail + "&hostId=" + hostId;
        webView.loadUrl(webviewUrl);

        //화면최초 진입시 리사이클러뷰로 채팅리스트 노출 되도록 설정
        List<ChatData> chatData = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatData, getApplicationContext(), nickname, this, hostId);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        rc_chat.setLayoutManager(layoutManager);
        rc_chat.setAdapter(chatAdapter);


        //webview에 권한을 부여하는 코드
        //아래 권한 처리를 하지 않으면 로컬비디오가 노출되지 않음
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String[] PERMISSIONS = {
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        };
                        request.grant(PERMISSIONS);
                    }
                });
            }
        });

        //인텐트로 전달되는 설정값을 보고 마이크 및 비디오 상태 설정
        handlerOnMicVideoState(muted, cameraOff);

        //클립보드 관리자를 "getSystemService"를 사용하여 인스턴스화
        mClipboardManager = (ClipboardManager) getSystemService( CLIPBOARD_SERVICE );
    }

    @Override
    public void onClickMeetingDetail(int meetingId, String startDate, String startTime) {

    }

    @Override
    public void onClickMeetingStart(int meetingId, String meetingNum, int participantsNum,
                        String meetingTitle, String meetingPwd, String duration, String status) {

    }

    //인풋창 외에 채팅창을 입력하면 키보드가 내려가도록 처리
    @Override
    public void onClickChatView() {
        Log.e(TAG, "onClick: 리사이클러뷰 리스트 클릭!!!");
        //키보드를 숨겨준다
        // 첫번째 매개변수에 해당하는(여기서는 input) 곳에 키보드가 보이면 키보드를 숨긴다
        // 두번째 매개변수에는 해당 Flag없이 0을 넘겨준다.
        imm.hideSoftInputFromWindow(editTxt_chat.getWindowToken(), 0);
        editTxt_chat.clearFocus();
    }


    //브릿지로부터 채팅메세지 수신 이벤트를 받는 핸들러 등록
    //상대방의 메세지를 받아 어뎁터 채팅리스트를 업데이트시킨다.
    class MainHandler extends Handler {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message msg) {

            String chatMsg = msg.getData().getString("message");
            String profile = msg.getData().getString("profile");
            String nickname = msg.getData().getString("remoteNickName");
            String userId = msg.getData().getString("remoteId");

            ChatData data = new ChatData(userId);
            data.setMessage(chatMsg);
            data.setProfile(profile);
            data.setNickname(nickname);
            data.setSendTime(CommonUtils.getCurrentTime());
            chatList.add(index, data);
            index++;

            //추가된 메세지데이터 인덱스를 파라미터로 전달해 어뎁터에 반영한다.
            chatAdapter.notifyItemInserted(index);

            //지정된 어댑터 위치로 스크롤
            rc_chat.getLayoutManager().scrollToPosition(chatAdapter.getItemCount()-1);


            //회의참여화면에서만 토스트 노출 및 읽지않는 채팅갯수 표기
            if(!isChatScreen) {

                chatCnt++; //읽지않는 채팅갯수 증가
                txt_cntChat.setVisibility(View.VISIBLE); //읽지않는 채팅갯수 표기
                txt_cntChat.setText(String.valueOf(chatCnt));

                if (chatMsg.length() > 16) { //16자 이상인 경우 말줄임 표기
                    chatMsg = chatMsg.substring(0, 16) + "...";
                }
                String chatInfoAlarm = "[" + nickname + "]님으로 부터 메세지\n" + chatMsg;

                //스낵바로 상대방으로부터 전달된 메세지 알림창 띄움
                Snackbar chatInfoSnackbar =  Snackbar.make(layout_coordinator, chatInfoAlarm, 3000);
                View snackbarView = chatInfoSnackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setMaxLines(3);
                chatInfoSnackbar.setAction("확인", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //채팅화면으로이동
                        moveToChatScreen();
                    }
                });
                chatInfoSnackbar.show();
            }
        }
    }

    //카메라와 오디오에 권한을 요청하는 코드
    private void requestPermissionForCameraAndMicrophone() {
        //권한을 요청하기 전에 근거가 있는 UI를 표시해야 하는지 여부를 가져옵니다.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                    "WE NEED CAMERA AND MIC PERMISSIONS",
                    Toast.LENGTH_LONG).show();
        } else {
            //이 애플리케이션에 부여할 권한을 요청합니다.
            // 이러한 권한은 매니페스트에서 요청해야 하며, 앱에 부여해서는 안 되며
            // 플랫폼 또는 타사 앱에서 선언했는지 여부에 관계없이 위험한 보호 수준을 가져야 합니다.
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    //설정값을 보고 마이크 및 비디오 상태 설정
    private void handlerOnMicVideoState(boolean muted, boolean cameraOff) {

        //마이크 상태 설정
        if (true == muted) {//음소거 설정상태
            img_micOn.setVisibility(View.GONE);
            img_micOff.setVisibility(View.VISIBLE);
            txt_micState.setText("음소거해제");
        }

        //비디오 상태 설정
        if (true == cameraOff) {//비디오꺼짐 상태
            img_videoOn.setVisibility(View.GONE);
            img_videoOff.setVisibility(View.VISIBLE);
            txt_videoState.setText("비디오시작");
        }
    }


    /**
     * 버튼 이벤트 등록
     */
    @Override
    protected void onResume() {
        super.onResume();
        //Log.e(TAG, "onResume: call! ");

        imgBtn_changeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //카메라전환 이벤트발생
                webView.loadUrl("javascript:handlerMobileCameraChange()");
            }
        });

        //회의화면 로고클릭시 회의정보 팝업 노출
        meeting_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //하단 대화 상자 호출 및 초기화
                showBottomSheetDialog();
            }
        });

        //회의나가기 버튼 클릭 실행!
        btn_leave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hostId.equals(userEmail)) { //회의호스트인 경우 팝업
                    isHost = true;
                    showPopup("모든참여자를 내보내고 회의를 종료하시겠습니까?", "회의를 종료합니다!");
                } else { //회의호스트가 아닌 경우 팝업 노출
                    showPopup("정말 회의에서 나가시겠어요?", "회의를 나갑니다!");
                }
            }
        });

        //마이크 음소거 설정 이벤트 등록
        handlerOnMicClick();
        //비디오 노출설정 이벤트 등록
        handlerOnVideoClick();

        //채팅화면 노출 이벤트 처리
        img_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToChatScreen();
            }
        });


        //채팅입력창에 포커스가 갔을때, 채팅리스트의 스크롤을 맨 하단으로 위치시킨다.
        editTxt_chat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.e(TAG, "onTouch: call,,,!!!");

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        Log.e(TAG, "전체 아이템 갯수: " + (chatAdapter.getItemCount()-1));
                        rc_chat.getLayoutManager().scrollToPosition(chatAdapter.getItemCount()-1);
                    }
                }, 500);
                return false;
            }
        });


        //채팅내용이 있을 경우만 전송버튼 활성화
        editTxt_chat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.e(TAG, "beforeTextChanged: call...!!!");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.e(TAG, "onTextChanged: call...!!!");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    btn_send.setEnabled(true);
                    btn_send.setBackgroundColor(getResources().getColor(R.color.deepGreen_primary));
                } else {
                    btn_send.setEnabled(false);
                    btn_send.setBackgroundColor(getResources().getColor(R.color.gray));
                }
            }
        });


        //채팅전송 버튼 클릭 이벤트처리
        btn_send.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {

                // 클라이언트 코드에서 웹뷰의 자바스크립트를 호출할 때는 아래와 같은 방식으로 처리
                Gson gson = new Gson();
                String message = gson.toJson(editTxt_chat.getText().toString());
                String profile = gson.toJson(PreferenceManager.getString(getApplicationContext(), "profile"));

                //sendChatData라는 JS함수가 실행 되면서
                //datachannel을 통해 다른피어에게 메세지, 내 프로필사진 정보가 전달된다.
                webView.loadUrl("javascript:sendChatData(" + message + "," + profile + ")");

                //내가 전송한 메세지를 내 로컬 채팅화면에 노출한다
                ChatData chatData = new ChatData(userEmail);
                chatData.setMessage(editTxt_chat.getText().toString());
                chatData.setNickname(nickname);
                chatData.setProfile(profile);
                chatData.setSendTime(CommonUtils.getCurrentTime());
                chatList.add(chatData);
                index++;

                //업데이트 채팅리스트를 어뎁터에 반영한다
                chatAdapter.notifyItemInserted(index);

                //채팅리스트의 스크롤을 맨 하단으로 위치시킨다.
                rc_chat.getLayoutManager().scrollToPosition(chatAdapter.getItemCount()-1);

                //채팅메세지 입력창 초기화
                editTxt_chat.setText("");
            }
        });


        //채팅알림 레이아웃을 클릭하면 회의화면으로 이동
        card_chatNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout_chatNotification.setVisibility(View.GONE);
                moveToChatScreen();
            }
        });

    }

    //채팅화면으로 이동 처리
    private void moveToChatScreen() {
        //읽지않는 채팅갯수 초기화 및 텍스트뷰 비노출처리
        chatCnt = 0;
        txt_cntChat.setVisibility(View.INVISIBLE);

        //채팅화면진입 여부 설정
        isChatScreen = true;

        //화면최초 진입시 리사이클러뷰로 채팅 노출
        chatAdapter = new ChatAdapter(chatList, getApplicationContext(), nickname, this, hostId);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rc_chat.setLayoutManager(layoutManager);
        rc_chat.setAdapter(chatAdapter);

        //지정된 어댑터 위치로 스크롤
        rc_chat.getLayoutManager().scrollToPosition(chatAdapter.getItemCount()-1);

        //채팅화면 레이아웃을 보이도록 노출
        layout_container.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
    }


    //회의정보를 노출하는 하단 대회상자
    private void showBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.meeting_info_popup);

        //회의정보 팝업내용 셋팅
        TextView txt_meetingTitle = bottomSheetDialog.findViewById(R.id.txt_meetingTitle);
        txt_meetingTitle.setText(meetingTitle);
        TextView txt_meetingNum = bottomSheetDialog.findViewById(R.id.txt_meetingNum);
        txt_meetingNum.setText(meetingNum);
        TextView txt_hostId = bottomSheetDialog.findViewById(R.id.txt_hostId);
        txt_hostId.setText(hostId);
        TextView txt_meetingPwd = bottomSheetDialog.findViewById(R.id.txt_meetingPwd);
        Log.e(TAG, "4. 회의비밀번호 확인 : " + meetingPwd);
        txt_meetingPwd.setText(meetingPwd);
        TextView txt_participantNum = bottomSheetDialog.findViewById(R.id.txt_participantNum);
        txt_participantNum.setText(maximum + "명");
        TextView txt_duration = bottomSheetDialog.findViewById(R.id.txt_duration);
        txt_duration.setText(duration);

        //회의ID,비밀번호 복사
        ImageView copyMeetingId = bottomSheetDialog.findViewById(R.id.copyMeetingId);
        ImageView copyMeetingPwd = bottomSheetDialog.findViewById(R.id.copyMeetingPwd);


        //회의정보복사 아이콘 클릭 이벤트 처리
        copyMeetingId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creates a new text clip to put on the clipboard
                mClipboardManager.setPrimaryClip(ClipData.newPlainText("회의 ID", meetingNum));
                Toast.makeText(MeetingActivity.this, "회의 ID를 복사했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        copyMeetingPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creates a new text clip to put on the clipboard
                mClipboardManager.setPrimaryClip(ClipData.newPlainText("회의비밀번호", meetingPwd));
                Toast.makeText(MeetingActivity.this, "회의 비밀번호를 복사했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }


    //비디오 노출 설정 이벤트 핸들링
    private void handlerOnVideoClick() {
        //비디오 꺼짐 설정
        img_videoOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_videoOn.setVisibility(View.GONE);
                img_videoOff.setVisibility(View.VISIBLE);
                txt_videoState.setText("비디오시작");

                //비디오아이콘 클릭 이벤트호출
                webView.loadUrl("javascript:handleCameraClick()");
            }
        });

        //비디오 해제설정
        img_videoOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_videoOn.setVisibility(View.VISIBLE);
                img_videoOff.setVisibility(View.GONE);
                txt_videoState.setText("비디오중지");

                //비디오아이콘 클릭 이벤트호출
                webView.loadUrl("javascript:handleCameraClick()");
            }
        });
    }

    //마이크 음소거 설정 핸들링 이벤트
    private void handlerOnMicClick() {
        //음소거 해제설정
        img_micOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_micOn.setVisibility(View.GONE);
                img_micOff.setVisibility(View.VISIBLE);
                txt_micState.setText("음소거해제");

                // 클라이언트 코드에서 웹뷰의 자바스크립트를 호출할 때는 아래와 같은 방식으로 처리
                //Gson gson = new Gson();
                //String dataString = gson.toJson(txt_micState.getText());

                //자바에서 자바스크립트 함수를 호출할 경우 표준 자바스크립트 호출구문을 이용한다.
                //webView의 loadUrl함수를 그대로 사용하면서, URL부분을 자바스크립트 함수 호출 URL로 넘기면 된다.

                //음소거 이벤트호출
                webView.loadUrl("javascript:handleMuteClick()");

            }
        });

        //음소거 설
        img_micOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_micOn.setVisibility(View.VISIBLE);
                img_micOff.setVisibility(View.GONE);
                txt_micState.setText("음소거");

                //음소거 이벤트호출
                webView.loadUrl("javascript:handleMuteClick()");

            }
        });
    }

    //웹뷰에 세부적인 내용을 셋팅해주는 메소드
    @SuppressLint("ClickableViewAccessibility")
    private void setUpWebViewDefaults(WebView webView) {

        WebSettings webSettings = webView.getSettings(); //웹뷰 세부 셋팅 등록
        webSettings.setJavaScriptEnabled(true); // Enable Javascript

        // Use WideViewport and Zoom out if there is no viewport defined
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Allow use of Local Storage
        webSettings.setDomStorageEnabled(true);

        // Hide the zoom controls
        webSettings.setDisplayZoomControls(false);

        // Enable remote debugging via chrome://inspect
        WebView.setWebContentsDebuggingEnabled(true);

        //WebView에서 미디어를 재생하기 위해 사용자 제스처가 필요한지 여부를 설정합니다(기본값은 true).
        //비디오, 오디오 작동을 위해 설정
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        //터치 이벤트가 webview로 전송될 때 호출될 콜백을 등록
        //인풋박스 포커스시 키보드 노출되도록 설정
        webView.requestFocus();
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });


        //webview그리기 전 클리어
        webView.clearHistory();
        webView.clearCache(true);

        webView.setWebViewClient(new WebViewClient());

    }


    //회의참여 화면에서 뒤로가기 버튼 클릭시, 별도 이벤트 처리를 해준다.
    @Override
    public void onBackPressed() {
        if (isChatScreen) { //채팅뷰가 노출되었을 경우는, 뒤로가기 클릭시 회의화면 보이도록 노출
            layout_container.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            isChatScreen = false;

        } else {
            //회의화면에서 뒤로가기를 눌렀을 경우 확인 팝업 발생
            AlertDialog.Builder ad = new AlertDialog.Builder(MeetingActivity.this);

            ad.setMessage("회의에서 나가시겠어요?");
            ad.setCancelable(false);
            ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    //webview로 소켓 disconnected event호출
                    webView.loadUrl("javascript:leaveFromBackBtn()");

                    progressDoalog = new ProgressDialog(MeetingActivity.this);
                    progressDoalog.setMessage("회의에서 나갑니다...!");
                    progressDoalog.show();

                    // 1.5초간 멈춘다음 이전화면으로 이동
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            progressDoalog.dismiss();
                            destroyWebView(); //회의참여 액티비티 종료 후 웹뷰도 종료 되도록 처리

                            //푸시화면으로 부터 들어온 회의화면에서 뒤로가기시 홈으로 이동하도록 처리
                            if (fromPushScreen) {
                                startActivity(new Intent(MeetingActivity.this, HomeActivity.class));
                                finish();
                            }
                            finish();
                        }
                    }, 1500);

                }
            }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            ad.show();
        }
    }


    //회의나가기 공통팝업 노출 메소드
    public void showPopup(String message1, String message2) {

        AlertDialog.Builder ad = new AlertDialog.Builder(MeetingActivity.this);

        ad.setMessage(message1);
        ad.setCancelable(false);
        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                //webview로 회의방을 나가는 이벤트 호출
                webView.loadUrl("javascript:leaveRoom(" + isHost + "," + meetingId + ")");

                progressDoalog = new ProgressDialog(MeetingActivity.this);
                progressDoalog.setMessage(message2);
                progressDoalog.show();

                // 일정시간 멈춘다음 이전화면으로 이동
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        progressDoalog.dismiss();
                        destroyWebView(); //회의참여 액티비티 종료 후 웹뷰도 종료 되도록 처리
                        finish();
                    }
                }, 1500);

            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        ad.show();
    }


    //호스트에 의해 회의를 나가기 팝업(확인버튼만 있는 팝업)
    public void showLeaveMeetingPopup(String message) {
        //Log.e(TAG, "1. showLeaveMeetingPopup : " + message);

        AlertDialog.Builder ad = new AlertDialog.Builder(MeetingActivity.this);

        //Log.e(TAG, "2. AlertDialog.Builder 생성...! ");

        ad.setMessage(message);
        ad.setCancelable(false);
        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Log.e(TAG, "3. showLeaveMeetingPopup 확인 버튼 클릭...!");

                dialog.dismiss();

                progressDoalog = new ProgressDialog(MeetingActivity.this);
                progressDoalog.setMessage("회의에서 나갑니다...!");
                progressDoalog.show();

                // 2초간 멈춘다음 이전화면으로 이동
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        progressDoalog.dismiss();
                        finish();
                    }
                }, 2000);
            }
        });
        ad.show();
    }

    //회의참여 액티비티 종료 후 웹뷰도 종료 되도록 처리하는 메소드
    //웹뷰를 종료시키지 않으면, 같은 회의를 다시 참여했을 경우 이전 웹뷰가 남아있어 화면이 노출되지 않는 등의 오류가 발생됨
    public void destroyWebView() {
        Log.e(TAG, "destroyWebView call !!!!");

        // Make sure you remove the WebView from its parent view before doing anything.
        meeting_container.removeAllViews();

        webView.clearHistory();

        // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
        // Probably not a great idea to pass true if you have other WebViews still alive.
        webView.clearCache(true);

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        webView.loadUrl("about:blank");

        webView.onPause();
        webView.removeAllViews();
        webView.destroyDrawingCache();

        // NOTE: This pauses JavaScript execution for ALL WebViews,
        // do not use if you have other WebViews still alive.
        // If you create another WebView after calling this,
        // make sure to call mWebView.resumeTimers().
        webView.pauseTimers();

        // NOTE: This can occasionally cause a segfault below API 17 (4.2)
        webView.destroy();

        // Null out the reference so that you don't end up re-using it.
        webView = null;
    }

}