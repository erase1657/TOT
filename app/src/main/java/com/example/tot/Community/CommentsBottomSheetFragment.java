package com.example.tot.Community;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.example.tot.User.UserCache;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "CommentsBottomSheet";
    private static final String ARG_POST_ID = "postId";

    private String postId;

    private RecyclerView rvComments;
    private EditText etComment;
    private Button btnSendComment;

    private CommentAdapter commentAdapter;
    private List<CommentDTO> commentList = new ArrayList<>();

    private FirebaseFirestore db;
    private CollectionReference communityPostsRef;

    public static CommentsBottomSheetFragment newInstance(String postId) {
        CommentsBottomSheetFragment fragment = new CommentsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
        }
        db = FirebaseFirestore.getInstance();
        communityPostsRef = db.collection("public").document("community").collection("posts");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_comments, container, false);

        rvComments = view.findViewById(R.id.rv_comments);
        etComment = view.findViewById(R.id.et_comment);
        btnSendComment = view.findViewById(R.id.btn_send_comment);

        setupRecyclerView();
        loadComments();

        btnSendComment.setOnClickListener(v -> postComment());

        return view;
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(getContext(), commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(commentAdapter);
        commentAdapter.setOnDeleteButtonClickListener(comment -> {
            showDeleteConfirmDialog(comment);
        });
    }

    private void loadComments() {
        if (postId == null) return;

        communityPostsRef.document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    commentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CommentDTO comment = document.toObject(CommentDTO.class);
                        comment.setCommentId(document.getId());
                        commentList.add(comment);
                    }
                    commentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading comments", e);
                    Toast.makeText(getContext(), "댓글을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void postComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(getContext(), "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UserCache.getUser(uid, user -> {
            if (user != null) {
                CommentDTO newComment = new CommentDTO(
                        postId,
                        uid,
                        user.getNickname(),
                        user.getProfileImageUrl(),
                        content,
                        Timestamp.now()
                );

                communityPostsRef.document(postId).collection("comments")
                        .add(newComment)
                        .addOnSuccessListener(documentReference -> {
                            etComment.setText("");
                            loadComments();

                            // ✅ 댓글 작성 후 게시글 작성자에게 알림 전송
                            sendCommentNotificationToPostAuthor(uid, user.getNickname(), content);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error posting comment", e);
                            Toast.makeText(getContext(), "댓글 등록에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getContext(), "사용자 정보를 찾을 수 없어 댓글을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ 댓글 작성 시 게시글 작성자에게 알림 전송 (수정됨)
     */
    private void sendCommentNotificationToPostAuthor(String commenterId, String commenterName, String commentContent) {
        if (postId == null) {
            Log.w(TAG, "❌ postId가 null입니다");
            return;
        }

        // 게시글 정보 조회
        communityPostsRef.document(postId)
                .get()
                .addOnSuccessListener(postDoc -> {
                    if (!postDoc.exists()) {
                        Log.w(TAG, "❌ 게시글을 찾을 수 없습니다");
                        return;
                    }

                    String postAuthorId = postDoc.getString("authorUid");

                    // ✅ 본인 게시글에는 알림 보내지 않음
                    if (postAuthorId == null || postAuthorId.equals(commenterId)) {
                        Log.d(TAG, "본인 게시글이므로 알림 전송 안 함");
                        return;
                    }

                    // ✅ 게시글 작성자의 commentNotifications 컬렉션에 알림 추가
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("postId", postId);
                    notificationData.put("commenterId", commenterId);
                    notificationData.put("commenterName", commenterName);
                    notificationData.put("commentContent", commentContent);
                    notificationData.put("timestamp", System.currentTimeMillis());
                    notificationData.put("isRead", false);

                    db.collection("user")
                            .document(postAuthorId)
                            .collection("commentNotifications")
                            .add(notificationData)
                            .addOnSuccessListener(docRef -> {
                                Log.d(TAG, "✅ 댓글 알림 전송 성공: " + postAuthorId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 댓글 알림 전송 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 게시글 조회 실패", e);
                });
    }

    private void showDeleteConfirmDialog(CommentDTO comment) {
        new AlertDialog.Builder(getContext())
                .setTitle("댓글 삭제")
                .setMessage("이 댓글을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteComment(comment))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteComment(CommentDTO comment) {
        if (postId == null || comment.getCommentId() == null) {
            Toast.makeText(getContext(), "오류: 댓글 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        communityPostsRef.document(postId).collection("comments").document(comment.getCommentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadComments();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting comment", e);
                    Toast.makeText(getContext(), "댓글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }
}