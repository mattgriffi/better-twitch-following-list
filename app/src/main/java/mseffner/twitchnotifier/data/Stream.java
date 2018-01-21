package mseffner.twitchnotifier.data;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Stream {

    public static final String STREAM_TYPE_LIVE = "live";

    private long id;
    private String currentGame;
    private int currentViewers;
    private String status;
    private int streamType;
    private long createdAt;

    public Stream(long channelId, String currentGame, int currentViewers, String status, long createdAt,
                  int streamType) {
        this(channelId, currentGame, currentViewers, status);
        this.createdAt = createdAt;
        this.streamType = streamType;
    }

    public Stream(long channelId, String currentGame, int currentViewers, String status, String createdAt,
                  String streamType) {
        this(channelId, currentGame, currentViewers, status);
        this.createdAt = getUnixTimestampFromUTC(createdAt);

        int streamTypeInt;
        switch (streamType) {
            // If the channel is offline, then a Stream object will never be created, so it's either
            // live or it's a rerun.
            case STREAM_TYPE_LIVE:
                streamTypeInt = ChannelContract.ChannelEntry.STREAM_TYPE_LIVE;
                break;
            default:
                streamTypeInt = ChannelContract.ChannelEntry.STREAM_TYPE_RERUN;
                break;
        }
        this.streamType = streamTypeInt;
    }

    private Stream(long channelId, String currentGame, int currentViewers, String status) {
        this.id = channelId;
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

    public long getChannelId() {
        return id;
    }

    private static long getUnixTimestampFromUTC(String utcFormattedTimestamp) {
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
