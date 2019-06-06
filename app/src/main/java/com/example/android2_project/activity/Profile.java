package com.example.android2_project.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.bumptech.glide.load.model.UriLoader;
import com.example.android2_project.R;
import com.example.android2_project.model.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.SnapshotMetadata;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Profile extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final int MAP_PERMISSION = 001;


    private GoogleApiClient googleApiClient;
    private TextView fullName, tvemail, firstName, lastName;
    private FirebaseFirestore mFirestore;
    DocumentReference docRef;
    private FirebaseAuth auth;

    private FirebaseUser currentUser;
    String firstNameFS;
    String lastNameFS;
    ImageView profileImage;
    String email;
    String photoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        //another stuff
        profileImage = (ImageView)findViewById(R.id.ivUserImage);
        fullName = findViewById(R.id.tvFullName);
        tvemail = findViewById(R.id.tvEmail);
        firstName = findViewById(R.id.resultFirstName);
        lastName = findViewById(R.id.resultLastName);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        this.mFirestore = FirebaseFirestore.getInstance();
        email = currentUser.getEmail();

        //gets reference to user's document
        docRef = mFirestore.collection("users").document(email);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("got users email", "onSuccess: ");
                getData();
                getPhotoUrl();
                Picasso.get()
                        .load("https://firebasestorage.googleapis.com/v0/b/android2-project-6d7c0.appspot.com/o/uploads%2F1559418382220.jpg?alt=media&token=6fb1c81f-e87d-49c0-b5d1-2f14481f4d85")
                        .into(profileImage);
            }
        });

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
                    data.put("longitude",location.getLongitude());
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
                            firstName.setText(firstNameFS);
                            lastName.setText(lastNameFS);
                            String result = firstNameFS + " " + lastNameFS;
                            fullName.setText(result);

                        } else {
                            Toast.makeText(Profile.this, "Not found", Toast.LENGTH_SHORT).show();
                            Log.d("error", "onError: ");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Profile.this, "Coundn't find", Toast.LENGTH_SHORT).show();
                        Log.d("BIGERROR", "onError: ");
                    }
                });

    }

    public void startEditProfile(View view) {
        Intent intent = new Intent(Profile.this, EditProfile.class);
        startActivity(intent);
    }

    public void onBackEvent(View view) {
        Intent intent = new Intent(Profile.this, Main2Activity.class);
        startActivity(intent);
    }


    private void getPhotoUrl() {
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("onStart", "Listen failed.", e);
                    return;
                }
                if (documentSnapshot.exists())
                {
                    photoUrl = documentSnapshot.get("photoUrl").toString();
                    Log.d("substracted", "onEvent: Here we go" + " photoUrl " + photoUrl);

                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
        System.out.println("entering onStart");
        getPhotoUrl();

    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
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
}