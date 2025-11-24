package com.example.tot.Community;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tot.Album.AlbumDTO;
import com.example.tot.Follow.FollowButtonHelper;
import com.example.tot.R;
import com.example.tot.Schedule.ScheduleSetting.ScheduleItemAdapter;
import com.example.tot.Schedule.ScheduleSetting.ScheduleItemDTO;
import com.example.tot.Schedule.ScheduleSetting.ScheduleSettingActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "PostDetailActivity";

    private ImageView btnBack, btnComment, btnHeart, btnEdit, btnDelete;
    private ImageView imgThumbnail;
    private TextView tvLocation, tvDateRange, tvPhotoIndicator, tvPostTitle;
    private LinearLayout layoutDayButtons;
    private View viewDayIndicator;
    private ViewPager2 viewpagerPhotos;
    private RecyclerView rvScheduleItems;

    private CircleImageView imgAuthorProfile;
    private TextView tvAuthorName, tvInvitedCount, btnFollowAuthor;

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private CollectionReference communityPostsRef;

    private String scheduleId, authorUid, currentUid, postId;
    private List<String> dateList = new ArrayList<>();
    private List<AlbumDTO> currentPhotos = new ArrayList<>();
    private List<ScheduleItemDTO> currentScheduleItems = new ArrayList<>();

    private int selectedDayIndex = 0;
    private boolean isLiked = false;
    private boolean isAuthor = false;
    private int currentHeartCount = 0;

    private boolean isFollowing = false;
    private boolean isFollower = false;

    private ListenerRegistration postListener;

    // ✅ 댓글창 자동 열기 플래그
    private boolean shouldOpenComments = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        communityPostsRef =
                db.collection("public")
                        .document("community")
                        .collection("posts");

        currentUid = FirebaseAuth.getInstance().getUid();

        scheduleId = getIntent().getStringExtra("scheduleId");
        authorUid = getIntent().getStringExtra("authorUid");
        postId = getIntent().getStringExtra("postId");

        // ✅ 댓글창 자동 열기 플래그 확인
        shouldOpenComments = getIntent().getBooleanExtra("openComments", false);

        initViews();
        setupMap();
        loadPostData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnComment = findViewById(R.id.btn_comment);
        btnHeart = findViewById(R.id.btn_heart);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        imgThumbnail = findViewById(R.id.img_thumbnail);
        tvLocation = findViewById(R.id.tv_location);
        tvDateRange = findViewById(R.id.tv_date_range);
        tvPostTitle = findViewById(R.id.tv_post_title);

        layoutDayButtons = findViewById(R.id.layout_day_buttons);
        viewDayIndicator = findViewById(R.id.view_day_indicator);
        viewpagerPhotos = findViewById(R.id.viewpager_photos);
        tvPhotoIndicator = findViewById(R.id.tv_photo_indicator);
        rvScheduleItems = findViewById(R.id.rv_schedule_items);

        View layoutAuthor = findViewById(R.id.layout_author);
        imgAuthorProfile = layoutAuthor.findViewById(R.id.img_profile);
        tvAuthorName = layoutAuthor.findViewById(R.id.tv_user_name);
        tvInvitedCount = layoutAuthor.findViewById(R.id.tv_nickname);
        btnFollowAuthor = layoutAuthor.findViewById(R.id.btn_follow);

        layoutAuthor.findViewById(R.id.tv_follow_back).setVisibility(View.GONE);
        layoutAuthor.findViewById(R.id.btn_edit_nickname).setVisibility(View.GONE);
        layoutAuthor.findViewById(R.id.layout_nickname_edit).setVisibility(View.GONE);
        layoutAuthor.findViewById(R.id.btn_menu).setVisibility(View.GONE);

        btnBack.setOnClickListener(v -> finish());

        // ✅ 댓글 버튼 - 바텀시트 열기
        btnComment.setOnClickListener(v -> openCommentsBottomSheet());

        btnHeart.setOnClickListener(v -> toggleHeart());
        btnEdit.setOnClickListener(v -> editPost());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        rvScheduleItems.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * ✅ 댓글 바텀시트 열기 (공통 메서드)
     */
    private void openCommentsBottomSheet() {
        if (postId != null) {
            CommentsBottomSheetFragment bottomSheet = CommentsBottomSheetFragment.newInstance(postId);
            bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
            Log.d(TAG, "✅ 댓글 바텀시트 열기: " + postId);
        } else {
            Toast.makeText(this, "게시글 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void loadPostData() {
        isAuthor = currentUid != null && currentUid.equals(authorUid);

        btnEdit.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isAuthor ? View.VISIBLE : View.GONE);

        if (isAuthor) {
            btnFollowAuthor.setVisibility(View.GONE);
        } else {
            btnFollowAuthor.setVisibility(View.VISIBLE);
            checkFollowStatus();
        }

        if (postId == null) {
            communityPostsRef
                    .whereEqualTo("scheduleId", scheduleId)
                    .whereEqualTo("authorUid", authorUid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            postId = querySnapshot.getDocuments().get(0).getId();
                            loadPostDetails();
                            listenToPostChanges();
                        }
                    });
        } else {
            loadPostDetails();
            listenToPostChanges();
        }

        loadAuthorInfo();
        loadInvitedCount();
    }

    private void listenToPostChanges() {
        if (postListener != null) postListener.remove();

        postListener = communityPostsRef.document(postId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "게시글 리스너 오류", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Long heartCount = snapshot.getLong("heartCount");
                        if (heartCount != null) currentHeartCount = heartCount.intValue();

                        checkUserLikeStatus();

                        String title = snapshot.getString("title");
                        if (title != null) tvPostTitle.setText(title);

                        if (selectedDayIndex >= 0 && selectedDayIndex < dateList.size()) {
                            loadDayDataFromPublic(selectedDayIndex);
                        }
                    }
                });
    }

    private void checkUserLikeStatus() {
        if (currentUid == null || postId == null) return;

        communityPostsRef.document(postId)
                .collection("likes")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    isLiked = doc.exists();
                    updateHeartUI();
                });
    }

    private void loadPostDetails() {
        communityPostsRef.document(postId)
                .get()
                .addOnSuccessListener(postDoc -> {
                    if (!postDoc.exists()) return;

                    String postTitle = postDoc.getString("title");
                    String locationName = postDoc.getString("locationName");
                    Long startDateLong = postDoc.getLong("startDate");
                    Long endDateLong = postDoc.getLong("endDate");
                    Long heartCount = postDoc.getLong("heartCount");
                    String thumbnailUrl = postDoc.getString("thumbnailUrl");

                    if (postTitle != null) tvPostTitle.setText(postTitle);
                    if (locationName != null) tvLocation.setText(locationName);

                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        imgThumbnail.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(Uri.parse(thumbnailUrl))
                                .into(imgThumbnail);
                    } else {
                        imgThumbnail.setVisibility(View.GONE);
                    }

                    if (heartCount != null) currentHeartCount = heartCount.intValue();

                    checkUserLikeStatus();

                    if (startDateLong != null && endDateLong != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                        tvDateRange.setText(
                                sdf.format(new Date(startDateLong)) + "~" +
                                        sdf.format(new Date(endDateLong))
                        );

                        generateDateList(new Date(startDateLong), new Date(endDateLong));
                        setupDayButtons();
                        loadDayDataFromPublic(0);
                    }

                    // ✅ 데이터 로드 완료 후 댓글창 자동 열기
                    if (shouldOpenComments) {
                        // UI가 완전히 로드된 후 댓글창 열기 (500ms 딜레이)
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            openCommentsBottomSheet();
                            shouldOpenComments = false; // 한 번만 실행되도록
                        }, 500);
                    }
                });
    }

    private void editPost() {
        if (scheduleId == null || authorUid == null) {
            Toast.makeText(this, "스케줄 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("user")
                .document(authorUid)
                .collection("schedule")
                .document(scheduleId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long startDateLong = doc.getLong("startDate");
                        Long endDateLong = doc.getLong("endDate");

                        if (startDateLong != null && endDateLong != null) {
                            Intent intent = new Intent(
                                    PostDetailActivity.this,
                                    ScheduleSettingActivity.class
                            );

                            intent.putExtra("scheduleId", scheduleId);
                            intent.putExtra("startDate", startDateLong);
                            intent.putExtra("endDate", endDateLong);
                            intent.putExtra("fromPostEdit", true);
                            intent.putExtra("postId", postId);

                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "스케줄 날짜 정보가 없습니다", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "원본 스케줄을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "스케줄 조회 실패", e);
                    Toast.makeText(this, "스케줄을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("게시글 삭제")
                .setMessage("이 게시글을 삭제하시겠습니까?\n삭제된 게시글은 복구할 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> deletePost())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deletePost() {
        if (postId == null) {
            Toast.makeText(this, "게시글 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDelete.setEnabled(false);

        communityPostsRef.document(postId)
                .collection("scheduleDate")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int[] deleteCount = {querySnapshot.size()};

                    if (deleteCount[0] == 0) {
                        deleteLikesAndCommentsCollection(() -> {
                            communityPostsRef.document(postId).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "게시글이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                        });
                        return;
                    }

                    for (DocumentSnapshot dateDoc : querySnapshot.getDocuments()) {
                        String dateKey = dateDoc.getId();

                        communityPostsRef.document(postId)
                                .collection("scheduleDate")
                                .document(dateKey)
                                .collection("scheduleItem")
                                .get()
                                .addOnSuccessListener(items -> {
                                    for (DocumentSnapshot item : items.getDocuments()) {
                                        item.getReference().delete();
                                    }
                                });

                        communityPostsRef.document(postId)
                                .collection("scheduleDate")
                                .document(dateKey)
                                .collection("album")
                                .get()
                                .addOnSuccessListener(albums -> {
                                    for (DocumentSnapshot album : albums.getDocuments()) {
                                        album.getReference().delete();
                                    }
                                });

                        dateDoc.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    deleteCount[0]--;

                                    if (deleteCount[0] == 0) {
                                        deleteLikesAndCommentsCollection(() -> {
                                            communityPostsRef.document(postId).delete()
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        Toast.makeText(this, "게시글이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    });
                                        });
                                    }
                                });
                    }
                });
    }

    /**
     * ✅ 좋아요 + 댓글 컬렉션 삭제
     */
    private void deleteLikesAndCommentsCollection(Runnable onComplete) {
        final int[] collectionsToDelete = {2}; // likes, comments

        // 좋아요 삭제
        communityPostsRef.document(postId)
                .collection("likes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        collectionsToDelete[0]--;
                        if (collectionsToDelete[0] == 0) onComplete.run();
                        return;
                    }

                    int[] count = {snapshot.size()};
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    count[0]--;
                                    if (count[0] == 0) {
                                        collectionsToDelete[0]--;
                                        if (collectionsToDelete[0] == 0) onComplete.run();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    collectionsToDelete[0]--;
                    if (collectionsToDelete[0] == 0) onComplete.run();
                });

        // 댓글 삭제
        communityPostsRef.document(postId)
                .collection("comments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        collectionsToDelete[0]--;
                        if (collectionsToDelete[0] == 0) onComplete.run();
                        return;
                    }

                    int[] count = {snapshot.size()};
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    count[0]--;
                                    if (count[0] == 0) {
                                        collectionsToDelete[0]--;
                                        if (collectionsToDelete[0] == 0) onComplete.run();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    collectionsToDelete[0]--;
                    if (collectionsToDelete[0] == 0) onComplete.run();
                });
    }

    private void checkFollowStatus() {
        FollowButtonHelper.checkFollowStatus(authorUid, (following, follower) -> {
            isFollowing = following;
            isFollower = follower;

            FollowButtonHelper.updateFollowButton(btnFollowAuthor, isFollowing, isFollower);

            btnFollowAuthor.setOnClickListener(v -> {
                FollowButtonHelper.handleFollowButtonClick(
                        PostDetailActivity.this,
                        authorUid,
                        isFollowing,
                        isFollower,
                        new FollowButtonHelper.FollowActionCallback() {
                            @Override
                            public void onSuccess(boolean nowFollowing) {
                                isFollowing = nowFollowing;
                                FollowButtonHelper.updateFollowButton(
                                        btnFollowAuthor,
                                        isFollowing,
                                        isFollower
                                );
                            }

                            @Override
                            public void onFailure(String message) {
                                Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            });
        });
    }

    private void generateDateList(Date start, Date end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();

        cal.setTime(start);
        while (!cal.getTime().after(end)) {
            dateList.add(sdf.format(cal.getTime()));
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void setupDayButtons() {
        layoutDayButtons.removeAllViews();

        for (int i = 0; i < dateList.size(); i++) {
            TextView dayBtn = new TextView(this);
            dayBtn.setText((i + 1) + "일차");
            dayBtn.setTextSize(14);
            dayBtn.setTextColor(
                    i == 0 ? Color.parseColor("#575DFB") :
                            Color.parseColor("#666666")
            );
            dayBtn.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

            int dayIndex = i;
            dayBtn.setOnClickListener(v -> selectDay(dayIndex));

            layoutDayButtons.addView(dayBtn);
        }
    }

    private void selectDay(int dayIndex) {
        selectedDayIndex = dayIndex;

        for (int i = 0; i < layoutDayButtons.getChildCount(); i++) {
            TextView btn = (TextView) layoutDayButtons.getChildAt(i);
            btn.setTextColor(
                    i == dayIndex ? Color.parseColor("#575DFB") :
                            Color.parseColor("#666666")
            );
        }

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) viewDayIndicator.getLayoutParams();
        params.leftMargin = dpToPx(16 + dayIndex * 70);
        viewDayIndicator.setLayoutParams(params);

        loadDayDataFromPublic(dayIndex);
    }

    private void loadDayDataFromPublic(int dayIndex) {
        if (postId == null || dateList.isEmpty() || dayIndex >= dateList.size()) return;

        String dateKey = dateList.get(dayIndex);

        loadPhotosFromPublic(dateKey);
        loadScheduleItemsFromPublic(dateKey);
        loadMapMarkersFromPublic(dateKey);
    }

    private void loadPhotosFromPublic(String dateKey) {
        communityPostsRef.document(postId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album")
                .orderBy("index")
                .get()
                .addOnSuccessListener(snap -> {
                    currentPhotos.clear();
                    snap.forEach(doc -> currentPhotos.add(doc.toObject(AlbumDTO.class)));

                    PhotoPagerAdapter photoAdapter = new PhotoPagerAdapter(currentPhotos);
                    viewpagerPhotos.setAdapter(photoAdapter);

                    if (currentPhotos.isEmpty()) {
                        tvPhotoIndicator.setText("0 / 0");
                    } else {
                        tvPhotoIndicator.setText("1 / " + currentPhotos.size());

                        viewpagerPhotos.registerOnPageChangeCallback(
                                new ViewPager2.OnPageChangeCallback() {
                                    @Override
                                    public void onPageSelected(int position) {
                                        tvPhotoIndicator.setText(
                                                (position + 1) + " / " + currentPhotos.size()
                                        );
                                    }
                                }
                        );
                    }
                });
    }

    private void loadScheduleItemsFromPublic(String dateKey) {
        communityPostsRef.document(postId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem")
                .orderBy("startTime")
                .get()
                .addOnSuccessListener(snap -> {
                    currentScheduleItems.clear();
                    snap.forEach(doc -> currentScheduleItems.add(doc.toObject(ScheduleItemDTO.class)));

                    ScheduleItemAdapter adapter =
                            new ScheduleItemAdapter((item, docId) -> {});

                    adapter.submitList(
                            new ArrayList<>(currentScheduleItems),
                            new ArrayList<>()
                    );

                    adapter.setReadOnlyMode(true);

                    rvScheduleItems.setAdapter(adapter);
                });
    }

    /**
     * ✅ 지도 마커 + 폴리라인 추가
     */
    private void loadMapMarkersFromPublic(String dateKey) {
        if (mMap == null) return;

        mMap.clear();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        PolylineOptions polylineOptions = new PolylineOptions()
                .width(10)
                .color(Color.parseColor("#575DFB"));

        communityPostsRef.document(postId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem")
                .orderBy("startTime")
                .get()
                .addOnSuccessListener(snap -> {
                    List<LatLng> points = new ArrayList<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        GeoPoint geoPoint = doc.getGeoPoint("place");
                        if (geoPoint != null) {
                            LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            points.add(latLng);
                            builder.include(latLng);

                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(doc.getString("placeName"))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        }
                    }

                    // 폴리라인 추가
                    if (points.size() > 1) {
                        mMap.addPolyline(polylineOptions.addAll(points));
                    }

                    if (!points.isEmpty()) {
                        try {
                            if (points.size() > 1) {
                                LatLngBounds bounds = builder.build();
                                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                            } else {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15f));
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "지도 카메라 이동 실패", e);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 10f));
                        }
                    }
                });
    }

    private void loadAuthorInfo() {
        db.collection("user")
                .document(authorUid)
                .get()
                .addOnSuccessListener(doc -> {
                    String nickname = doc.getString("nickname");
                    String profileUrl = doc.getString("profileImageUrl");

                    tvAuthorName.setText(nickname != null ? nickname : "사용자");

                    if (profileUrl != null && !profileUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileUrl)
                                .placeholder(R.drawable.ic_profile_default)
                                .error(R.drawable.ic_profile_default)
                                .into(imgAuthorProfile);
                    } else {
                        imgAuthorProfile.setImageResource(R.drawable.ic_profile_default);
                    }
                });
    }

    private void loadInvitedCount() {
        db.collection("user")
                .document(authorUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("invited")
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap.size();
                    tvInvitedCount.setText(count > 0 ? "외 " + count + "명" : "");
                });
    }

    private void toggleHeart() {
        if (currentUid == null || postId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        btnHeart.setEnabled(false);

        if (isLiked) {
            communityPostsRef.document(postId)
                    .collection("likes")
                    .document(currentUid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        communityPostsRef.document(postId)
                                .update("heartCount", FieldValue.increment(-1))
                                .addOnSuccessListener(aVoid2 -> {
                                    isLiked = false;
                                    currentHeartCount--;
                                    updateHeartUI();
                                    btnHeart.setEnabled(true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "좋아요 취소 실패", Toast.LENGTH_SHORT).show();
                                    btnHeart.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "좋아요 취소 실패", Toast.LENGTH_SHORT).show();
                        btnHeart.setEnabled(true);
                    });
        } else {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("userId", currentUid);
            likeData.put("timestamp", System.currentTimeMillis());

            communityPostsRef.document(postId)
                    .collection("likes")
                    .document(currentUid)
                    .set(likeData)
                    .addOnSuccessListener(aVoid -> {
                        communityPostsRef.document(postId)
                                .update("heartCount", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid2 -> {
                                    isLiked = true;
                                    currentHeartCount++;
                                    updateHeartUI();
                                    btnHeart.setEnabled(true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "좋아요 실패", Toast.LENGTH_SHORT).show();
                                    btnHeart.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "좋아요 실패", Toast.LENGTH_SHORT).show();
                        btnHeart.setEnabled(true);
                    });
        }
    }

    private void updateHeartUI() {
        if (isLiked) {
            btnHeart.setImageResource(R.drawable.ic_heart_c);
        } else {
            btnHeart.setImageResource(R.drawable.ic_heart);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (postListener != null) {
            postListener.remove();
        }
    }

    class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

        private List<AlbumDTO> photos;

        PhotoPagerAdapter(List<AlbumDTO> photos) {
            this.photos = photos;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post_photo, parent, false);

            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull PhotoViewHolder holder,
                int position
        ) {
            AlbumDTO photo = photos.get(position);

            Glide.with(PostDetailActivity.this)
                    .load(photo.getImageUrl())
                    .centerCrop()
                    .into(holder.imgPhoto);
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        class PhotoViewHolder extends RecyclerView.ViewHolder {

            ImageView imgPhoto;

            PhotoViewHolder(View itemView) {
                super(itemView);
                imgPhoto = itemView.findViewById(R.id.img_post_photo);
            }
        }
    }
}