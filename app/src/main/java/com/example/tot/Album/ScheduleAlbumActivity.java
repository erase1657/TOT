package com.example.tot.Album; // 본인의 패키지 이름

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager; // (중요) import 확인
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tot.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScheduleAlbumActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleAlbumActivity";

    private RecyclerView rvAlbumPhotos;
    private Button btnAddPhoto;
    private ScheduleAlbumAdapter adapter;
    private List<AlbumEntry> albumEntries = new ArrayList<>();

    private FirebaseFirestore db;
    private StorageReference storageRootRef;
    private CollectionReference albumCollectionRef;

    private String currentScheduleId;

    // --- ✅ 1. 순서 변경: galleryLauncher를 먼저 선언 ---
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(), // 여러 개 선택
            uris -> {
                // 사용자가 이미지를 선택했을 때 호출됨
                if (uris != null && !uris.isEmpty()) {
                    Log.d(TAG, "선택된 이미지 개수: " + uris.size());
                    for (Uri uri : uris) {
                        // 선택한 이미지를 Firebase Storage에 업로드
                        uploadImageToFirebaseStorage(uri);
                    }
                } else {
                    Log.d(TAG, "선택된 이미지가 없음");
                }
            }
    );

    // --- ✅ 2. 순서 변경: permissionLauncher를 나중에 선언 ---
    // (이제 permissionLauncher 내부에서 galleryLauncher를 참조해도 문제가 없습니다)
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // 권한이 승인되면 갤러리 실행
                    Log.d(TAG, "갤러리 권한 승인됨");
                    galleryLauncher.launch("image/*"); // (이제 에러 아님)
                } else {
                    // 권한이 거부된 경우
                    Log.w(TAG, "갤러리 권한 거부됨");
                    Toast.makeText(this, "갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // (주의) 'activity_schedule_album.xml' 레이아웃 파일이 있어야 합니다.
        // 이 파일이 없다면, 이전 답변을 참고하여 생성해주세요.
        setContentView(R.layout.activity_schedule_album);

        // (중요!) 스케줄 ID 가져오기
        // (ScheduleFragment에서 이 Activity를 시작시킬 때 ID를 넘겨줘야 합니다)
        // 예:
        // Intent intent = new Intent(getActivity(), ScheduleAlbumActivity.class);
        // intent.putExtra("scheduleId", "여기에_스케줄_ID_넣기");
        // startActivity(intent);
        currentScheduleId = getIntent().getStringExtra("scheduleId");

        // (테스트용 임시 ID) - 실제로는 위 Intent 로직을 사용해야 합니다.
        if (currentScheduleId == null) {
            currentScheduleId = "test-schedule-123";
            Log.w(TAG, "임시 스케줄 ID 사용: test-schedule-123. (실제 구현 시 Intent로 받아야 함)");
        }

        // --- UI 초기화 ---
        rvAlbumPhotos = findViewById(R.id.rv_album_photos);
        btnAddPhoto = findViewById(R.id.btn_add_photo);

        // --- Firebase 초기화 ---
        db = FirebaseFirestore.getInstance();
        storageRootRef = FirebaseStorage.getInstance().getReference();

        // Firestore 경로 설정
        albumCollectionRef = db.collection("schedules")
                .document(currentScheduleId)
                .collection("album");

        // --- 어댑터 설정 ---
        setupRecyclerView();

        // 1. DB에서 기존 앨범 데이터 불러오기
        loadAlbumEntriesFromFirestore();

        // "사진 추가" 버튼 리스너
        btnAddPhoto.setOnClickListener(v -> checkGalleryPermissionAndOpen());
    }

    /**
     * 1. Firestore에서 앨범 데이터를 로드합니다.
     */
    private void loadAlbumEntriesFromFirestore() {
        albumCollectionRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "불러올 앨범 데이터가 없습니다.");
                        return;
                    }
                    albumEntries.clear();
                    albumEntries.addAll(querySnapshot.toObjects(AlbumEntry.class));
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "총 " + albumEntries.size() + "개의 앨범 항목을 불러왔습니다.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "앨범 데이터 로드 실패", e);
                    Toast.makeText(this, "데이터 로딩 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 2. Storage 업로드 완료 후, Firestore에 '저장'을 요청합니다.
     */
    private void uploadImageToFirebaseStorage(Uri imageUri) {
        if (imageUri == null) return;

        Toast.makeText(this, "사진 업로드 시작...", Toast.LENGTH_SHORT).show();

        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = storageRootRef
                .child("schedule_albums")
                .child(currentScheduleId)
                .child(fileName);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String downloadUrl = downloadUri.toString();
                        Log.d(TAG, "업로드 성공, URL: " + downloadUrl);
                        AlbumEntry newEntry = new AlbumEntry(downloadUrl);
                        saveAlbumEntryToFirestore(newEntry);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "업로드 실패", e);
                    Toast.makeText(this, "사진 업로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 3. 새 앨범 항목을 Firestore에 저장합니다.
     */
    private void saveAlbumEntryToFirestore(AlbumEntry newEntry) {
        albumCollectionRef.add(newEntry)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    Log.d(TAG, "Firestore 저장 성공, ID: " + docId);

                    newEntry.setDocumentId(docId);
                    albumEntries.add(newEntry);
                    adapter.notifyItemInserted(albumEntries.size() - 1);
                    rvAlbumPhotos.smoothScrollToPosition(albumEntries.size() - 1);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore 저장 실패", e);
                    Toast.makeText(this, "사진 정보 저장 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * (A) 권한 확인 로직
     */
    private void checkGalleryPermissionAndOpen() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "권한 이미 있음. 갤러리 실행.");
            galleryLauncher.launch("image/*");
        } else {
            Log.d(TAG, "권한 없음. 권한 요청.");
            permissionLauncher.launch(permission);
        }
    }

    /**
     * (B) RecyclerView 설정
     */
    private void setupRecyclerView() {
        adapter = new ScheduleAlbumAdapter(albumEntries);
        rvAlbumPhotos.setAdapter(adapter);
        rvAlbumPhotos.setLayoutManager(new LinearLayoutManager(this)); // (중요) LayoutManager 설정
    }

    // --- 어댑터 및 ViewHolder 클래스 (이전과 동일) ---
    private class ScheduleAlbumAdapter extends RecyclerView.Adapter<ScheduleAlbumAdapter.AlbumViewHolder> {

        private List<AlbumEntry> items;

        public ScheduleAlbumAdapter(List<AlbumEntry> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // (주의) 'item_schedule_album_photo.xml' 레이아웃 파일이 필요합니다.
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_schedule_album_photo, parent, false);
            return new AlbumViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
            AlbumEntry entry = items.get(position);
            holder.bind(entry);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // --- ViewHolder ---
        class AlbumViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView imgPhoto;
            EditText etComment;
            ImageView btnEditComment;
            private CustomTextWatcher textWatcher;
            private final Handler commentHandler = new Handler(Looper.getMainLooper());

            public AlbumViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPhoto = itemView.findViewById(R.id.img_photo);
                etComment = itemView.findViewById(R.id.et_comment);
                btnEditComment = itemView.findViewById(R.id.btn_edit_comment);

                this.textWatcher = new CustomTextWatcher();
                this.etComment.addTextChangedListener(textWatcher);
            }

            public void bind(AlbumEntry entry) {
                Glide.with(itemView.getContext())
                        .load(entry.getImageUri())
                        .placeholder(R.drawable.sample2) // 로딩 중 이미지
                        .error(R.drawable.sample4) // 에러 시 이미지
                        .into(imgPhoto);

                textWatcher.setEnabled(false);
                etComment.setText(entry.getComment());
                textWatcher.setEnabled(true);

                textWatcher.setEntry(entry, commentHandler);

                btnEditComment.setOnClickListener(v -> etComment.requestFocus());
            }
        }

        // --- TextWatcher (코멘트 자동 저장) ---
        private class CustomTextWatcher implements TextWatcher {
            private AlbumEntry entry;
            private Handler handler;
            private boolean isEnabled = true;

            public void setEntry(AlbumEntry entry, Handler handler) {
                this.entry = entry;
                this.handler = handler;
            }

            public void setEnabled(boolean enabled) { isEnabled = enabled; }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isEnabled || entry == null || entry.getDocumentId() == null || handler == null) {
                    return;
                }

                String newComment = s.toString();
                entry.setComment(newComment); // 로컬 모델 즉시 업데이트

                // Debouncing: 1초간 입력 없으면 Firestore에 저장
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> {
                    if (entry.getDocumentId() == null) return;

                    albumCollectionRef.document(entry.getDocumentId())
                            .update("comment", newComment)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "코멘트 업데이트 성공: " + entry.getDocumentId()))
                            .addOnFailureListener(e -> Log.e(TAG, "코멘트 업데이트 실패", e));
                }, 1000); // 1초 딜레이
            }
        }
    }
}