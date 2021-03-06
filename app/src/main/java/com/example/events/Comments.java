package com.example.events;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Comments extends AppCompatActivity {

    private ProgressBar progressBar;
    private RecyclerView CommentsRecycler;
    private ArrayList<commentsUtils> commentsList;
    private CommentsAdapter commentsAdapter;
    private DatabaseReference postCommentRef;
    private EditText commentET;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);



        String postId = getIntent().getStringExtra("PostId");
        final String[] desc = {""};
        try {
            desc[0] =  getIntent().getStringExtra("Desc");
        }catch (Exception e){
            FirebaseFirestore.getInstance().collection("Posts").document(postId).get()
                    .addOnCompleteListener(task -> desc[0] = String.valueOf(task.getResult().get("Desc")));
        }

        progressBar =  findViewById(R.id.pm);
        TextView description = findViewById(R.id.post_desc);
        description.setText(desc[0]);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        postCommentRef = database.child("Comments").child(postId);

        commentET = findViewById(R.id.comment_et);
        ImageButton send = findViewById(R.id.send_comment);

        progressBar.setVisibility(View.VISIBLE);
        CommentsRecycler = findViewById(R.id.comments_recyclerView);
        ImageView profileImg = findViewById(R.id.current_user_prof_img);

        Picasso.get()
                .load(Objects.requireNonNull(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getPhotoUrl()).toString())
                .into(profileImg);

        CommentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        commentsList = new ArrayList<>();


        postCommentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot ds : snapshot.getChildren())
                    {
                        commentsList.add(ds.getValue(commentsUtils.class));
                    }
                    commentsAdapter = new CommentsAdapter(commentsList,Comments.this);
                    CommentsRecycler.setAdapter(commentsAdapter);
                    commentsAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                }else {
                    Toast.makeText(Comments.this, "No comments", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                Toast.makeText(Comments.this, "", Toast.LENGTH_SHORT).show();
            }
        });
        commentsAdapter = new CommentsAdapter(commentsList,Comments.this);
        CommentsRecycler.setAdapter(commentsAdapter);

        send.setOnClickListener(v -> {
            if (!commentET.getText().toString().isEmpty()){
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Loading...");
                progressDialog.show();

                String Comment = commentET.getText().toString().trim();
                String UserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String Time = new Date().toString();
                String key = postCommentRef.push().getKey();
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put( key+"/UserId/", UserId);
                childUpdates.put( key+"/Comment/", Comment);
                childUpdates.put( key+"/Time",Time);

                commentsList.add(new commentsUtils(Comment,UserId,Time));
                commentsAdapter.notifyDataSetChanged();

                postCommentRef.updateChildren(childUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        commentET.setText("");
                    }else Toast.makeText(Comments.this, "Error : "+task.getException(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });

            }
        });


    }
}