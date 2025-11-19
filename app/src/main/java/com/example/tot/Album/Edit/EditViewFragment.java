package com.example.tot.Album.Edit;



import android.net.Uri;
import android.os.Bundle;
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

public class EditViewFragment extends Fragment implements EditSectionAdapter.AddPhotoListener {

    private RecyclerView rvEditSections;
    private ArrayList<String> dateList;
    private String scheduleId, userUid;

    private Map<String, List<AlbumDTO>> photoMap = new HashMap<>();
    private String currentDateForPhoto;

    private EditSectionAdapter adapter;
    private ActivityResultLauncher<String> galleryLauncher;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        if (getArguments() != null) {
            dateList = getArguments().getStringArrayList("dateList");
            scheduleId = getArguments().getString("scheduleId");
            userUid = getArguments().getString("userUid");
        }

        // 여러 장 선택 가능
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty() && currentDateForPhoto != null) {
                        for (Uri uri : uris) {
                            uploadPhotoToStorage(uri, currentDateForPhoto);
                        }
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

        adapter = new EditSectionAdapter(dateList, photoMap, getContext(), this);
        rvEditSections.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEditSections.setAdapter(adapter);

        initPhotoLists();
    }

    // 날짜별 리스트 초기화
    private void initPhotoLists() {
        for (String date : dateList) {
            photoMap.put(date, new ArrayList<>());
        }
    }

    // -------------------- 🔥 Storage 업로드 + Firestore 저장 --------------------
    private void uploadPhotoToStorage(Uri uri, String dateKey) {

        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference fileRef =
                storageRef.child("album")
                        .child(userUid)
                        .child(scheduleId)
                        .child(dateKey)
                        .child(fileName);

        fileRef.putFile(uri)
                .addOnSuccessListener(task ->
                        fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            savePhotoToFirestore(dateKey, downloadUri.toString());
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void savePhotoToFirestore(String dateKey, String downloadUrl) {

        DocumentReference doc = db.collection("user")
                .document(userUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("album")
                .document(dateKey)
                .collection("photos")
                .document();

        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl", downloadUrl);
        data.put("comment", "");
        data.put("index", photoMap.get(dateKey).size());
        data.put("createdAt", Timestamp.now());

        doc.set(data)
                .addOnSuccessListener(unused -> {
                    photoMap.get(dateKey).add(new AlbumDTO(downloadUrl, "", photoMap.get(dateKey).size()));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onAddPhoto(String date) {
        currentDateForPhoto = date;
        galleryLauncher.launch("image/*");
    }
}
