
package com.example.tot.Community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tot.R;

import java.util.ArrayList;
import java.util.List;

public class PhotoFullscreenFragment extends DialogFragment {

    private static final String ARG_PHOTO_URLS = "photo_urls";
    private static final String ARG_CURRENT_POSITION = "current_position";

    private ViewPager2 viewPager;
    private TextView indicator;
    private ImageButton closeButton;
    private ArrayList<String> photoUrls;
    private int currentPosition;

    public static PhotoFullscreenFragment newInstance(ArrayList<String> photoUrls, int currentPosition) {
        PhotoFullscreenFragment fragment = new PhotoFullscreenFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PHOTO_URLS, photoUrls);
        args.putInt(ARG_CURRENT_POSITION, currentPosition);
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
        }
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
        closeButton = view.findViewById(R.id.btn_close);

        FullscreenPhotoAdapter adapter = new FullscreenPhotoAdapter(photoUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        updateIndicator(currentPosition);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicator(position);
            }
        });

        closeButton.setOnClickListener(v -> dismiss());
    }

    private void updateIndicator(int position) {
        indicator.setText((position + 1) + " / " + photoUrls.size());
    }

    private static class FullscreenPhotoAdapter extends RecyclerView.Adapter<FullscreenPhotoAdapter.PhotoViewHolder> {

        private final List<String> photoUrls;

        public FullscreenPhotoAdapter(List<String> photoUrls) {
            this.photoUrls = photoUrls;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
}
