package mseffner.twitchnotifier.networking;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

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

    public static String makeHttpRequest() throws IOException {

        URL url = buildUrl();
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

        String response = "";
        try {
            InputStream inputStream = urlConnection.getInputStream();
            response = readFromInputStream(inputStream);
        } finally {
            urlConnection.disconnect();
        }

        return response;
    }

    private static URL buildUrl() {

        // Build the Uri
        Uri uri = Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_CHANNELS)
                .appendPath(CHANNEL_NAME)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .build();

        // Convert Uri into URL
        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException e) {
            Log.e("NetworkUtils", e.toString());
        }

        return url;
    }

    private static String readFromInputStream(InputStream inputStream) {

        Scanner scanner = new Scanner(inputStream);
        // Using \A as the delimiter causes the Scanner to read in the InputStream in one chunk
        scanner.useDelimiter("\\A");

        String result = "";
        if (scanner.hasNext()) {
            result = scanner.next();
        }
        scanner.close();

        return result;
    }

}
