package com.example.tot.MemoryRecyclerView;

public class MemoryData {
    private int img_profile;
    private String tv_region;
    private String tv_place;
    public MemoryData(int img_profile, String tv_region, String tv_place){
        this.img_profile = img_profile;
        this.tv_region = tv_region;
        this.tv_place = tv_place;
    }

    public int getImg_profile() {
        return img_profile;
    }

    public String getTv_region() {
        return tv_region;
    }

    public String getTv_place() {
        return tv_place;
    }

    public void setTv_place(String tv_place) {
        this.tv_place = tv_place;
    }

    public void setTv_region(String tv_region) {
        this.tv_region = tv_region;
    }

    public void setImg_profile(int img_profile) {
        this.img_profile = img_profile;
    }
}
