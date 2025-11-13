package com.example.tot.Home;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Community.CommunityAdapter;
import com.example.tot.Community.CommunityPostDTO;
import com.example.tot.Community.CommunityViewModel;
import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    // 지역 코드
    private String selectedProvinceCode = "ALL";
    private String selectedCityCode = "";

    // UI
    private LinearLayout provinceButtonContainer;
    private LinearLayout cityButtonContainer;
    private HorizontalScrollView cityScrollView;
    private Button currentSelectedProvinceButton;
    private Button currentSelectedCityButton;
    private CircleImageView profileImage;
    private FrameLayout inboxContainer;
    private TextView inboxBadge;

    // 데이터
    private CommunityAdapter communityAdapter;
    private CommunityViewModel viewModel;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupProvinceButtons();
        setupMemoryRecyclerView(view.findViewById(R.id.re_memory));
        setupCommunityStyleRecyclerView(view.findViewById(R.id.re_album));
        setupProfileAndInbox();

        viewModel.loadDummyData();
        filterAlbums();
    }

    private void initViews(View view) {
        provinceButtonContainer = view.findViewById(R.id.provinceButtonContainer);
        cityButtonContainer = view.findViewById(R.id.cityButtonContainer);
        cityScrollView = view.findViewById(R.id.cityScrollView);
        profileImage = view.findViewById(R.id.profileImage);
        inboxContainer = view.findViewById(R.id.inbox_container);
        inboxBadge = view.findViewById(R.id.inbox_badge);
    }

    private void initViewModel() {
        viewModel = new CommunityViewModel(new CommunityViewModel.DataCallback() {
            @Override
            public void onDataChanged(List<CommunityPostDTO> posts) {
                if (communityAdapter != null) communityAdapter.updateData(posts);
            }

            @Override
            public void onDataAdded(List<CommunityPostDTO> posts) {
                if (communityAdapter != null) communityAdapter.addData(posts);
            }
        });
        viewModel.setFilter(CommunityViewModel.FilterMode.POPULAR);
    }

    /** ✅ 프로필 및 수신함 (애니메이션 포함) */
    private void setupProfileAndInbox() {
        profileImage.setOnClickListener(v -> {
            androidx.viewpager2.widget.ViewPager2 viewPager =
                    requireActivity().findViewById(R.id.viewpager);
            if (viewPager != null) viewPager.setCurrentItem(3);
        });

        inboxContainer.setOnClickListener(v ->
                Toast.makeText(getContext(), "수신함 (준비중)", Toast.LENGTH_SHORT).show()
        );

        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setShape(GradientDrawable.RECTANGLE);
        badgeBackground.setColor(Color.parseColor("#FF4444"));
        badgeBackground.setCornerRadius(dpToPx(8));
        inboxBadge.setBackground(badgeBackground);
        inboxBadge.setClipToOutline(true);

        int unreadCount = 11; // 예시 값
        if (unreadCount > 0) {
            inboxBadge.setVisibility(View.VISIBLE);
            inboxBadge.setText(unreadCount > 10 ? "10+" : String.valueOf(unreadCount));

            Animation shakeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.shake);
            inboxContainer.startAnimation(shakeAnim);
        } else {
            inboxBadge.setVisibility(View.GONE);
        }
    }

    /** 시/도 버튼 */
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

    /** 시군구 버튼 */
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

    /** 지역 버튼 생성 */
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

    /** 버튼 외형 업데이트 */
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

    /** 시/도 버튼 상태 갱신 */
    private void updateProvinceButtonStates(Button selectedButton) {
        if (currentSelectedProvinceButton != null)
            updateButtonAppearance(currentSelectedProvinceButton, false);
        updateButtonAppearance(selectedButton, true);
        currentSelectedProvinceButton = selectedButton;
    }

    /** 시군구 버튼 상태 갱신 */
    private void updateCityButtonStates(Button selectedButton) {
        if (currentSelectedCityButton != null)
            updateButtonAppearance(currentSelectedCityButton, false);
        updateButtonAppearance(selectedButton, true);
        currentSelectedCityButton = selectedButton;
    }

    /** 지역별 게시글 필터 */
    private void filterAlbums() {
        if (viewModel == null) return;

        List<CommunityPostDTO> allPosts = viewModel.getAllPosts();
        List<CommunityPostDTO> filtered = new ArrayList<>();

        if (selectedProvinceCode.equals("ALL")) {
            filtered.addAll(allPosts);
        } else if (selectedCityCode.isEmpty()) {
            for (CommunityPostDTO post : allPosts)
                if (post.getProvinceCode().equals(selectedProvinceCode))
                    filtered.add(post);
        } else {
            for (CommunityPostDTO post : allPosts)
                if (post.getProvinceCode().equals(selectedProvinceCode)
                        && post.getCityCode().equals(selectedCityCode))
                    filtered.add(post);
        }

        if (communityAdapter != null) communityAdapter.updateData(filtered);
    }

    /** 메모리 RecyclerView */
    private void setupMemoryRecyclerView(RecyclerView memoryView) {
        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        List<HomeScheduleDTO> items = new ArrayList<>();
        items.add(new HomeScheduleDTO("회의", "2025.10.15", "Room 6-205", "12:00", "13:05", R.drawable.ic_location_point));
        items.add(new HomeScheduleDTO("스터디", "2025.10.20", "Room 3-102", "15:00", "17:00", R.drawable.ic_location_point));
        items.add(new HomeScheduleDTO("약속", "2025.10.22", "Room 7-301", "18:30", "20:00", R.drawable.ic_location_point));

        HomeScheduleAdapter scheduleAdapter = new HomeScheduleAdapter(items);
        memoryView.setAdapter(scheduleAdapter);

        androidx.recyclerview.widget.SnapHelper snapHelper = new androidx.recyclerview.widget.LinearSnapHelper();
        snapHelper.attachToRecyclerView(memoryView);
    }

    /** 커뮤니티 스타일 RecyclerView */
    private void setupCommunityStyleRecyclerView(RecyclerView albumView) {
        albumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        List<CommunityPostDTO> initialData = new ArrayList<>();
        communityAdapter = new CommunityAdapter(initialData, (post, position) -> {
            if (post != null)
                Toast.makeText(getContext(), post.getTitle() + " 상세보기", Toast.LENGTH_SHORT).show();
        });
        albumView.setAdapter(communityAdapter);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) viewModel.destroy();
    }
}
