package com.example.tot.Album;

import com.google.firebase.firestore.DocumentReference;

public class AlbumDTO {
    private String imageURI;
    private String comment;
    private int index;
    AlbumDTO() {};
    public AlbumDTO(String imageURI, String comment, int index) {
        this.imageURI = imageURI;
        this.comment = comment;
        this.index = index;
    }

    public String getImageURI() {
        return imageURI;
    }

    public void setImageURI(String imageURI) {
        this.imageURI = imageURI;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
