package com.wkimdev.letsmeeting.reservation;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wkimdev.letsmeeting.OnItemClick;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.model.meeting.MeetingDataResult;
import com.wkimdev.letsmeeting.util.CommonUtils;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

// 회의에약 화면 리스트를 그리기 위한 리사이클러뷰 어댑터
public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder> {

    private List<MeetingDataResult> meetingDataResultList;
    private Context context;
    private OnItemClick mCallback; //화면이동 처리를 위한 클릭이벤트 리스너 등록

    //어댑터 생성시 등록한 아이템 객체를 파라미터로 받는다
    //해당 아이템 갯수만큼 리스트가 생성된다
    public MeetingAdapter (List<MeetingDataResult> meetingDataResultList, Context context, OnItemClick listener) {
        this.context = context;
        this.meetingDataResultList = meetingDataResultList;
        this.mCallback = listener;
    }

    // LayoutInflater를 통해 뷰 객체를 만들고,
    // 뷰홀더 객체를 생성하여 해당 뷰를 리턴
    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.meeting_item_new, parent, false);

        return new MeetingViewHolder(view);
    }

    // position에 해당하는 데이터를 뷰홀더내 뷰들에 바인딩
    @Override
    public void onBindViewHolder(@NonNull MeetingViewHolder holder, int position) {
        //yy.mm.dd로 노출하도록 처리
        holder.txt_startDate.setText(CommonUtils.formatDate(meetingDataResultList.get(position).getStartDate()).substring(2));
        holder.txt_startTime.setText(CommonUtils.formatTime(meetingDataResultList.get(position).getStartDate()));
        holder.txt_title.setText(meetingDataResultList.get(position).getTitle());
        holder.txt_meetingId.setText("회의ID " + meetingDataResultList.get(position).getMeetingNum());
        holder.txt_meetingPwd.setText(meetingDataResultList.get(position).getMeetingPwd());
        holder.txt_duration.setText(meetingDataResultList.get(position).getDuration());

        holder.itemView.setTag(position);
    }

    // 생성자를 통해 전달받은 전체 데이터 갯수 리턴
    @Override
    public int getItemCount() {
        return meetingDataResultList != null ? meetingDataResultList.size() : 0;
    }

    // 뷰홀더를 내부클래스로 선언
    public class MeetingViewHolder extends RecyclerView.ViewHolder {

        private TextView txt_startDate;
        private TextView txt_startTime;
        private TextView txt_meetingId;
        private TextView txt_title;
        private TextView txt_meetingPwd;
        private TextView txt_duration;
        private ImageView img_more;


        public MeetingViewHolder(@NonNull View itemView) {
            super(itemView);

            //뷰바인딩
            this.txt_startDate = itemView.findViewById(R.id.txt_startDate);
            this.txt_startTime = itemView.findViewById(R.id.txt_startTime);
            this.txt_title = itemView.findViewById(R.id.txt_title);
            this.txt_meetingId = itemView.findViewById(R.id.txt_meetingId);
            this.txt_meetingPwd = itemView.findViewById(R.id.txt_meetingPwd);
            this.txt_duration = itemView.findViewById(R.id.txt_duration);
            this.img_more = itemView.findViewById(R.id.img_more);

            //아이템뷰를 뷰홀더 객체에 갖고 있기 때문에
            //클릭이벤트를 뷰홀더에서 작성하도록 한다
            //뷰홀더가 만들어지는 시점에, 클릭이벤트 처리
            this.img_more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition(); //어뎁터내 아이템의 위치를 알기 위해 사용
                    if (pos != RecyclerView.NO_POSITION) { //NO_POSITION인지 검사

                        //리스너 등록
                        //회의상세화면으로 이동
                        mCallback.onClickMeetingDetail(
                                meetingDataResultList.get(pos).getMeetingId(),
                                CommonUtils.formatDate(meetingDataResultList.get(pos).getStartDate()),
                                CommonUtils.formatTime(meetingDataResultList.get(pos).getStartDate()));

                    }
                }
            });
        }

    }

}
