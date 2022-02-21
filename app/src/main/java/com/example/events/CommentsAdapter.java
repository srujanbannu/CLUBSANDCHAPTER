package com.example.events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import org.jetbrains.annotations.NotNull;
import org.ocpsoft.prettytime.PrettyTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.commentsViewHolder> {

    ArrayList<commentsUtils> commentsList;
    Context context;
    private TextView Name;
    private ImageView UserProfileImg;

    CommentsAdapter(ArrayList<commentsUtils> list,Context context){
        this.commentsList = list;
        this.context = context;
    }

    @NonNull
    @NotNull
    @Override
    public commentsViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comments_view,parent,false);
        return new commentsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull CommentsAdapter.commentsViewHolder holder, int position) {

        holder.Comment.setText(commentsList.get(position).getComment());

        PrettyTime prettyTime = new PrettyTime(Locale.getDefault());
        Date date = new Date(commentsList.get(position).getTime());
        String ago = prettyTime.format(date);
        holder.Time.setText(ago);

        Name = holder.Name;
        UserProfileImg = holder.UserProfileImg;

        FirebaseFirestore.getInstance().collection("Clubs")
                .document(commentsList.get(position).getUserId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        if(task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            String name = Objects.requireNonNull(document.get("Name")).toString();
                            String url = Objects.requireNonNull(document.get("ProfileImgUrl")).toString();
                            Name.setText(name);
                            Picasso.get()
                                    .load(url)
                                    .into(UserProfileImg);
                        }else{
                            FirebaseFirestore.getInstance().collection("Users")
                                    .document(commentsList.get(position).getUserId())
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        DocumentSnapshot document = task1.getResult();
                                        String name = Objects.requireNonNull(document.get("Name")).toString();
                                        String url = Objects.requireNonNull(document.get("ProfileImgUrl")).toString();
                                        Name.setText(name);
                                        Picasso.get()
                                                .load(url)
                                                .into(UserProfileImg);
                                    });
                        }
                    }else Toast.makeText(context, "error : "+task.getException(), Toast.LENGTH_SHORT).show();
                });




    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    public class commentsViewHolder extends RecyclerView.ViewHolder {
        ImageView UserProfileImg;
        TextView Name,Comment,Time;
        public commentsViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            UserProfileImg = itemView.findViewById(R.id.user_prof_img);
            Name = itemView.findViewById(R.id.user_name);
            Comment = itemView.findViewById(R.id.user_comment);
            Time = itemView.findViewById(R.id.comment_time);

        }
    }



}
