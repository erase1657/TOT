package com.example.tot.Home;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class HomeAlarmAdapter extends RecyclerView.Adapter<HomeAlarmAdapter.ViewHolder> {

    private List<HomeAlarmDTO> items;
    private Context context;

    public HomeAlarmAdapter(List<HomeAlarmDTO> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements SensorEventListener, LocationListener {
        TextView title, date, location, startTime, endTime, distanceText;
        ImageView locationIcon, gpsButton, directionIndicator, bearingIndicator;
        FrameLayout bearingContainer;
        View bearingModeLayout, currentLocationCircle;

        private SensorManager sensorManager;
        private Sensor accelerometer;
        private Sensor magnetometer;
        private LocationManager locationManager;

        private float[] gravity;
        private float[] geomagnetic;
        private float currentAzimuth = 0f;
        private float currentBearing = 0f;
        private Location currentLocation;
        private GeoPoint destinationLocation;

        private boolean isBearingMode = false;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            date = itemView.findViewById(R.id.text_date);
            location = itemView.findViewById(R.id.text_location);
            startTime = itemView.findViewById(R.id.text_start_time);
            endTime = itemView.findViewById(R.id.text_end_time);
            locationIcon = itemView.findViewById(R.id.image_location_point);

            // 베어링 관련 뷰
            bearingContainer = itemView.findViewById(R.id.bearing_container);
            gpsButton = itemView.findViewById(R.id.gps_button);
            bearingModeLayout = itemView.findViewById(R.id.bearing_mode_layout);
            currentLocationCircle = itemView.findViewById(R.id.current_location_circle);
            directionIndicator = itemView.findViewById(R.id.direction_indicator);
            bearingIndicator = itemView.findViewById(R.id.bearing_indicator);
            distanceText = itemView.findViewById(R.id.distance_text);

            sensorManager = (SensorManager) itemView.getContext().getSystemService(Context.SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            locationManager = (LocationManager) itemView.getContext().getSystemService(Context.LOCATION_SERVICE);
        }

        public void bind(HomeAlarmDTO item, Context context) {
            title.setText(item.getTitle());
            date.setText(item.getDate());
            location.setText(item.getPlace());
            startTime.setText(formatTime(item.getStartTime()));
            endTime.setText(formatTime(item.getEndTime()));

            // 장소가 있고 좌표가 있으면 GPS 버튼 표시
            if (item.getPlace() != null && !item.getPlace().isEmpty() && item.getPlaceLocation() != null) {
                locationIcon.setVisibility(View.VISIBLE);
                location.setVisibility(View.VISIBLE);
                bearingContainer.setVisibility(View.VISIBLE);
                destinationLocation = item.getPlaceLocation();

                // GPS 버튼 클릭 리스너
                gpsButton.setOnClickListener(v -> startBearingMode());
                bearingModeLayout.setOnClickListener(v -> stopBearingMode());

            } else {
                locationIcon.setVisibility(View.GONE);
                location.setVisibility(View.GONE);
                bearingContainer.setVisibility(View.GONE);
            }
        }

        private void startBearingMode() {
            if (isBearingMode) return;

            // 위치 권한 체크
            if (ActivityCompat.checkSelfPermission(itemView.getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(itemView.getContext(), "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            isBearingMode = true;
            gpsButton.setVisibility(View.GONE);
            bearingModeLayout.setVisibility(View.VISIBLE);
            distanceText.setVisibility(View.VISIBLE);

            // 센서 등록
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

            // 위치 업데이트 시작
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000, // 1초마다
                        1,    // 1미터마다
                        this
                );

                // 초기 위치 가져오기
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation != null) {
                    onLocationChanged(lastLocation);
                }
            } catch (SecurityException e) {
                Toast.makeText(itemView.getContext(), "위치 서비스에 접근할 수 없습니다", Toast.LENGTH_SHORT).show();
                stopBearingMode();
            }
        }

        private void stopBearingMode() {
            if (!isBearingMode) return;

            isBearingMode = false;
            gpsButton.setVisibility(View.VISIBLE);
            bearingModeLayout.setVisibility(View.GONE);
            distanceText.setVisibility(View.GONE);

            // 센서 해제
            sensorManager.unregisterListener(this);

            // 위치 업데이트 중지
            locationManager.removeUpdates(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!isBearingMode) return;

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravity = event.values.clone();
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic = event.values.clone();
            }

            if (gravity != null && geomagnetic != null) {
                float[] R = new float[9];
                float[] I = new float[9];

                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    float azimuth = (float) Math.toDegrees(orientation[0]);
                    azimuth = (azimuth + 360) % 360;

                    updateDirectionIndicator(azimuth);
                }
            }
        }
        private void updateDirectionIndicator(float azimuth) {
            RotateAnimation rotate = new RotateAnimation(
                    currentAzimuth,
                    -azimuth,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(200);
            rotate.setFillAfter(true);
            directionIndicator.startAnimation(rotate);
            currentAzimuth = -azimuth;
        }

        private void updateBearingIndicator(float bearing) {
            RotateAnimation rotate = new RotateAnimation(
                    currentBearing,
                    -bearing,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(200);
            rotate.setFillAfter(true);
            bearingIndicator.startAnimation(rotate);
            currentBearing = -bearing;
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (!isBearingMode || destinationLocation == null) return;

            currentLocation = location;

            // 목적지까지의 방향 계산
            float bearing = currentLocation.bearingTo(createLocation(destinationLocation));
            bearing = (bearing + 360) % 360;
            updateBearingIndicator(bearing);

            // 거리 계산 및 표시
            float distance = currentLocation.distanceTo(createLocation(destinationLocation));
            updateDistanceText(distance);
        }

        private Location createLocation(GeoPoint geoPoint) {
            Location location = new Location("");
            location.setLatitude(geoPoint.getLatitude());
            location.setLongitude(geoPoint.getLongitude());
            return location;
        }

        private void updateDistanceText(float distanceInMeters) {
            String distanceStr;
            if (distanceInMeters < 1000) {
                distanceStr = String.format("%.0f m", distanceInMeters);
            } else {
                distanceStr = String.format("%.1f km", distanceInMeters / 1000);
            }
            distanceText.setText(distanceStr);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Not used
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            // Not used
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            if (isBearingMode) {
                Toast.makeText(itemView.getContext(), "GPS가 비활성화되었습니다", Toast.LENGTH_SHORT).show();
                stopBearingMode();
            }
        }

        private String formatTime(Timestamp ts) {
            if (ts == null) return "";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
            return sdf.format(ts.toDate());
        }

        public void cleanup() {
            if (isBearingMode) {
                stopBearingMode();
            }
        }
    }

    @NonNull
    @Override
    public HomeAlarmAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_home_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeAlarmDTO item = items.get(position);
        holder.bind(item, context);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cleanup();
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}