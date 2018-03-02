package mseffner.twitchnotifier.networking;


import java.util.HashMap;
import java.util.Map;

/**
 * Netcode provides methods to run asynchronous network operations
 * using Volley and Gson.
 */
public class Netcode {

    private static final String LOG_TAG = Netcode.class.getSimpleName();

    // Note that this is a test client_id and is not used for release versions
    // TODO get new client_id for release version
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";
    private static final String CLIENT_ID_HEADER = "Client-ID";
    private static final Map<String, String> headers;
    static {
        headers = new HashMap<>();
        headers.put(CLIENT_ID_HEADER, CLIENT_ID);
    }
}
