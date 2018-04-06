package mseffner.twitchnotifier.networking;

import com.android.volley.Response;

import java.util.HashMap;
import java.util.Map;


public class RequestFactory {

    // Note that this is a test client id and is not used for release versions
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";
    private static final String CLIENT_ID_HEADER = "Client-ID";
    private static final Map<String, String> headers = new HashMap<>();
    static { headers.put(CLIENT_ID_HEADER, CLIENT_ID); }

    @SuppressWarnings("unchecked")
    public static GsonRequest getRequest(int requestType, String url, Response.Listener listener) {
        GsonRequest request = null;
        switch (requestType) {
            case Requests.REQUEST_TYPE_USERS:
                UpdateCoordinator.incrementUsers();
                request = getUsersRequest(url, listener);
                break;
            case Requests.REQUEST_TYPE_STREAMS:
                UpdateCoordinator.incrementStreams();
                request = getStreamsRequest(url, listener);
                break;
            case Requests.REQUEST_TYPE_FOLLOWS:
                UpdateCoordinator.incrementFollows();
                request = getFollowsRequest(url, listener);
                break;
            case Requests.REQUEST_TYPE_GAMES:
                UpdateCoordinator.incrementGames();
                request = getGamesRequest(url, listener);
                break;
            case Requests.REQUEST_TYPE_STREAMS_LEGACY:
                UpdateCoordinator.incrementStreams();
                request = getStreamsLegacyRequest(url, listener);
        }
        return request;
    }

    private static GsonRequest getUsersRequest(String url, Response.Listener<Containers.Users> listener) {
        return new GsonRequest<>(url, Containers.Users.class, headers, listener, new ErrorHandler(Requests.REQUEST_TYPE_USERS));
    }

    private static GsonRequest getFollowsRequest(String url, Response.Listener<Containers.Follows> listener) {
        return new GsonRequest<>(url, Containers.Follows.class, headers, listener, new ErrorHandler(Requests.REQUEST_TYPE_FOLLOWS));
    }

    private static GsonRequest getStreamsRequest(String url, Response.Listener<Containers.Streams> listener) {
        return new GsonRequest<>(url, Containers.Streams.class, headers, listener, new ErrorHandler(Requests.REQUEST_TYPE_STREAMS));
    }

    private static GsonRequest getGamesRequest(String url, Response.Listener<Containers.Games> listener) {
        return new GsonRequest<>(url, Containers.Games.class, headers, listener, new ErrorHandler(Requests.REQUEST_TYPE_GAMES));
    }

    private static GsonRequest getStreamsLegacyRequest(String url, Response.Listener<Containers.StreamsLegacy> listener) {
        return new GsonRequest<>(url, Containers.StreamsLegacy.class, headers, listener, new ErrorHandler(Requests.REQUEST_TYPE_STREAMS_LEGACY));
    }
}
