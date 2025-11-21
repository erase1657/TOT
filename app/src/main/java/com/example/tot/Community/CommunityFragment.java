package com.example.tot.Community;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // ✅ Firestore
    private FirebaseFirestore db;

    // ✅ 검색 디바운싱
    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY = 300;

    // ✅ 전체 검색 결과 저장 (더보기용)
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
        searchHandler = new Handler(Looper.getMainLooper());

        initViews(view);
        loadDummyData();
        setupRecyclerView();
        setupFilterButtons();
        setupSearch();

        btnWrite.setOnClickListener(v ->
                Toast.makeText(getContext(), "글쓰기 기능 (준비중)", Toast.LENGTH_SHORT).show()
        );

        applyFilter();
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
        adapter = new CommunityAdapter(filteredPosts, new CommunityAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(CommunityPostDTO post, int position) {
                Toast.makeText(getContext(), post.getTitle() + " 상세보기", Toast.LENGTH_SHORT).show();
            }
        }, new CommunityAdapter.OnMoreUsersClickListener() {
            @Override
            public void onMoreUsersClick() {
                showAllUsers();
            }
        });

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

        if (currentFilter == FilterMode.POPULAR) {
            btnPopular.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorSelected));
            btnPopular.setTextColor(textSelected);
        } else {
            btnPopular.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorUnselected));
            btnPopular.setTextColor(textUnselected);
        }

        if (currentFilter == FilterMode.ALL) {
            btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorSelected));
            btnAll.setTextColor(textSelected);
        } else {
            btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorUnselected));
            btnAll.setTextColor(textUnselected);
        }

        if (currentFilter == FilterMode.FRIENDS) {
            btnFriends.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorSelected));
            btnFriends.setTextColor(textSelected);
        } else {
            btnFriends.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorUnselected));
            btnFriends.setTextColor(textUnselected);
        }
    }

    /**
     * ✅ 검색창 설정 (디바운싱 적용)
     */
    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 이전 검색 작업 취소
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // 새로운 검색 작업 예약
                searchRunnable = () -> {
                    searchQuery = s.toString().trim();
                    resetPagination();

                    // ✅ 검색어가 있으면 Firestore에서 사용자 검색
                    if (!searchQuery.isEmpty()) {
                        searchUsersInFirestore(searchQuery);
                    } else {
                        // 검색어가 없으면 기존 필터 적용
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

    /**
     * ✅ Firestore에서 사용자 검색
     */
    private void searchUsersInFirestore(String query) {
        Log.d(TAG, "🔍 사용자 검색 시작: " + query);

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

                    Log.d(TAG, "✅ 검색 결과: " + allUserSearchResults.size() + "명");

                    // ✅ 검색 결과와 게시글 필터링 결과를 어댑터에 전달
                    applyFilterWithUsers();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 사용자 검색 실패", e);
                    Toast.makeText(getContext(), "검색 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    applyFilter();
                });
    }

    /**
     * ✅ 사용자 검색 결과와 게시글 필터링 통합
     */
    private void applyFilterWithUsers() {
        // 게시글 필터링
        List<CommunityPostDTO> filtered = filterPosts();

        // ✅ 사용자 검색 결과를 최대 3개로 제한
        List<UserSearchResult> limitedUsers = allUserSearchResults.size() > 3
                ? allUserSearchResults.subList(0, 3)
                : allUserSearchResults;

        // ✅ 4명 이상일 때만 더보기 버튼 표시
        boolean showMoreButton = allUserSearchResults.size() >= 4;

        // 어댑터에 전달
        adapter.updateDataWithUsers(
                getPagedPosts(filtered, 0),
                limitedUsers,
                !searchQuery.isEmpty(),
                showMoreButton
        );

        isLastPage = (PAGE_SIZE >= filtered.size());
    }

    /**
     * ✅ 더보기 버튼 클릭 시 전체 사용자 표시
     */
    private void showAllUsers() {
        Log.d(TAG, "📋 전체 사용자 표시: " + allUserSearchResults.size() + "명");

        // 게시글 필터링
        List<CommunityPostDTO> filtered = filterPosts();

        // ✅ 전체 사용자 표시 (더보기 버튼 숨김)
        adapter.updateDataWithUsers(
                getPagedPosts(filtered, 0),
                allUserSearchResults,
                !searchQuery.isEmpty(),
                false // 더보기 버튼 숨김
        );

        Toast.makeText(getContext(), allUserSearchResults.size() + "명의 사용자", Toast.LENGTH_SHORT).show();
    }

    /**
     * 필터 적용 (기존 방식)
     */
    private void applyFilter() {
        List<CommunityPostDTO> filtered = filterPosts();
        List<CommunityPostDTO> pagedPosts = getPagedPosts(filtered, 0);

        adapter.updateDataWithUsers(pagedPosts, new ArrayList<>(), false, false);
        isLastPage = (PAGE_SIZE >= filtered.size());
    }

    /**
     * 게시글 필터링 로직
     */
    private List<CommunityPostDTO> filterPosts() {
        List<CommunityPostDTO> filtered = new ArrayList<>();

        // 1단계: 필터 모드 적용
        for (CommunityPostDTO post : allPosts) {
            boolean matchFilter = false;

            switch (currentFilter) {
                case POPULAR:
                case ALL:
                    matchFilter = true;
                    break;
                case FRIENDS:
                    matchFilter = post.isFriend();
                    break;
            }

            if (matchFilter) {
                filtered.add(post);
            }
        }

        // 2단계: 검색어 필터링
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

        // 3단계: 정렬
        if (currentFilter == FilterMode.POPULAR) {
            Collections.sort(filtered, (o1, o2) ->
                    Integer.compare(o2.getHeartCount(), o1.getHeartCount()));
        } else {
            Collections.sort(filtered, (o1, o2) ->
                    Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
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
            List<CommunityPostDTO> filtered = getCurrentFilteredList();
            List<CommunityPostDTO> nextPage = getPagedPosts(filtered, currentPage);

            if (nextPage.isEmpty()) {
                isLastPage = true;
            } else {
                adapter.addData(nextPage);
            }

            isLoading = false;
        }, 500);
    }

    private List<CommunityPostDTO> getCurrentFilteredList() {
        return filterPosts();
    }

    private void resetPagination() {
        currentPage = 0;
        isLoading = false;
        isLastPage = false;
    }

    private void loadDummyData() {
        allPosts = new ArrayList<>();
        long now = System.currentTimeMillis();

        String[] titles = {
                "내 눈동자에 치얼스", "서울 여행 브이로그", "제주도 카페 투어",
                "부산 맛집 추천", "전주 한옥마을 데이트", "강릉 바다 뷰 숙소",
                "여수 밤바다 야경", "경주 역사 여행", "속초 설악산 등산",
                "대구 동성로 쇼핑", "광주 예술의 거리", "인천 차이나타운",
                "수원 화성 탐방", "춘천 닭갈비 맛집", "평창 겨울 여행",
                "통영 케이블카 체험", "남해 독일마을", "가평 아침고요수목원",
                "포항 호미곶 일출", "목포 해상케이블카", "안동 하회마을",
                "경주 불국사", "울산 대왕암공원", "태안 몽산포 해수욕장",
                "보령 머드축제", "단양 패러글라이딩", "담양 죽녹원",
                "순천 순천만습지", "진주 진주성", "창원 진해 벚꽃"
        };

        String[] names = {"박민주", "김서연", "이준호", "최유진", "정민수"};
        int[] profiles = {R.drawable.sample1, R.drawable.sample2, R.drawable.sample3, R.drawable.sample4};
        int[] images = {R.drawable.sample1, R.drawable.sample2, R.drawable.sample3, R.drawable.sample4};
        String[] regions = {"서울", "부산", "제주", "전주", "강릉", "여수", "경주", "속초", "대구", "광주"};
        String[] provinceCodes = {"11", "26", "49", "46", "42", "45", "47", "42", "27", "29"};
        String[] cityCodes = {"11680", "26350", "50110", "45110", "42150", "45110", "47130", "42210", "27200", "29200"};

        for (int i = 0; i < 30; i++) {
            int heartCount;
            if (i % 5 == 0) {
                heartCount = 109000 + (int) (Math.random() * 10000);
            } else if (i % 3 == 0) {
                heartCount = 5000 + (int) (Math.random() * 5000);
            } else {
                heartCount = (int) (Math.random() * 1000);
            }

            allPosts.add(new CommunityPostDTO(
                    "post_" + i,
                    "user_" + (i % 5),
                    names[i % names.length],
                    profiles[i % profiles.length],
                    titles[i % titles.length],
                    images[i % images.length],
                    heartCount,
                    (int) (Math.random() * 100),
                    regions[i % regions.length],
                    provinceCodes[i % provinceCodes.length],
                    cityCodes[i % cityCodes.length],
                    now - (i * 1000000),
                    i % 3 == 0
            ));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    /**
     * ✅ 사용자 검색 결과 DTO
     */
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