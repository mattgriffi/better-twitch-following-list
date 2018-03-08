package mseffner.twitchnotifier.data;


import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.Response;

import java.util.Arrays;
import java.util.List;

import mseffner.twitchnotifier.networking.Containers;
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
    private static int totalFollowsRequests;
    private static int totalStreamsRequests;
    private static int totalUsersRequests;
    private static int totalGamesRequests;
    private static int completedFollowsRequests;
    private static int completedStreamsRequests;
    private static int completedUsersRequests;
    private static int completedGamesRequests;

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
     * Updates the follows table and the streams, users, and games tables if
     * necessary.
     *
     *                  Flow Chart
     *
     * [follow request] -> [next follow request] -> [...]
     *      |        ----> [follows db insert]
     *      V                       |
     * [stream request]       (all complete)
     *      |                       |
     *      V                       V
     * [streams db insert]   [users requests]
     *      |                       |
     * (all complete)               V
     *      |                [users db insert]
     *      V                       |
     * [games requests]             |
     *      |                       |
     *      V                       |
     * [games db insert]            |
     *      |                       |
     *       -----(all complete)----
     *                  |
     *                  V
     *           [notify listener]
     *
     * @param errorListener notified if any network operation goes wrong
     */
    public static void updateFollowsData(final Response.ErrorListener errorListener) {
        DataUpdateManager.errorListener = errorListener;

        // Reset counters
        totalFollowsRequests = 0;
        totalStreamsRequests = 0;
        totalUsersRequests = 0;
        totalGamesRequests = 0;
        completedFollowsRequests = 0;
        completedStreamsRequests = 0;
        completedUsersRequests = 0;
        completedGamesRequests = 0;

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

    private static void performFollowsUpdate() {
        // Any rows that are not cleaned by the time we finish will be deleted as this
        // means that those channels were unfollowed
        // Note that channels will NOT be deleted if an error ends the update prematurely
        ChannelDb.setFollowsDirty();

        // Get all of the follows data in chunks of 100
        if (!SettingsManager.getUsername().equals(""))
            Requests.getFollows(null, new FollowsListener(), errorListener);
    }

    private static class FollowsListener implements Response.Listener<Containers.Follows> {
        @Override
        public void onResponse(Containers.Follows followsResponse) {
            ContainerParser parser = new ContainerParser();
            parser.setFollows(followsResponse);
            long[] userIds = parser.getUserIdsFromFollows();
            totalFollowsRequests = (int) Math.ceil(parser.getTotalFollows() / 100.0);

            // Insert data into database
            ThreadManager.post(() -> ChannelDb.insertFollowsData(followsResponse, new FollowsInsertListener()));

            // Get streams data
            Requests.getStreams(userIds, new StreamsListener(), errorListener);

            // If there are 100 elements, then there might be more to fetch
            if (parser.getFollowsDataSize() >= 100)
                Requests.getFollows(parser.getFollowsCursor(), new FollowsListener(), errorListener);
        }
    }

    private static class UsersListener implements Response.Listener<Containers.Users> {
        @Override
        public void onResponse(Containers.Users response) {
            ThreadManager.post(() -> ChannelDb.insertUsersData(response, new UsersInsertListener()));
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

    private static class FollowsInsertListener implements ChannelDb.InsertListener {
        @Override
        public void onInsertFinished() {
            completedFollowsRequests += 1;

            // If all of the follows data has been inserted, run the users requests
            if (completedFollowsRequests == totalFollowsRequests) {
                // If any follows rows haven't been updated, then they were unfollowed
                // and should be deleted
                ThreadManager.post(ChannelDb::cleanFollows);

                long[][] userIds = URLTools.splitIdArray(ChannelDb.getUnknownUserIds());
                totalUsersRequests = userIds.length;
                totalStreamsRequests = totalUsersRequests;
                for (long[] ids : userIds)
                    Requests.getUsers(ids, new UsersListener(), errorListener);
            }
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
                        ThreadManager.post(() -> ChannelDb.insertUsersData(usersResponse, null));
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
