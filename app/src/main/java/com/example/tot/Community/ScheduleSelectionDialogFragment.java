package com.example.tot.Community;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
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
            // 스케줄 선택 시 게시글 작성 화면으로 이동
            Intent intent = new Intent(getContext(), PostCreateActivity.class);
            intent.putExtra("scheduleId", schedule.getScheduleId());
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
            updateEmptyState();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("user")
                .document(uid)
                .collection("schedule")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    scheduleList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ScheduleDTO schedule = doc.toObject(ScheduleDTO.class);
                        if (schedule != null) {
                            // Firestore Document ID를 ScheduleDTO에 주입하여 식별
                            schedule.setScheduleId(doc.getId());
                            scheduleList.add(schedule);
                        }
                    }

                    scheduleAdapter.updateData(scheduleList);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e("ScheduleSelection", "스케줄 로드 실패", e);
                    Toast.makeText(getContext(), "스케줄을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        if (scheduleList.isEmpty()) {
            noScheduleLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noScheduleLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
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