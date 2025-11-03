// 경로: app/src/main/java/com/example/tot/ViewPager/ScheduleFragment.java

package com.example.tot.ViewPager;

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
import com.example.tot.ScheduleRecyclerView.ScheduleAdapter;
import com.example.tot.ScheduleRecyclerView.ScheduleData;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ScheduleFragment extends Fragment {

    private static final String TAG = "ScheduleFragment";

    private RecyclerView recyclerView;
    private ScheduleAdapter scheduleAdapter;
    private List<ScheduleData> scheduleList;
    private LinearLayout noScheduleLayout;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // --- ✅ 1. 멤버 변수 수정 ---
    // 다이얼로그에서 선택된 날짜 정보를 저장할 멤버 변수
    private String selectedDateRange = "";      // 다이얼로그 표시용 (예: "2025/01/01~...")
    private String selectedStartDate = "";      // DB 저장용 (예: "2025-01-01")
    private String selectedEndDate = "";        // DB 저장용 (예: "2025-01-05")
    private String newScheduleDateRange = "";   // DB 저장용 (예: "1-5")
    private String newScheduleYearMonth = "";   // DB 저장용 (예: "2025년 1월")
    // --- (여기까지 수정) ---

    public ScheduleFragment() {
        super(R.layout.fragment_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView = view.findViewById(R.id.rv_schedules);
        noScheduleLayout = view.findViewById(R.id.layout_no_schedule);
        ImageButton addScheduleButton = view.findViewById(R.id.btn_add_schedule);

        setupRecyclerView();

        if (currentUser != null) {
            fetchSchedulesFromServer();
        } else {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
        }

        addScheduleButton.setOnClickListener(v -> {
            if (currentUser != null) {
                showCreateScheduleDialog();
            } else {
                Toast.makeText(getContext(), "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        // (기존 코드와 동일)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        scheduleList = new ArrayList<>();

        scheduleAdapter = new ScheduleAdapter(scheduleList, (schedule, position) -> {
            Toast.makeText(getContext(),
                    schedule.getLocation() + " 여행 상세보기",
                    Toast.LENGTH_SHORT).show();
            // TODO: 스케줄 상세 화면으로 이동
        });

        recyclerView.setAdapter(scheduleAdapter);
    }

    private void fetchSchedulesFromServer() {
        // (기존 코드와 동일)
        if (currentUser == null) return;

        db.collection("schedules")
                .whereEqualTo("ownerUid", currentUser.getUid())
                .orderBy("fullStartDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    scheduleList.clear();
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "가져올 스케줄이 없습니다.");
                    } else {
                        scheduleList.addAll(querySnapshot.toObjects(ScheduleData.class));
                        Log.d(TAG, "총 " + scheduleList.size() + "개의 스케줄 로드 성공");
                    }
                    scheduleAdapter.updateData(scheduleList);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "스케줄 로드 실패", e);
                    Toast.makeText(getContext(), "스케줄 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        // (기존 코드와 동일)
        if (scheduleList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            noScheduleLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noScheduleLayout.setVisibility(View.GONE);
        }
    }

    private void showCreateScheduleDialog() {
        // (기존 코드와 동일)
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_schedule, null);

        RelativeLayout dateRangeBox = dialogView.findViewById(R.id.date_range_box);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tv_selected_date);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        Button btnPrev = dialogView.findViewById(R.id.btn_dialog_prev);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.Theme_TOT_RoundedDialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dateRangeBox.setOnClickListener(v -> showGoogleDateRangePicker(tvSelectedDate));

        btnConfirm.setOnClickListener(v -> {
            if (selectedDateRange.isEmpty()) { // selectedDateRange가 비어있으면 날짜 선택 안 한 것
                Toast.makeText(getContext(), "여행 기간을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            addNewScheduleToFirestore();
            dialog.dismiss();
        });

        btnPrev.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * ✅ 2. 이 메서드가 멤버 변수에 값을 저장하도록 수정합니다.
     */
    private void showGoogleDateRangePicker(TextView tvSelectedDate) {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_App_DatePicker);
        builder.setTitleText("여행 기간을 선택하세요");

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

            // 필요한 포맷들 정의
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyy년 M월", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());

            Date startDate = new Date(startDateMillis + startOffset);
            Date endDate = new Date(endDateMillis + endOffset);

            String startDateStr = sdf.format(startDate);
            String endDateStr = sdf.format(endDate);

            // --- ✅ 멤버 변수에 DB에 저장할 값들 저장 ---
            selectedStartDate = fullDateFormat.format(startDate);    // "2025-01-01"
            selectedEndDate = fullDateFormat.format(endDate);      // "2025-01-05"
            newScheduleDateRange = dayFormat.format(startDate) + "-" + dayFormat.format(endDate); // "1-5"
            newScheduleYearMonth = yearMonthFormat.format(startDate);  // "2025년 1월"
            // --- (여기까지) ---

            long diffInMillis = (endDateMillis + endOffset) - (startDateMillis + startOffset);
            long nights = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            long days = nights + 1;

            // 다이얼로그 표시용 텍스트 생성 및 멤버 변수에 저장
            selectedDateRange = String.format(Locale.getDefault(),
                    "%s~%s (%d박 %d일)",
                    startDateStr,
                    endDateStr,
                    nights,
                    days
            );

            // TextView 업데이트
            tvSelectedDate.setText(selectedDateRange);
            tvSelectedDate.setTextColor(getResources().getColor(R.color.black));
        });

        datePicker.show(getParentFragmentManager(), datePicker.toString());
    }

    /**
     * ✅ 3. 에러 코드를 삭제하고, 멤버 변수를 사용하도록 수정합니다.
     */
    private void addNewScheduleToFirestore() {
        if (currentUser == null) return;

        // --- ❌ 에러가 발생했던 아래 코드를 모두 삭제 ❌ ---
        // TextView tvSelectedDate = ((AlertDialog) getFragmentManager().findFragmentByTag(
        //         MaterialDatePicker.class.getName())
        //         .getDialog())
        //         .findViewById(R.id.tv_selected_date);
        // String tempDateRange = "1-5";
        // String tempYearMonth = "2025년 1월";
        // --- (여기까지 삭제) ---

        // ✅ (대체) 위에서 저장한 멤버 변수들을 사용하여 새 객체 생성
        ScheduleData newSchedule = new ScheduleData(
                null, // scheduleId (Firestore가 자동 생성)
                "새 여행", // 기본 위치
                newScheduleDateRange,    // (예: "1-5")
                newScheduleYearMonth,    // (예: "2025년 1월")
                R.drawable.sample3, // 기본 배경
                selectedStartDate,       // (예: "2025-01-01")
                selectedEndDate          // (예: "2025-01-05")
        );

        // ✅ (핵심) 보안 규칙 통과를 위해 소유자 ID(이름표) 설정
        newSchedule.setOwnerUid(currentUser.getUid());

        // Firestore 'schedules' 컬렉션에 새 문서 추가
        db.collection("schedules")
                .add(newSchedule)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "새 스케줄 저장 성공, ID: " + documentReference.getId());
                    Toast.makeText(getContext(), "새로운 여행이 추가되었습니다!", Toast.LENGTH_SHORT).show();

                    // (수정) 로컬 리스트에 즉시 추가 (ID 포함)
                    newSchedule.setScheduleId(documentReference.getId());
                    scheduleList.add(0, newSchedule);
                    scheduleAdapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                    updateEmptyState();

                    // ✅ (수정) 사용한 날짜 멤버 변수들 초기화
                    selectedDateRange = "";
                    selectedStartDate = "";
                    selectedEndDate = "";
                    newScheduleDateRange = "";
                    newScheduleYearMonth = "";
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "새 스케줄 저장 실패", e);
                    Toast.makeText(getContext(), "스케줄 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }
}