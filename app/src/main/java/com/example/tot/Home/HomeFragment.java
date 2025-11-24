package com.example.tot.Home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tot.Community.CommunityAdapter;
import com.example.tot.Community.CommunityPostDTO;
import com.example.tot.Community.PostDetailActivity;
import com.example.tot.Notification.NotificationActivity;
import com.example.tot.Notification.NotificationManager;
import com.example.tot.R;
import com.example.tot.User.ProfileImageHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private String selectedProvinceCode = "ALL";
    private String selectedCityCode = "";
    private List<HomeAlarmDTO> alarmList;
    private HomeAlarmAdapter alarmAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout provinceButtonContainer;
    private LinearLayout cityButtonContainer;
    private HorizontalScrollView cityScrollView;
    private Button currentSelectedProvinceButton;
    private Button currentSelectedCityButton;
    private CircleImageView profileImage;
    private FrameLayout inboxContainer;
    private TextView inboxBadge;
    private RecyclerView reMemory;
    private LinearLayout noScheduleSection;

    private CommunityAdapter communityAdapter;
    private List<CommunityPostDTO> allCommunityPosts = new ArrayList<>();

    private NotificationManager notificationManager;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews(view);
        setupNotificationManager();
        setupSwipeRefresh();
        setupProvinceButtons();
        setupMemoryRecyclerView(view.findViewById(R.id.re_memory));
        setupCommunityStyleRecyclerView(view.findViewById(R.id.re_album));
        setupProfileAndInbox();

        // ✅ 프로필 이미지 로드
        loadUserProfile();

        loadCommunityPostsFromFirestore();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_home);
        provinceButtonContainer = view.findViewById(R.id.provinceButtonContainer);
        cityButtonContainer = view.findViewById(R.id.cityButtonContainer);
        cityScrollView = view.findViewById(R.id.cityScrollView);
        profileImage = view.findViewById(R.id.profileImage);
        inboxContainer = view.findViewById(R.id.inbox_container);
        inboxBadge = view.findViewById(R.id.inbox_badge);
        reMemory = view.findViewById(R.id.re_memory);
        noScheduleSection = view.findViewById(R.id.noScheduleSection);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light)
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshHomeData);
    }

    private void refreshHomeData() {
        if (notificationManager != null) {
            notificationManager.refresh();
        }

        loadCommunityPostsFromFirestore();
        loadNextScheduleWithAllItems();

        // ✅ 프로필 이미지 새로고침
        loadUserProfile();

        swipeRefreshLayout.postDelayed(() -> {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), "새로고침 완료", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private void setupNotificationManager() {
        notificationManager = NotificationManager.getInstance();
        notificationManager.addListener(this::updateInboxBadge);
    }

    private void setupProfileAndInbox() {
        profileImage.setOnClickListener(v -> {
            androidx.viewpager2.widget.ViewPager2 viewPager =
                    requireActivity().findViewById(R.id.viewpager);
            if (viewPager != null) viewPager.setCurrentItem(3);
        });

        inboxContainer.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotificationActivity.class);
            startActivity(intent);
        });

        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.RECTANGLE);
        badgeBackground.setColor(Color.parseColor("#FF4444"));
        badgeBackground.setCornerRadius(dpToPx(8));
        inboxBadge.setBackground(badgeBackground);
        inboxBadge.setClipToOutline(true);

        updateInboxBadge(notificationManager.getUnreadCount());
    }

    /**
     * ✅ 사용자 프로필 이미지 로드 - ProfileImageHelper 사용
     */
    private void loadUserProfile() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("user")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String profileImageUrl = doc.getString("profileImageUrl");

                        // ✅ ProfileImageHelper 사용
                        ProfileImageHelper.loadCircleProfileImage(profileImage, profileImageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "프로필 로드 실패", e);
                    profileImage.setImageResource(R.drawable.ic_profile_default);
                });
    }

    private void updateInboxBadge(int unreadCount) {
        if (inboxBadge == null) return;

        if (unreadCount > 0) {
            inboxBadge.setVisibility(View.VISIBLE);
            inboxBadge.setText(unreadCount > 10 ? "10+" : String.valueOf(unreadCount));

            if (inboxBadge.getTag() == null) {
                Animation shakeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.shake);
                inboxContainer.startAnimation(shakeAnim);
                inboxBadge.setTag("animated");
            }
        } else {
            inboxBadge.setVisibility(View.GONE);
            inboxBadge.setTag(null);
        }
    }

    private void setupProvinceButtons() {
        Button allButton = createRegionButton("전체", "ALL", true);
        allButton.setOnClickListener(v -> {
            selectedProvinceCode = "ALL";
            selectedCityCode = "";
            updateProvinceButtonStates(allButton);
            cityScrollView.setVisibility(View.GONE);
            filterAlbums();
        });
        provinceButtonContainer.addView(allButton);
        currentSelectedProvinceButton = allButton;

        List<RegionDataProvider.Region> provinces = RegionDataProvider.getProvinces();
        for (RegionDataProvider.Region province : provinces) {
            Button button = createRegionButton(province.getName(), province.getCode(), false);
            button.setOnClickListener(v -> {
                selectedProvinceCode = province.getCode();
                selectedCityCode = "";
                updateProvinceButtonStates(button);
                setupCityButtons(province.getCode());
                filterAlbums();
            });
            provinceButtonContainer.addView(button);
        }
    }

    private void setupCityButtons(String provinceCode) {
        cityButtonContainer.removeAllViews();
        currentSelectedCityButton = null;

        List<RegionDataProvider.Region> cities = RegionDataProvider.getCities(provinceCode);
        if (cities == null || cities.isEmpty()) {
            cityScrollView.setVisibility(View.GONE);
            return;
        }
        cityScrollView.setVisibility(View.VISIBLE);

        Button allCityButton = createRegionButton("전체", "", true);
        allCityButton.setOnClickListener(v -> {
            selectedCityCode = "";
            updateCityButtonStates(allCityButton);
            filterAlbums();
        });
        cityButtonContainer.addView(allCityButton);
        currentSelectedCityButton = allCityButton;

        for (RegionDataProvider.Region city : cities) {
            Button button = createRegionButton(city.getName(), city.getCode(), false);
            button.setOnClickListener(v -> {
                selectedCityCode = city.getCode();
                updateCityButtonStates(button);
                filterAlbums();
            });
            cityButtonContainer.addView(button);
        }
    }

    private Button createRegionButton(String text, String regionCode, boolean isSelected) {
        Button button = new Button(getContext());
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

// 계속...
// HomeFragment.java 계속...

    private void loadCommunityPostsFromFirestore() {
        if (auth.getCurrentUser() == null) {
            allCommunityPosts.clear();
            filterAlbums();
            return;
        }

        db.collection("public")
                .document("community")
                .collection("posts")
                .orderBy("heartCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
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

                            post.setProvinceCode("");
                            post.setCityCode("");

                            tempPosts.add(post);
                            authorUidMap.put(postId, authorUid);
                        }
                    }

                    loadAuthorInfoForPosts(tempPosts, authorUidMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "커뮤니티 게시글 로드 실패", e);
                    allCommunityPosts.clear();
                    filterAlbums();
                });
    }

    private void loadAuthorInfoForPosts(List<CommunityPostDTO> posts, Map<String, String> authorUidMap) {
        if (posts.isEmpty()) {
            allCommunityPosts = new ArrayList<>();
            filterAlbums();
            return;
        }

        final int[] loadedCount = {0};
        final int totalCount = posts.size();
        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        for (CommunityPostDTO post : posts) {
            String authorUid = authorUidMap.get(post.getPostId());

            if (authorUid == null) {
                loadedCount[0]++;
                if (loadedCount[0] == totalCount) {
                    allCommunityPosts = new ArrayList<>(posts);
                    filterAlbums();
                }
                continue;
            }

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

            db.collection("user").document(authorUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String nickname = userDoc.getString("nickname");
                            String profileImageUrl = userDoc.getString("profileImageUrl");

                            post.setUserName(nickname != null ? nickname : "사용자");
                            post.setProfileImageUrl(profileImageUrl);
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == totalCount) {
                            allCommunityPosts = new ArrayList<>(posts);
                            Log.d(TAG, "홈 화면 게시글 로드 완료: " + allCommunityPosts.size() + "개");
                            filterAlbums();
                        }
                    })
                    .addOnFailureListener(e -> {
                        loadedCount[0]++;
                        if (loadedCount[0] == totalCount) {
                            allCommunityPosts = new ArrayList<>(posts);
                            filterAlbums();
                        }
                    });
        }
    }

    private void filterAlbums() {
        if (allCommunityPosts == null) return;

        List<CommunityPostDTO> filtered = new ArrayList<>();

        if (selectedProvinceCode.equals("ALL")) {
            filtered.addAll(allCommunityPosts);
        } else {
            filtered.addAll(allCommunityPosts);
        }

        filtered.sort((a, b) -> Integer.compare(b.getHeartCount(), a.getHeartCount()));

        if (communityAdapter != null) {
            communityAdapter.updateDataWithUsers(filtered, new ArrayList<>(), false, false);
        }
    }

    private void setupMemoryRecyclerView(RecyclerView memoryView) {
        alarmList = new ArrayList<>();
        alarmAdapter = new HomeAlarmAdapter(alarmList);

        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );
        memoryView.setAdapter(alarmAdapter);

        loadNextScheduleWithAllItems();
    }

    private void loadNextScheduleWithAllItems() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            showNoSchedule();
            return;
        }

        Calendar now = Calendar.getInstance();
        Timestamp currentTime = new Timestamp(now.getTime());

        db.collection("user")
                .document(uid)
                .collection("schedule")
                .get()
                .addOnSuccessListener(scheduleSnapshot -> {
                    List<ScheduleData> allSchedules = new ArrayList<>();

                    for (DocumentSnapshot doc : scheduleSnapshot.getDocuments()) {
                        String scheduleId = doc.getId();
                        Timestamp startDate = doc.getTimestamp("startDate");
                        Timestamp endDate = doc.getTimestamp("endDate");

                        if (startDate != null && endDate != null) {
                            allSchedules.add(new ScheduleData(scheduleId, startDate, endDate));
                        }
                    }

                    if (allSchedules.isEmpty()) {
                        showNoSchedule();
                        return;
                    }

                    Collections.sort(allSchedules, (a, b) ->
                            a.startDate.compareTo(b.startDate)
                    );

                    ScheduleData nextSchedule = null;
                    for (ScheduleData schedule : allSchedules) {
                        if (schedule.endDate.compareTo(currentTime) >= 0) {
                            nextSchedule = schedule;
                            break;
                        }
                    }

                    if (nextSchedule == null) {
                        showNoSchedule();
                        return;
                    }

                    loadAllItemsFromSchedule(uid, nextSchedule);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "스케줄 불러오기 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showNoSchedule();
                });
    }

    private void loadAllItemsFromSchedule(String uid, ScheduleData schedule) {
        List<String> dateList = generateDateList(schedule.startDate, schedule.endDate);

        if (dateList.isEmpty()) {
            showNoSchedule();
            return;
        }

        List<HomeAlarmDTO> allItems = new ArrayList<>();
        final int[] remainingDates = {dateList.size()};

        for (String dateKey : dateList) {
            db.collection("user")
                    .document(uid)
                    .collection("schedule")
                    .document(schedule.scheduleId)
                    .collection("scheduleDate")
                    .document(dateKey)
                    .collection("scheduleItem")
                    .get()
                    .addOnSuccessListener(itemSnapshot -> {
                        for (DocumentSnapshot doc : itemSnapshot.getDocuments()) {
                            String title = doc.getString("title");
                            String placeName = doc.getString("placeName");
                            Timestamp startTime = doc.getTimestamp("startTime");
                            Timestamp endTime = doc.getTimestamp("endTime");

                            if (title != null && startTime != null && endTime != null) {
                                HomeAlarmDTO dto = new HomeAlarmDTO(
                                        schedule.scheduleId,
                                        doc.getId(),
                                        title,
                                        dateKey,
                                        placeName,
                                        startTime,
                                        endTime
                                );
                                allItems.add(dto);
                            }
                        }

                        remainingDates[0]--;

                        if (remainingDates[0] == 0) {
                            displayScheduleItems(allItems);
                        }
                    })
                    .addOnFailureListener(e -> {
                        remainingDates[0]--;
                        if (remainingDates[0] == 0) {
                            displayScheduleItems(allItems);
                        }
                    });
        }
    }

    private List<String> generateDateList(Timestamp start, Timestamp end) {
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start.toDate());

        Date endDate = end.toDate();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        while (!calendar.getTime().after(endDate)) {
            dates.add(sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return dates;
    }

    private void displayScheduleItems(List<HomeAlarmDTO> items) {
        if (items.isEmpty()) {
            showNoSchedule();
            return;
        }

        Collections.sort(items, (a, b) -> {
            int dateCompare = a.getDate().compareTo(b.getDate());
            if (dateCompare != 0) return dateCompare;
            return a.getStartTime().compareTo(b.getStartTime());
        });

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                alarmList.clear();
                alarmList.addAll(items);
                alarmAdapter.notifyDataSetChanged();

                reMemory.setVisibility(View.VISIBLE);
                noScheduleSection.setVisibility(View.GONE);
            });
        }
    }

    private void showNoSchedule() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                alarmList.clear();
                alarmAdapter.notifyDataSetChanged();

                reMemory.setVisibility(View.GONE);
                noScheduleSection.setVisibility(View.VISIBLE);
            });
        }
    }

    private void setupCommunityStyleRecyclerView(RecyclerView albumView) {
        albumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        List<CommunityPostDTO> initial = new ArrayList<>();

        communityAdapter = new CommunityAdapter(
                initial,
                (post, pos) -> {
                    if (post.getScheduleId() != null && post.getUserId() != null) {
                        Intent intent = new Intent(getContext(), PostDetailActivity.class);
                        intent.putExtra("scheduleId", post.getScheduleId());
                        intent.putExtra("authorUid", post.getUserId());
                        intent.putExtra("postId", post.getPostId());
                        startActivity(intent);
                    } else {
                        Toast.makeText(getContext(), "게시글 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                },
                () -> {}
        );

        albumView.setAdapter(communityAdapter);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInboxBadge(notificationManager.getUnreadCount());
        loadNextScheduleWithAllItems();
        loadCommunityPostsFromFirestore();
        loadUserProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationManager != null)
            notificationManager.removeListener(this::updateInboxBadge);
    }

    private static class ScheduleData {
        String scheduleId;
        Timestamp startDate;
        Timestamp endDate;

        ScheduleData(String scheduleId, Timestamp startDate, Timestamp endDate) {
            this.scheduleId = scheduleId;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}