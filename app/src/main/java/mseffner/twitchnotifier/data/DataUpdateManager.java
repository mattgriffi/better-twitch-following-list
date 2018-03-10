package mseffner.twitchnotifier.data;


import android.support.annotation.NonNull;

import com.android.volley.Response;

import java.util.List;

import mseffner.twitchnotifier.networking.Containers;
import mseffner.twitchnotifier.networking.ErrorHandler;
import mseffner.twitchnotifier.networking.Requests;
import mseffner.twitchnotifier.networking.URLTools;
import mseffner.twitchnotifier.settings.SettingsManager;

/**
 * This class provides methods to update data via asynchronous network
 * and database operations. It also defines interfaces for listeners and
 * will notify listeners when the requests are completed.
 */
public class DataUpdateManager {

    private static TopDataUpdatedListener topDataUpdatedListener;
    private static FollowsUpdateListener followsUpdateListener;
    private static StreamsUpdateListener streamsUpdateListener;
    private static Response.ErrorListener errorListener;

    private static int remainingFollowsRequests;
    private static int remainingUsersRequests;
    private static int remainingStreamsRequests;
    private static int remainingGamesRequests;

    private static boolean followsUpdateInProgress = false;
    private static boolean streamsUpdateInProgress = false;

    private DataUpdateManager() {}

    public interface TopDataUpdatedListener {
        void onTopStreamsResponse(@NonNull List<ListEntry> channels);
    }

    public interface FollowsUpdateListener {
        void onFollowsDataUpdated();
    }

    public interface StreamsUpdateListener {
        void onStreamsDataUpdated();
    }

    public static void registerFollowsUpdateListener(FollowsUpdateListener listener) {
        followsUpdateListener = listener;
    }

    public static void unregisterFollowsUpdateListener() {
        followsUpdateListener = null;
    }

    public static void registerStreamsUpdateListener(StreamsUpdateListener listener) {
        streamsUpdateListener = listener;
    }

    public static void unregisterStreamsUpdateListener() {
        streamsUpdateListener = null;
    }

    /**
     * Updates the follows table, removing any rows that no longer appear in the
     * follows response, then updates the users table if necessary.
     *
     * @param errorListener notified if any network operation goes wrong
     */
    public static void updateFollowsData(final Response.ErrorListener errorListener) {
        DataUpdateManager.errorListener = errorListener;

        if (followsUpdateInProgress || streamsUpdateInProgress || !SettingsManager.validUsername())
            return;

        followsUpdateInProgress = true;
        remainingFollowsRequests = 0;
        remainingUsersRequests = 0;
        ChannelDb.setFollowsDirty();
        ThreadManager.post(DataUpdateManager::performFollowsUpdate);
    }

    /**
     * Updates the follows and users data. This method should NOT be called on the main thread.
     */
    private static void performFollowsUpdate() {
        Requests.getFollows(null, new FollowsListener(), errorListener);
    }

    /**
     * Inserts the follows data into the database, then either starts the next request
     * if there is more follows data to fetch, or cleans the follows table and starts
     * the users data update if all of the follows data has been updated.
     */
    private static class FollowsListener implements Response.Listener<Containers.Follows> {

        @Override
        public void onResponse(Containers.Follows followsResponse) {
            ThreadManager.post(() -> {
                // Track progress of the follows requests
                if (remainingFollowsRequests == 0)  // This is the first request
                    remainingFollowsRequests = (int) Math.ceil(followsResponse.total / 100.0);
                remainingFollowsRequests--;

                // Insert into database
                ChannelDb.insertFollowsData(followsResponse);

                if (remainingFollowsRequests > 0)  // There is still more to fetch
                    Requests.getFollows(followsResponse.pagination.cursor, new FollowsListener(), errorListener);
                else {  // We are done, clean follows and get the users data
                    ChannelDb.cleanFollows();
                    updateUsersData();
                }
            });
        }

    }

    /**
     * Requests the users data for any user id in the follows table that is not
     * already in the users table. Notifies the listener if there is no new
     * users data to fetch.
     */
    private static void updateUsersData() {
        long[][] userIds = URLTools.splitIdArray(ChannelDb.getUnknownUserIds());
        if (userIds.length == 0) {
            notifyFollowsListener();
        } else {
            remainingUsersRequests = userIds.length;
            for (long[] ids : userIds)
                Requests.getUsers(ids, new UsersListener(), new ErrorHandler() {});
        }
    }

    /**
     * Inserts the users data into the database and notifies the follows listener
     * if all of the users requests have completed.
     */
    private static class UsersListener implements Response.Listener<Containers.Users> {

        @Override
        public void onResponse(Containers.Users response) {
            ThreadManager.post(() -> {
                remainingUsersRequests--;
                ChannelDb.insertUsersData(response);
                if (remainingUsersRequests == 0)
                    notifyFollowsListener();
            });
        }

    }

    /**
     * Updates the streams table then updates the games table if necessary.
     *
     * @param errorListener notified if any network operation goes wrong
     */
    public static void updateStreamsData(final Response.ErrorListener errorListener) {
        DataUpdateManager.errorListener = errorListener;

        // If an update is already in progress, do nothing
        if (streamsUpdateInProgress || followsUpdateInProgress) return;

        streamsUpdateInProgress = true;
        remainingStreamsRequests = 0;
        remainingGamesRequests = 0;
        ThreadManager.post(DataUpdateManager::performStreamsUpdate);
    }

    /**
     * Updates the streams and games data. This method should NOT be called on the main thread.
     */
    private static void performStreamsUpdate() {
        ChannelDb.deleteAllStreams();
        long[][] userIds = URLTools.splitIdArray(ChannelDb.getAllFollowIds());
        remainingStreamsRequests = userIds.length;
        for (long[] ids : userIds)
            Requests.getStreams(ids, new StreamsListener(), errorListener);
    }

    /**
     * Inserts the streams data into the database, then starts the games update after
     * all of the streams data has been updated.
     */
    private static class StreamsListener implements Response.Listener<Containers.Streams> {
        @Override
        public void onResponse(Containers.Streams response) {
            ThreadManager.post(() -> {
                remainingStreamsRequests--;
                ChannelDb.insertStreamsData(response);
                if (remainingStreamsRequests == 0)
                    updateGamesData();
            });
        }
    }

    /**
     * Requests the games data for any games id in the streams table that is not
     * already in the games table. Notifies the listener if there are no games
     * to fetch.
     */
    private static void updateGamesData() {
        long[][] gameIds = URLTools.splitIdArray(ChannelDb.getUnknownGameIds());
        // 0 indicates a null game, so ignore that
        if (gameIds.length == 0 || (gameIds[0].length == 1 && gameIds[0][0] == 0))  {
            notifyStreamsListener();
        } else {
            remainingGamesRequests = gameIds.length;
            for (long[] ids : gameIds)
                Requests.getGames(ids, new GamesListener(), new ErrorHandler() {});
        }
    }

    /**
     * Inserts games data into database, then notifies the listener.
     */
    private static class GamesListener implements Response.Listener<Containers.Games> {
        @Override
        public void onResponse(Containers.Games response) {
            ThreadManager.post(() -> {
                remainingGamesRequests--;
                ChannelDb.insertGamesData(response);
                if (remainingGamesRequests == 0)
                    notifyStreamsListener();
            });
        }
    }

    public static void getTopStreamsData(@NonNull TopDataUpdatedListener listener,
                                         final Response.ErrorListener errorListener) {
        DataUpdateManager.topDataUpdatedListener = listener;
        ContainerParser parser = new ContainerParser();

        // Get the top streams
        Requests.getTopStreams(streamsResponse -> {
            parser.setStreams(streamsResponse);
            // Get the game names
            Requests.getGames(parser.getGameIdsFromStreams(),
                    gamesResponse -> {
                        ThreadManager.post(() -> ChannelDb.insertGamesData(gamesResponse));
                        parser.setGames(gamesResponse);
                        notifyListener(parser);
                    }, errorListener);
            // Get the streamer names
            Requests.getUsers(parser.getUserIdsFromStreams(),
                    usersResponse -> {
                        ThreadManager.post(() -> ChannelDb.insertUsersData(usersResponse));
                        parser.setUsers(usersResponse);
                        notifyListener(parser);
                    }, errorListener);
        }, errorListener);
    }

    private static synchronized void notifyListener(ContainerParser parser) {
        if (parser == null || !parser.isDataComplete() || topDataUpdatedListener == null) return;
        topDataUpdatedListener.onTopStreamsResponse(parser.getChannelList());
        topDataUpdatedListener = null;
    }

    private static synchronized  void notifyFollowsListener() {
        followsUpdateInProgress = false;
        if (followsUpdateListener != null)
            ThreadManager.postMainThread(() -> followsUpdateListener.onFollowsDataUpdated());
    }

    private static synchronized  void notifyStreamsListener() {
        streamsUpdateInProgress = false;
        if (streamsUpdateListener != null)
            ThreadManager.postMainThread(() -> streamsUpdateListener.onStreamsDataUpdated());
    }
}
