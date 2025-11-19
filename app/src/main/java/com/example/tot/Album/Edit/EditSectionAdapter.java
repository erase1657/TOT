package com.example.tot.Album.Edit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;

import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class EditSectionAdapter extends RecyclerView.Adapter<EditSectionAdapter.ViewHolder> {

    private List<String> dateList;
    private Map<String, List<AlbumDTO>> photoMap;
    private Context context;
    private ItemTouchHelper touchHelper;
    private AddPhotoListener listener;
    public EditSectionAdapter(List<String> dateList, Map<String, List<AlbumDTO>> photoMap, Context context,AddPhotoListener listener) {
        this.dateList = dateList;
        this.photoMap = photoMap;
        this.context = context;
        this.listener = listener;
    }
    public interface AddPhotoListener {
        void onAddPhoto(String date);
    }
    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
    }
    @NonNull
    @Override
    public EditSectionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_edit_section, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EditSectionAdapter.ViewHolder h, int position) {

        String date = dateList.get(position);
        h.tvDate.setText(date);

        List<AlbumDTO> photoList = photoMap.get(date);
        EditPhotoAdapter adapter = new EditPhotoAdapter(photoList);

        h.rvPhotos.setLayoutManager(new LinearLayoutManager(context));
        h.rvPhotos.setAdapter(adapter);
        // 날짜 기반 사진 추가
        h.btnAddPhoto.setOnClickListener(v -> listener.onAddPhoto(date));
        // 🔥 ItemTouchHelper 연결
        ItemTouchHelper helper = new ItemTouchHelper(new PhotoTouchHelperCallback(adapter));
        helper.attachToRecyclerView(h.rvPhotos);

        adapter.setItemTouchHelper(helper);
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate;
        RecyclerView rvPhotos;
        Button btnAddPhoto;

        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_edit_date);
            rvPhotos = v.findViewById(R.id.rv_edit_photo_list);
            btnAddPhoto = v.findViewById(R.id.btn_add_photo);
        }
    }
}
