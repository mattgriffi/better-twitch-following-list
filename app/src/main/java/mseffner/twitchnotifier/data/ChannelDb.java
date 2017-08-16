package mseffner.twitchnotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;


public class ChannelDb {

    private static final String TABLE = ChannelEntry.TABLE_NAME;
    private ChannelDbHelper dbHelper;


    public ChannelDb(Context context) {
        dbHelper = ChannelDbHelper.getInstance(context);
    }

    public Set<Integer> getOfflineIdSet() {

        String[] projection = new String[] {ChannelEntry._ID};
        String selection = ChannelEntry.COLUMN_STREAM_TYPE + "=?";
        String[] selectionArgs = new String[] {Integer.toString(ChannelEntry.STREAM_TYPE_OFFLINE)};

        Cursor cursor = query(projection, selection, selectionArgs, null);

        Set<Integer> idSet = new HashSet<>();
        int idColumnIndex = cursor.getColumnIndex(ChannelEntry._ID);

        while (cursor.moveToNext()) {
            idSet.add(cursor.getInt(idColumnIndex));
        }

        cursor.close();

        return idSet;
    }

    public int[] getAllChannelIds() {

        Cursor cursor = query(new String[]{ChannelEntry._ID}, null, null, null);

        int[] idArray = new int[cursor.getCount()];
        int idColumnIndex = cursor.getColumnIndex(ChannelEntry._ID);

        int i = 0;
        while (cursor.moveToNext()) {
            idArray[i++] = cursor.getInt(idColumnIndex);
        }

        cursor.close();

        return idArray;
    }

    public List<Channel> getAllChannels() {

        List<Channel> channelList = new ArrayList<>();

        String sortOrder =
            "CASE " + ChannelEntry.COLUMN_STREAM_TYPE +  // Show online streams first
                " WHEN " + ChannelEntry.STREAM_TYPE_LIVE + " THEN 1" +
                " WHEN " + ChannelEntry.STREAM_TYPE_PLAYLIST + " THEN 1" +
                " WHEN " + ChannelEntry.STREAM_TYPE_VODCAST + " THEN 1" +
                " ELSE 0" +
            " END DESC, " +
            "CASE " + ChannelEntry.COLUMN_PINNED + // Show pinned streams first
                " WHEN " + ChannelEntry.IS_PINNED + " THEN 0" +
                " WHEN " + ChannelEntry.IS_NOT_PINNED + " THEN 1 " +
            " END, " +
            ChannelEntry.COLUMN_VIEWERS + " DESC, " +  // Sort by viewer count (offline streams have 0)
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
            Channel channel = new Channel(id, displayName, name, logoUrl, channelUrl, pinned);
            if (streamType != ChannelEntry.STREAM_TYPE_OFFLINE)
                channel.setStream(new Stream(id, game, viewers, status, createdAt, streamType));
            channel.setLogoBmp(getBitmapFromByteArray(logoByteArray));

            channelList.add(channel);
        }

        cursor.close();

        return channelList;
    }

    public boolean insertChannel(Channel channel) {

        if (channel == null)
            return false;

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

    public long updateStreamData(Stream stream) {

        if (stream == null)
            return 0;

        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_GAME, stream.getCurrentGame());
        values.put(ChannelEntry.COLUMN_VIEWERS, stream.getCurrentViewers());
        values.put(ChannelEntry.COLUMN_STATUS, stream.getStatus());
        values.put(ChannelEntry.COLUMN_STREAM_TYPE, stream.getStreamType());
        values.put(ChannelEntry.COLUMN_CREATED_AT, stream.getCreatedAt());

        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(stream.getChannelId())};

        return update(values, selection, selectionArgs);
    }

    public void toggleChannelPin(Channel channel) {

        if (channel == null)
            return;

        ContentValues values = new ContentValues();
        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(channel.getId())};

        String[] projection = {ChannelEntry.COLUMN_PINNED};
        Cursor channelCursor = query(projection, selection, selectionArgs, null);
        channelCursor.moveToFirst();
        int currentPinnedStatus = channelCursor.getInt(channelCursor.getColumnIndex(ChannelEntry.COLUMN_PINNED));
        channelCursor.close();

        if (currentPinnedStatus == ChannelEntry.IS_PINNED) {
            values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_NOT_PINNED);
        } else {
            values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_PINNED);
        }

        update(values, selection, selectionArgs);
    }

    public long deleteAllChannels() {

        return delete(null, null);
    }

    /**
     * This method will set all of the Stream data for every row to their default values:
     *      stream_type = 0
     *      status = ""
     *      game = ""
     *      viewers = 0
     *      created_at = 0
     *
     * This method should be called before updating the database with fresh Stream data. For the
     * purpose of making notifications, be sure to call this method AFTER getting the old data to
     * compare against (use getOfflineIdSet).
     */
    public void resetAllStreamData() {

        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_STREAM_TYPE, ChannelEntry.STREAM_TYPE_OFFLINE);
        values.put(ChannelEntry.COLUMN_GAME, "");
        values.put(ChannelEntry.COLUMN_VIEWERS, 0);
        values.put(ChannelEntry.COLUMN_STATUS, "");
        values.put(ChannelEntry.COLUMN_CREATED_AT, 0);

        update(values, null, null);
    }

    private byte[] getByteArrayFromBitmap(Bitmap bmp) {

        if (bmp == null)
            return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Bitmap newBitmap = bmp;
        if (bmp.hasAlpha()) {
            // Draw the bmp onto a white background, otherwise transparent backgrounds may turn black
            newBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
            Canvas canvas = new Canvas(newBitmap);
            // TODO use a color resource to ensure consistency with other API versions
            canvas.drawColor(Color.parseColor("#FAFAFA"));
            canvas.drawBitmap(bmp, 0, 0, null);
            bmp.recycle();
        }

        newBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);  // Compress it to save space
        newBitmap.recycle();
        return stream.toByteArray();
    }

    private Bitmap getBitmapFromByteArray(byte[] bytes) {
        if (bytes == null)
            return null;
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
