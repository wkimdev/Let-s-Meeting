package com.wkimdev.letsmeeting;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.meeting.EnterMeetingActivity;
import com.wkimdev.letsmeeting.meeting.MeetingActivity;
import com.wkimdev.letsmeeting.meeting.NewMeetingActivity;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.model.meeting.RequestStatusUpdate;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeeting;
import com.wkimdev.letsmeeting.reservation.MeetingItemDecoration;
import com.wkimdev.letsmeeting.reservation.TodayMeetingAdapter;
import com.wkimdev.letsmeeting.service.MeetingService;

import java.time.LocalDate;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 홈 화면(메인화면에서 로그인 이후 진입하는 화면)
public class MeetingFrag extends Fragment implements OnItemClick {

    private static final String TAG = "MeetingFrag";
    private View view;

    private ImageView img_newMeeting; //새회의생성버튼
    private ImageView img_enterMeeting; //회의참여버튼


    private TextClock txt_currentDate;
    private LinearLayout title_meeting_today;
    private RecyclerView rv_todayMeeting;

    private NestedScrollView scroll_view;
    private TextView txt_todayMeeting;
    private ProgressBar progress_bar;
    private LinearLayout home_emptyMeeting; //오늘 회의 데이터가 없을 경우 노출

    //회의생성을 위한 레트로핏 서비스 API 객체 생성
    private MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);

    //리사이클러뷰에서 페이징 할 때 필요한 것
    private int limit = 5;
    private int page = 1;
    private String email;
    private int totalCont; //전체 회의리스트 갯수

    private ArrayList<MeetingDataResult> meetingDataResultList; //회의리스트
    private RecyclerView rc_TodayMeetingList; //회의리스트를 담을 리사이클러뷰 선언
    private TodayMeetingAdapter todayMeetingAdapter; //어뎁터 선언


    //프래그먼트에 맞는 UI를 그리기 위해 View를 반환하는 콜백메소드
    //프래그먼트의 레이아웃 루트이기 때문에 UI를 제공하지 않을 경우, null을 반환
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag1_new, container, false);
        return view;
    }

    //onCreateView가 호출 후 return한 뒤에 곧바로 실행되는 콜백 메소드
    //View가 완전히 생성되었을때 호출되는 된다.
    //onCreateaView가 아닌, 아래 콜백에서 뷰바인딩을 하도록 권장
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //프래그먼트 레아아웃의 루트view를 가져와 findViewById를 실행

        img_newMeeting = view.findViewById(R.id.img_newMeeting);
        img_enterMeeting = view.findViewById(R.id.img_enterMeeting);

        txt_currentDate = view.findViewById(R.id.txt_currentDate);
        title_meeting_today = view.findViewById(R.id.title_meeting_today);
        rv_todayMeeting = view.findViewById(R.id.rv_todayMeeting);
        home_emptyMeeting = view.findViewById(R.id.home_emptyMeeting);

        scroll_view = view.findViewById(R.id.scroll_view);
        txt_todayMeeting = view.findViewById(R.id.txt_todayMeeting);
        rc_TodayMeetingList = view.findViewById(R.id.rc_TodayMeetingList);
        progress_bar = view.findViewById(R.id.progress_bar);
    }

    //활동이 재개됨에 따라 초기화 작업을 수행하는 콜백구문
    //앱이 사용자와 상호작용 하는 구문
    //이벤트 발생 후 포커스가 떠나기 전까지 머무르는 상태
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onResume() {
        super.onResume();

        //호출페이지 초기화
        page = 1;
        //회의리스트 초기화
        meetingDataResultList = new ArrayList<>();

        email = PreferenceManager.getString(getContext(), "email");


        //화면최초 진입시 리사이클러뷰로 회의리스트 노출
        todayMeetingAdapter = new TodayMeetingAdapter(meetingDataResultList, getContext(), this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        //화면에 새로 진입할때마다 이전 decoration가 쌓여서 간격이 넓어지는 현상이 발생하기 때문에,
        //기존의 decoration을 remove() 시키고 새로 추가한다
        while (rc_TodayMeetingList.getItemDecorationCount() > 0) {
            rc_TodayMeetingList.removeItemDecorationAt(0);
        }
        rc_TodayMeetingList.addItemDecoration(new MeetingItemDecoration(getContext(), "home"));
        rc_TodayMeetingList.setLayoutManager(layoutManager);
        rc_TodayMeetingList.setAdapter(todayMeetingAdapter);

        //페이징과 함께 아이템을 업데이하도록 요청하는 메소드
        callItemWithPaging();

        //스크롤뷰 이벤트를 받아 다음페이징을 요청한다
        scroll_view.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener()
        {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY)
            {

                //getChildAt - 그룹의 지정된 view의 높이를 나타낸다
                //getMeasuredHeight - view의 높이값 반환
                //스크롤뷰 높이값이 아이템리스트가 있는 뷰의 전체 높이값과 동일해지면 그때 다음 페이지를 호출한다
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())
                {
                    //더이상 호출할 데이터가 없으면 스크롤 이벤트는 발생하더라도, 서버에 데이터요청은 하지 않도록 처리
                    if (todayMeetingAdapter.getItemCount() < totalCont) {
                        page++;
                        progress_bar.setVisibility(View.VISIBLE);
                        callItemWithPaging();
                    }
                }
            }
        });


        //새 회의 생성 버튼 클릭 이벤트
        img_newMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), NewMeetingActivity.class));
            }
        });


        //회의참여 버튼 클릭 이벤트
        img_enterMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), EnterMeetingActivity.class));
            }
        });
    }


    /**
     * 페이징이 되면서(페이지스크롤) 리스트의 데이터를 추가적으로 로딩하는 메소드
     *
     * 리사이클러뷰에서 페이징 할 때 필요한 것
     * 1) limit와 page 변수
     * 2) 쿼리문에 LIMIT limit(위의 변수) OFFSET (page-1)*limit를 붙인다
     * 3) 페이징할때 사용할 수 있는 함수2가지
     *  3-1) recyclerView.addOnScrollListener() 함수와 findLastCompletelyVisibleItemPosition 함수를 이용하는 방법
     *  3-2) 스크롤뷰의 이벤트를 활용해 스크롤을 내리는 도중에 레트로핏을 호출
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void callItemWithPaging() {

        LocalDate today = LocalDate.now();

        //처음에는 1페이지 10개의 아이템을 호출한다.
        service.getTodayMeetingList(today.toString(), email, limit, page).enqueue(new Callback<ResponseMeeting>() {
            @Override
            public void onResponse(Call<ResponseMeeting> call, Response<ResponseMeeting> response) {
                ResponseMeeting responseMeeting = response.body();

                //서버로부터 전달 받은 데이터의 응답코드가 성공
                if (200 == responseMeeting.getStatusCode()) {

                    //스크롤 로딩바
                    progress_bar.setVisibility(View.GONE);

                    //전체회의아이템 갯수 응답
                    totalCont = responseMeeting.getTotalCnt();

                    if (totalCont <= 0) { //오늘회의가 없을 경우, 없다고 노출
                        home_emptyMeeting.setVisibility(View.VISIBLE);
                        scroll_view.setVisibility(View.INVISIBLE);
                    } else {
                        home_emptyMeeting.setVisibility(View.INVISIBLE);
                        scroll_view.setVisibility(View.VISIBLE);
                    }

                    //회의리스트를 담아 반복문을 돌면서 리스트에 아이템을 업데이트 시키고,
                    //adapter에 값이 변경됨을 noti한다
                    generateDataList(response.body().getData());

                } else { //서버로부터 전달 받은 데이터의 응답코드가 실패
                    Toast.makeText(getContext(), "회의리스트를 가져오는데 실패했습니다!", Toast.LENGTH_SHORT).show();
                }

                Log.e(TAG, "요청 page: " + page + ", 요청 아이템의 갯수: " + limit +
                        ", Adapter Item 갯수(getItemCount): " + todayMeetingAdapter.getItemCount() +
                        ", 전체 회의리스트 갯수: " + totalCont);
            }

            @Override
            public void onFailure(Call<ResponseMeeting> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ", t);
            }
        });
    }


    //회의리스트를 담아 반복문을 돌면서 리스트에 아이템을 업데이트 시키고,
    //adapter에 값이 변경됨을 noti한다
    private void generateDataList(ArrayList<MeetingDataResult> meetingItem) {
        for (int i = 0; i < meetingItem.size(); i++) {
            meetingDataResultList.add(meetingItem.get(i));
            todayMeetingAdapter = new TodayMeetingAdapter(meetingDataResultList, getContext(), this);
            rc_TodayMeetingList.setAdapter(todayMeetingAdapter);
        }
    }

    @Override
    public void onClickMeetingDetail(int meetingId, String startDate, String startTime) {

    }

    //오늘 회의리스트 중 회의 하나를 클릭하게 되면,
    //회의상태값 업데이트 후, 회의화면으로 곧바로 이동한다.
    @Override
    public void onClickMeetingStart(int meetingId, String meetingNum, int participantsNum,
                        String meetingTitle, String meetingPwd, String duration, String status) {

        //회의상태값을 보고, close상태이면 open상태로 변경
        if ("close".equals(status)) {
            //상태값 업데이트 성공 후에 회의참여 화면으로 이동
            RequestStatusUpdate requestStatusUpdate = new RequestStatusUpdate(email, meetingId);
            service.updateMeetingStatus(requestStatusUpdate).enqueue(new Callback<ResponseCommonData>() {
                @Override
                public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                    ResponseCommonData result = response.body();
                    if (200 == result.getStatusCode()) { //응답성공
                        //회의가 open 상태 이기 때문에, 회의참여 화면 곧바로 이동
                        moveToMeetingScreen(meetingId, meetingNum, participantsNum, meetingTitle, meetingPwd, duration);
                    } else { //응답실패
                        Log.e(TAG, "updateMeetingStatus failed....! " + result.getStatusCode() + " : " + result.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<ResponseCommonData> call, Throwable t) {
                    Log.e(TAG, "레트로핏 응답 실패 오류 발생 ", t);
                    t.printStackTrace();
                }
            });


        } else {
            //회의가 open상태이기 때문에, 회의참여 화면 곧바로 이
            moveToMeetingScreen(meetingId, meetingNum, participantsNum,
                    meetingTitle, meetingPwd, duration);
        }

    }

    @Override
    public void onClickChatView() {

    }


    //회의참여화면 이동
    private void moveToMeetingScreen(int meetingId, String meetingNum, int participantsNum,
                                     String meetingTitle, String meetingPwd, String duration) {

        Intent intent = new Intent(getContext(), MeetingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra("nickname", PreferenceManager.getString(getContext(), "name"));
        intent.putExtra("meetingNum", meetingNum);
        intent.putExtra("maximum", participantsNum);
        intent.putExtra("meetingTitle", meetingTitle);
        intent.putExtra("meetingPwd", meetingPwd);
        intent.putExtra("duration", duration);
        intent.putExtra("userEmail", email);
        intent.putExtra("meetingId", meetingId);
        intent.putExtra("hostId", email);
        startActivity(intent);
    }

}
