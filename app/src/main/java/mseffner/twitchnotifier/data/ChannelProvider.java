package mseffner.twitchnotifier.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


public class ChannelProvider extends ContentProvider {

    private static final String LOG_TAG = ChannelProvider.class.getSimpleName();

    // UriMatcher codes for the two tables
    private static final int CHANNELS = 100;
    private static final int STREAMS = 101;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // TODO use addUri() to add the Uris that should be handled
    }

    private ChannelDbHelper dbHelper;


    @Override
    public boolean onCreate() {
        dbHelper = new ChannelDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s,
                        @Nullable String[] strings1, @Nullable String s1) {

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        switch (uriMatcher.match(uri)) {
            case CHANNELS:
                // TODO handle query of the channels table
                break;
            case STREAMS:
                // TODO handle query of the streams table
                break;
            default:
                throwIllegalArgumentException("query", uri);
        }

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        switch (uriMatcher.match(uri)) {
            case CHANNELS:
                // TODO get the channels table type
                break;
            case STREAMS:
                // TODO get the streams table type
                break;
            default:
                throwIllegalArgumentException("getType", uri);
        }

        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        // TODO validate input

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {
            case CHANNELS:
                // TODO handle insert into the channels table
                break;
            case STREAMS:
                // TODO handle insert into the streams table
                break;
            default:
                throwIllegalArgumentException("insert", uri);
        }

        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {
            case CHANNELS:
                // TODO handle deletion from the channels table
                break;
            case STREAMS:
                // TODO handle deletion from the streams table
                break;
            default:
                throwIllegalArgumentException("delete", uri);
        }

        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s,
                      @Nullable String[] strings) {

        // TODO validate input

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {
            case CHANNELS:
                // TODO handle update of the channels table
                break;
            case STREAMS:
                // TODO handle update of the streams table
                break;
            default:
                throwIllegalArgumentException("update", uri);
        }

        return 0;
    }

    private void throwIllegalArgumentException(String operation, Uri uri) {
        throw new IllegalArgumentException("Cannot perform " + operation + ", unknown URI: " + uri);
    }
}
