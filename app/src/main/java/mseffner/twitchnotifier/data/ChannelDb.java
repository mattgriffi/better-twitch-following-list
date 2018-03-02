package mseffner.twitchnotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;
import mseffner.twitchnotifier.settings.SettingsManager;


public class ChannelDb {

    private static ChannelDbHelper dbHelper;


    private ChannelDb() {}

    public static void initialize(Context context) {
        dbHelper = new ChannelDbHelper(context.getApplicationContext());
    }

    public static void destroy() {
        dbHelper = null;
    }

    public static void updateNewChannelData(List<Channel> channelList) {
        // Get the ids already in the database
        int[] existingIds = getAllChannelIds();
        Set<Integer> existingIdSet = new HashSet<>(existingIds.length);

        for (int id : existingIds)
            existingIdSet.add(id);

        // Get the ids of the new Channel list
        Set<Integer> newIdSet = new HashSet<>(channelList.size());
        for (Channel channel : channelList)
            newIdSet.add(channel.getId());

        // Delete any channel from the database that isn't in the new list
        // (This means that the channel was unfollowed)
        for (int existingId : existingIds)
            if (!newIdSet.contains(existingId))
                deleteChannel(existingId);

        // Add any channels that aren't in the database
        // (This means that the channel was newly followed)
        for (Channel channel : channelList)
            if (!existingIdSet.contains(channel.getId()))
                insertChannel(channel);
    }

    public static int[] getAllChannelIds() {
        Cursor cursor = query(new String[]{ChannelEntry._ID}, null, null, null);

        int[] idArray = new int[cursor.getCount()];
        int idColumnIndex = cursor.getColumnIndex(ChannelEntry._ID);

        int i = 0;
        while (cursor.moveToNext())
            idArray[i++] = cursor.getInt(idColumnIndex);
        cursor.close();

        return idArray;
    }

    public static List<Channel> getAllChannels() {
        // Get the vodcast display setting
        boolean vodcastOnline = SettingsManager.getRerunSetting() != SettingsManager.RERUN_OFFLINE;

        String sortOrder =
            // Sort by online/offline first
            "CASE " + ChannelEntry.COLUMN_STREAM_TYPE +
                // Online channels first
                " WHEN " + ChannelEntry.STREAM_TYPE_LIVE + " THEN 0" +
                // Show vodcasts as online or offline depending on the setting
                (vodcastOnline ? " WHEN " + ChannelEntry.STREAM_TYPE_RERUN + " THEN 0" : "") +
                " ELSE 1" +
            " END, " +
            // Show pinned streams before unpinned
            "CASE " + ChannelEntry.COLUMN_PINNED +
                " WHEN " + ChannelEntry.IS_PINNED + " THEN 0" +
                " WHEN " + ChannelEntry.IS_NOT_PINNED + " THEN 1 " +
            " END, " +
            // Sort online channels by viewer count (offline channels have 0 viewers by default)
            ChannelEntry.COLUMN_VIEWERS + " DESC, " +
            // Ties are broken alphabetically
            ChannelEntry.COLUMN_DISPLAY_NAME + " COLLATE NOCASE";  // Break ties by display_name

        Cursor cursor = query(null, null, null, sortOrder);

        List<Channel> channelList = new ArrayList<>();

        while (cursor.moveToNext()) {
            // Get all the data from the cursor
            int id = cursor.getInt(cursor.getColumnIndex(ChannelEntry._ID));
            String displayName = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_DISPLAY_NAME));
            String channelUrl = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_CHANNEL_URL));
            String logoUrl = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_LOGO_URL));
            int pinned = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_PINNED));
            int streamType = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_STREAM_TYPE));
            String status = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_STATUS));
            String game = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_GAME));
            int viewers = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_VIEWERS));
            long createdAt = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_CREATED_AT));

            // Build the Channel object from the data
            Channel channel = new Channel(id, displayName, logoUrl, channelUrl, pinned);
            if (streamType != ChannelEntry.STREAM_TYPE_OFFLINE)
                channel.setStream(new Stream(id, game, viewers, status, createdAt, streamType));

            channelList.add(channel);
        }
        cursor.close();

        return channelList;
    }

    public static void updateStreamData(Stream stream) {
        if (stream == null) return;

        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_GAME, stream.getCurrentGame());
        values.put(ChannelEntry.COLUMN_VIEWERS, stream.getCurrentViewers());
        values.put(ChannelEntry.COLUMN_STATUS, stream.getStatus());
        values.put(ChannelEntry.COLUMN_STREAM_TYPE, stream.getStreamType());
        values.put(ChannelEntry.COLUMN_CREATED_AT, stream.getCreatedAt());

        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(stream.getChannelId())};

        update(values, selection, selectionArgs);
    }

    public static void toggleChannelPin(Channel channel) {
        if (channel == null) return;

        // Determine the current pin status of the channel
        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(channel.getId())};
        String[] projection = {ChannelEntry.COLUMN_PINNED};
        Cursor channelCursor = query(projection, selection, selectionArgs, null);

        // If the cursor is empty, then the channel somehow isn't in the db, so abort
        if (channelCursor == null || channelCursor.getCount() == 0) return;

        channelCursor.moveToFirst();
        int currentPinnedStatus = channelCursor.getInt(channelCursor.getColumnIndex(ChannelEntry.COLUMN_PINNED));
        channelCursor.close();

        // Toggle the pin status
        ContentValues values = new ContentValues();
        if (currentPinnedStatus == ChannelEntry.IS_PINNED)
            values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_NOT_PINNED);
        else
            values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_PINNED);

        update(values, selection, selectionArgs);
    }

    public static void removeAllPins() {
        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_NOT_PINNED);

        String selection = ChannelEntry.COLUMN_PINNED + "=?";
        String[] selectionArgs = {Integer.toString(ChannelEntry.IS_PINNED)};

        update(values, selection, selectionArgs);
    }

    public static void deleteAllChannels() {
        // This will completely empty the table
        delete(null, null);
    }

    public static void resetAllStreamData() {
        // Sets all stream data to the default values (they appear as offline)
        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_STREAM_TYPE, ChannelEntry.STREAM_TYPE_OFFLINE);
        values.put(ChannelEntry.COLUMN_GAME, "");
        values.put(ChannelEntry.COLUMN_VIEWERS, 0);
        values.put(ChannelEntry.COLUMN_STATUS, "");
        values.put(ChannelEntry.COLUMN_CREATED_AT, 0);

        update(values, null, null);
    }

    private static void insertChannel(Channel channel) {
        if (channel == null) return;

        ContentValues values = new ContentValues();

        values.put(ChannelEntry._ID, channel.getId());
        values.put(ChannelEntry.COLUMN_DISPLAY_NAME, channel.getDisplayName());
        values.put(ChannelEntry.COLUMN_LOGO_URL, channel.getLogoUrl());
        values.put(ChannelEntry.COLUMN_CHANNEL_URL, channel.getStreamUrl());
        values.put(ChannelEntry.COLUMN_PINNED, channel.getPinned());

        // channel will have a Stream object if it is online
        Stream stream = channel.getStream();
        if (stream != null) {
            values.put(ChannelEntry.COLUMN_GAME, stream.getCurrentGame());
            values.put(ChannelEntry.COLUMN_VIEWERS, stream.getCurrentViewers());
            values.put(ChannelEntry.COLUMN_STATUS, stream.getStatus());
            values.put(ChannelEntry.COLUMN_STREAM_TYPE, stream.getStreamType());
            values.put(ChannelEntry.COLUMN_CREATED_AT, stream.getCreatedAt());
        }

        insert(values);
    }

    private static void deleteChannel(int id) {
        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Integer.toString(id)};
        delete(selection, selectionArgs);
    }

    private static Cursor query(String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.query(ChannelEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
    }

    private static void insert(ContentValues contentValues) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.insert(ChannelEntry.TABLE_NAME, null, contentValues);
    }

    private static void update(ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.update(ChannelEntry.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    private static void delete(String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(ChannelEntry.TABLE_NAME, selection, selectionArgs);
    }
}
