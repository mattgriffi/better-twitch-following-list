package mseffner.twitchnotifier.networking;


import java.util.HashMap;
import java.util.Map;

/**
 * Network provides methods to run asynchronous network operations
 * using Volley and Gson.
 */
public class Network {

    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    // Note that this is a test client_id and is not used for release versions
    // TODO get new client_id for release version
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";
    private static final String CLIENT_ID_HEADER = "Client-ID";
    private static final Map<String, String> headers;
    static {
        headers = new HashMap<>();
        headers.put(CLIENT_ID_HEADER, CLIENT_ID);
    }

    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/helix";

    private static final String PATH_STREAMS = "streams";
    private static final String PATH_USERS = "users";
    private static final String PATH_GAMES = "games";
    private static final String PATH_FOLLOWS = "users/follows";

    private static final String PARAM_FIRST = "first";
    private static final String PARAM_AFTER = "after";
    private static final String PARAM_FROM_ID = "from_id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_LOGIN = "login";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_UsER_ID = "user_id";
    private static final String PARAM_USER_LOGIN = "user_login";

    private static final String FIRST_MAX = "100";
    private static final String FIRST_DEFAULT = "25";
}
