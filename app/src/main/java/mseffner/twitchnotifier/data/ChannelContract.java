package mseffner.twitchnotifier.data;

import android.provider.BaseColumns;

public final class ChannelContract {
    private ChannelContract() {}

    public static class FollowEntry implements BaseColumns {
        private FollowEntry() {}

        public static final String TABLE_NAME = "follows";
        public static final String COLUMN_PINNED = "pinned";
        public static final String COLUMN_DIRTY = "dirty";

        public static final int NOT_PINNED = 0;
        public static final int PINNED = 1;
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
        public static final String COLUMN_FAVORITE = "favorite";

        public static final int NOT_FAVORITED = 0;
        public static final int FAVORITED = 1;
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

    public static class StreamLegacyEntry implements BaseColumns {
        private StreamLegacyEntry() {}

        public static final String TABLE_NAME = "streams_legacy";
        public static final String COLUMN_GAME = "game";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_VIEWER_COUNT = "viewer_count";
        public static final String COLUMN_STARTED_AT = "started_at";
        public static final String COLUMN_LANGUAGE = "language";
        public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";
        public static final String COLUMN_GAME_FAVORITE = "game_favorite";
    }
}
