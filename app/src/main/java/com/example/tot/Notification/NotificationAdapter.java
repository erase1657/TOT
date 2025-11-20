package com.example.tot.Notification;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private static final String TAG = "NotificationAdapter";

    private List<NotificationDTO> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationDTO notification);
        void onFollowBackClick(NotificationDTO notification);
    }

    public NotificationAdapter(List<NotificationDTO> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationDTO notification = notifications.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateData(List<NotificationDTO> newNotifications) {
        this.notifications.clear();
        if (newNotifications != null) {
            this.notifications.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private FrameLayout containerCard;
        private FrameLayout iconContainer;
        private ImageView iconImage;
        private TextView titleText;
        private TextView contentText;
        private TextView timeText;
        private FrameLayout followBackContainer;
        private TextView followBackButton;
        private FrameLayout unreadBadgeContainer;
        private TextView unreadBadge;

        // ✅ 팔로우 상태 추적
        private boolean isFollowing = false;
        private boolean isCheckingFollowStatus = false;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            containerCard = (FrameLayout) itemView;
            iconContainer = itemView.findViewById(R.id.icon_container);
            iconImage = itemView.findViewById(R.id.notification_icon);
            titleText = itemView.findViewById(R.id.notification_title);
            contentText = itemView.findViewById(R.id.notification_content);
            timeText = itemView.findViewById(R.id.notification_time);
            followBackContainer = itemView.findViewById(R.id.btn_follow_back_container);
            followBackButton = itemView.findViewById(R.id.btn_follow_back);
            unreadBadgeContainer = itemView.findViewById(R.id.unread_badge_container);
            unreadBadge = itemView.findViewById(R.id.unread_badge);
        }

        public void bind(NotificationDTO notification, OnNotificationClickListener listener) {
            // CardView 그림자 설정
            androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) containerCard;

            if (notification.isRead()) {
                // 읽은 메시지: 그림자 없음, 회색 배경
                cardView.setCardElevation(0);
                cardView.setCardBackgroundColor(Color.parseColor("#F9FAFB"));
            } else {
                // 읽지 않은 메시지: 그림자 있음, 흰색 배경
                cardView.setCardElevation(dpToPx(4));
                cardView.setCardBackgroundColor(Color.WHITE);
            }

            // 아이콘 컨테이너 설정 (동그란 박스)
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);

            // 타입별 아이콘 및 색상 설정
            switch (notification.getType()) {
                case SCHEDULE_INVITE:
                    // 스케줄: 보라색 박스, 아이콘
                    iconBg.setColor(Color.parseColor("#F1EDFF"));
                    iconContainer.setBackground(iconBg);
                    iconImage.setImageResource(R.drawable.ic_schedule);
                    iconImage.setColorFilter(Color.parseColor("#5A34D7"));

                    // 제목 설정 (일정 이름만 굵게)
                    String scheduleTitle = notification.getTitle();
                    String scheduleName = scheduleTitle.split(" 여행 일정")[0];
                    SpannableString titleSpan = new SpannableString(scheduleTitle);
                    int scheduleNameEnd = scheduleName.length();
                    titleSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, scheduleNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleText.setText(titleSpan);

                    // 내용 설정 (일반 텍스트)
                    contentText.setText(notification.getContent());

                    // 읽지 않은 메시지 배지 표시
                    followBackContainer.setVisibility(View.GONE);
                    if (!notification.isRead() && notification.getUnreadCount() > 0) {
                        unreadBadgeContainer.setVisibility(View.VISIBLE);
                        String badgeText = notification.getUnreadCount() > 10 ?
                                "10+" : String.valueOf(notification.getUnreadCount());
                        unreadBadge.setText(badgeText);

                        // 배지 동그라미 배경
                        GradientDrawable badgeBg = new GradientDrawable();
                        badgeBg.setShape(GradientDrawable.OVAL);
                        badgeBg.setColor(Color.parseColor("#0F9687"));
                        unreadBadge.setBackground(badgeBg);
                    } else {
                        unreadBadgeContainer.setVisibility(View.GONE);
                    }
                    break;

                case FOLLOW:
                    // 팔로우: 연한 초록색 박스, 아이콘
                    iconBg.setColor(Color.parseColor("#E8F8EA"));
                    iconContainer.setBackground(iconBg);
                    iconImage.setImageResource(R.drawable.ic_user_add);
                    iconImage.setColorFilter(Color.parseColor("#29C63F"));

                    // 제목 설정 (사용자 이름만 굵게)
                    String followTitle = notification.getTitle();
                    String userName = notification.getUserName();
                    SpannableString followTitleSpan = new SpannableString(followTitle);
                    int userNameEnd = userName.length();
                    followTitleSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, userNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleText.setText(followTitleSpan);

                    // 내용 설정 (일반 텍스트)
                    contentText.setText(notification.getContent());

                    // ✅ 맞팔로우 버튼 표시 (팔로우 상태 확인 후 동적 텍스트)
                    unreadBadgeContainer.setVisibility(View.GONE);
                    followBackContainer.setVisibility(View.VISIBLE);

                    // ✅ Firestore에서 팔로우 상태 확인
                    checkFollowStatus(notification.getUserId(), isFollowing -> {
                        this.isFollowing = isFollowing;
                        updateFollowButton(isFollowing);
                    });

                    followBackButton.setOnClickListener(v -> {
                        if (listener != null && !isCheckingFollowStatus) {
                            listener.onFollowBackClick(notification);
                        }
                    });
                    break;

                case COMMENT:
                    // 댓글: 핑크색 박스, 빨간 아이콘
                    iconBg.setColor(Color.parseColor("#FFEFF1"));
                    iconContainer.setBackground(iconBg);
                    iconImage.setImageResource(R.drawable.ic_comment);
                    iconImage.setColorFilter(Color.parseColor("#BA0017"));

                    // 제목 설정 (사용자 이름만 굵게)
                    String commentTitle = notification.getTitle();
                    String commenterName = notification.getUserName();
                    SpannableString commentTitleSpan = new SpannableString(commentTitle);
                    int commenterNameEnd = commenterName.length();
                    commentTitleSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, commenterNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleText.setText(commentTitleSpan);

                    // 내용 설정 (일반 텍스트)
                    contentText.setText(notification.getContent());

                    // 읽지 않은 메시지 배지 표시
                    followBackContainer.setVisibility(View.GONE);
                    if (!notification.isRead() && notification.getUnreadCount() > 0) {
                        unreadBadgeContainer.setVisibility(View.VISIBLE);
                        String badgeText = notification.getUnreadCount() > 10 ?
                                "10+" : String.valueOf(notification.getUnreadCount());
                        unreadBadge.setText(badgeText);

                        // 배지 동그라미 배경
                        GradientDrawable badgeBg = new GradientDrawable();
                        badgeBg.setShape(GradientDrawable.OVAL);
                        badgeBg.setColor(Color.parseColor("#0F9687"));
                        unreadBadge.setBackground(badgeBg);
                    } else {
                        unreadBadgeContainer.setVisibility(View.GONE);
                    }
                    break;
            }

            // 시간 표시
            timeText.setText(notification.getTimeDisplay());

            // 전체 카드 클릭 이벤트
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }

        /**
         * ✅ Firestore에서 팔로우 상태 확인
         */
        private void checkFollowStatus(String targetUserId, FollowStatusCallback callback) {
            if (targetUserId == null || targetUserId.isEmpty()) {
                callback.onResult(false);
                return;
            }

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() == null) {
                callback.onResult(false);
                return;
            }

            isCheckingFollowStatus = true;
            String myUid = mAuth.getCurrentUser().getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("user")
                    .document(myUid)
                    .collection("following")
                    .document(targetUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        isCheckingFollowStatus = false;
                        callback.onResult(doc.exists());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "팔로우 상태 확인 실패", e);
                        isCheckingFollowStatus = false;
                        callback.onResult(false);
                    });
        }

        /**
         * ✅ 팔로우 버튼 동적 텍스트 업데이트
         */
        private void updateFollowButton(boolean isFollowing) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(dpToPx(12));

            if (isFollowing) {
                // 이미 팔로우 중 → "팔로우 중" (회색)
                followBackButton.setText("팔로우 중");
                btnBg.setColor(Color.parseColor("#E0E0E0"));
                followBackButton.setTextColor(Color.parseColor("#666666"));
            } else {
                // 팔로우 안 함 → "맞팔로우" (검은색)
                followBackButton.setText("맞팔로우");
                btnBg.setColor(Color.parseColor("#000000"));
                followBackButton.setTextColor(Color.parseColor("#FFFFFF"));
            }

            followBackButton.setBackground(btnBg);
        }

        private int dpToPx(int dp) {
            float density = itemView.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }

        /**
         * ✅ 팔로우 상태 확인 콜백 인터페이스
         */
        interface FollowStatusCallback {
            void onResult(boolean isFollowing);
        }
    }
}