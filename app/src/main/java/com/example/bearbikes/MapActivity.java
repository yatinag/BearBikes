package com.example.bearbikes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bearbikes.utils.MyClusterManagerRenderer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback{

    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1212;

    //Map data
    private Boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final float DEFAULT_ZOOM = 15f;
    private ClusterManager mClusterManager;
    private MyClusterManagerRenderer myClusterManagerRenderer;
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();

    //Firebase data
    private DatabaseReference dataRetrieval;
    private DatabaseReference bikeRetrieval;

    //Berkeley map data
    private final double BERKELEY_SOUTH_LATITUDE = 37.842632;
    private final double BERKELEY_NORTH_LATITUDE = 37.909592;
    private final double BERKELEY_WEST_LONGITUDE = -122.318564;
    private final double BERKELEY_EAST_LONGITUDE = -122.248482;
    private final double BERKELEY_CENTER_LATITUDE = 37.871749;
    private final double BERKELEY_CENTER_LONGITUDE = -122.259501;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        FirebaseApp.initializeApp(this);
        dataRetrieval = FirebaseDatabase.getInstance().getReference();

        Log.d(TAG, "Map is ready.");
        getLocationPermission();

    }

    private void addMapMarkers(){

        if(mMap != null){
            if(mClusterManager == null){
                mClusterManager = new ClusterManager<ClusterMarker>(getApplicationContext(), mMap);
            }
            if(myClusterManagerRenderer == null){
                myClusterManagerRenderer = new MyClusterManagerRenderer(getApplicationContext(), mMap, mClusterManager);
                mClusterManager.setRenderer(myClusterManagerRenderer);
            }

            bikeRetrieval = dataRetrieval.child("bikes");

            bikeRetrieval.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        mClusterManager.clearItems();



                        for (DataSnapshot s : dataSnapshot.getChildren()) {
                            System.out.println(s.getValue());
                            BikeInformation bike = s.getValue(BikeInformation.class);
                            System.out.println("Latitude: " + bike.getLatitude() + " Longitude: " + bike.getLongitude());
                            Bitmap icon;

                            if(bike.getColor().equals("blue")){
                                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                        R.drawable.bluemarker);
                            } else{
                                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                        R.drawable.yellowmarker);
                            }
                            icon = Bitmap.createScaledBitmap(icon, 135, 190, true);

                            LatLng location = new LatLng(bike.getLatitude(), bike.getLongitude());

                            ClusterMarker newClusterMarker = new ClusterMarker(location, "Bike", "Origin: " + bike.getOrigin(), icon);
                            mClusterManager.addItem(newClusterMarker);
                            mClusterManager.cluster();
                        }
                    } catch (NullPointerException e){
                        Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });

        }
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting device's location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            if(!userInBerkeley(currentLocation)){
                                System.out.println("bruh");
                                moveCamera(new LatLng(BERKELEY_CENTER_LATITUDE, BERKELEY_CENTER_LONGITUDE), DEFAULT_ZOOM);
                                Toast.makeText(MapActivity.this, "Camera moved to center of campus!", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);
                            }

                        }
                        else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private Boolean userInBerkeley(Location currentLocation){
        double latitude = currentLocation.getLatitude();
        double longitude = currentLocation.getLongitude();
        System.out.println(latitude + " " + longitude);

        if(latitude < BERKELEY_SOUTH_LATITUDE || latitude > BERKELEY_NORTH_LATITUDE || longitude < BERKELEY_WEST_LONGITUDE || longitude > BERKELEY_EAST_LONGITUDE){
            return false;
        }
        return true;
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "onMapReady: map is ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;
        addMapMarkers();

        if(mLocationPermissionGranted){
            getDeviceLocation();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
                initMap();
            }
            else{
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
        else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: onRequestPermissionsResult() called");
        mLocationPermissionGranted = false;
        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Log.d(TAG, "onRequestPermissionsResult: permission denied");
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    initMap();
                }
            }
        }
    }

}
