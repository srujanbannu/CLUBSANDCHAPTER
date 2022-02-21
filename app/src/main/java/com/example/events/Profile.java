package com.example.events;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.paging.LoadState;
import androidx.paging.PagingConfig;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Text;

import java.util.Date;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class Profile extends AppCompatActivity {

    private ProgressBar progressBar;
    private RecyclerView EventsRecycler;
    private FirebaseFirestore fireStore;
    private boolean IsAdmin = false;
    private LinearLayout CounterLayout;
    private TextView PostsCount,EventsCount;
    private int postsC = 0;
    private int eventsC = 0;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = database.getReference();
    DatabaseReference likesRef = databaseReference.child("Likes");
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    Boolean IsLiked = false;
    private FirestorePagingAdapter<eventsUtils, eventsViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String UserId = getIntent().getStringExtra("UserId");

        LinearLayout UpdateLay = findViewById(R.id.update_lay);
        TextView Name = findViewById(R.id.club_name);
        TextView About = findViewById(R.id.about);
        TextView Links = findViewById(R.id.links);
        CircleImageView ProfileImg = findViewById(R.id.club_prof_img);
        Button SignOut = findViewById(R.id.sign_out);
        Button Update = findViewById(R.id.update);
        progressBar = new ProgressBar(this);
        EventsRecycler = findViewById(R.id.club_recyclerView);
        CounterLayout = findViewById(R.id.count_layout);
        PostsCount = findViewById(R.id.posts_count);
        EventsCount = findViewById(R.id.events_count);

        fireStore = FirebaseFirestore.getInstance();
        IsAdmin = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("IsAdmin",false);

        SignOut.setOnClickListener(v -> {
            PreferenceManager.getDefaultSharedPreferences(Profile.this).edit().putBoolean("IsAdmin",false).apply();
            GoogleSignInClient mGoogleSignInClient ;
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(getBaseContext(), gso);
            mGoogleSignInClient.signOut().addOnCompleteListener(Profile.this,
                    task -> {
                        FirebaseAuth.getInstance().signOut();
                        Intent setupIntent = new Intent(getBaseContext(),MainActivity.class);
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setupIntent);
                        finishAffinity();
                    });
        });
        Update.setOnClickListener(v -> startActivity(new Intent(Profile.this,UpdateProfile.class)));


        if (Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid().equals(UserId)){
            SignOut.setVisibility(View.VISIBLE);
            About.setVisibility(View.GONE);
            Links.setVisibility(View.GONE);
            CounterLayout.setVisibility(View.GONE);
            if (IsAdmin){
                Update.setVisibility(View.VISIBLE);
                About.setVisibility(View.VISIBLE);
                Links.setVisibility(View.VISIBLE);
                CounterLayout.setVisibility(View.VISIBLE);
            }else Update.setVisibility(View.GONE);
        }else {
            SignOut.setVisibility(View.GONE);
        };

        FirebaseFirestore.getInstance().collection("Clubs").document(UserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.getResult().exists()) {
                        UpdateLay.setVisibility(View.VISIBLE);
                        if (Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid().equals(UserId)) {
                            Update.setVisibility(View.VISIBLE);
                        } else Update.setVisibility(View.GONE);
                        DocumentSnapshot document = task.getResult();
                        Name.setText(Objects.requireNonNull(document.get("Name")).toString());
                        Picasso.get()
                                .load(Objects.requireNonNull(document.get("ProfileImgUrl")).toString())
                                .into(ProfileImg);
                       // Email.setText(document.get("Email").toString());
                        About.setText(document.get("About").toString());
                        Links.setText(document.get("Links").toString());
                    }else {
                        FirebaseFirestore.getInstance().collection("Users").document(UserId)
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    DocumentSnapshot document = task1.getResult();
                                    Name.setText(Objects.requireNonNull(document.get("Name")).toString());
                                   // Email.setText(document.get("Email").toString());
                                    Picasso.get()
                                            .load(Objects.requireNonNull(document.get("ProfileImgUrl")).toString())
                                            .into(ProfileImg);
                                });
                    }

                });


        Query query = fireStore.collection("Posts")
                .whereEqualTo("UserId",UserId)
                .orderBy("Time", Query.Direction.DESCENDING);

        PagingConfig config =  new PagingConfig(2,1);

        FirestorePagingOptions<eventsUtils> options = new FirestorePagingOptions.Builder<eventsUtils>()
                .setLifecycleOwner(this)
                .setQuery(query, config, snapshot -> {
                    String Title = Objects.requireNonNull(snapshot.get("Title")).toString();
                    String ImgUrl = Objects.requireNonNull(snapshot.get("ImgUrl")).toString();
                    String Desc = Objects.requireNonNull(snapshot.get("Desc")).toString();
                    String Userid = Objects.requireNonNull(snapshot.get("UserId")).toString();
                    String postId = snapshot.getId();
                    Timestamp Time = snapshot.getTimestamp("Time");
                    String DyLink = Objects.requireNonNull(snapshot.get("dynamicLink")).toString();
                    Boolean event = snapshot.getBoolean("Event");
                    String RegLink = "";
                    Timestamp RegEndTime = Time;
                    if (event){
                        RegLink = Objects.requireNonNull(snapshot.get("RegistrationLink")).toString();
                        RegEndTime = snapshot.getTimestamp("RegEndTime");
                        eventsC = eventsC+1;

                    }else {
                        postsC = postsC+1;

                    }
                    PostsCount.setText(postsC+"");
                    EventsCount.setText(eventsC+"");
                    return new eventsUtils(
                            Title, Desc, ImgUrl, Userid, postId,
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

        EventsRecycler.setLayoutManager(new LinearLayoutManager(Profile.this));
        EventsRecycler.setHasFixedSize(true);
        //eventsAdapter = new EventsAdapter(EventsList, Events.this);
        EventsRecycler.setAdapter(adapter);


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
        Intent intent = new Intent(Profile.this, Comments.class);
        intent.putExtra("PostId", PostId);
        intent.putExtra("Desc", Desc);
        startActivity(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void ShowImage(Bitmap bitmap) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.image_dialog);

        ImageView imageView = dialog.findViewById(R.id.image);
        ImageButton CloseButton = dialog.findViewById(R.id.close);
        CloseButton.setOnClickListener(v -> dialog.dismiss());
        imageView.setImageBitmap(bitmap);
        imageView.setOnTouchListener(new ImageMatrixTouchHandler(Profile.this));
        imageView.setImageBitmap(bitmap);

        dialog.show();

    }

}