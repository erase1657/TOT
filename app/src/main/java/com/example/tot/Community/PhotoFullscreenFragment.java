package com.example.tot.Community;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.tot.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PhotoFullscreenFragment extends DialogFragment {

    private static final String TAG = "PhotoFullscreenFragment";
    private static final String ARG_PHOTO_URLS = "photo_urls";
    private static final String ARG_CURRENT_POSITION = "current_position";
    private static final String ARG_SHOW_INDICATOR = "show_indicator";

    private ViewPager2 viewPager;
    private TextView indicator;
    private ImageButton backButton;
    private ImageButton saveButton;
    private ArrayList<String> photoUrls;
    private int currentPosition;
    private boolean showIndicator = true;

    private ActivityResultLauncher<String> permissionLauncher;

    public static PhotoFullscreenFragment newInstance(ArrayList<String> photoUrls, int currentPosition) {
        return newInstance(photoUrls, currentPosition, true);
    }

    public static PhotoFullscreenFragment newInstance(ArrayList<String> photoUrls, int currentPosition, boolean showIndicator) {
        PhotoFullscreenFragment fragment = new PhotoFullscreenFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PHOTO_URLS, photoUrls);
        args.putInt(ARG_CURRENT_POSITION, currentPosition);
        args.putBoolean(ARG_SHOW_INDICATOR, showIndicator);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (getArguments() != null) {
            photoUrls = getArguments().getStringArrayList(ARG_PHOTO_URLS);
            currentPosition = getArguments().getInt(ARG_CURRENT_POSITION);
            showIndicator = getArguments().getBoolean(ARG_SHOW_INDICATOR, true);
        }

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        saveCurrentPhoto();
                    } else {
                        Toast.makeText(getContext(), "저장 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_fullscreen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.viewpager_fullscreen);
        indicator = view.findViewById(R.id.tv_fullscreen_indicator);
        backButton = view.findViewById(R.id.btn_back);
        saveButton = view.findViewById(R.id.btn_save);

        FullscreenPhotoAdapter adapter = new FullscreenPhotoAdapter(photoUrls, viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        if (showIndicator) {
            indicator.setVisibility(View.VISIBLE);
            updateIndicator(currentPosition);
        } else {
            indicator.setVisibility(View.GONE);
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                if (showIndicator) {
                    updateIndicator(position);
                }
            }
        });

        backButton.setOnClickListener(v -> dismiss());
        saveButton.setOnClickListener(v -> checkPermissionAndSave());
    }

    private void updateIndicator(int position) {
        indicator.setText((position + 1) + " / " + photoUrls.size());
    }

    private void checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveCurrentPhoto();
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                saveCurrentPhoto();
            } else {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void saveCurrentPhoto() {
        if (currentPosition < 0 || currentPosition >= photoUrls.size()) {
            Toast.makeText(getContext(), "저장할 사진이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String photoUrl = photoUrls.get(currentPosition);

        Glide.with(requireContext())
                .asBitmap()
                .load(photoUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        saveBitmapToGallery(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Toast.makeText(getContext(), "이미지 다운로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        OutputStream fos;
        String fileName = "TOT_" + System.currentTimeMillis() + ".jpg";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TOT");

                Uri imageUri = requireContext().getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (imageUri != null) {
                    fos = requireContext().getContentResolver().openOutputStream(imageUri);
                    if (fos != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.close();
                        Toast.makeText(getContext(), "갤러리에 저장되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString() + "/TOT";
                File dir = new File(imagesDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File image = new File(imagesDir, fileName);
                fos = new FileOutputStream(image);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, image.getAbsolutePath());
                requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                Toast.makeText(getContext(), "갤러리에 저장되었습니다", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "이미지 저장 실패", e);
            Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static class FullscreenPhotoAdapter extends RecyclerView.Adapter<FullscreenPhotoAdapter.PhotoViewHolder> {

        private final List<String> photoUrls;
        private final ViewPager2 viewPager;

        public FullscreenPhotoAdapter(List<String> photoUrls, ViewPager2 viewPager) {
            this.photoUrls = photoUrls;
            this.viewPager = viewPager;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ZoomableImageView imageView = new ZoomableImageView(parent.getContext(), viewPager);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new PhotoViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext())
                    .load(photoUrls.get(position))
                    .into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return photoUrls.size();
        }

        static class PhotoViewHolder extends RecyclerView.ViewHolder {
            public PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    /**
     * ✨ 개선된 확대/축소 ImageView
     * - ViewPager2와의 터치 충돌 해결
     * - 확대 시 ViewPager 스크롤 비활성화
     */
    private static class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {

        private static final float MIN_SCALE = 1f;
        private static final float MAX_SCALE = 5f;
        private static final float DOUBLE_TAP_SCALE = 3f;

        private Matrix matrix = new Matrix();
        private float[] matrixValues = new float[9];

        private float scale = 1f;
        private PointF last = new PointF();
        private PointF start = new PointF();

        private ScaleGestureDetector scaleDetector;
        private GestureDetector gestureDetector;
        private ViewPager2 viewPager; // ✨ ViewPager 참조 추가

        public ZoomableImageView(Context context, ViewPager2 viewPager) {
            super(context);
            this.viewPager = viewPager;
            init();
        }

        private void init() {
            setScaleType(ScaleType.MATRIX);

            scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
            gestureDetector = new GestureDetector(getContext(), new GestureListener());

            setOnTouchListener((v, event) -> {
                scaleDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);

                matrix.getValues(matrixValues);
                float currentScale = matrixValues[Matrix.MSCALE_X];

                // ✨ 확대 상태에 따라 ViewPager 스크롤 제어
                boolean isZoomed = currentScale > MIN_SCALE + 0.01f;
                viewPager.setUserInputEnabled(!isZoomed);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        last.set(event.getX(), event.getY());
                        start.set(last);
                        // ✨ 확대된 상태면 터치 이벤트 소비
                        if (isZoomed) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1 && isZoomed) {
                            float dx = event.getX() - last.x;
                            float dy = event.getY() - last.y;

                            matrix.postTranslate(dx, dy);
                            fixTranslation();
                            setImageMatrix(matrix);

                            last.set(event.getX(), event.getY());
                            // ✨ 이동 중에도 부모 터치 차단
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        // ✨ 터치 종료 시 부모 터치 허용
                        getParent().requestDisallowInterceptTouchEvent(false);
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                return true;
            });
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScale = scale * scaleFactor;

                newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));

                if (Math.abs(newScale - scale) > 0.01f) {
                    matrix.postScale(
                            newScale / scale,
                            newScale / scale,
                            detector.getFocusX(),
                            detector.getFocusY()
                    );
                    scale = newScale;
                    fixTranslation();
                    setImageMatrix(matrix);

                    // ✨ 스케일 변경 중 부모 터치 차단
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                return true;
            }
        }

        private class GestureListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float targetScale = (scale > MIN_SCALE + 0.1f) ? MIN_SCALE : DOUBLE_TAP_SCALE;

                matrix.postScale(
                        targetScale / scale,
                        targetScale / scale,
                        e.getX(),
                        e.getY()
                );
                scale = targetScale;
                fixTranslation();
                setImageMatrix(matrix);

                // ✨ 더블 탭 후 ViewPager 상태 갱신
                boolean isZoomed = scale > MIN_SCALE + 0.01f;
                viewPager.setUserInputEnabled(!isZoomed);

                return true;
            }
        }

        private void fixTranslation() {
            if (getDrawable() == null) return;

            matrix.getValues(matrixValues);
            float transX = matrixValues[Matrix.MTRANS_X];
            float transY = matrixValues[Matrix.MTRANS_Y];

            float fixTransX = getFixTranslation(transX, getWidth(), getDrawable().getIntrinsicWidth() * scale);
            float fixTransY = getFixTranslation(transY, getHeight(), getDrawable().getIntrinsicHeight() * scale);

            if (Math.abs(fixTransX) > 0.1f || Math.abs(fixTransY) > 0.1f) {
                matrix.postTranslate(fixTransX, fixTransY);
            }
        }

        private float getFixTranslation(float trans, float viewSize, float contentSize) {
            float minTrans;
            float maxTrans;

            if (contentSize <= viewSize) {
                minTrans = 0;
                maxTrans = viewSize - contentSize;
            } else {
                minTrans = viewSize - contentSize;
                maxTrans = 0;
            }

            if (trans < minTrans) {
                return minTrans - trans;
            }
            if (trans > maxTrans) {
                return maxTrans - trans;
            }
            return 0;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (getDrawable() != null) {
                fitImageToView();
            }
        }

        @Override
        public void setImageDrawable(@Nullable Drawable drawable) {
            super.setImageDrawable(drawable);
            post(() -> {
                if (drawable != null && getWidth() > 0 && getHeight() > 0) {
                    fitImageToView();
                }
            });
        }

        private void fitImageToView() {
            if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) {
                return;
            }

            matrix.reset();
            scale = MIN_SCALE;

            int drawableWidth = getDrawable().getIntrinsicWidth();
            int drawableHeight = getDrawable().getIntrinsicHeight();
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            if (drawableWidth == 0 || drawableHeight == 0) {
                return;
            }

            float scaleX = (float) viewWidth / drawableWidth;
            float scaleY = (float) viewHeight / drawableHeight;
            float fitScale = Math.min(scaleX, scaleY);

            matrix.setScale(fitScale, fitScale);

            float redundantX = viewWidth - (fitScale * drawableWidth);
            float redundantY = viewHeight - (fitScale * drawableHeight);
            matrix.postTranslate(redundantX / 2, redundantY / 2);

            scale = fitScale;
            setImageMatrix(matrix);

            // ✨ 초기화 시 ViewPager 활성화
            viewPager.setUserInputEnabled(true);
        }
    }
}