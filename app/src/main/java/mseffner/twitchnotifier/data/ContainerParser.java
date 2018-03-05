package mseffner.twitchnotifier.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mseffner.twitchnotifier.data.ChannelContract.StreamEntry;
import mseffner.twitchnotifier.networking.Containers;


public class ContainerParser {

    private static final String TAG = ContainerParser.class.getSimpleName();

    private Containers.Follows follows;
    private Containers.Users users;
    private Containers.Games games;
    private Containers.Streams streams;
    private List<ListEntry> channelList = new ArrayList<>();
    private boolean dataComplete = false;

    public void setStreams(Containers.Streams streams) {
        this.streams = streams;
    }

    public void setGames(Containers.Games games) {
        this.games = games;
        parseData();
    }

    public void setUsers(Containers.Users users) {
        this.users = users;
        parseData();
    }

    public void setFollows(Containers.Follows follows) {
        this.follows = follows;
    }

    public int getTotalFollows() {
        if (follows == null)
            return Integer.MAX_VALUE;
        return follows.total;
    }

    public String getFollowsCursor() {
        if (follows == null)
            return null;
        return follows.pagination.cursor;
    }

    public int getFollowsDataSize() {
        return follows.data.size();
    }

    public boolean isDataComplete() {
        return dataComplete;
    }

    public List<ListEntry> getChannelList() {
        return channelList;
    }

    public long[] getGameIdsFromStreams() {
        int size = streams.data.size();
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            String id = streams.data.get(i).game_id;
            ids[i] = id.isEmpty() ? 0L : Long.parseLong(id);
        }
        return ids;
    }

    public long[] getUserIdsFromStreams() {
        int size = streams.data.size();
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            String id = streams.data.get(i).user_id;
            ids[i] = id.isEmpty() ? 0L : Long.parseLong(id);
        }
        return ids;
    }

    public long[] getUserIdsFromFollows() {
        int size = follows.data.size();
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            String id = follows.data.get(i).to_id;
            ids[i] = id.isEmpty() ? 0L : Long.parseLong(id);
        }
        return ids;
    }

    public long[] getUserIdsFromUsers() {
        int size = users.data.size();
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            String id = users.data.get(i).id;
            ids[i] = id.isEmpty() ? 0L : Long.parseLong(id);
        }
        return ids;
    }

    private void parseData() {
        // Only run if all of the requests have completed
        if (users == null || games == null || streams == null) return;

        // Get map of game id -> Games.Data object
        Map<String, Containers.Games.Data> gameMap = new HashMap<>();
        for (Containers.Games.Data data : games.data)
            gameMap.put(data.id, data);

        // Get map of user id -> Users.Data object
        Map<String, Containers.Users.Data> userMap = new HashMap<>();
        for (Containers.Users.Data data : users.data)
            userMap.put(data.id, data);

        // Build the list
        for (Containers.Streams.Data data : streams.data) {
            // Build ListEntry object
            String userId = data.user_id;
            long id = Long.parseLong(userId);
            int pinned = ChannelContract.ChannelEntry.IS_NOT_PINNED;
            String login = userMap.get(userId).login;
            String displayName = ignoreNullPointerException(() -> userMap.get(userId).display_name);
            String profileImageUrl = userMap.get(userId).profile_image_url;
            int type = data.type.equals("live") ? StreamEntry.STREAM_TYPE_LIVE : StreamEntry.STREAM_TYPE_RERUN;
            String title = data.title;
            int viewerCount = Integer.parseInt(data.viewer_count);
            long startedAt = ChannelDb.getUnixTimestampFromUTC(data.started_at);
            String language = data.language;
            String thumbnailUrl = data.thumbnail_url;
            String gameName = ignoreNullPointerException(() -> gameMap.get(data.game_id).name);
            String boxArtUrl = gameMap.get(data.game_id).box_art_url;

            ListEntry listEntry = new ListEntry(id, pinned, login, displayName, profileImageUrl, type,
                    title, viewerCount, startedAt, language, thumbnailUrl, gameName, boxArtUrl);
            channelList.add(listEntry);
        }
        dataComplete = true;
    }

    private static String ignoreNullPointerException(RunnableFunc r) {
        // NullPointerException can happen if the API doesn't give me valid
        // results for the requested data, so ignore those and display a default
        try {
            return r.run();
        } catch (NullPointerException e) {
            return "";
        }
    }

    public interface RunnableFunc {
        String run() throws NullPointerException;
    }
}
