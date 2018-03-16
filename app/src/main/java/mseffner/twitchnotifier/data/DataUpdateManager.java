package mseffner.twitchnotifier.data;


import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.events.FollowsUpdateStartedEvent;
import mseffner.twitchnotifier.events.FollowsUpdatedEvent;
import mseffner.twitchnotifier.events.StreamsUpdateStartedEvent;
import mseffner.twitchnotifier.events.StreamsUpdatedEvent;
import mseffner.twitchnotifier.events.TopListUpdateStartedEvent;
import mseffner.twitchnotifier.events.TopStreamsUpdatedEvent;
import mseffner.twitchnotifier.events.UserIdUpdatedEvent;
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

    private static final int UPDATE_TYPE_FOLLOWS = 0;
    private static final int UPDATE_TYPE_TOP_STREAMS = 1;
    /* Limit the max number of follows to fetch, since more than 25
    requests would risk hitting the rate limit. */
    private static final int MAX_FOLLOW_COUNT = 25;

    private static Response.ErrorListener errorListener;

    private static int remainingFollowsRequests;
    private static int remainingUsersRequests;
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
        Requests.makeRequest(Requests.REQUEST_TYPE_USERS, URLTools.getUserIdUrl(),
                new UserIdListener(), ErrorHandler.getInstance());
    }

    /**
     * Updates the stored user id then notifies listeners.
     */
    private static class UserIdListener implements Response.Listener<Containers.Users> {
        @Override
        public void onResponse(Containers.Users response) {
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
     *
     * @param errorListener notified if any network operation goes wrong
     */
    public static void updateFollowsData(final Response.ErrorListener errorListener) {
        DataUpdateManager.errorListener = new ErrorWrapper(errorListener);

        if (followsUpdateInProgress || streamsUpdateInProgress || !SettingsManager.validUsername())
            return;

        followsUpdateInProgress = true;
        remainingFollowsRequests = 0;
        remainingUsersRequests = 0;
        followsFetched = 0;
        ChannelDb.setFollowsDirty();
        EventBus.getDefault().post(new FollowsUpdateStartedEvent());
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
                        Requests.getFollows(followsResponse.pagination.cursor, new FollowsListener(), errorListener);
                    else {  // We are done, clean follows and get the users data
                        ChannelDb.cleanFollows();
                        SettingsManager.setFollowsNeedUpdate(false);
                        updateUsersData(UPDATE_TYPE_FOLLOWS);
                    }
                });
            } else {  // If we hit the limit, call it quits
                ThreadManager.post(() -> {
                    ToastMaker.makeToastLong(ToastMaker.MESSAGE_TOO_MANY_FOLLOWS);
                    ChannelDb.cleanFollows();
                    SettingsManager.setFollowsNeedUpdate(false);
                    updateUsersData(UPDATE_TYPE_FOLLOWS);
                });
            }
        }
    }

    /**
     * Requests the users data for any user id in the follows table that is not
     * already in the users table. Notifies the listener if there is no new
     * users data to fetch.
     */
    private static void updateUsersData(int type) {
        if (type == UPDATE_TYPE_FOLLOWS) {
            long[][] userIds = URLTools.splitIdArray(ChannelDb.getUnknownUserIdsFromFollows());
            if (userIds.length == 0) {
                postFollowsUpdatedEvent();
            } else {
                remainingUsersRequests = userIds.length;
                for (long[] ids : userIds)
                    Requests.getUsers(ids, new UsersListener(type), ErrorHandler.getInstance());
            }
        } else if (type == UPDATE_TYPE_TOP_STREAMS) {
            long[][] userIds = URLTools.splitIdArray(ChannelDb.getUnknownUserIdsFromStreams());
            if (userIds.length == 0) {
                topStreamsUsersUpdateInProgress = false;
                postTopStreamsUpdatedEvent();
            } else {
                for (long[] ids : userIds)
                    Requests.getUsers(ids, new UsersListener(type), ErrorHandler.getInstance());
            }
        }
    }

    /**
     * Inserts the users data into the database and notifies the follows listener
     * if all of the users requests have completed.
     */
    private static class UsersListener implements Response.Listener<Containers.Users> {

        private int type;

        UsersListener(int type) {
            this.type = type;
        }

        @Override
        public void onResponse(Containers.Users response) {
            if (type == UPDATE_TYPE_FOLLOWS)
                ThreadManager.post(() -> {
                    remainingUsersRequests--;
                    ChannelDb.insertUsersData(response);
                    if (remainingUsersRequests == 0)
                        postFollowsUpdatedEvent();
                });
            else if (type == UPDATE_TYPE_TOP_STREAMS)
                ThreadManager.post(() -> {
                    ChannelDb.insertUsersData(response);
                    topStreamsUsersUpdateInProgress = false;
                    postTopStreamsUpdatedEvent();
                });
        }

    }

    /**
     * Updates the streams table then updates the games table if necessary.
     *
     * @param errorListener notified if any network operation goes wrong
     */
    public static void updateStreamsData(final Response.ErrorListener errorListener) {
        DataUpdateManager.errorListener = new ErrorWrapper(errorListener);

        if (streamsUpdateInProgress || followsUpdateInProgress) return;

        streamsUpdateInProgress = true;
        remainingStreamsRequests = 0;
        remainingGamesRequests = 0;
        remainingUsersRequests = 0;
        EventBus.getDefault().post(new StreamsUpdateStartedEvent());
        ThreadManager.post(DataUpdateManager::performStreamsUpdate);
    }

    /**
     * Updates the streams and games data. This method should NOT be called on the main thread.
     */
    private static void performStreamsUpdate() {
        long[][] userIds = URLTools.splitIdArray(ChannelDb.getAllFollowIds());
        remainingStreamsRequests = userIds.length;
        for (long[] ids : userIds)
            Requests.getStreams(ids, new StreamsListener(UPDATE_TYPE_FOLLOWS), errorListener);

        // Check for unknown user ids in case follows update failed
        long[][] unknownUserIds = URLTools.splitIdArray(ChannelDb.getUnknownUserIdsFromFollows());
        remainingUsersRequests = unknownUserIds.length;
        for (long[] ids : unknownUserIds)
            Requests.getUsers(ids, new UsersListener(UPDATE_TYPE_FOLLOWS), errorListener);
    }

    /**
     * Inserts the streams data into the database, then starts the games update after
     * all of the streams data has been updated.
     */
    private static class StreamsListener implements Response.Listener<Containers.Streams> {

        private int type;

        StreamsListener(int type) {
            this.type = type;
        }

        @Override
        public void onResponse(Containers.Streams response) {
            if (type == UPDATE_TYPE_FOLLOWS)
                ThreadManager.post(() -> {
                    remainingStreamsRequests--;
                    ChannelDb.insertStreamsData(response);
                    if (remainingStreamsRequests == 0)
                        updateGamesData(type);
                });
            else if (type == UPDATE_TYPE_TOP_STREAMS)
                ThreadManager.post(() -> {
                    ChannelDb.insertStreamsData(response);
                    updateGamesData(type);
                    updateUsersData(type);
                });
        }
    }

    /**
     * Requests the games data for any games id in the streams table that is not
     * already in the games table. Notifies the listener if there are no games
     * to fetch.
     */
    private static void updateGamesData(int type) {
        long[][] gameIds = URLTools.splitIdArray(ChannelDb.getUnknownGameIds());
        // 0 indicates a null game, so ignore that
        if (gameIds.length == 0 || (gameIds[0].length == 1 && gameIds[0][0] == 0))  {
            if (type == UPDATE_TYPE_FOLLOWS) {
                postFollowsStreamsUpdatedEvent(true);
            } else if (type == UPDATE_TYPE_TOP_STREAMS) {
                topStreamsGamesUpdateInProgress = false;
                postTopStreamsUpdatedEvent();
            }
        } else {
            remainingGamesRequests = gameIds.length;
            for (long[] ids : gameIds)
                Requests.getGames(ids, new GamesListener(type), ErrorHandler.getInstance());
        }
    }

    /**
     * Inserts games data into database, then notifies the listener.
     */
    private static class GamesListener implements Response.Listener<Containers.Games> {

        private int type;

        GamesListener(int type) {
            this.type = type;
        }

        @Override
        public void onResponse(Containers.Games response) {
            if (type == UPDATE_TYPE_FOLLOWS)
                ThreadManager.post(() -> {
                    remainingGamesRequests--;
                    ChannelDb.insertGamesData(response);
                    if (remainingGamesRequests == 0)
                        postFollowsStreamsUpdatedEvent(true);
                });
            else if (type == UPDATE_TYPE_TOP_STREAMS)
                ThreadManager.post(() -> {
                    ChannelDb.insertGamesData(response);
                    topStreamsGamesUpdateInProgress = false;
                    postTopStreamsUpdatedEvent();
                });
        }
    }

    public static void updateTopStreamsData(final Response.ErrorListener errorListener) {
        if (topStreamsUsersUpdateInProgress || topStreamsGamesUpdateInProgress) return;
        topStreamsGamesUpdateInProgress = true;
        topStreamsUsersUpdateInProgress = true;
        // Get the top streams
        EventBus.getDefault().post(new TopListUpdateStartedEvent());
        Requests.getTopStreams(new StreamsListener(UPDATE_TYPE_TOP_STREAMS), errorListener);
    }

    private static synchronized void postTopStreamsUpdatedEvent() {
        if (topStreamsGamesUpdateInProgress || topStreamsUsersUpdateInProgress) return;
        EventBus.getDefault().post(new TopStreamsUpdatedEvent());
    }

    private static synchronized  void postFollowsUpdatedEvent() {
        followsUpdateInProgress = false;
        EventBus.getDefault().post(new FollowsUpdatedEvent());
    }

    private static synchronized  void postFollowsStreamsUpdatedEvent(boolean updated) {
        streamsUpdateInProgress = false;
        if (updated)
            SettingsManager.setLastUpdated();
        EventBus.getDefault().post(new StreamsUpdatedEvent());
    }

    private static class ErrorWrapper implements Response.ErrorListener {

        private Response.ErrorListener errorListener;

        ErrorWrapper(Response.ErrorListener errorListener) {
            this.errorListener = errorListener;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            followsUpdateInProgress = false;
            streamsUpdateInProgress = false;
            errorListener.onErrorResponse(error);
        }
    }
}
