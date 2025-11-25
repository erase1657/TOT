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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.Follow.FollowButtonHelper;
import com.example.tot.R;

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

        private boolean isFollowing = false;
        private boolean isFollower = false;

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
            androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) containerCard;

            if (notification.isRead()) {
                cardView.setCardElevation(0);
                cardView.setCardBackgroundColor(Color.parseColor("#F9FAFB"));
            } else {
                cardView.setCardElevation(dpToPx(4));
                cardView.setCardBackgroundColor(Color.WHITE);
            }

            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);

            switch (notification.getType()) {
                case SCHEDULE_INVITE:
                    iconBg.setColor(Color.parseColor("#F1EDFF"));
                    iconContainer.setBackground(iconBg);
                    iconImage.setImageResource(R.drawable.ic_schedule);
                    iconImage.setColorFilter(Color.parseColor("#5A34D7"));

                    String scheduleTitle = notification.getTitle();
                    String scheduleName = scheduleTitle.split(" 여행 일정")[0];
                    SpannableString titleSpan = new SpannableString(scheduleTitle);
                    int scheduleNameEnd = scheduleName.length();
                    titleSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, scheduleNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleText.setText(titleSpan);

                    contentText.setText(notification.getContent());

                    followBackContainer.setVisibility(View.GONE);
                    if (!notification.isRead() && notification.getUnreadCount() > 0) {
                        unreadBadgeContainer.setVisibility(View.VISIBLE);
                        String badgeText = notification.getUnreadCount() > 10 ?
                                "10+" : String.valueOf(notification.getUnreadCount());
                        unreadBadge.setText(badgeText);

                        GradientDrawable badgeBg = new GradientDrawable();
                        badgeBg.setShape(GradientDrawable.OVAL);
                        badgeBg.setColor(Color.parseColor("#0F9687"));
                        unreadBadge.setBackground(badgeBg);
                    } else {
                        unreadBadgeContainer.setVisibility(View.GONE);
                    }
                    break;

                case FOLLOW:
                    iconBg.setColor(Color.parseColor("#E8F8EA"));
                    iconContainer.setBackground(iconBg);
                    iconImage.setImageResource(R.drawable.ic_user_add);
                    iconImage.setColorFilter(Color.parseColor("#29C63F"));

                    String followTitle = notification.getTitle();
                    String userName = notification.getUserName();
                    SpannableString followTitleSpan = new SpannableString(followTitle);
                    int userNameEnd = userName.length();
                    followTitleSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, userNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleText.setText(followTitleSpan);

                    contentText.setText(notification.getContent());

                    unreadBadgeContainer.setVisibility(View.GONE);
                    followBackContainer.setVisibility(View.VISIBLE);

                    FollowButtonHelper.checkFollowStatus(notification.getUserId(), (following, follower) -> {
                        isFollowing = following;
                        isFollower = follower;
                        FollowButtonHelper.updateFollowButton(followBackButton, isFollowing, isFollower);
                    });

                    followBackButton.setOnClickListener(v -> {
                        FollowButtonHelper.handleFollowButtonClick(
                                itemView.getContext(),
                                notification.getUserId(),
                                isFollowing,
                                isFollower,
                                new FollowButtonHelper.FollowActionCallback() {
                                    @Override
                                    public void onSuccess(boolean nowFollowing) {
                                        isFollowing = nowFollowing;
                                        FollowButtonHelper.updateFollowButton(followBackButton, isFollowing, isFollower);

                                        NotificationManager.getInstance().markAsRead(notification.getId());
                                        notification.setRead(true);
                                    }

                                    @Override
                                    public void onFailure(String message) {
                                        Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    });
                    break;

                case COMMENT:
                    iconBg.setColor(Color.parseColor("#FFEFF1"));
                    iconContainer.setBackground(iconBg);
                    iconImage.setImageResource(R.drawable.ic_comment);
                    iconImage.setColorFilter(Color.parseColor("#BA0017"));

                    String commentTitle = notification.getTitle();
                    String commenterName = notification.getUserName();
                    SpannableString commentTitleSpan = new SpannableString(commentTitle);
                    int commenterNameEnd = commenterName.length();
                    commentTitleSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, commenterNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleText.setText(commentTitleSpan);

                    contentText.setText(notification.getContent());

                    followBackContainer.setVisibility(View.GONE);
                    if (!notification.isRead() && notification.getUnreadCount() > 0) {
                        unreadBadgeContainer.setVisibility(View.VISIBLE);
                        String badgeText = notification.getUnreadCount() > 10 ?
                                "10+" : String.valueOf(notification.getUnreadCount());
                        unreadBadge.setText(badgeText);

                        GradientDrawable badgeBg = new GradientDrawable();
                        badgeBg.setShape(GradientDrawable.OVAL);
                        badgeBg.setColor(Color.parseColor("#0F9687"));
                        unreadBadge.setBackground(badgeBg);
                    } else {
                        unreadBadgeContainer.setVisibility(View.GONE);
                    }
                    break;

                case POST:  // ✅ 친구 게시글 알림 UI
                    iconBg.setColor(Color.parseColor("#FFF4E8"));  // 연한 주황색 배경
                    iconContainer.setBackground(iconBg);
                    iconImage.setImageResource(R.drawable.ic_community);
                    iconImage.setColorFilter(Color.parseColor("#FF8A00"));  // 주황색 아이콘

                    String postTitle = notification.getTitle();
                    String authorName = notification.getUserName();
                    SpannableString postTitleSpan = new SpannableString(postTitle);
                    int authorNameEnd = authorName.length();
                    postTitleSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, authorNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleText.setText(postTitleSpan);

                    contentText.setText(notification.getContent());

                    followBackContainer.setVisibility(View.GONE);
                    if (!notification.isRead() && notification.getUnreadCount() > 0) {
                        unreadBadgeContainer.setVisibility(View.VISIBLE);
                        String badgeText = notification.getUnreadCount() > 10 ?
                                "10+" : String.valueOf(notification.getUnreadCount());
                        unreadBadge.setText(badgeText);

                        GradientDrawable badgeBg = new GradientDrawable();
                        badgeBg.setShape(GradientDrawable.OVAL);
                        badgeBg.setColor(Color.parseColor("#0F9687"));
                        unreadBadge.setBackground(badgeBg);
                    } else {
                        unreadBadgeContainer.setVisibility(View.GONE);
                    }
                    break;
            }

            timeText.setText(notification.getTimeDisplay());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }

        private int dpToPx(int dp) {
            float density = itemView.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}