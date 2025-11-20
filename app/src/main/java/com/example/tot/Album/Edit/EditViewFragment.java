package com.example.tot.Album.Edit;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditViewFragment extends Fragment implements
        EditSectionAdapter.AddPhotoListener,
        EditSectionAdapter.CommentEditListener {

    private RecyclerView rvEditSections;
    private ArrayList<String> dateList;
    private String scheduleId, userUid;

    private final Map<String, List<AlbumDTO>> photoMap = new HashMap<>();
    private String currentDateForPhoto;

    private EditSectionAdapter adapter;
    private ActivityResultLauncher<String> galleryLauncher;

    private FirebaseFirestore db;
    private StorageReference storageRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        if (getArguments() != null) {
            dateList = getArguments().getStringArrayList("dateList");
            scheduleId = getArguments().getString("scheduleId");
            userUid = getArguments().getString("userUid");
        }

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty() && currentDateForPhoto != null) {
                        for (Uri uri : uris) uploadPhotoToStorage(uri, currentDateForPhoto);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_album_edit_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {

        rvEditSections = v.findViewById(R.id.rv_edit_sections);

        adapter = new EditSectionAdapter(
                dateList,
                photoMap,
                getContext(),
                this,
                userUid,
                scheduleId
        );
        adapter.setCommentEditListener(this);

        rvEditSections.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEditSections.setAdapter(adapter);

        adapter.setDeleteListener((dto, onSuccess, onFail) -> {
            db.collection("user")
                    .document(userUid)
                    .collection("schedule")
                    .document(scheduleId)
                    .collection("scheduleDate")
                    .document(dto.getDateKey())
                    .collection("album")
                    .document(dto.getPhotoId())
                    .delete()
                    .addOnSuccessListener(unused -> onSuccess.run())
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreDelete", "삭제 실패: " + e.getMessage());
                        onFail.run();
                    });
        });

        initPhotoLists();
        loadPhotosFromFirestore();
    }

    private void initPhotoLists() {
        for (String date : dateList) photoMap.put(date, new ArrayList<>());
    }

    private void uploadPhotoToStorage(Uri uri, String dateKey) {

        String fileName = System.currentTimeMillis() + ".jpg";

        StorageReference fileRef =
                storageRef.child("album").child(userUid).child(scheduleId).child(dateKey).child(fileName);

        fileRef.putFile(uri).addOnSuccessListener(task ->
                fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    savePhotoToFirestore(dateKey, downloadUri.toString());
                })
        );
    }

    private void savePhotoToFirestore(String dateKey, String downloadUrl) {

        DocumentReference doc = db.collection("user")
                .document(userUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album")
                .document();

        String photoId = doc.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl", downloadUrl);
        data.put("comment", "");
        data.put("index", photoMap.get(dateKey).size());
        data.put("createdAt", Timestamp.now());

        doc.set(data).addOnSuccessListener(a -> {
            photoMap.get(dateKey).add(
                    new AlbumDTO(photoId, downloadUrl, "", photoMap.get(dateKey).size(), dateKey)
            );

            int idx = dateList.indexOf(dateKey);
            adapter.notifyItemChanged(idx);
        });
    }

    private void loadPhotosFromFirestore() {

        for (String dateKey : dateList) {
            db.collection("user")
                    .document(userUid)
                    .collection("schedule")
                    .document(scheduleId)
                    .collection("scheduleDate")
                    .document(dateKey)
                    .collection("album")
                    .orderBy("index")
                    .get()
                    .addOnSuccessListener(snap -> {
                        List<AlbumDTO> list = new ArrayList<>();
                        snap.forEach(doc -> list.add(
                                new AlbumDTO(
                                        doc.getId(),
                                        doc.getString("imageUrl"),
                                        doc.getString("comment"),
                                        doc.getLong("index").intValue(),
                                        dateKey
                                )
                        ));

                        photoMap.put(dateKey, list);

                        int idx = dateList.indexOf(dateKey);
                        adapter.notifyItemChanged(idx);
                    });
        }
    }

    @Override
    public void onAddPhoto(String date) {
        currentDateForPhoto = date;
        galleryLauncher.launch("image/*");
    }

    @Override
    public void onEditComment(String dateKey, AlbumDTO dto, int position) {
        EditCommentDialog dialog = new EditCommentDialog(
                requireContext(),
                dto.getComment(),
                newComment -> {

                    dto.setComment(newComment);

                    int idx = dateList.indexOf(dateKey);
                    adapter.notifyItemChanged(idx);

                    db.collection("user")
                            .document(userUid)
                            .collection("schedule")
                            .document(scheduleId)
                            .collection("scheduleDate")
                            .document(dateKey)
                            .collection("album")
                            .document(dto.getPhotoId())
                            .update("comment", newComment);
                }
        );

        dialog.show();
    }
}
