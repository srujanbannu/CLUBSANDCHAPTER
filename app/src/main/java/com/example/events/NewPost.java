package com.example.events;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.type.DateTime;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class NewPost extends AppCompatActivity {

    private TextInputEditText Title_ET,Desc_ET;
    private ImageView PostImage;
    private Uri uri = null;
    private String ImageUrl = null,PostId;
    private ProgressDialog PD;
    private FirebaseFirestore db;
    private DocumentReference PostKey;
    private String UserId;
    private LinearLayout eventsLayout;
    private EditText RegistrationLink;
    private Button EndTime;
    private MaterialCheckBox eventCheck;
    private boolean event = false;
    private MaterialDatePicker materialDatePicker;
    private TimePickerDialog mTimePicker;
    private Calendar date;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        PD = new ProgressDialog(this);
        PostImage = findViewById(R.id.post_img);
        LinearLayout upload_img = findViewById(R.id.upload_img);
        Title_ET = findViewById(R.id.title_et);
        Desc_ET = findViewById(R.id.desc_et);
        MaterialCardView post_bt = findViewById(R.id.post_bt);
        db = FirebaseFirestore.getInstance();
        eventsLayout = findViewById(R.id.event_layout);
        eventCheck = findViewById(R.id.event_checkbox);
        RegistrationLink = findViewById(R.id.registration_link);
        EndTime = findViewById(R.id.end_time);
        eventsLayout.setVisibility(View.GONE);

        eventCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    eventsLayout.setVisibility(View.VISIBLE);
                    event = true;
                }else {
                    eventsLayout.setVisibility(View.GONE);
                    event = false;
                }
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(NewPost.this);
        UserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (!preferences.getBoolean(UserId,false)) {
            checkUser();
        }

        upload_img.setOnClickListener(v -> ImagePicker());
        PostImage.setOnClickListener(v -> ImagePicker());


        CalendarConstraints calendarConstraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();
        final MaterialDatePicker.Builder datepicker = MaterialDatePicker.Builder.datePicker();
        materialDatePicker = datepicker
                .setCalendarConstraints(calendarConstraints)
                .build();


        EndTime.setOnClickListener(v -> {
                    showDateTimePicker(EndTime);
                });


        post_bt.setOnClickListener(v -> {
                if (Objects.requireNonNull(Title_ET.getText()).toString().isEmpty() || Objects.requireNonNull(Desc_ET.getText()).toString().isEmpty()){
                    Toast.makeText(this, "Title and Description is need", Toast.LENGTH_SHORT).show();
                }else if(eventCheck.isChecked()) {
                    if (EndTime.getText().toString().isEmpty() || RegistrationLink.getText().toString().isEmpty()){
                        Toast.makeText(NewPost.this, "Enter Proper Registration link or Time", Toast.LENGTH_SHORT).show();
                    }else {
                        try {
                            String url = " ";
                            url = extractUrls(RegistrationLink.getText().toString());
                            if (!url.trim().isEmpty()) {
                                PostKey = db.collection("Posts").document();
                                PostId = PostKey.getId();
                                updatePost();
                            }else Toast.makeText(NewPost.this, "Enter Proper Registration link", Toast.LENGTH_SHORT).show();
                        }catch (Exception e){
                                Toast.makeText(NewPost.this, "Enter Proper Registration link", Toast.LENGTH_SHORT).show();
                        }
                        }
                    } else {
                    PostKey = db.collection("Posts").document();
                    PostId = PostKey.getId();
                    updatePost();
                }

        });


    }


    private void updatePost() {
        PD.setMessage("Posting...");
        PD.show();
        if (uri!=null){
            UploadImage();
        }else UploadNewpost(false);

    }

    private void UploadNewpost(boolean withImg) {

        PD.setMessage("Posting...");
        Map<String, Object> Post = new HashMap<>();
        if (withImg){
            Post.put("ImgUrl",ImageUrl);
        }else Post.put("ImgUrl","");

        if (event){
            Post.put("Event",true);
            Post.put("RegistrationLink",extractUrls(RegistrationLink.getText().toString()));
            Post.put("RegEndTime",FormatDate(EndTime.getText().toString()));
        }else {
            Post.put("Event",false);
            Post.put("RegistrationLink",null);
            Post.put("EndTime",null);
        }

        Post.put("Title", Objects.requireNonNull(Title_ET.getText()).toString().trim());
        Post.put("Desc", Objects.requireNonNull(Desc_ET.getText()).toString().trim());
        Post.put("UserId", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid() );
        Post.put("Time", FieldValue.serverTimestamp());

        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://vitapclubs.web.app/"+PostId))
                .setDomainUriPrefix("https://vitapclubs.page.link/")
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder("com.example.events")
                                .build())
                .buildShortDynamicLink()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Uri shortLink = task.getResult().getShortLink();
                        assert shortLink != null;
                        Post.put("dynamicLink",shortLink.toString());
                        PostKey.set(Post)
                                .addOnSuccessListener(aVoid -> {
                                    PD.dismiss();
                                    startActivity(new Intent(NewPost.this,MainActivity.class));

                                })
                                .addOnFailureListener(e -> {
                                    PD.dismiss();
                                    Toast.makeText(NewPost.this, "Error : "+e, Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e("Dynamic link error : ",task.getException().toString());
                        Toast.makeText(this, ""+task.getException(), Toast.LENGTH_LONG).show();
                    }



                });


    }

    private void UploadImage() {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        PD.setMessage("Uploading...");
        PostImage.setDrawingCacheEnabled(true);
        PostImage.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) PostImage.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference storageRef = storage.getReference().child("Posts/");
        StorageReference mountainsRef = storageRef.child(PostId);
        UploadTask uploadTask = mountainsRef.putBytes(data);
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            PD.setMessage("Upload is " + progress + "% done");
        });
        uploadTask.addOnFailureListener(exception -> {
            PD.dismiss();
            Toast.makeText(NewPost.this, "Error : "+exception, Toast.LENGTH_SHORT).show();
        }).addOnSuccessListener(taskSnapshot -> {
            mountainsRef.getDownloadUrl().addOnSuccessListener(uri -> {
                ImageUrl = uri.toString();
                UploadNewpost(true);
            });

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            assert data != null;
            uri = data.getData();
            if (requestCode == 0) {
                PostImage.setImageURI(uri);
            }

        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show();
        }

    }

    public void ImagePicker(){
        ImagePicker.with(NewPost.this)
                .crop(3,2)
                .galleryOnly()
                .compress(150)
                .start(0);
    }

    public void checkUser(){
        db.collection("Clubs")
                .document(UserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.getResult().exists()){
                        PreferenceManager.getDefaultSharedPreferences(NewPost.this).edit().putBoolean(UserId,false).apply();
                        startActivity(new Intent(NewPost.this,UpdateProfile.class));
                    }else {
                        PreferenceManager.getDefaultSharedPreferences(NewPost.this).edit().putBoolean(UserId,true).apply();
                    }
                });

    }

    public void showDateTimePicker(Button editText) {
        final Calendar currentDate = Calendar.getInstance();
        date = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                date.set(year, monthOfYear, dayOfMonth);
                new TimePickerDialog(NewPost.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        date.set(Calendar.MINUTE, minute);
                        Log.v("date", "The choosen one " + date.getTime());
                        editText.setText(date.getTime().toString());
                    }
                }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }


    public Date FormatDate(String dtStart){
        Date date = null;
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzz yyyy");
        try {
            date = format.parse(dtStart);
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("Time error ",e.toString());
        }
        return date;
    }

    public static String extractUrls(String input)
    {
        String result = null;

        String[] words = input.split("\\s+");
        Pattern pattern = Patterns.WEB_URL;
        for(String word : words) {
            if(pattern.matcher(word).find()) {
                if(!word.toLowerCase().contains("http://") && !word.toLowerCase().contains("https://")) {
                    word = "http://" + word;
                }
                result = word;
            }
        }

        return result;
    }
}