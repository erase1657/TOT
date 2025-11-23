package com.example.tot.Schedule.ScheduleSetting.Invite;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.example.tot.Schedule.ScheduleSetting.ScheduleSettingActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InviteDialog extends Dialog {

    private RecyclerView rv_mutual_list;
    private Button btn_confirm, btn_send_sns;

    private InviteAdapter adapter;
    private List<InviteDTO> memberList = new ArrayList<>();

    private ScheduleSettingActivity parentActivity;

    // 🔸 맞팔 확인용 콜백
    private interface MutualCallback {
        void onResult(boolean isMutual);
    }

    public InviteDialog(@NonNull ScheduleSettingActivity activity) {
        super(activity);
        this.parentActivity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_invite_schedule);

        rv_mutual_list = findViewById(R.id.rv_mutual_list);
        btn_confirm = findViewById(R.id.btn_confirm);
        btn_send_sns = findViewById(R.id.btn_send_sns);
        memberList = new ArrayList<>();
        // 리사이클러뷰 설정
        rv_mutual_list.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InviteAdapter(getContext(), memberList);
        rv_mutual_list.setAdapter(adapter);

        // 🔥 다이얼로그 열리자마자 맞팔 유저 로드
        loadMutualFollowers();

        btn_send_sns.setOnClickListener(v -> {
            FirebaseFirestore db = parentActivity.getFirestore();
            String scheduleId = parentActivity.getScheduleId();
            sendKakaoInvite(scheduleId, db);
        });

        btn_confirm.setOnClickListener(v -> dismiss());
    }

    /**
     * -------------------------------------------------------------
     * 🔥 Firestore에서 "맞팔" 유저만 로드하는 함수
     * -------------------------------------------------------------
     */
    private void loadMutualFollowers() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String myUid = FirebaseAuth.getInstance().getUid();

        if (myUid == null) return;

        // 1) 내가 팔로우한 목록 → following
        db.collection("user")
                .document(myUid)
                .collection("following")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        String otherUid = doc.getId(); // 내가 follow 하는 사람

                        // 2) 이 사람이 나도 follow하는지 → follower
                        checkMutual(otherUid, isMutual -> {

                            if (isMutual) {

                                InviteDTO dto = new InviteDTO();
                                dto.setReceiverUID(otherUid);
                                dto.setStatus("none");

                                memberList.add(dto);
                                adapter.notifyDataSetChanged();

                            }
                        });
                    }
                });
    }

    /**
     * -------------------------------------------------------------
     * 🔥 맞팔 여부 확인
     * - otherUid → follow → myUid 문서가 존재하면 맞팔
     * -------------------------------------------------------------
     */
    private void checkMutual(String otherUid, MutualCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String myUid = FirebaseAuth.getInstance().getUid();

        if (myUid == null) {
            callback.onResult(false);
            return;
        }

        db.collection("user")
                .document(otherUid)
                .collection("following")   // 상대방 follower 확인
                .document(myUid)
                .get()
                .addOnSuccessListener(doc -> {

                    boolean heFollowsMe = doc.exists();
                    boolean iFollowHim = true; // load 단계에서 이미 following만 가져옴

                    callback.onResult(heFollowsMe && iFollowHim);
                });
    }


    /**
     * -------------------------------------------------------------
     * 🔥 카카오 초대 메시지 생성 및 Firestore에 초대 데이터 저장
     * -------------------------------------------------------------
     */
    private void sendKakaoInvite(String scheduleId, FirebaseFirestore db) {

        String senderUid = FirebaseAuth.getInstance().getUid();
        String receiverUid = null;   // 최초에는 null (초대 받은 사람이 앱 실행 시 설정)
        String inviteId = UUID.randomUUID().toString();

        InviteDTO inviteDTO = new InviteDTO(
                scheduleId,
                senderUid,
                receiverUid,
                "pending",
                Timestamp.now()
        );

        // 1) Firestore 초대 데이터 저장
        db.collection("user")
                .document(senderUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("invited")
                .document(inviteId)
                .set(inviteDTO)
                .addOnSuccessListener(aVoid -> {

                    Log.d("Invite", "🔥 초대 데이터 저장 성공: " + inviteId);
                    Toast.makeText(getContext(), "초대가 생성되었습니다!", Toast.LENGTH_SHORT).show();

                    // 2) 카카오 초대 URL 생성
                    String inviteUrl = "https://erase1657.github.io/invite?scheduleId="
                            + scheduleId + "&inviteId=" + inviteId;

                    Long templateId = 125804L;

                    // 3) Kakao 공유 실행
                    KakaoShareHelper.shareCustomTemplate(getContext(), inviteUrl, templateId);

                })
                .addOnFailureListener(e -> {
                    Log.e("Invite", "❌ 초대 데이터 저장 실패", e);
                    Toast.makeText(getContext(), "초대 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
