package com.wkimdev.letsmeeting.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.wkimdev.letsmeeting.model.auth.RequestPwdUpdateData;
import com.wkimdev.letsmeeting.setting.ChangePwdActivity;

//다이알로그 팝업 처리 공통 클래스
public class CommonPopupUtils {


//    public void showPopup(Activity activity, String message) {
//
//        //로딩바 뜨고 팝업 발생
//        AlertDialog.Builder ad = new AlertDialog.Builder(activity);
//        ad.setMessage(message);
//        ad.setCancelable(false);
//        ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//
//                //비밀번호 난수화
//                String passwordHash = CommonUtils.getHash(newPwdValue);
//                RequestPwdUpdateData requestPwdUpdateData = new RequestPwdUpdateData(email, passwordHash);
//                //비밀번호 업데이트 실행
//                updatePwd(requestPwdUpdateData);
//
//            }
//        });
//        ad.setNegativeButton("취소", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//        ad.show();
//    }

}
