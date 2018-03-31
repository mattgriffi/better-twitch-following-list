package mseffner.twitchnotifier.networking;

import com.android.volley.Response;

import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.ThreadManager;


/**
 * BaseListener should be subclassed by all request listeners. It will automatically
 * decrement the appropriate counter in RequestTracker depending on the type of the
 * child listener.
 *
 * @param <T>   a class defined in Containers
 */
public abstract class BaseListener<T> implements Response.Listener<T> {

    /**
     * This method provides a hook for custom response handling. Database insertions
     * and RequestTracker decrementing will be done automatically after this method
     * returns. It is called on a background thread.
     *
     * @param response  the request response object
     */
    protected void handleResponse(T response) {}

    @Override
    public final void onResponse(T response) {
        ThreadManager.post(() -> {
            handleResponse(response);
            insertDecrement(response);
        });
    }

    private void insertDecrement(T response) {
        /* The inserts must happen before the corresponding decrement so that
        the process will be fully complete before RequestTracker is notified. */
        if (response instanceof Containers.Streams) {
            ChannelDb.insertStreamsData((Containers.Streams) response);
            UpdateCoordinator.decrementStreams();
        } else if (response instanceof Containers.Games) {
            ChannelDb.insertGamesData((Containers.Games) response);
            UpdateCoordinator.decrementGames();
        } else if (response instanceof Containers.Follows) {
            ChannelDb.insertFollowsData((Containers.Follows) response);
            UpdateCoordinator.decrementFollows();
        } else if (response instanceof Containers.Users) {
            ChannelDb.insertUsersData((Containers.Users) response);
            UpdateCoordinator.decrementUsers();
        }
    }
}
