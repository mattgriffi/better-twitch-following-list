package mseffner.twitchnotifier.data;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import mseffner.twitchnotifier.data.ChannelContract.FollowEntry;
import mseffner.twitchnotifier.data.ChannelContract.GameEntry;
import mseffner.twitchnotifier.data.ChannelContract.StreamEntry;
import mseffner.twitchnotifier.data.ChannelContract.UserEntry;
import mseffner.twitchnotifier.networking.Containers;


public class ChannelDb {

    private static ChannelDbHelper dbHelper;

    private ChannelDb() {
    }

    public static void initialize(Context context) {
        dbHelper = new ChannelDbHelper(context.getApplicationContext());
    }

    public static void destroy() {
        dbHelper = null;
    }

    public static void insertFollowsData(@NonNull Containers.Follows follows) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        wrapTransaction(database, () -> {
            for (Containers.Follows.Data data : follows.data) {
                ContentValues values = new ContentValues();
                values.put(FollowEntry._ID, Long.parseLong(data.to_id));
                insert(database, FollowEntry.TABLE_NAME, values);
            }
        });
    }

    public static void insertGamesData(@NonNull Containers.Games games) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        wrapTransaction(database, () -> {
            for (Containers.Games.Data data : games.data) {
                ContentValues values = new ContentValues();
                values.put(GameEntry._ID, Long.parseLong(data.id));
                values.put(GameEntry.COLUMN_NAME, data.name);
                values.put(GameEntry.COLUMN_BOX_ART_URL, data.box_art_url);
                insert(database, GameEntry.TABLE_NAME, values);
            }
        });
    }

    public static void insertUsersData(@NonNull Containers.Users users) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        wrapTransaction(database, () -> {
            for (Containers.Users.Data data : users.data) {
                ContentValues values = new ContentValues();
                values.put(UserEntry._ID, Long.parseLong(data.id));
                values.put(UserEntry.COLUMN_LOGIN, data.login);
                values.put(UserEntry.COLUMN_DISPLAY_NAME, data.display_name);
                values.put(UserEntry.COLUMN_PROFILE_IMAGE_URL, data.profile_image_url);
                insert(database, UserEntry.TABLE_NAME, values);
            }
        });
    }

    public static void insertStreamsData(@NonNull Containers.Streams streams) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        wrapTransaction(database, () -> {
            for (Containers.Streams.Data data : streams.data) {
                ContentValues values = new ContentValues();
                values.put(StreamEntry._ID, Long.parseLong(data.user_id));
                if (!data.game_id.equals(""))
                    values.put(StreamEntry.COLUMN_GAME_ID, Long.parseLong(data.game_id));
                int streamType = data.type.equals("live") ? StreamEntry.STREAM_TYPE_LIVE : StreamEntry.STREAM_TYPE_RERUN;
                values.put(StreamEntry.COLUMN_TYPE, streamType);
                values.put(StreamEntry.COLUMN_TITLE, data.title);
                values.put(StreamEntry.COLUMN_VIEWER_COUNT, data.viewer_count);
                values.put(StreamEntry.COLUMN_STARTED_AT, getUnixTimestampFromUTC(data.started_at));
                values.put(StreamEntry.COLUMN_LANGUAGE, data.language);
                values.put(StreamEntry.COLUMN_THUMBNAIL_URL, data.thumbnail_url);

                insert(database, StreamEntry.TABLE_NAME, values);
            }
        });
    }

    private static void wrapTransaction(SQLiteDatabase db, Runnable r) {
        db.beginTransaction();
        try {
            r.run();
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();

        }
    }

    public static List<ListEntry> getAllChannels() {
        String selection =
            "SELECT " +
                FollowEntry.TABLE_NAME + "." + FollowEntry._ID + ", " +
                FollowEntry.TABLE_NAME + "." + FollowEntry.COLUMN_PINNED + ", " +
                UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_LOGIN + ", " +
                UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_DISPLAY_NAME + ", " +
                UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_PROFILE_IMAGE_URL + ", " +
                StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_TYPE + ", " +
                StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_TITLE + ", " +
                StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_VIEWER_COUNT + ", " +
                StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_STARTED_AT + ", " +
                StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_LANGUAGE + ", " +
                StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_THUMBNAIL_URL + ", " +
                GameEntry.TABLE_NAME + "." + GameEntry.COLUMN_NAME + ", " +
                GameEntry.TABLE_NAME + "." + GameEntry.COLUMN_BOX_ART_URL +
            " FROM " + FollowEntry.TABLE_NAME +
                " INNER JOIN " + UserEntry.TABLE_NAME + " ON " +
                    FollowEntry.TABLE_NAME + "." + FollowEntry._ID + " = " +
                    UserEntry.TABLE_NAME + "." + UserEntry._ID +
                " LEFT OUTER JOIN " + StreamEntry.TABLE_NAME + " ON " +
                    FollowEntry.TABLE_NAME + "." + FollowEntry._ID + " = " +
                    StreamEntry.TABLE_NAME + "." + StreamEntry._ID +
                " LEFT OUTER JOIN " + GameEntry.TABLE_NAME + " ON " +
                    StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_GAME_ID + " = " +
                    GameEntry.TABLE_NAME + "." + GameEntry._ID + ";";

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(selection, null);

        List<ListEntry> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(FollowEntry._ID));
            int pinned = cursor.getInt(cursor.getColumnIndex(FollowEntry.COLUMN_PINNED));
            String login = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_LOGIN));
            String displayName = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_DISPLAY_NAME));
            String profileImageUrl = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_PROFILE_IMAGE_URL));
            int type = cursor.getInt(cursor.getColumnIndex(StreamEntry.COLUMN_TYPE));
            String title = cursor.getString(cursor.getColumnIndex(StreamEntry.COLUMN_TITLE));
            int viewerCount = cursor.getInt(cursor.getColumnIndex(StreamEntry.COLUMN_VIEWER_COUNT));
            long startedAt = cursor.getLong(cursor.getColumnIndex(StreamEntry.COLUMN_STARTED_AT));
            String language = cursor.getString(cursor.getColumnIndex(StreamEntry.COLUMN_LANGUAGE));
            String thumbnailUrl = cursor.getString(cursor.getColumnIndex(StreamEntry.COLUMN_THUMBNAIL_URL));
            String gameName = cursor.getString(cursor.getColumnIndex(GameEntry.COLUMN_NAME));
            String boxArtUrl = cursor.getString(cursor.getColumnIndex(GameEntry.COLUMN_BOX_ART_URL));

            ListEntry listEntry = new ListEntry(id, pinned, login, displayName, profileImageUrl,
                    type, title, viewerCount, startedAt, language, thumbnailUrl, gameName, boxArtUrl);
            list.add(listEntry);
        }
        cursor.close();
        return list;
    }

    public static long[] getUnknownUserIds() {
        return getUnknownIds(FollowEntry.TABLE_NAME, FollowEntry._ID,
                UserEntry.TABLE_NAME, UserEntry._ID);
    }

    public static long[] getUnknownGameIds() {
        return getUnknownIds(StreamEntry.TABLE_NAME, StreamEntry.COLUMN_GAME_ID,
                GameEntry.TABLE_NAME, GameEntry._ID);
    }

    private static long[] getUnknownIds(String table1, String column1, String table2, String column2) {
        // Make query
        String query = "SELECT DISTINCT " + column1 + " FROM " + table1 +
                " WHERE " + column1 + " NOT IN " +
                "(SELECT DISTINCT " + column2 + " FROM " + table2 + ");";
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, null);
        // Build array
        long[] ids = new long[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext())
            ids[i++] = cursor.getLong(cursor.getColumnIndex(column1));
        cursor.close();
        return ids;
    }

    public static long[] getAllFollowIds() {
            Cursor cursor = query(FollowEntry.TABLE_NAME, new String[]{FollowEntry._ID},
                    null, null, null);
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

    private static void insert(SQLiteDatabase database, String tableName, ContentValues contentValues) {
        database.insertWithOnConflict(tableName, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static void update(String tableName, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.update(tableName, contentValues, selection, selectionArgs);
    }

    private static void delete(String tableName, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(tableName, selection, selectionArgs);
    }

    public static long getUnixTimestampFromUTC(String utcFormattedTimestamp) {
        @SuppressLint("SimpleDateFormat")
        // This is the format returned by the Twitch API
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return format.parse(utcFormattedTimestamp).getTime() / 1000;
        } catch (ParseException e) {
            return 0;
        }
    }
}
