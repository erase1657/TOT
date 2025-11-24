package com.example.tot.Community;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommunityFragment extends Fragment implements CommunityDataManager.DataUpdateListener {

    private static final String TAG = "CommunityFragment";

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private List<CommunityPostDTO> allPosts;
    private List<CommunityPostDTO> filteredPosts;
    private EditText edtSearch;
    private Button btnPopular, btnAll, btnFriends;
    private ImageButton btnWrite;

    // ✅ 새로고침 기능 추가
    private SwipeRefreshLayout swipeRefreshLayout;

    private FilterMode currentFilter = FilterMode.ALL;
    private String searchQuery = "";

    private static final int PAGE_SIZE = 15;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private CollectionReference communityPostsRef;

    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY = 300;

    private List<UserSearchResult> allUserSearchResults = new ArrayList<>();

    private Set<String> followingSet = new HashSet<>();
    private Set<String> followerSet = new HashSet<>();

    private CommunityDataManager dataManager;

    enum FilterMode {
        POPULAR,
        ALL,
        FRIENDS
    }

    public CommunityFragment() {
        super(R.layout.fragment_community);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        communityPostsRef = db.collection("public")
                .document("community")
                .collection("posts");
        searchHandler = new Handler(Looper.getMainLooper());

        dataManager = CommunityDataManager.getInstance();
        dataManager.addListener(this);

        initViews(view);
        setupRecyclerView();
        setupFilterButtons();
        setupSearch();
        // ✅ 새로고침 설정
        setupSwipeRefresh();

        btnWrite.setOnClickListener(v -> {
            ScheduleSelectionDialogFragment dialog = new ScheduleSelectionDialogFragment();
            dialog.show(getParentFragmentManager(), "ScheduleSelection");
        });

        loadFollowRelations(() -> {
            dataManager.getPosts(false);
        });
    }

    @Override
    public void onDataUpdated(List<CommunityPostDTO> posts) {
        allPosts = new ArrayList<>(posts);
        Log.d(TAG, "✅ 데이터 업데이트 수신: " + allPosts.size() + "개");
        applyFilter();
    }

    @Override
    public void onDataLoadFailed(String error) {
        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        allPosts = new ArrayList<>();
        applyFilter();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_community);
        edtSearch = view.findViewById(R.id.edt_search);
        btnPopular = view.findViewById(R.id.btn_popular);
        btnAll = view.findViewById(R.id.btn_all);
        btnFriends = view.findViewById(R.id.btn_friends);
        btnWrite = view.findViewById(R.id.btn_write);
        // ✅ SwipeRefreshLayout 초기화
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_community);
    }

    // ✅ 새로고침 기능 설정
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout == null) return;

        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light)
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshCommunityData);
    }

    // ✅ 새로고침 실행
    private void refreshCommunityData() {
        Log.d(TAG, "🔄 커뮤니티 새로고침 시작");

        // 팔로우 관계 다시 로드
        loadFollowRelations(() -> {
            // 데이터 강제 새로고침
            dataManager.getPosts(true);

            // 사용자 검색 결과 초기화
            if (!searchQuery.isEmpty()) {
                searchUsersInFirestore(searchQuery);
            }
        });

        // 1초 후 새로고침 종료
        swipeRefreshLayout.postDelayed(() -> {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), "새로고침 완료", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        filteredPosts = new ArrayList<>();
        adapter = new CommunityAdapter(filteredPosts, (post, position) -> {
            if (post.getScheduleId() != null && post.getUserId() != null) {
                Intent intent = new Intent(getContext(), PostDetailActivity.class);
                intent.putExtra("scheduleId", post.getScheduleId());
                intent.putExtra("authorUid", post.getUserId());
                intent.putExtra("postId", post.getPostId());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "게시글 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        }, () -> showAllUsers());

        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager != null) {
                    int visibleItemCount = manager.getChildCount();
                    int totalItemCount = manager.getItemCount();
                    int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= PAGE_SIZE) {
                            loadMorePosts();
                        }
                    }
                }
            }
        });
    }

    private void setupFilterButtons() {
        btnPopular.setOnClickListener(v -> {
            currentFilter = FilterMode.POPULAR;
            updateFilterButtonStates();
            resetPagination();
            applyFilter();
        });

        btnAll.setOnClickListener(v -> {
            currentFilter = FilterMode.ALL;
            updateFilterButtonStates();
            resetPagination();
            applyFilter();
        });

        btnFriends.setOnClickListener(v -> {
            currentFilter = FilterMode.FRIENDS;
            updateFilterButtonStates();
            resetPagination();
            applyFilter();
        });
    }

    private void updateFilterButtonStates() {
        int colorSelected = 0xFF575DFB;
        int colorUnselected = 0xFFF0F0F5;
        int textSelected = 0xFFFFFFFF;
        int textUnselected = 0xFF000000;

        btnPopular.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                currentFilter == FilterMode.POPULAR ? colorSelected : colorUnselected));
        btnPopular.setTextColor(currentFilter == FilterMode.POPULAR ? textSelected : textUnselected);

        btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                currentFilter == FilterMode.ALL ? colorSelected : colorUnselected));
        btnAll.setTextColor(currentFilter == FilterMode.ALL ? textSelected : textUnselected);

        btnFriends.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                currentFilter == FilterMode.FRIENDS ? colorSelected : colorUnselected));
        btnFriends.setTextColor(currentFilter == FilterMode.FRIENDS ? textSelected : textUnselected);
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    searchQuery = s.toString().trim();
                    resetPagination();

                    if (!searchQuery.isEmpty()) {
                        searchUsersInFirestore(searchQuery);
                    } else {
                        allUserSearchResults.clear();
                        applyFilter();
                    }
                };

                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsersInFirestore(String query) {
        db.collection("user")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUserSearchResults.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getId();
                        String nickname = document.getString("nickname");
                        String email = document.getString("email");

                        if (nickname != null && nickname.toLowerCase().contains(query.toLowerCase())) {
                            allUserSearchResults.add(new UserSearchResult(
                                    userId,
                                    nickname,
                                    email,
                                    document.getString("comment"),
                                    document.getString("profileImageUrl")
                            ));
                        }
                    }

                    applyFilterWithUsers();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "검색 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    applyFilter();
                });
    }

    private void applyFilterWithUsers() {
        List<CommunityPostDTO> filtered = filterPosts();
        List<UserSearchResult> limitedUsers = allUserSearchResults.size() > 3
                ? allUserSearchResults.subList(0, 3)
                : allUserSearchResults;
        boolean showMoreButton = allUserSearchResults.size() >= 4;

        adapter.updateDataWithUsers(getPagedPosts(filtered, 0), limitedUsers, !searchQuery.isEmpty(), showMoreButton);
        isLastPage = (PAGE_SIZE >= filtered.size());
    }

    private void showAllUsers() {
        List<CommunityPostDTO> filtered = filterPosts();
        adapter.updateDataWithUsers(getPagedPosts(filtered, 0), allUserSearchResults, !searchQuery.isEmpty(), false);
        Toast.makeText(getContext(), allUserSearchResults.size() + "명의 사용자", Toast.LENGTH_SHORT).show();
    }

    private void applyFilter() {
        List<CommunityPostDTO> filtered = filterPosts();
        List<CommunityPostDTO> pagedPosts = getPagedPosts(filtered, 0);
        adapter.updateDataWithUsers(pagedPosts, new ArrayList<>(), false, false);
        isLastPage = (PAGE_SIZE >= filtered.size());
    }

    private void loadFollowRelations(Runnable onComplete) {
        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUid == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        final int[] loadCount = {0};

        db.collection("user")
                .document(currentUid)
                .collection("following")
                .get()
                .addOnSuccessListener(snapshot -> {
                    followingSet.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        followingSet.add(doc.getId());
                    }
                    Log.d(TAG, "✅ 팔로잉 로드 완료: " + followingSet.size() + "명");
                    loadCount[0]++;
                    if (loadCount[0] == 2 && onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로잉 로드 실패", e);
                    loadCount[0]++;
                    if (loadCount[0] == 2 && onComplete != null) onComplete.run();
                });

        db.collection("user")
                .document(currentUid)
                .collection("follower")
                .get()
                .addOnSuccessListener(snapshot -> {
                    followerSet.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        followerSet.add(doc.getId());
                    }
                    Log.d(TAG, "✅ 팔로워 로드 완료: " + followerSet.size() + "명");
                    loadCount[0]++;
                    if (loadCount[0] == 2 && onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로워 로드 실패", e);
                    loadCount[0]++;
                    if (loadCount[0] == 2 && onComplete != null) onComplete.run();
                });
    }

    private List<CommunityPostDTO> filterPosts() {
        List<CommunityPostDTO> filtered = new ArrayList<>();
        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (allPosts == null) {
            return filtered;
        }

        for (CommunityPostDTO post : allPosts) {
            boolean matchFilter = false;

            switch (currentFilter) {
                case POPULAR:
                case ALL:
                    matchFilter = true;
                    break;
                case FRIENDS:
                    String postAuthorId = post.getUserId();
                    if (postAuthorId != null) {
                        boolean isMyPost = currentUid != null && postAuthorId.equals(currentUid);
                        boolean isFollowing = followingSet.contains(postAuthorId);
                        boolean isFollower = followerSet.contains(postAuthorId);
                        matchFilter = isMyPost || isFollowing || isFollower;
                    }
                    break;
            }

            if (matchFilter) {
                filtered.add(post);
            }
        }

        if (!searchQuery.isEmpty()) {
            List<CommunityPostDTO> searchFiltered = new ArrayList<>();
            for (CommunityPostDTO post : filtered) {
                if (post.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                        post.getRegionTag().toLowerCase().contains(searchQuery.toLowerCase())) {
                    searchFiltered.add(post);
                }
            }
            filtered = searchFiltered;
        }

        if (currentFilter == FilterMode.POPULAR) {
            Collections.sort(filtered, (o1, o2) -> Integer.compare(o2.getHeartCount(), o1.getHeartCount()));
        } else {
            Collections.sort(filtered, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
        }

        return filtered;
    }

    private List<CommunityPostDTO> getPagedPosts(List<CommunityPostDTO> source, int page) {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, source.size());

        if (start >= source.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(source.subList(start, end));
    }

    private void loadMorePosts() {
        isLoading = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            currentPage++;
            List<CommunityPostDTO> filtered = filterPosts();
            List<CommunityPostDTO> nextPage = getPagedPosts(filtered, currentPage);

            if (nextPage.isEmpty()) {
                isLastPage = true;
            } else {
                adapter.addData(nextPage);
            }

            isLoading = false;
        }, 500);
    }

    private void resetPagination() {
        currentPage = 0;
        isLoading = false;
        isLastPage = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFollowRelations(() -> {
            dataManager.getPosts(false);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (dataManager != null) {
            dataManager.removeListener(this);
        }
    }

    public static class UserSearchResult {
        private String userId;
        private String nickname;
        private String email;
        private String statusMessage;
        private String profileImageUrl;

        public UserSearchResult(String userId, String nickname, String email, String statusMessage, String profileImageUrl) {
            this.userId = userId;
            this.nickname = nickname;
            this.email = email;
            this.statusMessage = statusMessage;
            this.profileImageUrl = profileImageUrl;
        }

        public String getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getEmail() { return email; }
        public String getStatusMessage() { return statusMessage; }
        public String getProfileImageUrl() { return profileImageUrl; }
    }
}