package nz.co.curtainsolutions.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;

import nz.co.curtainsolutions.provider.CSContract.Jobs;
import nz.co.curtainsolutions.provider.CSContract.Rooms;
import nz.co.curtainsolutions.provider.CSContract.Windows;
import nz.co.curtainsolutions.provider.CSDatabase.Tables;
import nz.co.curtainsolutions.util.SelectionBuilder;

/**
 * Created by brettyukich on 20/08/13.
 */
public class CSProvider extends ContentProvider {
    public static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final String TAG = CSProvider.class.getSimpleName();
    private static final int JOBS = 100;
    private static final int JOBS_ID = 101;
    private static final int ROOMS = 200;
    private static final int ROOMS_ID = 201;
    private static final int WINDOWS = 300;
    private static final int WINDOWS_ID = 301;
    private CSDatabase mOpenHelper;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CSContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "jobs", JOBS);
        matcher.addURI(authority, "jobs/*", JOBS_ID);

        matcher.addURI(authority, "rooms", ROOMS);
        matcher.addURI(authority, "rooms/*", ROOMS_ID);

        matcher.addURI(authority, "windows", WINDOWS);
        matcher.addURI(authority, "windows/*", WINDOWS_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new CSDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.i(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                Cursor cursor = builder.where(selection, selectionArgs).query(db, projection, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;

            }
        }
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case JOBS:
                return Jobs.CONTENT_TYPE;
            case JOBS_ID:
                return Jobs.CONTENT_ITEM_TYPE;
            case ROOMS:
                return Rooms.CONTENT_TYPE;
            case ROOMS_ID:
                return Rooms.CONTENT_ITEM_TYPE;
            case WINDOWS:
                return Windows.CONTENT_TYPE;
            case WINDOWS_ID:
                return Windows.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.i(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case JOBS:
                db.insertOrThrow(Tables.JOBS, null, values);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return Jobs.buildJobUri(values.getAsString(Jobs._ID));

            case ROOMS:
                db.insertOrThrow(Tables.ROOMS, null, values);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return Rooms.buildRoomUri(values.getAsString(Rooms._ID));

            case WINDOWS:
                db.insertOrThrow(Tables.WINDOWS, null, values);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return Windows.buildWindowUri(values.getAsString(Windows._ID));

            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.i(TAG, "delete(uri=" + uri + ")");

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null, false);

        return retVal;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.i(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        getContext().getContentResolver().notifyChange(uri, null, false);

        return retVal;    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {

            case ROOMS: {
                return builder.table(Tables.ROOMS);
            }

            case ROOMS_ID: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.ROOMS)
                        .where(Rooms._ID + "=?", roomId);
            }

            case JOBS: {
                return builder.table(Tables.JOBS);
            }

            case JOBS_ID: {
                final String jobId = Jobs.getJobId(uri);
                return builder.table(Tables.JOBS)
                        .where(Jobs._ID + "=?", jobId);
            }

            case WINDOWS: {
                return builder.table(Tables.WINDOWS);
            }

            case WINDOWS_ID: {
                final String windowId = Windows.getWindowId(uri);
                return builder.table(Tables.WINDOWS)
                        .where(Windows._ID + "=?", windowId);
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case ROOMS: {
                return builder
                        .table(Tables.ROOMS)
                        .mapToTable(Rooms.JOB_ID, Tables.ROOMS)
                        .mapToTable(Rooms.DESCRIPTION, Tables.ROOMS);
            }
            case ROOMS_ID: {
                final String roomId = Rooms.getRoomId(uri);
                return builder
                        .table(Tables.ROOMS)
                        .mapToTable(Rooms.JOB_ID, Tables.ROOMS)
                        .mapToTable(Rooms.DESCRIPTION, Tables.ROOMS)
                        .where(Rooms._ID + "=?", roomId);
            }
            case WINDOWS_ID: {
                final String windowId = Windows.getWindowId(uri);
                return builder
                        .table(Tables.WINDOWS)
                        .mapToTable(Windows.ROOM_ID, Tables.WINDOWS)
                        .mapToTable(Windows.HEIGHT, Tables.WINDOWS)
                        .mapToTable(Windows.WIDTH, Tables.WINDOWS)
                        .where(Windows._ID + "=?", windowId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }
}
