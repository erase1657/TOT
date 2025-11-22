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

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityFragment extends Fragment {

    private static final String TAG = "CommunityFragment";

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private List<CommunityPostDTO> allPosts;
    private List<CommunityPostDTO> filteredPosts;
    private EditText edtSearch;
    private Button btnPopular, btnAll, btnFriends;
    private ImageButton btnWrite;

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

        initViews(view);
        setupRecyclerView();
        setupFilterButtons();
        setupSearch();

        btnWrite.setOnClickListener(v -> {
            ScheduleSelectionDialogFragment dialog = new ScheduleSelectionDialogFragment();
            dialog.show(getParentFragmentManager(), "ScheduleSelection");
        });

        loadFirestorePosts();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_community);
        edtSearch = view.findViewById(R.id.edt_search);
        btnPopular = view.findViewById(R.id.btn_popular);
        btnAll = view.findViewById(R.id.btn_all);
        btnFriends = view.findViewById(R.id.btn_friends);
        btnWrite = view.findViewById(R.id.btn_write);
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

    private List<CommunityPostDTO> filterPosts() {
        List<CommunityPostDTO> filtered = new ArrayList<>();

        for (CommunityPostDTO post : allPosts) {
            boolean matchFilter = (currentFilter == FilterMode.ALL || currentFilter == FilterMode.POPULAR) ||
                    (currentFilter == FilterMode.FRIENDS && post.isFriend());

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

    private void loadFirestorePosts() {
        if (auth.getCurrentUser() == null) {
            allPosts = new ArrayList<>();
            applyFilter();
            return;
        }

        communityPostsRef
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // ✅ 버그 수정: Map 대신 List로 직접 관리
                    List<CommunityPostDTO> tempPosts = new ArrayList<>();
                    Map<String, String> authorUidMap = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String postId = doc.getString("postId");
                        String authorUid = doc.getString("authorUid");
                        String scheduleId = doc.getString("scheduleId");
                        String title = doc.getString("title");
                        String locationName = doc.getString("locationName");
                        Long heartCount = doc.getLong("heartCount");
                        Long commentCount = doc.getLong("commentCount");
                        Long createdAt = doc.getLong("createdAt");

                        if (postId != null && authorUid != null && scheduleId != null) {
                            CommunityPostDTO post = new CommunityPostDTO();
                            post.setPostId(postId);
                            post.setUserId(authorUid);
                            post.setScheduleId(scheduleId);
                            post.setTitle(title != null ? title : "");
                            post.setRegionTag(locationName != null ? locationName : "");
                            post.setHeartCount(heartCount != null ? heartCount.intValue() : 0);
                            post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
                            post.setCreatedAt(createdAt != null ? createdAt : 0);

                            tempPosts.add(post);
                            authorUidMap.put(postId, authorUid);
                        }
                    }

                    loadAuthorInfoBatch(tempPosts, authorUidMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore 게시글 로드 실패", e);
                    allPosts = new ArrayList<>();
                    applyFilter();
                });
    }

    /**
     * ✅ 수정: 각 게시글별로 작성자 정보 로드
     */
    private void loadAuthorInfoBatch(List<CommunityPostDTO> posts, Map<String, String> authorUidMap) {
        if (posts.isEmpty()) {
            allPosts = new ArrayList<>();
            applyFilter();
            return;
        }

        final int[] loadedCount = {0};
        final int totalCount = posts.size();

        for (CommunityPostDTO post : posts) {
            String authorUid = authorUidMap.get(post.getPostId());

            if (authorUid == null) {
                loadedCount[0]++;
                if (loadedCount[0] == totalCount) {
                    allPosts = new ArrayList<>(posts);
                    applyFilter();
                }
                continue;
            }

            db.collection("user").document(authorUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String nickname = userDoc.getString("nickname");
                            String profileImageUrl = userDoc.getString("profileImageUrl");

                            post.setUserName(nickname != null ? nickname : "사용자");
                            post.setProfileImageUrl(profileImageUrl);
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == totalCount) {
                            allPosts = new ArrayList<>(posts);
                            Log.d(TAG, "✅ 모든 작성자 정보 로드 완료: " + allPosts.size() + "개");
                            applyFilter();
                        }
                    })
                    .addOnFailureListener(e -> {
                        loadedCount[0]++;
                        if (loadedCount[0] == totalCount) {
                            allPosts = new ArrayList<>(posts);
                            applyFilter();
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFirestorePosts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
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