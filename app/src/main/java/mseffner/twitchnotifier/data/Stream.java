package mseffner.twitchnotifier.data;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Stream {

    public static final String STREAM_TYPE_LIVE = "live";
    public static final String STREAM_TYPE_PLAYLIST = "playlist";
    public static final String STREAM_TYPE_VODCAST = "watch_party";

    private String currentGame;
    private int currentViewers;
    private String status;
    private int streamType;
    private long createdAt;

    public Stream(String currentGame, int currentViewers, String status, long createdAt, int streamType) {
        this(currentGame, currentViewers, status);
        this.createdAt = createdAt;
        this.streamType = streamType;
    }

    public Stream(String currentGame, int currentViewers, String status, String createdAt, String streamType) {
        this(currentGame, currentViewers, status);
        this.createdAt = getUnixTimestampFromUTC(createdAt);

        int streamTypeInt = ChannelContract.ChannelEntry.STREAM_TYPE_OFFLINE;
        switch (streamType) {
            case STREAM_TYPE_LIVE:
                streamTypeInt = ChannelContract.ChannelEntry.STREAM_TYPE_LIVE;
                break;
            case STREAM_TYPE_PLAYLIST:
                streamTypeInt = ChannelContract.ChannelEntry.STREAM_TYPE_PLAYLIST;
                break;
            case STREAM_TYPE_VODCAST:
                streamTypeInt = ChannelContract.ChannelEntry.STREAM_TYPE_VODCAST;
                break;
        }
        this.streamType = streamTypeInt;
    }

    private Stream(String currentGame, int currentViewers, String status) {
        this.currentGame = currentGame;
        this.currentViewers = currentViewers;
        this.status = status;
    }

    public String getCurrentGame() {
        return currentGame;
    }

    public int getCurrentViewers() {
        return currentViewers;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getStreamType() {
        return streamType;
    }

    private static long getUnixTimestampFromUTC(String utcFormattedTimestamp) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return format.parse(utcFormattedTimestamp).getTime() / 1000;
        } catch (ParseException e) {
            return 0;
        }
    }
}
