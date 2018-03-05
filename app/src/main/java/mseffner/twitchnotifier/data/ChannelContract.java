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

    public static class FollowEntry implements BaseColumns {
        private FollowEntry() {}

        public static final String TABLE_NAME = "follows";
        public static final String COLUMN_PINNED = "pinned";
        public static final String COLUMN_DIRTY = "dirty";

        public static final int IS_NOT_PINNED = 0;
        public static final int IS_PINNED = 1;
        public static final int CLEAN = 0;
        public static final int DIRTY = 1;
    }

    public static class UserEntry implements BaseColumns {
        private UserEntry() {}

        public static final String TABLE_NAME = "users";
        public static final String COLUMN_LOGIN = "login";
        public static final String COLUMN_DISPLAY_NAME = "display_name";
        public static final String COLUMN_PROFILE_IMAGE_URL = "profile_image_url";
    }

    public static class GameEntry implements BaseColumns {
        private GameEntry() {}

        public static final String TABLE_NAME = "games";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_BOX_ART_URL = "box_art_url";
    }

    public static class StreamEntry implements BaseColumns {
        private StreamEntry() {}

        public static final String TABLE_NAME = "streams";
        public static final String COLUMN_GAME_ID = "game_id";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_VIEWER_COUNT = "viewer_count";
        public static final String COLUMN_STARTED_AT = "started_at";
        public static final String COLUMN_LANGUAGE = "language";
        public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";

        public static final int STREAM_TYPE_OFFLINE = 0;
        public static final int STREAM_TYPE_LIVE = 1;
        public static final int STREAM_TYPE_RERUN = 2;
    }
}
