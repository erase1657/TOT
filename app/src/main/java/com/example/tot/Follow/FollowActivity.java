package com.example.tot.Follow;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FollowActivity extends AppCompatActivity implements FollowAdapter.FollowListener {

    // UI 요소
    private ImageView btnBack;
    private TextView tvUserName;
    private LinearLayout btnFollower;
    private LinearLayout btnFollowing;
    private TextView tvFollowerLabel;
    private TextView tvFollowingLabel;
    private View viewFollowerIndicator;
    private View viewFollowingIndicator;
    private EditText edtSearch;
    private TextView tvSortMode;
    private ImageView btnSort;
    private RecyclerView recyclerFollow;

    // 데이터
    private List<FollowUserDTO> allFollowers;
    private List<FollowUserDTO> allFollowing;
    private List<FollowUserDTO> displayedUsers;
    private FollowAdapter adapter;

    // 상태
    private boolean isFollowerMode = true;  // true: 팔로워, false: 팔로잉
    private boolean isMyProfile = true;     // true: 내 프로필, false: 친구 프로필
    private String targetUserId;            // 대상 사용자 ID
    private String targetUserName;          // 대상 사용자 이름
    private String searchQuery = "";
    private SortMode currentSortMode = SortMode.DEFAULT;

    enum SortMode {
        DEFAULT,    // 기본 (최신순)
        NAME,       // 이름순
        NICKNAME    // 별명순
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        // Intent에서 데이터 받기
        Intent intent = getIntent();
        targetUserId = intent.getStringExtra("userId");
        targetUserName = intent.getStringExtra("userName");
        isFollowerMode = intent.getBooleanExtra("isFollowerMode", true);
        isMyProfile = intent.getBooleanExtra("isMyProfile", true);

        initViews();
        loadDummyData();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();

        // 초기 필터 적용
        if (isFollowerMode) {
            selectFollowerMode();
        } else {
            selectFollowingMode();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvUserName = findViewById(R.id.tv_user_name);
        btnFollower = findViewById(R.id.btn_follower);
        btnFollowing = findViewById(R.id.btn_following);
        tvFollowerLabel = findViewById(R.id.tv_follower_label);
        tvFollowingLabel = findViewById(R.id.tv_following_label);
        viewFollowerIndicator = findViewById(R.id.view_follower_indicator);
        viewFollowingIndicator = findViewById(R.id.view_following_indicator);
        edtSearch = findViewById(R.id.edt_search);
        tvSortMode = findViewById(R.id.tv_sort_mode);
        btnSort = findViewById(R.id.btn_sort);
        recyclerFollow = findViewById(R.id.recycler_follow);

        // 사용자 이름 설정
        if (targetUserName != null && !targetUserName.isEmpty()) {
            tvUserName.setText(targetUserName);
        } else {
            tvUserName.setText("위찬우"); // 기본값
        }
    }

    private void setupRecyclerView() {
        recyclerFollow.setLayoutManager(new LinearLayoutManager(this));
        displayedUsers = new ArrayList<>();
        adapter = new FollowAdapter(displayedUsers, this, isMyProfile, isFollowerMode);
        recyclerFollow.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFollower.setOnClickListener(v -> selectFollowerMode());

        btnFollowing.setOnClickListener(v -> selectFollowingMode());

        btnSort.setOnClickListener(v -> showSortMenu());
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void selectFollowerMode() {
        isFollowerMode = true;

        // UI 업데이트
        tvFollowerLabel.setTextColor(0xFF000000);
        tvFollowerLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        viewFollowerIndicator.setBackgroundColor(0xFF000000);

        tvFollowingLabel.setTextColor(0xFF999999);
        tvFollowingLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        viewFollowingIndicator.setBackgroundColor(0xFFE0E0E0);

        // 어댑터 모드 변경
        adapter.setFollowerMode(true);

        // 필터 적용
        applyFilter();
    }

    private void selectFollowingMode() {
        isFollowerMode = false;

        // UI 업데이트
        tvFollowingLabel.setTextColor(0xFF000000);
        tvFollowingLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        viewFollowingIndicator.setBackgroundColor(0xFF000000);

        tvFollowerLabel.setTextColor(0xFF999999);
        tvFollowerLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        viewFollowerIndicator.setBackgroundColor(0xFFE0E0E0);

        // 어댑터 모드 변경
        adapter.setFollowerMode(false);

        // 필터 적용
        applyFilter();
    }

    private void applyFilter() {
        List<FollowUserDTO> sourceList = isFollowerMode ? allFollowers : allFollowing;
        List<FollowUserDTO> filtered = new ArrayList<>();

        // 검색 필터
        if (searchQuery.isEmpty()) {
            filtered.addAll(sourceList);
        } else {
            String lowerQuery = searchQuery.toLowerCase();
            for (FollowUserDTO user : sourceList) {
                String name = user.getUserName() != null ? user.getUserName().toLowerCase() : "";
                String nickname = user.getNickname() != null ? user.getNickname().toLowerCase() : "";

                if (name.contains(lowerQuery) || nickname.contains(lowerQuery)) {
                    filtered.add(user);
                }
            }
        }

        // 정렬
        sortUsers(filtered);

        // 어댑터 업데이트
        adapter.updateData(filtered);
    }

    private void sortUsers(List<FollowUserDTO> users) {
        switch (currentSortMode) {
            case DEFAULT:
                // 최신순 (팔로우 시간 역순)
                Collections.sort(users, new Comparator<FollowUserDTO>() {
                    @Override
                    public int compare(FollowUserDTO o1, FollowUserDTO o2) {
                        return Long.compare(o2.getFollowedAt(), o1.getFollowedAt());
                    }
                });
                break;

            case NAME:
                // 이름순 (가나다순)
                Collections.sort(users, new Comparator<FollowUserDTO>() {
                    @Override
                    public int compare(FollowUserDTO o1, FollowUserDTO o2) {
                        String name1 = o1.getUserName() != null ? o1.getUserName() : "";
                        String name2 = o2.getUserName() != null ? o2.getUserName() : "";
                        return name1.compareTo(name2);
                    }
                });
                break;

            case NICKNAME:
                // 별명순 (별명 있는 사람 우선, 그 다음 이름순)
                Collections.sort(users, new Comparator<FollowUserDTO>() {
                    @Override
                    public int compare(FollowUserDTO o1, FollowUserDTO o2) {
                        boolean hasNick1 = o1.getNickname() != null && !o1.getNickname().trim().isEmpty();
                        boolean hasNick2 = o2.getNickname() != null && !o2.getNickname().trim().isEmpty();

                        if (hasNick1 && !hasNick2) return -1;
                        if (!hasNick1 && hasNick2) return 1;

                        if (hasNick1 && hasNick2) {
                            return o1.getNickname().compareTo(o2.getNickname());
                        }

                        return o1.getUserName().compareTo(o2.getUserName());
                    }
                });
                break;
        }
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(this, btnSort);
        popup.getMenuInflater().inflate(R.menu.menu_sort_options, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.sort_default) {
                    currentSortMode = SortMode.DEFAULT;
                    tvSortMode.setText("기본");
                    applyFilter();
                    return true;
                } else if (itemId == R.id.sort_name) {
                    currentSortMode = SortMode.NAME;
                    tvSortMode.setText("이름순");
                    applyFilter();
                    return true;
                } else if (itemId == R.id.sort_nickname) {
                    currentSortMode = SortMode.NICKNAME;
                    tvSortMode.setText("별명순");
                    applyFilter();
                    return true;
                }

                return false;
            }
        });

        popup.show();
    }

    // FollowAdapter.FollowListener 구현
    @Override
    public void onProfileClick(FollowUserDTO user) {
        Toast.makeText(this, user.getUserName() + " 프로필로 이동", Toast.LENGTH_SHORT).show();
        // TODO: 사용자 프로필 화면으로 이동
    }

    @Override
    public void onFollowClick(FollowUserDTO user, int position) {
        if (user.isFollowing()) {
            // ✅ 2번 방식: 언팔로우하지만 프로필은 남김 (팔로잉 화면에서만 적용)
            user.setFollowing(false);
            allFollowing.remove(user);

            Toast.makeText(this, user.getUserName() + " 팔로우 취소", Toast.LENGTH_SHORT).show();

            // 팔로워 모드: 그대로 표시
            // 팔로잉 모드: 프로필은 남기고 버튼만 "팔로우"로 변경
            updateFollowCounts();
            adapter.notifyItemChanged(position);
        } else {
            // 팔로우
            user.setFollowing(true);
            user.setFollowedAt(System.currentTimeMillis());

            // 팔로잉 목록에 추가
            if (!allFollowing.contains(user)) {
                allFollowing.add(user);
            }

            Toast.makeText(this, user.getUserName() + " 팔로우", Toast.LENGTH_SHORT).show();
            updateFollowCounts();
            adapter.notifyItemChanged(position);
        }

        // TODO: 서버에 팔로우/언팔로우 요청
    }

    @Override
    public void onRemoveFollower(FollowUserDTO user, int position) {
        new AlertDialog.Builder(this)
                .setTitle("팔로워 삭제")
                .setMessage(user.getUserName() + "님을 팔로워에서 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    allFollowers.remove(user);
                    Toast.makeText(this, user.getUserName() + " 팔로워 삭제됨", Toast.LENGTH_SHORT).show();
                    updateFollowCounts();
                    applyFilter();

                    // TODO: 서버에 팔로워 삭제 요청
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    public void onNicknameChanged(FollowUserDTO user, String newNickname, int position) {
        Toast.makeText(this, "별명 저장: " + newNickname, Toast.LENGTH_SHORT).show();
        // TODO: 서버에 별명 저장 요청
    }

    private void updateFollowCounts() {
        tvFollowerLabel.setText(allFollowers.size() + " 팔로워");
        tvFollowingLabel.setText(allFollowing.size() + " 팔로잉");
    }

    /**
     * ✅ 팔로워/팔로잉 수 반환 (마이페이지 동기화용)
     */
    public int getFollowerCount() {
        return allFollowers.size();
    }

    public int getFollowingCount() {
        return allFollowing.size();
    }

    private void loadDummyData() {
        allFollowers = new ArrayList<>();
        allFollowing = new ArrayList<>();

        long now = System.currentTimeMillis();
        String[] names = {"박민주", "김서연", "이준호", "최유진", "정민수", "한지우", "송하늘", "강민지"};
        int[] profiles = {R.drawable.sample1, R.drawable.sample2, R.drawable.sample3, R.drawable.sample4};

        // 팔로워 더미 데이터 (5명)
        for (int i = 0; i < 5; i++) {
            boolean isFollowing = i % 2 == 0;
            allFollowers.add(new FollowUserDTO(
                    "follower_" + i,
                    names[i % names.length],
                    i % 3 == 0 ? "별명" + i : null,
                    "상태메시지입니다",
                    profiles[i % profiles.length],
                    isFollowing,
                    true,
                    now - (i * 1000000L)
            ));
        }

        // 팔로잉 더미 데이터 (4명)
        for (int i = 0; i < 4; i++) {
            FollowUserDTO user;
            if (i < 2) {
                // 맞팔 사용자
                user = allFollowers.get(i * 2);
            } else {
                // 추가 팔로잉 사용자
                user = new FollowUserDTO(
                        "following_" + i,
                        names[(i + 3) % names.length],
                        null,
                        "상태메시지",
                        profiles[i % profiles.length],
                        true,
                        false,
                        now - (i * 1000000L)
                );
            }
            allFollowing.add(user);
        }

        updateFollowCounts();
    }

    @Override
    public void finish() {
        // ✅ 결과 데이터 전달
        Intent resultIntent = new Intent();
        resultIntent.putExtra("followerCount", allFollowers.size());
        resultIntent.putExtra("followingCount", allFollowing.size());
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}