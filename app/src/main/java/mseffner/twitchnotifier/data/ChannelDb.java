package mseffner.twitchnotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class ChannelDb {

    private static final String TABLE = ChannelContract.ChannelEntry.TABLE_NAME;
    private ChannelDbHelper dbHelper;


    public ChannelDb(Context context) {
        dbHelper = new ChannelDbHelper(context);
    }

    public Cursor query(String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        return null;
    }

    public int insert(ContentValues contentValues) {
        return 0;
    }

    public int update(ContentValues contentValues, String selection, String[] selectionArgs) {
        return 0;
    }

    public int delete(String selection, String[] selectionArgs) {
        return 0;
    }
}
