package com.example.tot.Community;

import android.os.Handler;
import android.os.Looper;

import com.example.tot.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 커뮤니티 비즈니스 로직 관리 클래스
 * 경로: java/com/example/tot/Community/CommunityViewModel.java
 *
 * 역할: 필터링, 검색, 정렬, 페이지네이션 로직 담당
 */
public class CommunityViewModel {

    // 필터 모드
    public enum FilterMode {
        POPULAR,   // 인기순
        ALL,       // 전체보기 (최신순)
        FRIENDS    // 친구
    }

    // 데이터
    private List<CommunityPostDTO> allPosts;

    // 필터 상태
    private FilterMode currentFilter = FilterMode.ALL;
    private String searchQuery = "";

    // 페이지네이션
    private static final int PAGE_SIZE = 20;  // 한 번에 20개씩 로드
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // 검색 디바운싱
    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY = 300;

    // 콜백 인터페이스
    public interface DataCallback {
        void onDataChanged(List<CommunityPostDTO> posts);
        void onDataAdded(List<CommunityPostDTO> posts);
    }

    private DataCallback callback;

    public CommunityViewModel(DataCallback callback) {
        this.callback = callback;
        this.searchHandler = new Handler(Looper.getMainLooper());
        this.allPosts = new ArrayList<>();
    }

    /**
     * 더미 데이터 로드
     */
    public void loadDummyData() {
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
                "순천 순천만습지", "진주 진주성", "창원 진해 벚꽃",
                "제주 한라산 등반", "강화도 역사 투어", "양평 두물머리",
                "남이섬 단풍 구경", "설악산 케이블카", "부여 백제 문화",
                "공주 공산성", "익산 미륵사지", "정읍 내장산",
                "고창 선운사", "함평 나비축제", "무안 연꽃 단지",
                "영광 백수해안도로", "김제 지평선축제", "군산 근대문화유산",
                "전주 야시장", "남원 광한루원", "구례 화엄사",
                "여수 엑스포", "순천 낙안읍성", "광양 매화마을",
                "하동 섬진강", "사천 케이블카", "진주 유등축제",
                "고성 공룡박물관", "남해 독일마을", "거제 외도보타니아"
        };

        String[] names = {"박민주", "김서연", "이준호", "최유진", "정민수", "한지우", "송하늘", "강민지"};
        int[] profiles = {R.drawable.sample1, R.drawable.sample2, R.drawable.sample3, R.drawable.sample4};
        int[] images = {R.drawable.sample1, R.drawable.sample2, R.drawable.sample3, R.drawable.sample4};
        String[] regions = {"서울", "부산", "제주", "전주", "강릉", "여수", "경주", "속초", "대구", "광주"};
        String[] provinceCodes = {"11", "26", "49", "46", "42", "45", "47", "42", "27", "29"};
        String[] cityCodes = {"11680", "26350", "50110", "45110", "42150", "45110", "47130", "42210", "27200", "29200"};

        // 50개의 더미 데이터 생성 (스크롤 테스트용)
        for (int i = 0; i < 50; i++) {
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
                    "user_" + (i % names.length),
                    names[i % names.length],
                    profiles[i % profiles.length],
                    titles[i % titles.length],
                    images[i % images.length],
                    heartCount,
                    (int) (Math.random() * 100),
                    regions[i % regions.length],
                    provinceCodes[i % provinceCodes.length],
                    cityCodes[i % cityCodes.length],
                    now - (i * 1000000L),
                    i % 3 == 0
            ));
        }
    }

    /**
     * 전체 게시글 반환 (홈화면 지역 필터용)
     */
    public List<CommunityPostDTO> getAllPosts() {
        return new ArrayList<>(allPosts);
    }

    /**
     * 필터 변경
     */
    public void setFilter(FilterMode filter) {
        if (currentFilter != filter) {
            currentFilter = filter;
            resetPagination();
            applyFilter();
        }
    }

    /**
     * 검색어 변경 (디바운싱 적용)
     */
    public void setSearchQuery(String query) {
        // 이전 검색 작업 취소
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        // 새로운 검색 작업 예약
        searchRunnable = new Runnable() {
            @Override
            public void run() {
                searchQuery = query != null ? query.trim() : "";
                resetPagination();
                applyFilter();
            }
        };

        searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
    }

    /**
     * 필터 적용
     */
    public void applyFilter() {
        List<CommunityPostDTO> filtered = filterAndSort(allPosts);
        List<CommunityPostDTO> pagedPosts = getPagedPosts(filtered, 0);

        if (callback != null) {
            callback.onDataChanged(pagedPosts);
        }

        isLastPage = (PAGE_SIZE >= filtered.size());
    }

    /**
     * 더 많은 게시글 로드 (무한 스크롤)
     */
    public void loadMorePosts() {
        if (isLoading || isLastPage) {
            return;
        }

        isLoading = true;

        // 로딩 시뮬레이션 (0.5초 딜레이)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                currentPage++;

                List<CommunityPostDTO> filtered = filterAndSort(allPosts);
                List<CommunityPostDTO> nextPage = getPagedPosts(filtered, currentPage);

                if (nextPage.isEmpty()) {
                    isLastPage = true;
                } else {
                    if (callback != null) {
                        callback.onDataAdded(nextPage);
                    }
                }

                isLoading = false;
            }
        }, 500);
    }

    /**
     * 필터링 및 정렬
     */
    private List<CommunityPostDTO> filterAndSort(List<CommunityPostDTO> posts) {
        if (posts == null || posts.isEmpty()) {
            return new ArrayList<>();
        }

        List<CommunityPostDTO> filtered = new ArrayList<>();

        // 1단계: 필터 모드 적용
        for (CommunityPostDTO post : posts) {
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
            String lowerQuery = searchQuery.toLowerCase();

            for (CommunityPostDTO post : filtered) {
                String title = post.getTitle() != null ? post.getTitle().toLowerCase() : "";
                String region = post.getRegionTag() != null ? post.getRegionTag().toLowerCase() : "";

                if (title.contains(lowerQuery) || region.contains(lowerQuery)) {
                    searchFiltered.add(post);
                }
            }
            filtered = searchFiltered;
        }

        // 3단계: 정렬
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
     * 페이지별 게시글 가져오기
     */
    private List<CommunityPostDTO> getPagedPosts(List<CommunityPostDTO> source, int page) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, source.size());

        if (start >= source.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(source.subList(start, end));
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
     * 현재 필터 모드 반환
     */
    public FilterMode getCurrentFilter() {
        return currentFilter;
    }

    /**
     * 로딩 중 여부
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * 마지막 페이지 여부
     */
    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * 페이지 크기 반환
     */
    public int getPageSize() {
        return PAGE_SIZE;
    }

    /**
     * 리소스 정리
     */
    public void destroy() {
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchHandler = null;
        searchRunnable = null;
        callback = null;
        allPosts = null;
    }
}