package com.example.tot.Notification;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tot.Follow.FollowButtonHelper;
import com.example.tot.MyPage.UserProfileActivity;
import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
        setupNotificationListener();

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

        setupSwipeToDelete(recyclerToday, todayNotifications, todayAdapter);
        setupSwipeToDelete(recyclerRecent, recentNotifications, recentAdapter);
    }

    private void setupSwipeToDelete(RecyclerView recyclerView,
                                    List<NotificationDTO> dataList,
                                    NotificationAdapter adapter) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#FF4444"));
            private final Drawable deleteIcon = ContextCompat.getDrawable(NotificationActivity.this, R.drawable.ic_trash);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= dataList.size()) return;

                NotificationDTO deletedNotification = dataList.get(position);

                dataList.remove(position);
                adapter.notifyItemRemoved(position);

                NotificationManager.getInstance().deleteNotification(deletedNotification.getId());

                updateUI();

                Toast.makeText(NotificationActivity.this, "알림 삭제됨", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                int backgroundLeft = itemView.getRight() + (int) dX;
                background.setBounds(backgroundLeft, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                if (deleteIcon != null) {
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

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

    private void refreshNotifications() {
        Log.d(TAG, "🔄 새로고침 시작");

        NotificationManager manager = NotificationManager.getInstance();
        manager.refresh();

        swipeRefreshLayout.postDelayed(() -> {
            loadNotifications();
            updateUI();
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "새로고침 완료", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

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

    private void handleNotificationClick(NotificationDTO notification) {
        NotificationManager.getInstance().markAsRead(notification.getId());

        notification.setRead(true);
        todayAdapter.notifyDataSetChanged();
        recentAdapter.notifyDataSetChanged();

        switch (notification.getType()) {
            case SCHEDULE_INVITE:
                Toast.makeText(this, "일정 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                break;

            case FOLLOW:
                String userId = notification.getUserId();
                if (userId != null && !userId.isEmpty()) {
                    Intent intent = new Intent(this, UserProfileActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    Log.d(TAG, "✅ 프로필 화면으로 이동: " + userId);
                } else {
                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                }
                break;

            case COMMENT:
                Toast.makeText(this, "게시물 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * ✅ FollowButtonHelper를 사용한 맞팔로우 처리
     */
    private void handleFollowBack(NotificationDTO notification) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetUserId = notification.getUserId();

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FollowButtonHelper로 팔로우 상태 확인
        FollowButtonHelper.checkFollowStatus(targetUserId, (isFollowing, isFollower) -> {
            if (isFollowing) {
                // 이미 팔로우 중이면 언팔로우
                performUnfollowWithHelper(targetUserId, notification);
            } else {
                // 팔로우하지 않았으면 팔로우
                performFollowBackWithHelper(targetUserId, notification);
            }
        });
    }

    /**
     * ✅ FollowButtonHelper를 사용한 팔로우
     */
    private void performFollowBackWithHelper(String targetUserId, NotificationDTO notification) {
        FollowButtonHelper.checkFollowStatus(targetUserId, (isFollowing, isFollower) -> {
            FollowButtonHelper.handleFollowButtonClick(
                    this,
                    targetUserId,
                    false, // 현재 팔로우하지 않음
                    isFollower,
                    new FollowButtonHelper.FollowActionCallback() {
                        @Override
                        public void onSuccess(boolean nowFollowing) {
                            Toast.makeText(NotificationActivity.this,
                                    notification.getUserName() + " 님을 팔로우했습니다",
                                    Toast.LENGTH_SHORT).show();

                            NotificationManager.getInstance().markAsRead(notification.getId());
                            notification.setRead(true);

                            todayAdapter.notifyDataSetChanged();
                            recentAdapter.notifyDataSetChanged();

                            Log.d(TAG, "✅ 팔로우 성공: " + targetUserId);
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(NotificationActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    /**
     * ✅ FollowButtonHelper를 사용한 언팔로우
     */
    private void performUnfollowWithHelper(String targetUserId, NotificationDTO notification) {
        FollowButtonHelper.checkFollowStatus(targetUserId, (isFollowing, isFollower) -> {
            FollowButtonHelper.handleFollowButtonClick(
                    this,
                    targetUserId,
                    true, // 현재 팔로우 중
                    isFollower,
                    new FollowButtonHelper.FollowActionCallback() {
                        @Override
                        public void onSuccess(boolean nowFollowing) {
                            Toast.makeText(NotificationActivity.this,
                                    notification.getUserName() + " 님을 언팔로우했습니다",
                                    Toast.LENGTH_SHORT).show();

                            NotificationManager.getInstance().markAsRead(notification.getId());
                            notification.setRead(true);

                            todayAdapter.notifyDataSetChanged();
                            recentAdapter.notifyDataSetChanged();

                            Log.d(TAG, "✅ 언팔로우 성공: " + targetUserId);
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(NotificationActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadListener != null) {
            NotificationManager.getInstance().removeListener(unreadListener);
        }
    }
}