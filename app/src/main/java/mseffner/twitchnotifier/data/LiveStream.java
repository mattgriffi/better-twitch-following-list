package mseffner.twitchnotifier.data;

public class LiveStream {

    public static final String STREAM_TYPE_LIVE = "live";
    public static final String STREAM_TYPE_PLAYLIST = "playlist";
    public static final String STREAM_TYPE_VODCAST = "watch_party";

    private String currentGame;
    private int currentViewers;
    private String status;
    private String streamType;
    private String createdAt;

    public LiveStream(String currentGame, int currentViewers, String status, String createdAt, String streamType) {
        this.currentGame = currentGame;
        this.currentViewers = currentViewers;
        this.status = status;
        this.createdAt = createdAt;
        this.streamType = streamType;
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

    public String getCreatedAt() {
        return createdAt;
    }
    public String getStreamType() {
        return streamType;
    }
}
