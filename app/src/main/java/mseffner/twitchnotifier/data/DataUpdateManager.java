package mseffner.twitchnotifier.data;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.android.volley.Response;

import java.util.List;

import mseffner.twitchnotifier.networking.Containers;
import mseffner.twitchnotifier.networking.Requests;
import mseffner.twitchnotifier.settings.SettingsManager;

/**
 * This class provides methods to update data via asynchronous network
 * and database operations. It also defines interfaces for listeners and
 * will notify listeners when the requests are completed.
 */
public class DataUpdateManager {

    private static final String TAG = DataUpdateManager.class.getSimpleName();

    private static TopStreamsListener topStreamsListener;
    private static FollowsListener followsListener;
    private static Response.ErrorListener followsErrorListener;
    private static Handler handler;

    public interface TopStreamsListener {
        void onTopStreamsResponse(@NonNull List<Channel> channels);
    }

    public interface FollowsListener {
        void onFollowsDataUpdated();
    }

    public static void updateFollowsData(FollowsListener listener,
                                         final Response.ErrorListener errorListener) {
        followsListener = listener;
        followsErrorListener = errorListener;

        // Get all of the follows data in chunks of 100
        if (!SettingsManager.getUsername().equals(""))
            Requests.getFollows(null, new FollowsResponseListener(), followsErrorListener);
    }

    private static class FollowsResponseListener implements Response.Listener<Containers.Follows> {
        @Override
        public void onResponse(Containers.Follows followsResponse) {
            if (followsListener == null) {
                // TODO cancel update, clean database
            }
            checkHandler();

            handler.post(() -> ChannelDb.insertFollowsData(followsResponse));

            ContainerParser parser = new ContainerParser();
            parser.setFollows(followsResponse);
            long[] userIds = parser.getUserIdsFromFollows();

            // Get users data
            Requests.getUsers(userIds, usersResponse -> handler.post(() -> ChannelDb.updateUsersData(usersResponse)),
                    followsErrorListener);

            // Get streams data
            Requests.getStreams(userIds, streamsResponse -> {
                handler.post(() -> ChannelDb.updateStreamsData(streamsResponse));

                // Get the games data
                ContainerParser gamesParser = new ContainerParser();
                gamesParser.setStreams(streamsResponse);
                Requests.getGames(gamesParser.getGameIdsFromStreams(),
                        gamesResponse -> handler.post(() -> ChannelDb.updateGamesData(gamesResponse)),
                        followsErrorListener);

            }, followsErrorListener);

            // If there are 100 elements, then there might be more to fetch
            if (parser.getFollowsDataSize() >= 100) {
                String cursor = parser.getFollowsCursor();
                // Recursively fetch more data
                Requests.getFollows(cursor, new FollowsResponseListener(), followsErrorListener);
            } else {
                // We are done
                followsListener.onFollowsDataUpdated();
            }
        }
    }

    private static void checkHandler() {
        if (handler == null) {
            HandlerThread handlerThread = new HandlerThread("DatabaseOperations");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            handler = new Handler(looper);
        }
    }

    public static void getTopStreamsData(@NonNull TopStreamsListener listener,
                                         final Response.ErrorListener errorListener) {
        DataUpdateManager.topStreamsListener = listener;
        ContainerParser parser = new ContainerParser();
        checkHandler();

        // Get the top streams
        Requests.getTopStreams(streamsResponse -> {
            parser.setStreams(streamsResponse);
            // Get the game names
            Requests.getGames(parser.getGameIdsFromStreams(),
                    gamesResponse -> {
                        handler.post(() -> ChannelDb.insertGamesData(gamesResponse));
                        parser.setGames(gamesResponse);
                        notifyListener(parser);
                    }, errorListener);
            // Get the streamer names
            Requests.getUsers(parser.getUserIdsFromStreams(),
                    usersResponse -> {
                        handler.post(() -> ChannelDb.insertUsersData(usersResponse));
                        parser.setUsers(usersResponse);
                        notifyListener(parser);
                    }, errorListener);
        }, errorListener);
    }

    private static synchronized void notifyListener(ContainerParser parser) {
        if (parser == null || !parser.isDataComplete() || topStreamsListener == null) return;
        topStreamsListener.onTopStreamsResponse(parser.getChannelList());
        topStreamsListener = null;
    }
}
