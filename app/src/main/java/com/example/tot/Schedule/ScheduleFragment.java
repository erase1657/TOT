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

    // ✨ 현재 선택된 탭 상태 저장
    private boolean isMyScheduleTab = true;

    public ScheduleFragment() {
        super(R.layout.fragment_schedule);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null && editingPosition != -1) {
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
        storage = FirebaseStorage.getInstance();

        addScheduleButton.setOnClickListener(v -> showCreateScheduleDialog());

        // ✨ 초기 탭 설정 및 리스너 등록
        setTabSelected(true);
        loadMySchedules();
    }

    @Override
    public void onResume() {
        super.onResume();
        // ✨ 화면 복귀 시 현재 탭의 리스너 재등록
        if (isMyScheduleTab) {
            loadMySchedules();
        } else {
            loadInvitedSchedules();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // ✨ 화면 이탈 시 리스너 제거 (메모리 누수 방지)
        if (scheduleListener != null) {
            scheduleListener.remove();
            scheduleListener = null;
        }
    }

    private void setupTabs() {
        tabMy.setOnClickListener(v -> {
            if (!isMyScheduleTab) { // ✨ 이미 선택된 탭이면 재로드 안함
                isMyScheduleTab = true;
                setTabSelected(true);
                loadMySchedules();
            }
        });

        tabInvited.setOnClickListener(v -> {
            if (isMyScheduleTab) { // ✨ 이미 선택된 탭이면 재로드 안함
                isMyScheduleTab = false;
                setTabSelected(false);
                loadInvitedSchedules();
            }
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

        // ✨ 기존 리스너 제거 후 새 리스너 등록
        if (scheduleListener != null) {
            scheduleListener.remove();
        }

        scheduleListener = db.collection("user")
                .document(uid)
                .collection("schedule")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("ScheduleFragment", "Listen failed", e);
                        return;
                    }

                    if (snapshot == null) {
                        updateScheduleUI(new ArrayList<>());
                        return;
                    }

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

                    Log.d("ScheduleFragment", "✅ 내 스케줄 실시간 갱신: " + list.size() + "개");
                    updateScheduleUI(list);
                });
    }

    private void updateScheduleUI(List<ScheduleDTO> list) {
        if (getActivity() == null || !isAdded()) return;

        getActivity().runOnUiThread(() -> {
            scheduleAdapter.updateData(list);

            if (list.isEmpty()) {
                noScheduleLayout.setVisibility(View.VISIBLE);
            } else {
                noScheduleLayout.setVisibility(View.GONE);
            }
        });
    }

    private void loadInvitedSchedules() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        if (scheduleListener != null) {
            scheduleListener.remove();
        }

        scheduleListener = db.collection("user")
                .document(uid)
                .collection("sharedSchedule")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || e != null) {
                        Log.e("ScheduleFragment", "Listen failed", e);
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

                            if (loadedCount[0] == total) {
                                Log.d("ScheduleFragment", "✅ 초대된 스케줄 실시간 갱신: " + invitedList.size() + "개");
                                updateScheduleUI(invitedList);
                            }
                        }).addOnFailureListener(err -> {
                            Log.e("ScheduleFragment", "Error loading invited schedule", err);
                            loadedCount[0]++;
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
            intent.putExtra("startMillis", schedule.getStartDate().toDate().getTime());
            intent.putExtra("endMillis", schedule.getEndDate().toDate().getTime());
            startActivity(intent);
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

    private void uploadImageAndUpdateSchedule(int position, Uri imageUri) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        ScheduleDTO schedule = scheduleList.get(position);
        String scheduleId = schedule.getScheduleId();

        Toast.makeText(getContext(), "이미지를 업로드 중입니다...", Toast.LENGTH_SHORT).show();

        StorageReference imageRef = storage.getReference()
                .child("schedule_backgrounds/" + uid + "/" + scheduleId + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String imageUrl = downloadUri.toString();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("backgroundImageUri", imageUrl);

                        db.collection("user").document(uid).collection("schedule").document(scheduleId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // ✨ 리스너가 자동으로 UI 갱신하므로 별도 처리 불필요
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

    private void deleteSchedule(ScheduleDTO schedule, int position) {
        if (auth.getCurrentUser() == null) return;

        String currentUid = auth.getCurrentUser().getUid();
        String scheduleId = schedule.getScheduleId();
        String ownerUid = schedule.getOwnerUid();
        boolean isShared = schedule.isShared();

        if (isShared) {
            Toast.makeText(getContext(), "공유받은 스케줄은 삭제할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                Task<?> itemTask = dateDoc.getReference().collection("scheduleItem").get()
                        .addOnSuccessListener(itemSnapshot -> {
                            for (DocumentSnapshot itemDoc : itemSnapshot.getDocuments()) {
                                batch.delete(itemDoc.getReference());
                                db.collection("user").document(currentUid).collection("alarms")
                                        .document(itemDoc.getId()).delete();
                            }
                        });
                tasks.add(itemTask);

                Task<?> albumTask = dateDoc.getReference().collection("album").get()
                        .addOnSuccessListener(albumSnapshot -> {
                            for (DocumentSnapshot albumDoc : albumSnapshot.getDocuments()) {
                                batch.delete(albumDoc.getReference());
                            }
                        });
                tasks.add(albumTask);

                batch.delete(dateDoc.getReference());
            }

            Tasks.whenAllComplete(tasks).addOnSuccessListener(t -> {
                batch.delete(scheduleRef);

                batch.commit().addOnSuccessListener(aVoid -> {
                    // ✨ 리스너가 자동으로 UI 갱신하므로 별도 처리 불필요
                    Toast.makeText(getContext(), "스케줄이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "스케줄이 생성되었습니다", Toast.LENGTH_SHORT).show();

                    // ✨ 리스너가 자동으로 UI를 갱신하므로, 바로 상세 화면으로 이동
                    Intent intent = new Intent(getContext(), ScheduleSettingActivity.class);
                    intent.putExtra("scheduleId", scheduleId);
                    intent.putExtra("ownerUid", uid);
                    intent.putExtra("isShared", false);
                    intent.putExtra("startMillis", startDate.toDate().getTime());
                    intent.putExtra("endMillis", endDate.toDate().getTime());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "스케줄 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("ScheduleFragment", "Error creating schedule", e);
                });
    }

    private String generateScheduleId() {
        String prefix = "SCDL_" + System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "_" + random;
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
                    // ✨ 리스너가 자동으로 UI 갱신하므로 별도 처리 불필요
                    Toast.makeText(getContext(), "제목이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "제목 수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("ScheduleFragment", "Error updating title", e);
                });
    }
}