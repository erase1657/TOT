package com.example.tot.Schedule;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    public ScheduleFragment() {
        super(R.layout.fragment_schedule);
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
        listenSchedulesFromFirestore(); // ✅ UI 준비 완료 후 리스너 등록
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
            // TODO: 상세 화면 이동
        });

        recyclerView.setAdapter(scheduleAdapter);
    }

    private void updateEmptyState() {
        noScheduleLayout.setVisibility(scheduleList.isEmpty() ? View.VISIBLE : View.GONE);
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
        String uid = auth.getCurrentUser().getUid();
        String scheduleId = generateScheduleId();

        ScheduleDTO schedule = new ScheduleDTO(
                scheduleId,
                "지역",
                startDate,
                endDate,
                null,
                "",
                0
        );

        db.collection("user").document(uid)
                .collection("schedule").document(scheduleId)
                .set(schedule, SetOptions.merge());

        scheduleList.add(0, schedule);
        scheduleAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);

        updateEmptyState();
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
                        if (schedule != null) newList.add(schedule);
                    }

                    Log.d("FirestoreDebug", "📦 수신된 문서 수: " + newList.size());

                    requireActivity().runOnUiThread(() -> {
                        scheduleAdapter.updateData(newList);

                        // ⚡ 리스트에 실제 데이터가 있을 때만 empty layout 숨김
                        if (!newList.isEmpty()) {
                            noScheduleLayout.setVisibility(View.GONE);
                        } else {
                            noScheduleLayout.setVisibility(View.VISIBLE);
                        }
                    });
                });
    }
}
