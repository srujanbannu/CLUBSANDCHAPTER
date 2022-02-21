package com.example.events;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.Locale;
// time formart



/// for telling it 3 mins ago in that way
public class PrettytimeFromat {

    public PrettytimeFromat() {
    }

    public String Ago(String time) {
        PrettyTime prettyTime = new PrettyTime(Locale.getDefault());
        Date date = new Date(time);
        return prettyTime.format(date);
    }



}
