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
     * ✨✨✨ 완전 개선된 확대/축소 ImageView
     * - 이미지 크기와 상관없이 항상 확대/축소 가능
     * - 매번 matrix를 새로 계산하는 방식으로 안정적
     */
    private static class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {

        private static final float MIN_ZOOM = 1f;    // 최소 줌 (기본 크기)
        private static final float MAX_ZOOM = 5f;    // 최대 줌 (5배 확대)
        private static final float DOUBLE_TAP_ZOOM = 3f; // 더블탭 줌

        private Matrix matrix = new Matrix();

        // ✅ 줌 레벨 (1.0 = 기본, 2.0 = 2배 확대)
        private float currentZoom = 1f;

        // ✅ 이미지를 화면에 맞추기 위한 기본 스케일
        private float fitScale = 1f;

        // ✅ 이미지의 기본 위치 (중앙 정렬)
        private float baseDx = 0f;
        private float baseDy = 0f;

        // ✅ 사용자가 드래그한 오프셋
        private float userDx = 0f;
        private float userDy = 0f;

        private PointF lastTouch = new PointF();
        private PointF zoomCenter = new PointF();

        private ScaleGestureDetector scaleDetector;
        private GestureDetector gestureDetector;
        private ViewPager2 viewPager;

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

                boolean isZoomed = currentZoom > MIN_ZOOM + 0.01f;

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouch.set(event.getX(), event.getY());

                        if (isZoomed) {
                            viewPager.setUserInputEnabled(false);
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1 && isZoomed) {
                            float dx = event.getX() - lastTouch.x;
                            float dy = event.getY() - lastTouch.y;

                            userDx += dx;
                            userDy += dy;

                            updateMatrix();
                            lastTouch.set(event.getX(), event.getY());
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isZoomed) {
                            viewPager.setUserInputEnabled(true);
                        }
                        getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                return true;
            });
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                zoomCenter.set(detector.getFocusX(), detector.getFocusY());
                viewPager.setUserInputEnabled(false);
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newZoom = currentZoom * scaleFactor;

                // ✅ 줌 레벨 제한
                newZoom = Math.max(MIN_ZOOM, Math.min(newZoom, MAX_ZOOM));

                if (Math.abs(newZoom - currentZoom) > 0.01f) {
                    // ✅ 줌 중심점 계산 (현재 matrix 기준)
                    float focusX = detector.getFocusX();
                    float focusY = detector.getFocusY();

                    // 줌 전 이미지 상의 좌표
                    float oldScale = fitScale * currentZoom;
                    float imageX = (focusX - baseDx - userDx) / oldScale;
                    float imageY = (focusY - baseDy - userDy) / oldScale;

                    currentZoom = newZoom;

                    // 줌 후 같은 이미지 좌표가 같은 화면 위치에 오도록 조정
                    float newScale = fitScale * currentZoom;
                    userDx = focusX - baseDx - (imageX * newScale);
                    userDy = focusY - baseDy - (imageY * newScale);

                    updateMatrix();
                }

                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                boolean isZoomed = currentZoom > MIN_ZOOM + 0.01f;
                if (!isZoomed) {
                    viewPager.setUserInputEnabled(true);
                }
            }
        }

        private class GestureListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float targetZoom = (currentZoom > MIN_ZOOM + 0.1f) ? MIN_ZOOM : DOUBLE_TAP_ZOOM;

                if (targetZoom > MIN_ZOOM) {
                    // ✅ 더블탭 위치를 중심으로 확대
                    float focusX = e.getX();
                    float focusY = e.getY();

                    float oldScale = fitScale * currentZoom;
                    float imageX = (focusX - baseDx - userDx) / oldScale;
                    float imageY = (focusY - baseDy - userDy) / oldScale;

                    currentZoom = targetZoom;

                    float newScale = fitScale * currentZoom;
                    userDx = focusX - baseDx - (imageX * newScale);
                    userDy = focusY - baseDy - (imageY * newScale);
                } else {
                    // ✅ 축소할 때는 초기화
                    currentZoom = MIN_ZOOM;
                    userDx = 0;
                    userDy = 0;
                }

                updateMatrix();

                boolean isZoomed = currentZoom > MIN_ZOOM + 0.01f;
                viewPager.setUserInputEnabled(!isZoomed);

                return true;
            }
        }

        /**
         * ✅ Matrix를 매번 새로 계산
         * fitScale * currentZoom이 실제 스케일
         */
        private void updateMatrix() {
            if (getDrawable() == null) return;

            matrix.reset();

            // ✅ 1단계: 기본 스케일 적용 (이미지를 화면에 맞춤)
            matrix.postScale(fitScale, fitScale);

            // ✅ 2단계: 중앙 정렬
            matrix.postTranslate(baseDx, baseDy);

            // ✅ 3단계: 사용자 줌 적용 (현재 중심점 기준)
            if (currentZoom != 1f) {
                float pivotX = getWidth() / 2f;
                float pivotY = getHeight() / 2f;
                matrix.postScale(currentZoom, currentZoom, pivotX, pivotY);
            }

            // ✅ 4단계: 사용자 드래그 적용
            matrix.postTranslate(userDx, userDy);

            // ✅ 5단계: 경계 제한
            limitTranslation();

            setImageMatrix(matrix);
        }

        /**
         * ✅ 이미지가 화면 밖으로 나가지 않도록 제한
         */
        private void limitTranslation() {
            if (getDrawable() == null) return;

            int imgWidth = getDrawable().getIntrinsicWidth();
            int imgHeight = getDrawable().getIntrinsicHeight();

            float actualScale = fitScale * currentZoom;
            float scaledWidth = imgWidth * actualScale;
            float scaledHeight = imgHeight * actualScale;

            float viewWidth = getWidth();
            float viewHeight = getHeight();

            // X축 제한
            if (scaledWidth > viewWidth) {
                // 이미지가 화면보다 크면 양쪽 경계 체크
                float maxDx = 0;
                float minDx = viewWidth - scaledWidth;

                float currentX = baseDx + userDx;
                if (currentX > maxDx) {
                    userDx = maxDx - baseDx;
                } else if (currentX < minDx) {
                    userDx = minDx - baseDx;
                }
            } else {
                // 이미지가 화면보다 작으면 중앙 유지
                userDx = 0;
            }

            // Y축 제한
            if (scaledHeight > viewHeight) {
                float maxDy = 0;
                float minDy = viewHeight - scaledHeight;

                float currentY = baseDy + userDy;
                if (currentY > maxDy) {
                    userDy = maxDy - baseDy;
                } else if (currentY < minDy) {
                    userDy = minDy - baseDy;
                }
            } else {
                userDy = 0;
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (getDrawable() != null && w > 0 && h > 0) {
                calculateFitScale();
                updateMatrix();
            }
        }

        @Override
        public void setImageDrawable(@Nullable Drawable drawable) {
            super.setImageDrawable(drawable);
            if (drawable != null) {
                post(() -> {
                    if (getWidth() > 0 && getHeight() > 0) {
                        calculateFitScale();
                        updateMatrix();
                    }
                });
            }
        }

        /**
         * ✅ 이미지를 화면에 맞추기 위한 기본 스케일 계산
         */
        private void calculateFitScale() {
            Drawable drawable = getDrawable();
            if (drawable == null || getWidth() == 0 || getHeight() == 0) {
                return;
            }

            int imgWidth = drawable.getIntrinsicWidth();
            int imgHeight = drawable.getIntrinsicHeight();

            if (imgWidth == 0 || imgHeight == 0) {
                return;
            }

            int viewWidth = getWidth();
            int viewHeight = getHeight();

            // ✅ 이미지를 화면에 맞추는 스케일 계산
            float scaleX = (float) viewWidth / imgWidth;
            float scaleY = (float) viewHeight / imgHeight;
            fitScale = Math.min(scaleX, scaleY);

            // ✅ 중앙 정렬을 위한 오프셋 계산
            float scaledWidth = imgWidth * fitScale;
            float scaledHeight = imgHeight * fitScale;
            baseDx = (viewWidth - scaledWidth) / 2f;
            baseDy = (viewHeight - scaledHeight) / 2f;

            // ✅ 초기화
            currentZoom = MIN_ZOOM;
            userDx = 0;
            userDy = 0;

            viewPager.setUserInputEnabled(true);
        }
    }
}