package com.hw.anna.run_for_your_life;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;

public class Map extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, View.OnClickListener {

    private GoogleMap map;
    private MapView mapView;
    private TextView distanceView, speedView;
    private GoogleApiClient mGoogleApiClient;
    private float DISTANCE = 0;
    private double SPEED = 0;
    private double maxSPEED = 0;
    Button StartOrStop, Pause;
    Circle circle;
    private long startTime;
    private long stopTime;
    private long previousTime;

    private long pauseTime;
    private long stayOnPauseTime;

    Intent additAct;

    enum progressState {
        Start,
        Pause,
        Stop
    }

    private progressState state = progressState.Stop;

    private LatLng currentLocation;

    public LatLng getLatLngFromLocation(Location location){
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        distanceView = (TextView)findViewById(R.id.Distance);
        speedView = (TextView)findViewById(R.id.speedNow);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        StartOrStop = (Button) findViewById(R.id.startOrStop);
        Pause = (Button) findViewById(R.id.pause);

        StartOrStop.setOnClickListener(this);
        Pause.setOnClickListener(this);

        updateView();

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            GetAlertMessage.buildAlertMessageNoGps(this);
        }

        buildGoogleApiClient();

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onClick(View v) {
        try {
            int ID = v.getId();
            switch (ID) {
                case R.id.startOrStop:
                    if (state == progressState.Stop) {
                        state = progressState.Start;
                        StartOrStop.setText("Стоп");
                        stayOnPauseTime = 0;
                        startTime = SystemClock.elapsedRealtime();
                        previousTime = SystemClock.elapsedRealtime();
                        drawStartPoint(map.getCameraPosition().zoom);
                        updateView();
                    } else if (state == progressState.Start) {
                        state = progressState.Stop;
                        StartOrStop.setText("Пуск");
                        stopTime = SystemClock.elapsedRealtime();
                        stopSession();
                    }
                    break;
                case R.id.pause:
                    if (state == progressState.Start) {
                        state = progressState.Pause;
                        pauseTime = SystemClock.elapsedRealtime();
                        Pause.setText("Продолжить");
                    } else if (state == progressState.Pause) {
                        stayOnPauseTime += (SystemClock.elapsedRealtime() - pauseTime);
                        state = progressState.Start;
                        Pause.setText("Пауза");
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(LOG, "onClick if failed: " + e.getMessage());
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.w("Connected?", String.valueOf(mGoogleApiClient.isConnected()));
        mGoogleApiClient.disconnect();
        Log.w("Connected?", String.valueOf(mGoogleApiClient.isConnected()));
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.about:
                Intent aboutAct = new Intent(this, AboutActivity.class);
                startActivity(aboutAct);
                break;
            case R.id.additional_info:
                if (additAct != null) {
                    startActivity(additAct);
                } else {
                    Toast.makeText(this, "Вы еще не бегали", (Toast.LENGTH_SHORT)).show();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnCameraChangeListener(this);
        map.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {}

    @Override
    public void onConnectionSuspended(int smth){}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG, "Connected failed");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        currentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        drawStartPoint(16f);
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2500);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void drawStartPoint(float zoom) {
        map.clear();
        if (currentLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoom));
            circle = map.addCircle(new CircleOptions().center(currentLocation).radius(6)
                    .fillColor(Color.BLUE).strokeColor(Color.WHITE).strokeWidth(3));
        } else {
            Log.e(LOG, "DrawStartPoint: current location is null");
        }
    }

    public void drawLine(LatLng oldPoint){
        map.addPolyline(new PolylineOptions().geodesic(true).width(3f)
                .add(oldPoint)
                .add(currentLocation));
    }

    protected void createLocationRequest(LocationRequest mLocationRequest ) {}

    private void moveCameraAndCircle(LatLng currentLocation){
        circle.setCenter(currentLocation);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, map.getCameraPosition().zoom));
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng prevLocation = currentLocation;
        currentLocation = getLatLngFromLocation(location);

        DateFormat mLastUpdateTime = DateFormat.getTimeInstance();
        long nextTime = SystemClock.elapsedRealtime();
        double seconds = (nextTime - previousTime) / 1000.0;
        previousTime = nextTime;


        float distanceBetween = getDistance(prevLocation);

        if (state == progressState.Pause || state == progressState.Stop) {
            moveCameraAndCircle(currentLocation);
        } else if (state == progressState.Start) {
            DISTANCE += distanceBetween;
            SPEED = ((double) distanceBetween) / seconds;
            if (SPEED > maxSPEED) {
                maxSPEED = SPEED;
            }
            drawLine(prevLocation);
            moveCameraAndCircle(currentLocation);
        }

        if (state != progressState.Stop) {
            updateView();
        }
    }

    public void stopSession() {
        additAct = new Intent(this, AdditionalActivity.class);
        additAct.putExtra(AdditionalActivity.EXTRA_DISTANCE, (long)DISTANCE);
        additAct.putExtra(AdditionalActivity.EXTRA_MAXSPEED, (long)maxSPEED);
        double time = (stopTime - startTime - stayOnPauseTime) / 1000.0;
        additAct.putExtra(AdditionalActivity.EXTRA_TIME, (long)time);

        Toast.makeText(this, "Ваш забег закончен", (Toast.LENGTH_LONG)).show();
        speedView.setText("Ваша максимальная скорость " + (int)maxSPEED + " м/с");
        SPEED = 0;
        DISTANCE = 0;
    }

    public float getDistance(LatLng prevLocation) {
        Location loc1 = new Location("");
        loc1.setLatitude(prevLocation.latitude);
        loc1.setLongitude(prevLocation.longitude);

        Location loc2 = new Location("");
        loc2.setLatitude(currentLocation.latitude);
        loc2.setLongitude(currentLocation.longitude);

        float distanceInMeters = loc1.distanceTo(loc2);

        return distanceInMeters;
    }

    public void updateView() {
        distanceView.setText("Пройденное Вами расстояние: " + Integer.toString((int)DISTANCE) + " метров");
        speedView.setText("Ваша скорость " + (int)SPEED + " м/с");
    }

    public static final String LOG = "RUNMAP";
}