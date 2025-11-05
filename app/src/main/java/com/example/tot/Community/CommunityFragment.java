package com.example.tot.Community;

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
    private List<CommunityPostDTO> allPosts;           // 전체 게시글
    private List<CommunityPostDTO> filteredPosts;      // 필터링된 게시글.
    private EditText edtSearch;
    private Button btnPopular, btnAll, btnFriends;
    private ImageButton btnWrite;

    // 필터 상태
    private FilterMode currentFilter = FilterMode.ALL;
    private String searchQuery = "";

    // 페이지네이션
    private static final int PAGE_SIZE = 15;  // 한 페이지당 15개씩
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    enum FilterMode {
        POPULAR,   // 인기순
        ALL,       // 전체보기 (최신순)
        FRIENDS    // 친구
    }

    public CommunityFragment() {
        super(R.layout.fragment_community);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View 초기화
        initViews(view);

        // 더미 데이터 로드
        loadDummyData();

        // RecyclerView 설정
        setupRecyclerView();

        // 버튼 클릭 리스너
        setupFilterButtons();

        // 검색 기능
        setupSearch();

        // 글쓰기 버튼
        btnWrite.setOnClickListener(v -> {
            Toast.makeText(getContext(), "글쓰기 기능 (준비중)", Toast.LENGTH_SHORT).show();
            // TODO: 글쓰기 화면으로 이동
        });

        // 초기 데이터 로드
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

    /**
     * RecyclerView 설정
     */
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        filteredPosts = new ArrayList<>();
        adapter = new CommunityAdapter(filteredPosts, new CommunityAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(CommunityPostDTO post, int position) {
                Toast.makeText(getContext(),
                        post.getTitle() + " 상세보기",
                        Toast.LENGTH_SHORT).show();
                // TODO: 게시글 상세 화면으로 이동
            }
        });

        recyclerView.setAdapter(adapter);

        // 무한 스크롤 (페이지네이션)
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager != null) {
                    int visibleItemCount = manager.getChildCount();
                    int totalItemCount = manager.getItemCount();
                    int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();

                    // 하단에 도달했을 때 로드
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

    /**
     * 필터 버튼 설정
     */
    private void setupFilterButtons() {
        btnPopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = FilterMode.POPULAR;
                updateFilterButtonStates();
                resetPagination();
                applyFilter();
            }
        });

        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = FilterMode.ALL;
                updateFilterButtonStates();
                resetPagination();
                applyFilter();
            }
        });

        btnFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = FilterMode.FRIENDS;
                updateFilterButtonStates();
                resetPagination();
                applyFilter();
            }
        });
    }

    /**
     * 필터 버튼 상태 업데이트
     */
    private void updateFilterButtonStates() {
        btnPopular.setBackgroundResource(
                currentFilter == FilterMode.POPULAR ? R.drawable.button_style1 : R.drawable.button_style2);
        btnPopular.setTextColor(
                currentFilter == FilterMode.POPULAR ? 0xFFFFFFFF : 0xFF000000);

        btnAll.setBackgroundResource(
                currentFilter == FilterMode.ALL ? R.drawable.button_style1 : R.drawable.button_style2);
        btnAll.setTextColor(
                currentFilter == FilterMode.ALL ? 0xFFFFFFFF : 0xFF000000);

        btnFriends.setBackgroundResource(
                currentFilter == FilterMode.FRIENDS ? R.drawable.button_style1 : R.drawable.button_style2);
        btnFriends.setTextColor(
                currentFilter == FilterMode.FRIENDS ? 0xFFFFFFFF : 0xFF000000);
    }

    /**
     * 검색 기능 설정
     */
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
     * 필터 적용
     */
    private void applyFilter() {
        List<CommunityPostDTO> filtered = new ArrayList<>();

        // 1단계: 필터 모드 적용
        for (CommunityPostDTO post : allPosts) {
            boolean matchFilter = false;

            switch (currentFilter) {
                case POPULAR:
                    // 인기순: 모든 게시글 포함 (본인 포함)
                    matchFilter = true;
                    break;
                case ALL:
                    // 전체보기: 모든 게시글 포함 (본인 포함)
                    matchFilter = true;
                    break;
                case FRIENDS:
                    // 친구: 친구 게시글만 (본인 포함 가능)
                    matchFilter = post.isFriend();
                    break;
            }

            if (matchFilter) {
                filtered.add(post);
            }
        }

        // 2단계: 검색어 필터링 (제목 또는 지역 태그)
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
            Collections.sort(filtered, new Comparator<CommunityPostDTO>() {
                @Override
                public int compare(CommunityPostDTO o1, CommunityPostDTO o2) {
                    return Integer.compare(o2.getHeartCount(), o1.getHeartCount());
                }
            });
        } else {
            // 전체보기/친구: 최신순 (createdAt 내림차순)
            Collections.sort(filtered, new Comparator<CommunityPostDTO>() {
                @Override
                public int compare(CommunityPostDTO o1, CommunityPostDTO o2) {
                    return Long.compare(o2.getCreatedAt(), o1.getCreatedAt());
                }
            });
        }

        // 4단계: 페이지네이션 적용
        List<CommunityPostDTO> pagedPosts = getPagedPosts(filtered, 0);

        // 어댑터 업데이트
        adapter.updateData(pagedPosts);

        // 마지막 페이지 체크
        isLastPage = (PAGE_SIZE >= filtered.size());
    }

    /**
     * 페이지별 게시글 가져오기
     */
    private List<CommunityPostDTO> getPagedPosts(List<CommunityPostDTO> source, int page) {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, source.size());

        if (start >= source.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(source.subList(start, end));
    }

    /**
     * 추가 게시글 로드 (무한 스크롤)
     */
    private void loadMorePosts() {
        isLoading = true;

        // 로딩 시뮬레이션 (실제로는 서버에서 데이터 가져옴)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                currentPage++;

                // 현재 필터링된 전체 리스트 가져오기
                List<CommunityPostDTO> filtered = getCurrentFilteredList();

                // 다음 페이지 데이터
                List<CommunityPostDTO> nextPage = getPagedPosts(filtered, currentPage);

                if (nextPage.isEmpty()) {
                    isLastPage = true;
                } else {
                    adapter.addData(nextPage);
                }

                isLoading = false;
            }
        }, 500); // 0.5초 딜레이
    }

    /**
     * 현재 필터 조건에 맞는 전체 리스트 반환
     */
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

        // 검색어 필터링
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

        // 정렬
        if (currentFilter == FilterMode.POPULAR) {
            Collections.sort(filtered, new Comparator<CommunityPostDTO>() {
                @Override
                public int compare(CommunityPostDTO o1, CommunityPostDTO o2) {
                    return Integer.compare(o2.getHeartCount(), o1.getHeartCount());
                }
            });
        } else {
            Collections.sort(filtered, new Comparator<CommunityPostDTO>() {
                @Override
                public int compare(CommunityPostDTO o1, CommunityPostDTO o2) {
                    return Long.compare(o2.getCreatedAt(), o1.getCreatedAt());
                }
            });
        }

        return filtered;
    }

    /**
     * 페이지네이션 초기화
     */
    private void resetPagination() {
        currentPage = 0;
        isLoading = false;
        isLastPage = false;
    }

    /**
     * 더미 데이터 로드
     */
    private void loadDummyData() {
        allPosts = new ArrayList<>();
        long now = System.currentTimeMillis();

        // 더미 게시글 30개 생성
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
            // 좋아요 수를 다양하게 (일부는 만 단위, 일부는 천 단위)
            int heartCount;
            if (i % 5 == 0) {
                heartCount = 109000 + (int) (Math.random() * 10000); // 10.9만 ~ 11.9만
            } else if (i % 3 == 0) {
                heartCount = 5000 + (int) (Math.random() * 5000);    // 5천 ~ 1만
            } else {
                heartCount = (int) (Math.random() * 1000);            // 0 ~ 1000
            }

            allPosts.add(new CommunityPostDTO(
                    "post_" + i,
                    "user_" + (i % 5),
                    names[i % names.length],
                    profiles[i % profiles.length],
                    titles[i % titles.length],
                    images[i % images.length],
                    heartCount,
                    (int) (Math.random() * 100),  // 댓글 수
                    regions[i % regions.length],
                    provinceCodes[i % provinceCodes.length],
                    cityCodes[i % cityCodes.length],
                    now - (i * 1000000),
                    i % 3 == 0  // 친구 여부
            ));
        }
    }
}