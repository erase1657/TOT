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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

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

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration scheduleListener;

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
                updateScheduleBackground(editingPosition, uri);
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
        ImageButton addScheduleButton = view.findViewById(R.id.btn_add_schedule);

        setupRecyclerView();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        addScheduleButton.setOnClickListener(v -> showCreateScheduleDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        listenSchedulesFromFirestore();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (scheduleListener != null) {
            scheduleListener.remove();
            scheduleListener = null;
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        scheduleList = new ArrayList<>();

        scheduleAdapter = new ScheduleAdapter(scheduleList, (schedule, position) -> {
            Intent intent = new Intent(getContext(), ScheduleSettingActivity.class);
            intent.putExtra("scheduleId", schedule.getScheduleId());
            intent.putExtra("startDate", schedule.getStartDate().toDate().getTime());
            intent.putExtra("endDate", schedule.getEndDate().toDate().getTime());
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
                showDeleteConfirmDialog(schedule.getScheduleId(), position);
            }
        });

        recyclerView.setAdapter(scheduleAdapter);
    }

    private void updateScheduleBackground(int position, Uri imageUri) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        ScheduleDTO schedule = scheduleList.get(position);
        String scheduleId = schedule.getScheduleId();

        schedule.setBackgroundImageUri(imageUri.toString());
        scheduleAdapter.updateScheduleItem(position, schedule);

        Map<String, Object> updates = new HashMap<>();
        updates.put("backgroundImageUri", imageUri.toString());

        db.collection("user").document(uid).collection("schedule").document(scheduleId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "배경 이미지가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "배경 이미지 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }


    private void showDeleteConfirmDialog(String scheduleId, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("스케줄 삭제")
                .setMessage("이 스케줄을 정말 삭제하시겠습니까? 관련된 모든 정보가 영구적으로 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteSchedule(scheduleId, position);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteSchedule(String scheduleId, int position) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        CollectionReference scheduleDateRef = db.collection("user").document(uid)
                .collection("schedule").document(scheduleId)
                .collection("scheduleDate");

        scheduleDateRef.get().addOnSuccessListener(querySnapshot -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot dateDoc : querySnapshot.getDocuments()) {
                // Delete scheduleItems and alarms
                dateDoc.getReference().collection("scheduleItem").get().addOnSuccessListener(itemSnapshot -> {
                    for (DocumentSnapshot itemDoc : itemSnapshot.getDocuments()) {
                        batch.delete(itemDoc.getReference());
                        // Also delete associated alarm
                        db.collection("user").document(uid).collection("alarms").document(itemDoc.getId()).delete();
                    }
                });

                // Delete album items
                dateDoc.getReference().collection("album").get().addOnSuccessListener(albumSnapshot -> {
                    for (DocumentSnapshot albumDoc : albumSnapshot.getDocuments()) {
                        batch.delete(albumDoc.getReference());
                    }
                });

                batch.delete(dateDoc.getReference());
            }

            batch.commit().addOnSuccessListener(aVoid -> {
                // Finally, delete the schedule document itself
                db.collection("user").document(uid).collection("schedule").document(scheduleId)
                        .delete()
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(getContext(), "스케줄이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            // The listener will automatically update the UI.
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "스케줄 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "하위 데이터 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "스케줄 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show());
    }

    private void showCreateScheduleDialog() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_schedule, null);

        RelativeLayout dateRangeBox = dialogView.findViewById(R.id.date_range_box);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tv_selected_date);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        Button btnPrev = dialogView.findViewById(R.id.btn_dialog_prev);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.Theme_TOT_RoundedDialog)
                .setView(dialogView)
                .create();

        dateRangeBox.setOnClickListener(v -> showGoogleDateRangePicker(tvSelectedDate));

        btnConfirm.setOnClickListener(v -> {
            if (selectedDateRange.isEmpty()) {
                Toast.makeText(getContext(), "여행 기간을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            addNewSchedule();
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

    private void addNewSchedule() {
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
                "지역",
                startDate,
                endDate,
                null,
                "",
                0,
                null
        );

        // Firestore에 저장 (리스너가 자동으로 UI 업데이트함)
        db.collection("user").document(uid)
                .collection("schedule").document(scheduleId)
                .set(schedule, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("ScheduleFragment", "Successfully added schedule: " + scheduleId);

                    // 상세 화면으로 이동
                    Intent intent = new Intent(getContext(), ScheduleSettingActivity.class);
                    intent.putExtra("scheduleId", scheduleId);
                    intent.putExtra("startDate", startDate.toDate().getTime());
                    intent.putExtra("endDate", endDate.toDate().getTime());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e("ScheduleFragment", "Error adding schedule", e);
                    Toast.makeText(getContext(), "스케줄 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });

        // ✅ 수동 리스트 추가 제거 (Firestore 리스너가 자동으로 처리)
        // scheduleList.add(0, schedule);
        // scheduleAdapter.notifyItemInserted(0);
        // recyclerView.smoothScrollToPosition(0);
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
}
