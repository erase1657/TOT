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
            return new PhotoViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            ZoomableImageView imageView = (ZoomableImageView) holder.itemView;
            // ✅ Glide 로드 후 Matrix 초기화
            Glide.with(holder.itemView.getContext())
                    .load(photoUrls.get(position))
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            imageView.setImageDrawable(resource);
                            imageView.post(() -> imageView.resetMatrixPublic());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
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
    /* Part 1에서 계속... */

    private static class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {

        private static final String TAG = "ZoomableImageView";
        private static final float MIN_ZOOM = 1f;
        private static final float MAX_ZOOM = 5f;
        private static final float DOUBLE_TAP_ZOOM = 3f;

        private Matrix matrix = new Matrix();
        private float currentScale = 1f;
        private PointF lastTouch = new PointF();
        private PointF startTouch = new PointF();

        private ScaleGestureDetector scaleDetector;
        private GestureDetector gestureDetector;
        private ViewPager2 viewPager;

        private float[] matrixValues = new float[9];

        public ZoomableImageView(Context context, ViewPager2 viewPager) {
            super(context);
            this.viewPager = viewPager;
            init();
        }

        private void init() {
            setScaleType(ScaleType.MATRIX);
            setImageMatrix(matrix);

            scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
            gestureDetector = new GestureDetector(getContext(), new GestureListener());

            setOnTouchListener((v, event) -> {
                scaleDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);

                int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouch.set(event.getX(), event.getY());
                        startTouch.set(event.getX(), event.getY());

                        if (currentScale > MIN_ZOOM + 0.01f) {
                            viewPager.setUserInputEnabled(false);
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1 && currentScale > MIN_ZOOM + 0.01f) {
                            float dx = event.getX() - lastTouch.x;
                            float dy = event.getY() - lastTouch.y;

                            matrix.postTranslate(dx, dy);
                            checkBounds();
                            setImageMatrix(matrix);

                            lastTouch.set(event.getX(), event.getY());
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (currentScale <= MIN_ZOOM + 0.01f) {
                            viewPager.setUserInputEnabled(true);
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        break;
                }

                return true;
            });
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                viewPager.setUserInputEnabled(false);
                getParent().requestDisallowInterceptTouchEvent(true);
                Log.d(TAG, "🔍 줌 시작 - Scale: " + currentScale);
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScale = currentScale * scaleFactor;

                newScale = Math.max(MIN_ZOOM, Math.min(newScale, MAX_ZOOM));

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                if (Math.abs(newScale - currentScale) > 0.001f) {
                    matrix.postScale(
                            newScale / currentScale,
                            newScale / currentScale,
                            focusX,
                            focusY
                    );

                    currentScale = newScale;
                    checkBounds();
                    setImageMatrix(matrix);

                    Log.d(TAG, "🔍 줌 중 - Scale: " + currentScale);
                }

                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                Log.d(TAG, "🔍 줌 종료 - Final Scale: " + currentScale);

                if (currentScale <= MIN_ZOOM + 0.01f) {
                    viewPager.setUserInputEnabled(true);
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
        }

        private class GestureListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float targetScale;

                if (currentScale > MIN_ZOOM + 0.1f) {
                    targetScale = MIN_ZOOM;
                } else {
                    targetScale = DOUBLE_TAP_ZOOM;
                }

                float scaleChange = targetScale / currentScale;

                matrix.postScale(scaleChange, scaleChange, e.getX(), e.getY());
                currentScale = targetScale;

                checkBounds();
                setImageMatrix(matrix);

                viewPager.setUserInputEnabled(currentScale <= MIN_ZOOM + 0.01f);

                Log.d(TAG, "👆 더블탭 줌 - Scale: " + currentScale);
                return true;
            }
        }

        private void checkBounds() {
            if (getDrawable() == null) return;

            matrix.getValues(matrixValues);

            float transX = matrixValues[Matrix.MTRANS_X];
            float transY = matrixValues[Matrix.MTRANS_Y];
            float scaleX = matrixValues[Matrix.MSCALE_X];
            float scaleY = matrixValues[Matrix.MSCALE_Y];

            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int imgWidth = getDrawable().getIntrinsicWidth();
            int imgHeight = getDrawable().getIntrinsicHeight();

            float scaledWidth = imgWidth * scaleX;
            float scaledHeight = imgHeight * scaleY;

            float deltaX = 0;
            float deltaY = 0;

            if (scaledWidth > viewWidth) {
                if (transX > 0) {
                    deltaX = -transX;
                } else if (transX + scaledWidth < viewWidth) {
                    deltaX = viewWidth - (transX + scaledWidth);
                }
            } else {
                deltaX = (viewWidth - scaledWidth) / 2 - transX;
            }

            if (scaledHeight > viewHeight) {
                if (transY > 0) {
                    deltaY = -transY;
                } else if (transY + scaledHeight < viewHeight) {
                    deltaY = viewHeight - (transY + scaledHeight);
                }
            } else {
                deltaY = (viewHeight - scaledHeight) / 2 - transY;
            }

            if (Math.abs(deltaX) > 0.1f || Math.abs(deltaY) > 0.1f) {
                matrix.postTranslate(deltaX, deltaY);
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w > 0 && h > 0 && getDrawable() != null) {
                resetMatrix();
            }
        }

        private void resetMatrix() {
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

            matrix.reset();

            float scaleX = (float) viewWidth / imgWidth;
            float scaleY = (float) viewHeight / imgHeight;
            float scale = Math.min(scaleX, scaleY);

            matrix.postScale(scale, scale);

            float scaledWidth = imgWidth * scale;
            float scaledHeight = imgHeight * scale;
            float dx = (viewWidth - scaledWidth) / 2;
            float dy = (viewHeight - scaledHeight) / 2;
            matrix.postTranslate(dx, dy);

            currentScale = MIN_ZOOM;
            setImageMatrix(matrix);
            viewPager.setUserInputEnabled(true);

            Log.d(TAG, "🔄 Matrix 초기화 완료 - 이미지 크기: " + imgWidth + "x" + imgHeight +
                    ", 뷰 크기: " + viewWidth + "x" + viewHeight + ", 스케일: " + scale);
        }

        // ✅ public 메서드로 외부 호출 가능
        public void resetMatrixPublic() {
            resetMatrix();
        }
    }
}