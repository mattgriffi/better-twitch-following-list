package mseffner.twitchnotifier.networking;


import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

/**
 * Provides methods to run asynchronous network operations using Volley and Gson.
 */
public class Netcode {

    public static final int REQUEST_TYPE_USERS = 0;
    public static final int REQUEST_TYPE_STREAMS = 1;
    public static final int REQUEST_TYPE_FOLLOWS = 2;
    public static final int REQUEST_TYPE_GAMES = 3;

    private static final String LOG_TAG = Netcode.class.getSimpleName();
    private static final int CACHE_SIZE = 1024 * 1024;  // in bytes

    private static RequestQueue queue;

    public static void initialize(Context context) {
        Cache cache = new DiskBasedCache(context.getCacheDir(), CACHE_SIZE);
        Network network = new BasicNetwork(new HurlStack());
        queue = new RequestQueue(cache, network);
        queue.start();
    }

    public static void destroy() {
        queue.stop();
        queue = null;
    }

    public static void getFollows(@Nullable String cursor, Response.Listener<Containers.Follows> listener,
                                  @Nullable Response.ErrorListener errorListener) {
        String url = URLTools.getFollowsUrl(cursor);
        queue.add(RequestFactory.getRequest(REQUEST_TYPE_FOLLOWS, url, listener, errorListener));
    }

    public static void getUsers(long[] ids, Response.Listener<Containers.Users> listener,
                                @Nullable Response.ErrorListener errorListener) {
        String url = URLTools.getUsersUrl(ids);
        queue.add(RequestFactory.getRequest(REQUEST_TYPE_FOLLOWS, url, listener, errorListener));
    }

    public static void getGames(long[] ids, Response.Listener<Containers.Games> listener,
                                @Nullable Response.ErrorListener errorListener) {
        String url = URLTools.getGamesUrl(ids);
        queue.add(RequestFactory.getRequest(REQUEST_TYPE_GAMES, url, listener, errorListener));
    }

    public static void getTopStreams(Response.Listener<Containers.Streams> listener,
                                     @Nullable Response.ErrorListener errorListener) {
        String url = URLTools.getTopStreamsUrl();
        queue.add(RequestFactory.getRequest(REQUEST_TYPE_STREAMS, url, listener, errorListener));
    }

    public static void makeRequest(int requestType, String url, Response.Listener listener,
                                   @Nullable Response.ErrorListener errorListener) {
        queue.add(RequestFactory.getRequest(requestType, url, listener, errorListener));
    }
}
