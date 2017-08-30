package mseffner.twitchnotifier.data;


import android.graphics.Bitmap;
import android.support.annotation.NonNull;

public class Channel implements Comparable<Channel> {

    private int id;
    private String displayName;
    private String name;
    private String logoUrl;
    private String streamUrl;
    private int pinned;

    private Stream stream;

    public Channel(int id, String displayName, String name, String logoUrl, String streamUrl, int pinned) {
        this.id = id;
        this.displayName = displayName;
        this.name = name;
        this.logoUrl = logoUrl;
        this.streamUrl = streamUrl;
        this.stream = null;
        this.pinned = pinned;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public Stream getStream() {
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

    public int getId() {
        return id;
    }

    public int getPinned() {
        return pinned;
    }

    public void togglePinned() {
        if (pinned == ChannelContract.ChannelEntry.IS_PINNED) {
            pinned = ChannelContract.ChannelEntry.IS_NOT_PINNED;
        } else {
            pinned = ChannelContract.ChannelEntry.IS_PINNED;
        }
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