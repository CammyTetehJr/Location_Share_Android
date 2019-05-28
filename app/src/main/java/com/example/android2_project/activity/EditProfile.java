package com.example.android2_project.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android2_project.R;
import com.example.android2_project.model.User;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditProfile extends AppCompatActivity implements View.OnClickListener {

    private TextView fullName,email,firstName,lastName;
    private TextView etfirstName,etlastName;
    private FirebaseFirestore mFirestore;
    DocumentReference docRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    String firstNameFS;
    String lastNameFS;
    double Latitude;
    double Longitude;
    // image upload
    private static final int PICK_IMAGE_REQUEST = 234;
    private Button buttonChoose;
    private Button buttonUpload;
    private Uri filePath;
    private StorageReference storageReference;
    private FirebaseStorage mDatabase;
    private ImageView imageView;
    //other stuff
    String userFirstName;
    String userLastName;
    String uploadId;
    String photoUrl;
    TextView getURL;
    private static int RESULT_LOAD_IMAGE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);




        buttonChoose = (Button) findViewById(R.id.btnChoose);
        buttonUpload = (Button) findViewById(R.id.btnSave);
        imageView = (ImageView) findViewById(R.id.imgProfile);
        mDatabase = FirebaseStorage.getInstance();
        storageReference = mDatabase.getReferenceFromUrl("gs://android2-project-6d7c0.appspot.com/");
        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);

        // get user data
        fullName = findViewById(R.id.tvFullName);
        email = findViewById(R.id.tvEmail);
        firstName = findViewById(R.id.resultFirstName);
        lastName = findViewById(R.id.resultLastName);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        //get inserted date
        etfirstName = findViewById(R.id.resultFirstName);
        etlastName = findViewById(R.id.resultLastName);

        this.mFirestore = FirebaseFirestore.getInstance();
        String email = currentUser.getEmail();
        //gets reference to user's document
        docRef = mFirestore.collection("users").document(email);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("got users email", "onSuccess: ");
                getData();
                //UpdatePhoto(imageView, photoUrl);
            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onClick(View view) {
        if (view == buttonChoose) {
            showFileChooser();
        } else if (view == buttonUpload) {
            uploadData();
        }
    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadData() {
        //checking if file is available
        if (filePath != null) {
            //displaying progress dialog while image is uploading
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            //getting the storage reference
            final StorageReference sRef = storageReference.child(Constants.STORAGE_PATH_UPLOADS + System.currentTimeMillis() + "." + getFileExtension(filePath));

            //adding the file to reference
            sRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //dismissing the progress dialog
                            progressDialog.dismiss();


                            //displaying success toast
                            Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();

                            //creating the upload object to store uploaded image details
                            //User upload = new User(userFirstName,userLastName,taskSnapshot.getDownloadUrl().toString());

                            //adding an upload to firebase database
                            uploadId = taskSnapshot.getDownloadUrl().toString();
                            updateData();
                            getData();


                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //displaying the upload progress
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        } else {
            //display an error if no file is selected
        }
    }

    public void getData(){
        docRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){
                            Latitude = documentSnapshot.getDouble("latitude");
                            Longitude = documentSnapshot.getDouble("longitude");
                            firstNameFS  = documentSnapshot.getString("first");
                            lastNameFS  = documentSnapshot.getString("last");
                            firstName.setText(firstNameFS);
                            lastName.setText(lastNameFS);
                            String result = firstNameFS + " " + lastNameFS;
                            fullName.setText(result);
                            //UpdatePhoto(imageView, photoUrl);
                        }
                        else{
                            Toast.makeText(EditProfile.this, "Not found", Toast.LENGTH_SHORT).show();
                            Log.d("error", "onError: ");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfile.this, "Coundn't find", Toast.LENGTH_SHORT).show();
                        Log.d("Error", "onError: ");
                    }
                });
    }

    public void updateData(){
        userFirstName = etfirstName.getText().toString().trim();
        userLastName = etlastName.getText().toString().trim();
        final Map<String,Object> user = new HashMap<>();
        user.put("photoUrl",uploadId);
        user.put("latitude", Latitude);
        user.put("longitude", Longitude);
        user.put("first",userFirstName);
        user.put("last",userLastName);
        docRef.set(user);
    }

    public void onBackEvent(View view){
        Intent intent = new Intent(EditProfile.this, Profile.class);
        startActivity(intent);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}


