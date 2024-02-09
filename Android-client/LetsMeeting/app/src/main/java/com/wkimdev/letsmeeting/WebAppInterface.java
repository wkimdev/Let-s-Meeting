package com.wkimdev.letsmeeting;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * 화상회의화면의 webview로 부터 아래 이벤트들을 수신하기 위한 인터페이스
 * - 상대방이 전송한 채팅메세지를 수신
 * - 호스트가 회의를 종료했을 경우 이벤트 수신
 * - 회의참여후 회의참여자 인원 수 초과했을 경우 이벤트 수신
 * */
public class WebAppInterface {

    private static final String TAG = "WebAppInterface";

    private Context mContext;

    // 회의를 나갈때 띄우는 프로그래스바
    private ProgressDialog progressDoalog;

    // 채팅메세지가 수신 되었을 때 회의화면에서 메세지리스트를 업데이트하기 위한 핸들러
    // -> 자바스크립트에 결합된 객체는, 객체가 생성된 스레드가(메인스레드: 화상회의 액티비) 아닌 다른 스레드에서 실행된다.
    //  그렇기 때문에 메인스레드에서 실행되어야 하는 동작은 핸들러로 실행해야 한다.
    private Handler mHandler;

    /** Instantiate the interface and set the context */
    public WebAppInterface(Context c, Handler mainHandler) {
        mContext = c;
        mHandler = mainHandler;
    }

    /**
     * datachannel로 부터 전송된 채팅메세지를 받는 인터페이스
     */
    @JavascriptInterface
    public void receiveChatMessage(String chatMsg, String profile, String remoteNickName, String remoteId) {

        // 메인스레드로 채팅메세지를 전달하기 위한 핸들러 등록
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                Bundle data = new Bundle();
                data.putString("message", chatMsg);
                data.putString("profile", profile);
                data.putString("remoteNickName", remoteNickName);
                data.putString("remoteId", remoteId);

                Message message = new Message();
                //setData를 이용해 번들의 데이터를 전송
                message.setData(data);

                //sendMessage()함수를 이용해 UI스레드에 의뢰
                //이때 넘기는 Message객체는 UI스레드에 넘기는 데이터를 담는 객체
                mHandler.sendMessage(message);
            }
        };
        //handler의 post()함수를 호출해 UI스레드에게 작업을 의뢰
        myRunnable.run();
    }

    /**
     * 호스트에 의해 회의를 나가기 팝업(확인 버튼만 있는 팝업) 노출 처리
     */
    @JavascriptInterface
    public void showLeaveMeetingPopup(String message) {
        Log.e(TAG, "1. 호스트에 의해 회의를 나가기 팝업 발생...!");

        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(mContext.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder ad = new AlertDialog.Builder(mContext);
                ad.setMessage(message);
                ad.setCancelable(false);
                ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e(TAG, "2. showLeaveMeetingPopup 확인 버튼 클릭...!");

                        dialog.dismiss();
                        progressDoalog = new ProgressDialog(mContext);
                        progressDoalog.setMessage("회의에서 나갑니다...!");
                        progressDoalog.show();

                        // 0.8초간 멈춘다음 이전화면으로 이동
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                progressDoalog.dismiss();
                                Intent intent = new Intent(mContext, MainActivity.class);
                                mContext.startActivity(intent);
                            }
                        }, 800);
                    }
                });
                ad.show();
            }
        };
        mainHandler.post(myRunnable);
    }

    /**
     * 회의참여자 인원수가 초과했을때 회의에 진입못하도록 팝업 발생
     */
    @JavascriptInterface
    public void joinReject() {
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(mContext.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder ad = new AlertDialog.Builder(mContext);
                ad.setMessage("참가인원이 초과하여 회의에 입장할 수 없습니다!");
                ad.setCancelable(false);
                ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e(TAG, "3. showLeaveMeetingPopup 확인 버튼 클릭...!");

                        dialog.dismiss();
                        progressDoalog = new ProgressDialog(mContext);
                        progressDoalog.setMessage("회의에서 나갑니다...!");
                        progressDoalog.show();

                        // 0.8초간 멈춘다음 이전화면으로 이동
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                progressDoalog.dismiss();
                                Intent intent = new Intent(mContext, MainActivity.class);
                                mContext.startActivity(intent);
                            }
                        }, 800);
                    }
                });
                ad.show();
            }
        };
        mainHandler.post(myRunnable);

    }

}
