package mseffner.twitchnotifier.networking;


import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import mseffner.twitchnotifier.settings.SettingsManager;

/**
 * URLTools contains methods for creating URLs.
 */
public class URLTools {

    // Base URLs
    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/helix";
    private static final String TWITCH_STREAM_BASE_URL = "https://www.twitch.tv";

    // Paths
    private static final String PATH_STREAMS = "streams";
    private static final String PATH_USERS = "users";
    private static final String PATH_GAMES = "games";
    private static final String PATH_FOLLOWS = "users/follows";

    // Parameters
    private static final String PARAM_FIRST = "first";
    private static final String PARAM_AFTER = "after";
    private static final String PARAM_FROM_ID = "from_id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_LOGIN = "login";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_USER_ID = "user_id";
    private static final String PARAM_USER_LOGIN = "user_login";

    // Arguments
    private static final String FIRST_MAX = "100";
    private static final String FIRST_DEFAULT = "25";

    /**
     * @return  url to get the user information for the app user
     */
    public static String getUserIdUrl() {
        String username = SettingsManager.getUsername();
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_USERS)
                .appendQueryParameter(PARAM_LOGIN, username)
                .build().toString();
    }

    /**
     * @return  url to get info for games in idArray
     */
    public static String getGamesUrl(long[] idArray) {
        Uri.Builder builder =  Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_GAMES);
        for (long id : idArray)
            builder.appendQueryParameter(PARAM_ID, Long.toString(id));
        return builder.build().toString();
    }

    /**
     * @return  url to get info for users in idArray
     */
    public static String getUsersUrl(long[] idArray) {
        Uri.Builder builder =  Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_USERS);
        for (long id : idArray)
            builder.appendQueryParameter(PARAM_ID, Long.toString(id));
        return builder.build().toString();
    }

    /**
     * @return  url to get info for streams in idArray
     */
    public static String getStreamsUrl(long[] idArray) {
        Uri.Builder builder =  Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS);
        for (long id : idArray)
            builder.appendQueryParameter(PARAM_USER_ID, Long.toString(id));
        return builder.build().toString();
    }

    /**
     * @return url to get top 100 streams
     */
    public static String getTopStreamsUrl() {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS)
                .appendQueryParameter(PARAM_FIRST, FIRST_MAX)
                .build().toString();
    }

    /**
     * @return url to get first 100 followed channels after cursor
     */
    public static String getFollowsUrl(@Nullable String cursor) {
        String userId = Long.toString(SettingsManager.getUsernameId());
        Uri.Builder builder = Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendEncodedPath(PATH_FOLLOWS)
                .appendQueryParameter(PARAM_FIRST, FIRST_MAX)
                .appendQueryParameter(PARAM_FROM_ID, userId);
        if (cursor != null)
            builder.appendQueryParameter(PARAM_AFTER, cursor);
        return builder.toString();
    }

    /**
     * Creates the url for a Twitch channel.
     * Example: https://www.twitch.tv/cirno_tv
     *
     * @param channelName   login name of the channel (must be login, not display)
     * @return              url for the channel
     */
    public static String getStreamUrl(String channelName) {
        return Uri.parse(TWITCH_STREAM_BASE_URL).buildUpon()
                .appendPath(channelName)
                .build().toString();
    }
}
