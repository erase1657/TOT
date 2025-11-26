package com.example.tot.Album;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;


import com.example.tot.Album.Edit.EditViewFragment;
import com.example.tot.Album.Frame.FrameViewFragment;
import com.example.tot.R;

import java.util.ArrayList;

public class ScheduleAlbumActivity extends AppCompatActivity {


    private ImageButton btnModeChange;
    private Button btnBack;
    private String scheduleId, userUid;
    private ArrayList<String> dateList;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_schedule_album);


        btnModeChange = findViewById(R.id.btn_mode_change);
        btnBack = findViewById(R.id.btn_back);
        scheduleId = getIntent().getStringExtra("scheduleId");
        userUid = getIntent().getStringExtra("ownerUid");
        dateList = getIntent().getStringArrayListExtra("dateList");

        // 기본 Fragment (읽기 모드)
        loadFragment(createFragmentWithArgs(new FrameViewFragment()));

        btnBack.setOnClickListener(v -> {
            finish(); // 현재 액티비티 종료
        });

        btnModeChange.setOnClickListener(v -> {
            // 모드 반전
            isEditMode = !isEditMode;

            if (isEditMode) {
                // 편집 모드로 전환
                btnModeChange.setBackgroundResource(R.drawable.ic_frame);
                loadFragment(createFragmentWithArgs(new EditViewFragment()));
            } else {
                // 읽기 모드로 전환
                btnModeChange.setBackgroundResource(R.drawable.ic_album_edit);
                loadFragment(createFragmentWithArgs(new FrameViewFragment()));
            }
        });

    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_fragment, fragment)
                .commit();
    }

    private Fragment createFragmentWithArgs(Fragment fragment) {
        Bundle args = new Bundle();
        args.putString("scheduleId", scheduleId);
        args.putString("ownerUid", userUid);
        args.putStringArrayList("dateList", dateList);
        fragment.setArguments(args);
        return fragment;
    }
}
