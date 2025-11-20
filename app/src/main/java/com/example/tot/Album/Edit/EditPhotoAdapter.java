package com.example.tot.Album.Edit;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;

import java.util.Collections;
import java.util.List;

public class EditPhotoAdapter extends RecyclerView.Adapter<EditPhotoAdapter.ViewHolder> {

    private List<AlbumDTO> photos;
    private ItemTouchHelper touchHelper;

    private CommentEditListener commentListener;
    private FirestoreDeleteProvider deleteProvider;
    private FirestoreIndexUpdateListener indexUpdateListener;
    public interface CommentEditListener {
        void onEditComment(AlbumDTO dto, int position);
    }

    public interface FirestoreDeleteProvider {
        void deletePhoto(AlbumDTO dto, Runnable onSuccess, Runnable onFail);
    }
    public interface FirestoreIndexUpdateListener {
        void onUpdateIndexes(List<AlbumDTO> updatedList);
    }

    public EditPhotoAdapter(List<AlbumDTO> photos) {
        this.photos = photos;
    }

    public void updatePhotos(List<AlbumDTO> newList) {
        this.photos = newList;
        notifyDataSetChanged();
    }
    public void setIndexUpdateListener(FirestoreIndexUpdateListener l) {
        this.indexUpdateListener = l;
    }
    public void onDragFinished() {
        if (indexUpdateListener != null) {
            indexUpdateListener.onUpdateIndexes(photos);
        }
    }
    public void setCommentEditListener(CommentEditListener listener) {
        this.commentListener = listener;
    }

    public void setFirestoreDeleteProvider(FirestoreDeleteProvider provider) {
        this.deleteProvider = provider;
    }

    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
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
                .load(dto.getImageUrl())
                .centerCrop()
                .into(holder.imgPhoto);

        holder.tvComment.setText(dto.getComment());

        holder.btnEditComment.setOnClickListener(v -> {
            if (commentListener != null)
                commentListener.onEditComment(dto, holder.getBindingAdapterPosition());
        });

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && touchHelper != null) {
                touchHelper.startDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public void onItemDismiss(int position) {

        if (position < 0 || position >= photos.size()) return;

        AlbumDTO target = photos.get(position);

        photos.remove(position);
        notifyItemRemoved(position);

        if (deleteProvider != null) {
            deleteProvider.deletePhoto(
                    target,
                    () -> {},
                    () -> {
                        photos.add(position, target);
                        notifyItemInserted(position);
                    }
            );
        }
    }

    public void onItemMove(int from, int to) {
        Collections.swap(photos, from, to);
        notifyItemMoved(from, to);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto, dragHandle;
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
