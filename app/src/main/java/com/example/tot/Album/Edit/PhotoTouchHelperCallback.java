package com.example.tot.Album.Edit;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;

public class PhotoTouchHelperCallback extends ItemTouchHelper.Callback {

    private final EditPhotoAdapter adapter;

    public PhotoTouchHelperCallback(EditPhotoAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false; // 드래그는 핸들로만
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true; // 스와이프 활성화
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.LEFT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(
            RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target) {

        adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c,
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState,
                            boolean isCurrentlyActive) {
        EditPhotoAdapter.ViewHolder holder = (EditPhotoAdapter.ViewHolder) viewHolder;
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            holder.btnEditComment.setAlpha(0f);
        }
        if (!isCurrentlyActive && dX == 0) {
            holder.btnEditComment.setAlpha(1f);
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            View itemView = viewHolder.itemView;


            c.save();
            if (dX < 0) {
                c.clipRect(itemView.getRight() + dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom());
                c.drawColor(Color.TRANSPARENT);
            }
            c.restore();

            // 2) 빨간 배경 그리기
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#FF3B30"));

            if (dX < 0) {
                c.drawRect(itemView.getRight() + dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom(),
                        paint);
            }

            // 3) 휴지통 아이콘 그리기
            Drawable icon = ContextCompat.getDrawable(
                    recyclerView.getContext(),
                    R.drawable.ic_trash
            );

            if (icon != null && dX < 0) {
                int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + iconMargin;
                int iconBottom = iconTop + icon.getIntrinsicHeight();

                int iconRight = itemView.getRight() - iconMargin;
                int iconLeft = iconRight - icon.getIntrinsicWidth();

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                icon.draw(c);
            }
        }

        // 4) 항상 이것을 마지막에!
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // 🔥 드래그 시작: elevation 증가 + 약간 확대 효과
            viewHolder.itemView.setElevation(20f);
            viewHolder.itemView.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setDuration(100)
                    .start();
        }
    }
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        // 🔥 드래그 종료: elevation 초기화 + 원래 크기 복귀
        viewHolder.itemView.setElevation(0f);
        viewHolder.itemView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(100)
                .start();
    }

}
