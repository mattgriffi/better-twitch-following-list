package mseffner.twitchnotifier.networking;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.LiveStream;


public final class NetworkUtils {

    // Note that this is a test client_id and is not used for release versions
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";

    private static final int QUERY_TYPE_CHANNEL = 0;
    private static final int QUERY_TYPE_STREAM = 1;

    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/kraken/";

    private static final String PATH_CHANNELS = "channels";
    private static final String PATH_STREAMS = "streams";
    private static final String PATH_USERS = "users";
    private static final String PATH_FOLLOWS_CHANNELS = "follows/channels";

    private static final String PARAM_CLIENT_ID = "client_id";


    private NetworkUtils() {}

    public static List<Channel> getChannels(String[] channelNames) {

        List<Channel> channels = new ArrayList<>();

        for (String channelName : channelNames) {

            URL channelQueryUrl = buildUrl(channelName, QUERY_TYPE_CHANNEL);
            String channelJsonResponse = makeTwitchQuery(channelQueryUrl);
            Channel channel = getChannelFromJson(channelJsonResponse);

            if (channel != null) {

                URL streamQueryUrl = buildUrl(channelName, QUERY_TYPE_STREAM);
                String streamJsonResponse = makeTwitchQuery(streamQueryUrl);
                LiveStream stream = getLiveStreamFromJson(streamJsonResponse);

                channel.setStream(stream);

                channels.add(channel);
            }
        }

        return channels;
    }

    private static String makeTwitchQuery(URL url) {

        String response;

        HttpsURLConnection urlConnection = openHttpsConnection(url);
        if (urlConnection == null)
            return null;

        InputStream inputStream = getInputStreamFromConnection(urlConnection);
        if (inputStream == null) {
            closeConnections(urlConnection, null);
            return null;
        }

        response = readStringFromInputStream(inputStream);

        closeConnections(urlConnection, inputStream);

        return response;
    }

    private static HttpsURLConnection openHttpsConnection(URL url) {

        HttpsURLConnection urlConnection = null;

        try {

            urlConnection = setupHttpsURLConnection(url);
            urlConnection.connect();

            // Check the response code, log and return null if it's bad
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e("NetworkUtils",  "Error response code: " + responseCode);
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return urlConnection;
    }

    private static InputStream getInputStreamFromConnection(HttpURLConnection connection) {
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            closeConnections(connection, null);
            Log.e("NetworkUtils", e.toString());
        }
        return inputStream;
    }

    private static HttpsURLConnection setupHttpsURLConnection(URL url) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10_000);
        urlConnection.setConnectTimeout(15_000);
        return urlConnection;
    }

    private static void closeConnections(HttpURLConnection urlConnection, InputStream inputStream) {

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

    private static URL buildUrl(String channelName, int queryType) {

        Uri uri = Uri.EMPTY;
        if (queryType == QUERY_TYPE_CHANNEL)
            uri = buildChannelQueryUri(channelName);
        else if (queryType == QUERY_TYPE_STREAM)
            uri = buildStreamQueryUri(channelName);

        // Convert Uri into URL
        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException e) {
            Log.e("NetworkUtils", e.toString());
        }

        return url;
    }

    private static URL buildChannelLogoQueryURL(String logoUrl) {
        URL url = null;
        try {
            url = new URL(logoUrl);
        } catch (MalformedURLException e) {
            Log.e("NetworkUtils", e.toString());
        }
        return url;
    }

    private static Uri buildStreamQueryUri(String channelName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS)
                .appendPath(channelName)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .build();
    }

    private static Uri buildChannelQueryUri(String channelName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_CHANNELS)
                .appendPath(channelName)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .build();
    }

    private static String readStringFromInputStream(InputStream inputStream) {

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

    private static LiveStream getLiveStreamFromJson(String jsonResponse) {

        try {

            JSONObject resultJson = new JSONObject(jsonResponse);
            if (resultJson.isNull("stream"))
                return null;

            JSONObject streamData = resultJson.getJSONObject("stream");
            JSONObject channelData = streamData.getJSONObject("channel");

            String game = streamData.getString("game");
            int viewers = streamData.getInt("viewers");
            String status = channelData.getString("status");
            String streamType = streamData.getString("stream_type");
            String startTime = streamData.getString("created_at");

            return new LiveStream(game, viewers, status, startTime, streamType);

        }catch (JSONException e) {
            Log.e("NetworkUtils", e.toString());
        }

        return null;
    }

}
