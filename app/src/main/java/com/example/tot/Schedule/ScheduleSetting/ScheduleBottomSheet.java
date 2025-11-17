package com.example.tot.Schedule.ScheduleSetting;

import android.app.Activity;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    private boolean isEditMode = false;
    private String editingDocId = null;
    private ScheduleItemDTO editingItem = null;

    public ScheduleBottomSheet(Context context) {
        this.context = context;
    }

    public void setOnScheduleSaveListener(OnScheduleSaveListener listener) {
        this.listener = listener;
    }

    public void show() {
        showInternal(null, null);
    }

    public void showWithData(ScheduleItemDTO item, String docId) {
        showInternal(item, docId);
    }

    private void showInternal(ScheduleItemDTO item, String docId) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.RoundedBottomSheetDialog);
        View view = LayoutInflater.from(context).inflate(R.layout.bottomsheet_add_schedule, null);
        dialog.setContentView(view);

        view.post(() -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setPeekHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
            }
        });

        dialog.show();

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

        if (item != null) {
            isEditMode = true;
            editingDocId = docId;
            editingItem = item;

            et_Title.setText(item.getTitle());
            sw_Alarm.setOn(item.getAlarm());

            Calendar cal = Calendar.getInstance();
            Timestamp start = item.getStartTime();
            cal.setTime(start.toDate());
            np_StartHour.setValue(cal.get(Calendar.HOUR_OF_DAY));
            np_StartMinute.setValue(cal.get(Calendar.MINUTE));

            Timestamp end = item.getEndTime();
            cal.setTime(end.toDate());
            np_EndHour.setValue(cal.get(Calendar.HOUR_OF_DAY));
            np_EndMinute.setValue(cal.get(Calendar.MINUTE));
        }

        btn_Save.setOnClickListener(v -> {
            String title = et_Title.getText().toString().trim();
            int startHour = np_StartHour.getValue();
            int startMinute = np_StartMinute.getValue();
            int endHour = np_EndHour.getValue();
            int endMinute = np_EndMinute.getValue();
            boolean alarmIsOn = sw_Alarm.isOn();

            // 제목이 비어있으면 기본값 설정
            if (title.isEmpty()) {
                title = "제목 없음";
            }

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, startHour);
            cal.set(Calendar.MINUTE, startMinute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Timestamp startTimestamp = new Timestamp(cal.getTime());

            cal.set(Calendar.HOUR_OF_DAY, endHour);
            cal.set(Calendar.MINUTE, endMinute);
            Timestamp endTimestamp = new Timestamp(cal.getTime());

            if ((endHour < startHour) || (endHour == startHour && endMinute <= startMinute)) {
                Toast.makeText(context, "종료 시간이 시작 시간보다 빠릅니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (context instanceof ScheduleSettingActivity) {
                ScheduleSettingActivity activity = (ScheduleSettingActivity) context;
                List<ScheduleItemDTO> existingItems = activity.getCachedItemsForDate(activity.getSelectedDate());
                List<String> existingDocIds = activity.getCachedDocIdsForDate(activity.getSelectedDate());

                for (int i = 0; i < existingItems.size(); i++) {
                    ScheduleItemDTO existing = existingItems.get(i);
                    String existingDocId = (existingDocIds != null && i < existingDocIds.size())
                            ? existingDocIds.get(i) : null;

                    if (isEditMode && editingDocId != null && editingDocId.equals(existingDocId)) continue;

                    Timestamp existStart = existing.getStartTime();
                    Timestamp existEnd = existing.getEndTime();

                    boolean overlap = startTimestamp.compareTo(existEnd) < 0 && endTimestamp.compareTo(existStart) > 0;
                    if (overlap) {
                        Toast.makeText(context, "⚠️ 기존 일정과 시간이 겹칩니다.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            GeoPoint location = new GeoPoint(0, 0);
            ScheduleItemDTO newItem = new ScheduleItemDTO(title, startTimestamp, endTimestamp, location, "예시", alarmIsOn);

            if (isEditMode && editingDocId != null && context instanceof ScheduleSettingActivity) {
                ScheduleSettingActivity act = (ScheduleSettingActivity) context;
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("user")
                        .document(act.getUserUid())
                        .collection("schedule")
                        .document(act.getScheduleId())
                        .collection("scheduleDate")
                        .document(act.getSelectedDate())
                        .collection("scheduleItem")
                        .document(editingDocId)
                        .set(newItem)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "일정이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss(); // 수정 완료 후 다이얼로그 닫기
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            } else {
                if (listener != null) listener.onScheduleSaved(newItem);
                dialog.dismiss();
            }
        });
    }

    private void setupNumberPicker(NumberPicker picker, int min, int max) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setFormatter(i -> String.format("%02d", i));
        picker.setWrapSelectorWheel(true);
    }
}