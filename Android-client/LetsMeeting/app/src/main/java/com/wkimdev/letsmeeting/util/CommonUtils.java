package com.wkimdev.letsmeeting.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

//공통 유틸 클래스
public class CommonUtils {

    //String 암호화 메소드
    public static String getHash(String str) {
        String digest = "";
        try{

            //암호화
            MessageDigest sh = MessageDigest.getInstance("SHA-256"); // SHA-256 해시함수를 사용
            sh.update(str.getBytes()); // str의 문자열을 해싱하여 sh에 저장
            byte byteData[] = sh.digest(); // sh 객체의 다이제스트를 얻는다.

            //얻은 결과를 string으로 변환
            StringBuffer sb = new StringBuffer();
            for(int i = 0 ; i < byteData.length ; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            digest = sb.toString();
        }catch(NoSuchAlgorithmException e) {
            e.printStackTrace(); digest = null;
        }
        return digest; // 결과  return
    }



    //String 빈값 체크 메소드
    public static boolean isStringEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }


    //날짜 포맷 변환
    //요청파람포맷 : yyyy-mm-dd
    //응답포맷 : yyyy.mm.dd
    public static String formatDate(String startDate) {
        //빈값 기준으로 앞의 날짜만 가져오기
        int idx = startDate.indexOf(" ");
        String sDate = startDate.substring(0, idx);
        return sDate.replace("-", ".");
    }


    //날짜 포맷 변환
    //요청파람포맷 : yyyy.mm.dd
    //응답포맷 : yyyy-mm-dd
    public static String formatDate2(String startDate) {
        return startDate.replace(".", "-");
    }


    //날짜 포맷 변환 yyyy-MM-dd hh:mm:ss => 오전/오후 hh:mm
    public static String formatTime(String startDate) {

        DateFormat reqDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DateFormat resDateFormat = new SimpleDateFormat("a hh:mm");
        Date datetime = null;

        try {
            //문자열을 파싱해서 Date객체를 만들어준다
            datetime = reqDateFormat.parse(startDate);
        } catch (ParseException e) {
            //패턴과다른 문자열이 입력되면 parse exception이 발생된다
            e.printStackTrace();
        }
        return resDateFormat.format(datetime);
    }


    //문자열을 Date객체로 변경한 뒤 원하는 포맷으로 변경해주는 처리를 함
    //요청파람포맷 : hh:mm:ss
    //응답포맷 : 오후 hh:mm 포맷
    public static String formatTimeToAMPM(String startTime) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        DateFormat dateFormat2 = new SimpleDateFormat("a hh:mm");
        Date datetime = null;

        try {
            //문자열을 파싱해서 Date객체를 만들어준다
            datetime = dateFormat.parse(startTime);
        } catch (ParseException e) {
            //패턴과다른 문자열이 입력되면 parse exception이 발생된다
            e.printStackTrace();
        }
        //Date객체로 변환한 뒤, 원하는 포맷으로 다시 변경 후 리턴한다
        return dateFormat2.format(datetime);
    }


    //회의 시간포맷팅 처리
    //요청파람포맷 : hh:mm:ss
    //응답포맷 : 오후 hh:mm
    public static String formatTimeVer2(String startTime) {
        DateFormat dateFormat = new SimpleDateFormat("a hh:mm");
        DateFormat dateFormat2 = new SimpleDateFormat("hh:mm:ss");

        Date datetime = null;

        try {
            //문자열로 전달된 데이트포맷으로 파싱해서 Date객체를 만들어준다
            //startTime 문자열을 date객체로 변환
            datetime = dateFormat2.parse(startTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Date객체로 변환한 뒤, 원하는 포맷으로(오후 hh:mm) 다시 변경 후 리턴한다
        return dateFormat.format(datetime);
    }


    //한글시간에서 숫자시간으로 변경
    //요청파람포맷 : 오전/오후 12:21
    //응답포맷 : HH:mm:ss
    public static String convertTimeFormat(String startTime) {

        DateFormat dateFormat = new SimpleDateFormat("a hh:mm");
        DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss");

        Date datetime = null;

        try {
            //문자열로 전달된 데이트포맷으로 파싱해서 Date객체를 만들어준다
            //startTime 문자열을 date객체로 변환
            datetime = dateFormat.parse(startTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Date객체로 변환한 뒤, 원하는 포맷으로 다시 변경 후 리턴한다
        return dateFormat2.format(datetime);
    }


    //@TODO: 없애기!
    //숫자 타입을 문자로 변환
    //1의 자리를 2자리수로 변경 (ex: 1 -> 01)
    public static String convertDateValue(int value) {

        String resultValue = "";
        if (value < 10) {
            resultValue = "0" + value;
        } else {
            resultValue = String.valueOf(value);
        }
        return resultValue;
    }


    //회의날짜와 오늘날짜 비교
    // meetingDate > currentDate => 1
    // meetingDate < currentDate => -1
    // meetingDate == currentDate => 0
    public static int compareDate(String meetingDate, String currentDate) {
        return meetingDate.compareTo(currentDate);
    }


    //현재시각을 hh:mm a 포맷으로 리턴
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("a hh:mm");
        return now.format(dateTimeFormatter);
    }


    //DB에서 호출한 회의시간 포맷을 변경
    //요청파람포맷 : yyyy-MM-dd HH:mm:ss
    //응답포맷 : MM-dd a hh:mm
    public static String convertMeetingDateFormat(String meetingDate) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DateFormat dateFormat2 = new SimpleDateFormat("MM월 dd일 a hh:mm");

        Date datetime = null;

        try {
            //문자열로 전달된 데이트포맷으로 파싱해서 Date객체를 만들어준다
            //startTime 문자열을 date객체로 변환
            datetime = dateFormat.parse(meetingDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Date객체로 변환한 뒤, 원하는 포맷으로 다시 변경 후 리턴한다
        return dateFormat2.format(datetime);
    }

}
