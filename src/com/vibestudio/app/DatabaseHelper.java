package com.vibestudio.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "vibestudio.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_API_KEYS = "api_keys";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PROVIDER = "provider";
    public static final String COLUMN_KEY_VALUE = "key_value";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_API_KEYS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PROVIDER + " TEXT UNIQUE NOT NULL, " +
                COLUMN_KEY_VALUE + " TEXT NOT NULL" +
                ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_API_KEYS);
        onCreate(db);
    }

    public void saveApiKey(String provider, String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVIDER, provider);
        values.put(COLUMN_KEY_VALUE, key);

        int rows = db.update(TABLE_API_KEYS, values, COLUMN_PROVIDER + "=?", new String[]{provider});
        if (rows == 0) {
            db.insert(TABLE_API_KEYS, null, values);
        }
        db.close();
    }

    public String getApiKey(String provider) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_API_KEYS, new String[]{COLUMN_KEY_VALUE},
                COLUMN_PROVIDER + "=?", new String[]{provider},
                null, null, null);

        String key = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                key = cursor.getString(0);
            }
            cursor.close();
        }
        db.close();
        return key;
    }
}
