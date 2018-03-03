package mseffner.twitchnotifier.data;

import android.provider.BaseColumns;

public final class ChannelContract {

    private ChannelContract() {}

    public static class ChannelEntry implements BaseColumns {

        private ChannelEntry() {}

        // General channel data
        public static final String TABLE_NAME = "channels";
        public static final String COLUMN_DISPLAY_NAME = "display_name";
        public static final String COLUMN_LOGIN_NAME = "login_name";
        public static final String COLUMN_LOGO_URL = "logo_url";
        public static final String COLUMN_CHANNEL_URL = "channel_url";
        public static final String COLUMN_PINNED = "pinned";

        // Stream data
        public static final String COLUMN_GAME = "game";
        public static final String COLUMN_GAME_ID = "game_id";
        public static final String COLUMN_VIEWERS = "viewers";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_STREAM_TYPE = "stream_type";
        public static final String COLUMN_CREATED_AT = "created_at";

        // Used to keep track of updates
        public static final String COLUMN_DIRTY = "dirty";

        // Values
        public static final int STREAM_TYPE_OFFLINE = 0;
        public static final int STREAM_TYPE_LIVE = 1;
        public static final int STREAM_TYPE_RERUN = 2;
        public static final int IS_NOT_PINNED = 0;
        public static final int IS_PINNED = 1;
        public static final int CLEAN = 0;
        public static final int DIRTY = 1;
    }
}
