package mseffner.twitchnotifier.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;


public class ChannelDbHelper extends SQLiteOpenHelper {

    private static ChannelDbHelper instance;

    private static final String DATABASE_NAME = "channels.db";
    private static int DATABASE_VERSION = 1;

    private static final String DEFAULT_LOGO_URL = "\"https://www-cdn.jtvnw.net/images/xarth/404_user_300x300.png\"";

    private ChannelDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static ChannelDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ChannelDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        String CREATE_CHANNELS_TABLE =
            "CREATE TABLE " + ChannelEntry.TABLE_NAME +
            " (" +
            // The ID will be the ID returned by the Twitch API, which allows me to use it
            // to specify channels in future API requests.
            ChannelEntry._ID + " INTEGER PRIMARY KEY, " +
            ChannelEntry.COLUMN_NAME + " TEXT NOT NULL, " +
            ChannelEntry.COLUMN_DISPLAY_NAME + " TEXT NOT NULL DEFAULT \"unknown_name\", " +
            ChannelEntry.COLUMN_CHANNEL_URL + " TEXT NOT NULL, " +
            ChannelEntry.COLUMN_LOGO_URL + " TEXT NOT NULL DEFAULT " + DEFAULT_LOGO_URL +  ", " +
            ChannelEntry.COLUMN_LOGO_BMP + " BLOB, " +
            ChannelEntry.COLUMN_PINNED + " INTEGER NOT NULL DEFAULT " + ChannelEntry.IS_NOT_PINNED + ", " +
            ChannelEntry.COLUMN_STREAM_TYPE + " INTEGER NOT NULL DEFAULT " + ChannelEntry.STREAM_TYPE_OFFLINE + ", " +
            ChannelEntry.COLUMN_STATUS + " TEXT NOT NULL DEFAULT \"\", " +
            ChannelEntry.COLUMN_GAME + " TEXT NOT NULL DEFAULT \"\", " +
            ChannelEntry.COLUMN_VIEWERS + " INTEGER NOT NULL DEFAULT 0, " +
            // CREATED_AT will be a Unix timestamp showing when the stream went live.
            ChannelEntry.COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0" +
            ");";

        database.execSQL(CREATE_CHANNELS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int i, int i1) {}
}
