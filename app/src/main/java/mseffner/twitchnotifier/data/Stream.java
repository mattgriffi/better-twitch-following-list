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

    public Stream(String currentGame, int currentViewers, String status, String createdAt, String streamType) {
        this.currentGame = currentGame;
        this.currentViewers = currentViewers;
        this.status = status;
        this.createdAt = getUnixTimestampFromUTC(createdAt);

        switch (streamType) {
            case STREAM_TYPE_LIVE:
                this.streamType = ChannelContract.ChannelEntry.STREAM_TYPE_LIVE;
                break;
            case STREAM_TYPE_PLAYLIST:
                this.streamType = ChannelContract.ChannelEntry.STREAM_TYPE_PLAYLIST;
                break;
            case STREAM_TYPE_VODCAST:
                this.streamType = ChannelContract.ChannelEntry.STREAM_TYPE_VODCAST;
                break;
        }
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

    private long getUnixTimestampFromUTC(String utcFormattedTimestamp) {
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
