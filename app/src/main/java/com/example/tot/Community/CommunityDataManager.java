package com.example.tot.Community;

import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 데이터 중앙 관리 클래스
 * 홈 화면과 커뮤니티 화면에서 데이터 공유하여 중복 로드 방지
 */
public class CommunityDataManager {

    private static final String TAG = "CommunityDataManager";
    private static CommunityDataManager instance;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private List<CommunityPostDTO> cachedPosts = new ArrayList<>();
    private long lastLoadTime = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5분

    private List<DataUpdateListener> listeners = new ArrayList<>();
    private boolean isLoading = false;

    public interface DataUpdateListener {
        void onDataUpdated(List<CommunityPostDTO> posts);
        void onDataLoadFailed(String error);
    }

    private CommunityDataManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static CommunityDataManager getInstance() {
        if (instance == null) {
            instance = new CommunityDataManager();
        }
        return instance;
    }

    /**
     * 리스너 등록
     */
    public void addListener(DataUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "✅ 리스너 등록: " + listeners.size() + "개");
        }
    }

    /**
     * 리스너 제거
     */
    public void removeListener(DataUpdateListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "✅ 리스너 해제: " + listeners.size() + "개 남음");
    }

    /**
     * 캐시된 데이터 반환 (필요시 자동 갱신)
     */
    public void getPosts(boolean forceRefresh) {
        long currentTime = System.currentTimeMillis();
        boolean cacheExpired = (currentTime - lastLoadTime) > CACHE_DURATION;

        if (forceRefresh || cacheExpired || cachedPosts.isEmpty()) {
            loadPostsFromFirestore();
        } else {
            // 캐시된 데이터 즉시 반환
            notifyListeners(new ArrayList<>(cachedPosts));
            Log.d(TAG, "📦 캐시된 데이터 사용: " + cachedPosts.size() + "개");
        }
    }

    /**
     * Firestore에서 게시글 로드 (썸네일 이미지 포함)
     */
    private void loadPostsFromFirestore() {
        if (isLoading) {
            Log.d(TAG, "⏳ 이미 로딩 중...");
            return;
        }

        if (auth.getCurrentUser() == null) {
            notifyListenersError("로그인이 필요합니다");
            return;
        }

        isLoading = true;
        Log.d(TAG, "🔄 Firestore에서 게시글 로드 시작");

        db.collection("public")
                .document("community")
                .collection("posts")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
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

                    // 작성자 정보 및 썸네일 이미지 로드
                    loadPostDetails(tempPosts, authorUidMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 게시글 로드 실패", e);
                    isLoading = false;
                    notifyListenersError("게시글을 불러올 수 없습니다");
                });
    }

    /**
     * 게시글 상세 정보 로드 (작성자 정보, 좋아요 상태, 썸네일 이미지)
     */
    private void loadPostDetails(List<CommunityPostDTO> posts, Map<String, String> authorUidMap) {
        if (posts.isEmpty()) {
            cachedPosts = new ArrayList<>();
            lastLoadTime = System.currentTimeMillis();
            isLoading = false;
            notifyListeners(cachedPosts);
            return;
        }

        final int[] loadedCount = {0};
        final int totalCount = posts.size();
        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        for (CommunityPostDTO post : posts) {
            String authorUid = authorUidMap.get(post.getPostId());

            if (authorUid == null) {
                loadedCount[0]++;
                checkLoadComplete(loadedCount[0], totalCount, posts);
                continue;
            }

            // 좋아요 상태 확인
            if (currentUid != null) {
                db.collection("public")
                        .document("community")
                        .collection("posts")
                        .document(post.getPostId())
                        .collection("likes")
                        .document(currentUid)
                        .get()
                        .addOnSuccessListener(likeDoc -> {
                            post.setLiked(likeDoc.exists());
                        });
            }

            // 작성자 정보 로드
            db.collection("user").document(authorUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String nickname = userDoc.getString("nickname");
                            String profileImageUrl = userDoc.getString("profileImageUrl");

                            post.setUserName(nickname != null ? nickname : "사용자");
                            post.setProfileImageUrl(profileImageUrl);
                        }

                        // ✅ 썸네일 이미지 로드 (앨범의 첫 번째 사진)
                        loadThumbnailImage(post, () -> {
                            loadedCount[0]++;
                            checkLoadComplete(loadedCount[0], totalCount, posts);
                        });
                    })
                    .addOnFailureListener(e -> {
                        loadedCount[0]++;
                        checkLoadComplete(loadedCount[0], totalCount, posts);
                    });
        }
    }

    /**
     * ✅ 게시글 썸네일 이미지 로드 (앨범의 첫 번째 사진)
     */
    private void loadThumbnailImage(CommunityPostDTO post, Runnable onComplete) {
        String postId = post.getPostId();
        if (postId == null || postId.isEmpty()) {
            onComplete.run();
            return;
        }

        // scheduleDate 컬렉션의 모든 날짜 조회
        db.collection("public")
                .document("community")
                .collection("posts")
                .document(postId)
                .collection("scheduleDate")
                .orderBy("date")
                .limit(1)  // 첫 번째 날짜만
                .get()
                .addOnSuccessListener(dateSnapshot -> {
                    if (dateSnapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    String firstDateKey = dateSnapshot.getDocuments().get(0).getId();

                    // 첫 번째 날짜의 앨범에서 첫 번째 사진 조회
                    db.collection("public")
                            .document("community")
                            .collection("posts")
                            .document(postId)
                            .collection("scheduleDate")
                            .document(firstDateKey)
                            .collection("album")
                            .orderBy("index")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(albumSnapshot -> {
                                if (!albumSnapshot.isEmpty()) {
                                    DocumentSnapshot firstPhoto = albumSnapshot.getDocuments().get(0);
                                    String imageUrl = firstPhoto.getString("imageUrl");
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        post.setThumbnailUrl(imageUrl);
                                        Log.d(TAG, "✅ 썸네일 로드 완료: " + postId);
                                    }
                                }
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 앨범 조회 실패: " + postId, e);
                                onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 날짜 조회 실패: " + postId, e);
                    onComplete.run();
                });
    }

    /**
     * 로딩 완료 확인 및 캐시 업데이트
     */
    private void checkLoadComplete(int loadedCount, int totalCount, List<CommunityPostDTO> posts) {
        if (loadedCount == totalCount) {
            cachedPosts = new ArrayList<>(posts);
            lastLoadTime = System.currentTimeMillis();
            isLoading = false;

            Log.d(TAG, "✅ 게시글 로드 완료: " + cachedPosts.size() + "개");
            notifyListeners(new ArrayList<>(cachedPosts));
        }
    }

    /**
     * 리스너들에게 데이터 업데이트 알림
     */
    private void notifyListeners(List<CommunityPostDTO> posts) {
        for (DataUpdateListener listener : listeners) {
            listener.onDataUpdated(posts);
        }
    }

    /**
     * 리스너들에게 에러 알림
     */
    private void notifyListenersError(String error) {
        for (DataUpdateListener listener : listeners) {
            listener.onDataLoadFailed(error);
        }
    }

    /**
     * 캐시 강제 갱신
     */
    public void refresh() {
        getPosts(true);
    }

    /**
     * 캐시 초기화
     */
    public void clearCache() {
        cachedPosts.clear();
        lastLoadTime = 0;
        Log.d(TAG, "🗑️ 캐시 초기화");
    }
}