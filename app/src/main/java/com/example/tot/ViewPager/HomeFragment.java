package com.example.tot.ViewPager;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.SnapHelper;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.AlbumRecyclerView.AlbumAdapter;
import com.example.tot.AlbumRecyclerView.AlbumData;
import com.example.tot.MemoryRecyclerView.MemoryAdapter;
import com.example.tot.MemoryRecyclerView.MemoryData;
import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView memoryView = view.findViewById(R.id.re_memory);
        RecyclerView albumView = view.findViewById(R.id.re_album);

        // 메모리 RecyclerView 설정 (가로 스크롤, 호리젠탈)
        memoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        // 앨범 RecyclerView 설정 (세로 스크롤, 버티컬)
        albumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        // 메모리 테스트 데이터
        List<MemoryData> items = new ArrayList<>();
        items.add(new MemoryData("회의", "2025.10.15", "Room 6-205", "12:00", "13:05", R.drawable.mypage));
        items.add(new MemoryData("스터디", "2025.10.20", "Room 3-102", "15:00", "17:00", R.drawable.mypage));
        items.add(new MemoryData("약속", "2025.10.22", "Room 7-301", "18:30", "20:00", R.drawable.mypage));

        // 앨범 테스트 데이터
        List<AlbumData> items2 = new ArrayList<>();
        items2.add(new AlbumData("테스트 이름1", R.drawable.sample1, R.drawable.sample3));
        items2.add(new AlbumData("테스트 이름2", R.drawable.sample1, R.drawable.sample3));
        items2.add(new AlbumData("테스트 이름3", R.drawable.sample1, R.drawable.sample3));

        MemoryAdapter adapter = new MemoryAdapter(items);
        memoryView.setAdapter(adapter);

        // 스크롤 후 가장 중심에 있는 아이템을 자동 정렬해주는 SnapHelper 추가
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(memoryView);

        AlbumAdapter adapter2 = new AlbumAdapter(items2);
        albumView.setAdapter(adapter2);
    }
}
