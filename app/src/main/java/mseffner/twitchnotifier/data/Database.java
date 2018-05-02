package mseffner.twitchnotifier.data;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import mseffner.twitchnotifier.data.ChannelContract.FollowEntry;
import mseffner.twitchnotifier.data.ChannelContract.GameEntry;
import mseffner.twitchnotifier.data.ChannelContract.StreamEntry;
import mseffner.twitchnotifier.data.ChannelContract.StreamLegacyEntry;
import mseffner.twitchnotifier.data.ChannelContract.UserEntry;
import mseffner.twitchnotifier.networking.Containers;


public class Database {

    private static DatabaseHelper dbHelper;

    private Database() {
    }

    public static void initialize(Context context) {
        dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static void destroy() {
        dbHelper = null;
    }

    public static void insertFollowsData(@NonNull Containers.Follows follows) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Set<Long> idSet = getFollowIdSet();
        wrapTransaction(database, () -> {
            for (Containers.Follows.Data data : follows.data) {
                long id = Long.parseLong(data.to_id);
                ContentValues values = new ContentValues();
                // Check if the id is already in follows
                if (idSet.contains(id)) {  // If it is, mark it as clean
                    values.put(FollowEntry.COLUMN_DIRTY, FollowEntry.CLEAN);
                    String selection = FollowEntry._ID + " =?";
                    update(FollowEntry.TABLE_NAME, values, selection, new String[]{data.to_id});
                } else {  // Otherwise insert it
                    values.put(FollowEntry._ID, id);
                    insert(database, FollowEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_IGNORE);
                }
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
                insert(database, GameEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
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
                insert(database, UserEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
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

                insert(database, StreamEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        });
    }

    public static void insertStreamsLegacyData(@NonNull Containers.StreamsLegacy streams) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        wrapTransaction(database, () -> {
            for (Containers.StreamsLegacy.Data data : streams.streams) {
                ContentValues values = new ContentValues();
                values.put(StreamLegacyEntry._ID, data.channel._id);
                values.put(StreamLegacyEntry.COLUMN_GAME, data.game);
                int streamType = "live".equals(data.stream_type) ? StreamEntry.STREAM_TYPE_LIVE : StreamEntry.STREAM_TYPE_RERUN;
                values.put(StreamLegacyEntry.COLUMN_TYPE, streamType);
                values.put(StreamLegacyEntry.COLUMN_TITLE, data.channel.status);
                values.put(StreamLegacyEntry.COLUMN_VIEWER_COUNT, data.viewers);
                values.put(StreamLegacyEntry.COLUMN_STARTED_AT, getUnixTimestampFromUTC(data.created_at));
                values.put(StreamLegacyEntry.COLUMN_LANGUAGE, data.channel.language);
                values.put(StreamLegacyEntry.COLUMN_THUMBNAIL_URL, data.preview.template);

                insert(database, StreamLegacyEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
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

    public static List<ListEntry> getAllFollowsLegacy() {
        String selection =
                "SELECT " +
                        FollowEntry.TABLE_NAME + "." + FollowEntry._ID + ", " +
                        FollowEntry.TABLE_NAME + "." + FollowEntry.COLUMN_PINNED + ", " +
                        UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_LOGIN + ", " +
                        UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_DISPLAY_NAME + ", " +
                        UserEntry.TABLE_NAME + "." + UserEntry.COLUMN_PROFILE_IMAGE_URL + ", " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry.COLUMN_GAME + ", " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry.COLUMN_TYPE + ", " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry.COLUMN_TITLE + ", " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry.COLUMN_VIEWER_COUNT + ", " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry.COLUMN_STARTED_AT + ", " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry.COLUMN_LANGUAGE + ", " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry.COLUMN_THUMBNAIL_URL +
                        " FROM " + FollowEntry.TABLE_NAME +
                        " INNER JOIN " + UserEntry.TABLE_NAME + " ON " +
                        FollowEntry.TABLE_NAME + "." + FollowEntry._ID + " = " +
                        UserEntry.TABLE_NAME + "." + UserEntry._ID +
                        " LEFT OUTER JOIN " + StreamLegacyEntry.TABLE_NAME + " ON " +
                        FollowEntry.TABLE_NAME + "." + FollowEntry._ID + " = " +
                        StreamLegacyEntry.TABLE_NAME + "." + StreamLegacyEntry._ID + ";";

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(selection, null);

        // Get column indices
        int id = cursor.getColumnIndex(FollowEntry._ID);
        int pinned = cursor.getColumnIndex(FollowEntry.COLUMN_PINNED);
        int login = cursor.getColumnIndex(UserEntry.COLUMN_LOGIN);
        int display_name = cursor.getColumnIndex(UserEntry.COLUMN_DISPLAY_NAME);
        int profile_image_url = cursor.getColumnIndex(UserEntry.COLUMN_PROFILE_IMAGE_URL);
        int type = cursor.getColumnIndex(StreamLegacyEntry.COLUMN_TYPE);
        int title = cursor.getColumnIndex(StreamLegacyEntry.COLUMN_TITLE);
        int viewer_count = cursor.getColumnIndex(StreamLegacyEntry.COLUMN_VIEWER_COUNT);
        int started_at = cursor.getColumnIndex(StreamLegacyEntry.COLUMN_STARTED_AT);
        int language = cursor.getColumnIndex(StreamLegacyEntry.COLUMN_LANGUAGE);
        int thumbnail_url = cursor.getColumnIndex(StreamLegacyEntry.COLUMN_THUMBNAIL_URL);
        int game_name = cursor.getColumnIndex(StreamLegacyEntry.COLUMN_GAME);

        List<ListEntry> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(new ListEntry(
                            cursor.getLong(id),
                            cursor.getInt(pinned),
                            cursor.getString(login),
                            cursor.getString(display_name),
                            cursor.getString(profile_image_url),
                            cursor.getInt(type),
                            cursor.getString(title),
                            cursor.getInt(viewer_count),
                            cursor.getLong(started_at),
                            cursor.getString(language),
                            cursor.getString(thumbnail_url),
                            cursor.getString(game_name),
                            ""
                    )
            );
        }
        cursor.close();
        return list;
    }

    public static List<ListEntry> getAllFollows() {
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

        // Get column indices
        int id = cursor.getColumnIndex(FollowEntry._ID);
        int pinned = cursor.getColumnIndex(FollowEntry.COLUMN_PINNED);
        int login = cursor.getColumnIndex(UserEntry.COLUMN_LOGIN);
        int display_name = cursor.getColumnIndex(UserEntry.COLUMN_DISPLAY_NAME);
        int profile_image_url = cursor.getColumnIndex(UserEntry.COLUMN_PROFILE_IMAGE_URL);
        int type = cursor.getColumnIndex(StreamEntry.COLUMN_TYPE);
        int title = cursor.getColumnIndex(StreamEntry.COLUMN_TITLE);
        int viewer_count = cursor.getColumnIndex(StreamEntry.COLUMN_VIEWER_COUNT);
        int started_at = cursor.getColumnIndex(StreamEntry.COLUMN_STARTED_AT);
        int language = cursor.getColumnIndex(StreamEntry.COLUMN_LANGUAGE);
        int thumbnail_url = cursor.getColumnIndex(StreamEntry.COLUMN_THUMBNAIL_URL);
        int game_name = cursor.getColumnIndex(GameEntry.COLUMN_NAME);
        int box_art_url = cursor.getColumnIndex(GameEntry.COLUMN_BOX_ART_URL);

        List<ListEntry> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(new ListEntry(
                    cursor.getLong(id),
                    cursor.getInt(pinned),
                    cursor.getString(login),
                    cursor.getString(display_name),
                    cursor.getString(profile_image_url),
                    cursor.getInt(type),
                    cursor.getString(title),
                    cursor.getInt(viewer_count),
                    cursor.getLong(started_at),
                    cursor.getString(language),
                    cursor.getString(thumbnail_url),
                    cursor.getString(game_name),
                    cursor.getString(box_art_url)
                    )
            );
        }
        cursor.close();
        return list;
    }

    public static List<ListEntry> getTopStreams() {
        String selection =
            "SELECT " +
                StreamEntry.TABLE_NAME + "." + StreamEntry._ID + ", " +
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
                " FROM " + StreamEntry.TABLE_NAME +
                " INNER JOIN " + UserEntry.TABLE_NAME + " ON " +
                StreamEntry.TABLE_NAME + "." + StreamEntry._ID + " = " +
                UserEntry.TABLE_NAME + "." + UserEntry._ID +
                " LEFT OUTER JOIN " + GameEntry.TABLE_NAME + " ON " +
                StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_GAME_ID + " = " +
                GameEntry.TABLE_NAME + "." + GameEntry._ID +
            " ORDER BY " + StreamEntry.TABLE_NAME + "." + StreamEntry.COLUMN_VIEWER_COUNT + " DESC;";

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(selection, null);

        // Get column indices
        int id = cursor.getColumnIndex(StreamEntry._ID);
        int login = cursor.getColumnIndex(UserEntry.COLUMN_LOGIN);
        int display_name = cursor.getColumnIndex(UserEntry.COLUMN_DISPLAY_NAME);
        int profile_image_url = cursor.getColumnIndex(UserEntry.COLUMN_PROFILE_IMAGE_URL);
        int type = cursor.getColumnIndex(StreamEntry.COLUMN_TYPE);
        int title = cursor.getColumnIndex(StreamEntry.COLUMN_TITLE);
        int viewer_count = cursor.getColumnIndex(StreamEntry.COLUMN_VIEWER_COUNT);
        int started_at = cursor.getColumnIndex(StreamEntry.COLUMN_STARTED_AT);
        int language = cursor.getColumnIndex(StreamEntry.COLUMN_LANGUAGE);
        int thumbnail_url = cursor.getColumnIndex(StreamEntry.COLUMN_THUMBNAIL_URL);
        int game_name = cursor.getColumnIndex(GameEntry.COLUMN_NAME);
        int box_art_url = cursor.getColumnIndex(GameEntry.COLUMN_BOX_ART_URL);

        List<ListEntry> list = new ArrayList<>();
        int i = 0;
        while (cursor.moveToNext() && i < 100 /* only get top 100 */) {
            list.add(new ListEntry(
                            cursor.getLong(id),
                            FollowEntry.NOT_PINNED,
                            cursor.getString(login),
                            cursor.getString(display_name),
                            cursor.getString(profile_image_url),
                            cursor.getInt(type),
                            cursor.getString(title),
                            cursor.getInt(viewer_count),
                            cursor.getLong(started_at),
                            cursor.getString(language),
                            cursor.getString(thumbnail_url),
                            cursor.getString(game_name),
                            cursor.getString(box_art_url)
                    )
            );
            i++;
        }
        cursor.close();
        return list;
    }

    public static long[] getUnknownUserIds() {
        long[] fromFollows = getUnknownUserIdsFromFollows();
        long[] fromStreams = getUnknownUserIdsFromStreams();
        long[] userIds = new long[fromFollows.length + fromStreams.length];
        int i = 0;
        for (long id : fromFollows)
            userIds[i++] = id;
        for (long id : fromStreams)
            userIds[i++] = id;
        return userIds;
    }

    private static long[] getUnknownUserIdsFromFollows() {
        return getUnknownIds(FollowEntry.TABLE_NAME, FollowEntry._ID,
                UserEntry.TABLE_NAME, UserEntry._ID);
    }

    private static long[] getUnknownUserIdsFromStreams() {
        long streams[] = getUnknownIds(StreamEntry.TABLE_NAME, StreamEntry._ID,
                UserEntry.TABLE_NAME, UserEntry._ID);
        long streamsLegacy[] = getUnknownIds(StreamLegacyEntry.TABLE_NAME, StreamLegacyEntry._ID,
                UserEntry.TABLE_NAME, UserEntry._ID);
        long total[] = new long[streams.length + streamsLegacy.length];
        int i = 0;
        for (long id : streams)
            total[i++] = id;
        for (long id : streamsLegacy)
            total[i++] = id;
        return total;
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
                null, null);
        long[] idArray = new long[cursor.getCount()];
        int idColumnIndex = cursor.getColumnIndex(FollowEntry._ID);

        int i = 0;
        while (cursor.moveToNext())
            idArray[i++] = cursor.getLong(idColumnIndex);
        return idArray;
    }

    public static Set<Long> getFollowIdSet() {
        return getIdSet(FollowEntry.TABLE_NAME, FollowEntry._ID);
    }

    private static Set<Long> getIdSet(String tableName, String columnName) {
        Cursor cursor = query(tableName, new String[]{columnName},
                null, null);
        Set<Long> set = new HashSet<>();
        int idColumnIndex = cursor.getColumnIndex(columnName);

        while (cursor.moveToNext())
            set.add(cursor.getLong(idColumnIndex));
        return set;
    }

    public static void toggleChannelPin(long id) {
        // Determine the current pin status of the channel
        String selection = FollowEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(id)};
        String[] projection = {FollowEntry.COLUMN_PINNED};
        Cursor cursor = query(FollowEntry.TABLE_NAME, projection, selection, selectionArgs);
        // If the cursor is empty, then the channel somehow isn't in the db, so abort
        if (cursor == null || cursor.getCount() == 0) return;
        // Get current status
        cursor.moveToFirst();
        int status = cursor.getInt(cursor.getColumnIndex(FollowEntry.COLUMN_PINNED));
        cursor.close();

        // Toggle pin status
        ContentValues values = new ContentValues();
        int newStatus = status == FollowEntry.PINNED ? FollowEntry.NOT_PINNED : FollowEntry.PINNED;
        values.put(FollowEntry.COLUMN_PINNED, newStatus);
        update(FollowEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    public static void removeAllPins() {
        ContentValues values = new ContentValues();
        values.put(FollowEntry.COLUMN_PINNED, FollowEntry.NOT_PINNED);
        update(FollowEntry.TABLE_NAME, values, null, null);
    }

    public static void setFollowsDirty() {
        ContentValues values = new ContentValues();
        values.put(FollowEntry.COLUMN_DIRTY, FollowEntry.DIRTY);
        update(FollowEntry.TABLE_NAME, values, null, null);
    }

    public static void cleanFollows() {
        String selection = FollowEntry.COLUMN_DIRTY + "=?";
        String[] selectionArgs = {Integer.toString(FollowEntry.DIRTY)};
        delete(FollowEntry.TABLE_NAME, selection, selectionArgs);
    }

    public static void deleteAllStreams() {
        delete(StreamEntry.TABLE_NAME, null, null);
        delete(StreamLegacyEntry.TABLE_NAME, null, null);
    }

    public static void deleteAllFollows() {
        delete(FollowEntry.TABLE_NAME, null, null);
    }

    private static Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.query(tableName, projection, selection, selectionArgs, null, null, null);
    }

    private static void insert(SQLiteDatabase database, String tableName, ContentValues contentValues, int conflict) {
        database.insertWithOnConflict(tableName, null, contentValues, conflict);
    }

    private static void update(String tableName, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.update(tableName, contentValues, selection, selectionArgs);
    }

    private static void delete(String tableName, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(tableName, selection, selectionArgs);
    }

    private static long getUnixTimestampFromUTC(String utcFormattedTimestamp) {
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
