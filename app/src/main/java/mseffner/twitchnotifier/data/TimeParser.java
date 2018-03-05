package mseffner.twitchnotifier.data;


import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class TimeParser {

    private TimeParser() {}

    public static long getUnixTimestampFromUTC(String utcFormattedTimestamp) {
        @SuppressLint("SimpleDateFormat")
        // This is the format returned by the Twitch API
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return format.parse(utcFormattedTimestamp).getTime() / 1000;
        } catch (ParseException e) {
            return 0;
        }
    }
}
