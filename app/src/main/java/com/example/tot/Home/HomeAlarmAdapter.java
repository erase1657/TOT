package com.example.tot.Home;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.Timestamp;

import java.util.List;

/**
 * HomeAlarmAdapter - GPS 베어링 기능 추가
 * ✅ 기존 기능 완전 유지
 * ✅ GPS 좌표가 있는 일정만 GPS 버튼 표시
 */
public class HomeAlarmAdapter extends RecyclerView.Adapter<HomeAlarmAdapter.ViewHolder> {

    // ✅ 기존 변수 (절대 수정 안함)
    private List<HomeAlarmDTO> items;
    private Context context;

    // ✅ GPS 관련 변수 추가
    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private LocationCallback locationCallback;

    // ✅ 현재 GPS 활성화 상태 추적
    private int activeGpsPosition = -1;
    private ViewHolder activeHolder = null;

    // ✅ 기존 생성자 (절대 수정 안함)
    public HomeAlarmAdapter(List<HomeAlarmDTO> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // ✅ 기존 뷰 (절대 수정 안함)
        TextView title, date, location, startTime, endTime;
        ImageView locationIcon;

        // ✅ GPS 베어링 기능을 위한 새 뷰 추가
        ImageButton gpsButton;
        LinearLayout bearingInfoLayout;
        ImageView currentDirectionArrow;
        ImageView destDirectionArrow;
        TextView distanceText;

        // ✅ 센서 리스너 (각 ViewHolder마다 고유)
        SensorEventListener sensorListener;

        // ✅ 현재 위치 저장
        double currentLatitude = 0.0;
        double currentLongitude = 0.0;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // ✅ 기존 뷰 바인딩 (절대 수정 안함)
            title = itemView.findViewById(R.id.text_title);
            date = itemView.findViewById(R.id.text_date);
            location = itemView.findViewById(R.id.text_location);
            startTime = itemView.findViewById(R.id.text_start_time);
            endTime = itemView.findViewById(R.id.text_end_time);
            locationIcon = itemView.findViewById(R.id.image_location_point);

            // ✅ GPS 베어링 뷰 바인딩 추가
            gpsButton = itemView.findViewById(R.id.btn_gps);
            bearingInfoLayout = itemView.findViewById(R.id.layout_bearing_info);
            currentDirectionArrow = itemView.findViewById(R.id.iv_current_direction);
            destDirectionArrow = itemView.findViewById(R.id.iv_dest_direction);
            distanceText = itemView.findViewById(R.id.tv_distance);
        }
    }

    @NonNull
    @Override
    public HomeAlarmAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

        // ✅ GPS 및 센서 초기화 추가
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        }
        if (sensorManager == null) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        // ✅ 기존 코드 (절대 수정 안함)
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_home_alarm, parent, false);
        return new ViewHolder(view);
    }
    // ✅ onBindViewHolder - 기존 코드 유지 + GPS 기능 추가
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeAlarmDTO item = items.get(position);

        // ✅ 기존 데이터 바인딩 (절대 수정 안함)
        holder.title.setText(item.getTitle());
        holder.date.setText(item.getDate());
        holder.location.setText(item.getPlace());
        holder.startTime.setText(formatTime(item.getStartTime()));
        holder.endTime.setText(formatTime(item.getEndTime()));

        // ✅ 기존 로직 (절대 수정 안함)
        if (item.getPlace() == null || item.getPlace().isEmpty()) {
            holder.locationIcon.setVisibility(View.GONE);
            holder.location.setVisibility(View.GONE);
        } else {
            holder.locationIcon.setVisibility(View.VISIBLE);
            holder.location.setVisibility(View.VISIBLE);
            holder.location.setText(item.getPlace());
        }

        // ✅ GPS 버튼 초기 상태 설정 추가
        if (holder.gpsButton != null) {
            if (activeGpsPosition == position) {
                holder.gpsButton.setImageResource(R.drawable.ic_bearing_active);
                if (holder.bearingInfoLayout != null) {
                    holder.bearingInfoLayout.setVisibility(View.VISIBLE);
                }
            } else {
                holder.gpsButton.setImageResource(R.drawable.ic_location_point);
                if (holder.bearingInfoLayout != null) {
                    holder.bearingInfoLayout.setVisibility(View.GONE);
                }
            }

            // ✅ GPS 좌표가 없으면 버튼 숨김
            if (!item.hasValidCoordinates()) {
                holder.gpsButton.setVisibility(View.GONE);
            } else {
                holder.gpsButton.setVisibility(View.VISIBLE);
            }

            // ✅ GPS 버튼 클릭 리스너 추가
            holder.gpsButton.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) return;

                if (activeGpsPosition == currentPosition) {
                    deactivateGps();
                } else {
                    if (activeGpsPosition != -1) {
                        deactivateGps();
                    }
                    activateGps(holder, currentPosition, item);
                }
            });
        }
    }

    /**
     * ✅ GPS 및 센서 활성화 메서드 추가
     */
    private void activateGps(ViewHolder holder, int position, HomeAlarmDTO item) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        activeGpsPosition = position;
        activeHolder = holder;

        holder.gpsButton.setImageResource(R.drawable.ic_bearing_active);
        if (holder.bearingInfoLayout != null) {
            holder.bearingInfoLayout.setVisibility(View.VISIBLE);
        }

        // ✅ 센서 리스너 생성 및 등록
        holder.sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    updateCurrentDirection(holder, event);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        if (rotationSensor != null) {
            sensorManager.registerListener(
                    holder.sensorListener,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
        }

        // ✅ 위치 업데이트 요청 생성
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        // ✅ 위치 콜백 생성
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && holder == activeHolder) {
                    holder.currentLatitude = location.getLatitude();
                    holder.currentLongitude = location.getLongitude();
                    calculateBearingAndDistance(holder, item);
                }
            }
        };

        // ✅ 위치 업데이트 시작
        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

            // ✅ 초기 위치 가져오기
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && holder == activeHolder) {
                            holder.currentLatitude = location.getLatitude();
                            holder.currentLongitude = location.getLongitude();
                            calculateBearingAndDistance(holder, item);
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(context, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ GPS 및 센서 비활성화 메서드 추가
     */
    private void deactivateGps() {
        if (activeHolder != null) {
            activeHolder.gpsButton.setImageResource(R.drawable.ic_location_point);
            if (activeHolder.bearingInfoLayout != null) {
                activeHolder.bearingInfoLayout.setVisibility(View.GONE);
            }

            if (activeHolder.sensorListener != null) {
                sensorManager.unregisterListener(activeHolder.sensorListener);
                activeHolder.sensorListener = null;
            }
        }

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }

        activeGpsPosition = -1;
        activeHolder = null;

        notifyDataSetChanged();
    }
    /**
     * ✅ 센서로부터 현재 방향(Azimuth) 계산 및 화살표 회전 메서드 추가
     */
    private void updateCurrentDirection(ViewHolder holder, SensorEvent event) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);

        float azimuthRad = orientation[0];
        float azimuthDeg = (float) Math.toDegrees(azimuthRad);

        if (azimuthDeg < 0) {
            azimuthDeg += 360;
        }

        if (holder.currentDirectionArrow != null) {
            holder.currentDirectionArrow.setRotation(azimuthDeg);
        }
    }

    /**
     * ✅ 목적지까지의 베어링(방향) 및 거리 계산 메서드 추가
     */
    private void calculateBearingAndDistance(ViewHolder holder, HomeAlarmDTO item) {
        if (holder.currentLatitude == 0.0 || holder.currentLongitude == 0.0) {
            return;
        }

        if (!item.hasValidCoordinates()) {
            return;
        }

        Location currentLoc = new Location("");
        currentLoc.setLatitude(holder.currentLatitude);
        currentLoc.setLongitude(holder.currentLongitude);

        Location destLoc = new Location("");
        destLoc.setLatitude(item.getDestLatitude());
        destLoc.setLongitude(item.getDestLongitude());

        float bearing = currentLoc.bearingTo(destLoc);

        if (bearing < 0) {
            bearing += 360;
        }

        if (holder.destDirectionArrow != null) {
            holder.destDirectionArrow.setRotation(bearing);
        }

        float distanceInMeters = currentLoc.distanceTo(destLoc);
        float distanceInKm = distanceInMeters / 1000;

        if (holder.distanceText != null) {
            if (distanceInKm < 1) {
                holder.distanceText.setText(String.format("📍 남은 거리: %.0f m", distanceInMeters));
            } else {
                holder.distanceText.setText(String.format("📍 남은 거리: %.2f km", distanceInKm));
            }
        }
    }

    // ✅ 기존 formatTime 메서드 (절대 수정 안함)
    private String formatTime(Timestamp ts) {
        if (ts == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(ts.toDate());
    }

    // ✅ 기존 getItemCount 메서드 (절대 수정 안함)
    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * ✅ 어댑터가 destroy될 때 리소스 정리 메서드 추가
     * Fragment의 onDestroyView()에서 호출해야 함
     */
    public void onDestroy() {
        deactivateGps();

        // ✅ 에러 수정: SensorEventListener 명시
        if (sensorManager != null && activeHolder != null && activeHolder.sensorListener != null) {
            sensorManager.unregisterListener(activeHolder.sensorListener);
        }
    }

    /**
     * ✅ 데이터 업데이트 메서드 추가 (기존 기능 유지)
     */
    public void updateData(List<HomeAlarmDTO> newItems) {
        if (activeGpsPosition != -1) {
            deactivateGps();
        }

        this.items = newItems;
        notifyDataSetChanged();
    }
}