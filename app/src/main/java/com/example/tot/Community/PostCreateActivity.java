package com.example.tot.Community;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.tot.Home.RegionDataProvider;
import com.example.tot.R;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostCreateActivity extends AppCompatActivity {

    private static final String TAG = "PostCreateActivity";

    private ImageView btnBack;
    private TextView tvScheduleInfo;
    private EditText edtPostTitle;
    private Button btnPublish;
    private ImageView imgThumbnail;

    private LinearLayout provinceButtonContainer;
    private LinearLayout cityButtonContainer;
    private android.widget.HorizontalScrollView cityScrollView;
    private Button btnAddRegionTag;
    private FlexboxLayout layoutAddedTags;

    private String scheduleId;
    private String locationName;
    private long startDate;
    private long endDate;
    private String thumbnailUri;

    private String selectedProvinceCode = "";
    private String selectedProvinceName = "";
    private String selectedCityCode = "";
    private String selectedCityName = "";
    private Button currentSelectedProvinceButton;
    private Button currentSelectedCityButton;

    private List<CommunityPostDTO.RegionTag> addedRegionTags = new ArrayList<>();

    private FirebaseFirestore db;
    private CollectionReference communityPostsRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_create);

        db = FirebaseFirestore.getInstance();
        communityPostsRef = db.collection("public")
                .document("community")
                .collection("posts");
        auth = FirebaseAuth.getInstance();

        scheduleId = getIntent().getStringExtra("scheduleId");
        locationName = getIntent().getStringExtra("locationName");
        startDate = getIntent().getLongExtra("startDate", 0);
        endDate = getIntent().getLongExtra("endDate", 0);

        initViews();
        displayScheduleInfo();
        loadScheduleData();
        setupRegionButtons();

        btnBack.setOnClickListener(v -> finish());
        btnPublish.setOnClickListener(v -> publishPost());
        btnAddRegionTag.setOnClickListener(v -> addRegionTag());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvScheduleInfo = findViewById(R.id.tv_schedule_info);
        edtPostTitle = findViewById(R.id.edt_post_title);
        btnPublish = findViewById(R.id.btn_publish);
        imgThumbnail = findViewById(R.id.img_thumbnail);

        provinceButtonContainer = findViewById(R.id.provinceButtonContainer);
        cityButtonContainer = findViewById(R.id.cityButtonContainer);
        cityScrollView = findViewById(R.id.cityScrollView);
        btnAddRegionTag = findViewById(R.id.btn_add_region_tag);
        layoutAddedTags = findViewById(R.id.layout_added_tags);
    }

    private void displayScheduleInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        String start = sdf.format(new Date(startDate));
        String end = sdf.format(new Date(endDate));

        String info = locationName + " (" + start + " ~ " + end + ")";
        tvScheduleInfo.setText(info);
    }

    private void loadScheduleData() {
        if (scheduleId == null || auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        db.collection("user").document(uid).collection("schedule").document(scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // ✅ 스케줄의 backgroundImageUri를 썸네일로 사용
                        String imageUri = documentSnapshot.getString("backgroundImageUri");
                        Log.d(TAG, "📸 스케줄 썸네일 로드: " + imageUri);

                        if (imageUri != null && !imageUri.isEmpty()) {
                            thumbnailUri = imageUri;
                            imgThumbnail.setVisibility(View.VISIBLE);
                            Glide.with(this)
                                    .load(Uri.parse(thumbnailUri))
                                    .placeholder(R.drawable.sample3)
                                    .error(R.drawable.sample3)
                                    .centerCrop()
                                    .into(imgThumbnail);
                            Log.d(TAG, "✅ 썸네일 이미지 표시 완료");
                        } else {
                            Log.d(TAG, "⚠️ 스케줄에 썸네일 없음");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 스케줄 데이터 로드 실패", e);
                });
    }

    private void setupRegionButtons() {
        List<RegionDataProvider.Region> provinces = RegionDataProvider.getProvinces();

        for (RegionDataProvider.Region province : provinces) {
            Button button = createRegionButton(province.getName(), province.getCode(), false);
            button.setOnClickListener(v -> {
                selectedProvinceCode = province.getCode();
                selectedProvinceName = province.getName();
                selectedCityCode = "";
                selectedCityName = "";
                updateProvinceButtonStates(button);
                setupCityButtons(province.getCode());
            });
            provinceButtonContainer.addView(button);
        }
    }

    private void setupCityButtons(String provinceCode) {
        cityButtonContainer.removeAllViews();
        currentSelectedCityButton = null;
        selectedCityCode = "";
        selectedCityName = "";

        List<RegionDataProvider.Region> cities = RegionDataProvider.getCities(provinceCode);
        if (cities == null || cities.isEmpty()) {
            cityScrollView.setVisibility(View.GONE);
            return;
        }
        cityScrollView.setVisibility(View.VISIBLE);

        for (RegionDataProvider.Region city : cities) {
            Button button = createRegionButton(city.getName(), city.getCode(), false);
            button.setOnClickListener(v -> {
                selectedCityCode = city.getCode();
                selectedCityName = city.getName();
                updateCityButtonStates(button);
            });
            cityButtonContainer.addView(button);
        }
    }

    private Button createRegionButton(String text, String regionCode, boolean isSelected) {
        Button button = new Button(this);
        button.setText(text);
        button.setTag(regionCode);
        button.setTextSize(14);
        button.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8));
        button.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(35)
        );
        params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        button.setLayoutParams(params);

        updateButtonAppearance(button, isSelected);
        return button;
    }

    private void updateButtonAppearance(Button button, boolean isSelected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dpToPx(18));
        if (isSelected) {
            drawable.setColor(Color.parseColor("#6366F1"));
            button.setTextColor(Color.WHITE);
        } else {
            drawable.setColor(Color.parseColor("#E5E7EB"));
            button.setTextColor(Color.parseColor("#6B7280"));
        }
        button.setBackground(drawable);
    }

    private void updateProvinceButtonStates(Button selectedButton) {
        if (currentSelectedProvinceButton != null)
            updateButtonAppearance(currentSelectedProvinceButton, false);

        updateButtonAppearance(selectedButton, true);
        currentSelectedProvinceButton = selectedButton;
    }

    private void updateCityButtonStates(Button selectedButton) {
        if (currentSelectedCityButton != null)
            updateButtonAppearance(currentSelectedCityButton, false);

        updateButtonAppearance(selectedButton, true);
        currentSelectedCityButton = selectedButton;
    }

    private void addRegionTag() {
        if (selectedProvinceCode.isEmpty()) {
            Toast.makeText(this, "지역을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        for (CommunityPostDTO.RegionTag tag : addedRegionTags) {
            if (tag.getProvinceCode().equals(selectedProvinceCode) &&
                    (selectedCityCode.isEmpty() || tag.getCityCode().equals(selectedCityCode))) {
                Toast.makeText(this, "이미 추가된 지역입니다", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        CommunityPostDTO.RegionTag newTag = new CommunityPostDTO.RegionTag(
                selectedProvinceCode,
                selectedProvinceName,
                selectedCityCode.isEmpty() ? "" : selectedCityCode,
                selectedCityName.isEmpty() ? "" : selectedCityName
        );

        addedRegionTags.add(newTag);
        displayAddedTags();
        Toast.makeText(this, "지역태그가 추가되었습니다", Toast.LENGTH_SHORT).show();
    }

    private void displayAddedTags() {
        layoutAddedTags.removeAllViews();

        for (int i = 0; i < addedRegionTags.size(); i++) {
            CommunityPostDTO.RegionTag tag = addedRegionTags.get(i);
            final int index = i;

            LinearLayout tagView = new LinearLayout(this);
            tagView.setOrientation(LinearLayout.HORIZONTAL);
            tagView.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

            GradientDrawable tagBg = new GradientDrawable();
            tagBg.setColor(Color.parseColor("#E0E7FF"));
            tagBg.setCornerRadius(dpToPx(16));
            tagView.setBackground(tagBg);

            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            tagView.setLayoutParams(params);

            TextView tvTag = new TextView(this);
            tvTag.setText("#" + tag.getDisplayName());
            tvTag.setTextSize(14);
            tvTag.setTextColor(Color.parseColor("#4338CA"));
            tagView.addView(tvTag);

            TextView btnRemove = new TextView(this);
            btnRemove.setText(" ✕");
            btnRemove.setTextSize(14);
            btnRemove.setTextColor(Color.parseColor("#4338CA"));
            btnRemove.setPadding(dpToPx(4), 0, 0, 0);
            btnRemove.setOnClickListener(v -> {
                addedRegionTags.remove(index);
                displayAddedTags();
                Toast.makeText(this, "지역태그가 제거되었습니다", Toast.LENGTH_SHORT).show();
            });
            tagView.addView(btnRemove);

            layoutAddedTags.addView(tagView);
        }
    }

    private void publishPost() {
        String title = edtPostTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "게시글 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String postId = communityPostsRef.document().getId();
        final String postTitle = title;

        btnPublish.setEnabled(false);

        Map<String, Object> postData = new HashMap<>();
        postData.put("postId", postId);
        postData.put("authorUid", uid);
        postData.put("scheduleId", scheduleId);
        postData.put("title", title);
        postData.put("locationName", locationName);
        postData.put("startDate", startDate);
        postData.put("endDate", endDate);
        postData.put("createdAt", System.currentTimeMillis());
        postData.put("heartCount", 0);
        postData.put("commentCount", 0);

        // ✅ 썸네일 URL 저장 (스케줄의 backgroundImageUri)
        if (thumbnailUri != null && !thumbnailUri.isEmpty()) {
            postData.put("thumbnailUrl", thumbnailUri);
            Log.d(TAG, "✅ 게시글에 썸네일 URL 저장: " + thumbnailUri);
        } else {
            Log.d(TAG, "⚠️ 썸네일 없이 게시글 생성");
        }

        List<Map<String, Object>> tagMaps = new ArrayList<>();
        for (CommunityPostDTO.RegionTag tag : addedRegionTags) {
            Map<String, Object> tagMap = new HashMap<>();
            tagMap.put("provinceCode", tag.getProvinceCode());
            tagMap.put("provinceName", tag.getProvinceName());
            tagMap.put("cityCode", tag.getCityCode());
            tagMap.put("cityName", tag.getCityName());
            tagMaps.add(tagMap);
        }
        postData.put("regionTags", tagMaps);

        communityPostsRef
                .document(postId)
                .set(postData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ 게시글 등록 성공: " + postId);
                    sendNotificationToFollowers(uid, postId, postTitle);
                    copyScheduleToPost(uid, scheduleId, postId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 게시글 등록 실패", e);
                    Toast.makeText(this, "게시글 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                });
    }

    private void sendNotificationToFollowers(String authorUid, String postId, String postTitle) {
        db.collection("user")
                .document(authorUid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String authorName = userDoc.exists() ? userDoc.getString("nickname") : "사용자";
                    if (authorName == null || authorName.isEmpty()) {
                        authorName = "사용자";
                    }
                    PostCreateNotificationHelper.notifyFollowers(authorUid, authorName, postId, postTitle);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 작성자 정보 조회 실패 (알림 전송 불가)", e);
                });
    }

    private void copyScheduleToPost(String uid, String scheduleId, String postId) {
        Log.d(TAG, "📋 일정 데이터 복사 시작: " + scheduleId);

        CollectionReference sourceScheduleRef = db.collection("user")
                .document(uid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate");

        CollectionReference destScheduleRef = communityPostsRef
                .document(postId)
                .collection("scheduleDate");

        sourceScheduleRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "⚠️ 복사할 일정 데이터가 없음");
                        // ✅ 게시글 생성 완료 후 커뮤니티 새로고침
                        refreshCommunityData();
                        Toast.makeText(this, "게시글이 등록되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    int[] pendingCopies = {0};

                    for (DocumentSnapshot dateDoc : querySnapshot.getDocuments()) {
                        String dateKey = dateDoc.getId();
                        Map<String, Object> dateData = dateDoc.getData();

                        if (dateData != null) {
                            batch.set(destScheduleRef.document(dateKey), dateData);
                        }

                        pendingCopies[0]++;
                        copyScheduleItems(uid, scheduleId, dateKey, postId, () -> {
                            pendingCopies[0]--;
                            if (pendingCopies[0] == 0) {
                                Log.d(TAG, "✅ 모든 일정 복사 완료");
                                // ✅ 게시글 생성 완료 후 커뮤니티 새로고침
                                refreshCommunityData();
                                Toast.makeText(this, "게시글이 등록되었습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });

                        pendingCopies[0]++;
                        copyAlbumData(uid, scheduleId, dateKey, postId, () -> {
                            pendingCopies[0]--;
                            if (pendingCopies[0] == 0) {
                                Log.d(TAG, "✅ 모든 앨범 복사 완료");
                            }
                        });
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid2 -> Log.d(TAG, "✅ scheduleDate 배치 저장 완료"))
                            .addOnFailureListener(e -> Log.e(TAG, "❌ scheduleDate 저장 실패", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 일정 데이터 복사 실패", e);
                    // ✅ 실패해도 게시글은 등록되었으므로 새로고침
                    refreshCommunityData();
                    Toast.makeText(this, "게시글이 등록되었습니다 (일정 복사 실패)", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * ✅ 커뮤니티 데이터 새로고침
     */
    private void refreshCommunityData() {
        CommunityDataManager.getInstance().refresh();
        Log.d(TAG, "🔄 커뮤니티 데이터 새로고침 완료");
    }

    private void copyScheduleItems(String uid, String scheduleId, String dateKey, String postId, Runnable onComplete) {
        CollectionReference sourceItems = db.collection("user")
                .document(uid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem");

        CollectionReference destItems = communityPostsRef
                .document(postId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem");

        sourceItems.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            batch.set(destItems.document(doc.getId()), data);
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ scheduleItem 복사 완료: " + dateKey);
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ scheduleItem 복사 실패: " + dateKey, e);
                                onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ scheduleItem 로드 실패", e);
                    onComplete.run();
                });
    }

    private void copyAlbumData(String uid, String scheduleId, String dateKey, String postId, Runnable onComplete) {
        CollectionReference sourceAlbum = db.collection("user")
                .document(uid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album");

        CollectionReference destAlbum = communityPostsRef
                .document(postId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album");

        sourceAlbum.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            batch.set(destAlbum.document(doc.getId()), data);
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ album 복사 완료: " + dateKey);
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ album 복사 실패", e);
                                onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ album 로드 실패", e);
                    onComplete.run();
                });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}