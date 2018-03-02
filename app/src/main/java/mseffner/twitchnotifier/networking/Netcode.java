package mseffner.twitchnotifier.networking;


import android.content.Context;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Netcode provides methods to run asynchronous network operations
 * using Volley and Gson.
 */
public class Netcode {

    public static final int REQUEST_TYPE_USERS = 0;
    public static final int REQUEST_TYPE_STREAMS = 1;
    public static final int REQUEST_TYPE_FOLLOWS = 2;
    public static final int REQUEST_TYPE_GAMES = 3;

    private static final String LOG_TAG = Netcode.class.getSimpleName();

    // Note that this is a test client id and is not used for release versions
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";
    private static final String CLIENT_ID_HEADER = "Client-ID";
    private static final int CACHE_SIZE = 1024 * 1024;  // in bytes
    private static final Map<String, String> headers;
    static {
        headers = new HashMap<>();
        headers.put(CLIENT_ID_HEADER, CLIENT_ID);
    }
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

    @SuppressWarnings("unchecked")
    public static void makeRequest(int requestType, String url, Response.Listener listener,
                                   Response.ErrorListener errorListener) {
        Log.e(LOG_TAG, "Making request for url: " + url);
        GsonRequest request;
        switch (requestType) {
            case REQUEST_TYPE_USERS:
                request = getUsersRequest(url, listener, errorListener);
                break;
            case REQUEST_TYPE_STREAMS:
                request = getStreamsRequest(url, listener, errorListener);
                break;
            case REQUEST_TYPE_FOLLOWS:
                request = getFollowsRequest(url, listener, errorListener);
                break;
            case REQUEST_TYPE_GAMES:
                request = getGamesRequest(url, listener, errorListener);
                break;
            default: return;
        }
        queue.add(request);
    }

    private static GsonRequest getUsersRequest(String url, Response.Listener<Containers.Users> listener,
                                               Response.ErrorListener errorListener) {
        return new GsonRequest<>(url, Containers.Users.class, headers, listener, errorListener);
    }

    private static GsonRequest getFollowsRequest(String url, Response.Listener<Containers.Follows> listener,
                                                 Response.ErrorListener errorListener) {
        return new GsonRequest<>(url, Containers.Follows.class, headers, listener, errorListener);
    }

    private static GsonRequest getStreamsRequest(String url, Response.Listener<Containers.Streams> listener,
                                                 Response.ErrorListener errorListener) {
        return new GsonRequest<>(url, Containers.Streams.class, headers, listener, errorListener);
    }

    private static GsonRequest getGamesRequest(String url, Response.Listener<Containers.Games> listener,
                                               Response.ErrorListener errorListener) {
        return new GsonRequest<>(url, Containers.Games.class, headers, listener, errorListener);
    }
}
