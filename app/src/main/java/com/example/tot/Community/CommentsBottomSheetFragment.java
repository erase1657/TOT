package com.example.tot.Community;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.List;

public class CommentsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "CommentsBottomSheet";
    private static final String ARG_POST_ID = "postId";

    private String postId;

    private RecyclerView rvComments;
    private EditText etComment;
    private Button btnSendComment;
    private TextView tvCommentCount;

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
        tvCommentCount = view.findViewById(R.id.tv_comment_count);

        setupRecyclerView();
        loadComments();

        btnSendComment.setOnClickListener(v -> postComment());

        return view;
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(getContext(), commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(commentAdapter);
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
                    updateCommentCount();
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
                            loadComments(); // 댓글 목록 새로고침
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

    private void updateCommentCount() {
        tvCommentCount.setText("댓글 " + commentList.size() + "개");
    }
}