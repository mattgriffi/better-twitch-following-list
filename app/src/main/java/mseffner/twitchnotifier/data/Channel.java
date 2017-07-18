package mseffner.twitchnotifier.data;


import android.support.annotation.NonNull;

public class Channel implements Comparable<Channel> {

    private String displayName;
    private String name;
    private String logoUrl;
    private String streamUrl;

    private LiveStream stream;

    public Channel(String displayName, String name, String logoUrl, String streamUrl) {
        this.displayName = displayName;
        this.name = name;
        this.logoUrl = logoUrl;
        this.streamUrl = streamUrl;
        this.stream = null;
    }

    public void setStream(LiveStream stream) {
        this.stream = stream;
    }

    public LiveStream getStream() {
        return stream;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    @Override
    public int compareTo(@NonNull Channel channel) {

        // Sort live channels by viewers
        if (this.stream != null && channel.stream != null)
            return this.stream.getCurrentViewers() - channel.stream.getCurrentViewers();
        // Live channels greater than offline
        else if (this.stream != null)
            return 1;
        else
            return -1;

    }
}