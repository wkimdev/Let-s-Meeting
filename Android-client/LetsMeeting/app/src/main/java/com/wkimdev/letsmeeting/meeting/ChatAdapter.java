package com.wkimdev.letsmeeting.meeting;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wkimdev.letsmeeting.OnItemClick;
import com.wkimdev.letsmeeting.R;
import com.wkimdev.letsmeeting.configure.ApplicationConstants;
import com.wkimdev.letsmeeting.model.ChatData;
import com.wkimdev.letsmeeting.util.CommonUtils;

import java.util.List;

// 채팅 화면 메시지 리스트를 그리기 위한 리사이클러뷰 어댑터
// 화상 회의중 채팅을 진행하는 어뎁터
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private String TAG = "ChatAdapter";
    private List<ChatData> mChatData;
    private Context context;
    private String myNickName;
    private OnItemClick mCallback; //채팅화면 클릭시 이벤트처리를 위한 콜백등록
    private String hostId; //호스트ID


    //나와 상대방 채팅 레이아웃을 구분하기 위한 뷰타입상수
    private static final int VIEW_TYPE_LOCAL = 0;
    private static final int VIEW_TYPE_REMOTE = 1;

    //어댑터 생성시 등록한 아이템 객체를 파라미터로 받는다
    //해당 아이템 갯수만큼 리스트가 생성된다
    public ChatAdapter(List<ChatData> mChatData, Context context, String myNickName, OnItemClick listener, String hostId){
        this.mChatData = mChatData;
        this.context = context;
        this.myNickName = myNickName;
        this.mCallback = listener;
        this.hostId = hostId;
    }

    // LayoutInflater를 통해 뷰 객체를 만들고,
    // 뷰홀더 객체를 생성하여 해당 뷰를 리턴
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //리턴되는 뷰 선언
        View view;
        switch (viewType) {
            // 내가 작성한 채팅은 왼쪽에 정렬되는 레이아웃 객체 생성,
            case VIEW_TYPE_LOCAL:
                view = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.chat_item_mine, parent, false);
                return new ChatViewHolder(view);
            // 상대방이 보낸 채팅은 오른쪽으로 정렬되도록 처리한 레이아웃 객체 생성
            case VIEW_TYPE_REMOTE:
                view = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.chat_item, parent, false);
                return new ChatViewHolder(view);
        }
        view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.chat_item_mine, parent, false);
        return new ChatViewHolder(view);
    }


    //내 채팅과 상대방 채팅뷰를 닉네임으로 구분해 리턴해준다.
    @Override
    public int getItemViewType(int position) {
        ChatData chat = mChatData.get(position);
        if (chat.getNickname().equals(this.myNickName)) { //내가 작성한 채팅 메세지
            return VIEW_TYPE_LOCAL;
        } else {
            return VIEW_TYPE_REMOTE;
        }
    }


    // position에 해당하는 데이터를 뷰홀더내 뷰들에 바인딩
    // 내가 작성한 채팅은 왼쪽에 정렬, 상대방이 보낸 채팅은 오른쪽으로 정렬되도록 처리
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {

        ChatData chat = mChatData.get(position);
        holder.txt_nickname.setText(chat.getNickname());

        //프로필사진 셋팅
        String profileImg = chat.getProfile().replace("\"", "");
        if (CommonUtils.isStringEmpty(profileImg)) {
            holder.iv_profile.setImageResource(R.drawable.default_user);
        } else {
            String imgUrl = ApplicationConstants.API_SERVER_URL + "/api/member/profile/" + chat.getProfile();
            Glide.with(holder.itemView)
                    .load(imgUrl.replace("\"", ""))
                    .into(holder.iv_profile);
            holder.iv_profile.setBackgroundResource(R.drawable.profile_img);
        }
        holder.txt_message.setText(chat.getMessage());
        holder.txt_sendTime.setText(chat.getSendTime());
        //호스트여부 체크해서 호스트텍스트뷰 노출
        if (hostId.equals(chat.getUserId())) {
            holder.txt_nickname_host.setVisibility(View.VISIBLE);
        }
        holder.itemView.setTag(position);
    }


    // 생성자를 통해 전달받은 전체 데이터 갯수 리턴
    @Override
    public int getItemCount() {
        return mChatData != null ? mChatData.size() : 0;
    }

    // 뷰홀더를 내부클래스로 선언
    public class ChatViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout layout_chat_item;
        private RelativeLayout layout_chat_mine_item;
        private TextView txt_nickname;
        private ImageView iv_profile;
        private TextView txt_message;
        private TextView txt_sendTime;
        private TextView txt_nickname_host;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            //뷰바인딩
            this.layout_chat_item = itemView.findViewById(R.id.layout_chat_item);
            this.layout_chat_mine_item = itemView.findViewById(R.id.layout_chat_item_mine);
            this.txt_nickname = itemView.findViewById(R.id.txt_nickname);
            this.iv_profile = itemView.findViewById(R.id.iv_profile);
            this.txt_message = itemView.findViewById(R.id.txt_message);
            this.txt_sendTime = itemView.findViewById(R.id.txt_sendTime);
            this.txt_nickname_host = itemView.findViewById(R.id.txt_nickname_host);

            //아이템뷰를 뷰홀더 객체에 갖고 있기 때문에
            //클릭이벤트를 뷰홀더에서 작성하도록 한다
            //뷰홀더가 만들어지는 시점에, 클릭이벤트 처리
            if (this.layout_chat_item != null) {
                this.layout_chat_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //채팅화면 클릭시, 키보드가 올라가 있으면 키보드가 내려가도록 이벤트처리
                        mCallback.onClickChatView();
                    }
                });
            }
            if (this.layout_chat_mine_item != null) {
                this.layout_chat_mine_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallback.onClickChatView();
                    }
                });
            }
        }

    }

}
