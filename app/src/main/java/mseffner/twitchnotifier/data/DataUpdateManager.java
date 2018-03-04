package mseffner.twitchnotifier.data;


import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.Response;

import java.util.List;

import mseffner.twitchnotifier.networking.Requests;

/**
 * This class provides methods to update data via asynchronous network
 * and database operations. It also defines interfaces for listeners and
 * will notify listeners when the requests are completed.
 */
public class DataUpdateManager {

    private static final String TAG = DataUpdateManager.class.getSimpleName();

    private static TopStreamsListener listener;

    public interface TopStreamsListener {
        void onTopStreamsResponse(@NonNull List<Channel> channels);
    }

    public static void getTopStreamsData(@NonNull TopStreamsListener listener,
                                         final Response.ErrorListener errorListener) {
        DataUpdateManager.listener = listener;
        ContainerParser parser = new ContainerParser();

        // Get the top streams
        Requests.getTopStreams(streamsResponse -> {
            parser.setStreams(streamsResponse);
            // Get the game names
            Requests.getGames(parser.getGameIdsFromStreams(),
                    gamesResponse -> {
                        parser.setGames(gamesResponse);
                        notifyListener(parser);
                    }, errorListener);
            // Get the streamer names
            Requests.getUsers(parser.getUserIdsFromStreams(),
                    usersResponse -> {
                        parser.setUsers(usersResponse);
                        notifyListener(parser);
                    }, errorListener);
        }, errorListener);
    }

    private static synchronized void notifyListener(ContainerParser parser) {
        Log.e(TAG, "notifyListener");
        if (parser == null || !parser.isDataComplete() || listener == null) return;
        listener.onTopStreamsResponse(parser.getChannelList());
        listener = null;
    }
}
