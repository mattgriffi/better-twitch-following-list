package mseffner.twitchnotifier.networking;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
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
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.Stream;


public final class NetworkUtils {

    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    // Note that this is a test client_id and is not used for release versions
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";

    private static final int QUERY_TYPE_CHANNEL = 0;
    private static final int QUERY_TYPE_STREAM = 1;
    private static final int QUERY_TYPE_STREAM_MULTIPLE = 3;
    private static final int QUERY_TYPE_USER_ID = 4;

    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/kraken/";

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
    private static final String STREAM_TYPE_LIVE = "live";
    private static final String API_VERSION = "5";


    private NetworkUtils() {}

    public static void populateUserFollowedChannels(String userName, ChannelDb database) {

        String userId = Long.toString(getTwitchUserId(userName));
        int chunkSize = Integer.parseInt(LIMIT_MAX);
        int offsetMultiplier = 0;
        // This will be set within the loop, since I don't know how many channels there are until
        // after I have gotten the first response
        int totalFollowedChannels = 0;

        do {
            // The API will only return 100 channels in a single response, so I have to use an
            // offset in order to get all of the channels in chunks of 100
            URL followsQueryUrl = buildUserFollowsUrl(userId, offsetMultiplier * chunkSize);
            String followsJsonResponse = makeTwitchQuery(followsQueryUrl);

            try {

                // TODO fix  java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
                JSONObject responseObject = new JSONObject(followsJsonResponse);
                totalFollowedChannels = responseObject.getInt("_total");
                JSONArray followsJsonArray = responseObject.getJSONArray("follows");

                // Iterate over the array of followed channels
                for (int i = 0; i < followsJsonArray.length(); i++) {

                    // Get the JSONObject String for each channel
                    String channelJsonString = followsJsonArray.getJSONObject(i)
                            .getJSONObject("channel").toString();

                    // Insert each channel into the database
                    database.insertChannel(getChannelFromJson(channelJsonString));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.toString());
            }

            offsetMultiplier++;

        } while (offsetMultiplier < (totalFollowedChannels / chunkSize) + 1);
    }

    public static void updateStreamData(ChannelDb database) {

        int[] channelIds = database.getAllChannelIds();
        String[] commaSeparatedIdLists = getCommaSeparatedIdsArray(channelIds);

        List<Stream> streamList = getStreamsFromCommaSeparatedIds(commaSeparatedIdLists);

        for (Stream stream : streamList) {
            database.updateStreamData(stream);
        }
    }

    @NonNull
    private static List<Stream> getStreamsFromCommaSeparatedIds(String[] commaSeparatedIdLists) {

        List<Stream> streamList = new ArrayList<>();

        for (String commaSeparatedIds : commaSeparatedIdLists) {

            URL streamQueryUrl = buildUrl(commaSeparatedIds, QUERY_TYPE_STREAM_MULTIPLE);
            String streamQueryResponse = makeTwitchQuery(streamQueryUrl);

            fillStreamListFromJson(streamList, streamQueryResponse);
        }
        return streamList;
    }

    private static void fillStreamListFromJson(List<Stream> streamList, String streamQueryResponse) {

        if (streamQueryResponse == null || streamQueryResponse.length() == 0) {
            Log.e(LOG_TAG, "fillStreamListFromJson called with empty String");
            return;
        }

        JSONArray streamArray = null;
        int numStreams = 0;
        try {
            JSONObject response = new JSONObject(streamQueryResponse);
            streamArray = response.getJSONArray("streams");
            numStreams = response.getInt("_total");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < numStreams; i++) {
            try {
                streamList.add(getStreamFromJson(streamArray.getJSONObject(i).toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param idArray An array of every channel ID in the database.
     * @return An array of Strings where each string is a comma separated list of IDs, up to 100 each.
     */
    private static String[] getCommaSeparatedIdsArray(int[] idArray) {

        int chunkSize = Integer.parseInt(LIMIT_MAX);
        String[] commaSeparatedIdList = new String[(idArray.length / chunkSize) + 1];

        StringBuilder stringBuilder = new StringBuilder(1000);

        int idArrayIndex = 0;
        int stringArrayIndex = 0;
        int totalIds = idArray.length;
        while (idArrayIndex < totalIds) {
            if (idArrayIndex > 0 && idArrayIndex % chunkSize == 0) {  // We have the required amount in the StringBuilder
                commaSeparatedIdList[stringArrayIndex++] = stringBuilder.toString();
                stringBuilder.setLength(0);
            }
            stringBuilder.append(Integer.toString(idArray[idArrayIndex++]));
            stringBuilder.append(",");
        }
        commaSeparatedIdList[stringArrayIndex] = stringBuilder.toString();

        return commaSeparatedIdList;
    }

    private static long getTwitchUserId(String userName) {

        URL userIdQueryUrl = buildUrl(userName, QUERY_TYPE_USER_ID);
        String response = makeTwitchQuery(userIdQueryUrl);
        return getUserIdFromJson(response);
    }

    private static String makeTwitchQuery(URL url) {

        String response;

        HttpsURLConnection urlConnection = openHttpConnection(url);
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

    private static HttpsURLConnection openHttpConnection(URL url) {

        HttpsURLConnection urlConnection = null;

        try {

            urlConnection = setupHttpsURLConnection(url);
            urlConnection.connect();

            // Check the response code, log and return null if it's bad
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e(LOG_TAG,  "Error response code: " + responseCode);
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return urlConnection;
    }

    private static InputStream getInputStreamFromConnection(HttpsURLConnection connection) {
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            closeConnections(connection, null);
            Log.e(LOG_TAG, e.toString());
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

    private static void closeConnections(HttpsURLConnection urlConnection, InputStream inputStream) {

        if (urlConnection != null)
            urlConnection.disconnect();

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private static URL buildUrl(String query, int queryType) {

        Uri uri = Uri.EMPTY;
        if (queryType == QUERY_TYPE_CHANNEL)
            uri = buildChannelQueryUri(query);
        else if (queryType == QUERY_TYPE_STREAM)
            uri = buildStreamQueryUri(query);
        else if (queryType == QUERY_TYPE_STREAM_MULTIPLE)
            uri = buildMultiStreamQueryUri(query);
        else if (queryType == QUERY_TYPE_USER_ID)
            uri = buildUserIdQueryUri(query);

        return getUrlFromUri(uri);
    }

    private static URL buildUserFollowsUrl(String channelId, int offset) {
        Uri uri = buildUserFollowsQueryUri(channelId, offset);

        return getUrlFromUri(uri);
    }

    @Nullable
    private static URL getUrlFromUri(Uri uri) {
        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return url;
    }

    private static Uri buildStreamQueryUri(String channelName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS)
                .appendPath(channelName)
                .appendQueryParameter(PARAM_API_VERSION, API_VERSION)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .build();
    }

    private static Uri buildChannelQueryUri(String channelName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_CHANNELS)
                .appendPath(channelName)
                .appendQueryParameter(PARAM_API_VERSION, API_VERSION)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .build();
    }

    private static Uri buildUserFollowsQueryUri(String userName, int offset) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_USERS)
                .appendPath(userName)
                .appendEncodedPath(PATH_FOLLOWS_CHANNELS)
                .appendQueryParameter(PARAM_API_VERSION, API_VERSION)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .appendQueryParameter(PARAM_LIMIT, LIMIT_MAX)
                .appendQueryParameter(PARAM_OFFSET, Integer.toString(offset))
                .build();
    }

    private static Uri buildMultiStreamQueryUri(String commaSeparatedChannelIds) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS)
                .appendQueryParameter(PARAM_API_VERSION, API_VERSION)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .appendQueryParameter(PARAM_LIMIT, LIMIT_MAX)
                .appendQueryParameter(PARAM_STREAM_TYPE, STREAM_TYPE_LIVE)
                .appendQueryParameter(PARAM_CHANNEL, commaSeparatedChannelIds)
                .build();
    }

    private static Uri buildUserIdQueryUri(String userName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_USERS)
                .appendQueryParameter(PARAM_API_VERSION, API_VERSION)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .appendQueryParameter(PARAM_LOGIN, userName)
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

            int id = channelData.getInt("_id");
            String displayName = channelData.getString("display_name");
            String name = channelData.getString("name");
            String logoUrl = channelData.getString("logo");
            String streamUrl = channelData.getString("url");

            return new Channel(id, displayName, name, logoUrl, streamUrl,
                    ChannelContract.ChannelEntry.IS_NOT_PINNED);

        }catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return null;
    }

    private static Stream getStreamFromJson(String jsonResponse) {

        if (jsonResponse == null)
            return null;

        try {

            JSONObject streamData = new JSONObject(jsonResponse);
            JSONObject channelData = streamData.getJSONObject("channel");

            long id = channelData.getLong("_id");
            String game = streamData.getString("game");
            int viewers = streamData.getInt("viewers");
            String status = channelData.getString("status");
            String streamType = streamData.getString("stream_type");
            String startTime = streamData.getString("created_at");

            return new Stream(id, game, viewers, status, startTime, streamType);

        }catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return null;
    }

    private static long getUserIdFromJson(String jsonResponse) {

        try {

            // TODO fix java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
            JSONObject resultJson = new JSONObject(jsonResponse);
            return resultJson.getJSONArray("users").getJSONObject(0).getLong("_id");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return 0;
    }

}
