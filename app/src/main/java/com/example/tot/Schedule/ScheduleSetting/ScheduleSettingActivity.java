package com.example.tot.Schedule.ScheduleSetting;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.example.tot.Schedule.ScheduleSetting.Invite.InviteDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 여행 일정 설정 화면
 * 상단: 날짜 리스트 (RecyclerView)
 * 하단: 각 날짜별 일정 리스트 (실시간 반영 + 캐시)
 */
public class ScheduleSettingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userUid, scheduleId, selectedDate;
    private Timestamp startDate, endDate;
    private RecyclerView rvDate, rvScheduleItem;
    private DateAdapter dateAdapter;
    private ScheduleItemAdapter scheduleItemAdapter;
    private List<String> dateList = new ArrayList<>();

    // ✅ 기존 일정 데이터 캐시
    private final Map<String, List<ScheduleItemDTO>> localCache = new HashMap<>();

    // ✅ 각 날짜별 문서 ID 캐시 (겹침 검사에서 자기 자신 제외용)
    private final Map<String, List<String>> localCacheDocIds = new HashMap<>();

    private Button btn_AddSchedule, btn_Menu, btn_Invite;
    private ListenerRegistration currentListener; // 실시간 리스너

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_setting);

        db = FirebaseFirestore.getInstance();
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        scheduleId = getIntent().getStringExtra("scheduleId");
        long startMillis = getIntent().getLongExtra("startDate", 0);
        long endMillis = getIntent().getLongExtra("endDate", 0);

        if (scheduleId == null || startMillis == 0 || endMillis == 0) {
            Toast.makeText(this, "스케줄 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startDate = new Timestamp(new Date(startMillis));
        endDate = new Timestamp(new Date(endMillis));

        rvDate = findViewById(R.id.rv_datelist);
        rvScheduleItem = findViewById(R.id.rv_schedulelist);
        btn_AddSchedule = findViewById(R.id.btn_add_schedule);
        btn_Menu = findViewById(R.id.btn_menu);
        btn_Invite = findViewById(R.id.btn_invite);

        setRvDate();
        setRvScheduleItem();
        generateScheduleDates(startDate, endDate);

        btn_Menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(ScheduleSettingActivity.this, v);
                menu.getMenuInflater().inflate(R.menu.schedule_menu, menu.getMenu());

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();

                        if (id == R.id.menu_map) {
                            Toast.makeText(ScheduleSettingActivity.this, "지도 클릭됨", Toast.LENGTH_SHORT).show();
                            return true;
                        } else if (id == R.id.menu_album) {
                            Toast.makeText(ScheduleSettingActivity.this, "앨범 클릭됨", Toast.LENGTH_SHORT).show();
                            return true;
                        } else if (id == R.id.menu_delete) {
                            Toast.makeText(ScheduleSettingActivity.this, "스케줄 삭제 클릭됨", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });
        btn_Invite.setOnClickListener(v -> {
            InviteDialog dialog = new InviteDialog(ScheduleSettingActivity.this);
            dialog.show();
        });
        // ✅ 일정 추가 버튼
        btn_AddSchedule.setOnClickListener(v -> {
            ScheduleBottomSheet bottom = new ScheduleBottomSheet(ScheduleSettingActivity.this);

            bottom.setOnScheduleSaveListener(item -> {
                if (selectedDate == null) {
                    Toast.makeText(this, "날짜가 선택되지 않았습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("user")
                        .document(userUid)
                        .collection("schedule")
                        .document(scheduleId)
                        .collection("scheduleDate")
                        .document(selectedDate)
                        .collection("scheduleItem")
                        .add(item)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "일정이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "일정 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });

            bottom.show();
        });
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
            ScheduleBottomSheet bottom = new ScheduleBottomSheet(ScheduleSettingActivity.this);
            bottom.showWithData(item, docID); // ✅ 수정 모드로 열기
            Toast.makeText(this, "클릭됨: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        });
        rvScheduleItem.setLayoutManager(new LinearLayoutManager(this));
        rvScheduleItem.setAdapter(scheduleItemAdapter);
    }

    /**
     * ✅ 실시간 반영 (Firestore snapshot listener)
     */
    private void listenScheduleItems(String dateKey) {
        if (currentListener != null) currentListener.remove();

        currentListener = db.collection("user")
                .document(userUid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        Log.e("Firestore", "❌ 실시간 데이터 수신 실패", e);
                        return;
                    }

                    List<ScheduleItemDTO> list = new ArrayList<>();
                    List<String> docIds = new ArrayList<>(); // ✅ 문서 ID 리스트 추가됨

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ScheduleItemDTO item = doc.toObject(ScheduleItemDTO.class);
                        if (item != null) {
                            list.add(item);
                            docIds.add(doc.getId()); // ✅ 문서 ID 함께 저장
                        }
                    }

                    // 🔹 시작시간 기준 정렬
                    list.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

                    // 🔹 캐시에 저장
                    localCache.put(dateKey, list);
                    localCacheDocIds.put(dateKey, docIds); // ✅ 문서 ID 캐시 추가됨

                    // 🔹 어댑터에 데이터 반영
                    scheduleItemAdapter.submitList(new ArrayList<>(list), docIds);
                    Log.d("Firestore", "⚡ 실시간 반영 완료: " + dateKey + " (" + list.size() + "개)");
                });
    }

    /**
     * ✅ 여행기간 기반 날짜 문서 자동 생성
     */
    private void generateScheduleDates(Timestamp start, Timestamp end) {
        long diffMillis = end.toDate().getTime() - start.toDate().getTime();
        int days = (int) TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date current = start.toDate();

        for (int i = 0; i < days; i++) {
            Date date = new Date(current.getTime() + TimeUnit.DAYS.toMillis(i));
            String dateString = sdf.format(date);
            dateList.add(dateString);

            Map<String, Object> scheduleDate = new HashMap<>();
            scheduleDate.put("dayIndex", i + 1);
            scheduleDate.put("date", dateString);

            db.collection("user")
                    .document(userUid)
                    .collection("schedule")
                    .document(scheduleId)
                    .collection("scheduleDate")
                    .document(dateString)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            doc.getReference().set(scheduleDate)
                                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "✅ 날짜 문서 생성됨: " + dateString))
                                    .addOnFailureListener(e -> Log.e("Firestore", "❌ 날짜 문서 생성 실패", e));
                        }
                    });
        }

        if (!dateList.isEmpty()) {
            selectedDate = dateList.get(0);
            dateAdapter.setSelectedDate(selectedDate);
            listenScheduleItems(selectedDate);
            dateAdapter.notifyDataSetChanged();
        }
    }

    /** ✅ 날짜별 일정 캐시 반환 */
    public List<ScheduleItemDTO> getCachedItemsForDate(String dateKey) {
        return localCache.getOrDefault(dateKey, new ArrayList<>());
    }

    /** ✅ 날짜별 문서 ID 캐시 반환 (겹침 검사용) */
    public List<String> getCachedDocIdsForDate(String dateKey) { // ✅ 추가됨
        return localCacheDocIds.getOrDefault(dateKey, new ArrayList<>());
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentListener != null) currentListener.remove();
    }

    public String getUserUid() {
        return userUid;
    }

    public String getScheduleId() {
        return scheduleId;
    }
    public FirebaseFirestore getFirestore() {return db;}
}
