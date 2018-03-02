package mseffner.twitchnotifier.networking;


import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import mseffner.twitchnotifier.settings.SettingsManager;

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
    private static final String PARAM_USER_ID = "user_id";
    private static final String PARAM_USER_LOGIN = "user_login";

    private static final String FIRST_MAX = "100";
    private static final String FIRST_DEFAULT = "25";

    /**
     * @return  url to get the user information for the app user
     */
    private static String getUserIdUrl() {
        String username = SettingsManager.getUsername();
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_USERS)
                .appendQueryParameter(PARAM_LOGIN, username)
                .build().toString();
    }

    /**
     * @return url to get top 100 streams
     */
    private static String getTopStreamsUrl() {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS)
                .appendQueryParameter(PARAM_FIRST, FIRST_MAX)
                .build().toString();
    }

    /**
     * @return url to get first 100 followed channels after cursor
     */
    private static String getFollowsUrl(@Nullable String cursor) {
        String userId = Long.toString(SettingsManager.getUsernameId());
        Uri.Builder builder = Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendEncodedPath(PATH_FOLLOWS)
                .appendQueryParameter(PARAM_FIRST, FIRST_MAX)
                .appendQueryParameter(PARAM_FROM_ID, userId);
        if (cursor != null)
            builder.appendQueryParameter(PARAM_AFTER, cursor);
        return builder.toString();
    }
}
