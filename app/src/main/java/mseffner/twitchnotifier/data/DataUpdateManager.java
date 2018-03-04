package mseffner.twitchnotifier.data;


import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mseffner.twitchnotifier.networking.Containers;
import mseffner.twitchnotifier.networking.Requests;
import mseffner.twitchnotifier.networking.URLTools;

/**
 * This class provides methods to update data via asynchronous network
 * and database operations. It also defines interfaces for listeners and
 * will notify listeners when the requests are completed.
 */
public class DataUpdateManager {

    private static final String TAG = DataUpdateManager.class.getSimpleName();

    private static Containers.Streams streams;
    private static Containers.Users users;
    private static Containers.Games games;

    private static TopStreamsListener listener;

    public interface TopStreamsListener {
        void onTopStreamsResponse(@NonNull List<Channel> channels);
    }

    public static void getTopStreamsData(@NonNull TopStreamsListener listener,
                                         final Response.ErrorListener errorListener) {
        DataUpdateManager.listener = listener;

        // Get the top streams
        Requests.getTopStreams(streamsResponse -> {
            streams = streamsResponse;
            // Get the game names
            Requests.getGames(getGameIdsFromStreams(streamsResponse),
                    gamesResponse -> {
                        games = gamesResponse;
                        buildData();
                    }, errorListener);
            // Get the streamer names
            Requests.getUsers(getUserIdsFromStreams(streamsResponse),
                    usersResponse -> {
                        users = usersResponse;
                        buildData();
                    }, errorListener);
        }, errorListener);
    }

    private static synchronized void buildData() {
        // Only run if all of the requests have completed
        if (users == null || games == null || streams == null) return;
        Log.e(TAG, "buildData");

        // Get map of game id -> Games.Data object
        Map<String, Containers.Games.Data> gameMap = new HashMap<>();
        for (Containers.Games.Data data : games.data)
            gameMap.put(data.id, data);

        // Get map of user id -> Users.Data object
        Map<String, Containers.Users.Data> userMap = new HashMap<>();
        for (Containers.Users.Data data : users.data)
            userMap.put(data.id, data);

        // Build the list
        List<Channel> list = new ArrayList<>();
        for (Containers.Streams.Data data : streams.data) {
            // Build Channel object
            String userId = data.user_id;
            long id = Long.parseLong(userId);
            String displayName = ignoreNullPointerException(() -> userMap.get(userId).display_name);
            String logoUrl = userMap.get(userId).profile_image_url;
            String streamUrl = URLTools.getStreamUrl(userMap.get(userId).login);
            int pinned = ChannelContract.ChannelEntry.IS_NOT_PINNED;
            Channel channel = new Channel(id, displayName, logoUrl, streamUrl, pinned);

            // Build Stream object
            String currentGame = ignoreNullPointerException(() -> gameMap.get(data.game_id).name);
            int currentViewers = Integer.parseInt(data.viewer_count);
            String status = data.title;
            String createdAt = data.started_at;
            String streamType = data.type;
            Stream stream = new Stream(id, currentGame, currentViewers, status, createdAt, streamType);

            channel.setStream(stream);
            list.add(channel);
        }
        notifyListener(list);
    }

    private static synchronized void notifyListener(List<Channel> list) {
        Log.e(TAG, "notifyListener");
        users = null;
        games = null;
        streams = null;
        if (listener != null)
            listener.onTopStreamsResponse(list);
        listener = null;
    }

    private static long[] getGameIdsFromStreams(Containers.Streams streams) {
        int size = streams.data.size();
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            String id = streams.data.get(i).game_id;
            ids[i] = id.isEmpty() ? 0L : Long.parseLong(id);
        }
        return ids;
    }

    private static long[] getUserIdsFromStreams(Containers.Streams streams) {
        int size = streams.data.size();
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            String id = streams.data.get(i).user_id;
            ids[i] = id.isEmpty() ? 0L : Long.parseLong(id);
        }
        return ids;
    }

    private static String ignoreNullPointerException(RunnableFunc r) {
        // NullPointerException can happen if the API doesn't give me valid
        // results for the requested data, so ignore those and display a default
        try { return r.run(); } catch (NullPointerException e) { return ""; }
    }

    public interface RunnableFunc {
        String run() throws NullPointerException;
    }
}
