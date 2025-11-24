package com.example.tot.User;

import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.tot.R;

/**
 * 프로필 이미지 로딩 통합 헬퍼
 * - Glide를 사용한 이미지 로딩 중복 제거
 * - 캐싱 전략 통일
 */
public class ProfileImageHelper {

    /**
     * 프로필 이미지 로드 (기본 플레이스홀더 사용)
     */
    public static void loadProfileImage(@NonNull ImageView imageView, String profileUrl) {
        loadProfileImage(imageView, profileUrl, R.drawable.ic_profile_default);
    }

    /**
     * 프로필 이미지 로드 (커스텀 플레이스홀더)
     */
    public static void loadProfileImage(@NonNull ImageView imageView,
                                        String profileUrl,
                                        int placeholderResId) {
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(profileUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(placeholderResId)
                    .error(placeholderResId)
                    .into(imageView);
        } else {
            imageView.setImageResource(placeholderResId);
        }
    }

    /**
     * 배경 이미지 로드 (centerCrop 적용)
     */
    public static void loadBackgroundImage(@NonNull ImageView imageView,
                                           String backgroundUrl,
                                           int defaultResId) {
        if (backgroundUrl != null && !backgroundUrl.isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(backgroundUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(defaultResId);
        }
    }

    /**
     * 원형 프로필 이미지 로드 (CircleImageView용)
     */
    public static void loadCircleProfileImage(@NonNull ImageView imageView, String profileUrl) {
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(profileUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .circleCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_profile_default);
        }
    }

    /**
     * URI로 프로필 이미지 로드 (편집 모드용)
     */
    public static void loadProfileImageFromUri(@NonNull ImageView imageView, Uri imageUri) {
        if (imageUri != null) {
            Glide.with(imageView.getContext())
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_profile_default);
        }
    }

    /**
     * URI로 배경 이미지 로드 (편집 모드용)
     */
    public static void loadBackgroundImageFromUri(@NonNull ImageView imageView, Uri imageUri, int defaultResId) {
        if (imageUri != null) {
            Glide.with(imageView.getContext())
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(defaultResId);
        }
    }
}