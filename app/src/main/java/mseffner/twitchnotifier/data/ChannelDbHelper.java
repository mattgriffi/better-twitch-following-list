package mseffner.twitchnotifier.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;
import mseffner.twitchnotifier.data.ChannelContract.StreamEntry;


public class ChannelDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "channels.db";
    private static int DATABASE_VERSION = 1;

    private static final String DEFAULT_LOGO_URL = "http://www-cdn.jtvnw.net/images/xarth/404_user_300x300.png";

    public ChannelDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {


        String CREATE_CHANNELS_TABLE =
                        "CREATE TABLE " +
                        ChannelEntry.TABLE_NAME + " (" +
                        ChannelEntry._ID + " INTEGER PRIMARY KEY, " +
                        ChannelEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                        ChannelEntry.COLUMN_DISPLAY_NAME + " TEXT NOT NULL DEFAULT \"unknown_name\", " +
                        ChannelEntry.COLUMN_CHANNEL_URL + " TEXT NOT NULL, " +
                        ChannelEntry.COLUMN_LOGO_URL + " TEXT NOT NULL DEFAULT " + DEFAULT_LOGO_URL +  ", " +
                        ChannelEntry.COLUMN_LOGO_BMP + " BLOB" + ");";

        String CREATE_STREAMS_TABLE =
                        "CREATE TABLE " +
                        StreamEntry.TABLE_NAME + " (" +
                        StreamEntry._ID + " INTEGER PRIMARY KEY, " +
                        StreamEntry.COLUMN_GAME + " TEXT NOT NULL DEFAULT \"\", " +
                        StreamEntry.COLUMN_VIEWERS + " INTEGER NOT NULL DEFAULT 0, " +
                        StreamEntry.COLUMN_STATUS + " TEXT NOT NULL DEFAULT \"\", " +
                        StreamEntry.COLUMN_STREAM_TYPE + " INTEGER NOT NULL DEFAULT " + StreamEntry.STREAM_TYPE_UNKNOWN + ", " +
                        StreamEntry.COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0" + ");";  // Unix timestamp

        database.execSQL(CREATE_CHANNELS_TABLE);
        database.execSQL(CREATE_STREAMS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int i, int i1) {
        DATABASE_VERSION++;
    }
}