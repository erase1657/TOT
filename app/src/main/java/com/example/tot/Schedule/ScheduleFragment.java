package com.example.tot.Schedule;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.example.tot.Schedule.ScheduleSetting.ScheduleSettingActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ScheduleFragment extends Fragment {

    private Timestamp startDate;
    private Timestamp endDate;


    private RecyclerView recyclerView;
    private ScheduleAdapter scheduleAdapter;
    private List<ScheduleDTO> scheduleList;
    private LinearLayout noScheduleLayout;
    private TextView tabMy, tabInvited;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private ListenerRegistration scheduleListener;
    private ImageButton addScheduleButton;
    private String selectedDateRange = "";
    private int editingPosition = -1;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    public ScheduleFragment() {
        super(R.layout.fragment_schedule);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null && editingPosition != -1) {
                // 변경된 부분: 로컬 URI를 직접 사용하지 않고 업로드 함수 호출
                uploadImageAndUpdateSchedule(editingPosition, uri);
            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rv_schedules);
        noScheduleLayout = view.findViewById(R.id.layout_no_schedule);
        addScheduleButton = view.findViewById(R.id.btn_add_schedule);

        tabMy = view.findViewById(R.id.tab_my_schedule);
        tabInvited = view.findViewById(R.id.tab_invited_schedule);
        setupRecyclerView();
        setupTabs();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance(); // Firebase Storage 초기화

        addScheduleButton.setOnClickListener(v -> showCreateScheduleDialog());
        setTabSelected(true);
        loadMySchedules();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (scheduleListener != null) {
            scheduleListener.remove();
            scheduleListener = null;
        }
    }
    private void setupTabs() {
        tabMy.setOnClickListener(v -> {
            setTabSelected(true);
            loadMySchedules();
        });

        tabInvited.setOnClickListener(v -> {
            setTabSelected(false);
            loadInvitedSchedules();
        });
    }
    private void setTabSelected(boolean isMy) {
        tabMy.setTextColor(isMy ? 0xFF303748 : 0xFFB0B2B8);
        tabInvited.setTextColor(!isMy ? 0xFF303748 : 0xFFB0B2B8);
        addScheduleButton.setVisibility(isMy ? View.VISIBLE : View.GONE);
    }
    private void loadMySchedules() {

        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        // 기존 리스너 제거
        if (scheduleListener != null) scheduleListener.remove();

        scheduleListener = db.collection("user")
                .document(uid)
                .collection("schedule")
                .addSnapshotListener((snapshot, e) -> {

                    if (e != null || snapshot == null) return;

                    List<ScheduleDTO> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ScheduleDTO dto = doc.toObject(ScheduleDTO.class);
                        if (dto != null) {
                            dto.setScheduleId(doc.getId());
                            dto.setOwnerUid(uid);
                            dto.setShared(false);
                            list.add(dto);
                        }
                    }

                    updateScheduleUI(list);
                });
    }
    private void updateScheduleUI(List<ScheduleDTO> list) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            scheduleAdapter.updateData(list);

            if (list.isEmpty()) noScheduleLayout.setVisibility(View.VISIBLE);
            else noScheduleLayout.setVisibility(View.GONE);
        });
    }

    // -----------------------------------------------------------
    // 🔹 2) 초대받은 스케줄 불러오기
    // -----------------------------------------------------------
    private void loadInvitedSchedules() {

        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        if (scheduleListener != null) scheduleListener.remove();

        scheduleListener = db.collection("user")
                .document(uid)
                .collection("sharedSchedule")
                .addSnapshotListener((snapshot, e) -> {

                    if (snapshot == null || e != null) {
                        updateScheduleUI(new ArrayList<>());
                        return;
                    }

                    List<ScheduleDTO> invitedList = new ArrayList<>();

                    int total = snapshot.size();
                    if (total == 0) {
                        updateScheduleUI(invitedList);
                        return;
                    }

                    final int[] loadedCount = {0};

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        DocumentReference ref = doc.getDocumentReference("scheduleRef");
                        String ownerUid = doc.getString("ownerUid");

                        if (ref == null) {
                            loadedCount[0]++;
                            if (loadedCount[0] == total) {
                                updateScheduleUI(invitedList);
                            }
                            continue;
                        }

                        ref.get().addOnSuccessListener(scheduleDoc -> {

                            ScheduleDTO dto = scheduleDoc.toObject(ScheduleDTO.class);

                            if (dto != null) {
                                dto.setScheduleId(scheduleDoc.getId());
                                dto.setOwnerUid(ownerUid);
                                dto.setShared(true);
                                invitedList.add(dto);
                            }

                            loadedCount[0]++;

                            // ⭐ 모든 문서 로딩이 끝난 후 단 한 번 UI 갱신
                            if (loadedCount[0] == total) {
                                updateScheduleUI(invitedList);
                            }
                        });
                    }
                });
    }


    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        scheduleList = new ArrayList<>();

        scheduleAdapter = new ScheduleAdapter(scheduleList, (schedule, position) -> {
            Intent intent = new Intent(getContext(), ScheduleSettingActivity.class);
            intent.putExtra("scheduleId", schedule.getScheduleId());
            intent.putExtra("ownerUid", schedule.getOwnerUid());
            intent.putExtra("isShared", schedule.isShared());
            intent.putExtra("startMillisUtc", schedule.getStartDate().toDate().getTime());
            intent.putExtra("endMillisUtc", schedule.getEndDate().toDate().getTime());            startActivity(intent);
        });

        scheduleAdapter.setOnMenuItemClickListener(new ScheduleAdapter.OnMenuItemClickListener() {
            @Override
            public void onChangeBackgroundClick(ScheduleDTO schedule, int position) {
                editingPosition = position;
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }

            @Override
            public void onDeleteClick(ScheduleDTO schedule, int position) {
                showDeleteConfirmDialog(schedule, position);
            }

            @Override
            public void onEditTitleClick(ScheduleDTO schedule, int position) {
                showEditTitleDialog(schedule, position);
            }
        });

        recyclerView.setAdapter(scheduleAdapter);
    }

    // 이미지를 Storage에 업로드하고 Firestore 정보를 업데이트하는 통합 메서드
    private void uploadImageAndUpdateSchedule(int position, Uri imageUri) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        ScheduleDTO schedule = scheduleList.get(position);
        String scheduleId = schedule.getScheduleId();

        Toast.makeText(getContext(), "이미지를 업로드 중입니다...", Toast.LENGTH_SHORT).show();

        // Firebase Storage 경로 설정 (유저UID/스케줄ID.jpg)
        StorageReference imageRef = storage.getReference().child("schedule_backgrounds/" + uid + "/" + scheduleId + ".jpg");

        // 이미지 업로드 실행
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // 업로드 성공 시, 다운로드 URL 가져오기
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String imageUrl = downloadUri.toString();

                        // Firestore 문서 업데이트
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("backgroundImageUri", imageUrl);

                        db.collection("user").document(uid).collection("schedule").document(scheduleId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // 로컬 데이터 업데이트 및 UI 갱신
                                    schedule.setBackgroundImageUri(imageUrl);
                                    scheduleAdapter.updateScheduleItem(position, schedule);
                                    Toast.makeText(getContext(), "배경 이미지가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "데이터베이스 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    Log.e("ScheduleFragment", "Error updating Firestore", e);
                                });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "이미지 URL 가져오기에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        Log.e("ScheduleFragment", "Error getting download URL", e);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("ScheduleFragment", "Error uploading image", e);
                });
    }


    private void showDeleteConfirmDialog(ScheduleDTO schedule, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("스케줄 삭제")
                .setMessage("이 스케줄을 정말 삭제하시겠습니까? 관련된 모든 정보가 영구적으로 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteSchedule(schedule, position);
                })
                .setNegativeButton("취소", null)
                .show();
    }
    // ✅ 수정: ownerUid와 isShared를 고려한 삭제 메서드
    private void deleteSchedule(ScheduleDTO schedule, int position) {
        if (auth.getCurrentUser() == null) return;

        String currentUid = auth.getCurrentUser().getUid();
        String scheduleId = schedule.getScheduleId();
        String ownerUid = schedule.getOwnerUid();
        boolean isShared = schedule.isShared();

        // 공유받은 스케줄인 경우
        if (isShared) {
            Toast.makeText(getContext(), "공유받은 스케줄은 삭제할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 소유자가 아닌 경우
        if (!currentUid.equals(ownerUid)) {
            Toast.makeText(getContext(), "본인의 스케줄만 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference scheduleRef = db.collection("user")
                .document(ownerUid)
                .collection("schedule")
                .document(scheduleId);

        scheduleRef.collection("scheduleDate").get().addOnSuccessListener(dateSnapshot -> {
            WriteBatch batch = db.batch();
            List<Task<?>> tasks = new ArrayList<>();

            for (DocumentSnapshot dateDoc : dateSnapshot.getDocuments()) {

                // scheduleItem 삭제
                Task<?> itemTask = dateDoc.getReference().collection("scheduleItem").get()
                        .addOnSuccessListener(itemSnapshot -> {
                            for (DocumentSnapshot itemDoc : itemSnapshot.getDocuments()) {
                                batch.delete(itemDoc.getReference());
                                // 알람도 함께 삭제
                                db.collection("user").document(currentUid).collection("alarms")
                                        .document(itemDoc.getId()).delete();
                            }
                        });
                tasks.add(itemTask);

                // album 삭제
                Task<?> albumTask = dateDoc.getReference().collection("album").get()
                        .addOnSuccessListener(albumSnapshot -> {
                            for (DocumentSnapshot albumDoc : albumSnapshot.getDocuments()) {
                                batch.delete(albumDoc.getReference());
                            }
                        });
                tasks.add(albumTask);

                batch.delete(dateDoc.getReference());
            }

            // 모든 하위 조회 작업이 완료되면 실행
            Tasks.whenAllComplete(tasks).addOnSuccessListener(t -> {

                // 마지막에 스케줄 문서 삭제
                batch.delete(scheduleRef);

                batch.commit().addOnSuccessListener(aVoid -> {
                    // ✅ 추가: UI 업데이트 (리스너가 자동으로 처리하지만, 즉각 반영을 위해 추가)
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            scheduleAdapter.removeSchedule(position);
                            Toast.makeText(getContext(), "스케줄이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "삭제 실패", Toast.LENGTH_SHORT).show();
                    Log.e("ScheduleFragment", "Error deleting schedule", e);
                });
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "데이터 로딩 실패", Toast.LENGTH_SHORT).show();
            Log.e("ScheduleFragment", "Error loading schedule data", e);
        });
    }

    private void showCreateScheduleDialog() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_schedule, null);

        EditText etLocationName = dialogView.findViewById(R.id.et_location_name);
        RelativeLayout dateRangeBox = dialogView.findViewById(R.id.date_range_box);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tv_selected_date);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        Button btnPrev = dialogView.findViewById(R.id.btn_dialog_prev);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.Theme_TOT_RoundedDialog)
                .setView(dialogView)
                .create();

        dateRangeBox.setOnClickListener(v -> showGoogleDateRangePicker(tvSelectedDate));

        btnConfirm.setOnClickListener(v -> {
            String locationName = etLocationName.getText().toString();
            if (locationName.isEmpty()) {
                Toast.makeText(getContext(), "제목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDateRange.isEmpty()) {
                Toast.makeText(getContext(), "여행 기간을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            addNewSchedule(locationName);
            dialog.dismiss();
            Toast.makeText(getContext(), "스케줄이 생성되었습니다", Toast.LENGTH_SHORT).show();
        });

        btnPrev.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showGoogleDateRangePicker(TextView tvSelectedDate) {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_App_DatePicker)
                        .setTitleText("여행 기간을 선택하세요");

        builder.setSelection(Pair.create(
                MaterialDatePicker.todayInUtcMilliseconds(),
                MaterialDatePicker.todayInUtcMilliseconds()
        ));

        MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Long startDateMillis = selection.first;
            Long endDateMillis = selection.second;

            TimeZone timeZone = TimeZone.getDefault();
            long startOffset = timeZone.getOffset(startDateMillis);
            long endOffset = timeZone.getOffset(endDateMillis);

            Date CalStartDate = new Date(startDateMillis + startOffset);
            Date CalEndDate = new Date(endDateMillis + endOffset);

            startDate = new Timestamp(CalStartDate);
            endDate = new Timestamp(CalEndDate);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            String startDateStr = sdf.format(CalStartDate);
            String endDateStr = sdf.format(CalEndDate);

            long diffInMillis = (endDateMillis + endOffset) - (startDateMillis + startOffset);
            long nights = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            long days = nights + 1;

            selectedDateRange = String.format(Locale.getDefault(),
                    "%s~%s (%d박 %d일)", startDateStr, endDateStr, nights, days);

            tvSelectedDate.setText(selectedDateRange);
            tvSelectedDate.setTextColor(getResources().getColor(R.color.black));
        });

        datePicker.show(getParentFragmentManager(), "date_picker");
    }

    private void addNewSchedule(String locationName) {
        if (auth.getCurrentUser() == null) {
            Log.e("ScheduleFragment", "User is not logged in.");
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String scheduleId = generateScheduleId();

        if (startDate == null || endDate == null) {
            Toast.makeText(getContext(), "기간이 선택되지 않았습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        ScheduleDTO schedule = new ScheduleDTO(
                scheduleId,
                locationName,
                startDate,
                endDate,
                null,
                "",
                0,
                null
        );

        db.collection("user").document(uid)
                .collection("schedule").document(scheduleId)
                .set(schedule, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {

                    // 🔥 생성 직후 즉시 UI 반영
                    scheduleList.add(0, schedule);
                    scheduleAdapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                    String myUid = auth.getCurrentUser().getUid();
                    // 🔥 이후 화면 이동
                    Intent intent = new Intent(getContext(), ScheduleSettingActivity.class);
                    intent.putExtra("scheduleId", scheduleId);
                    intent.putExtra("ownerUid", myUid);
                    intent.putExtra("isShared", false);
                    intent.putExtra("startMillisUtc", startDate.toDate().getTime());  // 🔥 이름 통일
                    intent.putExtra("endMillisUtc", endDate.toDate().getTime());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "스케줄 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private String generateScheduleId() {
        String prefix = "SCDL_" + System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "_" + random;
    }

    private void listenSchedulesFromFirestore() {
        if (auth.getCurrentUser() == null) {
            Log.w("FirestoreDebug", "❌ 로그인된 유저 없음. 리스너 등록 안 함");
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        Log.d("FirestoreDebug", "📡 Listening path: /user/" + uid + "/schedule");

        if (scheduleListener != null) scheduleListener.remove();

        scheduleListener = db.collection("user")
                .document(uid)
                .collection("schedule")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreDebug", "리스너 오류", e);
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<ScheduleDTO> newList = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ScheduleDTO schedule = doc.toObject(ScheduleDTO.class);
                        if (schedule != null) {
                            // ✅ scheduleId 설정 (Firestore 문서 ID 사용)
                            schedule.setScheduleId(doc.getId());
                            newList.add(schedule);
                        }
                    }

                    Log.d("FirestoreDebug", "📦 수신된 문서 수: " + newList.size());

                    // ✅ Activity null 체크 추가
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        scheduleAdapter.updateData(newList);

                        // Empty state 업데이트
                        if (!newList.isEmpty()) {
                            noScheduleLayout.setVisibility(View.GONE);
                        } else {
                            noScheduleLayout.setVisibility(View.VISIBLE);
                        }
                    });
                });
    }

    private void showEditTitleDialog(ScheduleDTO schedule, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_title, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.et_title);
        etTitle.setText(schedule.getLocationName());

        builder.setTitle("제목 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String newTitle = etTitle.getText().toString();
                    if (!newTitle.isEmpty()) {
                        updateScheduleTitle(schedule, newTitle, position);
                    } else {
                        Toast.makeText(getContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateScheduleTitle(ScheduleDTO schedule, String newTitle, int position) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        String scheduleId = schedule.getScheduleId();

        db.collection("user").document(uid).collection("schedule").document(scheduleId)
                .update("locationName", newTitle)
                .addOnSuccessListener(aVoid -> {
                    schedule.setLocationName(newTitle);
                    scheduleAdapter.updateScheduleItem(position, schedule);
                    Toast.makeText(getContext(), "제목이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "제목 수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("ScheduleFragment", "Error updating title", e);
                });
    }
}