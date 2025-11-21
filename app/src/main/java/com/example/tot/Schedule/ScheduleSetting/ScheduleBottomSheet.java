package com.example.tot.Schedule.ScheduleSetting;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tot.Home.HomeAlarmDTO;
import com.example.tot.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleBottomSheet {

    public interface OnScheduleSaveListener {
        void onScheduleSaved(ScheduleItemDTO item);
    }
    public interface OnAddPlaceListener {
        void onAddPlaceClicked();
    }
    private final Context context;
    private OnScheduleSaveListener listener;
    private OnAddPlaceListener placeListener;
    private NumberPicker np_StartHour, np_StartMinute, np_EndHour, np_EndMinute;
    private com.github.angads25.toggle.widget.LabeledSwitch sw_Alarm;
    private EditText et_Title;
    private Button btn_Save;
    private TextView tv_PlaceAddress;
    // 수정 모드 관련 변수
    private LatLng selectedLatLng;
    private boolean isEditMode = false;
    private String editingDocId = null;
    private ScheduleItemDTO editingItem = null;

    public ScheduleBottomSheet(Context context) {
        this.context = context;
    }

    public void setOnScheduleSaveListener(OnScheduleSaveListener listener) {
        this.listener = listener;
    }
    public void setOnAddPlaceListener(OnAddPlaceListener listener) {
        this.placeListener = listener;
    }
    /** 새 일정 추가용 */
    public void show() {
        showInternal(null, null);
    }
    public void setPlaceData(String address, LatLng latLng) {
        if (tv_PlaceAddress != null) {
            tv_PlaceAddress.setText(address);
        }
        this.selectedLatLng = latLng;
    }

    /** 기존 일정 수정용 */
    public void showWithData(ScheduleItemDTO item, String docId) {
        showInternal(item, docId);
    }

    /** 내부 공용 로직 */
    private void showInternal(ScheduleItemDTO item, String docId) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.RoundedBottomSheetDialog);
        View view = LayoutInflater.from(context).inflate(R.layout.bottomsheet_add_schedule, null);
        dialog.setContentView(view);

        // 바텀시트 전체 펼치기
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

        // 구성요소 초기화
        et_Title = view.findViewById(R.id.et_title);
        np_StartHour = view.findViewById(R.id.np_start_hour);
        np_StartMinute = view.findViewById(R.id.np_start_minute);
        np_EndHour = view.findViewById(R.id.np_end_hour);
        np_EndMinute = view.findViewById(R.id.np_end_minute);
        sw_Alarm = view.findViewById(R.id.sw_alarm);
        btn_Save = view.findViewById(R.id.btn_save);
        Button btn_AddPlace = view.findViewById(R.id.btn_add_place);
        tv_PlaceAddress = view.findViewById(R.id.tv_place_address);

        setupNumberPicker(np_StartHour, 0, 23);
        setupNumberPicker(np_StartMinute, 0, 59);
        setupNumberPicker(np_EndHour, 0, 23);
        setupNumberPicker(np_EndMinute, 0, 59);

        // 🔹 수정모드 진입 시 기존 데이터 세팅
        if (item != null) {
            isEditMode = true;
            editingDocId = docId;
            editingItem = item;

            et_Title.setText(item.getTitle());
            sw_Alarm.setOn(item.getAlarm());
            if (item.getPlaceName() != null && !item.getPlaceName().isEmpty()) {
                tv_PlaceAddress.setText(item.getPlaceName());
            }
            if (item.getPlace() != null) {
                selectedLatLng = new LatLng(item.getPlace().getLatitude(), item.getPlace().getLongitude());
            }
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

        btn_AddPlace.setOnClickListener(v -> {
            if (placeListener != null) {
                placeListener.onAddPlaceClicked();
            }
        });

        // 저장버튼 클릭
        btn_Save.setOnClickListener(v -> {
            String title = et_Title.getText().toString().trim();
            int startHour = np_StartHour.getValue();
            int startMinute = np_StartMinute.getValue();
            int endHour = np_EndHour.getValue();
            int endMinute = np_EndMinute.getValue();
            boolean alarmIsOn = sw_Alarm.isOn();
            String placeAddress = tv_PlaceAddress.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(context, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
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

            GeoPoint location = null;
            if (selectedLatLng != null) {
                location = new GeoPoint(selectedLatLng.latitude, selectedLatLng.longitude);
            }
            String placeName = tv_PlaceAddress.getText().toString();
            if (placeName.equals("장소")) {
                placeName = null;
            }

            ScheduleItemDTO newItem = new ScheduleItemDTO(title, startTimestamp, endTimestamp, location, placeName, alarmIsOn);

            if (isEditMode && editingDocId != null && context instanceof ScheduleSettingActivity) {

                ScheduleSettingActivity act = (ScheduleSettingActivity) context;
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 1) 입력된 값 (수정되었을 수도 있음)
                String newTitle = et_Title.getText().toString().trim();
                Timestamp newStart = startTimestamp;
                Timestamp newEnd = endTimestamp;
                boolean newAlarm = alarmIsOn;

                // 2) 기존 값 유지 처리
                // --- 장소 이름 ---
                String newPlaceName = tv_PlaceAddress.getText().toString();
                if (newPlaceName.equals("장소")) {
                    newPlaceName = editingItem.getPlaceName(); // 기존 값 유지
                }

                // --- 좌표(GeoPoint) ---
                GeoPoint newLocation;
                if (selectedLatLng != null) {
                    newLocation = new GeoPoint(selectedLatLng.latitude, selectedLatLng.longitude);
                } else {
                    newLocation = editingItem.getPlace(); // 기존 값 유지
                }

                // 3) Firestore update 필드 구성
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("title", newTitle);
                updateData.put("startTime", newStart);
                updateData.put("endTime", newEnd);
                updateData.put("placeName", newPlaceName);
                updateData.put("place", newLocation);
                updateData.put("alarm", newAlarm);

                // 4) Firestore 업데이트
                String finalNewPlaceName = newPlaceName;
                db.collection("user")
                        .document(act.getUserUid())
                        .collection("schedule")
                        .document(act.getScheduleId())
                        .collection("scheduleDate")
                        .document(act.getSelectedDate())
                        .collection("scheduleItem")
                        .document(editingDocId)
                        .update(updateData)
                        .addOnSuccessListener(aVoid -> {

                            // =========================
                            // 🔥 alarms 컬렉션 반영
                            // =========================

                            boolean oldAlarm = editingItem.getAlarm();

                            // (1) 알람 OFF → ON
                            if (!oldAlarm && newAlarm) {

                                Map<String, Object> alarmData = new HashMap<>();
                                alarmData.put("scheduleId", act.getScheduleId());
                                alarmData.put("planId", editingDocId);
                                alarmData.put("title", newTitle);
                                alarmData.put("date", act.getSelectedDate());
                                alarmData.put("place", finalNewPlaceName);
                                alarmData.put("startTime", newStart);
                                alarmData.put("endTime", newEnd);

                                db.collection("user")
                                        .document(act.getUserUid())
                                        .collection("alarms")
                                        .document(editingDocId)
                                        .set(alarmData);
                            }

                            // (2) 알람 ON → OFF
                            if (oldAlarm && !newAlarm) {
                                db.collection("user")
                                        .document(act.getUserUid())
                                        .collection("alarms")
                                        .document(editingDocId)
                                        .delete();
                            }

                            // (3) 알람 ON 유지 → 알람 정보도 업데이트해줌
                            if (oldAlarm && newAlarm) {

                                Map<String, Object> alarmUpdate = new HashMap<>();
                                alarmUpdate.put("title", newTitle);
                                alarmUpdate.put("place", finalNewPlaceName);
                                alarmUpdate.put("startTime", newStart);
                                alarmUpdate.put("endTime", newEnd);

                                db.collection("user")
                                        .document(act.getUserUid())
                                        .collection("alarms")
                                        .document(editingDocId)
                                        .update(alarmUpdate);
                            }

                            Toast.makeText(context, "일정이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
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
    private void saveAlarmToUserCollection(FirebaseFirestore db, ScheduleSettingActivity act,
                                           String ScheduleItem, ScheduleItemDTO item) {

        String uid = act.getUserUid();
        String scheduleId = act.getScheduleId();
        String date = act.getSelectedDate();

        // 알람 문서 데이터 구성
        HomeAlarmDTO alarm = new HomeAlarmDTO(
                scheduleId,
                ScheduleItem,
                item.getTitle(),
                date,
                item.getPlaceName(),
                item.getStartTime(),
                item.getEndTime()
        );

        db.collection("user")
                .document(uid)
                .collection("alarms")
                .document(ScheduleItem)
                .set(alarm);
    }
    private void deleteAlarmFromUserCollection(FirebaseFirestore db,
                                               ScheduleSettingActivity act,
                                               String ScheduleItemId) {

        String uid = act.getUserUid();

        db.collection("user")
                .document(uid)
                .collection("alarms")
                .document(ScheduleItemId)
                .delete();
    }

    private void setupNumberPicker(NumberPicker picker, int min, int max) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setFormatter(i -> String.format("%02d", i));
        picker.setWrapSelectorWheel(true);
    }
}
