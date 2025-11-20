package com.example.tot.Follow;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.example.tot.Notification.NotificationManager;
import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FollowActivity extends AppCompatActivity implements FollowAdapter.FollowListener {

    private static final String TAG = "FollowActivity";

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
    private boolean isFollowerMode = true;
    private boolean isMyProfile = true;
    private String targetUserId;
    private String targetUserName;
    private String searchQuery = "";
    private SortMode currentSortMode = SortMode.DEFAULT;

    // Firestore
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    enum SortMode {
        DEFAULT,
        NAME,
        NICKNAME
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        targetUserId = intent.getStringExtra("userId");
        targetUserName = intent.getStringExtra("userName");
        isFollowerMode = intent.getBooleanExtra("isFollowerMode", true);
        isMyProfile = intent.getBooleanExtra("isMyProfile", true);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();

        // Firestore에서 실제 데이터 로드
        loadFollowersFromFirestore();
        loadFollowingFromFirestore();

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

        if (targetUserName != null && !targetUserName.isEmpty()) {
            tvUserName.setText(targetUserName);
        } else {
            tvUserName.setText("사용자");
        }
    }

    private void setupRecyclerView() {
        recyclerFollow.setLayoutManager(new LinearLayoutManager(this));
        displayedUsers = new ArrayList<>();
        allFollowers = new ArrayList<>();
        allFollowing = new ArrayList<>();
        adapter = new FollowAdapter(displayedUsers, this, isMyProfile, isFollowerMode);
        recyclerFollow.setAdapter(adapter);
    }

    /**
     * ✅ Firestore에서 팔로워 목록 로드
     */
    private void loadFollowersFromFirestore() {
        if (targetUserId == null) return;

        db.collection("user")
                .document(targetUserId)
                .collection("follower")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allFollowers.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String followerId = doc.getId();

                        // 팔로워 사용자 정보 가져오기
                        db.collection("user")
                                .document(followerId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        FollowUserDTO user = new FollowUserDTO();
                                        user.setUserId(followerId);
                                        user.setUserName(userDoc.getString("nickname"));
                                        user.setStatusMessage(userDoc.getString("comment"));
                                        user.setFollowedAt(doc.getLong("followedAt") != null ? doc.getLong("followedAt") : System.currentTimeMillis());

                                        // ✅ 내가 이 사용자를 팔로우하고 있는지 확인 + 별명 로드
                                        checkIfFollowingAndLoadNickname(user);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "팔로워 로드 실패", e);
                    Toast.makeText(this, "팔로워 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ Firestore에서 팔로잉 목록 로드
     */
    private void loadFollowingFromFirestore() {
        if (targetUserId == null) return;

        db.collection("user")
                .document(targetUserId)
                .collection("following")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allFollowing.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String followingId = doc.getId();

                        // ✅ 별명 로드 (following 문서에서)
                        String customNickname = doc.getString("customNickname");

                        // 팔로잉 사용자 정보 가져오기
                        db.collection("user")
                                .document(followingId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        FollowUserDTO user = new FollowUserDTO();
                                        user.setUserId(followingId);
                                        user.setUserName(userDoc.getString("nickname"));
                                        user.setStatusMessage(userDoc.getString("comment"));
                                        user.setFollowing(true); // 이미 팔로잉 중
                                        user.setNickname(customNickname); // ✅ 별명 설정
                                        user.setFollowedAt(doc.getLong("followedAt") != null ? doc.getLong("followedAt") : System.currentTimeMillis());

                                        // 이 사용자가 나를 팔로우하고 있는지 확인
                                        checkIfFollower(user);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "팔로잉 로드 실패", e);
                    Toast.makeText(this, "팔로잉 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ 내가 이 사용자를 팔로우하고 있는지 확인 + 별명 로드
     */
    private void checkIfFollowingAndLoadNickname(FollowUserDTO user) {
        if (targetUserId == null) return;

        db.collection("user")
                .document(targetUserId)
                .collection("following")
                .document(user.getUserId())
                .get()
                .addOnSuccessListener(doc -> {
                    user.setFollowing(doc.exists());
                    user.setFollower(true); // 팔로워 목록에서 가져왔으므로 true

                    // ✅ 별명 로드
                    if (doc.exists()) {
                        String customNickname = doc.getString("customNickname");
                        user.setNickname(customNickname);
                    }

                    if (!allFollowers.contains(user)) {
                        allFollowers.add(user);
                    }

                    updateFollowCounts();
                    applyFilter();
                });
    }

    /**
     * ✅ 이 사용자가 나를 팔로우하고 있는지 확인
     */
    private void checkIfFollower(FollowUserDTO user) {
        if (targetUserId == null) return;

        db.collection("user")
                .document(targetUserId)
                .collection("follower")
                .document(user.getUserId())
                .get()
                .addOnSuccessListener(doc -> {
                    user.setFollower(doc.exists());

                    if (!allFollowing.contains(user)) {
                        allFollowing.add(user);
                    }

                    updateFollowCounts();
                    applyFilter();
                });
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
        tvFollowerLabel.setTextColor(0xFF000000);
        tvFollowerLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        viewFollowerIndicator.setBackgroundColor(0xFF000000);
        tvFollowingLabel.setTextColor(0xFF999999);
        tvFollowingLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        viewFollowingIndicator.setBackgroundColor(0xFFE0E0E0);
        adapter.setFollowerMode(true);
        applyFilter();
    }

    private void selectFollowingMode() {
        isFollowerMode = false;
        tvFollowingLabel.setTextColor(0xFF000000);
        tvFollowingLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        viewFollowingIndicator.setBackgroundColor(0xFF000000);
        tvFollowerLabel.setTextColor(0xFF999999);
        tvFollowerLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        viewFollowerIndicator.setBackgroundColor(0xFFE0E0E0);
        adapter.setFollowerMode(false);
        applyFilter();
    }

    private void applyFilter() {
        List<FollowUserDTO> sourceList = isFollowerMode ? allFollowers : allFollowing;
        List<FollowUserDTO> filtered = new ArrayList<>();

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

        sortUsers(filtered);
        adapter.updateData(filtered);
    }

    private void sortUsers(List<FollowUserDTO> users) {
        switch (currentSortMode) {
            case DEFAULT:
                Collections.sort(users, (o1, o2) ->
                        Long.compare(o2.getFollowedAt(), o1.getFollowedAt()));
                break;
            case NAME:
                Collections.sort(users, (o1, o2) -> {
                    String name1 = o1.getUserName() != null ? o1.getUserName() : "";
                    String name2 = o2.getUserName() != null ? o2.getUserName() : "";
                    return name1.compareTo(name2);
                });
                break;
            case NICKNAME:
                Collections.sort(users, (o1, o2) -> {
                    boolean hasNick1 = o1.getNickname() != null && !o1.getNickname().trim().isEmpty();
                    boolean hasNick2 = o2.getNickname() != null && !o2.getNickname().trim().isEmpty();
                    if (hasNick1 && !hasNick2) return -1;
                    if (!hasNick1 && hasNick2) return 1;
                    if (hasNick1 && hasNick2) {
                        return o1.getNickname().compareTo(o2.getNickname());
                    }
                    return o1.getUserName().compareTo(o2.getUserName());
                });
                break;
        }
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(this, btnSort);
        popup.getMenuInflater().inflate(R.menu.menu_sort_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
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
        });
        popup.show();
    }

    @Override
    public void onProfileClick(FollowUserDTO user) {
        // 프로필 화면으로 이동 (이미 FollowAdapter에서 처리됨)
    }

    @Override
    public void onFollowClick(FollowUserDTO user, int position) {
        if (user.isFollowing()) {
            performUnfollow(user, position);
        } else {
            performFollow(user, position);
        }
    }

    /**
     * ✅ 팔로우 실행 (양방향 처리 + 알림 전송)
     * 🔥 핵심 수정: 실제 로그인한 사용자 ID 사용
     */
    private void performFollow(FollowUserDTO user, int position) {
        if (targetUserId == null || mAuth.getCurrentUser() == null) return;

        // 🔥 실제 로그인한 사용자 ID
        String myActualUserId = mAuth.getCurrentUser().getUid();

        Map<String, Object> followData = new HashMap<>();
        followData.put("followedAt", System.currentTimeMillis());

        // ✅ 1. 내 following에 추가
        db.collection("user")
                .document(targetUserId)
                .collection("following")
                .document(user.getUserId())
                .set(followData)
                .addOnSuccessListener(aVoid -> {
                    // ✅ 2. 상대방 follower에 추가
                    db.collection("user")
                            .document(user.getUserId())
                            .collection("follower")
                            .document(targetUserId)
                            .set(followData)
                            .addOnSuccessListener(aVoid2 -> {
                                user.setFollowing(true);
                                user.setFollowedAt(System.currentTimeMillis());

                                if (!allFollowing.contains(user)) {
                                    allFollowing.add(user);
                                }

                                // 🔥 3. 팔로우 알림 전송 (수정됨!)
                                sendFollowNotification(user.getUserId(), myActualUserId);

                                Toast.makeText(this, user.getUserName() + " 팔로우", Toast.LENGTH_SHORT).show();
                                updateFollowCounts();
                                adapter.notifyItemChanged(position);

                                Log.d(TAG, "✅ 팔로우 성공: " + targetUserId + " → " + user.getUserId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 상대방 follower 추가 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로우 실패", e);
                    Toast.makeText(this, "팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 🔥 핵심 수정: 팔로우 알림 전송
     * @param recipientUserId 팔로우를 받는 사람 (상대방)
     * @param senderUserId 팔로우를 하는 사람 (실제 로그인한 나)
     */
    private void sendFollowNotification(String recipientUserId, String senderUserId) {
        // 🔥 실제 로그인한 내 프로필 정보 가져오기
        db.collection("user")
                .document(senderUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String myNickname = doc.getString("nickname");
                        if (myNickname == null || myNickname.isEmpty()) {
                            myNickname = "사용자";
                        }

                        // 🔥 수정: recipientId = 팔로우를 받는 사람 (상대방)
                        //          senderId = 실제 로그인한 나
                        NotificationManager.getInstance()
                                .addFollowNotification(
                                        recipientUserId,  // 상대방 ID (알림을 받을 사람)
                                        myNickname,       // 내 닉네임
                                        senderUserId      // 🔥 실제 내 ID (알림을 보낸 사람)
                                );

                        Log.d(TAG, "✅ 팔로우 알림 전송 성공");
                        Log.d(TAG, "   - 받는 사람: " + recipientUserId);
                        Log.d(TAG, "   - 보낸 사람: " + senderUserId + " (" + myNickname + ")");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 내 프로필 정보 로드 실패", e);
                });
    }

    /**
     * ✅ 언팔로우 실행 (양방향 처리)
     */
    private void performUnfollow(FollowUserDTO user, int position) {
        new AlertDialog.Builder(this)
                .setTitle("언팔로우")
                .setMessage("정말 언팔로우하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    if (targetUserId == null) return;

                    // ✅ 1. 내 following에서 삭제
                    db.collection("user")
                            .document(targetUserId)
                            .collection("following")
                            .document(user.getUserId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // ✅ 2. 상대방 follower에서 삭제
                                db.collection("user")
                                        .document(user.getUserId())
                                        .collection("follower")
                                        .document(targetUserId)
                                        .delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            user.setFollowing(false);
                                            allFollowing.remove(user);

                                            Toast.makeText(this, user.getUserName() + " 팔로우 취소", Toast.LENGTH_SHORT).show();
                                            updateFollowCounts();
                                            adapter.notifyItemChanged(position);

                                            Log.d(TAG, "✅ 언팔로우 성공: " + user.getUserId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 상대방 follower 삭제 실패", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 언팔로우 실패", e);
                                Toast.makeText(this, "언팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("아니오", null)
                .show();
    }

    @Override
    public void onRemoveFollower(FollowUserDTO user, int position) {
        new AlertDialog.Builder(this)
                .setTitle("팔로워 삭제")
                .setMessage(user.getUserName() + "님을 팔로워에서 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    if (targetUserId == null) return;

                    // ✅ 1. 내 follower에서 삭제
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(user.getUserId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // ✅ 2. 상대방 following에서 삭제
                                db.collection("user")
                                        .document(user.getUserId())
                                        .collection("following")
                                        .document(targetUserId)
                                        .delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            allFollowers.remove(user);
                                            Toast.makeText(this, user.getUserName() + " 팔로워 삭제됨", Toast.LENGTH_SHORT).show();
                                            updateFollowCounts();
                                            applyFilter();

                                            Log.d(TAG, "✅ 팔로워 삭제 성공: " + user.getUserId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 상대방 following 삭제 실패", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 팔로워 삭제 실패", e);
                                Toast.makeText(this, "팔로워 삭제 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * ✅ 별명 변경 (Firestore following 문서에 저장)
     */
    @Override
    public void onNicknameChanged(FollowUserDTO user, String newNickname, int position) {
        if (targetUserId == null) return;

        db.collection("user")
                .document(targetUserId)
                .collection("following")
                .document(user.getUserId())
                .update("customNickname", newNickname.isEmpty() ? null : newNickname)
                .addOnSuccessListener(aVoid -> {
                    user.setNickname(newNickname);
                    Toast.makeText(this, "별명이 저장되었습니다", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);

                    Log.d(TAG, "✅ 별명 저장 성공: " + newNickname);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 별명 저장 실패", e);
                    Toast.makeText(this, "별명 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFollowCounts() {
        tvFollowerLabel.setText(allFollowers.size() + " 팔로워");
        tvFollowingLabel.setText(allFollowing.size() + " 팔로잉");
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("followerCount", allFollowers.size());
        resultIntent.putExtra("followingCount", allFollowing.size());
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}