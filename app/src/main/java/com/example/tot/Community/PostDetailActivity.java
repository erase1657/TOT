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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "PostDetailActivity";

    private com.google.android.flexbox.FlexboxLayout layoutRegionTags;
    private ImageView btnBack, btnComment, btnDelete;
    private ImageView btnBottomHeart;
    private TextView tvBottomHeartCount;
    private Button btnCopySchedule;
    private ImageView imgThumbnail;
    private TextView tvPostTitle, tvDateRange, tvPhotoIndicator;
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
    private ListenerRegistration likeListener;

    private boolean shouldOpenComments = false;

    private long originalStartDate = 0;
    private long originalEndDate = 0;
    private String locationName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        communityPostsRef = db.collection("public")
                .document("community")
                .collection("posts");

        currentUid = FirebaseAuth.getInstance().getUid();

        scheduleId = getIntent().getStringExtra("scheduleId");
        authorUid = getIntent().getStringExtra("authorUid");
        postId = getIntent().getStringExtra("postId");
        shouldOpenComments = getIntent().getBooleanExtra("openComments", false);

        initViews();
        setupMap();
        loadPostData();

        View rootView = findViewById(android.R.id.content);
        View bottomBar = findViewById(R.id.layout_bottom_fixed);
        View appBar = findViewById(R.id.app_bar);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int insetTop = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
            ).top;
            // 네비게이션 바 높이 구하기
            int insetBottom = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
            ).bottom;
            appBar.setPadding(
                    appBar.getPaddingLeft(),
                    insetTop,
                    appBar.getPaddingRight(),
                    appBar.getPaddingBottom()
            );
            // 기존 padding 유지 + inset 추가
            bottomBar.setPadding(
                    bottomBar.getPaddingLeft(),
                    bottomBar.getPaddingTop(),
                    bottomBar.getPaddingRight(),
                    insetBottom
            );

            return insets;
        });
    }

    private void initViews() {
        layoutRegionTags = findViewById(R.id.layout_region_tags);
        btnBack = findViewById(R.id.btn_back);
        btnComment = findViewById(R.id.btn_comment);
        btnDelete = findViewById(R.id.btn_delete);
        imgThumbnail = findViewById(R.id.img_thumbnail);
        tvPostTitle = findViewById(R.id.tv_post_title);
        tvDateRange = findViewById(R.id.tv_date_range);

        layoutDayButtons = findViewById(R.id.layout_day_buttons);
        viewDayIndicator = findViewById(R.id.view_day_indicator);
        viewpagerPhotos = findViewById(R.id.viewpager_photos);
        tvPhotoIndicator = findViewById(R.id.tv_photo_indicator);
        rvScheduleItems = findViewById(R.id.rv_schedule_items);

        btnBottomHeart = findViewById(R.id.btn_bottom_heart);
        tvBottomHeartCount = findViewById(R.id.tv_bottom_heart_count);
        btnCopySchedule = findViewById(R.id.btn_copy_schedule);

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
        btnComment.setOnClickListener(v -> openCommentsBottomSheet());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        btnBottomHeart.setOnClickListener(v -> toggleHeart());
        btnCopySchedule.setOnClickListener(v -> showCopyScheduleDialog());

        rvScheduleItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void openCommentsBottomSheet() {
        if (postId != null) {
            CommentsBottomSheetFragment bottomSheet = CommentsBottomSheetFragment.newInstance(postId);
            bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
        } else {
            Toast.makeText(this, "게시글 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void loadPostData() {
        isAuthor = currentUid != null && currentUid.equals(authorUid);

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
                            listenToLikeStatus();
                        }
                    });
        } else {
            loadPostDetails();
            listenToPostChanges();
            listenToLikeStatus();
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
                        if (heartCount != null) {
                            currentHeartCount = heartCount.intValue();
                            tvBottomHeartCount.setText(String.valueOf(currentHeartCount));
                        }

                        String title = snapshot.getString("title");
                        if (title != null) tvPostTitle.setText(title);

                        if (selectedDayIndex >= 0 && selectedDayIndex < dateList.size()) {
                            loadDayDataFromPublic(selectedDayIndex);
                        }
                    }
                });
    }

    private void listenToLikeStatus() {
        if (currentUid == null || postId == null) return;

        if (likeListener != null) likeListener.remove();

        likeListener = communityPostsRef.document(postId)
                .collection("likes")
                .document(currentUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "좋아요 상태 리스너 오류", error);
                        return;
                    }

                    isLiked = snapshot != null && snapshot.exists();
                    updateHeartUI();
                });
    }

    private void loadPostDetails() {
        communityPostsRef.document(postId)
                .get()
                .addOnSuccessListener(postDoc -> {
                    if (!postDoc.exists()) return;

                    String postTitle = postDoc.getString("title");
                    locationName = postDoc.getString("locationName");
                    Long startDateLong = postDoc.getLong("startDate");
                    Long endDateLong = postDoc.getLong("endDate");
                    Long heartCount = postDoc.getLong("heartCount");

                    // ✅ 썸네일 로드 개선
                    String thumbnailUrl = postDoc.getString("thumbnailUrl");
                    Log.d(TAG, "📸 게시글 썸네일 URL: " + thumbnailUrl);

                    if (postTitle != null) tvPostTitle.setText(postTitle);

                    // ✅ 썸네일 표시 로직 개선
                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        imgThumbnail.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(thumbnailUrl)
                                .placeholder(R.drawable.sample3)
                                .error(R.drawable.sample3)
                                .centerCrop()
                                .into(imgThumbnail);
                        Log.d(TAG, "✅ 썸네일 이미지 로드 완료");
                    } else {
                        // 썸네일이 없으면 스케줄의 첫 번째 사진을 로드
                        Log.d(TAG, "⚠️ 썸네일 없음, 첫 번째 앨범 사진 시도");
                        loadFirstAlbumPhotoAsThumbnail();
                    }

                    if (heartCount != null) {
                        currentHeartCount = heartCount.intValue();
                        tvBottomHeartCount.setText(String.valueOf(currentHeartCount));
                    }

                    displayRegionTags(postDoc);

                    if (startDateLong != null && endDateLong != null) {
                        originalStartDate = startDateLong;
                        originalEndDate = endDateLong;

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                        tvDateRange.setText(
                                sdf.format(new Date(startDateLong)) + "~" +
                                        sdf.format(new Date(endDateLong))
                        );

                        generateDateList(new Date(startDateLong), new Date(endDateLong));
                        setupDayButtons();
                        loadDayDataFromPublic(0);
                    }

                    if (shouldOpenComments) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            openCommentsBottomSheet();
                            shouldOpenComments = false;
                        }, 500);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 게시글 로드 실패", e);
                    Toast.makeText(this, "게시글을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
    }
    // ✅ 썸네일이 없을 때 첫 번째 앨범 사진을 로드하는 메서드 추가
    private void loadFirstAlbumPhotoAsThumbnail() {
        if (dateList.isEmpty()) {
            imgThumbnail.setVisibility(View.GONE);
            return;
        }

        String firstDateKey = dateList.get(0);

        communityPostsRef.document(postId)
                .collection("scheduleDate")
                .document(firstDateKey)
                .collection("album")
                .orderBy("index")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        String imageUrl = snapshot.getDocuments().get(0).getString("imageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            imgThumbnail.setVisibility(View.VISIBLE);
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.sample3)
                                    .error(R.drawable.sample3)
                                    .centerCrop()
                                    .into(imgThumbnail);
                            Log.d(TAG, "✅ 첫 번째 앨범 사진을 썸네일로 사용");
                        } else {
                            imgThumbnail.setVisibility(View.GONE);
                        }
                    } else {
                        imgThumbnail.setVisibility(View.GONE);
                        Log.d(TAG, "⚠️ 앨범 사진 없음, 썸네일 숨김");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 앨범 사진 로드 실패", e);
                    imgThumbnail.setVisibility(View.GONE);
                });
    }
    private void displayRegionTags(DocumentSnapshot postDoc) {
        layoutRegionTags.removeAllViews();

        List<Map<String, Object>> tagMaps = (List<Map<String, Object>>) postDoc.get("regionTags");

        if (tagMaps == null || tagMaps.isEmpty()) {
            layoutRegionTags.setVisibility(View.GONE);
            return;
        }

        layoutRegionTags.setVisibility(View.VISIBLE);

        for (Map<String, Object> tagMap : tagMaps) {
            String cityName = (String) tagMap.get("cityName");
            String provinceName = (String) tagMap.get("provinceName");

            String displayName = (cityName != null && !cityName.isEmpty()) ? cityName : provinceName;

            if (displayName == null) continue;

            TextView tagView = new TextView(this);
            tagView.setText("#" + displayName);
            tagView.setTextSize(13);
            tagView.setTextColor(Color.parseColor("#6366F1"));
            tagView.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));

            android.graphics.drawable.GradientDrawable tagBg = new android.graphics.drawable.GradientDrawable();
            tagBg.setColor(Color.parseColor("#EEF2FF"));
            tagBg.setCornerRadius(dpToPx(12));
            tagView.setBackground(tagBg);

            com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                    new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            tagView.setLayoutParams(params);

            layoutRegionTags.addView(tagView);
        }
    }
    private void showCopyScheduleDialog() {
        if (currentUid == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("스케줄 카피하기")
                .setMessage("이 스케줄을 내 일정으로 가져오시겠습니까?")
                .setPositiveButton("날짜 변경하고 가져오기", (dialog, which) -> showDatePickerForCopy())
                .setNegativeButton("그대로 가져오기", (dialog, which) -> copyScheduleWithOriginalDates())
                .setNeutralButton("취소", null)
                .show();
    }

    private void showDatePickerForCopy() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTheme(R.style.ThemeOverlay_App_DatePicker)
                .setTitleText("여행 기간을 선택하세요");

        if (originalStartDate != 0 && originalEndDate != 0) {
            builder.setSelection(Pair.create(originalStartDate, originalEndDate));
        }

        MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Long startDateMillis = selection.first;
            Long endDateMillis = selection.second;

            TimeZone timeZone = TimeZone.getDefault();
            long startOffset = timeZone.getOffset(startDateMillis);
            long endOffset = timeZone.getOffset(endDateMillis);

            long newStartDate = startDateMillis + startOffset;
            long newEndDate = endDateMillis + endOffset;

            long diffInMillis = newEndDate - newStartDate;
            int newDays = (int) TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1;

            if (newDays < dateList.size()) {
                Toast.makeText(this,
                        String.format("최소 %d일 이상이어야 합니다.\n(원본 스케줄이 %d일입니다)",
                                dateList.size(), dateList.size()),
                        Toast.LENGTH_LONG).show();
                return;
            }

            copyScheduleWithNewDates(newStartDate, newEndDate);
        });

        datePicker.show(getSupportFragmentManager(), "copy_date_picker");
    }

    private void copyScheduleWithOriginalDates() {
        if (originalStartDate == 0 || originalEndDate == 0) {
            Toast.makeText(this, "날짜 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        copyScheduleWithNewDates(originalStartDate, originalEndDate);
    }

    private void copyScheduleWithNewDates(long newStartDate, long newEndDate) {
        if (currentUid == null || postId == null) {
            Toast.makeText(this, "스케줄을 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCopySchedule.setEnabled(false);
        btnCopySchedule.setText("가져오는 중...");

        String newScheduleId = db.collection("user")
                .document(currentUid)
                .collection("schedule")
                .document().getId();

        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("scheduleId", newScheduleId);
        scheduleData.put("locationName", locationName);
        scheduleData.put("startDate", new Timestamp(new Date(newStartDate)));
        scheduleData.put("endDate", new Timestamp(new Date(newEndDate)));
        scheduleData.put("createdAt", System.currentTimeMillis());

        db.collection("user")
                .document(currentUid)
                .collection("schedule")
                .document(newScheduleId)
                .set(scheduleData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ 스케줄 문서 생성 완료: " + newScheduleId);
                    copyScheduleDateData(newScheduleId, newStartDate, newEndDate);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 스케줄 생성 실패", e);
                    Toast.makeText(this, "스케줄 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnCopySchedule.setEnabled(true);
                    btnCopySchedule.setText("카피하기");
                });
    }

    private void copyScheduleDateData(String newScheduleId, long newStartDate, long newEndDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        List<String> newDateList = new ArrayList<>();
        long diffMillis = newEndDate - newStartDate;
        int days = (int) TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;

        for (int i = 0; i < days; i++) {
            Date date = new Date(newStartDate + TimeUnit.DAYS.toMillis(i));
            newDateList.add(sdf.format(date));
        }

        Map<String, String> dateMapping = new HashMap<>();
        for (int i = 0; i < Math.min(dateList.size(), newDateList.size()); i++) {
            dateMapping.put(dateList.get(i), newDateList.get(i));
        }

        AtomicInteger pendingCopies = new AtomicInteger(dateMapping.size() * 2);

        for (Map.Entry<String, String> entry : dateMapping.entrySet()) {
            String oldDateKey = entry.getKey();
            String newDateKey = entry.getValue();
            int dayIndex = newDateList.indexOf(newDateKey) + 1;

            Map<String, Object> dateDoc = new HashMap<>();
            dateDoc.put("dayIndex", dayIndex);
            dateDoc.put("date", newDateKey);

            db.collection("user")
                    .document(currentUid)
                    .collection("schedule")
                    .document(newScheduleId)
                    .collection("scheduleDate")
                    .document(newDateKey)
                    .set(dateDoc)
                    .addOnSuccessListener(aVoid -> {
                        if (pendingCopies.decrementAndGet() == 0) {
                            finishCopy(newScheduleId, newStartDate, newEndDate);
                        }
                    });

            copyScheduleItemsAndAlbum(oldDateKey, newDateKey, newScheduleId, () -> {
                if (pendingCopies.decrementAndGet() == 0) {
                    finishCopy(newScheduleId, newStartDate, newEndDate);
                }
            });
        }
    }

    private void copyScheduleItemsAndAlbum(String oldDateKey, String newDateKey, String newScheduleId, Runnable onComplete) {
        CollectionReference sourceItems = communityPostsRef
                .document(postId)
                .collection("scheduleDate")
                .document(oldDateKey)
                .collection("scheduleItem");

        CollectionReference destItems = db.collection("user")
                .document(currentUid)
                .collection("schedule")
                .document(newScheduleId)
                .collection("scheduleDate")
                .document(newDateKey)
                .collection("scheduleItem");

        sourceItems.get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                copyAlbumData(oldDateKey, newDateKey, newScheduleId, onComplete);
                return;
            }

            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    batch.set(destItems.document(doc.getId()), data);
                }
            }

            batch.commit().addOnSuccessListener(aVoid -> {
                copyAlbumData(oldDateKey, newDateKey, newScheduleId, onComplete);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "❌ 일정 복사 실패", e);
                onComplete.run();
            });
        });
    }

    private void copyAlbumData(String oldDateKey, String newDateKey, String newScheduleId, Runnable onComplete) {
        CollectionReference sourceAlbum = communityPostsRef
                .document(postId)
                .collection("scheduleDate")
                .document(oldDateKey)
                .collection("album");

        CollectionReference destAlbum = db.collection("user")
                .document(currentUid)
                .collection("schedule")
                .document(newScheduleId)
                .collection("scheduleDate")
                .document(newDateKey)
                .collection("album");

        sourceAlbum.get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                onComplete.run();
                return;
            }

            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    batch.set(destAlbum.document(doc.getId()), data);
                }
            }

            batch.commit().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ 앨범 복사 완료: " + newDateKey);
                onComplete.run();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "❌ 앨범 복사 실패", e);
                onComplete.run();
            });
        });
    }

    private void finishCopy(String newScheduleId, long newStartDate, long newEndDate) {
        runOnUiThread(() -> {
            Toast.makeText(this, "스케줄을 내 일정으로 가져왔습니다!", Toast.LENGTH_SHORT).show();
            btnCopySchedule.setEnabled(true);
            btnCopySchedule.setText("카피하기");

            Intent intent = new Intent(PostDetailActivity.this, ScheduleSettingActivity.class);
            intent.putExtra("scheduleId", newScheduleId);
            intent.putExtra("startDate", newStartDate);
            intent.putExtra("endDate", newEndDate);
            startActivity(intent);
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

    private void deleteLikesAndCommentsCollection(Runnable onComplete) {
        final int[] collectionsToDelete = {2};

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

    private void toggleHeart() {
        if (currentUid == null || postId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBottomHeart.setEnabled(false);

        if (isLiked) {
            communityPostsRef.document(postId)
                    .collection("likes")
                    .document(currentUid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        communityPostsRef.document(postId)
                                .update("heartCount", FieldValue.increment(-1))
                                .addOnSuccessListener(aVoid2 -> {
                                    btnBottomHeart.setEnabled(true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "좋아요 취소 실패", Toast.LENGTH_SHORT).show();
                                    btnBottomHeart.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "좋아요 취소 실패", Toast.LENGTH_SHORT).show();
                        btnBottomHeart.setEnabled(true);
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
                                    btnBottomHeart.setEnabled(true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "좋아요 실패", Toast.LENGTH_SHORT).show();
                                    btnBottomHeart.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "좋아요 실패", Toast.LENGTH_SHORT).show();
                        btnBottomHeart.setEnabled(true);
                    });
        }
    }

    private void updateHeartUI() {
        int iconRes = isLiked ? R.drawable.ic_heart_c : R.drawable.ic_heart;
        btnBottomHeart.setImageResource(iconRes);
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
                                FollowButtonHelper.updateFollowButton(btnFollowAuthor, isFollowing, isFollower);
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
            dayBtn.setTextColor(i == 0 ? Color.parseColor("#575DFB") : Color.parseColor("#666666"));
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
            btn.setTextColor(i == dayIndex ? Color.parseColor("#575DFB") : Color.parseColor("#666666"));
        }

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewDayIndicator.getLayoutParams();
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
                                        tvPhotoIndicator.setText((position + 1) + " / " + currentPhotos.size());
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

                    ScheduleItemAdapter adapter = new ScheduleItemAdapter((item, docId) -> {});
                    adapter.submitList(new ArrayList<>(currentScheduleItems), new ArrayList<>());
                    adapter.setReadOnlyMode(true);

                    rvScheduleItems.setAdapter(adapter);
                });
    }

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
        if (likeListener != null) {
            likeListener.remove();
        }
    }

    class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

        private List<AlbumDTO> photos;

        PhotoPagerAdapter(List<AlbumDTO> photos) {
            this.photos = photos;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post_photo, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            AlbumDTO photo = photos.get(position);

            Glide.with(PostDetailActivity.this)
                    .load(photo.getImageUrl())
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

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ArrayList<String> photoUrls = new ArrayList<>();
                        for (AlbumDTO photo : photos) {
                            photoUrls.add(photo.getImageUrl());
                        }

                        PhotoFullscreenFragment fragment = PhotoFullscreenFragment.newInstance(photoUrls, position);
                        fragment.show(getSupportFragmentManager(), "photo_fullscreen");
                    }
                });
            }
        }
    }

}