package com.example.tot.Schedule.ScheduleSetting.Invite;

import com.example.tot.Authentication.UserDTO;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class InvitedMemberLoader {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnMembersLoadedListener {
        void onLoaded(UserDTO owner, List<UserDTO> invited);
    }

    /**
     * owner + invited 멤버를 Firestore에서 읽어오는 메인 함수
     */
    public void loadMembers(String ownerUid, String scheduleId, OnMembersLoadedListener listener) {

        // 1) owner 정보 먼저 불러온다
        db.collection("user")
                .document(ownerUid)
                .get()
                .addOnSuccessListener(ownerDoc -> {

                    UserDTO owner = ownerDoc.toObject(UserDTO.class);
                    if (owner == null) {
                        listener.onLoaded(null, new ArrayList<>());
                        return;
                    }

                    // 2) invited 서브컬렉션에서 초대된 UID들 얻기
                    db.collection("user")
                            .document(ownerUid)
                            .collection("schedule")
                            .document(scheduleId)
                            .collection("invited")
                            .get()
                            .addOnSuccessListener(inviteQuery -> {

                                List<String> uidList = new ArrayList<>();

                                for (DocumentSnapshot d : inviteQuery.getDocuments()) {
                                    uidList.add(d.getId());  // 문서 ID가 UID임
                                }

                                // 초대된 멤버 없으면 owner만 반환
                                if (uidList.isEmpty()) {
                                    listener.onLoaded(owner, new ArrayList<>());
                                    return;
                                }

                                // 3) invited UID를 기반으로 user 컬렉션에서 정보 가져오기
                                db.collection("user")
                                        .whereIn("UID", uidList)
                                        .get()
                                        .addOnSuccessListener(userQuery -> {

                                            List<UserDTO> invitedList = new ArrayList<>();

                                            for (DocumentSnapshot udoc : userQuery.getDocuments()) {
                                                UserDTO member = udoc.toObject(UserDTO.class);
                                                invitedList.add(member);
                                            }

                                            listener.onLoaded(owner, invitedList);
                                        });
                            });
                });
    }
}
