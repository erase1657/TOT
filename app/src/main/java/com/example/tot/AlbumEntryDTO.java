// 경로: app/src/main/java/com/example/tot/AlbumEntryDTO.java (새 파일)

package com.example.tot;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class AlbumEntryDTO {

    @DocumentId
    private String documentId;
    private String imageUri; // Storage 다운로드 URL
    private String comment;

    @ServerTimestamp
    private Date timestamp; // 전체 정렬용 (날짜별 정렬은 photoDate 사용)

    // ✅ "이 사진이 속한 날짜" (예: "2025-12-15")
    private String photoDate;

    // Firestore 필수: 빈 생성자
    public AlbumEntryDTO() {}

    public AlbumEntryDTO(String imageUri, String photoDate) {
        this.imageUri = imageUri;
        this.photoDate = photoDate;
        this.comment = "";
    }

    // --- 모든 필드에 대한 Getter와 Setter ---
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    // ✅ photoDate의 Getter/Setter
    public String getPhotoDate() { return photoDate; }
    public void setPhotoDate(String photoDate) { this.photoDate = photoDate; }
}