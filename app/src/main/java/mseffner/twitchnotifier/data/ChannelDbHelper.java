package mseffner.twitchnotifier.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;
import mseffner.twitchnotifier.data.ChannelContract.FollowEntry;
import mseffner.twitchnotifier.data.ChannelContract.GameEntry;
import mseffner.twitchnotifier.data.ChannelContract.UserEntry;
import mseffner.twitchnotifier.data.ChannelContract.StreamEntry;

public class ChannelDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "channels.db";
    private static int DATABASE_VERSION = 2;

    private static final String DEFAULT_LOGO_URL = "\"https://www-cdn.jtvnw.net/images/xarth/404_user_300x300.png\"";

    public ChannelDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        // TODO create version 3 tables
        database.execSQL(getVersion2CreateStatement(ChannelEntry.TABLE_NAME));
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.beginTransaction();
        try {
            switch (oldVersion) {
                case 1:
                    upgradeFrom1to2(database);
                case 2:
                    upgradeFrom2to3(database);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static String getFollowsCreateStatement(String tableName) {
        return "CREATE TABLE " + tableName +
            " (" +
                FollowEntry._ID + " INTEGER PRIMARY KEY, " +
                FollowEntry.COLUMN_PINNED + " INTEGER NOT NULL DEFAULT " + FollowEntry.IS_NOT_PINNED + ", " +
                FollowEntry.COLUMN_DIRTY + " INTEGER NOT NULL DEFAULT " + FollowEntry.CLEAN +
            ");";
    }

    private static String getUsersCreateStatement(String tableName) {
        return "CREATE TABLE " + tableName +
            " (" +
                UserEntry._ID + " INTEGER PRIMARY KEY, " +
                UserEntry.COLUMN_LOGIN + " TEXT NOT NULL DEFAULT \"\", " +
                UserEntry.COLUMN_DISPLAY_NAME + " TEXT NOT NULL DEFAULT \"\", " +
                UserEntry.COLUMN_PROFILE_IMAGE_URL + " TEXT NOT NULL DEFAULT \"\"" +
            ");";
    }

    private static String getStreamsCreateStatement(String tableName) {
        return "CREATE TABLE " + tableName +
            " (" +
                StreamEntry._ID + " INTEGER PRIMARY KEY, " +
                StreamEntry.COLUMN_GAME_ID + " INTEGER NOT NULL DEFAULT 0, " +
                StreamEntry.COLUMN_TYPE + " TEXT NOT NULL DEFAULT " + StreamEntry.STREAM_TYPE_OFFLINE + ", " +
                StreamEntry.COLUMN_TITLE + " TEXT NOT NULL DEFAULT \"\"" +
                StreamEntry.COLUMN_VIEWER_COUNT + " INTEGER NOT NULL DEFAULT 0" +
                StreamEntry.COLUMN_STARTED_AT + " INTEGER NOT NULL DEFAULT 0" +
                StreamEntry.COLUMN_LANGUAGE + " TEXT NOT NULL DEFAULT \"en\"" +
                StreamEntry.COLUMN_THUMBNAIL_URL + " TEXT NOT NULL DEFAULT \"\"" +
            ");";
    }

    private static String getGamesCreateStatement(String tableName) {
        return "CREATE TABLE " + tableName +
            " (" +
                GameEntry._ID + " INTEGER PRIMARY KEY, " +
                GameEntry.COLUMN_NAME + " TEXT NOT NULL DEFAULT \"\"" +
                GameEntry.COLUMN_BOX_ART_URL + " TEXT NOT NULL DEFAULT \"\"" +
            ");";
    }

    private static void createVersion3(SQLiteDatabase database) {
        // TODO implement
    }

    private static String getVersion2CreateStatement(String tableName) {
        return "CREATE TABLE " + tableName +
            " (" +
                // The ID will be the ID returned by the Twitch API, which allows me to use it
                // to specify channels in future API requests.
                ChannelEntry._ID + " INTEGER PRIMARY KEY, " +
                ChannelEntry.COLUMN_DISPLAY_NAME + " TEXT NOT NULL DEFAULT \"\", " +
                ChannelEntry.COLUMN_LOGIN_NAME + " TEXT NOT NULL DEFAULT \"\", " +
                ChannelEntry.COLUMN_CHANNEL_URL + " TEXT NOT NULL DEFAULT \"\", " +
                ChannelEntry.COLUMN_LOGO_URL + " TEXT NOT NULL DEFAULT " + DEFAULT_LOGO_URL +  ", " +
                ChannelEntry.COLUMN_PINNED + " INTEGER NOT NULL DEFAULT " + ChannelEntry.IS_NOT_PINNED + ", " +
                ChannelEntry.COLUMN_STREAM_TYPE + " INTEGER NOT NULL DEFAULT " + ChannelEntry.STREAM_TYPE_OFFLINE + ", " +
                ChannelEntry.COLUMN_STATUS + " TEXT NOT NULL DEFAULT \"\", " +
                ChannelEntry.COLUMN_GAME + " TEXT NOT NULL DEFAULT \"\", " +
                ChannelEntry.COLUMN_GAME_ID + " INTEGER NOT NULL DEFAULT 0, " +
                ChannelEntry.COLUMN_VIEWERS + " INTEGER NOT NULL DEFAULT 0, " +
                // CREATED_AT will be a Unix timestamp showing when the stream went live.
                ChannelEntry.COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0, " +
                ChannelEntry.COLUMN_DIRTY + " INTEGER NOT NULL DEFAULT " + ChannelEntry.CLEAN +
            ");";
    }

    private static void upgradeFrom1to2(SQLiteDatabase database) {
        String create = getVersion2CreateStatement("temp");
        String columnList = ChannelEntry._ID + ", " + ChannelEntry.COLUMN_DISPLAY_NAME + ", " +
                ChannelEntry.COLUMN_CHANNEL_URL + ", " + ChannelEntry.COLUMN_LOGO_URL + ", " +
                ChannelEntry.COLUMN_PINNED + ", " + ChannelEntry.COLUMN_STREAM_TYPE + ", " +
                ChannelEntry.COLUMN_STATUS + ", " + ChannelEntry.COLUMN_VIEWERS + ", " +
                ChannelEntry.COLUMN_CREATED_AT + ", " + ChannelEntry.COLUMN_GAME;
        String insert = "INSERT INTO temp (" + columnList + ") SELECT " + columnList +
                " FROM " + ChannelEntry.TABLE_NAME + ";";
        String drop = "DROP TABLE " + ChannelEntry.TABLE_NAME + ";";
        String rename = "ALTER TABLE temp RENAME TO " + ChannelEntry.TABLE_NAME + ";";

        // Create temp table
        database.execSQL(create);
        // Insert old data into temp
        database.execSQL(insert);
        // Drop old table
        database.execSQL(drop);
        // Rename temp table
        database.execSQL(rename);
    }

    private static void upgradeFrom2to3(SQLiteDatabase database) {
        // TODO implement
    }
}
