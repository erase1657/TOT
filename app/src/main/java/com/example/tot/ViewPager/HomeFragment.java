package com.example.tot.ViewPager;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.example.tot.AlbumRecyclerView.AlbumAdapter;
import com.example.tot.AlbumRecyclerView.AlbumData;
import com.example.tot.MemoryRecyclerView.MemoryAdapter;
import com.example.tot.MemoryRecyclerView.MemoryData;
import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // 선택된 지역 코드 (서버 전송용)
    private String selectedProvinceCode = "ALL";  // "ALL" = 전체
    private String selectedCityCode = "";

    // 버튼 컨테이너
    private LinearLayout provinceButtonContainer;
    private LinearLayout cityButtonContainer;
    private HorizontalScrollView cityScrollView;

    // 현재 선택된 버튼 추적
    private Button currentSelectedProvinceButton;
    private Button currentSelectedCityButton;

    // RecyclerView 어댑터 (필터링 시 업데이트용)
    private AlbumAdapter albumAdapter;
    private List<AlbumData> allAlbumItems;  // 전체 앨범 데이터

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View 초기화
        RecyclerView memoryView = view.findViewById(R.id.re_memory);
        RecyclerView albumView = view.findViewById(R.id.re_album);
        provinceButtonContainer = view.findViewById(R.id.provinceButtonContainer);
        cityButtonContainer = view.findViewById(R.id.cityButtonContainer);
        cityScrollView = view.findViewById(R.id.cityScrollView);

        // 1. 시/도 버튼 생성 (인구 순으로 이미 정렬됨)
        setupProvinceButtons();

        // 2. 메모리 RecyclerView 설정
        setupMemoryRecyclerView(memoryView);

        // 3. 앨범 RecyclerView 설정
        setupAlbumRecyclerView(albumView);
    }

    /**
     * 시/도 버튼 설정 (인구 순 정렬)
     */
    private void setupProvinceButtons() {
        // "전체" 버튼 추가
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

        // RegionDataProvider에서 시/도 데이터 가져오기 (인구 순으로 정렬됨)
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

    /**
     * 시군구 버튼 설정 (가나다 순 정렬)
     */
    private void setupCityButtons(String provinceCode) {
        cityButtonContainer.removeAllViews();
        currentSelectedCityButton = null;

        // RegionDataProvider에서 해당 시/도의 시군구 데이터 가져오기 (가나다 순으로 정렬됨)
        List<RegionDataProvider.Region> cities = RegionDataProvider.getCities(provinceCode);

        if (cities == null || cities.isEmpty()) {
            cityScrollView.setVisibility(View.GONE);
            return;
        }

        cityScrollView.setVisibility(View.VISIBLE);

        // "전체" 버튼 추가
        Button allCityButton = createRegionButton("전체", "", true);
        allCityButton.setOnClickListener(v -> {
            selectedCityCode = "";
            updateCityButtonStates(allCityButton);
            filterAlbums();
        });
        cityButtonContainer.addView(allCityButton);
        currentSelectedCityButton = allCityButton;

        // 각 시군구 버튼 추가
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

    /**
     * 지역 버튼 생성 (공통)
     * @param text 버튼에 표시될 텍스트
     * @param regionCode 서버 전송용 지역 코드 (Button의 Tag에 저장)
     * @param isSelected 선택 여부
     */
    private Button createRegionButton(String text, String regionCode, boolean isSelected) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTag(regionCode);  // 중요: 지역 코드를 Tag에 저장하여 나중에 참조 가능
        button.setTextSize(14);
        button.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8));
        button.setAllCaps(false);  // 텍스트 대문자 변환 방지

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(35)
        );
        params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        button.setLayoutParams(params);

        // 배경과 텍스트 색상 설정
        updateButtonAppearance(button, isSelected);

        return button;
    }

    /**
     * 버튼 외형 업데이트
     */
    private void updateButtonAppearance(Button button, boolean isSelected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dpToPx(18));

        if (isSelected) {
            drawable.setColor(Color.parseColor("#6366F1")); // 선택됨: 보라색 배경
            button.setTextColor(Color.parseColor("#FFFFFF")); // 흰색 텍스트
        } else {
            drawable.setColor(Color.parseColor("#E5E7EB")); // 선택 안됨: 회색 배경
            button.setTextColor(Color.parseColor("#6B7280")); // 회색 텍스트
        }

        button.setBackground(drawable);
    }

    /**
     * 시/도 버튼 상태 업데이트
     */
    private void updateProvinceButtonStates(Button selectedButton) {
        if (currentSelectedProvinceButton != null) {
            updateButtonAppearance(currentSelectedProvinceButton, false);
        }
        updateButtonAppearance(selectedButton, true);
        currentSelectedProvinceButton = selectedButton;
    }

    /**
     * 시군구 버튼 상태 업데이트
     */
    private void updateCityButtonStates(Button selectedButton) {
        if (currentSelectedCityButton != null) {
            updateButtonAppearance(currentSelectedCityButton, false);
        }
        updateButtonAppearance(selectedButton, true);
        currentSelectedCityButton = selectedButton;
    }

    /**
     * 앨범 필터링 및 업데이트
     * 서버 연동 시 이 메서드에서 API 호출
     */
    private void filterAlbums() {
        // 현재 선택된 지역 코드 로그 출력
        String filterLog = "필터링 - 시/도 코드: " + selectedProvinceCode;
        if (!selectedCityCode.isEmpty()) {
            filterLog += ", 시군구 코드: " + selectedCityCode;
        }
        android.util.Log.d("HomeFragment", filterLog);

        // TODO: 서버 연동 시 아래와 같이 API 호출
        // fetchAlbumsFromServer(selectedProvinceCode, selectedCityCode);

        // 현재는 더미 데이터로 필터링 (로컬 필터링 예시)
        if (albumAdapter != null && allAlbumItems != null) {
            List<AlbumData> filteredItems = new ArrayList<>();

            if (selectedProvinceCode.equals("ALL")) {
                // 전체 선택 시 모든 앨범 표시
                filteredItems.addAll(allAlbumItems);
            } else if (selectedCityCode.isEmpty()) {
                // 시/도만 선택된 경우
                for (AlbumData item : allAlbumItems) {
                    if (item.getProvinceCode().equals(selectedProvinceCode)) {
                        filteredItems.add(item);
                    }
                }
            } else {
                // 시/도와 시군구 모두 선택된 경우
                for (AlbumData item : allAlbumItems) {
                    if (item.getProvinceCode().equals(selectedProvinceCode)
                            && item.getCityCode().equals(selectedCityCode)) {
                        filteredItems.add(item);
                    }
                }
            }

            // 어댑터 업데이트
            albumAdapter.updateData(filteredItems);

            // 결과 로그
            android.util.Log.d("HomeFragment", "필터링 결과: " + filteredItems.size() + "개 앨범");
        }
    }

    /**
     * 서버에서 앨범 데이터 가져오기 (예시)
     * 실제 구현 시 Retrofit 등의 HTTP 클라이언트 사용
     */
    private void fetchAlbumsFromServer(String provinceCode, String cityCode) {
        // 예시: API 호출 구조
        // ApiService.getAlbums(provinceCode, cityCode)
        //     .enqueue(new Callback<List<AlbumData>>() {
        //         @Override
        //         public void onResponse(Call<List<AlbumData>> call, Response<List<AlbumData>> response) {
        //             if (response.isSuccessful() && response.body() != null) {
        //                 albumAdapter.updateData(response.body());
        //             }
        //         }
        //
        //         @Override
        //         public void onFailure(Call<List<AlbumData>> call, Throwable t) {
        //             Log.e("HomeFragment", "API 호출 실패", t);
        //         }
        //     });
    }

    /**
     * 메모리 RecyclerView 설정
     */
    private void setupMemoryRecyclerView(RecyclerView memoryView) {
        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        List<MemoryData> items = new ArrayList<>();
        items.add(new MemoryData("회의", "2025.10.15", "Room 6-205", "12:00", "13:05", R.drawable.location_point));
        items.add(new MemoryData("스터디", "2025.10.20", "Room 3-102", "15:00", "17:00", R.drawable.location_point));
        items.add(new MemoryData("약속", "2025.10.22", "Room 7-301", "18:30", "20:00", R.drawable.location_point));

        MemoryAdapter memoryAdapter = new MemoryAdapter(items);
        memoryView.setAdapter(memoryAdapter);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(memoryView);
    }

    /**
     * 앨범 RecyclerView 설정
     */
    private void setupAlbumRecyclerView(RecyclerView albumView) {
        albumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        // 전체 앨범 데이터 초기화 (더미 데이터 - 지역 정보 포함)
        allAlbumItems = new ArrayList<>();
        // 지역 정보 포함: new AlbumData(이름, 프로필, 앨범, 시/도코드, 시군구코드)
        allAlbumItems.add(new AlbumData("테스트 이름1", R.drawable.sample1, R.drawable.sample3, "11", "11680")); // 서울 강남구
        allAlbumItems.add(new AlbumData("테스트 이름2", R.drawable.sample1, R.drawable.sample3, "26", "26350")); // 부산 해운대구
        allAlbumItems.add(new AlbumData("테스트 이름3", R.drawable.sample1, R.drawable.sample3, "11", "11200")); // 서울 성동구

        albumAdapter = new AlbumAdapter(new ArrayList<>(allAlbumItems));
        albumView.setAdapter(albumAdapter);
    }

    /**
     * DP를 PX로 변환
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}