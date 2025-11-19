package com.example.tot.Map;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.example.tot.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Review;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    public static final String EXTRA_PLACE_ADDRESS = "com.example.tot.Map.PLACE_ADDRESS";
    public static final String EXTRA_PLACE_LAT_LNG = "com.example.tot.Map.PLACE_LAT_LNG";
    public static final String EXTRA_PLACE_LAT_LNG_LIST = "com.example.tot.Map.PLACE_LAT_LNG_LIST";
    public static final String EXTRA_PLACE_DAY_LIST = "com.example.tot.Map.PLACE_DAY_LIST";

    private static final LatLngBounds KOREA_BOUNDS = new LatLngBounds(
            new LatLng(33.1, 124.6), // 남서쪽
            new LatLng(38.6, 131.9)  // 북동쪽
    );
    private static final float MIN_ZOOM = 7.0f;
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.5665, 126.9780); // 서울

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlacesClient placesClient;

    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
    private ImageView placePhotoImageView;
    private TextView placeNameTextView;
    private TextView placeAddressTextView;
    private RatingBar placeRatingBar;
    private TextView placeRatingTextView;
    private TextView placeReviewsTitleTextView;
    private TextView placeReviewTextView;
    private Button btnSelectPlace;

    private LatLng selectedPlaceLatLng;
    private Marker temporaryMarker; // 임시 마커를 저장할 변수

    private ActivityResultLauncher<Intent> autocompleteLauncher;
    private ChipGroup dayChipGroup;
    private Map<Integer, List<LatLng>> dailyLocations = new LinkedHashMap<>();
    private List<Marker> currentMarkers = new ArrayList<>();
    private List<Polyline> currentPolylines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        try {
            String apiKey = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData.getString("com.google.android.geo.API_KEY");
            if (apiKey != null && !apiKey.equals("YOUR_GOOGLE_MAPS_API_KEY")) {
                if (!Places.isInitialized()) {
                    Places.initialize(getApplicationContext(), apiKey);
                }
                placesClient = Places.createClient(this);
            } else {
                Log.e(TAG, "API Key is not set in AndroidManifest.xml");
                Toast.makeText(this, "API Key가 설정되지 않았습니다. 앱을 종료합니다.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
            Toast.makeText(this, "앱 설정 오류가 발생했습니다. 앱을 종료합니다.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        initViews();
        initBottomSheet();
        setupAutocompleteLauncher();
    }

    private void initViews() {
        ImageButton searchButton = findViewById(R.id.search_button);
        ImageButton myLocationButton = findViewById(R.id.my_location_button);
        ImageButton zoomInButton = findViewById(R.id.zoom_in_button);
        ImageButton zoomOutButton = findViewById(R.id.zoom_out_button);
        dayChipGroup = findViewById(R.id.day_chip_group);
        HorizontalScrollView chipScrollView = findViewById(R.id.chip_scroll_view);
        chipScrollView.setVisibility(View.GONE); // 기본적으로 숨김

        searchButton.setOnClickListener(v -> launchAutocomplete());
        myLocationButton.setOnClickListener(v -> moveToUserLocation(true));

        zoomInButton.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        zoomOutButton.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });
    }

    private void initBottomSheet() {
        NestedScrollView bottomSheetLayout = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        placePhotoImageView = bottomSheetLayout.findViewById(R.id.place_photo);
        placeNameTextView = bottomSheetLayout.findViewById(R.id.place_name);
        placeAddressTextView = bottomSheetLayout.findViewById(R.id.place_address);
        placeRatingBar = bottomSheetLayout.findViewById(R.id.place_rating);
        placeRatingTextView = bottomSheetLayout.findViewById(R.id.place_rating_text);
        btnSelectPlace = bottomSheetLayout.findViewById(R.id.btn_select_place);
        placeReviewsTitleTextView = bottomSheetLayout.findViewById(R.id.place_reviews_title);
        placeReviewTextView = bottomSheetLayout.findViewById(R.id.place_review);

        ViewCompat.setOnApplyWindowInsetsListener(bottomSheetLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        btnSelectPlace.setOnClickListener(v -> {
            CharSequence placeName = placeNameTextView.getText();
            if (placeName != null && !placeName.toString().isEmpty() && selectedPlaceLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_PLACE_ADDRESS, placeName.toString());
                resultIntent.putExtra(EXTRA_PLACE_LAT_LNG, selectedPlaceLatLng);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void launchAutocomplete() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("KR")
                .build(this);
        autocompleteLauncher.launch(intent);
    }

    private void setupAutocompleteLauncher() {
        autocompleteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        LatLng latLng = place.getLatLng();
                        if (latLng != null) {
                            if (temporaryMarker != null) {
                                temporaryMarker.remove();
                            }
                            temporaryMarker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(place.getName())
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                            fetchPlaceDetails(place.getId());
                        }
                    } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                        Status status = Autocomplete.getStatusFromIntent(result.getData());
                        if (status != null && status.getStatusMessage() != null) {
                            Log.e(TAG, status.getStatusMessage());
                        }
                    }
                }
        );
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setLatLngBoundsForCameraTarget(KOREA_BOUNDS);
        mMap.setMinZoomPreference(MIN_ZOOM);
        mMap.setOnPoiClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        Intent intent = getIntent();
        ArrayList<LatLng> locations = intent.getParcelableArrayListExtra(EXTRA_PLACE_LAT_LNG_LIST);
        ArrayList<Integer> days = intent.getIntegerArrayListExtra(EXTRA_PLACE_DAY_LIST);

        if (locations != null && !locations.isEmpty()) {
            ArrayList<Integer> finalDays = days;
            // 데이터 안전장치: days 정보가 없거나 locations와 크기가 맞지 않으면 모두 1일차로 간주
            if (finalDays == null || finalDays.size() != locations.size()) {
                finalDays = new ArrayList<>(Collections.nCopies(locations.size(), 1));
            }
            setupScheduleView(locations, finalDays);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                moveToUserLocation(false);
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, MIN_ZOOM));
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnMapClickListener(latLng -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            if (temporaryMarker != null) {
                temporaryMarker.remove();
                temporaryMarker = null;
            }
        });
    }

    private void setupScheduleView(ArrayList<LatLng> locations, ArrayList<Integer> days) {
        dailyLocations.clear();
        for (int i = 0; i < locations.size(); i++) {
            int day = days.get(i);
            LatLng location = locations.get(i);
            dailyLocations.computeIfAbsent(day, k -> new ArrayList<>()).add(location);
        }
        setupDayChipsAndListeners();
    }

    private void setupDayChipsAndListeners() {
        if (dailyLocations.isEmpty()) return;

        HorizontalScrollView chipScrollView = findViewById(R.id.chip_scroll_view);
        chipScrollView.setVisibility(View.VISIBLE);

        dayChipGroup.removeAllViews();
        dayChipGroup.setSingleSelection(true);

        // '전체' 칩을 항상 추가
        Chip allChip = new Chip(this);
        allChip.setText("전체");
        allChip.setTag(-1); // 전체를 의미하는 태그
        allChip.setCheckable(true);
        allChip.setId(View.generateViewId());
        dayChipGroup.addView(allChip);

        // 일차별 칩 추가
        for (int day : dailyLocations.keySet()) {
            Chip dayChip = new Chip(this);
            dayChip.setText(day + "일차");
            dayChip.setTag(day);
            dayChip.setCheckable(true);
            dayChip.setId(View.generateViewId());
            dayChipGroup.addView(dayChip);
        }

        dayChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) return;
            Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null) {
                int day = (int) checkedChip.getTag();
                displayFilteredRoutes(day);
            }
        });

        // '전체' 칩을 기본으로 선택
        allChip.setChecked(true);
    }

    private void displayFilteredRoutes(int day) {
        clearMapObjects();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        if (day == -1) { // '전체' 선택
            List<LatLng> allLocations = new ArrayList<>();
            // dailyLocations는 LinkedHashMap이므로 일차 순서가 보장됨
            for (List<LatLng> locations : dailyLocations.values()) {
                allLocations.addAll(locations);
            }
            // 모든 장소를 하나의 리스트로 합쳐서 한 번에 경로를 그림
            drawPathWithNumberedMarkers(allLocations, boundsBuilder, 0);
        } else { // 특정 일차 선택
            List<LatLng> locations = dailyLocations.get(day);
            if (locations != null) {
                // 해당 일차의 경로만 그림 (마커 번호는 1부터 시작)
                drawPathWithNumberedMarkers(locations, boundsBuilder, 0);
            }
        }

        try {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 200;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (IllegalStateException e) {
            if (!currentMarkers.isEmpty()) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMarkers.get(0).getPosition(), 15f));
            }
        }
    }

    private void drawPathWithNumberedMarkers(List<LatLng> locations, LatLngBounds.Builder boundsBuilder, int numberOffset) {
        if (locations == null || locations.isEmpty()) return;

        for (int i = 0; i < locations.size(); i++) {
            LatLng location = locations.get(i);
            BitmapDescriptor numberedMarker = createNumberedMarkerBitmap(i + 1 + numberOffset);
            Marker marker = mMap.addMarker(new MarkerOptions().position(location).icon(numberedMarker));
            currentMarkers.add(marker);
            boundsBuilder.include(location);

            if (i > 0) {
                LatLng previousLocation = locations.get(i - 1);
                Polyline polyline = mMap.addPolyline(new PolylineOptions()
                        .add(previousLocation, location)
                        .color(Color.BLUE)
                        .width(10));
                currentPolylines.add(polyline);
            }
        }
    }

    private void clearMapObjects() {
        for (Marker marker : currentMarkers) {
            marker.remove();
        }
        currentMarkers.clear();

        for (Polyline polyline : currentPolylines) {
            polyline.remove();
        }
        currentPolylines.clear();
    }

    @Override
    public void onPoiClick(@NonNull PointOfInterest poi) {
        if (temporaryMarker != null) {
            temporaryMarker.remove();
        }
        temporaryMarker = mMap.addMarker(new MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); // 색상 변경으로 구분

        mMap.animateCamera(CameraUpdateFactory.newLatLng(poi.latLng));
        fetchPlaceDetails(poi.placeId);
    }

    private BitmapDescriptor createNumberedMarkerBitmap(int number) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30); // 텍스트 크기 축소
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        int diameter = 65; // 마커 지름 축소
        Bitmap bgBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bgBitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#575DFB")); // 메인 색상
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, circlePaint);

        float yPos = (canvas.getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(String.valueOf(number), canvas.getWidth() / 2f, yPos, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bgBitmap);
    }

    private void fetchPlaceDetails(String placeId) {
        Log.d(TAG, "Fetching details for placeId: " + placeId);
        if (placeId == null) {
            Log.e(TAG, "fetchPlaceDetails called with null placeId.");
            return;
        }

        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.RATING, Place.Field.PHOTO_METADATAS, Place.Field.REVIEWS);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        if (placesClient == null) {
            Log.e(TAG, "placesClient is not initialized!");
            Toast.makeText(this, "PlacesClient가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Log.d(TAG, "Successfully fetched place details.");
            Place place = response.getPlace();

            placeNameTextView.setText(place.getName() != null ? place.getName() : "이름 정보 없음");
            placeAddressTextView.setText(place.getAddress() != null ? place.getAddress() : "주소 정보 없음");
            selectedPlaceLatLng = place.getLatLng();

            if (place.getRating() != null) {
                placeRatingBar.setVisibility(View.VISIBLE);
                placeRatingTextView.setVisibility(View.VISIBLE);
                placeRatingBar.setRating(place.getRating().floatValue());
                placeRatingTextView.setText(String.valueOf(place.getRating()));
            } else {
                placeRatingBar.setVisibility(View.GONE);
                placeRatingTextView.setVisibility(View.GONE);
            }

            final List<PhotoMetadata> metadata = place.getPhotoMetadatas();
            if (metadata == null || metadata.isEmpty()) {
                placePhotoImageView.setVisibility(View.GONE);
            } else {
                final PhotoMetadata photoMetadata = metadata.get(0);
                final FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata).setMaxWidth(500).setMaxHeight(300).build();
                placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                    Bitmap bitmap = fetchPhotoResponse.getBitmap();
                    placePhotoImageView.setImageBitmap(bitmap);
                    placePhotoImageView.setVisibility(View.VISIBLE);
                }).addOnFailureListener((exception) -> {
                    Log.e(TAG, "Photo not found: " + exception.getMessage());
                    placePhotoImageView.setVisibility(View.GONE);
                });
            }

            final List<Review> reviews = place.getReviews();
            if (reviews == null || reviews.isEmpty()){
                placeReviewsTitleTextView.setVisibility(View.GONE);
                placeReviewTextView.setVisibility(View.GONE);
            } else {
                Review firstReview = reviews.get(0);
                if (firstReview != null && firstReview.getText() != null && !firstReview.getText().isEmpty()) {
                    placeReviewsTitleTextView.setVisibility(View.VISIBLE);
                    placeReviewTextView.setVisibility(View.VISIBLE);
                    placeReviewTextView.setText(firstReview.getText());
                } else {
                     placeReviewsTitleTextView.setVisibility(View.GONE);
                     placeReviewTextView.setVisibility(View.GONE);
                }
            }

            if (bottomSheetBehavior != null) {
                Log.d(TAG, "Setting bottom sheet state to EXPANDED");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                Log.e(TAG, "bottomSheetBehavior is null!");
            }

        }).addOnFailureListener( (exception) -> {
            Log.e(TAG, "Failed to fetch place details.", exception);
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Toast.makeText(MapActivity.this, "장소 정보 로딩 실패: " + apiException.getStatusCode(), Toast.LENGTH_SHORT).show();
            } else {
                 Toast.makeText(MapActivity.this, "장소 정보 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveToUserLocation(boolean animate) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, MIN_ZOOM));
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (animate) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15.0f));
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15.0f));
                }
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, MIN_ZOOM));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!getIntent().hasExtra(EXTRA_PLACE_LAT_LNG_LIST)) {
                    moveToUserLocation(false);
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
