package com.example.tot.ViewPager;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
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
    public HomeFragment(){
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView MemoryView = view.findViewById(R.id.re_memory);
        RecyclerView AlbumView = view.findViewById(R.id.re_album);
        MemoryView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        AlbumView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        List<MemoryData> items = new ArrayList<>();
        List<AlbumData> items2 = new ArrayList<>();
        items.add(new MemoryData( R.drawable.sample2,"테스트 지역1", "테스트 장소1"));
        items.add(new MemoryData( R.drawable.sample2,"테스트 지역2", "테스트 장소2"));
        items.add(new MemoryData( R.drawable.sample2,"테스트 지역3", "테스트 장소3"));
        items.add(new MemoryData( R.drawable.sample2,"테스트 지역4", "테스트 장소4"));
        items.add(new MemoryData( R.drawable.sample2,"테스트 지역5", "테스트 장소5"));
        items.add(new MemoryData( R.drawable.sample2,"테스트 지역6", "테스트 장소6"));
        items.add(new MemoryData( R.drawable.sample2,"테스트 지역7", "테스트 장소7"));

        items2.add(new AlbumData("테스트 이름1", R.drawable.sample1, R.drawable.sample3));
        items2.add(new AlbumData("테스트 이름2", R.drawable.sample1, R.drawable.sample3));
        items2.add(new AlbumData("테스트 이름3", R.drawable.sample1, R.drawable.sample3));
        items2.add(new AlbumData("테스트 이름4", R.drawable.sample1, R.drawable.sample3));
        items2.add(new AlbumData("테스트 이름5", R.drawable.sample1, R.drawable.sample3));
        items2.add(new AlbumData("테스트 이름6", R.drawable.sample1, R.drawable.sample3));
        MemoryAdapter adapter = new MemoryAdapter(items);
        MemoryView.setAdapter(adapter);

        AlbumAdapter adapter2 = new AlbumAdapter(items2);
        AlbumView.setAdapter(adapter2);

    }
}
