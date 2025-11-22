package com.example.tot.User;

import com.example.tot.Authentication.UserDTO;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserCache {

    private static final Map<String, UserDTO> userCache = new HashMap<>();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface UserCallback {
        void onUserLoaded(UserDTO user);
    }

    public static void getUser(String uid, UserCallback callback) {
        if (userCache.containsKey(uid)) {
            callback.onUserLoaded(userCache.get(uid));
            return;
        }

        db.collection("user").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserDTO user = documentSnapshot.toObject(UserDTO.class);
                        if (user != null) {
                            userCache.put(uid, user);
                            callback.onUserLoaded(user);
                        }
                    } else {
                        callback.onUserLoaded(null);
                    }
                })
                .addOnFailureListener(e -> callback.onUserLoaded(null));
    }

    public static void clearCache() {
        userCache.clear();
    }
}