package com.example.tot.Community;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 커뮤니티 프래그먼트 (리팩토링 버전)
 * 경로: java/com/example/tot/Community/CommunityFragment.java
 *
 * 역할: UI 컨트롤러 (View 초기화 및 이벤트 처리)
 */
public class CommunityFragment extends Fragment {

    // UI 컴포넌트
    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private EditText edtSearch;
    private MaterialButton btnPopular, btnAll, btnFriends;
    private ImageButton btnWrite;

    // ViewModel
    private CommunityViewModel viewModel;

    // 데이터
    private List<CommunityPostDTO> displayedPosts;

    public CommunityFragment() {
        super(R.layout.fragment_community);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModel();
        initViews(view);
        setupRecyclerView();
        setupFilterButtons();
        setupSearch();
        setupWriteButton();

        // 데이터 로드 및 초기 필터 적용
        viewModel.loadDummyData();
        viewModel.applyFilter();

        // 초기 버튼 상태 설정
        updateFilterButtonStates();
    }

    /**
     * ViewModel 초기화
     */
    private void initViewModel() {
        viewModel = new CommunityViewModel(new CommunityViewModel.DataCallback() {
            @Override
            public void onDataChanged(List<CommunityPostDTO> posts) {
                // 데이터 전체 교체
                if (adapter != null) {
                    adapter.updateData(posts);
                }
            }

            @Override
            public void onDataAdded(List<CommunityPostDTO> posts) {
                // 데이터 추가 (무한 스크롤)
                if (adapter != null) {
                    adapter.addData(posts);
                }
            }
        });
    }

    /**
     * View 초기화
     */
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_community);
        edtSearch = view.findViewById(R.id.edt_search);
        btnPopular = view.findViewById(R.id.btn_popular);
        btnAll = view.findViewById(R.id.btn_all);
        btnFriends = view.findViewById(R.id.btn_friends);
        btnWrite = view.findViewById(R.id.btn_write);
    }

    /**
     * RecyclerView 설정
     */
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        displayedPosts = new ArrayList<>();
        adapter = new CommunityAdapter(displayedPosts, new CommunityAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(CommunityPostDTO post, int position) {
                if (post != null) {
                    Toast.makeText(getContext(),
                            post.getTitle() + " 상세보기",
                            Toast.LENGTH_SHORT).show();
                    // TODO: 게시글 상세 화면으로 이동
                }
            }
        });

        recyclerView.setAdapter(adapter);

        // 무한 스크롤 리스너
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (viewModel == null) return;

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager != null && !viewModel.isLoading() && !viewModel.isLastPage()) {
                    int visibleItemCount = manager.getChildCount();
                    int totalItemCount = manager.getItemCount();
                    int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();

                    // 하단에 도달했을 때 추가 로드
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= viewModel.getPageSize()) {
                        viewModel.loadMorePosts();
                    }
                }
            }
        });
    }

    /**
     * 필터 버튼 설정
     */
    private void setupFilterButtons() {
        btnPopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setFilter(CommunityViewModel.FilterMode.POPULAR);
                updateFilterButtonStates();
            }
        });

        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setFilter(CommunityViewModel.FilterMode.ALL);
                updateFilterButtonStates();
            }
        });

        btnFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setFilter(CommunityViewModel.FilterMode.FRIENDS);
                updateFilterButtonStates();
            }
        });
    }

    /**
     * 필터 버튼 상태 업데이트 (MaterialButton 사용)
     */
    private void updateFilterButtonStates() {
        if (viewModel == null) return;

        CommunityViewModel.FilterMode currentFilter = viewModel.getCurrentFilter();

        // 색상 정의
        int selectedBgColor = 0xFF575DFB; // 보라색
        int unselectedBgColor = 0xFFF0F0F5; // 연한 회색
        int selectedTextColor = 0xFFFFFFFF; // 흰색
        int unselectedTextColor = 0xFF000000; // 검은색

        // 인기순 버튼
        if (currentFilter == CommunityViewModel.FilterMode.POPULAR) {
            btnPopular.setBackgroundTintList(ColorStateList.valueOf(selectedBgColor));
            btnPopular.setTextColor(selectedTextColor);
        } else {
            btnPopular.setBackgroundTintList(ColorStateList.valueOf(unselectedBgColor));
            btnPopular.setTextColor(unselectedTextColor);
        }

        // 전체보기 버튼
        if (currentFilter == CommunityViewModel.FilterMode.ALL) {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(selectedBgColor));
            btnAll.setTextColor(selectedTextColor);
        } else {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(unselectedBgColor));
            btnAll.setTextColor(unselectedTextColor);
        }

        // 친구 버튼
        if (currentFilter == CommunityViewModel.FilterMode.FRIENDS) {
            btnFriends.setBackgroundTintList(ColorStateList.valueOf(selectedBgColor));
            btnFriends.setTextColor(selectedTextColor);
        } else {
            btnFriends.setBackgroundTintList(ColorStateList.valueOf(unselectedBgColor));
            btnFriends.setTextColor(unselectedTextColor);
        }
    }

    /**
     * 검색 기능 설정
     */
    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (viewModel != null) {
                    viewModel.setSearchQuery(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 글쓰기 버튼 설정
     */
    private void setupWriteButton() {
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "글쓰기 기능 (준비중)", Toast.LENGTH_SHORT).show();
                // TODO: 글쓰기 화면으로 이동
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) {
            viewModel.destroy();
        }
    }
}