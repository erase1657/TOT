package com.example.tot.Album.Edit;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.example.tot.R;

public class EditCommentDialog extends Dialog {

    private String initialComment;
    private OnSaveListener listener;

    private EditText etComment;
    private Button btnSave;

    public interface OnSaveListener {
        void onSave(String newComment);
    }

    public EditCommentDialog(
            @NonNull Context context,
            String initialComment,
            OnSaveListener listener
    ) {
        super(context);
        this.initialComment = initialComment;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 타이틀 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 투명 배경 (라운드 적용 위해)
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.dialog_edit_comment);

        etComment = findViewById(R.id.et_comment);
        btnSave = findViewById(R.id.btn_save_comment);

        // 이전 코멘트 불러오기
        etComment.setText(initialComment);
        etComment.setSelection(etComment.getText().length());

        // EditText 스크롤 가능하게 설정
        etComment.setMovementMethod(new ScrollingMovementMethod());
        etComment.setVerticalScrollBarEnabled(true);

        btnSave.setOnClickListener(v -> {
            String newText = etComment.getText().toString();
            if (listener != null) listener.onSave(newText);
            dismiss();
        });
    }
}
