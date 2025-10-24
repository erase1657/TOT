package com.example.tot.ViewPager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair; // Google 라이브러리는 이 Pair를 사용합니다.
import androidx.fragment.app.Fragment;

import com.example.tot.R;
// 1. 기존 라이브러리 import를 삭제합니다.
// import com.joshhalvorson.calendar_date_range_picker.CalendarDateRangePicker;

// 2. Google 공식 라이브러리를 import 합니다.
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date; // Calendar 대신 Date를 사용 (밀리초 변환용)
import java.util.Locale;
import java.util.TimeZone; // 시간대 보정용
import java.util.concurrent.TimeUnit; // 날짜 계산용

public class ScheduleFragment extends Fragment {
    public ScheduleFragment(){
        super(R.layout.fragment_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton addScheduleButton = view.findViewById(R.id.btn_add_schedule);
        addScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateScheduleDialog();
            }
        });
    }

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

        dateRangeBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 3. Google 공식 라이브러리를 호출하도록 변경
                showGoogleDateRangePicker(tvSelectedDate);
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Google의 공식 Material Date Range Picker를 띄웁니다.
     */
    private void showGoogleDateRangePicker(TextView tvSelectedDate) {
        // 1. Google 날짜 범위 선택기(Picker) 빌더 생성
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_App_DatePicker);;
        builder.setTitleText("여행 기간을 선택하세요");

        // 2. 현재 날짜를 기본값으로 설정 (선택 사항)
        builder.setSelection(Pair.create(
                MaterialDatePicker.todayInUtcMilliseconds(),
                MaterialDatePicker.todayInUtcMilliseconds()
        ));

        // 3. 빌더로 Picker 객체 생성
        MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();

        // 4. "확인" 버튼 클릭 리스너 설정
        datePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>>() {
            @Override
            public void onPositiveButtonClick(Pair<Long, Long> selection) {
                // 5. 날짜가 성공적으로 선택되었을 때 실행되는 부분
                Long startDateMillis = selection.first;
                Long endDateMillis = selection.second;

                // 시간대(Timezone) 문제 보정 (UTC -> Local)
                TimeZone timeZone = TimeZone.getDefault();
                long startOffset = timeZone.getOffset(startDateMillis);
                long endOffset = timeZone.getOffset(endDateMillis);

                // SimpleDateFormat으로 날짜 포맷팅
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                String startDateStr = sdf.format(new Date(startDateMillis + startOffset));
                String endDateStr = sdf.format(new Date(endDateMillis + endOffset));

                // "X박 Y일" 계산
                long diffInMillis = (endDateMillis + endOffset) - (startDateMillis + startOffset);
                long nights = TimeUnit.MILLISECONDS.toDays(diffInMillis); // X박
                long days = nights + 1; // Y일

                // 6. 최종 텍스트 조합
                String formattedDate = String.format(Locale.getDefault(),
                        "%s~%s (%d박 %d일)",
                        startDateStr,
                        endDateStr,
                        nights,
                        days
                );

                // 7. 팝업창의 TextView에 날짜 텍스트 업데이트
                tvSelectedDate.setText(formattedDate);
                tvSelectedDate.setTextColor(getResources().getColor(R.color.black)); // 글자색 변경
            }
        });

        // 5. 날짜 선택기를 화면에 띄웁니다. (Fragment이므로 getParentFragmentManager 사용)
        datePicker.show(getParentFragmentManager(), datePicker.toString());
    }
}