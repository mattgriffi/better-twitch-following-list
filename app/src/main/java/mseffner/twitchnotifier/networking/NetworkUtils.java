package mseffner.twitchnotifier.networking;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import mseffner.twitchnotifier.data.Channel;

public final class NetworkUtils {

    // Note that this is a test client_id and is not used for release versions
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";

    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/kraken/";

    private static final String PATH_CHANNELS = "channels";
    private static final String PATH_STREAMS = "streams";
    private static final String PATH_USERS = "users";
    private static final String PATH_FOLLOWS_CHANNELS = "follows/channels";

    private static final String PARAM_CLIENT_ID = "client_id";


    private NetworkUtils() {}

    public static String makeHttpsRequest(String channelName) {

        URL url = buildUrl(channelName);

        String response = "";
        HttpsURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {

            urlConnection = setupHttpsURLConnection(url);
            urlConnection.connect();

            // Check the response code and get the response if it's OK
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                inputStream = urlConnection.getInputStream();
                response = readFromInputStream(inputStream);
            } else {
                Log.e("NetworkUtils",  "Error response code: " + responseCode);
            }

        } catch (IOException e) {
            Log.e("NetworkUtils", e.toString());
        } finally {
            closeConnections(urlConnection, inputStream);
        }

        return response;
    }

    public static List<Channel> getChannels(String[] channelNames) {

        List<Channel> channels = new ArrayList<>();

        for (String channelName : channelNames) {
            String jsonResponse = makeHttpsRequest(channelName);
            Channel channel = getChannelFromJson(jsonResponse);
            channels.add(channel);
        }

        return channels;
    }

    private static Channel getChannelFromJson(String jsonResponse) {

        try {

            JSONObject channelData = new JSONObject(jsonResponse);

            String displayName = channelData.getString("display_name");
            String name = channelData.getString("name");
            String logoUrl = channelData.getString("logo");
            String streamUrl = channelData.getString("url");

            return new Channel(displayName, name, logoUrl, streamUrl);

        }catch (JSONException e) {
            Log.e("NetworkUtils", e.toString());
        }

        return null;
    }

    private static HttpsURLConnection setupHttpsURLConnection(URL url) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10_000);
        urlConnection.setConnectTimeout(15_000);
        return urlConnection;
    }

    private static void closeConnections(HttpsURLConnection urlConnection, InputStream inputStream) {

        if (urlConnection != null)
            urlConnection.disconnect();

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e("NetworkUtils", e.toString());
            }
        }
    }

    private static URL buildUrl(String channelName) {

        // Build the Uri
        Uri uri = Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_CHANNELS)
                .appendPath(channelName)
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
