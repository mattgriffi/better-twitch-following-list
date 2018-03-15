package mseffner.twitchnotifier.data;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class ChannelDbHelper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "channels.db";
    private static int DATABASE_VERSION = 2;

    public ChannelDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
}
