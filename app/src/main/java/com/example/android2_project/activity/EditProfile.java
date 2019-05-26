package com.example.android2_project.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android2_project.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditProfile extends AppCompatActivity {

    private TextView fullName,email,firstName,lastName;
    private TextView etfirstName,etlastName;
    private FirebaseFirestore mFirestore;
    DocumentReference docRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    String firstNameFS;
    String lastNameFS;
    String emailFS;
    double Latitude;
    double Longitude;
    private static int RESULT_LOAD_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

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
            }
        });

        Button buttonLoadImage = findViewById(R.id.tvEmail);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.imgProfile);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
        }
    }

    private void uploadImage() {

        if(filePath != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(EditProfile.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(EditProfile.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
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
                            emailFS = documentSnapshot.getString("email");
                            firstNameFS  = documentSnapshot.getString("first");
                            lastNameFS  = documentSnapshot.getString("last");
                            email.setText(emailFS);
                            firstName.setText(firstNameFS);
                            lastName.setText(lastNameFS);
                            String result = firstNameFS + " " + lastNameFS;
                            fullName.setText(result);
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
                        Log.d("BIGERROR", "onError: ");
                    }
                });
    }

    public void updateData(View view){
        String userFirstName = etfirstName.getText().toString().trim();
        String userLastName = etlastName.getText().toString().trim();
        Map<String,Object> data = new HashMap<>();
        data.put("latitude", Latitude);
        data.put("longitude", Longitude);
        data.put("first",userFirstName);
        data.put("last",userLastName);
        docRef.set(data);
        Intent intent = new Intent(EditProfile.this, Profile.class);
        startActivity(intent);
    }

    public void onBackEvent(View view){
        Intent intent = new Intent(EditProfile.this, Main2Activity.class);
        startActivity(intent);
    }
}
