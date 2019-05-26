package com.example.android2_project.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class Profile extends AppCompatActivity {

    private TextView fullName,email,firstName,lastName;
    private FirebaseFirestore mFirestore;
    DocumentReference docRef;
    private String UserId;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth auth;

    private FirebaseUser currentUser;
    String firstNameFS;
    String lastNameFS;
    String emailFS;
    String fullNameFS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        fullName = findViewById(R.id.tvFullName);
        email = findViewById(R.id.tvEmail);
        firstName = findViewById(R.id.resultFirstName);
        lastName = findViewById(R.id.resultLastName);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();


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
    }



   public void getData(){
        docRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){
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

   public void startEditProfile(View view){
        Intent intent = new Intent(Profile.this, EditProfile.class);
        startActivity(intent);
   }

   public void onBackEvent(View view){
        Intent intent = new Intent(Profile.this, Main2Activity.class);
        startActivity(intent);
   }
}
