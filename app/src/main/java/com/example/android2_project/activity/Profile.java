package com.example.android2_project.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.session.MediaSession;
import android.net.Uri;
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

import com.bumptech.glide.load.model.UriLoader;
import com.example.android2_project.R;
import com.example.android2_project.model.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
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
import com.google.firebase.firestore.SnapshotMetadata;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Profile extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;
    private TextView fullName,tvemail,firstName,lastName;
    private FirebaseFirestore mFirestore;
    DocumentReference docRef;
    private FirebaseAuth auth;
    private StorageReference storageReference;
    private FirebaseStorage mDatabase;

    private FirebaseUser currentUser;
    String firstNameFS;
    String lastNameFS;
    ImageView profileImage;
    String email;
    String getPhoto;
    String photoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        mDatabase = FirebaseStorage.getInstance();
        storageReference = mDatabase.getReferenceFromUrl("gs://android2-project-6d7c0.appspot.com/");
        //another stuff
        profileImage = findViewById(R.id.imgProfile);
        fullName = findViewById(R.id.tvFullName);
        tvemail = findViewById(R.id.tvEmail);
        firstName = findViewById(R.id.resultFirstName);
        lastName = findViewById(R.id.resultLastName);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        this.mFirestore = FirebaseFirestore.getInstance();
        email = currentUser.getEmail();

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        //gets reference to user's document
        docRef = mFirestore.collection("users").document(email);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("got users email", "onSuccess: ");
                getData();

            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //fusedLocationProviderClient.requestLocationUpdates( locationRequest,locationCallback, this.getMainLooper());
        }
        else {
            //ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MAP_PERMISSION);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
        System.out.println("entering onStart");
        //listener for my position
        getUserPhoto();
        //listener for other users
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void getData(){
        docRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){
                            firstNameFS  = documentSnapshot.getString("first");
                            lastNameFS  = documentSnapshot.getString("last");
                            tvemail.setText(email);
                            firstName.setText(firstNameFS);
                            lastName.setText(lastNameFS);
                            String result = firstNameFS + " " + lastNameFS;
                            fullName.setText(result);
                            Log.d(getPhoto, "Photo with URL : ");
                        }
                        else{
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

    private void getUserPhoto()
    {
        //listener for my position
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
                    Log.d("URL WAS FOUND HERE!!!! " + photoUrl, "onEvent: ");
                }
            }
        });
    }


   public void startEditProfile(View view){
        Intent intent = new Intent(Profile.this, EditProfile.class);
        startActivity(intent);
   }

   public void onBackEvent(View view){
        Intent intent = new Intent(Profile.this, Main2Activity.class);
        startActivity(intent);
   }

}
