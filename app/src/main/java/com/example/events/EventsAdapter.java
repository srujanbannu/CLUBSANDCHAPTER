package com.example.events;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.type.Color;
import com.squareup.picasso.Picasso;
import org.jetbrains.annotations.NotNull;
import org.ocpsoft.prettytime.PrettyTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventsViewHolder> {

    ArrayList<eventsUtils> EventsList;
    Context context;
    EventsAdapter(ArrayList<eventsUtils> list,Context context){
        this.EventsList = list;
        this.context = context;
    }
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference  databaseReference = database.getReference();
    DatabaseReference likesRef = databaseReference.child("Likes");
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    Boolean IsLiked = false;


    @NonNull
    @NotNull
    @Override
    public EventsViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.events_items,parent,false);
        return new EventsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull EventsAdapter.EventsViewHolder holder, int position) {

        holder.Title.setText(EventsList.get(position).getTitle());
        holder.ImageView.setImageTintList(null);

        holder.time.setText(new PrettytimeFromat().Ago(EventsList.get(position).getTime().toString()));

        String PostId = EventsList.get(position).getPostId();
        String Desc = EventsList.get(position).getDesc();
        String UserId = EventsList.get(position).getUserId();
        holder.Decs.setText(Desc);

        if (!EventsList.get(position).getImgUrl().isEmpty()) {
            Picasso.get()
                    .load(EventsList.get(position).getImgUrl())
                    .into(holder.ImageView);
            holder.Decs.setMaxLines(3);
            holder.Decs.setEllipsize(TextUtils.TruncateAt.END);
        }
        else {
            holder.ImageView.setVisibility(View.GONE);
            holder.Decs.setMaxLines(12);
            holder.Decs.setSingleLine(false);
        }

        holder.setLikesButtonStatus(EventsList.get(position).getPostId());
        holder.getCommentsCount(EventsList.get(position).getPostId());

        FirebaseFirestore.getInstance().collection("Clubs").document(UserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        holder.clubName.setText(Objects.requireNonNull(document.get("Name")).toString());
                        Picasso.get()
                                .load(Objects.requireNonNull(document.get("ProfileImgUrl")).toString())
                                .into(holder.ClubImg);
                    }
                });

        holder.Comment.setOnClickListener(v -> openCommentsActivity(PostId,Desc));
        holder.PostCard.setOnClickListener(v -> openCommentsActivity(PostId,Desc));
        holder.Decs.setOnClickListener(v -> openCommentsActivity(PostId,Desc));
        holder.ImageView.setOnClickListener(v -> {
            Bitmap bitmap = ((BitmapDrawable)holder.ImageView.getDrawable()).getBitmap();
            ShowImage(bitmap);
        });

        holder.clubName.setOnClickListener(v -> {
            Intent intent = new Intent(context,Profile.class);
            intent.putExtra("UserId",EventsList.get(position).getUserId());
            context.startActivity(intent);
        });
        holder.ClubImg.setOnClickListener(v -> {
            Intent intent = new Intent(context,Profile.class);
            intent.putExtra("UserId",EventsList.get(position).getUserId());
            context.startActivity(intent);
        });

        holder.Share.setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, EventsList.get(position).getTitle()+
                    " "+EventsList.get(position).getDyLink());
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            context.startActivity(shareIntent);
        });

        holder.Like.setOnClickListener(v -> {
            IsLiked = true;
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                    if (IsLiked){
                        if(snapshot.child(PostId).hasChild(user.getUid())){
                           likesRef.child(PostId).child(user.getUid()).removeValue();
                           holder.Like.setImageResource(R.drawable.ic_love);
                           IsLiked = false;
                        }else {
                            likesRef.child(PostId).child(user.getUid()).setValue(new Date().toString());
                            holder.Like.setImageResource(R.drawable.ic_heart_1_);
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



    @Override
    public int getItemCount() {
        return EventsList.size();
    }

    public class EventsViewHolder extends RecyclerView.ViewHolder{

        TextView Title,Decs,time,clubName,likesCount,CommentsCount;
        ImageView ImageView,ClubImg;
        ImageButton Like,Comment,Share;
        CardView PostCard;

        public EventsViewHolder(@NonNull View itemView) {
            super(itemView);
            Title = itemView.findViewById(R.id.event_title);
            Decs =itemView.findViewById(R.id.event_decs);
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
        }

        public void setLikesButtonStatus(final String postId){
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                    if(snapshot.child(postId).hasChild(user.getUid())){
                        String LikesCount = String.valueOf((int) snapshot.child(postId).getChildrenCount());
                        likesCount.setText(LikesCount);
                        Like.setImageResource(R.drawable.ic_heart_1_);
                    }else {
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

    public void openCommentsActivity(String PostId,String Desc){
        Intent intent = new Intent(context,Comments.class);
        intent.putExtra("PostId",PostId);
        intent.putExtra("Desc",Desc);
        context.startActivity(intent);
    }

    public void ShowImage(Bitmap bitmap){
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.image_dialog);

        ImageView imageView = dialog.findViewById(R.id.image);
        ImageButton CloseButton = dialog.findViewById(R.id.close);
        CloseButton.setOnClickListener(v -> dialog.dismiss());
        imageView.setImageBitmap(bitmap);

        dialog.show();

    }



}
