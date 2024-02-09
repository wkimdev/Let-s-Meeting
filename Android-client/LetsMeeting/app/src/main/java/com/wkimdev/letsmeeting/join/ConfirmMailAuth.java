package com.wkimdev.letsmeeting.join;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.auth.GMailSender;
import com.wkimdev.letsmeeting.configure.ServiceConstants;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

// 가입화면, 메일 인증하기 화면
public class ConfirmMailAuth extends AppCompatActivity {

    private static final String TAG = "ConfirmMailAuth";

    private EditText editTxt_email; //이메일입력란
    private Button btn_sendAuthCode; //인증코드 보내기 버튼

    private ImageView img_sendmail; //메일보냄 이미지
    private TextView txt_title2;    //메일보낸 후 안내 텍스트
    private EditText editTxt_authCode; //인증메일전송 입력칸
    private TextView txt_authTime;      //인증제한시간 카운트
    private TextView txt_auth_success; //인증성공 문자
    private TextView txt_auth_fail; //인증실패 문자
    private Button btn_next; //계속 버튼

    //인증코드
    private String GmailCode;
    private static int value;
    private int mailSend=0;

    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     *  - 뷰 바인딩
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_mail_auth);

        //인증메일 보내기
        editTxt_email = findViewById(R.id.editTxt_email);
        btn_sendAuthCode = findViewById(R.id.btn_sendAuthCode);

        //인증메일 보낸후 보여지는 뷰
        img_sendmail = findViewById(R.id.img_sendmail);
        txt_title2 = findViewById(R.id.txt_title2);
        editTxt_authCode = findViewById(R.id.editTxt_authCode);
        //txt_authTime = findViewById(R.id.txt_authTime);
        txt_auth_success = findViewById(R.id.txt_auth_success);
        txt_auth_fail = findViewById(R.id.txt_auth_fail);
        btn_next = findViewById(R.id.btn_next);



        //인증메일을 보내는 클릭 이벤트 발생
        //인증코드 시간초가 흐르는데 이때 인증을 마치지 못하면 인증 코드를 지우게 만든다.
        btn_sendAuthCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //메일을 보내주는 쓰레드
                MailTread mailTread = new MailTread();
                mailTread.start();

                if(mailSend==0){
                    value=300; //5분
                    //쓰레드 객체 생성
                    BackgrounThread backgroundThread = new BackgrounThread();
                    //쓰레드 스타트
                    //backgroundThread.start();
                    mailSend+=1;
                }else{
                    value = 300;
                }

                //이메일이 보내지면 이 부분을 실행시킨다.
                img_sendmail.setVisibility(View.VISIBLE);
                txt_title2.setVisibility(View.VISIBLE);
                editTxt_authCode.setVisibility(View.VISIBLE);
            }
        });


        //입력된 이메일 유효성 검사
        editTxt_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            //이메일작성시 유효성 체크
            @Override
            public void afterTextChanged(Editable s) {
                String emailValue = editTxt_email.getText().toString().trim();
                if (emailValue.matches(ServiceConstants.EMAIL_VALIDATION_VALUE) && s.length() > 0) {
                    //이메일이 유효할때 검정색깔
                    editTxt_email.setTextColor(Color.BLACK);
                } else {
                    //이메일이 유효하지 않을때 빨간색깔
                    editTxt_email.setTextColor(Color.RED);
                }
            }
        });

        //인증코드 유효성 체크
        editTxt_authCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            //인증코드 작성시 유효성 체크
            @Override
            public void afterTextChanged(Editable s) {
                //인증코드가 정상이면 아래 문구 노출 및 버튼 활성화
                //인증코드가 정상이 아니면 아래 실패  문구 노출 및 버튼 비활성화
                String authCodeValue = editTxt_authCode.getText().toString().trim();

                if (GmailCode.equals(authCodeValue) && s.length() > 0) {
                    txt_auth_fail.setVisibility(View.INVISIBLE);
                    txt_auth_success.setVisibility(View.VISIBLE);
                    btn_next.setEnabled(true);
                    btn_next.setBackgroundColor(getResources().getColor(R.color.green_dark));
                } else {
                    txt_auth_fail.setVisibility(View.VISIBLE);
                    txt_auth_success.setVisibility(View.INVISIBLE);
                    btn_next.setEnabled(false);
                    btn_next.setBackgroundColor(getResources().getColor(R.color.gray));
                }

            }
        });


        //인증성공 후 회원가입 화면이동
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), JoinActivity.class);
                intent.putExtra("joinEmail", editTxt_email.getText().toString());
                startActivity(intent);
                finish();
            }
        });
    }


    //메일 보내는 쓰레드
    class MailTread extends Thread{

        public void run(){
            // GMailSender("메일 송신자 이메일", "2차 비밀번호");
            GMailSender gMailSender = new GMailSender("wkimdev@gmail.com", "lbokkvmjsxdgvxfa");

            //인증코드
            GmailCode=gMailSender.getEmailCode();
            try {
                String bodyContent = "안녕하세요, Let's Meeting 입니다. 인증코드를 확인해주세요 : " + GmailCode;

                //GMailSender.sendMail(제목, 본문내용, 받는사람);
                gMailSender.sendMail("렛츠미팅 회원가입 이메일 인증", bodyContent , editTxt_email.getText().toString());


            } catch (SendFailedException e) {

            } catch (MessagingException e) {
                System.out.println("인터넷 문제"+e);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //시간초가 카운트 되는 쓰레드
    class BackgrounThread extends Thread{
        //300초는 5분
        //메인 쓰레드에 value를 전달하여 시간초가 카운트다운 되게 한다.

        public void run(){
            //300초 보다 밸류값이 작거나 같으면 계속 실행시켜라
            while(true){
                value-=1;
                try{
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                if(value<=0){
                    GmailCode="";
                    break;
                }
            }
        }
    }

}