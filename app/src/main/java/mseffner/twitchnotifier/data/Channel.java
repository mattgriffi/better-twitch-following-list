package mseffner.twitchnotifier.data;


import android.graphics.Bitmap;
import android.support.annotation.NonNull;

public class Channel implements Comparable<Channel> {

    private int id;
    private String displayName;
    private String name;
    private String logoUrl;
    private String streamUrl;
    private Bitmap bmp;
    private int pinned;

    private Stream stream;

    public Channel(int id, String displayName, String name, String logoUrl, String streamUrl) {
        this.id = id;
        this.displayName = displayName;
        this.name = name;
        this.logoUrl = logoUrl;
        this.streamUrl = streamUrl;
        this.bmp = null;
        this.stream = null;
        this.pinned = ChannelContract.ChannelEntry.IS_NOT_PINNED;
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

    public void setLogoBmp(Bitmap bmp) {
        this.bmp = bmp;
    }

    public Bitmap getLogoBmp() {
        return bmp;
    }

    public int getId() {
        return id;
    }

    public int getPinned() {
        return pinned;
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