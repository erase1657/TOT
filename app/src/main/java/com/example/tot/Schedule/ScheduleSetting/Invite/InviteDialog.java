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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InviteDialog extends Dialog {

    private static final String TAG = "InviteDialog";

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

        // ✅ 초대 클릭 콜백 추가
        adapter = new InviteAdapter(getContext(), memberList, dto -> sendAppInvite(dto));
        rv_mutual_list.setAdapter(adapter);

        // 🔥 다이얼로그 열리자마자 맞팔 유저 로드
        loadMutualFollowers();

        btn_send_sns.setOnClickListener(v -> {
            FirebaseFirestore db = parentActivity.getFirestore();
            String scheduleId = parentActivity.getScheduleId();
            sendKakaoInvite(scheduleId);
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
     * ✅ 앱 내부 초대: 수신함에 알림 전송
     * -------------------------------------------------------------
     */
    private void sendAppInvite(InviteDTO dto) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String myUid = FirebaseAuth.getInstance().getUid();
        final String scheduleId = parentActivity.getScheduleId();
        final String receiverUid = dto.getReceiverUID();

        if (myUid == null || scheduleId == null || receiverUid == null) {
            Toast.makeText(getContext(), "초대 정보가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) 스케줄 정보 가져오기
        db.collection("user")
                .document(myUid)
                .collection("schedule")
                .document(scheduleId)
                .get()
                .addOnSuccessListener(scheduleDoc -> {
                    if (!scheduleDoc.exists()) {
                        Toast.makeText(getContext(), "스케줄을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String scheduleName = scheduleDoc.getString("scheduleName") != null ?
                            scheduleDoc.getString("scheduleName") : "여행 일정";

                    // 2) 내 닉네임 가져오기
                    db.collection("user")
                            .document(myUid)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                final String myNickname = userDoc.exists() && userDoc.getString("nickname") != null ?
                                        userDoc.getString("nickname") : "알 수 없음";

                                // 3) 수신함에 알림 저장
                                String notificationId = UUID.randomUUID().toString();
                                Map<String, Object> notification = new HashMap<>();
                                notification.put("type", "SCHEDULE_INVITE");
                                notification.put("senderUid", myUid);
                                notification.put("senderName", myNickname);
                                notification.put("scheduleId", scheduleId);
                                notification.put("scheduleName", scheduleName);
                                notification.put("content", myNickname + " 님이 " + scheduleName + "에 초대했습니다");
                                notification.put("isRead", false);
                                notification.put("createdAt", System.currentTimeMillis());

                                db.collection("user")
                                        .document(receiverUid)
                                        .collection("scheduleInvitations")
                                        .document(notificationId)
                                        .set(notification)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ 앱 초대 전송 성공: " + receiverUid);
                                            Toast.makeText(getContext(),
                                                    "초대를 전송했습니다",
                                                    Toast.LENGTH_SHORT).show();

                                            // DTO 상태 업데이트
                                            dto.setStatus("pending");
                                            adapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 앱 초대 전송 실패", e);
                                            Toast.makeText(getContext(),
                                                    "초대 전송에 실패했습니다",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 사용자 정보 조회 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 스케줄 정보 조회 실패", e);
                    Toast.makeText(getContext(), "스케줄 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * -------------------------------------------------------------
     * 카카오톡 초대 (기존 기능 유지)
     * -------------------------------------------------------------
     */
    private void sendKakaoInvite(String scheduleId) {

        String senderUid = FirebaseAuth.getInstance().getUid();
        String inviteId = UUID.randomUUID().toString();

        Long templateId = 125804L;

        Map<String, String> templateArgs = new HashMap<>();
        templateArgs.put("senderUid", senderUid);
        templateArgs.put("scheduleId", scheduleId);
        templateArgs.put("inviteId", inviteId);

        KakaoShareHelper.shareCustomTemplate(
                getContext(),
                templateId,
                templateArgs
        );
    }
}