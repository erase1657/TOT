package com.example.tot.Community;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommunityFragment extends Fragment {

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
        adapter = new CommunityAdapter(filteredPosts, (post, position) ->
                Toast.makeText(getContext(), post.getTitle() + " 상세보기", Toast.LENGTH_SHORT).show()
        );

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

    /**
     * 필터 버튼 상태 업데이트
     * 버튼 크기나 모양이 변하지 않도록 backgroundTint만 변경하도록 수정.
     */
    private void updateFilterButtonStates() {
        int colorSelected = 0xFF575DFB;     // 선택된 버튼 (보라색)
        int colorUnselected = 0xFFF0F0F5;   // 비활성 버튼 (회색)
        int textSelected = 0xFFFFFFFF;      // 선택된 버튼 텍스트 색 (흰색)
        int textUnselected = 0xFF000000;    // 비활성 텍스트 색 (검정)

        // 인기순 버튼
        if (currentFilter == FilterMode.POPULAR) {
            btnPopular.setBackgroundTintList(ColorStateList.valueOf(colorSelected));
            btnPopular.setTextColor(textSelected);
        } else {
            btnPopular.setBackgroundTintList(ColorStateList.valueOf(colorUnselected));
            btnPopular.setTextColor(textUnselected);
        }

        // 전체보기 버튼
        if (currentFilter == FilterMode.ALL) {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(colorSelected));
            btnAll.setTextColor(textSelected);
        } else {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(colorUnselected));
            btnAll.setTextColor(textUnselected);
        }

        // 친구 버튼
        if (currentFilter == FilterMode.FRIENDS) {
            btnFriends.setBackgroundTintList(ColorStateList.valueOf(colorSelected));
            btnFriends.setTextColor(textSelected);
        } else {
            btnFriends.setBackgroundTintList(ColorStateList.valueOf(colorUnselected));
            btnFriends.setTextColor(textUnselected);
        }
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                resetPagination();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 필터 적용 (인기순/전체보기/친구)
     */
    private void applyFilter() {
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
            // 인기순: 좋아요 수 내림차순
            Collections.sort(filtered, (o1, o2) ->
                    Integer.compare(o2.getHeartCount(), o1.getHeartCount()));
        } else {
            // 전체보기/친구: 최신순
            Collections.sort(filtered, (o1, o2) ->
                    Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
        }

        // 4단계: 페이지네이션
        List<CommunityPostDTO> pagedPosts = getPagedPosts(filtered, 0);
        adapter.updateData(pagedPosts);
        isLastPage = (PAGE_SIZE >= filtered.size());
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
        List<CommunityPostDTO> filtered = new ArrayList<>();

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
            Collections.sort(filtered, (o1, o2) ->
                    Integer.compare(o2.getHeartCount(), o1.getHeartCount()));
        } else {
            Collections.sort(filtered, (o1, o2) ->
                    Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
        }

        return filtered;
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
}
