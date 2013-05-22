package com.rchukka.trantil.content;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DatabaseHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG            = "trantil_dbhelper";
    private static final int    DEFAULT_DB_VERSION = 1;

    DatabaseHelper(Context context, String dbName) {
        super(context, dbName, null, DEFAULT_DB_VERSION);
    }

    DatabaseHelper(Context context, String dbName, int dbVersion) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS modelversions "
                + "(tablename varchar(100) PRIMARY KEY, classname varchar(100), version INTEGER)");

        Log.d(LOG_TAG, "onCreate: created modelversions table");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // db.execSQL("DROP TABLE IF EXISTS modelversions");
        dropAllTables(db);
        onCreate(db);
    }

    void dropAllTables(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'", null);

        cursor.moveToFirst();
        do {
            db.execSQL("DROP TABLE IF EXISTS " + cursor.getString(0));
        } while (cursor.moveToNext());
        cursor.close();

        Log.d(LOG_TAG, "onUpgrade: dropped _all_ tables");
    }
}