package mseffner.twitchnotifier.networking;


public class NetworkUtils2 {

    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    // Note that this is a test client_id and is not used for release versions
    // TODO get new client_id for release version
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";

    private static final int QUERY_TYPE_CHANNEL = 0;
    private static final int QUERY_TYPE_STREAM = 1;
    private static final int QUERY_TYPE_STREAM_MULTIPLE = 3;
    private static final int QUERY_TYPE_USER_ID = 4;

    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/helix/";

    private static final String PATH_CHANNELS = "channels";
    private static final String PATH_STREAMS = "streams";
    private static final String PATH_USERS = "users";
    private static final String PATH_FOLLOWS_CHANNELS = "follows/channels";

    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_CHANNEL = "channel";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_STREAM_TYPE = "stream_type";
    private static final String PARAM_API_VERSION = "api_version";
    private static final String PARAM_LOGIN = "login";

    private static final String LIMIT_MAX = "100";
    private static final String LIMIT_MED = "25";
    private static final String STREAM_TYPE_LIVE = "live";
    private static final String API_VERSION = "5";

    public static class NetworkException extends Exception {}

    public static class InvalidUsernameException extends Exception {}


}
