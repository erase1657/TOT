package com.example.tot.Schedule.ScheduleSetting;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Album.ScheduleAlbumActivity;
import com.example.tot.Authentication.UserDTO;
import com.example.tot.Map.MapActivity;
import com.example.tot.R;
import com.example.tot.Schedule.ScheduleSetting.Invite.InviteDialog;
import com.example.tot.Schedule.ScheduleSetting.Invite.InvitedMemberAdapter;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleSettingActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String userUid,ownerUid,effectiveUid, scheduleId, selectedDate;
    private Timestamp startDate, endDate;
    private RecyclerView rvDate, rvScheduleItem,rvMembers;
    private InvitedMemberAdapter memberAdapter;
    private DateAdapter dateAdapter;
    private ScheduleItemAdapter scheduleItemAdapter;
    private List<String> dateList = new ArrayList<>();
    private ScheduleBottomSheet currentBottomSheet;
    private final Map<String, List<ScheduleItemDTO>> localCache = new HashMap<>();
    private final Map<String, List<String>> localCacheDocIds = new HashMap<>();
    private Button btn_Menu, btn_Invite;
    private ImageButton btn_AddSchedule;
    private ListenerRegistration currentListener;
    private ActivityResultLauncher<Intent> mapActivityLauncher;
    private boolean isPostEditMode = false;
    private String relatedPostId = null;
    private ListenerRegistration postUpdateListener;
    private ScheduleSettingHelper helper;
    private boolean isReadOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_setting);

        db = FirebaseFirestore.getInstance();
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        scheduleId = getIntent().getStringExtra("scheduleId");
        isPostEditMode = getIntent().getBooleanExtra("fromPostEdit", false);
        relatedPostId = getIntent().getStringExtra("postId");
        ownerUid = getIntent().getStringExtra("ownerUid");
        Log.e("ScheduleSetting", "ownerUid = " + ownerUid + ", scheduleId = " + scheduleId);

        isReadOnly = !userUid.equals(ownerUid);

        long startMillis = getIntent().getLongExtra("startMillisUtc", 0);
        long endMillis   = getIntent().getLongExtra("endMillisUtc", 0);
        startDate = new Timestamp(new Date(startMillis));
        endDate = new Timestamp(new Date(endMillis));
        if (scheduleId == null || startMillis == 0 || endMillis == 0) {
            Toast.makeText(this, "스케줄 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ Helper 초기화
        helper = new ScheduleSettingHelper(this, db, ownerUid, scheduleId);

        rvDate = findViewById(R.id.rv_datelist);
        rvScheduleItem = findViewById(R.id.rv_schedulelist);
        btn_AddSchedule = findViewById(R.id.btn_add_schedule);
        btn_Menu = findViewById(R.id.btn_menu);
        btn_Invite = findViewById(R.id.btn_invite);

        rvMembers = findViewById(R.id.rv_member);
        memberAdapter = new InvitedMemberAdapter(this);
        rvMembers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMembers.setAdapter(memberAdapter);
        loadMembers(ownerUid, scheduleId);

        setRvDate();
        setRvScheduleItem();
        generateScheduleDates(startDate, endDate);
        if (isReadOnly) {
            btn_AddSchedule.setVisibility(View.GONE);
            btn_Menu.setVisibility(View.GONE);
            btn_Invite.setVisibility(View.GONE);
        }
        if (isPostEditMode && relatedPostId != null) {
            startPostUpdateSync();
        }

        mapActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String address = result.getData().getStringExtra(MapActivity.EXTRA_PLACE_ADDRESS);
                        LatLng latLng = result.getData().getParcelableExtra(MapActivity.EXTRA_PLACE_LAT_LNG);
                        if (address != null && latLng != null && currentBottomSheet != null) {
                            currentBottomSheet.setPlaceData(address, latLng);
                        }
                    }
                });

        btn_Menu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(ScheduleSettingActivity.this, v);
            menu.getMenuInflater().inflate(R.menu.schedule_menu, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                // ✅ 날짜 변경 메뉴 추가
                if (id == R.id.menu_change_date) {
                    showDateChangeDialog();
                    return true;
                } else if (id == R.id.menu_map) {
                    showAllPlacesOnMap();
                    return true;
                } else if (id == R.id.menu_album) {
                    Intent intent = new Intent(ScheduleSettingActivity.this, ScheduleAlbumActivity.class);
                    intent.putExtra("scheduleId", scheduleId);
                    intent.putExtra("ownerUid", ownerUid);
                    intent.putStringArrayListExtra("dateList", new ArrayList<>(dateList));
                    startActivity(intent);
                    return true;
                } else if (id == R.id.menu_delete) {
                    showDeleteConfirmDialog();
                    return true;
                }
                return false;
            });
            menu.show();
        });

        btn_Invite.setOnClickListener(v -> {
            InviteDialog dialog = new InviteDialog(ScheduleSettingActivity.this);
            dialog.show();
        });

        btn_AddSchedule.setOnClickListener(v -> {
            currentBottomSheet = new ScheduleBottomSheet(ScheduleSettingActivity.this);
            currentBottomSheet.setOnAddPlaceListener(this::openMapForPlaceSelection);
            currentBottomSheet.setOnScheduleSaveListener(item -> {
                if (selectedDate == null) {
                    Toast.makeText(this, "날짜가 선택되지 않았습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.collection("user")
                        .document(ownerUid)
                        .collection("schedule")
                        .document(scheduleId)
                        .collection("scheduleDate")
                        .document(selectedDate)
                        .collection("scheduleItem")
                        .add(item)
                        .addOnSuccessListener(docRef -> {
                            String scheduleItemId = docRef.getId();
                            Toast.makeText(this, "일정이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            if (item.getAlarm()) {
                                Map<String, Object> alarm = new HashMap<>();
                                alarm.put("scheduleId", scheduleId);
                                alarm.put("planId", scheduleItemId);
                                alarm.put("title", item.getTitle());
                                alarm.put("date", selectedDate);
                                alarm.put("place", item.getPlaceName());
                                alarm.put("startTime", item.getStartTime());
                                alarm.put("endTime", item.getEndTime());
                                db.collection("user")
                                        .document(ownerUid)
                                        .collection("alarms")
                                        .document(scheduleItemId)
                                        .set(alarm);
                            }
                            if (isPostEditMode && relatedPostId != null) {
                                syncScheduleItemToPost(selectedDate, scheduleItemId, item);
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "일정 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
            currentBottomSheet.show();
        });
    }
    private void loadMembers(String ownerUid, String scheduleId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1) Owner 로드
        db.collection("user")
                .document(ownerUid)
                .get()
                .addOnSuccessListener(ownerDoc -> {

                    UserDTO owner = ownerDoc.toObject(UserDTO.class);
                    if (owner == null) {
                        Log.e("loadMembers", "❌ owner is NULL. UserDTO 필드 mismatch 가능");
                        return;
                    }
                    Log.d("MEMBER", "owner.nickname = " + owner.getNickname());
                    Log.d("MEMBER", "owner.profileImageUrl = " + owner.getProfileImageUrl());
                    // 2) invited 서브컬렉션에서 UID 가져오기
                    db.collection("user")
                            .document(ownerUid)
                            .collection("schedule")
                            .document(scheduleId)
                            .collection("invited")
                            .get()
                            .addOnSuccessListener(inviteQuery -> {

                                List<String> uidList = new ArrayList<>();

                                for (DocumentSnapshot d : inviteQuery.getDocuments()) {
                                    uidList.add(d.getId());   // 문서 ID = 초대된 유저 UID
                                }

                                // 초대된 멤버 없으면 owner만 표시
                                if (uidList.isEmpty()) {
                                    memberAdapter.setMembers(owner, new ArrayList<>());
                                    return;
                                }

                                // invited 멤버들 로드
                                List<UserDTO> invitedList = new ArrayList<>();
                                AtomicInteger counter = new AtomicInteger(uidList.size());

                                for (String uid : uidList) {

                                    db.collection("user")
                                            .document(uid)
                                            .get()
                                            .addOnSuccessListener(doc -> {

                                                UserDTO member = doc.toObject(UserDTO.class);
                                                if (member != null) {
                                                    invitedList.add(member);
                                                }

                                                // 모든 문서 로드 끝난 시점
                                                if (counter.decrementAndGet() == 0) {
                                                    memberAdapter.setMembers(owner, invitedList);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("loadMembers", "초대 멤버 로드 실패: " + e.getMessage());
                                                if (counter.decrementAndGet() == 0) {
                                                    memberAdapter.setMembers(owner, invitedList);
                                                }
                                            });
                                }
                            });
                });
    }

    // ✅ 날짜 변경 다이얼로그 표시
    private void showDateChangeDialog() {
        helper.showDateChangeDialog(dateList, startDate, endDate, (newStartDate, newEndDate) -> {
            // 날짜 변경 성공 후 UI 업데이트
            startDate = newStartDate;
            endDate = newEndDate;
            generateScheduleDates(startDate, endDate);
        });
    }

    private void startPostUpdateSync() {
        if (postUpdateListener != null) postUpdateListener.remove();
        postUpdateListener = db.collection("user")
                .document(ownerUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot != null && !snapshot.isEmpty()) {
                        syncAllScheduleDataToPost();
                    }
                });
    }

    private void syncAllScheduleDataToPost() {
        if (relatedPostId == null) return;
        CollectionReference postScheduleRef = db.collection("public")
                .document("community")
                .collection("posts")
                .document(relatedPostId)
                .collection("scheduleDate");
        CollectionReference userScheduleRef = db.collection("user")
                .document(ownerUid)
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
                        syncScheduleItemsForDate(dateKey);
                        syncAlbumForDate(dateKey);
                    }
                    batch.commit();
                });
    }

    private void syncScheduleItemsForDate(String dateKey) {
        if (relatedPostId == null) return;
        CollectionReference userItems = db.collection("user")
                .document(ownerUid)
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

    private void syncAlbumForDate(String dateKey) {
        if (relatedPostId == null) return;
        CollectionReference userAlbum = db.collection("user")
                .document(ownerUid)
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

    private void syncScheduleItemToPost(String dateKey, String itemId, ScheduleItemDTO item) {
        if (relatedPostId == null) return;
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("title", item.getTitle());
        itemData.put("startTime", item.getStartTime());
        itemData.put("endTime", item.getEndTime());
        itemData.put("place", item.getPlace());
        itemData.put("placeName", item.getPlaceName());
        itemData.put("alarm", item.getAlarm());
        db.collection("public")
                .document("community")
                .collection("posts")
                .document(relatedPostId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem")
                .document(itemId)
                .set(itemData);
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("스케줄 삭제")
                .setMessage("이 스케줄을 삭제하시겠습니까?\n스케줄 내 모든 일정과 알람이 함께 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> deleteSchedule())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteSchedule() {
        if (ownerUid == null || scheduleId == null) return;
        AtomicInteger deleteCount = new AtomicInteger(dateList.size());
        for (String dateKey : dateList) {
            db.collection("user")
                    .document(ownerUid)
                    .collection("schedule")
                    .document(scheduleId)
                    .collection("scheduleDate")
                    .document(dateKey)
                    .collection("scheduleItem")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String itemId = doc.getId();
                            doc.getReference().delete();
                            db.collection("user")
                                    .document(ownerUid)
                                    .collection("alarms")
                                    .document(itemId)
                                    .delete();
                        }
                        db.collection("user")
                                .document(ownerUid)
                                .collection("schedule")
                                .document(scheduleId)
                                .collection("scheduleDate")
                                .document(dateKey)
                                .delete();
                        if (deleteCount.decrementAndGet() == 0) {
                            db.collection("user")
                                    .document(ownerUid)
                                    .collection("schedule")
                                    .document(scheduleId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "스케줄이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                        }
                    });
        }
    }

    private void showDeleteItemConfirmDialog(String docId) {
        new AlertDialog.Builder(this)
                .setTitle("일정 삭제")
                .setMessage("이 일정을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteScheduleItem(docId))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteScheduleItem(String docId) {
        if (ownerUid == null || scheduleId == null || selectedDate == null || docId == null) {
            Toast.makeText(this, "오류: 삭제 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("user").document(ownerUid)
                .collection("schedule").document(scheduleId)
                .collection("scheduleDate").document(selectedDate)
                .collection("scheduleItem").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("user").document(ownerUid)
                            .collection("alarms").document(docId).delete();

                    Toast.makeText(this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    if (isPostEditMode && relatedPostId != null) {
                        deleteScheduleItemFromPost(selectedDate, docId);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "삭제에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteScheduleItemFromPost(String dateKey, String itemId) {
        if (relatedPostId == null) return;
        db.collection("public")
                .document("community")
                .collection("posts")
                .document(relatedPostId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem")
                .document(itemId)
                .delete();
    }


    private void openMapForPlaceSelection() {
        Intent intent = new Intent(this, MapActivity.class);
        ArrayList<LatLng> sortedLocations = new ArrayList<>();
        ArrayList<Integer> dayListForMap = new ArrayList<>();
        Collections.sort(this.dateList);
        for (int i = 0; i < this.dateList.size(); i++) {
            String dateKey = this.dateList.get(i);
            List<ScheduleItemDTO> items = localCache.get(dateKey);
            if (items != null) {
                for (ScheduleItemDTO item : items) {
                    if (item.getPlace() != null) {
                        GeoPoint geoPoint = item.getPlace();
                        sortedLocations.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
                        dayListForMap.add(i + 1);
                    }
                }
            }
        }
        intent.putParcelableArrayListExtra(MapActivity.EXTRA_PLACE_LAT_LNG_LIST, sortedLocations);
        intent.putIntegerArrayListExtra(MapActivity.EXTRA_PLACE_DAY_LIST, dayListForMap);
        mapActivityLauncher.launch(intent);
    }

    private void setRvDate() {
        dateAdapter = new DateAdapter(dateList, date -> {
            selectedDate = date;
            dateAdapter.setSelectedDate(date);
            listenScheduleItems(date);
        });
        rvDate.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDate.setAdapter(dateAdapter);
    }

    private void setRvScheduleItem() {
        scheduleItemAdapter = new ScheduleItemAdapter((item, docID) -> {
            currentBottomSheet = new ScheduleBottomSheet(ScheduleSettingActivity.this);
            currentBottomSheet.setOnAddPlaceListener(this::openMapForPlaceSelection);
            currentBottomSheet.showWithData(item, docID);
        });
        scheduleItemAdapter.setOnScheduleMenuItemClickListener(this::showDeleteItemConfirmDialog);
        rvScheduleItem.setLayoutManager(new LinearLayoutManager(this));
        rvScheduleItem.setAdapter(scheduleItemAdapter);
    }

    private void listenScheduleItems(String dateKey) {
        if (currentListener != null) currentListener.remove();
        currentListener = db.collection("user")
                .document(ownerUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;

                    Map<String, ScheduleItemDTO> itemMap = new HashMap<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ScheduleItemDTO item = doc.toObject(ScheduleItemDTO.class);
                        if (item != null) {
                            itemMap.put(doc.getId(), item);
                        }
                    }

                    List<String> sortedDocIds = new ArrayList<>(itemMap.keySet());
                    sortedDocIds.sort((docId1, docId2) -> {
                        ScheduleItemDTO item1 = itemMap.get(docId1);
                        ScheduleItemDTO item2 = itemMap.get(docId2);
                        if (item1 != null && item1.getStartTime() != null && item2 != null && item2.getStartTime() != null) {
                            return item1.getStartTime().compareTo(item2.getStartTime());
                        }
                        return 0;
                    });

                    List<ScheduleItemDTO> sortedList = new ArrayList<>();
                    for (String docId : sortedDocIds) {
                        sortedList.add(itemMap.get(docId));
                    }

                    localCache.put(dateKey, sortedList);
                    localCacheDocIds.put(dateKey, sortedDocIds);
                    scheduleItemAdapter.submitList(new ArrayList<>(sortedList), sortedDocIds);
                });
    }

    private void launchMapWithAllPlaces(Map<String, List<ScheduleItemDTO>> itemsMap) {
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
            Toast.makeText(this, "지도에 표시할 장소가 없습니다.", Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putParcelableArrayListExtra(MapActivity.EXTRA_PLACE_LAT_LNG_LIST, sortedLocations);
            intent.putIntegerArrayListExtra(MapActivity.EXTRA_PLACE_DAY_LIST, dayList);
            startActivity(intent);
        }
    }

    private void showAllPlacesOnMap() {
        helper.launchMapWithAllPlaces(dateList, localCache);
    }

    private void generateScheduleDates(Timestamp start, Timestamp end) {
        long diffMillis = end.toDate().getTime() - start.toDate().getTime();
        int days = (int) TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date current = start.toDate();
        dateList.clear();
        for (int i = 0; i < days; i++) {
            Date date = new Date(current.getTime() + TimeUnit.DAYS.toMillis(i));
            String dateString = sdf.format(date);
            dateList.add(dateString);
            Map<String, Object> scheduleDate = new HashMap<>();
            scheduleDate.put("dayIndex", i + 1);
            scheduleDate.put("date", dateString);
            db.collection("user")
                    .document(ownerUid)
                    .collection("schedule")
                    .document(scheduleId)
                    .collection("scheduleDate")
                    .document(dateString)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            doc.getReference().set(scheduleDate);
                        }
                    });
        }
        if (!dateList.isEmpty()) {
            Collections.sort(dateList);
            selectedDate = dateList.get(0);
            dateAdapter.setSelectedDate(selectedDate);
            listenScheduleItems(selectedDate);
            dateAdapter.notifyDataSetChanged();
        }
    }

    public List<ScheduleItemDTO> getCachedItemsForDate(String dateKey) {
        return localCache.getOrDefault(dateKey, new ArrayList<>());
    }

    public List<String> getCachedDocIdsForDate(String dateKey) {
        return localCacheDocIds.getOrDefault(dateKey, new ArrayList<>());
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentListener != null) currentListener.remove();
        if (postUpdateListener != null) postUpdateListener.remove();
    }

    public String getUserUid() {
        return userUid;
    }

    public String getOwnerUid() {return ownerUid;}

    public String getScheduleId() {
        return scheduleId;
    }

    public FirebaseFirestore getFirestore() {
        return db;
    }
}