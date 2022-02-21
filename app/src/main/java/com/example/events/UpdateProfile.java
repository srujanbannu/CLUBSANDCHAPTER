package com.example.events;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class
UpdateProfile extends AppCompatActivity {

    private ImageView ProfileImg;
    private FirebaseUser User;
    private EditText About,Links;
    private String PhotoUrl;
    private FirebaseFirestore db;
    private Boolean PicSelected = false;
    private ProgressDialog PD;
    private Boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        User = FirebaseAuth.getInstance().getCurrentUser();
        ProfileImg = findViewById(R.id.club_image);
        TextView email1 = findViewById(R.id.club_email);
        TextView name1 = findViewById(R.id.club_name);
        About = findViewById(R.id.about);
        Links = findViewById(R.id.links);
        Button update = findViewById(R.id.update);
        db = FirebaseFirestore.getInstance();
        PD = new ProgressDialog(this);

        name1.setText(User.getDisplayName());
        email1.setText(User.getEmail());
        Picasso.get().load(User.getPhotoUrl()).into(ProfileImg);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        isAdmin = preferences.getBoolean("IsAdmin",false);


        if (!isAdmin){
            findViewById(R.id.about_lay).setVisibility(View.GONE);
            findViewById(R.id.link_lay).setVisibility(View.GONE);
            update.setText("Continue");
        }else getClubDetails(FirebaseAuth.getInstance().getCurrentUser().getUid());


        ProfileImg.setOnClickListener(v -> {
            if(isAdmin) {
                ImagePicker.with(UpdateProfile.this)
                        .cropSquare()
                        .galleryOnly()
                        .compress(100)
                        .maxResultSize(1080, 1080)
                        .start(0);
            }else {
                Toast.makeText(this, "Sorry \nChanging profile is limited\nOnly for clubs for Now", Toast.LENGTH_SHORT).show();
            }
        });

        update.setOnClickListener(v -> {
            PD.setMessage("Updating...");
            if (PicSelected){
                UploadImage();
            }else UpdateDetails(false);
        });



    }

    private void getClubDetails(String uid) {
        db.collection("Clubs")
                .document(User.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()){
                        DocumentSnapshot document = task.getResult();
                        About.setText(document.get("About").toString());
                        Links.setText(document.get("Links").toString());
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            assert data != null;
            Uri profileUri = data.getData();
            if (requestCode == 0) {
                PicSelected = true;
                ProfileImg.setImageURI(profileUri);
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show();
        }

    }

    private void UploadImage() {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        PD.setMessage("Uploading...");
        ProfileImg.setDrawingCacheEnabled(true);
        ProfileImg.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) ProfileImg.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference storageRef = storage.getReference().child("ClubsProfileImgs/");
        StorageReference mountainsRef = storageRef.child(Objects.requireNonNull(User.getDisplayName()));
        UploadTask uploadTask = mountainsRef.putBytes(data);
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            PD.setMessage("Upload is " + progress + "% done");
        });
        uploadTask.addOnFailureListener(exception -> {
            PD.dismiss();
            Toast.makeText(UpdateProfile.this, "Error : "+exception, Toast.LENGTH_SHORT).show();
        }).addOnSuccessListener(taskSnapshot ->
                mountainsRef.getDownloadUrl().addOnSuccessListener(uri -> {
            PhotoUrl = uri.toString();
            UpdateDetails(true);
        }));
    }

    private void UpdateDetails(boolean SelectedImg) {
        PD.setMessage("Update...");
        String Collection;
        Map<String, Object> Post = new HashMap<>();
        if (SelectedImg){
            Post.put("ProfileImgUrl",PhotoUrl);
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(PhotoUrl))
                    .build();
            User.updateProfile(profileUpdates);
        }else Post.put("ProfileImgUrl", Objects.requireNonNull(User.getPhotoUrl()).toString());

        Post.put("Name", User.getDisplayName());
        Post.put("Email", User.getEmail());
        Post.put("About",About.getText().toString().trim());
        Post.put("Links",Links.getText().toString().trim());

        if (isAdmin){
            Collection = "Clubs";
        }else Collection = "Users";

        db.collection(Collection)
                .document(User.getUid())
                .set(Post)
                .addOnSuccessListener(aVoid -> {
                    PD.dismiss();
                    startActivity(new Intent(UpdateProfile.this, MainActivity.class));
                })
                .addOnFailureListener(e -> {
                    PD.dismiss();
                    Toast.makeText(UpdateProfile.this, "Error : "+e, Toast.LENGTH_SHORT).show();
                });
    }



}