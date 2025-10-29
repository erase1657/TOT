package com.example.tot.ViewPager;

import android.os.Bundle;
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
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ScheduleFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScheduleAdapter scheduleAdapter;
    private List<ScheduleData> scheduleList;
    private LinearLayout noScheduleLayout;

    // 선택된 날짜 정보 (다이얼로그용)
    private String selectedDateRange = "";
    private String selectedStartDate = "";
    private String selectedEndDate = "";

    public ScheduleFragment() {
        super(R.layout.fragment_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View 초기화
        recyclerView = view.findViewById(R.id.rv_schedules);
        noScheduleLayout = view.findViewById(R.id.layout_no_schedule);
        ImageButton addScheduleButton = view.findViewById(R.id.btn_add_schedule);

        // RecyclerView 설정 (2열 그리드)
        setupRecyclerView();

        // 더미 데이터 로드
        loadDummyData();

        // 스케줄 추가 버튼 클릭 이벤트
        addScheduleButton.setOnClickListener(v -> showCreateScheduleDialog());
    }

    /**
     * RecyclerView 설정 (2열 그리드 레이아웃)
     */
    private void setupRecyclerView() {
        // GridLayoutManager로 2열 설정
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        // 스케줄 리스트 초기화
        scheduleList = new ArrayList<>();

        // 어댑터 설정 (클릭 이벤트 포함)
        scheduleAdapter = new ScheduleAdapter(scheduleList, (schedule, position) -> {
            // 스케줄 카드 클릭 시 동작
            Toast.makeText(getContext(),
                    schedule.getLocation() + " 여행 상세보기",
                    Toast.LENGTH_SHORT).show();

            // TODO: 스케줄 상세 화면으로 이동
            // Intent intent = new Intent(getActivity(), ScheduleDetailActivity.class);
            // intent.putExtra("scheduleId", schedule.getScheduleId());
            // startActivity(intent);
        });

        recyclerView.setAdapter(scheduleAdapter);
    }

    /**
     * 더미 데이터 로드 (서버 연동 전 테스트용)
     */
    private void loadDummyData() {
        // 더미 스케줄 데이터 생성
        scheduleList.clear();

        scheduleList.add(new ScheduleData(
                "schedule_001",
                "익산",
                "23-27",
                "2020년 8월",
                R.drawable.sample3,
                "2020-08-23",
                "2020-08-27"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_002",
                "광주",
                "14-25",
                "2022년 9월",
                R.drawable.sample1,
                "2022-09-14",
                "2022-09-25"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_003",
                "춘천",
                "14-23",
                "2023년 3월",
                R.drawable.sample2,
                "2023-03-14",
                "2023-03-23"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_004",
                "부산",
                "1-5",
                "2023년 7월",
                R.drawable.sample3,
                "2023-07-01",
                "2023-07-05"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_005",
                "제주",
                "10-15",
                "2024년 4월",
                R.drawable.sample1,
                "2024-04-10",
                "2024-04-15"
        ));

        scheduleList.add(new ScheduleData(
                "schedule_006",
                "서울",
                "20-25",
                "2024년 12월",
                R.drawable.sample2,
                "2024-12-20",
                "2024-12-25"
        ));

        // 어댑터 업데이트
        scheduleAdapter.updateData(scheduleList);

        // 스케줄 유무에 따른 UI 변경
        updateEmptyState();
    }

    /**
     * 빈 상태 UI 업데이트
     */
    private void updateEmptyState() {
        if (scheduleList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            noScheduleLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noScheduleLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 스케줄 생성 다이얼로그 표시
     */
    private void showCreateScheduleDialog() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_schedule, null);

        RelativeLayout dateRangeBox = dialogView.findViewById(R.id.date_range_box);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tv_selected_date);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        Button btnPrev = dialogView.findViewById(R.id.btn_dialog_prev);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.Theme_TOT_RoundedDialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // 날짜 선택 박스 클릭
        dateRangeBox.setOnClickListener(v -> showGoogleDateRangePicker(tvSelectedDate));

        // 확인 버튼 클릭
        btnConfirm.setOnClickListener(v -> {
            if (selectedDateRange.isEmpty()) {
                Toast.makeText(getContext(), "여행 기간을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: 서버에 스케줄 생성 요청
            // 현재는 로컬에 더미 데이터 추가
            addNewSchedule();

            dialog.dismiss();
            Toast.makeText(getContext(), "스케줄이 생성되었습니다", Toast.LENGTH_SHORT).show();
        });

        // 이전 버튼 클릭
        btnPrev.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Google Material Date Range Picker 표시
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

            // 시간대 보정
            TimeZone timeZone = TimeZone.getDefault();
            long startOffset = timeZone.getOffset(startDateMillis);
            long endOffset = timeZone.getOffset(endDateMillis);

            // 날짜 포맷팅
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            String startDateStr = sdf.format(new Date(startDateMillis + startOffset));
            String endDateStr = sdf.format(new Date(endDateMillis + endOffset));

            // 전체 날짜 저장 (서버 전송용)
            selectedStartDate = fullDateFormat.format(new Date(startDateMillis + startOffset));
            selectedEndDate = fullDateFormat.format(new Date(endDateMillis + endOffset));

            // X박 Y일 계산
            long diffInMillis = (endDateMillis + endOffset) - (startDateMillis + startOffset);
            long nights = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            long days = nights + 1;

            // 날짜 범위 텍스트 생성
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
     * 새 스케줄 추가 (더미 데이터)
     */
    private void addNewSchedule() {
        // 실제로는 서버에서 받은 데이터로 생성
        // 여기서는 임시로 더미 데이터 추가

        String newScheduleId = "schedule_" + String.format("%03d", scheduleList.size() + 1);

        ScheduleData newSchedule = new ScheduleData(
                newScheduleId,
                "새 여행",
                "1-5",
                "2025년 1월",
                R.drawable.sample3,
                selectedStartDate,
                selectedEndDate
        );

        scheduleList.add(0, newSchedule); // 맨 앞에 추가
        scheduleAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);

        updateEmptyState();
    }

    /**
     * 서버에서 스케줄 데이터 가져오기 (예시)
     */
    private void fetchSchedulesFromServer() {
        // TODO: Retrofit 등으로 서버 API 호출
        // ApiService.getSchedules()
        //     .enqueue(new Callback<List<ScheduleData>>() {
        //         @Override
        //         public void onResponse(Call<List<ScheduleData>> call, Response<List<ScheduleData>> response) {
        //             if (response.isSuccessful() && response.body() != null) {
        //                 scheduleAdapter.updateData(response.body());
        //                 updateEmptyState();
        //             }
        //         }
        //
        //         @Override
        //         public void onFailure(Call<List<ScheduleData>> call, Throwable t) {
        //             Log.e("ScheduleFragment", "API 호출 실패", t);
        //         }
        //     });
    }
}