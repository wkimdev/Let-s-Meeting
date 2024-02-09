package com.wkimdev.letsmeeting.join;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.ApplicationConstants;

//개인정보처리방침 약관보기 액티비티
public class PrivacyAgreementActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private ConstraintLayout layout_privacyRule;
    private ImageView iv_moveToback;
    private WebView webView;

    /**
     * 앱 실행시 필요한 기능들 초기화 및 연동 선언
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement2);

        layout_privacyRule = findViewById(R.id.layout_privacyRule);
        iv_moveToback = findViewById(R.id.iv_moveToback);
        webView = findViewById(R.id.wv_privacyRule);

        //웹뷰에 세부적인 내용을 셋팅해준다.
        this.setUpWebViewDefaults(webView);

        //loadUrl은 주어진 URL을 로드 시키는 메소드
        // /content/privacy경로에 있는 약관내용을 웹뷰로 가져오도록 처리함
        webView.loadUrl(ApplicationConstants.WEBRTC_SERVER_URL + "/content/privacy");
    }

    /**
     * 버튼 이벤트 등록
     */
    @Override
    protected void onResume() {
        super.onResume();

        //뒤로가기 버튼클릭 이벤트 처리
        iv_moveToback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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

    //회의참여 액티비티 종료 후 웹뷰도 종료 되도록 처리하는 메소드
    //웹뷰를 종료시키지 않으면, 같은 회의를 다시 참여했을 경우 이전 웹뷰가 남아있어 화면이 노출되지 않는 등의 오류가 발생됨
    public void destroyWebView() {
        Log.e(TAG, "destroyWebView call !!!!");

        // Make sure you remove the WebView from its parent view before doing anything.
        layout_privacyRule.removeAllViews();

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