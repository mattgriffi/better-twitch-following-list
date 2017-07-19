package mseffner.twitchnotifier.data;

import android.net.Uri;
import android.provider.BaseColumns;

public final class ChannelContract {

    public static final String CONTENT_AUTHORITY = "mseffner.twitchnotifier";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private ChannelContract() {}

    public static class ChannelEntry implements BaseColumns {

        public static final String TABLE_NAME = "channels";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_DISPLAY_NAME = "display_name";
        public static final String COLUMN_LOGO_URL = "logo_url";
        public static final String COLUMN_CHANNEL_URL = "channel_url";
        public static final String COLUMN_LOGO_BMP = "logo_bmp";
    }

    public static class StreamEntry implements BaseColumns {

        public static final String TABLE_NAME = "streams";
        public static final String COLUMN_GAME = "game";
        public static final String COLUMN_VIEWERS = "viewers";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_STREAM_TYPE = "stream_type";
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final int STREAM_TYPE_LIVE = 0;
        public static final int STREAM_TYPE_PLAYLIST = 1;
        public static final int STREAM_TYPE_VODCAST = 2;
        public static final int STREAM_TYPE_UNKNOWN = 3;
    }
}
