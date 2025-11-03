package com.example.tot; // 본인의 패키지

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class AlbumEntry {

    @DocumentId // Firestore 문서 ID를 이 필드에 자동으로 매핑
    private String documentId;

    private String imageUri; // Firebase Storage의 다운로드 URL
    private String comment;

    @ServerTimestamp // 서버 시간으로 타임스탬프 자동 기록 (정렬에 사용)
    private Date timestamp;

    // ❗️ Firestore가 데이터를 객체로 변환할 때 빈 생성자가 반드시 필요합니다!
    public AlbumEntry() {}

    public AlbumEntry(String imageUri) {
        this.imageUri = imageUri;
        this.comment = "";
        // timestamp는 Firestore 서버에서 자동으로 생성됩니다.
    }

    // --- 모든 필드에 대한 Getter와 Setter ---
    // (Firestore는 Getter/Setter를 사용해 데이터를 읽고 씁니다)

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}