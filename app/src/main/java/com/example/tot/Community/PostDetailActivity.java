package com.example.tot.Community;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;
import com.example.tot.Schedule.ScheduleSetting.ScheduleItemAdapter;
import com.example.tot.Schedule.ScheduleSetting.ScheduleItemDTO;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

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
    private String scheduleId, authorUid, currentUid;

    private List<String> dateList = new ArrayList<>();
    private List<AlbumDTO> currentPhotos = new ArrayList<>();
    private List<ScheduleItemDTO> currentScheduleItems = new ArrayList<>();
    private int selectedDayIndex = 0;
    private boolean isLiked = false;
    private boolean isAuthor = false;

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
        btnComment.setOnClickListener(v -> Toast.makeText(this, "댓글 기능 준비중", Toast.LENGTH_SHORT).show());
        btnHeart.setOnClickListener(v -> toggleHeart());
        btnEdit.setOnClickListener(v -> Toast.makeText(this, "수정 기능 준비중", Toast.LENGTH_SHORT).show());
        btnDelete.setOnClickListener(v -> Toast.makeText(this, "삭제 기능 준비중", Toast.LENGTH_SHORT).show());

        rvScheduleItems.setLayoutManager(new LinearLayoutManager(this));
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
        btnEdit.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isAuthor ? View.VISIBLE : View.GONE);

        // Firestore에서 게시글 정보 로드 (제목 포함)
        communityPostsRef
                .whereEqualTo("scheduleId", scheduleId)
                .whereEqualTo("authorUid", authorUid)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String postTitle = querySnapshot.getDocuments().get(0).getString("title");
                        if (postTitle != null && !postTitle.isEmpty()) {
                            tvPostTitle.setText(postTitle);
                        }
                    }
                });

        db.collection("user").document(authorUid)
                .collection("schedule").document(scheduleId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String location = doc.getString("locationName");
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                        String startDate = sdf.format(doc.getTimestamp("startDate").toDate());
                        String endDate = sdf.format(doc.getTimestamp("endDate").toDate());

                        tvLocation.setText(location);
                        tvDateRange.setText(startDate + "~" + endDate);

                        generateDateList(doc.getTimestamp("startDate").toDate(),
                                doc.getTimestamp("endDate").toDate());
                        setupDayButtons();
                        loadDayData(0);
                    }
                });

        loadAuthorInfo();
        loadThumbnail();
        loadInvitedCount();
    }

    private void generateDateList(java.util.Date start, java.util.Date end) {
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

        loadDayData(dayIndex);
    }

    private void loadDayData(int dayIndex) {
        String dateKey = dateList.get(dayIndex);
        loadPhotos(dateKey);
        loadScheduleItems(dateKey);
        loadMapMarkers(dateKey);
    }

    private void loadPhotos(String dateKey) {
        db.collection("user").document(authorUid)
                .collection("schedule").document(scheduleId)
                .collection("scheduleDate").document(dateKey)
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
                        viewpagerPhotos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                tvPhotoIndicator.setText((position + 1) + " / " + currentPhotos.size());
                            }
                        });
                    }
                });
    }

    private void loadScheduleItems(String dateKey) {
        db.collection("user").document(authorUid)
                .collection("schedule").document(scheduleId)
                .collection("scheduleDate").document(dateKey)
                .collection("scheduleItem")
                .orderBy("startTime")
                .get()
                .addOnSuccessListener(snap -> {
                    currentScheduleItems.clear();
                    snap.forEach(doc -> currentScheduleItems.add(doc.toObject(ScheduleItemDTO.class)));

                    ScheduleItemAdapter adapter = new ScheduleItemAdapter((item, docId) -> {});
                    adapter.submitList(new ArrayList<>(currentScheduleItems), new ArrayList<>());
                    adapter.setReadOnlyMode(true); // 수정 버튼 숨김
                    rvScheduleItems.setAdapter(adapter);
                });
    }

    private void loadMapMarkers(String dateKey) {
        if (mMap == null) return;

        mMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        db.collection("user").document(authorUid)
                .collection("schedule").document(scheduleId)
                .collection("scheduleDate").document(dateKey)
                .collection("scheduleItem")
                .get()
                .addOnSuccessListener(snap -> {
                    boolean hasMarkers = false;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        GeoPoint geoPoint = doc.getGeoPoint("place");
                        if (geoPoint != null) {
                            LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(doc.getString("placeName"))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            builder.include(latLng);
                            hasMarkers = true;
                        }
                    }

                    if (hasMarkers) {
                        try {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void loadAuthorInfo() {
        db.collection("user").document(authorUid).get()
                .addOnSuccessListener(doc -> {
                    tvAuthorName.setText(doc.getString("nickname"));
                    String profileUrl = doc.getString("profileImageUrl");
                    if (profileUrl != null && !profileUrl.isEmpty()) {
                        Glide.with(this).load(profileUrl).into(imgAuthorProfile);
                    }
                });
    }

    private void loadThumbnail() {
        if (dateList.isEmpty()) return;

        db.collection("user").document(authorUid)
                .collection("schedule").document(scheduleId)
                .collection("scheduleDate").document(dateList.get(0))
                .collection("album")
                .orderBy("index")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String imageUrl = snap.getDocuments().get(0).getString("imageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(imgThumbnail);
                        }
                    }
                });
    }

    private void loadInvitedCount() {
        db.collection("user").document(authorUid)
                .collection("schedule").document(scheduleId)
                .collection("invited")
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap.size();
                    tvInvitedCount.setText(count > 0 ? "외 " + count + "명" : "");
                });
    }

    private void toggleHeart() {
        isLiked = !isLiked;
        btnHeart.setImageResource(isLiked ? R.drawable.ic_heart_c : R.drawable.ic_heart);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // 사진 어댑터 내부 클래스
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