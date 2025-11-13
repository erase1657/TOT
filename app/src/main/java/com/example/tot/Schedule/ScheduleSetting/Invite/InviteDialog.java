package com.example.tot.Schedule.ScheduleSetting.Invite;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tot.R;
import com.example.tot.Schedule.ScheduleSetting.ScheduleSettingActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.kakao.sdk.share.ShareClient;
import com.kakao.sdk.share.WebSharerClient;
import com.kakao.sdk.template.model.Link;
import com.kakao.sdk.template.model.TextTemplate;

import android.util.Log;
import android.widget.*;
import java.util.List;
import java.util.UUID;

public class InviteDialog extends Dialog {
    private RecyclerView rv_followList;
    private Button btn_confirm, btn_send_sns;
    private InviteAdapter adapter;
    private List<InviteDTO> memberList;
    private OnInviteConfirmListener listener;
    private ScheduleSettingActivity parentActivity;
    public interface OnInviteConfirmListener {
        void onInviteConfirmed(List<InviteDTO> invitedMembers);
    }
    public InviteDialog(@NonNull ScheduleSettingActivity activity) {
        super(activity);
        this.parentActivity = activity;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_invite_schedule);

        rv_followList = findViewById(R.id.rv_followlist);
        btn_confirm = findViewById(R.id.btn_confirm);
        btn_send_sns = findViewById(R.id.btn_send_sns);
        /*
        memberList = new ArrayList<>();
        memberList.add(new InviteMember("박민주", "https://...", true));
        memberList.add(new InviteMember("박지우", "https://...", true));
        memberList.add(new InviteMember("카리나", "https://...", false));
        memberList.add(new InviteMember("윈터", "https://...", false));

        adapter = new InviteAdapter(getContext(), memberList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
*/
        btn_send_sns.setOnClickListener(v -> {
            FirebaseFirestore db = parentActivity.getFirestore();

            String scheduleId = parentActivity.getScheduleId();
            sendKakaoInvite(scheduleId,db);
        });
        btn_confirm.setOnClickListener(v -> {

            dismiss();
        });
    }

    private void sendKakaoInvite(String scheduleId, FirebaseFirestore db){
        String senderUid = FirebaseAuth.getInstance().getUid();
        String receiverUid = null; //초대 받는 사람은 카카오 메시지를 클릭하고 앱을 실행할때 uid값을 설정하므로 일단 null값으로 설정함
        String inviteId = UUID.randomUUID().toString();//초대 메시지 ID


        InviteDTO inviteDTO = new InviteDTO(
                scheduleId,
                senderUid,
                receiverUid,
                "pending",
                Timestamp.now()
        );

        db.collection("user")
                .document(senderUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("invited")
                .document(inviteId)
                .set(inviteDTO)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Invite", "✅ 초대 데이터 저장 성공 (스케줄 하위): " + inviteId);
                    Toast.makeText(getContext(), "초대가 생성되었습니다!", Toast.LENGTH_SHORT).show();

                    // 🔹 카카오톡 메시지 전송
                    String inviteUrl = "https://erase1657.github.io/invite?scheduleId="
                            + scheduleId + "&inviteId=" + inviteId;
                    Long templateId = 125804L;
                    KakaoShareHelper.shareCustomTemplate(getContext(), inviteUrl, templateId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Invite", "❌ 초대 데이터 저장 실패", e);
                    Toast.makeText(getContext(), "초대 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
}
