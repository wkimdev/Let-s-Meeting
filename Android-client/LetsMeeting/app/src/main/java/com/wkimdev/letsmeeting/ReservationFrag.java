package com.wkimdev.letsmeeting;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wkimdev.letsmeeting.configure.PreferenceManager;
import com.wkimdev.letsmeeting.configure.RetrofitClientInstance;
import com.wkimdev.letsmeeting.model.ResponseCommonData;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.model.meeting.ResponseMeeting;
import com.wkimdev.letsmeeting.reservation.AddReservationActivity;
import com.wkimdev.letsmeeting.reservation.MeetingAdapter;
import com.wkimdev.letsmeeting.reservation.MeetingDetailActivity;
import com.wkimdev.letsmeeting.reservation.MeetingItemDecoration;
import com.wkimdev.letsmeeting.service.MeetingService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 회의예약 화면
public class ReservationFrag extends Fragment implements OnItemClick {

    private String TAG = this.getClass().getSimpleName();

    private View view;

    private TextView txt_noMeeting; //회의내역이 없을경우 문구
    private FloatingActionButton fab_addMeeting; //회의추가 버튼
    private RecyclerView rc_meetingList; //회의리스트를 담을 리사이클러뷰 선언
    private MeetingAdapter meetingAdapter; //어뎁터 선언

    // 진행바
    private ProgressDialog progressDoalog;

    //회의생성을 위한 레트로핏 서비스 API 객체 생성
    private MeetingService service = RetrofitClientInstance.getRetrofitInstance().create(MeetingService.class);

    //리사이클러뷰에서 페이징 할 때 필요한 것
    private int limit = 10;
    private int page = 1;
    private String email;


    private ProgressBar progressBar2; //scroll이후 보여지는 프로그레스바
    private NestedScrollView nestedScrollView; //스크롤뷰
    private ArrayList<MeetingDataResult> meetingDataResultList; //회의리스트
    private int totalCont; //전체 회의리스트 갯수


    //프래그먼트 생성시 시스템에서 호출함.
    //onDestroyView() 호출이후, 다시 프래그먼트로 돌아왔을때 호출됨
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "onCreate: 호출!!!");
    }

    //프래그먼트가 있는 액티비티의 생명주기는 해당 프래그먼트의 생명주기에 영향을 미친다
    //Activiy와 같은 생명주기 콜백메소드
    //액티비티가 시작됨 상태에 들어가면 호출됨
    @Override
    public void onStart() {
        super.onStart();

        Log.e(TAG, "onStart: 호출!!!");
    }

    //프래그먼트에 맞는 UI를 그리기 위해 View를 반환하는 콜백메소드
    //프래그먼트의 레이아웃 루트이기 때문에 UI를 제공하지 않을 경우, null을 반환
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag2_new, container, false);

        return view;
    }

    //onCreateView가 호출 후 return한 뒤에 곧바로 실행되는 콜백 메소드
    //View가 완전히 생성되었을때 호출된다.
    //onCreateaView가 아닌, 아래 콜백에서 뷰바인딩을 하도록 권장
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //프래그먼트 레아아웃의 루트view를 가져와 findViewById를 실행
        txt_noMeeting = view.findViewById(R.id.txt_noMeeting);
        fab_addMeeting = view.findViewById(R.id.fab_addMeeting);
        rc_meetingList = view.findViewById(R.id.rc_meetingList);

        nestedScrollView = view.findViewById(R.id.scroll_view); //스크롤뷰
        progressBar2 = view.findViewById(R.id.progress_bar); //스크롤시 로딩바 노출

    }

    //활동이 재개됨에 따라 초기화 작업을 수행하는 콜백구문
    //앱이 사용자와 상호작용 하는 구문
    //이벤트 발생 후 포커스가 떠나기 전까지 머무르는 상태
    //UI내 데이터를 업데이트하기 위한 처리가 들어가야 한다
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onResume() {
        super.onResume();

        Log.e(TAG, "onResume: 호출!!");

        page = 1; //호출페이지 초기화
        meetingDataResultList = new ArrayList<>(); //회의리스트 초기화

        email = PreferenceManager.getString(getContext(), "email");

        //오늘날짜 이전 회의는 리스트에서 삭제하도록 요청
        deletePreviousMeeting();

        //기본로딩바 노출
        progressDoalog = new ProgressDialog(getContext());
        progressDoalog.setMessage("Loading....");
        progressDoalog.show();


        //화면최초 진입시 리사이클러뷰로 회의리스트 노출
        meetingAdapter = new MeetingAdapter(meetingDataResultList, getContext(), this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        //화면에 새로 진입할때마다 이전 deco가 쌓여서 간격이 넓어지는 현상이 발생하기 때문에,
        //기존의 decoration을 remove() 시키고 새로 추가한다
        while (rc_meetingList.getItemDecorationCount() > 0) {
            rc_meetingList.removeItemDecorationAt(0);
        }
        rc_meetingList.addItemDecoration(new MeetingItemDecoration(getContext(), "reservation"));
        rc_meetingList.setLayoutManager(layoutManager);
        rc_meetingList.setAdapter(meetingAdapter);

        //페이징과 함께 아이템을 업데이하도록 요청하는 메소드
        callItemWithPaging();

        //스크롤뷰 이벤트를 받아 다음페이징을 요청한다
        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener()
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
                    if (meetingAdapter.getItemCount() < totalCont) {
                        page++;
                        progressBar2.setVisibility(View.VISIBLE);
                        callItemWithPaging();
                    }
                }
            }
        });


        //회의예약화면 이동
        fab_addMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), AddReservationActivity.class));

            }
        });
    }

    //오늘날짜 이전 회의는 리스트에서 삭제하도록 요청
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void deletePreviousMeeting() {

        LocalDate currentDate = LocalDate.now();
        service.deletePreviousMeeting(currentDate.toString()).enqueue(new Callback<ResponseCommonData>() {
            @Override
            public void onResponse(Call<ResponseCommonData> call, Response<ResponseCommonData> response) {

                ResponseCommonData responseCommonData = response.body();

                //서버로부터 전달 받은 데이터의 응답코드가 성공
                if (200 != responseCommonData.getStatusCode()) {
                    Toast.makeText(getContext(), "이전회를 삭제하는데 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseCommonData> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ", t);
            }
        });
    }

    //레트로핏으로 회의리스트 요청
    private void getMeetingList(String email) {

        // 레트로핏 인스턴스 생성을 해줍니다.
        // enqueue로 비동기 통신을 싱행합니다.
        service.getMeetingList(email).enqueue(new Callback<ResponseMeeting>() {
            //통신완료후 이벤트 처리를 위한 콜백 리스너 등록
            @Override
            public void onResponse(Call<ResponseMeeting> call, Response<ResponseMeeting> response) {

                ResponseMeeting responseMeeting = response.body();

                if (200 == responseMeeting.getStatusCode()) {//성공
                    //회의생성 후 로딩바가 뜬 다음, 예약화면으로 이동
                    progressDoalog.dismiss();
                    generateDataList(response.body().getData());
                } else {
                    Toast.makeText(getContext(), "회의생성이 실패했습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            // 통신 실패시(예외발생, 인터넷끊김 등의 이유)
            @Override
            public void onFailure(Call<ResponseMeeting> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ");
                t.printStackTrace();
            }
        });
    }


    // 리사이클러뷰를 그린다
    private void generateDataList(List<MeetingDataResult> meetingItem) {

        //여기서, 호출갯수가 0개면 더이상 스크롤 이벤트가 발생되더라도, API를 호출하지 않는다.
        for (int i = 0; i < meetingItem.size(); i++) {
            meetingDataResultList.add(meetingItem.get(i));
            meetingAdapter = new MeetingAdapter(meetingDataResultList, getContext(), this);
            rc_meetingList.setAdapter(meetingAdapter);
        }
    }


    /**
     * 페이징과 함께 아이템을 업데이하도록 요청하는 메소드
     *
     * 리사이클러뷰에서 페이징 할 때 필요한 것
     * 1) limit와 page 변수
     * 2) 쿼리문에 LIMIT limit(위의 변수) OFFSET (page-1)*limit를 붙인다
     * 3) recyclerView.addOnScrollListener() 함수와 findLastCompletelyVisibleItemPosition 함수를 이용하는 방법
     * 또는 스크롤뷰의 이벤트를 활용해 스크롤을 내리는 도중에 레트로핏을 호출
     */
    public void callItemWithPaging() {

        //처음에는 1페이지 10개의 아이템을 호출한다.
        service.getMeetingListWithPaging(email, limit, page).enqueue(new Callback<ResponseMeeting>() {
            @Override
            public void onResponse(Call<ResponseMeeting> call, Response<ResponseMeeting> response) {

                ResponseMeeting responseMeeting = response.body();

                //서버로부터 전달 받은 데이터의 응답코드가 성공
                if (200 == responseMeeting.getStatusCode()) {

                    //회의생성 후 로딩바가 뜬 다음, 예약화면으로 이동
                    progressDoalog.dismiss();
                    //스크롤 로딩바
                    progressBar2.setVisibility(View.GONE);

                    //전체회의아이템 갯수 응답
                    totalCont = responseMeeting.getTotalCnt();

                    //회의예약 내역이 없을 경우, 리스트값이 없다는 문구 노출
                    if (totalCont <= 0) {
                        txt_noMeeting.setVisibility(View.VISIBLE);
                        //scroll_view.set
                    }  else {
                        txt_noMeeting.setVisibility(View.INVISIBLE);
                    }

                    //회의리스트를 담아 반복문을 돌면서 리스트에 아이템을 업데이트 시키고,
                    //adapter에 값이 변경됨을 noti한다
                    generateDataList(response.body().getData());

                } else { //서버로부터 전달 받은 데이터의 응답코드가 실패
                    Toast.makeText(getContext(), "회의리스트를 가져오는데 실패했습니다!", Toast.LENGTH_SHORT).show();
                }

                Log.e(TAG, "요청 page: " + page + ", 요청 아이템의 갯수: " + limit +
                    ", Adapter Item 갯수(getItemCount): " +  meetingAdapter.getItemCount() +
                    ", 전체 회의리스트 갯수: " + totalCont);

            }

            @Override
            public void onFailure(Call<ResponseMeeting> call, Throwable t) {
                Log.e(TAG, "레트로핏 응답 실패 오류 발생 ", t);
            }
        });
    }


    //사용자가 프래그먼트를 떠나면 호출됨
    //사용자가 돌아오지 않을 수 도있기 때문에, 현재 사용자 세션을 넘어 지속되어야 하는 변경사항을 저장한다
    @Override
    public void onPause() {
        super.onPause();

        Log.e(TAG, "onPause: 호출!!!");
    }

    //액티비티가 완전히 가려지면 호출됨
    //앱이 보이지 않을 동안 필요하지 않는 리소스를 해제하거나 조정하는 구문을 작성한다
    @Override
    public void onStop() {
        super.onStop();

        Log.e(TAG, "onStop: 호출!!!");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.e(TAG, "onDestroyView: 호출!!!");
    }

    @Override
    public void onClickMeetingDetail(int meetingId, String startDate, String startTime) {
        Intent intent = new Intent(getContext(), MeetingDetailActivity.class);
        intent.putExtra("meetingId", meetingId);
        intent.putExtra("startDate", startDate);
        intent.putExtra("startTime", startTime);
        startActivity(intent);
    }

    @Override
    public void onClickMeetingStart(int meetingId, String meetingNum, int participantsNum,
                        String meetingTitle, String meetingPwd, String duration, String status) {

    }

    @Override
    public void onClickChatView() {

    }

}
