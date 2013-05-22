package com.rchukka.trantil.content;

import java.util.List;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

@SuppressWarnings("rawtypes")
public class DataStore {

    private static final String   AUTHORITY = "content://com.rchukka.trantil/";
    private static final String   TAG       = "trantil_datastore";
    private static Context        CONTEXT   = null;
    private static DatabaseHelper DB_HELPER;
    private static SQLiteDatabase DB;
    private static String         DB_NAME   = "trantil.db";
    private static TableManager   TABLE_MANAGER;

    public static void init(Application app) {
        init(app, 1);
    }

    public static void init(Application app, int storeDBVersion) {
        init(app.getApplicationContext(), storeDBVersion);
    }

    static void init(Context context, int storeDBVersion) {
        if (CONTEXT != null) {
            Log.e(TAG, "DataStore has already been initialized.");
            return;
        }

        CONTEXT = context;
        DB_HELPER = new DatabaseHelper(CONTEXT, getDBName(), storeDBVersion);
        DB = DB_HELPER.getWritableDatabase();
        TABLE_MANAGER = new TableManager(DB_HELPER);
    }

    private static String getDBName() {
        return DB_NAME;
    }

    public static String getTableName(Class klass) {

        if (TABLE_MANAGER == null)
            throw new RuntimeException(
                    "Datastore has not been initialized."
                            + " Missing DataStore.init() call in application onCreate()");

        return TABLE_MANAGER.maybeCreateOrUpdateTable(klass);
    }

    private static Uri getUri(Class klass) {
        return Uri.parse(AUTHORITY + klass.getName().replace(".", ""));
    }

    public static void registerForChanges(Cursor cursor, Class klass) {
        cursor.setNotificationUri(CONTEXT.getContentResolver(), getUri(klass));
    }

    public static void notifyChange(Class klass) {
        CONTEXT.getContentResolver().notifyChange(getUri(klass), null);
    }

    public static Cursor rawQuery(String sqlQuery) {
        return DB.rawQuery(sqlQuery, null);
    }

    // start interface implementation similar to content provider
    public static int delete(Class klass, String selection,
            String[] selectionArgs) {
        return delete(klass, selection, selectionArgs, false);
    }
    
    public static int delete(Class klass, String selection,
            String[] selectionArgs, boolean autoNotify) {
        int count = DB.delete(getTableName(klass), selection, selectionArgs);
        if (autoNotify) DataStore.notifyChange(klass);
        return count;
    }

    public static long insert(Class klass, ContentValues values) {
        return insert(klass, values, false);
    }

    public static long insert(Class klass, ContentValues values,
            boolean autoNotify) {
        long id = DB.insertOrThrow(getTableName(klass), null, values);
        if (autoNotify) DataStore.notifyChange(klass);
        return id;
    }

    public static void insert(Class klass, List<ContentValues> values) {
        insert(klass, values, false);
    }

    public static void insert(Class klass, List<ContentValues> values,
            boolean autoNotify) {
        try {
            DB.beginTransaction();
            String tableName = getTableName(klass);
            for (ContentValues v : values) {
                DB.insertOrThrow(tableName, null, v);
            }
            DB.setTransactionSuccessful();
        } finally {
            DB.endTransaction();
        }
    }

    public static Cursor query(Class klass, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return query(klass, projection, selection, selectionArgs, sortOrder,
                false);
    }

    public static Cursor query(Class klass, String[] projection,
            String selection, String[] selectionArgs, String sortOrder,
            boolean autoRegister) {
        Cursor c = DB.query(getTableName(klass), projection, selection,
                selectionArgs, null, null, sortOrder);
        if (autoRegister) DataStore.registerForChanges(c, klass);
        return c;
    }

    public static int update(Class klass, ContentValues values,
            String selection, String[] selectionArgs) {
        return update(klass, values, selection, selectionArgs, false);
    }

    public static int update(Class klass, ContentValues values,
            String selection, String[] selectionArgs, boolean autoNotify) {        
        int count = DB.update(getTableName(klass), values, selection, selectionArgs);
        if (autoNotify) DataStore.notifyChange(klass);
        return count;
    }
    // end interface implementation similar to content provider

    // start instrumentation API
    /** ONLY for instrumentation purpose. Do not use it in real app. */
    static void dropTable(Class klass) {
        DataStore.TABLE_MANAGER.dropTable(klass);
    }

    /** ONLY for instrumentation purpose. Do not use it in real app. */
    static void dropAllTables() {
        DataStore.TABLE_MANAGER.dropAllTables();
    }

    /** ONLY for instrumentation purpose. Do not use it in real app. */
    static boolean tableExists(Class klass) {
        return DataStore.TABLE_MANAGER.tableExists(klass);
    }

    // end instrumentation API
}
