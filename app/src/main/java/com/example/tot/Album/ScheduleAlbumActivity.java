package com.example.tot.Album;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;


import com.example.tot.Album.Edit.EditViewFragment;
import com.example.tot.Album.Frame.FrameViewFragment;
import com.example.tot.R;

import java.util.ArrayList;

public class ScheduleAlbumActivity extends AppCompatActivity {

    private SwitchCompat swMode;

    private String scheduleId, userUid;
    private ArrayList<String> dateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_schedule_album);

        swMode = findViewById(R.id.sw_mode);

        scheduleId = getIntent().getStringExtra("scheduleId");
        userUid = getIntent().getStringExtra("userUid");
        dateList = getIntent().getStringArrayListExtra("dateList");

        // 기본 Fragment (읽기 모드)
        loadFragment(createFragmentWithArgs(new FrameViewFragment()));

        swMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                loadFragment(createFragmentWithArgs(new EditViewFragment()));
            } else {
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
        args.putString("userUid", userUid);
        args.putStringArrayList("dateList", dateList);
        fragment.setArguments(args);
        return fragment;
    }
}
