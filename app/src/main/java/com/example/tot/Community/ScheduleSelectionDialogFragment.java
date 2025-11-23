package com.example.tot.Community;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.example.tot.Schedule.ScheduleAdapter;
import com.example.tot.Schedule.ScheduleDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ScheduleSelectionDialogFragment extends DialogFragment {

    private static final String TAG = "ScheduleSelection";

    private RecyclerView recyclerView;
    private ScheduleAdapter scheduleAdapter;
    private List<ScheduleDTO> scheduleList;
    private LinearLayout noScheduleLayout;
    private ImageButton btnClose;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_schedule_selection, container, false);

        recyclerView = view.findViewById(R.id.rv_schedules);
        noScheduleLayout = view.findViewById(R.id.layout_no_schedule);
        btnClose = view.findViewById(R.id.btn_close);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadSchedules();

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(requireActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                dismiss();
            }
        };
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        scheduleList = new ArrayList<>();

        scheduleAdapter = new ScheduleAdapter(scheduleList, (schedule, position) -> {
            String scheduleId = schedule.getScheduleId();
            if (scheduleId == null || scheduleId.isEmpty()) {
                Toast.makeText(getContext(), "스케줄 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "❌ scheduleId가 null입니다: " + schedule.getLocationName());
                return;
            }

            Log.d(TAG, "✅ 스케줄 선택됨 - ID: " + scheduleId + ", 지역: " + schedule.getLocationName());

            Intent intent = new Intent(getContext(), PostCreateActivity.class);
            intent.putExtra("scheduleId", scheduleId);
            intent.putExtra("locationName", schedule.getLocationName());
            intent.putExtra("startDate", schedule.getStartDate().toDate().getTime());
            intent.putExtra("endDate", schedule.getEndDate().toDate().getTime());
            startActivity(intent);
            dismiss();
        });

        recyclerView.setAdapter(scheduleAdapter);
    }

    private void loadSchedules() {
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "❌ 로그인된 사용자 없음");
            updateEmptyState();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Log.d(TAG, "📡 스케줄 로드 시작 - 경로: /user/" + uid + "/schedule");

        db.collection("user")
                .document(uid)
                .collection("schedule")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // ✅ 임시 리스트에 먼저 담기
                    List<ScheduleDTO> tempList = new ArrayList<>();

                    Log.d(TAG, "📦 받은 스케줄 수: " + querySnapshot.size());

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ScheduleDTO schedule = doc.toObject(ScheduleDTO.class);
                        if (schedule != null) {
                            // ✅ scheduleId 설정
                            schedule.setScheduleId(doc.getId());
                            tempList.add(schedule);

                            Log.d(TAG, "✅ 스케줄 추가됨 - ID: " + doc.getId() +
                                    ", 지역: " + schedule.getLocationName());
                        } else {
                            Log.w(TAG, "⚠️ 스케줄 파싱 실패 - Doc ID: " + doc.getId());
                        }
                    }

                    Log.d(TAG, "✅ 최종 스케줄 리스트 크기: " + tempList.size());

                    // ✅ UI 스레드에서 업데이트
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            scheduleList.clear();
                            scheduleList.addAll(tempList);
                            scheduleAdapter.notifyDataSetChanged();
                            updateEmptyState();

                            Log.d(TAG, "🔄 어댑터 업데이트 완료 - 표시할 항목 수: " + scheduleList.size());
                        });
                    } else {
                        Log.e(TAG, "❌ Activity가 null입니다");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 스케줄 로드 실패", e);

                    // ✅ UI 스레드에서 에러 처리
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "스케줄을 불러올 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            updateEmptyState();
                        });
                    }
                });
    }

    private void updateEmptyState() {
        if (scheduleList.isEmpty()) {
            noScheduleLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            Log.d(TAG, "ℹ️ 스케줄이 없어서 빈 화면 표시");
        } else {
            noScheduleLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            Log.d(TAG, "ℹ️ 스케줄 목록 표시 - " + scheduleList.size() + "개");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }
}