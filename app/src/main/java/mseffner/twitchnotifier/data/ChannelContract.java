package mseffner.twitchnotifier.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class ChannelContract {

    public static final String CONTENT_AUTHORITY = "mseffner.twitchnotifier";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_CHANNELS = "channels";

    // MIME types for ChannelProvider
    public static final String CONTENT_CHANNELS_LIST_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHANNELS;
    public static final String CONTENT_CHANNELS_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHANNELS;

    private ChannelContract() {}

    public static class ChannelEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_CHANNELS);

        // General channel data
        public static final String TABLE_NAME = "channels";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_DISPLAY_NAME = "display_name";
        public static final String COLUMN_LOGO_URL = "logo_url";
        public static final String COLUMN_CHANNEL_URL = "channel_url";
        public static final String COLUMN_LOGO_BMP = "logo_bmp";

        // Stream data
        public static final String COLUMN_GAME = "game";
        public static final String COLUMN_VIEWERS = "viewers";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_STREAM_TYPE = "stream_type";
        public static final String COLUMN_CREATED_AT = "created_at";  // This should be a unix timestamp

        public static final int STREAM_TYPE_OFFLINE = 0;
        public static final int STREAM_TYPE_LIVE = 1;
        public static final int STREAM_TYPE_VODCAST = 2;
        public static final int STREAM_TYPE_PLAYLIST = 3;
    }
}
