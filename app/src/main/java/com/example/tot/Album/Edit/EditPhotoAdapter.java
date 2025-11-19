package com.example.tot.Album.Edit;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class EditPhotoAdapter extends RecyclerView.Adapter<EditPhotoAdapter.ViewHolder> {

    private List<AlbumDTO> photos;
    private ItemTouchHelper touchHelper;
    public EditPhotoAdapter(List<AlbumDTO> photos) {
        this.photos = photos;
    }
    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
    }
    public List<AlbumDTO> getPhotos() {
        return photos;
    }

    @NonNull
    @Override
    public EditPhotoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_edit_photo, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull EditPhotoAdapter.ViewHolder holder, int position) {
        AlbumDTO dto = photos.get(position);

        Glide.with(holder.itemView.getContext())
                .load(dto.getImageURI())
                .centerCrop()
                .into(holder.imgPhoto);
        holder.tvComment.setText(dto.getComment());
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (touchHelper != null) {
                    touchHelper.startDrag(holder);
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return photos == null ? 0 : photos.size();
    }

    // 🔥 ItemTouchHelper가 호출하는 필수 메서드
    public void onItemDismiss(int position) {
        photos.remove(position);
        notifyItemRemoved(position);
    }

    public void onItemMove(int from, int to) {
        Collections.swap(photos, from, to);
        notifyItemMoved(from, to);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        ImageView dragHandle;
        TextView tvComment;
        Button btnEditComment;
        ViewHolder(View v) {
            super(v);
            imgPhoto = v.findViewById(R.id.img_edit_photo);
            dragHandle = v.findViewById(R.id.btn_drag_handle);
            tvComment = v.findViewById(R.id.tv_comment);
            btnEditComment = v.findViewById(R.id.btn_edit_comment);
        }
    }
}
