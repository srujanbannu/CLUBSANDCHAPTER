package com.example.events;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
        
 public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 234;
    private static final String TAG = "srujan";
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    private ProgressDialog pm;
    private TextView Message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pm = new ProgressDialog(this);
        pm.setCancelable(false);
        
        Message = findViewById(R.id.message);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        findViewById(R.id.signin).setOnClickListener(view -> {

                signIn();
                pm.setMessage("Signing In...");
                pm.show();

        });

    }



     @Override
     protected void onStart() {
         super.onStart();
         if (mAuth.getCurrentUser() != null) {
             finish();
             startActivity(new Intent(this, Events.class));
         }
     }


     @RequiresApi(api = Build.VERSION_CODES.M)
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);

         if (requestCode == RC_SIGN_IN) {
             Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
             try {
                 GoogleSignInAccount account = task.getResult(ApiException.class);
                 firebaseAuthWithGoogle(account);

             } catch (ApiException e)
             {
                 Toast.makeText(MainActivity.this,"Login failed\nPlease try again",Toast.LENGTH_SHORT).show();
                 Toast.makeText(MainActivity.this, "ERROR CODE "+e.getMessage(), Toast.LENGTH_SHORT).show();
                 pm.dismiss();
             }
         }
     }


     public void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
         Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

         //getting the auth credential
         AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
         mAuth.signInWithCredential(credential)
                 .addOnCompleteListener(this, task -> {
                     if (task.isSuccessful()) {
                         IsAdmin();

                     } else {
                         pm.dismiss();
                         Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

                     }

                 });
     }


     private void signIn() {
         Intent signInIntent = mGoogleSignInClient.getSignInIntent();
         startActivityForResult(signInIntent, RC_SIGN_IN);
     }

     @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
     @Override
     public void onBackPressed() {
         System.exit(0);
         finishAffinity();
     }

     public void IsAdmin(){

         FirebaseDatabase.getInstance().getReference().child("Admins")
                 .addListenerForSingleValueEvent(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                 for (DataSnapshot Ds : snapshot.getChildren()) {
                     String email = Ds.child("email").getValue().toString();

                     if (mAuth.getCurrentUser().getEmail().equals(email)) {
                         PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("IsAdmin", true).apply();
                         break;
                     } else {
                         PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("IsAdmin", false).apply();
                     }
                 }
                 pm.dismiss();
                 startActivity(new Intent(MainActivity.this, UpdateProfile.class));
               /*  if (VerifyUser()) {
                     pm.dismiss();
                     startActivity(new Intent(MainActivity.this, UpdateProfile.class));
                 }else {
                     pm.dismiss();
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("IsAdmin",false).apply();
                     startActivity(new Intent(MainActivity.this, UpdateProfile.class));*/

                     /* mGoogleSignInClient.signOut().addOnCompleteListener(MainActivity.this,
                             task -> {
                                 FirebaseAuth.getInstance().signOut();
                                 Message.setText("Error : Not using Vitap Email.\nTry again");
                             });
                      */



             }

             @Override
             public void onCancelled(@NonNull @NotNull DatabaseError error) {

             }
         });
     }

     public boolean VerifyUser(){
         FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
         assert user != null;
         String email = user.getEmail();
         assert email != null;
         if (email.contains("vitap.ac.in")) {
          return true;
         }else return false;

     }
}