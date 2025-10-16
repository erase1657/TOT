package com.example.tot.AlbumRecyclerView;

public class AlbumData {
    private String UserName;
    private int UserProfile;
    private int AlbumProfile;

    public AlbumData(String UserName, int UserProfile, int AlbumProfile){
        this.UserName = UserName;
        this.UserProfile = UserProfile;
        this.AlbumProfile = AlbumProfile;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public void setUserProfile(int userProfile) {
        UserProfile = userProfile;
    }

    public void setAlbumProfile(int albumProfile) {
        AlbumProfile = albumProfile;
    }

    public String getUserName() {
        return UserName;
    }

    public int getUserProfile() {
        return UserProfile;
    }

    public int getAlbumProfile() {
        return AlbumProfile;
    }
}
