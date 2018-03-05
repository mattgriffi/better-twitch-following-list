package mseffner.twitchnotifier.data;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import mseffner.twitchnotifier.data.ChannelContract.FollowEntry;
import mseffner.twitchnotifier.data.ChannelContract.UserEntry;
import mseffner.twitchnotifier.data.ChannelContract.StreamEntry;
import mseffner.twitchnotifier.data.ChannelContract.GameEntry;
import mseffner.twitchnotifier.networking.Containers;


public class ChannelDb {

    private static ChannelDbHelper dbHelper;


    private ChannelDb() {}

    public static void initialize(Context context) {
        dbHelper = new ChannelDbHelper(context.getApplicationContext());
    }

    public static void destroy() {
        dbHelper = null;
    }

    public static void insertFollowsData(@NonNull Containers.Follows follows) {
        for (Containers.Follows.Data data : follows.data) {
            ContentValues values = new ContentValues();
            values.put(FollowEntry._ID, Long.parseLong(data.to_id));
            insert(FollowEntry.TABLE_NAME, values);
        }
    }

    public static void insertGamesData(@NonNull Containers.Games games) {
        for (Containers.Games.Data data : games.data) {
            ContentValues values = new ContentValues();
            values.put(GameEntry._ID, Long.parseLong(data.id));
            values.put(GameEntry.COLUMN_NAME, data.name);
            values.put(GameEntry.COLUMN_BOX_ART_URL, data.box_art_url);
            insert(GameEntry.TABLE_NAME, values);
        }
    }

    public static void insertUsersData(@NonNull Containers.Users users) {
        for (Containers.Users.Data data : users.data) {
            ContentValues values = new ContentValues();
            values.put(UserEntry._ID, Long.parseLong(data.id));
            values.put(UserEntry.COLUMN_LOGIN, data.login);
            values.put(UserEntry.COLUMN_DISPLAY_NAME, data.display_name);
            values.put(UserEntry.COLUMN_PROFILE_IMAGE_URL, data.profile_image_url);
            insert(UserEntry.TABLE_NAME, values);
        }
    }

    public static void updateUsersData(@NonNull Containers.Users users) {
        for (Containers.Users.Data data : users.data) {
            ContentValues values = new ContentValues();
            values.put(UserEntry.COLUMN_LOGIN, data.login);
            values.put(UserEntry.COLUMN_DISPLAY_NAME, data.display_name);
            values.put(UserEntry.COLUMN_PROFILE_IMAGE_URL, data.profile_image_url);

            String selection = UserEntry._ID + "=?";
            String[] selectionArgs = {data.id};

            update(UserEntry.TABLE_NAME, values, selection, selectionArgs);
        }
    }

    public static void updateStreamsData(@NonNull Containers.Streams streams) {
        for (Containers.Streams.Data data : streams.data) {
            ContentValues values = new ContentValues();
            values.put(StreamEntry.COLUMN_GAME_ID, Long.parseLong(data.game_id));
            int streamType = data.type.equals("live") ? StreamEntry.STREAM_TYPE_LIVE : StreamEntry.STREAM_TYPE_RERUN;
            values.put(StreamEntry.COLUMN_TYPE, streamType);
            values.put(StreamEntry.COLUMN_TITLE, data.title);
            values.put(StreamEntry.COLUMN_VIEWER_COUNT, Integer.parseInt(data.viewer_count));
            values.put(StreamEntry.COLUMN_STARTED_AT, Stream.getUnixTimestampFromUTC(data.started_at));
            values.put(StreamEntry.COLUMN_LANGUAGE, data.language);
            values.put(StreamEntry.COLUMN_THUMBNAIL_URL, data.thumbnail_url);

            String selection = StreamEntry._ID + "=?";
            String[] selectionArgs = {data.user_id};

            update(StreamEntry.TABLE_NAME, values, selection, selectionArgs);
        }
    }

    public static void updateGamesData(@NonNull Containers.Games games) {
        for (Containers.Games.Data data : games.data) {
            ContentValues values = new ContentValues();
            values.put(GameEntry.COLUMN_NAME, data.name);
            values.put(GameEntry.COLUMN_BOX_ART_URL, data.box_art_url);

            String selection = GameEntry._ID + "=?";
            String[] selectionArgs = {data.id};

            update(GameEntry.TABLE_NAME, values, selection, selectionArgs);
        }
    }

    public static Set<Long> getChannelIdSet() {
        Cursor cursor = query(FollowEntry.TABLE_NAME, true, new String[]{FollowEntry._ID}, null, null, null);
        Set<Long> idSet = new HashSet<>();
        int i = cursor.getColumnIndex(FollowEntry._ID);
        while (cursor.moveToNext())
            idSet.add(cursor.getLong(i));
        return idSet;
    }

    public static Set<Long> getUserIdSet() {
        Cursor cursor = query(UserEntry.TABLE_NAME, true, new String[]{UserEntry._ID}, null, null, null);
        Set<Long> idSet = new HashSet<>();
        int i = cursor.getColumnIndex(UserEntry._ID);
        while (cursor.moveToNext())
            idSet.add(cursor.getLong(i));
        return idSet;
    }

    public static Set<Long> getGameIdSet() {
        Cursor cursor = query(GameEntry.TABLE_NAME, true, new String[]{GameEntry._ID}, null, null, null);
        Set<Long> idSet = new HashSet<>();
        int i = cursor.getColumnIndex(GameEntry._ID);
        while (cursor.moveToNext())
            idSet.add(cursor.getLong(i));
        return idSet;
    }

    public static long[] getAllChannelIds() {
        Cursor cursor = query(FollowEntry.TABLE_NAME, new String[]{FollowEntry._ID}, null, null, null);
        long[] idArray = new long[cursor.getCount()];
        int idColumnIndex = cursor.getColumnIndex(FollowEntry._ID);

        int i = 0;
        while (cursor.moveToNext())
            idArray[i++] = cursor.getLong(idColumnIndex);
        return idArray;
    }

    public static void toggleChannelPin(long id) {
        // Determine the current pin status of the channel
        String selection = FollowEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(id)};
        String[] projection = {FollowEntry.COLUMN_PINNED};
        Cursor cursor = query(FollowEntry.TABLE_NAME, projection, selection, selectionArgs, null);
        // If the cursor is empty, then the channel somehow isn't in the db, so abort
        if (cursor == null || cursor.getCount() == 0) return;
        // Get current status
        cursor.moveToFirst();
        int status = cursor.getInt(cursor.getColumnIndex(FollowEntry.COLUMN_PINNED));
        cursor.close();

        // Toggle pin status
        ContentValues values = new ContentValues();
        int newStatus = status == FollowEntry.IS_PINNED ? FollowEntry.IS_NOT_PINNED : FollowEntry.IS_PINNED;
        values.put(FollowEntry.COLUMN_PINNED, newStatus);
        update(FollowEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    public static void removeAllPins() {
        ContentValues values = new ContentValues();
        values.put(FollowEntry.COLUMN_PINNED, FollowEntry.IS_NOT_PINNED);
        update(FollowEntry.TABLE_NAME, values, null, null);
    }

    public static void setFollowsDirty() {
        ContentValues values = new ContentValues();
        values.put(FollowEntry.COLUMN_DIRTY, FollowEntry.DIRTY);
        update(FollowEntry.TABLE_NAME, values, null, null);
    }

    public static void setFollowsClean() {
        ContentValues values = new ContentValues();
        values.put(FollowEntry.COLUMN_DIRTY, FollowEntry.CLEAN);
        update(FollowEntry.TABLE_NAME, values, null, null);
    }

    public static void cleanFollows() {
        String selection = FollowEntry.COLUMN_DIRTY + "=?";
        String[] selectionArgs = {Integer.toString(FollowEntry.DIRTY)};
        delete(FollowEntry.TABLE_NAME, selection, selectionArgs);
    }

    public static void deleteAllStreams() {
        delete(StreamEntry.TABLE_NAME, null, null);
    }

    public static void deleteAllFollows() {
        delete(FollowEntry.TABLE_NAME, null, null);
    }

    private static Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs,
                                String sortOrder) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
    }

    private static Cursor query(String tableName, boolean distinct, String[] projection,
                                String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.query(distinct, tableName, projection, selection,
                selectionArgs, null, null, sortOrder, null);
    }

    private static void insert(String tableName, ContentValues contentValues) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.insertWithOnConflict(tableName, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void update(String tableName, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.update(tableName, contentValues, selection, selectionArgs);
    }

    private static void delete(String tableName, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(tableName, selection, selectionArgs);
    }
}
