package mseffner.twitchnotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;


public class ChannelDb {

    private static final String TABLE = ChannelEntry.TABLE_NAME;
    private ChannelDbHelper dbHelper;


    public ChannelDb(Context context) {
        dbHelper = ChannelDbHelper.getInstance(context);
    }

    public List<Channel> getAllChannels() {

        List<Channel> channelList = new ArrayList<>();

        String sortOrder =
            "CASE " + ChannelEntry.COLUMN_STREAM_TYPE +  // Show online streams first
                " WHEN " + ChannelEntry.STREAM_TYPE_LIVE + " THEN 1" +
                " WHEN " + ChannelEntry.STREAM_TYPE_PLAYLIST + " THEN 1" +
                " WHEN " + ChannelEntry.STREAM_TYPE_VODCAST + " THEN 1" +
                " ELSE 0" +
            " END, " +
            ChannelEntry.COLUMN_VIEWERS + ", " +  // Sort by viewer count (offline streams have 0)
            ChannelEntry.COLUMN_DISPLAY_NAME + " COLLATE NOCASE";  // Break ties by display_name

        Cursor cursor = query(null, null, null, sortOrder);

        while (cursor.moveToNext()) {

            // Get all the data from the cursor
            int id = cursor.getInt(cursor.getColumnIndex(ChannelEntry._ID));
            String name = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_NAME));
            String displayName = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_DISPLAY_NAME));
            String channelUrl = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_CHANNEL_URL));
            String logoUrl = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_LOGO_URL));
            byte[] logoByteArray = cursor.getBlob(cursor.getColumnIndex(ChannelEntry.COLUMN_LOGO_BMP));
            int pinned = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_PINNED));
            int streamType = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_STREAM_TYPE));
            String status = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_STATUS));
            String game = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_GAME));
            int viewers = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_VIEWERS));
            long createdAt = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_CREATED_AT));

            // Build the Channel object from the data
            Channel channel = new Channel(id, displayName, name, logoUrl, channelUrl);
            if (streamType != ChannelEntry.STREAM_TYPE_OFFLINE)
                channel.setStream(new Stream(game, viewers, status, createdAt, streamType));
            channel.setLogoBmp(getBitmapFromByteArray(logoByteArray));

            channelList.add(channel);
        }

        return channelList;
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

    private Bitmap getBitmapFromByteArray(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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
