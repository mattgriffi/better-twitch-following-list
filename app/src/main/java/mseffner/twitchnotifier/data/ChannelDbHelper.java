package mseffner.twitchnotifier.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;

public class ChannelDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "channels.db";
    private static int DATABASE_VERSION = 2;

    private static final String DEFAULT_LOGO_URL = "\"https://www-cdn.jtvnw.net/images/xarth/404_user_300x300.png\"";

    public ChannelDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(getCreateStatement(ChannelEntry.TABLE_NAME));
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.beginTransaction();
        try {
            switch (oldVersion) {
                case 1:
                    upgradeFrom1to2(database);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static String getCreateStatement(String tableName) {
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
        String create = getCreateStatement("temp");
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
}
