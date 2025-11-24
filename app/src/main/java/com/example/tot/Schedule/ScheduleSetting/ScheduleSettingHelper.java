package com.example.tot.Schedule.ScheduleSetting;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.util.Pair;

import com.example.tot.Map.MapActivity;
import com.example.tot.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleSettingHelper {
    private final ScheduleSettingActivity activity;
    private final FirebaseFirestore db;
    private final String userUid;
    private final String scheduleId;

    public ScheduleSettingHelper(ScheduleSettingActivity activity, FirebaseFirestore db, String userUid, String scheduleId) {
        this.activity = activity;
        this.db = db;
        this.userUid = userUid;
        this.scheduleId = scheduleId;
    }

    public void showDateChangeDialog(List<String> dateList, Timestamp currentStartDate, Timestamp currentEndDate, OnDateChangeListener listener) {
        checkDateConstraints(dateList, (minDays, maxDayIndex) -> {
            long currentStartMillis = currentStartDate.toDate().getTime();
            long currentEndMillis = currentEndDate.toDate().getTime();

            MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                    .setTheme(R.style.ThemeOverlay_App_DatePicker)
                    .setTitleText("여행 기간을 선택하세요");

            builder.setSelection(Pair.create(currentStartMillis, currentEndMillis));

            MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Long startDateMillis = selection.first;
                Long endDateMillis = selection.second;

                TimeZone timeZone = TimeZone.getDefault();
                long startOffset = timeZone.getOffset(startDateMillis);
                long endOffset = timeZone.getOffset(endDateMillis);

                Date newStartDate = new Date(startDateMillis + startOffset);
                Date newEndDate = new Date(endDateMillis + endOffset);

                long diffInMillis = (endDateMillis + endOffset) - (startDateMillis + startOffset);
                int newDays = (int) TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1;

                if (newDays < minDays) {
                    String message = minDays == 1
                            ? "최소 1일 이상이어야 합니다."
                            : String.format("최소 %d일 이상이어야 합니다.\n(일정 또는 앨범 데이터가 %d일차까지 있습니다)", minDays, maxDayIndex);
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                    return;
                }

                Timestamp newStart = new Timestamp(newStartDate);
                Timestamp newEnd = new Timestamp(newEndDate);

                migrateDateData(dateList, currentStartDate, newStart, newEnd, listener);
            });

            datePicker.show(activity.getSupportFragmentManager(), "date_change_picker");
        });
    }

    private void checkDateConstraints(List<String> dateList, OnConstraintCheckListener callback) {
        if (dateList == null || dateList.isEmpty()) {
            callback.onResult(1, 0);
            return;
        }

        Collections.sort(dateList);
        AtomicInteger maxDayWithData = new AtomicInteger(0);
        AtomicInteger pendingChecks = new AtomicInteger(dateList.size() * 2);

        for (int i = 0; i < dateList.size(); i++) {
            final int dayIndex = i + 1;
            String dateKey = dateList.get(i);

            db.collection("user").document(userUid)
                    .collection("schedule").document(scheduleId)
                    .collection("scheduleDate").document(dateKey)
                    .collection("scheduleItem")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            synchronized (maxDayWithData) {
                                if (dayIndex > maxDayWithData.get()) {
                                    maxDayWithData.set(dayIndex);
                                }
                            }
                        }
                        if (pendingChecks.decrementAndGet() == 0) {
                            int minDays = Math.max(1, maxDayWithData.get());
                            callback.onResult(minDays, maxDayWithData.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pendingChecks.decrementAndGet() == 0) {
                            int minDays = Math.max(1, maxDayWithData.get());
                            callback.onResult(minDays, maxDayWithData.get());
                        }
                    });

            db.collection("user").document(userUid)
                    .collection("schedule").document(scheduleId)
                    .collection("scheduleDate").document(dateKey)
                    .collection("album")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            synchronized (maxDayWithData) {
                                if (dayIndex > maxDayWithData.get()) {
                                    maxDayWithData.set(dayIndex);
                                }
                            }
                        }
                        if (pendingChecks.decrementAndGet() == 0) {
                            int minDays = Math.max(1, maxDayWithData.get());
                            callback.onResult(minDays, maxDayWithData.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pendingChecks.decrementAndGet() == 0) {
                            int minDays = Math.max(1, maxDayWithData.get());
                            callback.onResult(minDays, maxDayWithData.get());
                        }
                    });
        }
    }

    /**
     * ✅ 핵심 수정: 서브컬렉션까지 완전히 삭제 후 데이터 이동
     */
    private void migrateDateData(List<String> oldDateList, Timestamp oldStartDate, Timestamp newStartDate, Timestamp newEndDate, OnDateChangeListener listener) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        List<String> newDateList = generateNewDateList(newStartDate, newEndDate, sdf);

        if (newDateList.isEmpty()) {
            Toast.makeText(activity, "날짜 생성 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.sort(oldDateList);

        // 1단계: 기존 데이터 수집
        Map<Integer, Map<String, Object>> oldDateData = new HashMap<>();
        Map<Integer, List<Map<String, Object>>> oldScheduleItems = new HashMap<>();
        Map<Integer, List<Map<String, Object>>> oldAlbumItems = new HashMap<>();

        AtomicInteger pendingReads = new AtomicInteger(oldDateList.size() * 3);

        for (int i = 0; i < oldDateList.size(); i++) {
            final int dayIndex = i + 1;
            String oldDateKey = oldDateList.get(i);

            // 날짜 문서 데이터
            db.collection("user").document(userUid)
                    .collection("schedule").document(scheduleId)
                    .collection("scheduleDate").document(oldDateKey)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            oldDateData.put(dayIndex, doc.getData());
                        }
                        if (pendingReads.decrementAndGet() == 0) {
                            deleteOldDataAndMigrate(oldDateList, newDateList, oldDateData, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pendingReads.decrementAndGet() == 0) {
                            deleteOldDataAndMigrate(oldDateList, newDateList, oldDateData, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                        }
                    });

            // 일정 데이터
            db.collection("user").document(userUid)
                    .collection("schedule").document(scheduleId)
                    .collection("scheduleDate").document(oldDateKey)
                    .collection("scheduleItem")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Map<String, Object>> items = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Map<String, Object> data = new HashMap<>(doc.getData());
                            data.put("_docId", doc.getId());
                            items.add(data);
                        }
                        if (!items.isEmpty()) {
                            oldScheduleItems.put(dayIndex, items);
                        }
                        if (pendingReads.decrementAndGet() == 0) {
                            deleteOldDataAndMigrate(oldDateList, newDateList, oldDateData, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pendingReads.decrementAndGet() == 0) {
                            deleteOldDataAndMigrate(oldDateList, newDateList, oldDateData, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                        }
                    });

            // 앨범 데이터
            db.collection("user").document(userUid)
                    .collection("schedule").document(scheduleId)
                    .collection("scheduleDate").document(oldDateKey)
                    .collection("album")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<Map<String, Object>> items = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Map<String, Object> data = new HashMap<>(doc.getData());
                            data.put("_docId", doc.getId());
                            items.add(data);
                        }
                        if (!items.isEmpty()) {
                            oldAlbumItems.put(dayIndex, items);
                        }
                        if (pendingReads.decrementAndGet() == 0) {
                            deleteOldDataAndMigrate(oldDateList, newDateList, oldDateData, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pendingReads.decrementAndGet() == 0) {
                            deleteOldDataAndMigrate(oldDateList, newDateList, oldDateData, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                        }
                    });
        }
    }

    /**
     * ✅ 2단계: 기존 서브컬렉션까지 완전 삭제
     */
    private void deleteOldDataAndMigrate(List<String> oldDateList, List<String> newDateList,
                                         Map<Integer, Map<String, Object>> oldDateData,
                                         Map<Integer, List<Map<String, Object>>> oldScheduleItems,
                                         Map<Integer, List<Map<String, Object>>> oldAlbumItems,
                                         Timestamp newStartDate, Timestamp newEndDate,
                                         OnDateChangeListener listener) {

        AtomicInteger pendingDeletes = new AtomicInteger(0);

        // 삭제할 문서 개수 계산
        for (String oldDateKey : oldDateList) {
            pendingDeletes.incrementAndGet(); // 날짜 문서
        }

        for (int dayIndex : oldScheduleItems.keySet()) {
            pendingDeletes.addAndGet(oldScheduleItems.get(dayIndex).size());
        }

        for (int dayIndex : oldAlbumItems.keySet()) {
            pendingDeletes.addAndGet(oldAlbumItems.get(dayIndex).size());
        }

        if (pendingDeletes.get() == 0) {
            performMigration(newDateList, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
            return;
        }

        // 서브컬렉션 삭제
        for (int i = 0; i < oldDateList.size(); i++) {
            final int dayIndex = i + 1;
            String oldDateKey = oldDateList.get(i);

            // 일정 삭제
            if (oldScheduleItems.containsKey(dayIndex)) {
                for (Map<String, Object> item : oldScheduleItems.get(dayIndex)) {
                    String docId = (String) item.get("_docId");
                    db.collection("user").document(userUid)
                            .collection("schedule").document(scheduleId)
                            .collection("scheduleDate").document(oldDateKey)
                            .collection("scheduleItem").document(docId)
                            .delete()
                            .addOnCompleteListener(task -> {
                                if (pendingDeletes.decrementAndGet() == 0) {
                                    performMigration(newDateList, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                                }
                            });
                }
            }

            // 앨범 삭제
            if (oldAlbumItems.containsKey(dayIndex)) {
                for (Map<String, Object> item : oldAlbumItems.get(dayIndex)) {
                    String docId = (String) item.get("_docId");
                    db.collection("user").document(userUid)
                            .collection("schedule").document(scheduleId)
                            .collection("scheduleDate").document(oldDateKey)
                            .collection("album").document(docId)
                            .delete()
                            .addOnCompleteListener(task -> {
                                if (pendingDeletes.decrementAndGet() == 0) {
                                    performMigration(newDateList, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                                }
                            });
                }
            }

            // 날짜 문서 삭제
            db.collection("user").document(userUid)
                    .collection("schedule").document(scheduleId)
                    .collection("scheduleDate").document(oldDateKey)
                    .delete()
                    .addOnCompleteListener(task -> {
                        if (pendingDeletes.decrementAndGet() == 0) {
                            performMigration(newDateList, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                        }
                    });
        }
    }

    /**
     * ✅ 3단계: 새 날짜에 데이터 생성
     */
    private void performMigration(List<String> newDateList,
                                  Map<Integer, List<Map<String, Object>>> oldScheduleItems,
                                  Map<Integer, List<Map<String, Object>>> oldAlbumItems,
                                  Timestamp newStartDate, Timestamp newEndDate,
                                  OnDateChangeListener listener) {

        WriteBatch batch = db.batch();

        // 새 날짜 문서 생성
        for (int i = 0; i < newDateList.size(); i++) {
            int dayIndex = i + 1;
            String newDateKey = newDateList.get(i);

            Map<String, Object> dateDoc = new HashMap<>();
            dateDoc.put("dayIndex", dayIndex);
            dateDoc.put("date", newDateKey);

            batch.set(db.collection("user").document(userUid)
                    .collection("schedule").document(scheduleId)
                    .collection("scheduleDate").document(newDateKey), dateDoc);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    migrateSubcollections(newDateList, oldScheduleItems, oldAlbumItems, newStartDate, newEndDate, listener);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "날짜 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ 4단계: 서브컬렉션 데이터 이동
     */
    private void migrateSubcollections(List<String> newDateList,
                                       Map<Integer, List<Map<String, Object>>> oldScheduleItems,
                                       Map<Integer, List<Map<String, Object>>> oldAlbumItems,
                                       Timestamp newStartDate, Timestamp newEndDate,
                                       OnDateChangeListener listener) {

        AtomicInteger pendingWrites = new AtomicInteger(0);

        for (int i = 0; i < newDateList.size(); i++) {
            int dayIndex = i + 1;
            String newDateKey = newDateList.get(i);

            // 일정 데이터 이동
            if (oldScheduleItems.containsKey(dayIndex)) {
                List<Map<String, Object>> items = oldScheduleItems.get(dayIndex);
                pendingWrites.addAndGet(items.size());

                for (Map<String, Object> item : items) {
                    String docId = (String) item.remove("_docId");

                    db.collection("user").document(userUid)
                            .collection("schedule").document(scheduleId)
                            .collection("scheduleDate").document(newDateKey)
                            .collection("scheduleItem").document(docId)
                            .set(item)
                            .addOnCompleteListener(task -> {
                                if (pendingWrites.decrementAndGet() == 0) {
                                    finalizeMigration(newStartDate, newEndDate, listener);
                                }
                            });
                }
            }

            // 앨범 데이터 이동
            if (oldAlbumItems.containsKey(dayIndex)) {
                List<Map<String, Object>> items = oldAlbumItems.get(dayIndex);
                pendingWrites.addAndGet(items.size());

                for (Map<String, Object> item : items) {
                    String docId = (String) item.remove("_docId");

                    db.collection("user").document(userUid)
                            .collection("schedule").document(scheduleId)
                            .collection("scheduleDate").document(newDateKey)
                            .collection("album").document(docId)
                            .set(item)
                            .addOnCompleteListener(task -> {
                                if (pendingWrites.decrementAndGet() == 0) {
                                    finalizeMigration(newStartDate, newEndDate, listener);
                                }
                            });
                }
            }
        }

        if (pendingWrites.get() == 0) {
            finalizeMigration(newStartDate, newEndDate, listener);
        }
    }

    /**
     * ✅ 5단계: 스케줄 문서 업데이트 및 완료
     */
    private void finalizeMigration(Timestamp newStartDate, Timestamp newEndDate, OnDateChangeListener listener) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("startDate", newStartDate);
        updateData.put("endDate", newEndDate);

        db.collection("user").document(userUid)
                .collection("schedule").document(scheduleId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "날짜가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onDateChanged(newStartDate, newEndDate);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "날짜 업데이트 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private List<String> generateNewDateList(Timestamp start, Timestamp end, SimpleDateFormat sdf) {
        List<String> list = new ArrayList<>();
        long diffMillis = end.toDate().getTime() - start.toDate().getTime();
        int days = (int) TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;
        Date current = start.toDate();

        for (int i = 0; i < days; i++) {
            Date date = new Date(current.getTime() + TimeUnit.DAYS.toMillis(i));
            list.add(sdf.format(date));
        }

        return list;
    }

    public void launchMapWithAllPlaces(List<String> dateList, Map<String, List<ScheduleItemDTO>> localCache) {
        final Map<String, List<ScheduleItemDTO>> tempItemsMap = new HashMap<>();
        AtomicInteger pendingFetches = new AtomicInteger(dateList.size());
        Collections.sort(dateList);

        for (String dateKey : dateList) {
            if (localCache.containsKey(dateKey)) {
                tempItemsMap.put(dateKey, localCache.get(dateKey));
                if (pendingFetches.decrementAndGet() == 0) {
                    launchMapIntent(dateList, tempItemsMap);
                }
            } else {
                db.collection("user").document(userUid).collection("schedule").document(scheduleId)
                        .collection("scheduleDate").document(dateKey).collection("scheduleItem").get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<ScheduleItemDTO> items = new ArrayList<>();
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                ScheduleItemDTO item = doc.toObject(ScheduleItemDTO.class);
                                if (item != null && item.getPlace() != null) {
                                    items.add(item);
                                }
                            }
                            items.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
                            tempItemsMap.put(dateKey, items);
                            if (pendingFetches.decrementAndGet() == 0) {
                                launchMapIntent(dateList, tempItemsMap);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (pendingFetches.decrementAndGet() == 0) {
                                launchMapIntent(dateList, tempItemsMap);
                            }
                        });
            }
        }
    }

    private void launchMapIntent(List<String> dateList, Map<String, List<ScheduleItemDTO>> itemsMap) {
        ArrayList<LatLng> sortedLocations = new ArrayList<>();
        ArrayList<Integer> dayList = new ArrayList<>();
        int dayIndex = 1;

        for (String dateKey : dateList) {
            List<ScheduleItemDTO> items = itemsMap.get(dateKey);
            if (items != null) {
                for (ScheduleItemDTO item : items) {
                    if (item.getPlace() != null) {
                        GeoPoint geoPoint = item.getPlace();
                        sortedLocations.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
                        dayList.add(dayIndex);
                    }
                }
            }
            dayIndex++;
        }

        if (sortedLocations.isEmpty()) {
            Toast.makeText(activity, "지도에 표시할 장소가 없습니다.", Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(activity, MapActivity.class);
            intent.putParcelableArrayListExtra(MapActivity.EXTRA_PLACE_LAT_LNG_LIST, sortedLocations);
            intent.putIntegerArrayListExtra(MapActivity.EXTRA_PLACE_DAY_LIST, dayList);
            activity.startActivity(intent);
        }
    }

    public void syncAllScheduleDataToPost(String relatedPostId, List<String> dateList) {
        if (relatedPostId == null) return;

        CollectionReference postScheduleRef = db.collection("public")
                .document("community")
                .collection("posts")
                .document(relatedPostId)
                .collection("scheduleDate");

        CollectionReference userScheduleRef = db.collection("user")
                .document(userUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate");

        userScheduleRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot dateDoc : querySnapshot.getDocuments()) {
                        String dateKey = dateDoc.getId();
                        Map<String, Object> dateData = dateDoc.getData();
                        if (dateData != null) {
                            batch.set(postScheduleRef.document(dateKey), dateData);
                        }
                        syncScheduleItemsForDate(relatedPostId, dateKey);
                        syncAlbumForDate(relatedPostId, dateKey);
                    }
                    batch.commit();
                });
    }

    private void syncScheduleItemsForDate(String relatedPostId, String dateKey) {
        if (relatedPostId == null) return;

        CollectionReference userItems = db.collection("user")
                .document(userUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem");

        CollectionReference postItems = db.collection("public")
                .document("community")
                .collection("posts")
                .document(relatedPostId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem");

        userItems.get().addOnSuccessListener(querySnapshot -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    batch.set(postItems.document(doc.getId()), data);
                }
            }
            batch.commit();
        });
    }

    private void syncAlbumForDate(String relatedPostId, String dateKey) {
        if (relatedPostId == null) return;

        CollectionReference userAlbum = db.collection("user")
                .document(userUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album");

        CollectionReference postAlbum = db.collection("public")
                .document("community")
                .collection("posts")
                .document(relatedPostId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album");

        userAlbum.get().addOnSuccessListener(querySnapshot -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    batch.set(postAlbum.document(doc.getId()), data);
                }
            }
            batch.commit();
        });
    }

    public interface OnDateChangeListener {
        void onDateChanged(Timestamp newStartDate, Timestamp newEndDate);
    }

    private interface OnConstraintCheckListener {
        void onResult(int minDays, int maxDayIndex);
    }
}