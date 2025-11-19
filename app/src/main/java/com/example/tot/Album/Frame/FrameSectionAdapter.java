package com.example.tot.Album.Frame;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;

import java.util.List;
import java.util.Map;

import android.widget.TextView;

public class FrameSectionAdapter extends RecyclerView.Adapter<FrameSectionAdapter.ViewHolder> {



    private List<String> dateList;                  // 날짜 리스트
    private Map<String, List<AlbumDTO>> photoMap;   // 날짜별 사진 리스트
    private Context context;


    public FrameSectionAdapter(List<String> dateList,
                               Map<String, List<AlbumDTO>> photoMap,
                               Context context) {

        this.dateList = dateList;
        this.photoMap = photoMap;
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_frame_section, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {

        // 날짜 바인딩
        String date = dateList.get(pos);
        h.tvDate.setText(date);
        // 화면 너비 가져오기
        int screenWidth = h.itemView.getContext().getResources().getDisplayMetrics().widthPixels;

        // 4열 → 각 사진의 폭
        int size = screenWidth / 4;

        // 사진 리스트 가져오기
        List<AlbumDTO> photoList = photoMap.get(date);
        if (photoList == null) {
            photoList = new java.util.ArrayList<>();
        }

        // 사진 그리드
        FramePhotoAdapter adapter = new FramePhotoAdapter(photoList);
        h.rvPhotoGrid.setLayoutManager(new GridLayoutManager(context, 4, RecyclerView.VERTICAL, false));
        h.rvPhotoGrid.setAdapter(adapter);
        h.rvPhotoGrid.setNestedScrollingEnabled(false);
        h.rvPhotoGrid.setHasFixedSize(true);
        // 날짜 기반 사진 추가
        /*h.btnAddPhoto.setOnClickListener(v -> listener.onAddPhoto(date));*/
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        RecyclerView rvPhotoGrid;


        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_date);
            rvPhotoGrid = v.findViewById(R.id.rv_photo_list);

        }
    }
}
