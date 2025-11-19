package com.example.tot.Notification;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout todaySection;
    private LinearLayout recentSection;
    private LinearLayout emptyView;
    private RecyclerView recyclerToday;
    private RecyclerView recyclerRecent;

    private NotificationAdapter todayAdapter;
    private NotificationAdapter recentAdapter;

    private List<NotificationDTO> todayNotifications = new ArrayList<>();
    private List<NotificationDTO> recentNotifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        initViews();
        setupRecyclerViews();
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

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        // 오늘 알림 RecyclerView
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

        // 최근 30일 알림 RecyclerView
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

    private void loadNotifications() {
        // NotificationManager에서 데이터 가져오기
        NotificationManager manager = NotificationManager.getInstance();
        todayNotifications.addAll(manager.getTodayNotifications());
        recentNotifications.addAll(manager.getRecentNotifications());
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
        // 알림을 읽음 처리
        notification.setRead(true);
        todayAdapter.notifyDataSetChanged();
        recentAdapter.notifyDataSetChanged();

        // 알림 타입별 처리
        switch (notification.getType()) {
            case SCHEDULE_INVITE:
                Toast.makeText(this, "일정 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                // TODO: 일정 상세 화면으로 이동
                break;
            case FOLLOW:
                Toast.makeText(this, notification.getUserName() + " 님의 프로필로 이동", Toast.LENGTH_SHORT).show();
                // TODO: 프로필 화면으로 이동
                break;
            case COMMENT:
                Toast.makeText(this, "게시물 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                // TODO: 게시물 상세 화면으로 이동
                break;
        }
    }

    private void handleFollowBack(NotificationDTO notification) {
        Toast.makeText(this, notification.getUserName() + " 님을 팔로우했습니다", Toast.LENGTH_SHORT).show();
        // TODO: 팔로우 API 호출
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
}