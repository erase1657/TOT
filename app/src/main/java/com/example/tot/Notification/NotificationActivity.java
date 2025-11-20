package com.example.tot.Notification;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tot.Follow.FollowActionHelper;
import com.example.tot.MyPage.UserProfileActivity;
import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private ImageView btnBack;
    private LinearLayout todaySection;
    private LinearLayout recentSection;
    private LinearLayout emptyView;
    private RecyclerView recyclerToday;
    private RecyclerView recyclerRecent;
    private SwipeRefreshLayout swipeRefreshLayout;

    private NotificationAdapter todayAdapter;
    private NotificationAdapter recentAdapter;

    private List<NotificationDTO> todayNotifications = new ArrayList<>();
    private List<NotificationDTO> recentNotifications = new ArrayList<>();

    // Firestore
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // ✅ NotificationManager 리스너
    private NotificationManager.UnreadCountListener unreadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerViews();
        setupSwipeRefresh();

        // ✅ NotificationManager 리스너 등록 (UI 업데이트용)
        setupNotificationListener();

        // ✅ 초기 데이터 로드
        loadNotifications();
        updateUI();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        todaySection = findViewById(R.id.today_section);
        recentSection = findViewById(R.id.recent_section);
        emptyView = findViewById(R.id.empty_view);
        recyclerToday = findViewById(R.id.recycler_today);
        recyclerRecent = findViewById(R.id.recycler_recent);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        recyclerToday.setLayoutManager(new LinearLayoutManager(this));
        todayAdapter = new NotificationAdapter(todayNotifications, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationDTO notification) {
                handleNotificationClick(notification);
            }

            @Override
            public void onFollowBackClick(NotificationDTO notification) {
                handleFollowBack(notification);
            }
        });
        recyclerToday.setAdapter(todayAdapter);

        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new NotificationAdapter(recentNotifications, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationDTO notification) {
                handleNotificationClick(notification);
            }

            @Override
            public void onFollowBackClick(NotificationDTO notification) {
                handleFollowBack(notification);
            }
        });
        recyclerRecent.setAdapter(recentAdapter);
    }

    /**
     * ✅ 새로고침 설정
     */
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light)
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshNotifications();
        });
    }

    /**
     * ✅ 알림 새로고침
     */
    private void refreshNotifications() {
        Log.d(TAG, "🔄 새로고침 시작");

        NotificationManager manager = NotificationManager.getInstance();
        manager.refresh();

        // 1초 후 새로고침 완료
        swipeRefreshLayout.postDelayed(() -> {
            loadNotifications();
            updateUI();
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "새로고침 완료", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    /**
     * ✅ NotificationManager 리스너 등록 (실시간 업데이트)
     */
    private void setupNotificationListener() {
        unreadListener = count -> {
            runOnUiThread(() -> {
                Log.d(TAG, "📬 알림 카운트 변경: " + count + "개");
                loadNotifications();
                updateUI();
            });
        };
        NotificationManager.getInstance().addListener(unreadListener);
    }

    private void loadNotifications() {
        NotificationManager manager = NotificationManager.getInstance();
        todayNotifications.clear();
        recentNotifications.clear();
        todayNotifications.addAll(manager.getTodayNotifications());
        recentNotifications.addAll(manager.getRecentNotifications());

        if (todayAdapter != null) {
            todayAdapter.notifyDataSetChanged();
        }
        if (recentAdapter != null) {
            recentAdapter.notifyDataSetChanged();
        }

        Log.d(TAG, "✅ 알림 로드: 오늘 " + todayNotifications.size() +
                "개, 최근 " + recentNotifications.size() + "개");
    }

    private void updateUI() {
        boolean hasToday = !todayNotifications.isEmpty();
        boolean hasRecent = !recentNotifications.isEmpty();

        if (hasToday || hasRecent) {
            emptyView.setVisibility(View.GONE);
            todaySection.setVisibility(hasToday ? View.VISIBLE : View.GONE);
            recentSection.setVisibility(hasRecent ? View.VISIBLE : View.GONE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
            todaySection.setVisibility(View.GONE);
            recentSection.setVisibility(View.GONE);
        }
    }

    /**
     * 🔥 수정: 알림 클릭 시 프로필로 실제 이동
     */
    private void handleNotificationClick(NotificationDTO notification) {
        // ✅ Firestore에 읽음 상태 업데이트
        NotificationManager.getInstance().markAsRead(notification.getId());

        notification.setRead(true);
        todayAdapter.notifyDataSetChanged();
        recentAdapter.notifyDataSetChanged();

        switch (notification.getType()) {
            case SCHEDULE_INVITE:
                Toast.makeText(this, "일정 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                // TODO: 일정 상세 화면으로 이동하는 코드 추가
                break;

            case FOLLOW:
                // 🔥 수정: 실제 프로필 화면으로 이동
                String userId = notification.getUserId();
                if (userId != null && !userId.isEmpty()) {
                    Intent intent = new Intent(this, UserProfileActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    Log.d(TAG, "✅ 프로필 화면으로 이동: " + userId);
                } else {
                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "⚠️ userId가 null입니다");
                }
                break;

            case COMMENT:
                Toast.makeText(this, "게시물 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                // TODO: 게시물 상세 화면으로 이동하는 코드 추가
                break;
        }
    }

    /**
     * ✅ 맞팔로우 버튼 클릭 처리 (Firestore 연동 - 양방향 처리)
     */
    private void handleFollowBack(NotificationDTO notification) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = mAuth.getCurrentUser().getUid();
        String targetUserId = notification.getUserId();

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ 이미 팔로우 중인지 확인
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // 이미 팔로우 중인 경우 → 언팔로우
                        performUnfollow(myUid, targetUserId, notification);
                    } else {
                        // 팔로우하지 않은 경우 → 팔로우
                        performFollowBack(myUid, targetUserId, notification);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로우 상태 확인 실패", e);
                    Toast.makeText(this, "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ 맞팔로우 실행 (양방향 처리)
     */
    private void performFollowBack(String myUid, String targetUserId, NotificationDTO notification) {
        Map<String, Object> followData = new HashMap<>();
        followData.put("followedAt", System.currentTimeMillis());

        // ✅ 1. 내 following에 추가
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .set(followData)
                .addOnSuccessListener(aVoid -> {
                    // ✅ 2. 상대방 follower에 추가
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(myUid)
                            .set(followData)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, notification.getUserName() + " 님을 팔로우했습니다", Toast.LENGTH_SHORT).show();

                                // ✅ 알림 읽음 처리
                                NotificationManager.getInstance().markAsRead(notification.getId());
                                notification.setRead(true);

                                // ✅ 어댑터 업데이트 (버튼 상태 갱신)
                                todayAdapter.notifyDataSetChanged();
                                recentAdapter.notifyDataSetChanged();

                                Log.d(TAG, "✅ 팔로우 성공: " + targetUserId);
                                FollowActionHelper.sendFollowNotification(targetUserId, myUid);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 상대방 팔로워 추가 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로우 실패", e);
                    Toast.makeText(this, "팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ 언팔로우 실행 (양방향 처리)
     */
    private void performUnfollow(String myUid, String targetUserId, NotificationDTO notification) {
        // ✅ 1. 내 following에서 삭제
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // ✅ 2. 상대방 follower에서 삭제
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(myUid)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, notification.getUserName() + " 님을 언팔로우했습니다", Toast.LENGTH_SHORT).show();

                                // ✅ 알림 읽음 처리
                                NotificationManager.getInstance().markAsRead(notification.getId());
                                notification.setRead(true);

                                // ✅ 어댑터 업데이트 (버튼 상태 갱신)
                                todayAdapter.notifyDataSetChanged();
                                recentAdapter.notifyDataSetChanged();

                                Log.d(TAG, "✅ 언팔로우 성공: " + targetUserId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 상대방 follower 삭제 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 언팔로우 실패", e);
                    Toast.makeText(this, "언팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    public int getTotalUnreadCount() {
        int count = 0;
        for (NotificationDTO notif : todayNotifications) {
            if (!notif.isRead()) count++;
        }
        for (NotificationDTO notif : recentNotifications) {
            if (!notif.isRead()) count++;
        }
        return count;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ 리스너 해제
        if (unreadListener != null) {
            NotificationManager.getInstance().removeListener(unreadListener);
        }
    }
}