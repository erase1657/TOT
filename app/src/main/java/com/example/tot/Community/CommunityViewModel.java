package com.example.tot.Community;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 커뮤니티 비즈니스 로직 관리 클래스
 * 경로: java/com/example/tot/Community/CommunityViewModel.java
 *
 * 역할: 필터링, 검색, 정렬, 페이지네이션 로직 담당
 * ✅ 더미 데이터 제거 완료 - Firestore 기반으로 동작
 */
public class CommunityViewModel {

    // 필터 모드
    public enum FilterMode {
        POPULAR,   // 인기순
        ALL,       // 전체보기 (최신순)
        FRIENDS    // 친구 (팔로워 + 팔로잉 + 본인)
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
     * ✅ 더미 데이터 로드 메서드 제거됨
     * Firestore에서 직접 로드하도록 변경
     */

    /**
     * 전체 게시글 반환 (홈화면 지역 필터용)
     */
    public List<CommunityPostDTO> getAllPosts() {
        return new ArrayList<>(allPosts);
    }

    /**
     * ✅ 외부에서 Firestore 데이터를 주입하는 메서드 추가
     */
    public void setAllPosts(List<CommunityPostDTO> posts) {
        this.allPosts = posts != null ? new ArrayList<>(posts) : new ArrayList<>();
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
                    // ✅ 친구 필터: 팔로워, 팔로잉, 본인의 게시물
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