package mseffner.twitchnotifier.data;


import android.support.annotation.NonNull;
import android.util.Log;

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
    private static DataUpdatedListener dataUpdatedListener;
    private static Response.ErrorListener errorListener;
    // Variables to track when sets of requests have finished
    private static int totalStreamsRequests;
    private static int totalUsersRequests;
    private static int totalGamesRequests;
    private static int completedStreamsRequests;
    private static int completedUsersRequests;
    private static int completedGamesRequests;

    private static int remainingFollowsRequests;
    private static int remainingUsersRequests;

    private DataUpdateManager() {}

    public interface TopDataUpdatedListener {
        void onTopStreamsResponse(@NonNull List<ListEntry> channels);
    }

    public interface DataUpdatedListener {
        void onFollowsDataUpdated();
    }

    public static void registerOnDataUpdatedListener(DataUpdatedListener listener) {
        dataUpdatedListener = listener;
    }

    public static void unregisterOnDataUpdatedListener() {
        dataUpdatedListener = null;
    }

    /**
     * Updates the follows table, removing any rows that no longer appear in the
     * follows response, then updates the users table if necessary.
     *
     * @param errorListener notified if any network operation goes wrong
     */
    public static void updateFollowsData(final Response.ErrorListener errorListener) {
        DataUpdateManager.errorListener = errorListener;

        // Reset counters
        totalStreamsRequests = 0;
        totalUsersRequests = 0;
        totalGamesRequests = 0;
        completedStreamsRequests = 0;
        completedUsersRequests = 0;
        completedGamesRequests = 0;

        remainingFollowsRequests = 0;
        remainingUsersRequests = 0;

        ThreadManager.post(DataUpdateManager::performFollowsUpdate);
    }

    public static void updateStreamsData(final Response.ErrorListener errorListener) {
        DataUpdateManager.errorListener = errorListener;

        // Reset counters
        totalStreamsRequests = 0;
        totalUsersRequests = 0;
        totalGamesRequests = 0;
        completedStreamsRequests = 0;
        completedUsersRequests = 0;
        completedGamesRequests = 0;

        ThreadManager.post(DataUpdateManager::performStreamsUpdate);
    }

    private static void performStreamsUpdate() {
        ChannelDb.deleteAllStreams();
        long[][] userIds = URLTools.splitIdArray(ChannelDb.getAllFollowIds());
        totalStreamsRequests = userIds.length;
        for (long[] ids : userIds)
            Requests.getStreams(ids, new StreamsListener(), errorListener);
    }

    /**
     * Updates the follows data. This method should NOT be called on the main thread.
     */
    private static void performFollowsUpdate() {
        // Mark rows to be deleted if they're not updated
        ChannelDb.setFollowsDirty();

        // If there is a valid username set, get their follows
        if (!SettingsManager.getUsername().equals(""))
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
        }
    }

    /**
     * Requests the users data for any user id in the follows table that is not
     * already in the users table.
     */
    private static void updateUsersData() {
        long[][] userIds = URLTools.splitIdArray(ChannelDb.getUnknownUserIds());
        if (userIds.length == 0) return;
        remainingUsersRequests = userIds.length;
        for (long[] ids : userIds)
            Requests.getUsers(ids, new UsersListener(), new ErrorHandler() {});
    }

    /**
     * Inserts the users data into the database and notifies the follows listener
     * if all of the users requests have completed.
     */
    private static class UsersListener implements Response.Listener<Containers.Users> {
        @Override
        public void onResponse(Containers.Users response) {
            remainingUsersRequests--;
            ChannelDb.insertUsersData(response);
            if (remainingUsersRequests == 0)
                notifyFollowsListener();
        }
    }

    private static class GamesListener implements Response.Listener<Containers.Games> {
        @Override
        public void onResponse(Containers.Games response) {
            ThreadManager.post(() -> ChannelDb.insertGamesData(response, new GamesInsertListener()));
        }
    }

    private static class StreamsListener implements Response.Listener<Containers.Streams> {
        @Override
        public void onResponse(Containers.Streams response) {
            ThreadManager.post(() -> ChannelDb.insertStreamsData(response, new StreamsInsertListener()));
        }
    }

    private static class StreamsInsertListener implements ChannelDb.InsertListener {
        @Override
        public void onInsertFinished() {
            completedStreamsRequests += 1;

            // If all of the streams data has been inserted, run the games requests
            if (completedStreamsRequests == totalStreamsRequests) {
                long[][] gameIds = URLTools.splitIdArray(ChannelDb.getUnknownGameIds());
                if (gameIds.length == 0) {
                    notifyFollowsListener();
                } else {
                    totalGamesRequests = gameIds.length;
                    for (long[] ids : gameIds)
                        Requests.getGames(ids, new GamesListener(), errorListener);
                }
            }
        }
    }

    private static class UsersInsertListener implements ChannelDb.InsertListener {
        @Override
        public void onInsertFinished() {
            completedUsersRequests += 1;
            // Notify the listener if all operations are complete
            if (completedUsersRequests == totalUsersRequests && completedGamesRequests == totalGamesRequests)
                notifyFollowsListener();
        }
    }

    private static class GamesInsertListener implements ChannelDb.InsertListener {
        @Override
        public void onInsertFinished() {
            completedGamesRequests += 1;
            // Notify the listener if all operations are complete
            if (completedUsersRequests == totalUsersRequests && completedGamesRequests == totalGamesRequests)
                notifyFollowsListener();
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
                        ThreadManager.post(() -> ChannelDb.insertGamesData(gamesResponse, null));
                        parser.setGames(gamesResponse);
                        notifyListener(parser);
                    }, errorListener);
            // Get the streamer names
            Requests.getUsers(parser.getUserIdsFromStreams(),
                    usersResponse -> {
//                        ThreadManager.post(() -> ChannelDb.insertUsersData(usersResponse, null));
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
        if (dataUpdatedListener != null)
            ThreadManager.postMainThread(() -> dataUpdatedListener.onFollowsDataUpdated());
    }
}
