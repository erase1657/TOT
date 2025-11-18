package com.example.tot.MyPage;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * 타인의 프로필을 보여주는 Activity
 * 실제 UI는 MyPageFragment를 재사용
 */
public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private String targetUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 빈 컨테이너 레이아웃 사용

        targetUserId = getIntent().getStringExtra("userId");

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 현재 로그인한 사용자와 동일한 경우 체크
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null &&
                targetUserId.equals(mAuth.getCurrentUser().getUid())) {
            Toast.makeText(this, "내 프로필입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // MyPageFragment를 타인 모드로 로드
        loadUserProfileFragment();
    }

    private void loadUserProfileFragment() {
        Fragment fragment = MyPageFragment.newInstance(targetUserId);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.content, fragment);
        transaction.commit();
    }
}