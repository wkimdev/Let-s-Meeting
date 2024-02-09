package com.wkimdev.letsmeeting.setting;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.wkimdev.letsmeeting.HomeActivity;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.service.MemberService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 프로필 셋팅 화면
 * - 프로필 사진 변경, 닉네임 변경, 비밀번호 변경, 회원탈퇴 제공하는 화면
 */
public class SettingActivity extends AppCompatActivity {

    private String TAG = "SettingActivity";

    private TextView txt_emailValue; //계정 이메일
    private TextView txt_nameValue; //이름
    private Button btn_logout; //로그아웃 버튼
    private CircleImageView circleImg_profile; //프로필사진

    private ProgressBar progressBar; //로그아웃시 로딩바호출

    private ImageButton imgBtn_changeName; //이름변경 버튼
    private ImageButton imgBtn_changePwd; //비번변경 버튼
    private ImageButton imgBtn_changeProfile; //프로실사진 이미지 변경 버튼
    private ImageButton imgBtn_leaveMember; //회원탈퇴

    private String email;
    private String name;
    private String profile; //프로필 사진

    private Uri mImageCaptureUri; //프로필이미지 처리를 위한 Uri전역 변수
    private String tempName;      //임시 파일명
    private File image; //이미지 디렉토리를 만들때 사용하는 변수

    private boolean isAlbumPermission; //앨범 권한설정 여부
    private boolean isCameraPermission; //카메라 권한설정 여부

    private boolean isAlbumRequest; //프로필사진을 앨범에서 선택할 경우

    private Sprite fadingCircle; //노출되는 로딩바 애니메이션 선언

    //프로필사진 업로드/다운로드를 위한 레트로핏 서비스 API 객체 생성
    private MemberService service = RetrofitClientInstance.getRetrofitInstance().create(MemberService.class);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Log.e(TAG, "onCreate: 호출");

        txt_emailValue = findViewById(R.id.txt_emailValue);
        txt_nameValue = findViewById(R.id.txt_nameValue);
        btn_logout = findViewById(R.id.btn_logout);
        imgBtn_changeName = findViewById(R.id.imgBtn_changeName);
        imgBtn_changePwd = findViewById(R.id.imgBtn_changePwd);
        imgBtn_changeProfile = findViewById(R.id.imgBtn_changeProfile);
        circleImg_profile = findViewById(R.id.circleImg_profile);
        imgBtn_leaveMember = findViewById(R.id.imgBtn_leaveMember);

        progressBar = findViewById(R.id.spin_kit);; //로그아웃시 로딩바 노출
        fadingCircle = new FadingCircle(); //노출되는 로딩바 애니메이션 스타일

        //sharedpref에서 로그인 사용자 정보를 가져와 셋팅
        email = PreferenceManager.getString(getApplicationContext(), "email");
        name = PreferenceManager.getString(getApplicationContext(), "name");
        profile = PreferenceManager.getString(getApplicationContext(), "profile");

        //프로필사진값이 있으면, 서버에서 이미지를 가져와 뷰에 담는다
        if (!TextUtils.isEmpty(profile) || profile == null) {
            //레트로핏으로 서버에 이미지 요청 후 뷰에 프로필 사진 바인딩
            downloadProfileImg(profile);
        }


        //변경된 이름이 있으면 UI에 노출
        Intent intent = getIntent();
        if(!TextUtils.isEmpty(intent.getStringExtra("changeName"))) {
            txt_nameValue.setText(intent.getStringExtra("changeName"));
        } else {
            //sharedPreference에서 이름 정보를 가져와 셋팅
            txt_nameValue.setText(name);
        }
        txt_emailValue.setText(email);

    }


    @Override
    protected void onResume() {
        super.onResume();

        //Log.e(TAG, "onResume: 호출");

        // 사진 선택 다이알로그 팝업 발생
        imgBtn_changeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePickOption();
            }
        });

        //이름 변경 화면으로 이동
        imgBtn_changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChangeNameActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("currentName", name);
                startActivity(intent);
            }
        });

        //비밀번호 변경 화면으로 이동
        imgBtn_changePwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChangePwdActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            }
        });

        //로그아웃 버튼 클릭 후 로딩바 뜨고 앱처음 화면으로 이동
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //로딩애니메이션 노출
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminateDrawable(fadingCircle); //진행바 모양을 그리는데 사용되는 드로어블 정의

                // 1.5초간 스피너를 띄운 뒤 메인화면으로 이동
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        //로그인 정보 삭제
                        PreferenceManager.removeKey(getApplicationContext(), "name");
                        PreferenceManager.removeKey(getApplicationContext(), "email");
                        PreferenceManager.removeKey(getApplicationContext(), "profile");

                        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                }, 1500);
            }
        });

        //회원탈퇴 화면으로 이동
        imgBtn_leaveMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingActivity.this, MemberLeaveActivity.class));
                finish();
            }
        });
    }


    //레트로핏을 통해 서버에서 이미지를 다운받는 코드
    private void downloadProfileImg(String filename) {

        service.downloadImage(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                /**
                 * decodeStream를 실행할 때, 아래 오류 발생하여 쓰레드 생성 후 실행
                 * android.os.NetworkOnMainThreadException
                 *  at android.os.StrictMode$AndroidBlockGuardPolicy.onNetwork(StrictMode.java:1605)
                 * 발생한 원인은 이해하지 못하여 관련글 링크함
                 * - https://stackoverflow.com/questions/38507517/android-networkonmainthreadexception-occurs-when-using-retrofit-in-a-new-thread
                 * */
                new Thread() {
                    public void run() {
                        //response body를 byteStream로 읽는다
                        InputStream is = response.body().byteStream();
                        //input stream을 디코딩해서 bitmap으로 변환한다
                        Bitmap bitmap = BitmapFactory.decodeStream(is);

                        //변환된 bitmap을 뷰에 바인딩하기 위해 핸들러 등록
                        Handler handler = new Handler(Looper.getMainLooper());//?
                        handler.postDelayed(new Runnable() {
                            public void run() {
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


    //이미지 선택하는 다이알로그 팝업 생성 코드
    public void openImagePickOption() {

        final CharSequence[] items = { "사진 찍기", "앨범에서 사진선택", "취소" };

        AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
        builder.setTitle("사진 선택");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (items[item].equals("사진 찍기")) {


                    //카메라에서 이미지 가져오기
                    if (PreferenceManager.getBoolean(getApplicationContext(), "isCameraAlbumPermission")) {

                        try {
                            doTakePhotoAction();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //사용자의 권한허용 여부를 처리하는 메소드
                        tedPermission();

                    }

                } else if (items[item].equals("앨범에서 사진선택")) {

                    //카메라만 허용했는데, 같이 허용되고 있어 임시로 아래 처럼 처리
                    if (PreferenceManager.getBoolean(getApplicationContext(), "isCameraAlbumPermission")) {
                        try {
                            doTakeAlbumAction();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        tedPermission();
                    }

                } else if (items[item].equals("취소")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    //사용자의 권한허용 여부를 처리하는 메소드
    private void tedPermission() {

        //카메라, 앨범접근 권한 허용 리스너 등록
        PermissionListener cameraPermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                // 권한 요청 성공
                PreferenceManager.setBoolean(getApplicationContext(), "isCameraAlbumPermission", true);
            }

            @Override
            public void onPermissionDenied(List<String> list) {
                // 권한 요청 실패
                PreferenceManager.setBoolean(getApplicationContext(), "isCameraAlbumPermission", false);
            }
        };

        //카메라 접근권한 체크
        TedPermission.create()
                .setPermissionListener(cameraPermissionListener)
                .setRationaleMessage(getResources().getString(R.string.permission_1)) //권한필요 문구 노출
                .setDeniedMessage(getResources().getString(R.string.permission_2)) //거부했을 경우 안내 문구 노출
                .setDeniedTitle("접근권한허용 거부")
                .setDeniedCloseButtonText("취소")
                .setRationaleConfirmText("확인")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .check();

    }


    //카메라에서 이미지 가져오기
    private void doTakePhotoAction() throws IOException {
        //MediaStore - Media Provider가 제공하는 파일들을 접근할 수 있도록 도와주는 API의 묶음
        //Media Provider - 단말에 저장된 이미지,동영상,오디오 파일의 정보를 제공하는 프로바이더
        //카메라 파일 공유방식으로 이미지를 로드하기 위한 인텐트 선언
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // 카메라로 찍은 사진을 불러오기 위해선, 임시 파일 경로가 필요하기 때문에,
        // 임시로 사용할 파일의 경로를 생성한다

        //이미지가 저장될 폴더 이름 지정
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures/");
        //디렉토리 생성
        if (!storageDir.exists()) storageDir.mkdirs();

        //지정된 디렉토리(storageDir)에 빈 파일 생성
        //prefix - 파일명. 그 뒤에 임의의 숫자가 더 붙는다. ex) prerix : test -> test1234578
        image = File.createTempFile("JPEG_", ".jpg", storageDir);

        //image로 부터 Uri를 만든다
        //file://이 아닌, Cotent:// 로 파일 Uri경로를 바꾸기 위해 FileProvider를 사용한다
        mImageCaptureUri = FileProvider.getUriForFile(this,
                "com.wkimdev.letsmeeting.fileprovider", image);

        //카메라에서 찍은 사진이 저장될 주소를 전달해준다
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

        //카메라 촬영 후 결과값을 받는 코드
        startActivityResultForCamera.launch(intent);
    }


    /**
     * 앨범에서 이미지 가져오기
     */
    private void doTakeAlbumAction() throws IOException {
        isAlbumRequest = true;

        // 앨범 호출
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityResultForAlbum.launch(intent);
    }


    // 카메라 촬영 후 결과값을 받는 처리
    // 촬영된 이미지 결과값으로 CROP을 시킨다
    ActivityResultLauncher<Intent> startActivityResultForCamera = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) { //성공

                        //crop을 하기 위한 이미지를 가져와 crop activity에서 사용한다
                        CropImage.activity(mImageCaptureUri).setGuidelines(CropImageView.Guidelines.ON)
                                .start(SettingActivity.this);
                    }
                }
            });


    // 앨범에서 이미지 선택 후 결과값을 받는 처리
    // 앨범에서 선택한 이미지 결과값을 받아, CROP을 시킨다
    ActivityResultLauncher<Intent> startActivityResultForAlbum = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) { //성공

                        Intent data = result.getData();
                        mImageCaptureUri = data.getData();

                        //crop을 하기 위한 이미지를 가져와 crop activity에서 사용한다
                        CropImage.activity(mImageCaptureUri).setGuidelines(CropImageView.Guidelines.ON)
                                .start(SettingActivity.this);

                    }
                }
            });


    //CROP결과를 받기 위한 메소드
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {//크롭 성공시

                //이미지뷰에 크롭된 이미지 Uri를 넣는다
                circleImg_profile.setImageURI(result.getUri());

                if(isAlbumRequest) {
                    image = new File(result.getUri().getPath());
                }



                // Uri 타입의 파일경로를 가지는 RequestBody 객체 생성
                RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), image);

                // RequestBody로 Multipart.Part 객체 생성
                // 파라미터1) 서버에서 받는 키값(String)
                // 파라미터2) 파일이름(String)
                // 파라미터3) 파일 경로를 가지는 RequestBody 객체
                MultipartBody.Part filePart = MultipartBody.Part.createFormData("profile", image.getName(), fileBody);

                //추가적인 요청 body값 생성
                Map<String, RequestBody> map = new HashMap<>();
                RequestBody emailValue = RequestBody.create(MediaType.parse("text/plain"), email);
                map.put("email", emailValue);

                //레트로핏 요청
                updateProfileImg(filePart, map);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                Log.e(TAG, "onActivityResult: ", error);
            }

        }
    }


    //멤버 프로필 사진 업데이트
    private void updateProfileImg(MultipartBody.Part filePart, Map<String, RequestBody> requestBody) {

        service.uploadProfileImg(filePart, requestBody).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {
                Log.e(TAG, "onResponse: " + "레트로핏 응답 성공 !" );

                PreferenceManager.setString(SettingActivity.this,"profile", image.getName());

                //임시로 만들었던 파일 삭제
                Log.e(TAG, "file 존재여부 확인 "+ image.exists());
                if(image.exists()){
                    Log.e(TAG, "파일삭제 코드 동작!!!");
                    image.delete();
                }

                //앨범선택 요청 구분값 초기화
                if(isAlbumRequest) {
                    isAlbumRequest = false;
                }
            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {
                Log.e(TAG, "onResponse: " + "레트로핏 응답 실패 !"  + t);
            }
        });
    }

}