package com.wkimdev.letsmeeting;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.join.PrivacyAgreementActivity;
import com.wkimdev.letsmeeting.join.ServiceAgreementActivity;
import com.wkimdev.letsmeeting.service.MemberService;
import com.wkimdev.letsmeeting.setting.SettingActivity;

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 셋팅화면 프래그먼트
public class SettingFrag extends Fragment {

    private String TAG = "SettingFrag";

    private View view;
    private TextView txt_memName;
    private TextView txt_email;
    private ImageButton imgBtn_goMyProfile; //내 프로필 화면

    private ImageButton btn_serviceAgreement;
    private ImageButton btn_privacyAgreement;

    private CircleImageView circleImg_profile;
    private String profile; //프로필 사진

    //프로필사진 업로드/다운로드를 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);


    //프래그먼트 생성시 시스템에서 호출함.
    //onDestroyView() 호출이후, 다시 프래그먼트로 돌아왔을때 호출됨
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.e(TAG, "onCreate: 호출!!!");
    }

    //프래그먼트가 있는 액티비티의 생명주기는 해당 프래그먼트의 생명주기에 영향을 미친다
    //Activiy와 같은 생명주기 콜백메소드
    //액티비티가 시작됨 상태에 들어가면 호출됨
    @Override
    public void onStart() {
        super.onStart();
        //Log.e(TAG, "onStart: 호출!!!");
    }

    //프래그먼트에 맞는 UI를 그리기 위해 View를 반환하는 콜백메소드
    //프래그먼트의 레이아웃 루트이기 때문에 UI를 제공하지 않을 경우, null을 반환
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag3, container, false);
        //Log.e(TAG, "onCreateView: 호출!!!");
        return view;
    }

    //onCreateView가 호출 후 return한 뒤에 곧바로 실행되는 콜백 메소드
    //View가 완전히 생성되었을때 호출된다.
    //onCreateaView가 아닌, 아래 콜백에서 뷰바인딩을 하도록 권장
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Log.e(TAG, "onViewCreated: 호출!!! ");

        //프래그먼트 레아아웃의 루트view를 가져와 findViewById를 실행
        txt_memName = view.findViewById(R.id.txt_memName);
        txt_email = view.findViewById(R.id.txt_email);
        imgBtn_goMyProfile = view.findViewById(R.id.imgBtn_goMyProfile);
        circleImg_profile = view.findViewById(R.id.circleImg_profile);

        btn_privacyAgreement = view.findViewById(R.id.btn_privacyAgreement);
        btn_serviceAgreement = view.findViewById(R.id.btn_serviceAgreement);
    }

    //활동이 재개됨에 따라 초기화 작업을 수행하는 콜백구문
    //앱이 사용자와 상호작용 하는 구문
    //이벤트 발생 후 포커스가 떠나기 전까지 머무르는 상태
    @Override
    public void onResume() {
        super.onResume();

        Log.e(TAG, "onResume: 호출!!!");

        //UI내 데이터를 업데이트하기 위한 처리

        txt_email.setText(PreferenceManager.getString(getContext(), "email"));
        txt_memName.setText(PreferenceManager.getString(getContext(), "name"));

        //프로필사진이 있을 경우, 프사를 뷰에 바인딩 한다
        profile = PreferenceManager.getString(getContext(), "profile");
        Log.e(TAG, "프로필사진 값 확인...!!! :" + profile);

        //프로필사진값이 있으면, 서버에서 이미지를 가져와 뷰에 담는다
        if (!TextUtils.isEmpty(profile) || profile == null) {
            //레트로핏으로 서버에 이미지 요청 후 뷰에 프로필 사진 바인딩
            downloadProfileImg(profile);
        }

        //내 프로필 화면 으로 이동
        imgBtn_goMyProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), SettingActivity.class));
            }
        });

        //개인정보처리방침 내용 화면 이동
        btn_privacyAgreement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PrivacyAgreementActivity.class));
            }
        });

        //서비스이용약관 내용 화면 이동
        btn_serviceAgreement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ServiceAgreementActivity.class
                ));
            }
        });

    }


    //사용자가 프래그먼트를 떠나면 호출됨
    //사용자가 돌아오지 않을 수 도있기 때문에, 현재 사용자 세션을 넘어 지속되어야 하는 변경사항을 저장한다
    @Override
    public void onPause() {
        super.onPause();
        //Log.e(TAG, "onPause: 호출!!!");
    }

    //액티비티가 완전히 가려지면 호출됨
    //앱이 보이지 않을 동안 필요하지 않는 리소스를 해제하거나 조정하는 구문을 작성한다
    @Override
    public void onStop() {
        super.onStop();
        //Log.e(TAG, "onStop: 호출!!!");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //Log.e(TAG, "onDestroyView: 호출!!!");
    }

    //레트로핏을 통해 서버에서 이미지를 다운받는 코드
    private void downloadProfileImg(String filename) {
        service.downloadImage(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                // decodeStream를 실행할 때, android.os.NetworkOnMainThreadException 발생하여 쓰레드 생성 후 실행
                // at android.os.StrictMode$AndroidBlockGuardPolicy.onNetwork(StrictMode.java:1605)
                new Thread() {
                    public void run() {
                        //response body를 byteStream로 읽는다
                        InputStream is = response.body().byteStream();
                        //input stream을 디코딩해서 bitmap으로 변환한다
                        Bitmap bitmap = BitmapFactory.decodeStream(is);

                        //변환된 bitmap을 뷰에 바인딩하기 위해 핸들러 등록
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                Log.e(TAG, "이미지 셋팅 구문 ...! " + bitmap );
                                circleImg_profile.setImageBitmap(bitmap);
                            }
                        }, 0);
                    }
                }.start();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "레트로핏으로 서버 프로필사진 이미지 다운로드 실패 onFailure: ", t);
            }
        });
    }

}
