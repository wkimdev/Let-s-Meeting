package com.wkimdev.letsmeeting.reservation;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.wkimdev.letsmeeting.R;

/**
 * 회의예약 화면 리스트의 각 아이템을 꾸미기 위한 요소
 * ItemDecoration
 * onDraw : 항목을 배치하기 전에 호출
 * onDrawOver: 모든 항목이 배치된 이후 호출
 * getItemOffsets: 각 항목을 배치할 때 호출
 * */
public class MeetingItemDecoration extends RecyclerView.ItemDecoration {

    private Context context;
    private String type;

    public MeetingItemDecoration(Context context, String type) {
        this.context = context;
        this.type = type;
    }

    //각 항목을 배치할 때 호출된다
    //Rect - 항목을 구성하기 위한 사각형 정보가 호출
    //view - 항목을 구성하기 위한 view 호출
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        //항목의 index값 획득
        int index = parent.getChildAdapterPosition(view)+1;

        //인덱스가 3으로 나누어 떨어지는 경우, 즉 항목3개 마다 세로방향 여백을 60을 준다
        outRect.set(20, 20, 20, 20 );

        //배경색을 지정함
        view.setBackgroundColor(context.getResources().getColor(R.color.gray_light));

        //전달되는 데이터 타입별로 떠있는 효과를 다르게 줌
        if ("home".equals(type)) {
            ViewCompat.setElevation(view, 8.0f); //떠있는 효과를 준다
        } else {
            ViewCompat.setElevation(view, 20.0f); //떠있는 효과를 준다
        }
    }


}
