package mseffner.twitchnotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;


public class ChannelDb {

    private static final String TABLE = ChannelEntry.TABLE_NAME;
    private ChannelDbHelper dbHelper;


    public ChannelDb(Context context) {
        dbHelper = ChannelDbHelper.getInstance(context);
    }

    public boolean insertChannel(Channel channel) {

        ContentValues values = new ContentValues();

        values.put(ChannelEntry._ID, channel.getId());
        values.put(ChannelEntry.COLUMN_NAME, channel.getName());
        values.put(ChannelEntry.COLUMN_DISPLAY_NAME, channel.getDisplayName());
        values.put(ChannelEntry.COLUMN_LOGO_URL, channel.getLogoUrl());
        values.put(ChannelEntry.COLUMN_CHANNEL_URL, channel.getStreamUrl());
        values.put(ChannelEntry.COLUMN_PINNED, channel.getPinned());
        values.put(ChannelEntry.COLUMN_LOGO_BMP, getByteArrayFromBitmap(channel.getLogoBmp()));

        Stream stream = channel.getStream();
        if (stream != null) {
            values.put(ChannelEntry.COLUMN_GAME, stream.getCurrentGame());
            values.put(ChannelEntry.COLUMN_VIEWERS, stream.getCurrentViewers());
            values.put(ChannelEntry.COLUMN_STATUS, stream.getStatus());
            values.put(ChannelEntry.COLUMN_STREAM_TYPE, stream.getStreamType());
            values.put(ChannelEntry.COLUMN_CREATED_AT, stream.getCreatedAt());
        }

        long numRowsInserted = insert(values);
        return numRowsInserted == 1;
    }

    private byte[] getByteArrayFromBitmap(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    private Cursor query(String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.query(TABLE, projection, selection, selectionArgs, null, null, sortOrder);
    }

    private long insert(ContentValues contentValues) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.insert(TABLE, null, contentValues);
    }

    private long update(ContentValues contentValues, String selection, String[] selectionArgs) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.update(TABLE, contentValues, selection, selectionArgs);
    }

    private long delete(String selection, String[] selectionArgs) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.delete(TABLE, selection, selectionArgs);
    }
}
