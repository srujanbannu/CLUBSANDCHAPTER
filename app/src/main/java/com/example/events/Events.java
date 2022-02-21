package com.example.events;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.paging.PagedList;
import androidx.paging.PagingConfig;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.SnapshotParser;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class Events extends AppCompatActivity {

    private ProgressBar progressBar;
    private RecyclerView EventsRecycler;
    private FirebaseFirestore fireStore;
    private Context context;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = database.getReference();
    DatabaseReference likesRef = databaseReference.child("Likes");
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    Boolean IsLiked = false;
    private FirestorePagingAdapter<eventsUtils,eventsViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        fireStore = FirebaseFirestore.getInstance();

        context = this;

        FirebaseMessaging.getInstance().subscribeToTopic("general");

        progressBar = findViewById(R.id.pm);
        CircleImageView ProfileImg = findViewById(R.id.My_profile);
        Picasso.get()
                .load(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getPhotoUrl())
                .into(ProfileImg);
        ProfileImg.setOnClickListener(v -> {
            Intent intent = new Intent(this, Profile.class);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    Pair.create(ProfileImg,"profile_trans"));
            intent.putExtra("UserId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            startActivity(intent,options.toBundle());
        });


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean isAdmin = preferences.getBoolean("IsAdmin", false);
        if (isAdmin) {
            findViewById(R.id.newPost).setVisibility(View.VISIBLE);
        } else findViewById(R.id.newPost).setVisibility(View.GONE);

        progressBar.setVisibility(View.VISIBLE);
        EventsRecycler = findViewById(R.id.events_recyclerView);

        Query query = fireStore.collection("Posts")
                .orderBy("Time", Query.Direction.DESCENDING);

        PagingConfig config =  new PagingConfig(2,1);


        FirestorePagingOptions<eventsUtils> options = new FirestorePagingOptions.Builder<eventsUtils>()
                .setLifecycleOwner(this)
                .setQuery(query, config, snapshot -> {
                        String Title = Objects.requireNonNull(snapshot.get("Title")).toString();
                        String ImgUrl = Objects.requireNonNull(snapshot.get("ImgUrl")).toString();
                        String Desc = Objects.requireNonNull(snapshot.get("Desc")).toString();
                        String UserId = Objects.requireNonNull(snapshot.get("UserId")).toString();
                        String postId = snapshot.getId();
                        Timestamp Time = snapshot.getTimestamp("Time");
                        String DyLink = Objects.requireNonNull(snapshot.get("dynamicLink")).toString();
                        Boolean event = snapshot.getBoolean("Event");
                        String RegLink = "";
                        Timestamp RegEndTime = new Timestamp(new Date());
                        if (event){
                            RegLink = Objects.requireNonNull(snapshot.get("RegistrationLink")).toString();
                            RegEndTime = snapshot.getTimestamp("RegEndTime");
                        }

                    return new eventsUtils(
                            Title, Desc, ImgUrl, UserId, postId,
                            Objects.requireNonNull(Time).toDate(), DyLink,
                            event, Objects.requireNonNull(RegEndTime).toDate(),RegLink
                    );
                })
                .build();

        adapter = new FirestorePagingAdapter<eventsUtils, eventsViewHolder>(options) {

            @NonNull
            public eventsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.events_items, parent, false);
                return new eventsViewHolder(view);
            }

            @SuppressLint("SetTextI18n")
            @Override
            protected void onBindViewHolder(@NonNull eventsViewHolder holder, int position, @NonNull eventsUtils model) {

                holder.Title.setText(model.getTitle());
                holder.ImageView.setImageTintList(null);

                holder.time.setText(new PrettytimeFromat().Ago(model.getTime().toString()));

                String PostId = model.getPostId();
                String Desc = model.getDesc();
                String UserId = model.getUserId();
                holder.Decs.setText(Desc);

                if (!model.getImgUrl().isEmpty()) {
                    Picasso.get()
                            .load(model.getImgUrl())
                            .into(holder.ImageView);
                    holder.Decs.setMaxLines(3);
                    holder.Decs.setEllipsize(TextUtils.TruncateAt.END);
                } else {
                    holder.ImageView.setVisibility(View.GONE);
                    holder.Decs.setMaxLines(14);
                    holder.Decs.setSingleLine(false);
                }

                if (model.isEvent()){
                    holder.RegistrationLayout.setVisibility(View.VISIBLE);
                    holder.RegEndTime.setText("Registration ends : "+new PrettytimeFromat().Ago(model.getRegEndTime().toString()));
                    holder.register.setOnClickListener(v ->
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(model.getRegLink())))
                    );
                }else holder.RegistrationLayout.setVisibility(View.GONE);

                FirebaseFirestore.getInstance().collection("Clubs").document(UserId).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                holder.clubName.setText(Objects.requireNonNull(document.get("Name")).toString());
                                Picasso.get()
                                        .load(Objects.requireNonNull(document.get("ProfileImgUrl")).toString())
                                        .centerInside()
                                        .resize(300,200)
                                        .into(holder.ClubImg);
                            }
                        });

                holder.Comment.setOnClickListener(v -> openCommentsActivity(PostId, Desc));
                holder.PostCard.setOnClickListener(v -> openCommentsActivity(PostId, Desc));
                holder.Decs.setOnClickListener(v -> openCommentsActivity(PostId, Desc));
                holder.ImageView.setOnClickListener(v -> {
                    Bitmap bitmap = ((BitmapDrawable) holder.ImageView.getDrawable()).getBitmap();
                    ShowImage(bitmap);
                });

                holder.clubName.setOnClickListener(v -> {
                    Intent intent = new Intent(context, Profile.class);
                    intent.putExtra("UserId", model.getUserId());
                    context.startActivity(intent);
                });
                holder.ClubImg.setOnClickListener(v -> {
                    Intent intent = new Intent(context, Profile.class);
                    intent.putExtra("UserId", model.getUserId());
                    context.startActivity(intent);
                });

                holder.Share.setOnClickListener(v -> {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, model.getDyLink());
                    sendIntent.setType("text/plain");
                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    startActivity(shareIntent);
                });


                holder.setLikesButtonStatus(PostId);
                holder.getCommentsCount(PostId);


                holder.Like.setOnClickListener(v -> {
                    IsLiked = true;
                    likesRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                            if (IsLiked) {
                                if (snapshot.child(PostId).hasChild(user.getUid())) {
                                    likesRef.child(PostId).child(user.getUid()).removeValue();
                                    holder.Like.setImageResource(R.drawable.ic_love);
                                } else {
                                    likesRef.child(PostId).child(user.getUid()).setValue(new Date().toString());
                                    holder.Like.setImageResource(R.drawable.ic_heart_1_);

                                }
                                IsLiked = false;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull @NotNull DatabaseError error) {

                        }
                    });
                });


            }

        };


        /*
        firestore.collection("Posts")
                .orderBy("Time", Query.Direction.DESCENDING)
                .limit(Limit)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String Title = Objects.requireNonNull(document.get("Title")).toString();
                                String ImgUrl = Objects.requireNonNull(document.get("ImgUrl")).toString();
                                String Desc = Objects.requireNonNull(document.get("Desc")).toString();
                                String UserId = Objects.requireNonNull(document.get("UserId")).toString();
                                String postId = document.getId();
                                Timestamp Time = document.getTimestamp("Time");
                                String DyLink = Objects.requireNonNull(document.get("dynamicLink")).toString();
                                EventsList.add(new eventsUtils(Title, Desc, ImgUrl, UserId, postId, Objects.requireNonNull(Time).toDate(), DyLink));
                            }
                            eventsAdapter.notifyDataSetChanged();

                        } else Toast.makeText(this, "No new Posts", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    } else progressBar.setVisibility(View.GONE);
                });

         */


        EventsRecycler.setLayoutManager(new LinearLayoutManager(Events.this));
        EventsRecycler.setHasFixedSize(true);
        //eventsAdapter = new EventsAdapter(EventsList, Events.this);
        EventsRecycler.setAdapter(adapter);


        findViewById(R.id.newPost).setOnClickListener(v ->
                startActivity(new Intent(Events.this, NewPost.class))
        );



        adapter.addLoadStateListener(states -> {

            LoadState refresh = states.getRefresh();
            LoadState append = states.getAppend();

            // The previous load (either initial or additional) failed. Call
            // the retry() method in order to retry the load operation.
            if (refresh instanceof LoadState.Error || append instanceof LoadState.Error) {

            }

            if (refresh instanceof LoadState.Loading) {
                // The initial Load has begun
                // ...

            }


            if (append instanceof LoadState.Loading) {
                // The adapter has started to load an additional page
                // ...
                progressBar.setVisibility(View.VISIBLE);
            }

            if (append instanceof LoadState.NotLoading) {
                LoadState.NotLoading notLoading = (LoadState.NotLoading) append;
                if (notLoading.getEndOfPaginationReached()) {
                    // The adapter has finished loading all of the data set
                    // ...
                    progressBar.setVisibility(View.GONE);
                    return null;
                }

                if (refresh instanceof LoadState.NotLoading) {
                    // The previous load (either initial or additional) completed
                    // ...
                    progressBar.setVisibility(View.GONE);

                    return null;
                }
            }
            return null;
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private class eventsViewHolder extends RecyclerView.ViewHolder {
        TextView Title, Decs, time, clubName, likesCount, CommentsCount,RegEndTime;
        android.widget.ImageView ImageView, ClubImg;
        ImageButton Like, Comment, Share;
        RelativeLayout RegistrationLayout;
        Button register;
        CardView PostCard;

        public eventsViewHolder(@NonNull View itemView) {
            super(itemView);
            Title = itemView.findViewById(R.id.event_title);
            Decs = itemView.findViewById(R.id.event_decs);
            ImageView = itemView.findViewById(R.id.event_image);
            time = itemView.findViewById(R.id.time);
            clubName = itemView.findViewById(R.id.club_name);
            ClubImg = itemView.findViewById(R.id.club_prof_img);
            Like = itemView.findViewById(R.id.like);
            Comment = itemView.findViewById(R.id.comment);
            likesCount = itemView.findViewById(R.id.like_counter);
            CommentsCount = itemView.findViewById(R.id.comment_counter);
            PostCard = itemView.findViewById(R.id.post_card);
            Share = itemView.findViewById(R.id.share);
            RegEndTime = itemView.findViewById(R.id.reg_end_time);
            register = itemView.findViewById(R.id.register_button);
            RegistrationLayout = itemView.findViewById(R.id.event_registration_layout);
        }

        public void setLikesButtonStatus(final String postId) {
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                    if (snapshot.child(postId).hasChild(user.getUid())) {
                        String LikesCount = String.valueOf((int) snapshot.child(postId).getChildrenCount());
                        likesCount.setText(LikesCount);
                        Like.setImageResource(R.drawable.ic_heart_1_);
                    } else {
                        String LikesCount = String.valueOf((int) snapshot.child(postId).getChildrenCount());
                        likesCount.setText(LikesCount);
                        Like.setImageResource(R.drawable.ic_love);
                    }
                }

                @Override
                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                }
            });


        }
        public void getCommentsCount(final String postId){
            databaseReference
                    .child("Comments")
                    .child(postId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                            String commentsCount = String.valueOf((int) snapshot.getChildrenCount());
                            CommentsCount.setText(commentsCount);
                        }

                        @Override
                        public void onCancelled(@NonNull @NotNull DatabaseError error) {

                        }
                    });
        }

    }

    public void openCommentsActivity(String PostId, String Desc) {
        Intent intent = new Intent(context, Comments.class);
        intent.putExtra("PostId", PostId);
        intent.putExtra("Desc", Desc);
        startActivity(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void ShowImage(Bitmap bitmap) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.image_dialog);

        ImageView imageView = dialog.findViewById(R.id.image);
        ImageButton CloseButton = dialog.findViewById(R.id.close);
        CloseButton.setOnClickListener(v -> dialog.dismiss());
        imageView.setImageBitmap(bitmap);
        imageView.setOnTouchListener(new ImageMatrixTouchHandler(Events.this));
        dialog.show();

    }


}