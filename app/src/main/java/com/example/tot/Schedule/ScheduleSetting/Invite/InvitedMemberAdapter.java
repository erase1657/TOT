package com.example.tot.Schedule.ScheduleSetting.Invite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.Authentication.UserDTO;
import com.example.tot.R;


import java.util.ArrayList;
import java.util.List;

/**
 * 스케줄 세팅 화면 상단의 멤버 리스트 (Owner + Invited Users)
 */
public class InvitedMemberAdapter extends RecyclerView.Adapter<InvitedMemberAdapter.MemberViewHolder> {

    private final Context context;
    private final List<UserDTO> memberList = new ArrayList<>();
    private OnMemberClickListener clickListener;

    public InvitedMemberAdapter(Context context) {
        this.context = context;
    }

    /**
     * owner 를 맨 앞에 배치하고 초대 인원은 뒤에 순서대로 정렬
     */
    public void setMembers(UserDTO owner, List<UserDTO> invitedMembers) {
        memberList.clear();
        if (owner != null) {
            memberList.add(owner);  // 항상 첫 번째
        }
        if (invitedMembers != null) {
            memberList.addAll(invitedMembers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_invited_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        UserDTO user = memberList.get(position);

        // 프로필 이미지 로드
        Glide.with(context)
                .load(user.getProfileImageUrl())
                .circleCrop()
                .into(holder.profileImage);

        // 클릭 리스너
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onMemberClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_member_profile);
        }
    }

    public interface OnMemberClickListener {
        void onMemberClick(UserDTO user);
    }

    public void setOnMemberClickListener(OnMemberClickListener listener) {
        this.clickListener = listener;
    }
}
