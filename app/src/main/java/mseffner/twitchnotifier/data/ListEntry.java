package mseffner.twitchnotifier.data;


import mseffner.twitchnotifier.data.ChannelContract.FollowEntry;
import mseffner.twitchnotifier.networking.URLTools;

public class ListEntry {

    public long id;
    public boolean pinned;
    public String login;
    public String displayName;
    public String profileImageUrl;
    public int type;
    public String title;
    public int viewerCount;
    public long startedAt;
    public String language;
    public String thumbnailUrl;
    public String gameName;
    public String boxArtUrl;
    public String streamUrl;

    public ListEntry(long id, int pinned, String login, String displayName, String profileImageUrl,
                     int type, String title, int viewerCount, long startedAt, String language,
                     String thumbnailUrl, String gameName, String boxArtUrl) {
        this.id = id;
        this.pinned = pinned == FollowEntry.IS_PINNED;
        this.login = login;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.type = type;
        this.title = title;
        this.viewerCount = viewerCount;
        this.startedAt = startedAt;
        this.language = language;
        this.thumbnailUrl = thumbnailUrl;
        this.gameName = gameName;
        this.boxArtUrl = boxArtUrl;
        this.streamUrl = URLTools.getStreamUrl(this.login);
    }

    public void togglePinned() {
        pinned = !pinned;
    }

    @Override
    public String toString() {
        return "id: " + id +
                "\tpinned: " + pinned +
                "\tlogin: " + login +
                "\tdisplayName: " + displayName +
                "\tprofileImageUrl: " + profileImageUrl +
                "\ttype: " + type +
                "\ttitle: " + title +
                "\tviewerCount: " + viewerCount +
                "\tstartedAt: " + startedAt +
                "\tlanguage: " + language +
                "\tthumbnailUrl: " + thumbnailUrl +
                "\tgameName: " + gameName +
                "\tboxArtUrl: " + boxArtUrl +
                "\tstreamUrl: " + streamUrl + "\n";
    }
}