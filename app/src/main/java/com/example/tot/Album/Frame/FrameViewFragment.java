package com.example.tot.Album.Frame;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Album.AlbumDTO;
import com.example.tot.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrameViewFragment extends Fragment {

    private RecyclerView rvSections;

    private ArrayList<String> dateList;
    private String scheduleId, userUid;

    // Firestore에서 읽어올 날짜별 사진
    private Map<String, List<AlbumDTO>> photoMap = new HashMap<>();

    private FrameSectionAdapter adapter;

    private FirebaseFirestore db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            dateList = getArguments().getStringArrayList("dateList");
            scheduleId = getArguments().getString("scheduleId");
            userUid = getArguments().getString("userUid");
        }

        // 날짜별 Map 초기화
        for (String date : dateList) {
            photoMap.put(date, new ArrayList<>());
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_album_frame_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {

        rvSections = v.findViewById(R.id.rv_frame_view);

        adapter = new FrameSectionAdapter(dateList, photoMap, getContext());
        rvSections.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSections.setAdapter(adapter);

        loadPhotosFromFirestore();
    }

    // -------------------- 🔥 날짜별 Firestore에서 사진 로드 --------------------
    private void loadPhotosFromFirestore() {

        for (String dateKey : dateList) {

            db.collection("user")
                    .document(userUid)
                    .collection("schedule")
                    .document(scheduleId)
                    .collection("album")
                    .document(dateKey)
                    .collection("photos")
                    .orderBy("index")
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        List<AlbumDTO> list = new ArrayList<>();

                        for (DocumentSnapshot doc : snapshot) {
                            String imgUrl = doc.getString("imageUrl");
                            String comment = doc.getString("comment");
                            Long indexLong = doc.getLong("index");

                            int index = indexLong != null ? indexLong.intValue() : 0;

                            list.add(new AlbumDTO(imgUrl, comment, index));
                        }

                        photoMap.put(dateKey, list);
                        adapter.notifyDataSetChanged();
                    });
        }
    }
}
