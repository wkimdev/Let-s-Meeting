package com.wkimdev.letsmeeting.configure;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//레트로핏을 사용하기 위한 인스턴스 생성
public class RetrofitClientInstance {

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            //레트로핏 객체 초기화
            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(ApplicationConstants.API_SERVER_URL)
                    // Json을 변환해줄 Gson변환기 등록
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

