package com.example.tot.Album.Edit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class EditSectionAdapter extends RecyclerView.Adapter<EditSectionAdapter.ViewHolder> {

    private final List<String> dateList;
    private final Map<String, List<AlbumDTO>> photoMap;
    private final Context context;
    private final String userUid;
    private final String scheduleId;
    private AddPhotoListener addListener;
    private CommentEditListener commentListener;
    private EditPhotoAdapter.FirestoreDeleteProvider deleteProvider;

    public interface AddPhotoListener {
        void onAddPhoto(String date);
    }

    public interface CommentEditListener {
        void onEditComment(String dateKey, AlbumDTO dto, int position);
    }

    public EditSectionAdapter(List<String> dateList,
                              Map<String, List<AlbumDTO>> photoMap,
                              Context context,
                              AddPhotoListener listener,
                              String userUid,
                              String scheduleId) {
        this.dateList = dateList;
        this.photoMap = photoMap;
        this.context = context;
        this.addListener = listener;
        this.userUid = userUid;
        this.scheduleId = scheduleId;
    }

    public void setCommentEditListener(CommentEditListener listener) {
        this.commentListener = listener;
    }

    public void setDeleteListener(EditPhotoAdapter.FirestoreDeleteProvider provider) {
        this.deleteProvider = provider;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_edit_section, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        String date = dateList.get(position);
        h.tvDate.setText(date);

        List<AlbumDTO> photoList = photoMap.get(date);

        if (h.photoAdapter == null) {  // 최초 1회만 생성
            h.photoAdapter = new EditPhotoAdapter(photoList);
            h.photoAdapter.setIndexUpdateListener(updatedList -> {
                for (int i = 0; i < updatedList.size(); i++) {
                    AlbumDTO dto = updatedList.get(i);

                    dto.setIndex(i);

                    FirebaseFirestore.getInstance()
                            .collection("user")
                            .document(userUid)
                            .collection("schedule")
                            .document(scheduleId)
                            .collection("scheduleDate")
                            .document(dto.getDateKey())
                            .collection("album")
                            .document(dto.getPhotoId())
                            .update("index", i);
                }
            });
            h.photoAdapter.setCommentEditListener((dto, pos) -> {
                if (commentListener != null)
                    commentListener.onEditComment(date, dto, pos);
            });

            h.photoAdapter.setFirestoreDeleteProvider(deleteProvider);

            h.rvPhotos.setLayoutManager(new LinearLayoutManager(context));
            h.rvPhotos.setAdapter(h.photoAdapter);

            ItemTouchHelper helper =
                    new ItemTouchHelper(new PhotoTouchHelperCallback(h.photoAdapter));
            helper.attachToRecyclerView(h.rvPhotos);

            h.photoAdapter.setItemTouchHelper(helper);
            h.helper = helper;

        } else {
            h.photoAdapter.updatePhotos(photoList);
        }

        h.btnAddPhoto.setOnClickListener(v -> addListener.onAddPhoto(date));
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate;
        RecyclerView rvPhotos;
        Button btnAddPhoto;

        EditPhotoAdapter photoAdapter;
        ItemTouchHelper helper;

        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_edit_date);
            rvPhotos = v.findViewById(R.id.rv_edit_photo_list);
            btnAddPhoto = v.findViewById(R.id.btn_add_photo);
        }
    }
}
