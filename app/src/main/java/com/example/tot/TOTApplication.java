package com.example.tot;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class TOTApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ✅ Firestore 캐시(로컬 영구 저장) 설정
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // 캐시 활성화
                .build();
        firestore.setFirestoreSettings(settings);
    }
}