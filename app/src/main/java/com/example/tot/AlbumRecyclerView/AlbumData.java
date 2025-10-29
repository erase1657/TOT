package com.example.tot.AlbumRecyclerView;

public class AlbumData {
    private String UserName;
    private int UserProfile;
    private int AlbumProfile;
    private String ProvinceCode;  // 시/도 코드 (서버 연동용)
    private String CityCode;      // 시군구 코드 (서버 연동용)

    // 기존 생성자 (하위 호환성 유지)
    public AlbumData(String UserName, int UserProfile, int AlbumProfile){
        this.UserName = UserName;
        this.UserProfile = UserProfile;
        this.AlbumProfile = AlbumProfile;
        this.ProvinceCode = "";
        this.CityCode = "";
    }

    // 지역 정보 포함 생성자 (서버 연동 시 사용)
    public AlbumData(String UserName, int UserProfile, int AlbumProfile, String ProvinceCode, String CityCode){
        this.UserName = UserName;
        this.UserProfile = UserProfile;
        this.AlbumProfile = AlbumProfile;
        this.ProvinceCode = ProvinceCode;
        this.CityCode = CityCode;
    }

    // Setter 메서드
    public void setUserName(String userName) {
        UserName = userName;
    }

    public void setUserProfile(int userProfile) {
        UserProfile = userProfile;
    }

    public void setAlbumProfile(int albumProfile) {
        AlbumProfile = albumProfile;
    }

    public void setProvinceCode(String provinceCode) {
        ProvinceCode = provinceCode;
    }

    public void setCityCode(String cityCode) {
        CityCode = cityCode;
    }

    // Getter 메서드
    public String getUserName() {
        return UserName;
    }

    public int getUserProfile() {
        return UserProfile;
    }

    public int getAlbumProfile() {
        return AlbumProfile;
    }

    public String getProvinceCode() {
        return ProvinceCode;
    }

    public String getCityCode() {
        return CityCode;
    }
}