package mseffner.twitchnotifier.networking;

import java.io.InputStream;
import java.net.URL;

public final class NetworkUtils {

    // Note that this is a test client_id and is not used for release versions
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";

    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/kraken/";

    private static final String PATH_CHANNELS = "channels";
    private static final String PATH_STREAMS = "streams";
    private static final String PATH_USERS = "users";
    private static final String PATH_FOLLOWS_CHANNELS = "follows/channels";

    private static final String CHANNEL_NAME = "Cirno_TV";

    private static final String PARAM_CLIENT_ID = "client_id";


    private NetworkUtils() {}

    public static String makeHttpRequest() {
        return null;
    }
    private static URL buildUrl() {
        return null;
    }

    private static String readFromInputStream(InputStream inputStream) {
        return null;
    }

}
