package mseffner.twitchnotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ChannelDb {

    private static final String TABLE = ChannelContract.ChannelEntry.TABLE_NAME;
    private ChannelDbHelper dbHelper;


    public ChannelDb(Context context) {
        dbHelper = ChannelDbHelper.getInstance(context);
    }

    public Cursor query(String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.query(TABLE, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public long insert(ContentValues contentValues) {

        // TODO input validation

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.insert(TABLE, null, contentValues);
    }

    public long update(ContentValues contentValues, String selection, String[] selectionArgs) {

        // TODO input validation

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.update(TABLE, contentValues, selection, selectionArgs);
    }

    public long delete(String selection, String[] selectionArgs) {

        // TODO input validation

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.delete(TABLE, selection, selectionArgs);
    }
}
