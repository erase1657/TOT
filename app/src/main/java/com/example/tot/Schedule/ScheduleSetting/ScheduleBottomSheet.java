package com.example.tot.Schedule.ScheduleSetting;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.example.tot.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.Calendar;


public class ScheduleBottomSheet {
    public interface OnScheduleSaveListener {
        void onScheduleSaved(ScheduleItemDTO item);
    }
    private final Context context;
    private OnScheduleSaveListener listener;
    private NumberPicker np_StartHour, np_StartMinute, np_EndHour, np_EndMinute;
    private com.github.angads25.toggle.widget.LabeledSwitch sw_Alarm;
    private EditText et_Title;
    private Button btn_Save;
    public ScheduleBottomSheet(Context context) {
        this.context = context;
    }
    public void setOnScheduleSaveListener(OnScheduleSaveListener listener) {
        this.listener = listener;
    }

    public void show(){
        BottomSheetDialog dialog = new BottomSheetDialog(context,  R.style.RoundedBottomSheetDialog);
        View view = LayoutInflater.from(context).inflate(R.layout.bottomsheet_add_schedule, null);
        dialog.setContentView(view);
        view.post(() -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // 완전히 펼치기
                behavior.setSkipCollapsed(true); // 접힌 상태를 아예 건너뜀
                behavior.setPeekHeight(Resources.getSystem().getDisplayMetrics().heightPixels); // 전체 높이로 peekHeight 설정
            }
        });
        dialog.show();

        // 바텀시트 다이얼로그 구성 요소
        et_Title = view.findViewById(R.id.et_title);
        np_StartHour = view.findViewById(R.id.np_start_hour);
        np_StartMinute = view.findViewById(R.id.np_start_minute);
        np_EndHour = view.findViewById(R.id.np_end_hour);
        np_EndMinute = view.findViewById(R.id.np_end_minute);
        sw_Alarm = view.findViewById(R.id.sw_alarm);
        btn_Save = view.findViewById(R.id.btn_save);

        setupNumberPicker(np_StartHour, 0, 23);
        setupNumberPicker(np_StartMinute, 0, 59);
        setupNumberPicker(np_EndHour, 0, 23);
        setupNumberPicker(np_EndMinute, 0, 59);

        // 4️⃣ 저장 버튼 클릭 로직
        btn_Save.setOnClickListener(v -> {
            int startHour = np_StartHour.getValue();
            int startMinute = np_StartMinute.getValue();
            int endHour = np_EndHour.getValue();
            int endMinute = np_EndMinute.getValue();
            String title = et_Title.getText().toString().trim();
            boolean alarmEnabled = sw_Alarm.isEnabled();



            Calendar calendar = Calendar.getInstance();

            calendar.set(Calendar.HOUR_OF_DAY, startHour);
            calendar.set(Calendar.MINUTE, startMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Timestamp startTimestamp = new Timestamp(calendar.getTime());


            calendar.set(Calendar.HOUR_OF_DAY, endHour);
            calendar.set(Calendar.MINUTE, endMinute);
            Timestamp endTimestamp = new Timestamp(calendar.getTime());

            GeoPoint location = new GeoPoint(0, 0); //예시 장소
            ScheduleItemDTO item = new ScheduleItemDTO(title, startTimestamp, endTimestamp, location, "예시", true);
            // 제목 입력 확인
            if (title.isEmpty()) {
                Toast.makeText(context, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 시간 유효성 검사
            if ((endHour < startHour) || (endHour == startHour && endMinute <= startMinute)) {
                Toast.makeText(context, "종료 시간이 시작 시간보다 빠릅니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 결과 표시 (→ 추후 DB 저장 또는 상위 Activity에 전달 가능)
            String result = String.format(
                    "제목: %s\n시간: %02d:%02d ~ %02d:%02d\n알람: %s",
                    title,
                    startHour, startMinute,
                    endHour, endMinute,
                    alarmEnabled ? "ON" : "OFF"
            );

            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            if (listener != null) {
                listener.onScheduleSaved(item);
            }
            dialog.dismiss();
        });
    }
    private void setupNumberPicker(NumberPicker picker, int min, int max) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setFormatter(i -> String.format("%02d", i)); // 항상 2자리로 표시
        picker.setWrapSelectorWheel(true);
    }
}
