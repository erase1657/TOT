package com.example.tot.Schedule.ScheduleSetting.Invite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder> {

    private Context context;
    private List<InviteDTO> memberList;
    private OnInviteClickListener listener; // ✅ 콜백 추가

    // ✅ 초대 클릭 콜백 인터페이스
    public interface OnInviteClickListener {
        void onInviteClick(InviteDTO dto);
    }

    // 기존 생성자 유지 (하위 호환성)
    public InviteAdapter(Context context, List<InviteDTO> memberList) {
        this.context = context;
        this.memberList = memberList;
        this.listener = null;
    }

    // ✅ 콜백 포함 생성자 추가
    public InviteAdapter(Context context, List<InviteDTO> memberList, OnInviteClickListener listener) {
        this.context = context;
        this.memberList = memberList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_invite_dialog, parent, false);
        return new InviteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        InviteDTO dto = memberList.get(position);

        // 1) Firestore에서 nickname, image 로드
        loadUserInfo(dto.getReceiverUID(), holder);

        // 2) 버튼 상태 UI 반영 (selected 기반)
        updateInviteIcon(holder, dto.getStatus());

        // 3) 버튼 클릭 시 토글 처리
        holder.btnInvite.setOnClickListener(v -> {

            if ("none".equals(dto.getStatus())) {
                // ✅ 초대 실행 - 콜백 호출
                if (listener != null) {
                    listener.onInviteClick(dto);
                } else {
                    // 콜백이 없으면 기존 방식대로 상태만 변경
                    dto.setStatus("pending");
                    updateInviteIcon(holder, "pending");
                }

            } else if ("pending".equals(dto.getStatus())) {
                // 이미 초대한 상태 - 동작 없음 또는 취소 기능 추가 가능
                // 현재는 아무 동작 없음
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    class InviteViewHolder extends RecyclerView.ViewHolder {

        CircleImageView ivProfile;
        TextView tvName;
        Button btnInvite;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvName = itemView.findViewById(R.id.tv_user_name);
            btnInvite = itemView.findViewById(R.id.btn_invite);
        }
    }

    /**
     * Firestore에서 사용자 프로필과 닉네임 불러오기
     */
    private void loadUserInfo(String uid, InviteViewHolder holder) {
        FirebaseFirestore.getInstance()
                .collection("user")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nickname = doc.getString("nickname");
                        String img = doc.getString("profileImageUrl");

                        holder.tvName.setText(nickname != null ? nickname : "이름 없음");

                        if (img != null && img.contains(".xml")) {
                            holder.ivProfile.setImageResource(R.drawable.ic_profile_default);
                        }
                        else if (img != null && !img.isEmpty()) {
                            Glide.with(context)
                                    .load(img)
                                    .placeholder(R.drawable.ic_profile_default)
                                    .error(R.drawable.ic_profile_default)
                                    .into(holder.ivProfile);
                        }
                        else {
                            holder.ivProfile.setImageResource(R.drawable.ic_profile_default);
                        }
                    }
                });
    }

    /**
     * 초대 선택 여부에 따라 버튼 아이콘 변경
     */
    private void updateInviteIcon(InviteViewHolder holder, String status) {

        if (status == null || status.equals("none")) {
            // 아직 초대 안 함
            holder.btnInvite.setBackgroundResource(R.drawable.ic_add2);

        } else if (status.equals("pending")) {
            // 초대 보낸 상태
            holder.btnInvite.setBackgroundResource(R.drawable.ic_inbox);

        } else if (status.equals("accepted")) {
            // 초대 수락됨
            holder.btnInvite.setBackgroundResource(R.drawable.ic_check);

        } else if (status.equals("rejected")) {
            // 상대가 거절
            holder.btnInvite.setBackgroundResource(R.drawable.ic_add2);
        }
    }

}