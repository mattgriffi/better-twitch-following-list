package mseffner.twitchnotifier.data;


import com.android.volley.Response;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.events.FollowsUpdateStartedEvent;
import mseffner.twitchnotifier.events.StreamsUpdateStartedEvent;
import mseffner.twitchnotifier.events.StreamsUpdatedEvent;
import mseffner.twitchnotifier.events.TopListUpdateStartedEvent;
import mseffner.twitchnotifier.events.TopStreamsUpdatedEvent;
import mseffner.twitchnotifier.events.UserIdUpdatedEvent;
import mseffner.twitchnotifier.networking.Containers;
import mseffner.twitchnotifier.networking.RequestTracker;
import mseffner.twitchnotifier.networking.Requests;
import mseffner.twitchnotifier.networking.URLTools;
import mseffner.twitchnotifier.settings.SettingsManager;

/**
 * This class provides methods to update data via asynchronous network
 * and database operations. It also defines interfaces for listeners and
 * will notify listeners when the requests are completed.
 */
public class DataUpdateManager {

    private static final int UPDATE_TYPE_FOLLOWS = 0;
    private static final int UPDATE_TYPE_TOP_STREAMS = 1;
    /* Limit the max number of follows to fetch, since more than 25
    requests would risk hitting the rate limit. */
    private static final int MAX_FOLLOW_COUNT = 25;

    private static int remainingFollowsRequests;
    private static int remainingStreamsRequests;
    private static int remainingGamesRequests;

    private static int followsFetched = 0;

    private static boolean followsUpdateInProgress = false;
    private static boolean streamsUpdateInProgress = false;
    private static boolean topStreamsGamesUpdateInProgress = false;
    private static boolean topStreamsUsersUpdateInProgress = false;

    private DataUpdateManager() {}

    /**
     * @return whether or not there is an update in progress
     */
    public static boolean updateInProgress() {
        return followsUpdateInProgress || streamsUpdateInProgress;
    }

    /**
     * Updates the stored user id.
     */
    public static void updateUserId() {
        Requests.makeRequest(Requests.REQUEST_TYPE_USERS, URLTools.getUserIdUrl(), new UserIdListener());
    }

    /**
     * Updates the stored user id then notifies listeners.
     */
    private static class UserIdListener implements Response.Listener<Containers.Users> {
        @Override
        public void onResponse(Containers.Users response) {
            RequestTracker.decrementUsers();
            if (response.data.isEmpty()) {
                ToastMaker.makeToastLong(ToastMaker.MESSAGE_INVALID_USERNAME);
                return;
            }
            ToastMaker.makeToastLong(ToastMaker.MESSAGE_USERNAME_CHANGE);
            SettingsManager.setUsernameId(Long.parseLong(response.data.get(0).id));
            EventBus.getDefault().post(new UserIdUpdatedEvent());
        }
    }

    /**
     * Updates the follows table, removing any rows that no longer appear in the
     * follows response, then updates the users table if necessary.
     */
    public static void updateFollowsData() {
        if (!SettingsManager.validUsername())
            return;

        followsUpdateInProgress = true;
        remainingFollowsRequests = 0;
        followsFetched = 0;
        ChannelDb.setFollowsDirty();
        EventBus.getDefault().post(new FollowsUpdateStartedEvent());
        ThreadManager.post(DataUpdateManager::performFollowsUpdate);
    }

    /**
     * Updates the follows and users data. This method should NOT be called on the main thread.
     */
    private static void performFollowsUpdate() {
        Requests.getFollows(null, new FollowsListener());
    }

    /**
     * Inserts the follows data into the database, then either starts the next request
     * if there is more follows data to fetch, or cleans the follows table and starts
     * the users data update if all of the follows data has been updated.
     */
    private static class FollowsListener implements Response.Listener<Containers.Follows> {

        @Override
        public void onResponse(Containers.Follows followsResponse) {
            RequestTracker.decrementFollows();
            // Track progress of the follows requests
            if (remainingFollowsRequests == 0)  // This is the first request
                remainingFollowsRequests = (int) Math.ceil(followsResponse.total / 100.0);
            remainingFollowsRequests--;
            followsFetched++;

            // Fetch more if we haven't reached the limit yet
            if (followsFetched < MAX_FOLLOW_COUNT) {
                ThreadManager.post(() -> {
                    ChannelDb.insertFollowsData(followsResponse);
                    if (remainingFollowsRequests > 0)  // There is still more to fetch
                        Requests.getFollows(followsResponse.pagination.cursor, new FollowsListener());
                    else {  // We are done, clean follows and get the users data
                        followsUpdateComplete();
                    }
                });
            } else {  // If we hit the limit, call it quits
                ThreadManager.post(() -> {
                    ToastMaker.makeToastLong(ToastMaker.MESSAGE_TOO_MANY_FOLLOWS);
                    followsUpdateComplete();
                });
            }
        }
    }

    /**
     * Requests the users data for any user id in the follows table that is not
     * already in the users table.
     */
    private static void updateUsersData() {
        long[][] userIds = URLTools.splitIdArray(ChannelDb.getUnknownUserIds());
        for (long[] ids : userIds)
            Requests.getUsers(ids, new UsersListener());
    }

    /**
     * Inserts the users data into the database.
     */
    private static class UsersListener implements Response.Listener<Containers.Users> {
        @Override
        public void onResponse(Containers.Users response) {
            ThreadManager.post(() -> {
                ChannelDb.insertUsersData(response);
                RequestTracker.decrementUsers();
            });
        }
    }

    /**
     * Updates the streams table then updates the games table if necessary.
     */
    public static void updateStreamsData() {
        streamsUpdateInProgress = true;
        remainingStreamsRequests = 0;
        remainingGamesRequests = 0;
        EventBus.getDefault().post(new StreamsUpdateStartedEvent());
        ThreadManager.post(DataUpdateManager::performStreamsUpdate);
    }

    /**
     * Updates the streams and games data. This method should NOT be called on the main thread.
     */
    private static void performStreamsUpdate() {
        long[][] userIds = URLTools.splitIdArray(ChannelDb.getAllFollowIds());
        for (long[] ids : userIds)
            Requests.getStreams(ids, new StreamsListener());

        ThreadManager.post(DataUpdateManager::updateUsersData);
    }

    /**
     * Inserts the streams data into the database.
     */
    private static class StreamsListener implements Response.Listener<Containers.Streams> {
        @Override
        public void onResponse(Containers.Streams response) {
            ThreadManager.post(() -> {
                ChannelDb.insertStreamsData(response);
                RequestTracker.decrementStreams();
            });
        }
    }

    /**
     * Requests the games data for any games id in the streams table that is not
     * already in the games table.
     */
    private static void updateGamesData() {
        long[][] gameIds = URLTools.splitIdArray(ChannelDb.getUnknownGameIds());
        // 0 is a null game, so ignore that
        if (gameIds.length == 0 || (gameIds[0].length == 1 && gameIds[0][0] == 0)) return;
        for (long[] ids : gameIds)
            Requests.getGames(ids, new GamesListener());
    }

    /**
     * Inserts games data into database.
     */
    private static class GamesListener implements Response.Listener<Containers.Games> {
        @Override
        public void onResponse(Containers.Games response) {
            ThreadManager.post(() -> {
                ChannelDb.insertGamesData(response);
                RequestTracker.decrementGames();
            });
        }
    }

    public static void updateTopStreamsData() {
        if (topStreamsUsersUpdateInProgress || topStreamsGamesUpdateInProgress) return;
        topStreamsGamesUpdateInProgress = true;
        topStreamsUsersUpdateInProgress = true;
        // Get the top streams
        EventBus.getDefault().post(new TopListUpdateStartedEvent());
        Requests.getTopStreams(new StreamsListener());
    }

    private static synchronized void postTopStreamsUpdatedEvent() {
        if (topStreamsGamesUpdateInProgress || topStreamsUsersUpdateInProgress) return;
        EventBus.getDefault().post(new TopStreamsUpdatedEvent());
    }

    private static synchronized void followsUpdateComplete() {
        followsUpdateInProgress = false;
        SettingsManager.setFollowsNeedUpdate(false);
        ThreadManager.post(ChannelDb::cleanFollows);
        updateStreamsData();
    }

    private static synchronized void postFollowsStreamsUpdatedEvent() {
        streamsUpdateInProgress = false;
        EventBus.getDefault().post(new StreamsUpdatedEvent());
    }
}
