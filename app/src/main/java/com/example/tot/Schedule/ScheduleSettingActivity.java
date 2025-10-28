package com.example.tot.Schedule;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 상단 날짜 리스트 + 하단 일정 리스트 연동 예제
 */
public class ScheduleSettingActivity extends AppCompatActivity {

    // ✅ Firestore 관련
    private FirebaseFirestore db;
    private String userUid;
    private String scheduleId = "exampleScheduleId"; // 실제로는 intent로 전달받는 값

    // ✅ UI 컴포넌트
    private RecyclerView rvDateList;
    private RecyclerView rvScheduleList;

    // ✅ 어댑터
    private DateAdapter dateAdapter;
    private ScheduleItemAdapter scheduleItemAdapter;

    // ✅ 데이터 변수
    private List<LocalDate> dateList = new ArrayList<>();
    private LocalDate selectedDate;

    private Button btn_AddSchedule;

    private ScheduleItemAdapter adapter;
    private List<ScheduleItem> scheduleList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
      ;
        // 1️⃣ Firestore 초기화
        db = FirebaseFirestore.getInstance();
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 2️⃣ RecyclerView 참조
        rvDateList = findViewById(R.id.rv_datelist);
        rvScheduleList = findViewById(R.id.rv_schedulelist);

        btn_AddSchedule = findViewById(R.id.btn_add_schedule);

        // 3️⃣ 날짜 리스트 생성 (예: 오늘 기준 7일)
        generateDateList();

        // 4️⃣ 상단 날짜 어댑터 설정
        dateAdapter = new DateAdapter(dateList, date -> {
            selectedDate = date;
            loadScheduleForDate(date); // 날짜 클릭 시 일정 불러오기
        });
        rvDateList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDateList.setAdapter(dateAdapter);

        // 5️⃣ 하단 일정 어댑터 설정
        scheduleItemAdapter = new ScheduleItemAdapter(item -> {
            // TODO: 클릭 시 일정 수정 다이얼로그 띄우기 등

        });
        rvScheduleList.setLayoutManager(new LinearLayoutManager(this));
        rvScheduleList.setAdapter(scheduleItemAdapter);
        RecyclerView recyclerView = findViewById(R.id.rv_schedulelist);
        adapter = new ScheduleItemAdapter(item -> {
            // 아이템 클릭 시 동작 (예: 수정)
            Toast.makeText(this, "클릭됨: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        // 6️⃣ 초기 선택 날짜 = 첫 날짜
        selectedDate = dateList.get(0);
        dateAdapter.setSelectedDate(selectedDate);
        loadScheduleForDate(selectedDate);

        btn_AddSchedule.setOnClickListener(v -> {
            ScheduleBottomSheet bottom = new ScheduleBottomSheet(ScheduleSettingActivity.this);

            // ✅ 여기 추가: 바텀시트에 콜백 연결
            bottom.setOnScheduleSaveListener(item -> {
                List<ScheduleItem> currentList = new ArrayList<>(adapter.getCurrentList());
                currentList.add(item);
                adapter.submitList(currentList);
                Toast.makeText(this, "일정이 추가되었습니다: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            });

            bottom.show();
        });
    }

    /**
     * Firestore에서 선택된 날짜의 일정 데이터를 불러옵니다.
     */
    private void loadScheduleForDate(LocalDate date) {
        String dateKey = date.toString(); // ex) "2025-10-25"

        db.collection("schedule")
                .document(scheduleId)
                .collection("scheduleItem")
                .whereEqualTo("date", dateKey)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ScheduleItem> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ScheduleItem item = doc.toObject(ScheduleItem.class);
                        if (item != null) {
                            list.add(item);
                        }
                    }
                    scheduleItemAdapter.submitList(list);
                    Log.d("Firestore", "불러온 일정 개수: " + list.size());
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "일정 불러오기 실패", e));
    }

    /**
     * 오늘 기준 7일 날짜 리스트 생성
     */
    private void generateDateList() {
        LocalDate today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now(ZoneId.systemDefault());
        }
        for (int i = 0; i < 10; i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dateList.add(today.plusDays(i));
            }
        }
    }
}
