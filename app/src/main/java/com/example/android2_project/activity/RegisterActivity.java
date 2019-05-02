package com.example.android2_project.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android2_project.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // Cloud Firestore instance
    FirebaseFirestore db;

    // Collection reference
    CollectionReference users;

    //Firebase Auth Reference
    private FirebaseAuth mAuth;

    //Firebase user
    final FirebaseUser user = null;

    EditText firstname;
    EditText lastname;
    EditText email;
    EditText password;
    Button register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //initialize firestore instance
        initializeCloudFirebaseInstance();
        //initialize collection
        users = db.collection("users");
        //get views
        firstname = findViewById(R.id.firstname);
        lastname = findViewById(R.id.lastname);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        register = findViewById(R.id.btnRegister);

        register.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                RegisterNewUser(firstname, lastname, email, password);
            }
        });
    }

    private void initializeCloudFirebaseInstance() {
        db = FirebaseFirestore.getInstance();
    }

    private void RegisterNewUser(EditText first, EditText last, EditText email, EditText password) {
        // Create records of user in database
        final Map<String, Object>[] user = new Map[]{new HashMap<>()};
        user[0].put("first", first.getText().toString());
        user[0].put("last", last.getText().toString());
//        user.put("email", email.getText().toString());
//        user.put("password", password.getText().toString());

        //create user with email as document reference id
        users.document(email.getText().toString()).set(user[0]).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                Log.d("add user data success", "Document added");

            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("add user failure", "Error adding document", e);
                    }
                });


        // Add a new document with a generated ID
//        db.collection("users")
//                .add(user)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Log.d("add user success", "DocumentSnapshot added with ID: " + documentReference.getId());
//                        //go to maps view
//                        goToMapsView();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.w("add user failure", "Error adding document", e);
//                    }
//                });



        //create auth user
        mAuth = FirebaseAuth.getInstance();

        mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("user status", "createUserWithEmail:success");
                            FirebaseUser user= mAuth.getCurrentUser();
                            //go to maps activity
                            goToMapsView();
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("user status", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

        private void goToMapsView()
        {
            Intent intent = new Intent(RegisterActivity.this, MapsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }


    }


