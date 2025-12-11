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
import androidx.viewpager2.adapter.FragmentStateAdapter;
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

        // 권한 요청 런처 등록
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

        FullscreenPhotoAdapter adapter = new FullscreenPhotoAdapter(photoUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        // 인디케이터 표시 여부 설정
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
            // Android 10 이상에서는 권한 불필요
            saveCurrentPhoto();
        } else {
            // Android 9 이하에서는 WRITE_EXTERNAL_STORAGE 권한 필요
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

        // Glide를 사용해 이미지 다운로드 후 저장
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
                // Android 10 이상
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
                // Android 9 이하
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

                // 갤러리에 스캔 요청
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
    // Part 2: 확대/축소 기능이 포함된 어댑터

    private static class FullscreenPhotoAdapter extends RecyclerView.Adapter<FullscreenPhotoAdapter.PhotoViewHolder> {

        private final List<String> photoUrls;

        public FullscreenPhotoAdapter(List<String> photoUrls) {
            this.photoUrls = photoUrls;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ZoomableImageView(parent.getContext());
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
     * 확대/축소 기능을 지원하는 ImageView
     * - 핀치 줌 (두 손가락으로 확대/축소)
     * - 더블 탭 (확대/축소 토글)
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

        public ZoomableImageView(Context context) {
            super(context);
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

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        last.set(event.getX(), event.getY());
                        start.set(last);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1 && currentScale > MIN_SCALE) {
                            float dx = event.getX() - last.x;
                            float dy = event.getY() - last.y;

                            matrix.postTranslate(dx, dy);
                            fixTranslation();
                            setImageMatrix(matrix);

                            last.set(event.getX(), event.getY());
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
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

                if (newScale != scale) {
                    matrix.postScale(
                            newScale / scale,
                            newScale / scale,
                            detector.getFocusX(),
                            detector.getFocusY()
                    );
                    scale = newScale;
                    fixTranslation();
                    setImageMatrix(matrix);
                }

                return true;
            }
        }

        private class GestureListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float targetScale = (scale > MIN_SCALE) ? MIN_SCALE : DOUBLE_TAP_SCALE;

                matrix.postScale(
                        targetScale / scale,
                        targetScale / scale,
                        e.getX(),
                        e.getY()
                );
                scale = targetScale;
                fixTranslation();
                setImageMatrix(matrix);

                return true;
            }
        }

        private void fixTranslation() {
            matrix.getValues(matrixValues);
            float transX = matrixValues[Matrix.MTRANS_X];
            float transY = matrixValues[Matrix.MTRANS_Y];

            float fixTransX = getFixTranslation(transX, getWidth(), getDrawable().getIntrinsicWidth() * scale);
            float fixTransY = getFixTranslation(transY, getHeight(), getDrawable().getIntrinsicHeight() * scale);

            if (fixTransX != 0 || fixTransY != 0) {
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
            if (drawable != null) {
                fitImageToView();
            }
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

            float scaleX = (float) viewWidth / drawableWidth;
            float scaleY = (float) viewHeight / drawableHeight;
            float fitScale = Math.min(scaleX, scaleY);

            matrix.setScale(fitScale, fitScale);

            float redundantX = viewWidth - (fitScale * drawableWidth);
            float redundantY = viewHeight - (fitScale * drawableHeight);
            matrix.postTranslate(redundantX / 2, redundantY / 2);

            scale = fitScale;
            setImageMatrix(matrix);
        }
    }
}