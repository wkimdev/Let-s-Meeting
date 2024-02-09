package com.wkimdev.letsmeeting.configure;

/**
 * 액티비티 서비스들내에서 사용하는 상수 선언
 */
public class ServiceConstants {

    public static final String EMAIL_VALIDATION_VALUE =
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    //비밀번호 유효성 체크(영문자,숫자 포함)
    //문자와 숫자 포함해서 1개이상 들어갔는지 체크함
    public static final String PASSWORD_VALIDATION_VALUE = "^(?=.*[0-9])(?=.*[a-zA-Z])([a-zA-Z0-9]+)$";

}
