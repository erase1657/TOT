package com.example.tot.Album;

public class AlbumDTO {

    private String photoId;
    private String imageUrl;
    private String comment;
    private int index;
    private String dateKey;

    public AlbumDTO() {}

    public AlbumDTO(String photoId, String imageUrl, String comment, int index, String dateKey) {
        this.photoId = photoId;
        this.imageUrl = imageUrl;
        this.comment = comment;
        this.index = index;
        this.dateKey = dateKey;
    }

    public String getPhotoId() {
        return photoId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getComment() {
        return comment;
    }

    public int getIndex() {
        return index;
    }

    public String getDateKey() {
        return dateKey;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
