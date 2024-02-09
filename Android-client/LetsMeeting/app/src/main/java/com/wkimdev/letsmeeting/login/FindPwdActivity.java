package com.wkimdev.letsmeeting.login;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.auth.GMailSender;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.join.JoinActivity;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.auth.RequestPwdUpdateData;
import com.wkimdev.letsmeeting.model.login.LoginResponse;
import com.wkimdev.letsmeeting.service.MemberService;
import com.wkimdev.letsmeeting.util.CommonUtils;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


// 비밀번호 재발급 화면
public class FindPwdActivity extends AppCompatActivity {

    private String TAG = "FindPwdActivity";

    private EditText editTxt_email; //입력한 이메일
    private Button btn_send; //이메일전송 버튼

    //임시비밀번호
    private String tempPwd;

    //이메일 유효성 체크
    private String emailValidation = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    //유효한 이메일인지 확인 및 재발급된 비밀번호로 업데이트 하기 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);


    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pwd);

        editTxt_email = findViewById(R.id.editTxt_email);
        btn_send = findViewById(R.id.btn_send);

    }

    /**
     * 버튼 이벤트 등록
     */
    @Override
    protected void onResume() {
        super.onResume();

        //이메일입력시 유효성 체크
        editTxt_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                //이메일입력시 유효성 체크
                String emailValue = editTxt_email.getText().toString().trim();
                if (emailValue.matches(emailValidation) && s.length() > 0) {

                    //이메일이 유효할때 버튼 활성화
                    btn_send.setEnabled(true);
                    btn_send.setBackgroundColor(getResources().getColor(R.color.green_dark));

                } else {

                    //이메일이 유효하지 않을때 버튼 비활성화
                    btn_send.setEnabled(false);
                    btn_send.setBackgroundColor(getResources().getColor(R.color.gray));
                }
            }
        });


        //전송버튼 클릭 후, 임시비밀번호 발급 팝업 발생
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //유효한 이메일인지 확인
                checkEmailExist(editTxt_email.getText().toString().trim());
            }
        });
    }

    //유효한 이메일인지 확인 하는 체크 메소드
    private void checkEmailExist(String email) {

        service.memberInfo(email).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                LoginResponse result = response.body();

                if (200 == result.getStatusCode()) {//응답성공

                    //임시비번발급 메일을 보내주는 쓰레드
                    MailTread mailTread = new MailTread();
                    mailTread.start();

                } else if (201 == result.getStatusCode()) {//가입되지 않은 계정

                    AlertDialog.Builder ad = new AlertDialog.Builder(FindPwdActivity.this);

                    ad.setMessage("이 이메일주소에 연결된 계정이 없습니다. \n렛츠미팅에 가입하시려면 아래 버튼을 클릭해주세요.");
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(FindPwdActivity.this, JoinActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            startActivity(intent);
                        }
                    }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    ad.show();

                } else { //오류 발생

                    AlertDialog.Builder ad = new AlertDialog.Builder(FindPwdActivity.this);

                    ad.setMessage("오류가 발생했습니다. 잠시 후에 다시 시도해주세요");
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    ad.show();

                }


            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });
    }


    //비밀번호 재설정 코드를 메일로 전송해주는 쓰레드
    class MailTread extends Thread{

        public void run(){
            // GMailSender("메일 송신자 이메일", "2차 비밀번호");
            GMailSender gMailSender = new GMailSender("wkimdev@gmail.com", "lbokkvmjsxdgvxfa");

            //임시비밀번호 발급
            tempPwd = gMailSender.getTempPwd();

            //비밀번호 난수화
            String passwordHash = CommonUtils.getHash(tempPwd);

            RequestPwdUpdateData request = new RequestPwdUpdateData(
                    editTxt_email.getText().toString().trim(), passwordHash);
            //API를 통해 비밀번호 업데이트 요청
            updatePwd(request);


            try {

                String bodyContent = "안녕하세요, Let's Meeting 입니다. 임시비밀번호를 확인해주세요 : " + tempPwd;

                //GMailSender.sendMail(제목, 본문내용, 받는사람);
                gMailSender.sendMail("렛츠미팅 임시 비밀번호 발급", bodyContent , editTxt_email.getText().toString());


            } catch (SendFailedException e) {

            } catch (MessagingException e) {
                System.out.println("인터넷 문제"+e);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //레트로핏으로 비밀번호 업데이트를 수행하는 메소드
    private void updatePwd(RequestPwdUpdateData requestPwdUpdateData) {

        service.updatePwd(requestPwdUpdateData).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                //임시비밀번호 업데이트 이후, 리턴결과가 성공일때 팝업 발생
                ResponseCommonData result = response.body();
                if (result.getStatusCode() == 200) {

                    // 임시 비밀번호를 발급했다는 팝업 발생
                    AlertDialog.Builder ad = new AlertDialog.Builder(FindPwdActivity.this);
                    ad.setTitle("메일로 임시 비밀번호를 발급했습니다");
                    ad.setMessage("발급받은 비밀번호로 로그인 후 비밀번호를 변경해주세요!.");
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                    });
                    ad.show();

                } else {

                    AlertDialog.Builder ad = new AlertDialog.Builder(FindPwdActivity.this);
                    ad.setMessage("오류 발생!");
                    ad.setCancelable(false);
                    ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    ad.show();
                }


            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {

                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();

            }
        });

    }

}