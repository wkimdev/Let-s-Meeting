package com.wkimdev.letsmeeting.reservation;

import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.wkimdev.letsmeeting.R;

import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.meeting.PushMeetingActivity;
import com.wkimdev.letsmeeting.util.CommonUtils;

//회의시간이 되면, 알림을 수신할 수 있도록 하는 리시버
public class NotificationReceiver extends BroadcastReceiver {

    private String TAG = this.getClass().getSimpleName();

    NotificationManager manager;
    NotificationCompat.Builder builder;

    //오레오 이상은 반드시 채널을 설정해줘야 Notification이 작동함
    private static String CHANNEL_ID = "channel1";
    private static String CHANNEL_NAME = "Channel1";

    boolean isLogin; //로그인여부

    //수신되는 인텐트 - The Intent being received.
    //미로그인 상태면 푸시알림을 받지 않는다.
    @Override
    public void onReceive(Context context, Intent intent) {
        String email = PreferenceManager.getString(context, "email");
        Log.e(TAG, "onReceive 알람이 들어옴!!, 이메일 값 체크 >>>>>" + email);

        isLogin = !CommonUtils.isStringEmpty(email);

        Log.e(TAG, "onReceive 알람이 들어옴!!, 로그인 여부 체크 >>>>>" + isLogin);

        //회의 호스트의경우, 회의 상태값을 open으로 변경해야 하기 때문에, 로그인상태의 경우만 푸시알림을 보낸다.
        if (isLogin) {

            String contentValue = intent.getStringExtra("content");
            //Log.e(TAG, "onReceive contentValue값 확인 : " + contentValue);
            int meetingId = intent.getIntExtra("meetingId", 0);

            builder = null;
            //푸시 알림을 보내기위해 시스템에 권한을 요청하여 생성
            manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            /* 안드로이드 오레오 버전 대응 */
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                manager.createNotificationChannel(
                        new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                );
                builder = new NotificationCompat.Builder(context, CHANNEL_ID);
            } else {
                builder = new NotificationCompat.Builder(context);
            }

            //알림창 클릭 시 지정된 activity 화면으로 이동
            Intent intent2 = new Intent(context, PushMeetingActivity.class);
            intent2.putExtra("meetingId", meetingId);
            // FLAG_UPDATE_CURRENT ->
            // 설명된 PendingIntent가 이미 존재하는 경우 유지하되 추가 데이터를 이 새 Intent에 있는 것으로 대체함을 나타내는 플래그입니다.
            // getActivity, getBroadcast 및 getService와 함께 사용
            PendingIntent pendingIntent = PendingIntent.getActivity(context,101,intent2,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            //알림창 제목
            builder.setContentTitle(contentValue); //회의명노출
            //builder.setContentText(intent.getStringExtra("content")); //회의 내용
            //알림창 아이콘
            builder.setSmallIcon(R.drawable.letsmeeting_logo);
            //알림창 터치시 자동 삭제
            builder.setAutoCancel(true);

            builder.setContentIntent(pendingIntent);

            //푸시알림 빌드
            Notification notification = builder.build();

            //NotificationManager를 이용하여 푸시 알림 보내기
            manager.notify(1,notification);
        }

    }
}