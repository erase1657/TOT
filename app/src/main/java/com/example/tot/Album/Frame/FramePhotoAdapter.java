package com.example.tot.Album.Frame;

import java.util.*;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;

import com.example.tot.Album.AlbumDTO;
import com.example.tot.Community.PhotoFullscreenFragment;
import com.example.tot.R;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

public class FramePhotoAdapter extends RecyclerView.Adapter<FramePhotoAdapter.ViewHolder> {

    private List<AlbumDTO> photos;

    public FramePhotoAdapter(List<AlbumDTO> photos) {
        this.photos = photos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_frame_photo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {

        // 화면 너비 가져오기
        int screenWidth = h.itemView.getContext().getResources().getDisplayMetrics().widthPixels;

        // 4열 → 각 사진의 폭
        int size = screenWidth / 4;

        // 아이템 크기 강제 적용
        ViewGroup.LayoutParams params = h.imgPhoto.getLayoutParams();
        params.width = size;
        params.height = size;
        h.imgPhoto.setLayoutParams(params);

        // 이미지 로드
        AlbumDTO dto = photos.get(pos);
        Glide.with(h.itemView.getContext())
                .load(dto.getImageUrl())
                .centerCrop()
                .into(h.imgPhoto);

        // ✅ 사진 클릭 시 확대 화면 표시
        h.imgPhoto.setOnClickListener(v -> {
            ArrayList<String> photoUrls = new ArrayList<>();
            for (AlbumDTO photo : photos) {
                photoUrls.add(photo.getImageUrl());
            }

            if (h.itemView.getContext() instanceof FragmentActivity) {
                PhotoFullscreenFragment fragment = PhotoFullscreenFragment.newInstance(photoUrls, pos, true);
                fragment.show(((FragmentActivity) h.itemView.getContext()).getSupportFragmentManager(), "photo_fullscreen");
            }
        });
    }

    @Override
    public int getItemCount() {
        return photos != null ? photos.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        ViewHolder(View v) {
            super(v);
            imgPhoto = v.findViewById(R.id.imgPhoto);
        }
    }
}