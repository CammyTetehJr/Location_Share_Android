package com.example.android2_project.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android2_project.R;
import com.example.android2_project.model.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main2Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // google maps var
    private static final int MAP_PERMISSION = 001;
    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;


    //Reference to User's Document in Database
    DocumentReference docRef;

    // Authentication variables
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // Cloud Firestore instance
    FirebaseFirestore db;

    //firestore users variable
    CollectionReference users;

    //custom object user
    User user;

    //get user's name
    String userName;

    //list of other Latlng
    List<LatLng> allOtherLocations = new ArrayList<LatLng>();

    //list of other users name
    List<String> otherUsersName = new ArrayList<>();

    //reference of user's location
    Location myLocation = null;
    //reference of user's latnl
    LatLng userLatLng = null;

    private Marker meMarker;
    private Marker otherMarker;

    // for getting data in navigation drawer
    private TextView fullName, tvemail, firstName, lastName;
    String email;
    String firstNameFS;
    String lastNameFS;
    String photoUrl;
    ImageView image;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        if(FirebaseAuth.getInstance() != null)
        {
            auth = FirebaseAuth.getInstance();
        }

        currentUser = auth.getCurrentUser();

        //initialize db
        this.db = FirebaseFirestore.getInstance();
        email = currentUser.getEmail();
        Log.d("user email", "onCreate: " + currentUser.getEmail());

        //gets reference to user's document
        docRef = db.collection("users").document(email);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("got users email", "onSuccess: ");
                getData();
                Picasso.get().load(photoUrl).into(image);
            }
        });

        //nav header view
        View navheaderView = navigationView.getHeaderView(0);
        fullName = navheaderView.findViewById(R.id.drawerName);
        tvemail = navheaderView.findViewById(R.id.drawerEmail);
        image = navheaderView.findViewById(R.id.ivProfile);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    //update db with new location
                    Map<String, Object> data = new HashMap<>();
                    data.put("latitude", location.getLatitude());
                    data.put("longitude", location.getLongitude());
                    docRef.set(data, SetOptions.merge());
                }
            }
        };
    }

    public void getData() {
        docRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            firstNameFS = documentSnapshot.getString("first");
                            lastNameFS = documentSnapshot.getString("last");
                            tvemail.setText(email);
                            String result = firstNameFS + " " + lastNameFS;
                            fullName.setText(result);
                        } else {
                            Toast.makeText(Main2Activity.this, "Not found", Toast.LENGTH_SHORT).show();
                            Log.d("error", "onError: ");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Main2Activity.this, "Coundn't find", Toast.LENGTH_SHORT).show();
                        Log.d("BIGERROR", "onError: ");
                    }
                });

    }

    private void initializeCollectionOfUsers() {
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("success getting users", document.getId() + " => " + document.getData());
                            }
                        } else {
                            Log.w("error getting users", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private boolean alreadyDisplayedNotification;
    private void listenToOtherUsers() {
        initializeCollectionOfUsers();
        db.collection("users").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                allOtherLocations.clear();
                otherUsersName.clear();
                if (e != null) {
                    Log.w("listen to other users", "Listen failed.", e);
                    return;
                }
                if (!queryDocumentSnapshots.isEmpty()) {
                    List<DocumentSnapshot> documentSnapshots = queryDocumentSnapshots.getDocuments();
                    Log.d("", "onEvent: document snapshot" + documentSnapshots.size() + documentSnapshots);
                    Log.d("at doc snapshop", "onEvent: username is" + userName + " photoUrl " + photoUrl);
                    for (DocumentSnapshot snapshot : documentSnapshots) {
                        if (snapshot.getDouble("latitude") != null && snapshot.getDouble("longitude") != null) {
                            double Latitude = snapshot.getDouble("latitude");
                            Log.d("", "onEvent: document snapshot" + Latitude);
                            double Longitude = snapshot.getDouble("longitude");
                            Log.d("", "onEvent: document snapshot" + Longitude);

                            String name = snapshot.getString("first");
                            if (name.equals(userName)) {
                                name = "My Location";
                            }

                            LatLng latLng = new LatLng(Latitude, Longitude);
                            DecimalFormat f = new DecimalFormat("##.00");
                            double distance = Double.parseDouble(f.format(getDistanceBetweenTwoPoints(userLatLng.latitude, userLatLng.longitude, latLng.latitude, latLng.longitude) / 1000));
                            //alert if close
                            if (!(name == "My Location") && alreadyDisplayedNotification == false) {
                                alertProximity(distance, name);
                                alreadyDisplayedNotification = true;
                            }
                            allOtherLocations.add(latLng);
                            otherUsersName.add(name + " " + distance + " km away");
                            drawOtherUsersPosition(allOtherLocations);
                        }
                    }
                }
            }
        });
    }

    private void alertProximity(double distance, String name) {
        if (distance * 1000 < 100) {
            Toast.makeText(getApplicationContext(), name + " is close", Toast.LENGTH_LONG).show();
            Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(300);
        }
    }

    private float getDistanceBetweenTwoPoints(double lat1, double lon1, double lat2, double lon2) {
        float[] distance = new float[2];
        Location.distanceBetween(lat1, lon1,
                lat2, lon2, distance);
        return distance[0];
    }


    private void listenToMe() {
        //listener for my position
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("onStart", "Listen failed.", e);
                    return;
                }
                if (documentSnapshot.exists()) {
                    Double Latitude = Double.valueOf(documentSnapshot.getDouble("latitude"));
                    Double Longitude = Double.valueOf(documentSnapshot.getDouble("longitude"));
                    userName = documentSnapshot.getString("first");
                    if (photoUrl != null) {
                        photoUrl = documentSnapshot.get("photoUrl").toString();
                    }
                    Log.d("my username is " + userName + photoUrl, "onEvent: ");

                    LatLng latLng = new LatLng(Latitude, Longitude);
                    userLatLng = latLng;
                    myLocation = new Location("myLocation");
                    myLocation.setLatitude(Latitude);
                    myLocation.setLongitude(Longitude);
                    //draw new position
                    drawMyPosition(latLng, userName);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
        System.out.println("entering onStart");
        //listener for my position
        listenToMe();
        //listener for other users
        listenToOtherUsers();
    }

    private void drawOtherUsersPosition(List<LatLng> allOtherLocations) {
        map.clear();
        int count = 0;
       // alreadyDisplayedNotification = false;
        for (LatLng latLng : allOtherLocations) {
            if (latLng != null) {
                // Logic to handle location object
                LatLng userLocation = latLng;
                // Add a marker in User Location and move the camera
                String name = otherUsersName.get(count);
                if (name != userName) {
                    otherMarker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(userLocation.latitude, userLocation.longitude))
                            .title(name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    otherMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    count++;
                    Log.d("drawn other users", "drawOtherUsersPosition: " + name);
                    System.out.println("location: " + latLng + " other users count " + allOtherLocations.size());

                }
            }
        }
    }

    private void drawMyPosition(LatLng location, String userName) {
        map.clear();
        if (location != null) {
            // Logic to handle location object
            LatLng userLocation = location;
            // Add a marker in User Location and move the camera

            meMarker = map.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title(userName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            meMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.latitude, location.longitude), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.latitude, location.longitude))      // Sets the center of the map to location user
                    .zoom(10)                   // Sets the zoom
                    .bearing(0)                 // Sets the orientation of the camera to east
                    .tilt(60)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            System.out.println("location: I just drew myself at " + location);
        }
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.isBuildingsEnabled();
        map.getUiSettings().setIndoorLevelPickerEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        map.getUiSettings().setIndoorLevelPickerEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, this.getMainLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MAP_PERMISSION);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(final Location location) {
        Log.d("location", " location changed");
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            new Thread(new Runnable() {
                @Override
                public void run() {
//                    latitudeRef.setValue(location.getLatitude());
//                    longitudeRef.setValue(location.getLongitude());
                    Map<String, Object> data = new HashMap<>();
                    data.put("latitude", location.getLatitude());
                    data.put("longitude", location.getLongitude());
                    docRef.set(data, SetOptions.merge());
                }
            }).start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MAP_PERMISSION);
        }
    }

    // menu code
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent;
        if (id == R.id.nav_profile) {
            intent = new Intent(Main2Activity.this, Profile.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (id == R.id.nav_logOut) {
            auth.signOut();
            intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

