package com.example.tot.ViewPager; // 본인의 패키지 이름으로 변경하세요

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.tot.R;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;

// MapFragment를 사용하려면 AppCompatActivity(또는 FragmentActivity)를 상속하고,
// 지도가 준비되었을 때 콜백을 받기 위해 OnMapReadyCallback을 구현(implements)해야 합니다.
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. XML 레이아웃 파일을 화면에 설정합니다.
        // R.layout.activity_map은 'activity_map.xml'을 의미합니다.
        // XML 파일 이름을 다르게 하셨다면, 그 이름으로 변경해야 합니다.
        setContentView(R.layout.map); // "activity_map" 부분은 XML 파일 이름과 일치해야 함

        // 2. 레이아웃 파일(XML)에 정의된 FragmentContainerView를 찾습니다.
        // supportFragmentManager를 사용하여 MapFragment를 가져옵니다.
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        if (mapFragment == null) {
            // MapFragment가 아직 존재하지 않으면 새로 생성합니다.
            mapFragment = MapFragment.newInstance();
            // R.id.map (FragmentContainerView의 ID) 위치에 MapFragment를 추가합니다.
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        // 3. 지도가 비동기적으로 준비되면 'onMapReady' 콜백 메서드를 호출하도록 설정합니다.
        // 이 시점에는 mapFragment가 null이 아님이 보장됩니다.
        mapFragment.getMapAsync(this);
    }

    /**
     * 4. 지도 준비가 완료되면 이 메서드가 호출됩니다.
     * @param naverMap 준비된 NaverMap 객체
     */
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // 여기에서 지도에 대한 초기 설정(예: 카메라 위치, 마커 추가 등)을 합니다.
        // 예: 서울 시청 근처로 카메라 이동
        // LatLng coord = new LatLng(37.5666102, 126.9783881);
        // CameraUpdate cameraUpdate = CameraUpdate.scrollTo(coord);
        // naverMap.moveCamera(cameraUpdate);

        // 예: 줌 레벨 설정
        // naverMap.setMinZoom(10.0);
    }
}
