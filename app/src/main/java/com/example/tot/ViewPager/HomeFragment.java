package com.example.tot.ViewPager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

import nl.bryanderidder.themedtogglebuttongroup.ThemedButton;
import nl.bryanderidder.themedtogglebuttongroup.ThemedToggleButtonGroup;

public class HomeFragment extends Fragment {
    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView memoryView = view.findViewById(R.id.re_memory);
        RecyclerView albumView = view.findViewById(R.id.re_album);
        ThemedToggleButtonGroup regionGroup = view.findViewById(R.id.map_tags);

        // 1 지역 더미 데이터
        List<String> regions = new ArrayList<>();
        regions.add("서울");
        regions.add("부산");
        regions.add("대전");
        regions.add("대구");
        regions.add("광주");
        regions.add("인천");
        regions.add("익산");

        // 2 "전체" 외 지역 버튼을 동적으로 추가
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (String region : regions) {
            ThemedButton button = (ThemedButton) inflater.inflate(
                    R.layout.item_region_button,
                    regionGroup,
                    false
            );
            button.setText(region);

            // FlexboxLayout.LayoutParams로 flexShrink 설정
            FlexboxLayout.LayoutParams params = (FlexboxLayout.LayoutParams) button.getLayoutParams();
            if (params != null) {
                params.setFlexShrink(0.0f); // 축소 방지
            }

            regionGroup.addView(button);
        }

        // 3 지역 선택 리스너
        regionGroup.setOnSelectListener((button) -> {
            String regionText = button.getText().toString();
            if (button.isSelected()) {
                // 선택된 지역에 따라 게시글(앨범) 필터링 가능
            }
            return null;
        });

        // 4 메모리 RecyclerView 설정
        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        List<MemoryData> items = new ArrayList<>();
        items.add(new MemoryData("회의", "2025.10.15", "Room 6-205", "12:00", "13:05", R.drawable.mypage));
        items.add(new MemoryData("스터디", "2025.10.20", "Room 3-102", "15:00", "17:00", R.drawable.mypage));
        items.add(new MemoryData("약속", "2025.10.22", "Room 7-301", "18:30", "20:00", R.drawable.mypage));

        MemoryAdapter memoryAdapter = new MemoryAdapter(items);
        memoryView.setAdapter(memoryAdapter);

        // SnapHelper
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(memoryView);

        // 5 앨범 RecyclerView 설정
        albumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        List<AlbumData> albumItems = new ArrayList<>();
        albumItems.add(new AlbumData("테스트 이름1", R.drawable.sample1, R.drawable.sample3));
        albumItems.add(new AlbumData("테스트 이름2", R.drawable.sample1, R.drawable.sample3));
        albumItems.add(new AlbumData("테스트 이름3", R.drawable.sample1, R.drawable.sample3));

        AlbumAdapter albumAdapter = new AlbumAdapter(albumItems);
        albumView.setAdapter(albumAdapter);
    }
}