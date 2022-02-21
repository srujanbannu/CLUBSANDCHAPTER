package com.example.events;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class Post extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference databaseReference ;
    DatabaseReference likesRef ;
    FirebaseUser user ;
    Boolean IsLiked = false;
    private ProgressDialog progressDialog;
    private String PostId ;
    private TextView ClubName,Time,Title,Desc,LikesCount,CommentsCount;
    private ImageView Image;
    private CircleImageView ClubImg;
    private ImageButton Like,Comment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        progressDialog = new ProgressDialog(this);

        Title = findViewById(R.id.event_title);
        Desc = findViewById(R.id.event_decs);
        Image = findViewById(R.id.event_image);
        Time = findViewById(R.id.time);
        ClubName = findViewById(R.id.club_name);
        ClubImg = findViewById(R.id.club_prof_img);
        Like = findViewById(R.id.like);
        Comment = findViewById(R.id.comment);
        LikesCount = findViewById(R.id.like_counter);
        CommentsCount = findViewById(R.id.comment_counter);

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        likesRef = databaseReference.child("Likes");
        user = FirebaseAuth.getInstance().getCurrentUser();

        progressDialog.setMessage("Loading...");
        progressDialog.show();

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Uri deepLink ;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                            PostId = deepLink.toString().replace("https://clubsforvitap.web.app/","");
                            getPostDetails(PostId);
                        }
                        progressDialog.dismiss();

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Post.this, "Error : "+e, Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (user == null){
            startActivity(new Intent(Post.this,MainActivity.class));
        }
    }

    public void getPostDetails(String postId){
        FirebaseFirestore.getInstance().collection("Posts")
                .document(postId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        if (task.getResult().exists()){
                            DocumentSnapshot document = task.getResult();
                            String title = Objects.requireNonNull(document.get("Title")).toString();
                            String imgUrl = Objects.requireNonNull(document.get("ImgUrl")).toString();
                            String desc = Objects.requireNonNull(document.get("Desc")).toString();
                            String ClubId = Objects.requireNonNull(document.get("UserId")).toString();
                            String postId1 = document.getId();
                            Timestamp time = document.getTimestamp("Time");
                            String dynamicLink = document.get("dynamicLink").toString();

                            Title.setText(title);
                            Desc.setText(desc);
                            Time.setText(new PrettytimeFromat().Ago(time.toDate().toString()));
                            if (imgUrl.isEmpty()){
                                Image.setVisibility(View.GONE);
                            }else Picasso.get().load(imgUrl).into(Image);
                            getClubDetails(ClubId);
                            getCommentsCount(postId1);
                            setLikesButtonStatus(postId1);
                            Comment.setOnClickListener(v -> openCommentsActivity(postId1,desc));
                            Desc.setOnClickListener(v -> openCommentsActivity(postId1,desc));

                            findViewById(R.id.share).setOnClickListener(v -> {
                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, title+" "+dynamicLink);
                                sendIntent.setType("text/plain");
                                Intent shareIntent = Intent.createChooser(sendIntent, null);
                                startActivity(shareIntent);
                            });

                        }
                    }
                });


    }

    public void getClubDetails(String clubId){
        FirebaseFirestore.getInstance().collection("Clubs").document(clubId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        ClubName.setText(Objects.requireNonNull(document.get("Name")).toString());
                        Picasso.get()
                                .load(Objects.requireNonNull(document.get("ProfileImgUrl")).toString())
                                .into(ClubImg);
                    }
                });

        ClubName.setOnClickListener(v -> openProfile(clubId));
        ClubImg.setOnClickListener(v -> openProfile(clubId));
    }

    public void setLikesButtonStatus(final String postId){
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.child(postId).hasChild(user.getUid())){
                    String likesCount = String.valueOf((int) snapshot.child(postId).getChildrenCount());
                    LikesCount.setText(likesCount);
                    Like.setImageResource(R.drawable.ic_heart_1_);
                }else {
                    String likesCount = String.valueOf((int) snapshot.child(postId).getChildrenCount());
                    LikesCount.setText(likesCount);
                    Like.setImageResource(R.drawable.ic_love);
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        Like.setOnClickListener(v -> {
            IsLiked = true;
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                    if (IsLiked){
                        if(snapshot.child(PostId).hasChild(user.getUid())){
                            likesRef.child(PostId).child(user.getUid()).removeValue();
                            Like.setImageResource(R.drawable.ic_love);
                            IsLiked = false;
                        }else {
                            likesRef.child(PostId).child(user.getUid()).setValue(new Date().toString());
                            Like.setImageResource(R.drawable.ic_heart_1_);
                            IsLiked = false;

                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                }
            });
        });
    }

    public void getCommentsCount(final String postId){
        databaseReference
                .child("Comments")
                .child(postId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        String commentCount = String.valueOf((int) snapshot.getChildrenCount());
                        CommentsCount.setText(commentCount);
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });
    }

    public void openCommentsActivity(String PostId,String Desc){
        Intent intent = new Intent(Post.this,Comments.class);
        intent.putExtra("PostId",PostId);
        intent.putExtra("Desc",Desc);
        startActivity(intent);
    }
    public void openProfile(String clubId){
        Intent intent = new Intent(Post.this,Profile.class);
        intent.putExtra("UserId",clubId);
        startActivity(intent);
    }
}