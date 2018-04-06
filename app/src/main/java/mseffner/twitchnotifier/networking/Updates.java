package mseffner.twitchnotifier.networking;


import com.android.volley.Response;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.UserIdUpdatedEvent;
import mseffner.twitchnotifier.settings.SettingsManager;

/**
 * This class provides methods to update data via asynchronous network
 * and database operations. It also defines interfaces for listeners and
 * will notify listeners when the requests are completed.
 */
public class Updates {

    private Updates() {}

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
            if (response.data.isEmpty()) {
                ToastMaker.makeToastLong(ToastMaker.MESSAGE_INVALID_USERNAME);
                return;
            }
            ToastMaker.makeToastLong(ToastMaker.MESSAGE_USERNAME_CHANGE);
            SettingsManager.setUsernameId(Long.parseLong(response.data.get(0).id));
            EventBus.getDefault().post(new UserIdUpdatedEvent());
            UpdateCoordinator.decrementUsersNoUpdate();
        }
    }

    /**
     * Updates the follows table, removing any rows that no longer appear in the
     * follows response. This is run
     * on a background thread.
     */
    public static void updateFollows() {
        if (!SettingsManager.validUsername())
            return;

        ThreadManager.post(() -> {
            Database.setFollowsDirty();
            Requests.getFollows(null, new FollowsListener());
        });
    }

    /**
     * Inserts the follows data into the database, then either starts the next request
     * if there is more follows data to fetch, or cleans the follows table.
     */
    private static class FollowsListener extends BaseListener<Containers.Follows> {
        @Override
        protected void handleResponse(Containers.Follows followsResponse) {
            if (UpdateCoordinator.followsNotStartedYet())
                UpdateCoordinator.setRemainingFollows((int) Math.ceil(followsResponse.total / 100.0) - 1);

            if (UpdateCoordinator.needMoreFollows())
                Requests.getFollows(followsResponse.pagination.cursor, this);
            else
                followsUpdateSuccessful();
        }
    }

    private static void followsUpdateSuccessful() {
        Database.cleanFollows();
        SettingsManager.setFollowsNeedUpdate(false);
    }

    /**
     * Updates the streams table from follows and top streams. This
     * is run on a background thread.
     */
    public static void updateStreams() {
        ThreadManager.post(() -> {
            long[][] userIds = URLTools.splitIdArray(Database.getAllFollowIds());
            for (long[] ids : userIds)
                Requests.getStreams(ids, new BaseListener<Containers.Streams>() {});
            Requests.getTopStreams(new BaseListener<Containers.Streams>() {});
        });
    }

    /**
     * Requests the users data for any user id in the follows or streams tables
     * that is not already in the users table. This is run on a background thread.
     */
    public static void updateUsers() {
        ThreadManager.post(() -> {
            long[][] userIds = URLTools.splitIdArray(Database.getUnknownUserIds());
            if (userIds.length == 0) UpdateCoordinator.noUsersUpdateNeeded();
            for (long[] ids : userIds)
                Requests.getUsers(ids, new BaseListener<Containers.Users>() {});
        });
    }

    /**
     * Requests the games data for any games id in the streams table that is not
     * already in the games table. This is run on a background thread.
     */
    public static void updateGames() {
        ThreadManager.post(() -> {
            long[][] gameIds = URLTools.splitIdArray(Database.getUnknownGameIds());
            // 0 is a null game, so ignore that
            if (gameIds.length == 0 || (gameIds[0].length == 1 && gameIds[0][0] == 0)) {
                UpdateCoordinator.noGamesUpdateNeeded();
                return;
            }
            for (long[] ids : gameIds)
                Requests.getGames(ids, new BaseListener<Containers.Games>() {});
        });
    }
}
