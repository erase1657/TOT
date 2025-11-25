package com.example.tot;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tot.Authentication.LoginActivity;
import com.example.tot.Notification.NotificationManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private ChipNavigationBar chipNav;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Uri data = getIntent().getData();
        if (data != null) {
            String senderUid = data.getQueryParameter("senderUid");
            String scheduleId = data.getQueryParameter("scheduleId");
            String inviteId = data.getQueryParameter("inviteId");

            boolean validInvite = senderUid != null && scheduleId != null && inviteId != null;

            if (validInvite) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    String receiverUid = user.getUid();
                    updateInvitedDocument(senderUid, receiverUid, scheduleId, inviteId);
                } else {
                    // 로그인 안 되어 있으면 LoginActivity로 넘기고, 거기서 다시 Main으로 돌아오게 설계하면 됨
                    Intent login = new Intent(this, LoginActivity.class);
                    login.putExtra("senderUid", senderUid);
                    login.putExtra("scheduleId", scheduleId);
                    login.putExtra("inviteId", inviteId);
                    startActivity(login);
                    finish();
                    return;
                }
            }
        }
        viewPager = findViewById(R.id.viewpager);
        chipNav = findViewById(R.id.navbar);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        chipNav.setOnItemSelectedListener(id -> {
            if (id == R.id.home) {
                viewPager.setCurrentItem(0);
            } else if (id == R.id.schedule) {
                viewPager.setCurrentItem(1);
            } else if (id == R.id.community) {
                viewPager.setCurrentItem(2);
            } else if (id == R.id.mypage) {
                viewPager.setCurrentItem(3);
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        chipNav.setItemSelected(R.id.home, true);
                        break;
                    case 1:
                        chipNav.setItemSelected(R.id.schedule, true);
                        break;
                    case 2:
                        chipNav.setItemSelected(R.id.community, true);
                        break;
                    case 3:
                        chipNav.setItemSelected(R.id.mypage, true);
                        break;
                }
            }
        });

        chipNav.setItemSelected(R.id.home, true);

        // ✅ 초기 로드 (실시간 리스너 시작)
        NotificationManager.getInstance().initialLoad();
    }
    private void updateInvitedDocument(String senderUid,
                                       String receiverUid,
                                       String scheduleId,
                                       String inviteId) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> invitedData = new HashMap<>();
        invitedData.put("senderUid", senderUid);
        invitedData.put("receiverUid", receiverUid);
        invitedData.put("scheduleId", scheduleId);
        invitedData.put("inviteId", inviteId);
        invitedData.put("status", "pending");             // 필요 시 다른 값으로 변경 가능
        invitedData.put("createdAt", System.currentTimeMillis());

        db.collection("user")
                .document(senderUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("invited")
                .document(inviteId)
                .set(invitedData)       // ★ set() = 전체 필드 갱신
                .addOnSuccessListener(a -> {
                    // Log.d("Invite", "invited 전체 필드 갱신 완료");
                })
                .addOnFailureListener(e -> {
                    // Log.e("Invite", "invited 갱신 실패", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ 실시간 리스너 정리
        NotificationManager.getInstance().stopListening();
    }
}