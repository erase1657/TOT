// 🔥 기존 startListeningForNotifications() 삭제
// 🔥 기존 stopListeningForNotifications() 삭제
// ➕ NotificationManager의 initialLoad() 호출로 교체

package com.example.tot;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tot.Notification.NotificationManager;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private ChipNavigationBar chipNav;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewpager);
        chipNav = findViewById(R.id.navbar);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        // ChipNavigationBar 선택 리스너
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

        // ViewPager 페이지 변경 리스너
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

        // ✅ 기존 startListeningForNotifications() → initialLoad() 로 변경
        NotificationManager.getInstance().initialLoad();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ❌ 실시간 리스너가 없으므로 stop 호출 필요 없음
        // NotificationManager.getInstance().stopListeningForNotifications();
    }
}
