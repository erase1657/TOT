package com.example.tot.Album.Edit;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
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
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder) {

        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.LEFT;

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {

        adapter.onItemMove(
                viewHolder.getBindingAdapterPosition(),
                target.getBindingAdapterPosition()
        );

        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getBindingAdapterPosition();
        adapter.onItemDismiss(pos);
    }
    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        adapter.onDragFinished();  // ← 드래그 종료 시 index 저장 요청
    }
    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return 0.3f;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 0.1f;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue * 0.1f;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;

        float limitedDX = Math.max(dX, -itemView.getWidth() * 0.35f);

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && limitedDX < 0) {

            // 🔥 1) 배경은 그리되
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#FF3B30"));

            c.drawRect(
                    itemView.getRight() + limitedDX,
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom(),
                    paint
            );

            // 🔥 2) 스와이프가 충분하지 않으면 아이콘 숨기기
            float iconThreshold = -itemView.getWidth() * 0.15f;  // 15% 이상 밀어야 아이콘 보임
            if (limitedDX > iconThreshold) {
                // 아직 충분히 안 밀렸으면 아이콘 그리지 않고 종료
                super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive);
                return;
            }

            // 🔥 3) 충분히 밀렸을 때만 아이콘 표시
            Drawable icon = ContextCompat.getDrawable(
                    recyclerView.getContext(),
                    R.drawable.ic_trash
            );

            if (icon != null) {
                int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;

                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                int iconTop = itemView.getTop() + iconMargin;
                int iconBottom = iconTop + icon.getIntrinsicHeight();

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                icon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive);
    }

}
